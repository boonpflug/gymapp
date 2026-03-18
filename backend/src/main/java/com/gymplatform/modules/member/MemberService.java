package com.gymplatform.modules.member;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.member.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberNoteRepository memberNoteRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AuditLogService auditLogService;

    private static final AtomicLong memberCounter = new AtomicLong(1000);

    @Transactional
    public MemberDto createMember(CreateMemberRequest request) {
        Member member = Member.builder()
                .memberNumber("MBR-" + memberCounter.incrementAndGet())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(new Address(
                        request.getStreet(), request.getCity(), request.getState(),
                        request.getPostalCode(), request.getCountry()))
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .healthNotes(request.getHealthNotes())
                .status(MemberStatus.ACTIVE)
                .joinDate(LocalDate.now())
                .tenantId(TenantContext.getTenantId())
                .build();

        member = memberRepository.save(member);

        auditLogService.log("Member", member.getId(), "CREATE", null, null, null);

        try {
            rabbitTemplate.convertAndSend("member.events", "member.created", toDto(member));
        } catch (Exception e) {
            log.warn("Failed to publish member.created event", e);
        }

        return toDto(member);
    }

    @Transactional
    public MemberDto updateMember(UUID id, UpdateMemberRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id));

        if (request.getFirstName() != null) member.setFirstName(request.getFirstName());
        if (request.getLastName() != null) member.setLastName(request.getLastName());
        if (request.getEmail() != null) member.setEmail(request.getEmail());
        if (request.getPhone() != null) member.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) member.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) member.setGender(request.getGender());
        if (request.getStreet() != null || request.getCity() != null) {
            Address addr = member.getAddress() != null ? member.getAddress() : new Address();
            if (request.getStreet() != null) addr.setStreet(request.getStreet());
            if (request.getCity() != null) addr.setCity(request.getCity());
            if (request.getState() != null) addr.setState(request.getState());
            if (request.getPostalCode() != null) addr.setPostalCode(request.getPostalCode());
            if (request.getCountry() != null) addr.setCountry(request.getCountry());
            member.setAddress(addr);
        }
        if (request.getEmergencyContactName() != null)
            member.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null)
            member.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getHealthNotes() != null) member.setHealthNotes(request.getHealthNotes());

        member = memberRepository.save(member);
        auditLogService.log("Member", member.getId(), "UPDATE", null, null, null);
        return toDto(member);
    }

    public MemberDto getMember(UUID id) {
        return toDto(memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id)));
    }

    public Page<MemberDto> searchMembers(String name, MemberStatus status, String email,
                                          Pageable pageable) {
        Specification<Member> spec = Specification.where(MemberSpecification.hasName(name))
                .and(MemberSpecification.hasStatus(status))
                .and(MemberSpecification.hasEmail(email));
        return memberRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional
    public void deactivateMember(UUID id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id));
        member.setStatus(MemberStatus.INACTIVE);
        memberRepository.save(member);
        auditLogService.log("Member", id, "DEACTIVATE", null, null, null);
    }

    @Transactional
    public MemberNoteDto addNote(UUID memberId, String content, UUID authorId) {
        if (!memberRepository.existsById(memberId)) {
            throw BusinessException.notFound("Member", memberId);
        }
        MemberNote note = MemberNote.builder()
                .memberId(memberId)
                .authorId(authorId)
                .content(content)
                .build();
        note = memberNoteRepository.save(note);
        return toNoteDto(note);
    }

    public List<MemberNoteDto> getNotes(UUID memberId) {
        return memberNoteRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(this::toNoteDto).toList();
    }

    private MemberDto toDto(Member m) {
        return MemberDto.builder()
                .id(m.getId())
                .memberNumber(m.getMemberNumber())
                .firstName(m.getFirstName())
                .lastName(m.getLastName())
                .email(m.getEmail())
                .phone(m.getPhone())
                .dateOfBirth(m.getDateOfBirth())
                .gender(m.getGender())
                .street(m.getAddress() != null ? m.getAddress().getStreet() : null)
                .city(m.getAddress() != null ? m.getAddress().getCity() : null)
                .state(m.getAddress() != null ? m.getAddress().getState() : null)
                .postalCode(m.getAddress() != null ? m.getAddress().getPostalCode() : null)
                .country(m.getAddress() != null ? m.getAddress().getCountry() : null)
                .emergencyContactName(m.getEmergencyContactName())
                .emergencyContactPhone(m.getEmergencyContactPhone())
                .profilePhotoUrl(m.getProfilePhotoUrl())
                .healthNotes(m.getHealthNotes())
                .status(m.getStatus())
                .joinDate(m.getJoinDate())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private MemberNoteDto toNoteDto(MemberNote n) {
        return MemberNoteDto.builder()
                .id(n.getId())
                .memberId(n.getMemberId())
                .authorId(n.getAuthorId())
                .content(n.getContent())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
