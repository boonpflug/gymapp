package com.gymplatform.modules.marketing;

import com.gymplatform.modules.checkin.CheckIn;
import com.gymplatform.modules.checkin.CheckInRepository;
import com.gymplatform.modules.contract.Contract;
import com.gymplatform.modules.contract.ContractRepository;
import com.gymplatform.modules.contract.ContractStatus;
import com.gymplatform.modules.marketing.dto.AtRiskMemberDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.member.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtRiskDetectionService {

    private final MemberRepository memberRepository;
    private final CheckInRepository checkInRepository;
    private final ContractRepository contractRepository;

    public List<AtRiskMemberDto> detectAtRiskMembers(int inactiveDaysThreshold) {
        Specification<Member> activeSpec = (root, query, cb) ->
                cb.equal(root.get("status"), MemberStatus.ACTIVE);
        List<Member> activeMembers = memberRepository.findAll(activeSpec);

        List<AtRiskMemberDto> atRiskMembers = new ArrayList<>();
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant sixtyDaysAgo = now.minus(60, ChronoUnit.DAYS);

        for (Member member : activeMembers) {
            var recentCheckins = checkInRepository.findByMemberIdOrderByCheckInTimeDesc(
                    member.getId(), PageRequest.of(0, 100));

            if (recentCheckins.isEmpty()) {
                atRiskMembers.add(buildAtRiskDto(member, null, 0, 0, "HIGH",
                        "No check-ins recorded"));
                continue;
            }

            CheckIn lastCheckIn = recentCheckins.getContent().get(0);
            long daysSinceLast = ChronoUnit.DAYS.between(lastCheckIn.getCheckInTime(), now);

            long visitsLast30 = recentCheckins.getContent().stream()
                    .filter(c -> c.getCheckInTime().isAfter(thirtyDaysAgo))
                    .count();
            long visitsPrev30 = recentCheckins.getContent().stream()
                    .filter(c -> c.getCheckInTime().isAfter(sixtyDaysAgo)
                            && c.getCheckInTime().isBefore(thirtyDaysAgo))
                    .count();

            double avgWeeklyVisits = visitsLast30 / 4.3;
            double visitTrend = visitsPrev30 > 0
                    ? ((double) visitsLast30 - visitsPrev30) / visitsPrev30 * 100
                    : visitsLast30 > 0 ? 100 : -100;

            if (daysSinceLast >= inactiveDaysThreshold) {
                String riskLevel;
                String reason;
                if (daysSinceLast >= inactiveDaysThreshold * 2) {
                    riskLevel = "HIGH";
                    reason = "No visit in " + daysSinceLast + " days";
                } else {
                    riskLevel = "MEDIUM";
                    reason = "No visit in " + daysSinceLast + " days";
                }
                atRiskMembers.add(buildAtRiskDto(member, lastCheckIn.getCheckInTime(),
                        avgWeeklyVisits, visitTrend, riskLevel, reason));
            } else if (visitTrend < -50 && visitsLast30 > 0) {
                atRiskMembers.add(buildAtRiskDto(member, lastCheckIn.getCheckInTime(),
                        avgWeeklyVisits, visitTrend, "MEDIUM",
                        "Visit frequency dropped " + Math.abs((int) visitTrend) + "%"));
            } else if (visitTrend < -30 && visitsLast30 > 0) {
                atRiskMembers.add(buildAtRiskDto(member, lastCheckIn.getCheckInTime(),
                        avgWeeklyVisits, visitTrend, "LOW",
                        "Visit frequency declining"));
            }
        }

        atRiskMembers.sort(Comparator.comparing(AtRiskMemberDto::getRiskLevel)
                .thenComparing(AtRiskMemberDto::getDaysSinceLastCheckIn, Comparator.reverseOrder()));

        return atRiskMembers;
    }

    public Map<String, Long> getAtRiskSummary(int inactiveDaysThreshold) {
        List<AtRiskMemberDto> atRisk = detectAtRiskMembers(inactiveDaysThreshold);
        return atRisk.stream()
                .collect(Collectors.groupingBy(AtRiskMemberDto::getRiskLevel, Collectors.counting()));
    }

    private AtRiskMemberDto buildAtRiskDto(Member member, Instant lastCheckIn,
                                            double avgWeeklyVisits, double visitTrend,
                                            String riskLevel, String riskReason) {
        List<Contract> contracts = contractRepository.findByMemberIdAndStatus(
                member.getId(), ContractStatus.ACTIVE);
        String contractStatus = contracts.isEmpty() ? "NONE" : "ACTIVE";

        long daysSinceLast = lastCheckIn != null
                ? ChronoUnit.DAYS.between(lastCheckIn, Instant.now())
                : 999;

        return AtRiskMemberDto.builder()
                .memberId(member.getId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .memberStatus(member.getStatus().name())
                .contractStatus(contractStatus)
                .lastCheckIn(lastCheckIn)
                .daysSinceLastCheckIn((int) daysSinceLast)
                .avgWeeklyVisits(Math.round(avgWeeklyVisits * 10) / 10.0)
                .visitTrend(Math.round(visitTrend * 10) / 10.0)
                .riskLevel(riskLevel)
                .riskReason(riskReason)
                .build();
    }
}
