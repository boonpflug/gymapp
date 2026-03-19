package com.gymplatform.modules.marketing;

import com.gymplatform.modules.checkin.CheckIn;
import com.gymplatform.modules.checkin.CheckInRepository;
import com.gymplatform.modules.contract.Contract;
import com.gymplatform.modules.contract.ContractRepository;
import com.gymplatform.modules.contract.ContractStatus;
import com.gymplatform.modules.marketing.dto.AudienceCriteria;
import com.gymplatform.modules.marketing.dto.AudiencePreviewDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.member.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudienceBuilderService {

    private final MemberRepository memberRepository;
    private final CheckInRepository checkInRepository;
    private final ContractRepository contractRepository;

    public List<Member> buildAudience(AudienceCriteria criteria) {
        if (criteria == null) {
            return memberRepository.findAll();
        }

        Specification<Member> spec = buildMemberSpec(criteria);
        List<Member> members = memberRepository.findAll(spec);

        if (criteria.getNoCheckInDays() != null && criteria.getNoCheckInDays() > 0) {
            members = filterByInactivity(members, criteria.getNoCheckInDays());
        }

        if (criteria.getMinCheckInFrequencyDays() != null || criteria.getMaxCheckInFrequencyDays() != null) {
            members = filterByCheckInFrequency(members,
                    criteria.getMinCheckInFrequencyDays(),
                    criteria.getMaxCheckInFrequencyDays());
        }

        if (criteria.getContractStatus() != null) {
            members = filterByContractStatus(members, criteria.getContractStatus());
        }

        if (criteria.getContractExpiresWithinDays() != null) {
            members = filterByContractExpiry(members, criteria.getContractExpiresWithinDays());
        }

        return members;
    }

    public AudiencePreviewDto preview(AudienceCriteria criteria) {
        List<Member> audience = buildAudience(criteria);
        List<AudiencePreviewDto.AudienceMemberSummary> sample = audience.stream()
                .limit(20)
                .map(m -> AudiencePreviewDto.AudienceMemberSummary.builder()
                        .memberId(m.getId())
                        .firstName(m.getFirstName())
                        .lastName(m.getLastName())
                        .email(m.getEmail())
                        .status(m.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return AudiencePreviewDto.builder()
                .totalCount(audience.size())
                .sample(sample)
                .build();
    }

    private Specification<Member> buildMemberSpec(AudienceCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getMemberStatuses() != null && !criteria.getMemberStatuses().isEmpty()) {
                List<MemberStatus> statuses = criteria.getMemberStatuses().stream()
                        .map(MemberStatus::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("status").in(statuses));
            }

            if (criteria.getGender() != null && !criteria.getGender().isBlank()) {
                predicates.add(cb.equal(root.get("gender"), criteria.getGender()));
            }

            if (criteria.getJoinedAfter() != null && !criteria.getJoinedAfter().isBlank()) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("joinDate"),
                        LocalDate.parse(criteria.getJoinedAfter())));
            }

            if (criteria.getJoinedBefore() != null && !criteria.getJoinedBefore().isBlank()) {
                predicates.add(cb.lessThanOrEqualTo(root.get("joinDate"),
                        LocalDate.parse(criteria.getJoinedBefore())));
            }

            if (criteria.getMinAge() != null) {
                LocalDate maxDob = LocalDate.now().minus(Period.ofYears(criteria.getMinAge()));
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxDob));
            }

            if (criteria.getMaxAge() != null) {
                LocalDate minDob = LocalDate.now().minus(Period.ofYears(criteria.getMaxAge() + 1));
                predicates.add(cb.greaterThan(root.get("dateOfBirth"), minDob));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<Member> filterByInactivity(List<Member> members, int noCheckInDays) {
        Instant cutoff = Instant.now().minus(noCheckInDays, ChronoUnit.DAYS);
        return members.stream()
                .filter(m -> {
                    var checkins = checkInRepository.findByMemberIdOrderByCheckInTimeDesc(
                            m.getId(), org.springframework.data.domain.PageRequest.of(0, 1));
                    if (checkins.isEmpty()) return true;
                    return checkins.getContent().get(0).getCheckInTime().isBefore(cutoff);
                })
                .collect(Collectors.toList());
    }

    private List<Member> filterByCheckInFrequency(List<Member> members,
                                                    Integer minDays, Integer maxDays) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        return members.stream()
                .filter(m -> {
                    var checkins = checkInRepository.findByMemberIdOrderByCheckInTimeDesc(
                            m.getId(), org.springframework.data.domain.PageRequest.of(0, 100));
                    long recentCount = checkins.getContent().stream()
                            .filter(c -> c.getCheckInTime().isAfter(thirtyDaysAgo))
                            .count();
                    double avgDaysBetween = recentCount > 0 ? 30.0 / recentCount : 999;
                    if (minDays != null && avgDaysBetween < minDays) return false;
                    if (maxDays != null && avgDaysBetween > maxDays) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Member> filterByContractStatus(List<Member> members, String contractStatusStr) {
        ContractStatus contractStatus = ContractStatus.valueOf(contractStatusStr);
        return members.stream()
                .filter(m -> {
                    List<Contract> contracts = contractRepository.findByMemberIdAndStatus(
                            m.getId(), contractStatus);
                    return !contracts.isEmpty();
                })
                .collect(Collectors.toList());
    }

    private List<Member> filterByContractExpiry(List<Member> members, int withinDays) {
        LocalDate deadline = LocalDate.now().plusDays(withinDays);
        return members.stream()
                .filter(m -> {
                    List<Contract> contracts = contractRepository.findByMemberId(m.getId());
                    return contracts.stream().anyMatch(c ->
                            c.getEndDate() != null && !c.getEndDate().isAfter(deadline));
                })
                .collect(Collectors.toList());
    }
}
