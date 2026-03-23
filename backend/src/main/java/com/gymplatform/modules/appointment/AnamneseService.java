package com.gymplatform.modules.appointment;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.appointment.dto.*;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnamneseService {

    private final AnamneseFormRepository anamneseFormRepository;
    private final AnamneseQuestionRepository anamneseQuestionRepository;
    private final AnamneseSubmissionRepository anamneseSubmissionRepository;
    private final AnamneseAnswerRepository anamneseAnswerRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;

    // ── Forms ──────────────────────────────────────────────────────────

    @Transactional
    public AnamneseFormDto createForm(CreateAnamneseFormRequest request, UUID userId) {
        AnamneseForm form = AnamneseForm.builder()
                .name(request.getName())
                .description(request.getDescription())
                .version(1)
                .active(true)
                .tenantId(TenantContext.getTenantId())
                .build();
        form = anamneseFormRepository.save(form);

        List<AnamneseQuestion> questions = createQuestions(form.getId(), request.getQuestions());

        auditLogService.log("AnamneseForm", form.getId(), "CREATED", userId, null, form.getName());
        log.info("Anamnese form created: {} by user {}", form.getName(), userId);

        return toFormDto(form, questions);
    }

    @Transactional(readOnly = true)
    public List<AnamneseFormDto> listForms() {
        return anamneseFormRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(form -> {
                    List<AnamneseQuestion> questions = anamneseQuestionRepository
                            .findByFormIdOrderBySortOrderAsc(form.getId());
                    return toFormDto(form, questions);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnamneseFormDto getForm(UUID id) {
        AnamneseForm form = anamneseFormRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AnamneseForm", id));

        List<AnamneseQuestion> questions = anamneseQuestionRepository
                .findByFormIdOrderBySortOrderAsc(form.getId());

        return toFormDto(form, questions);
    }

    @Transactional
    public AnamneseFormDto updateForm(UUID id, CreateAnamneseFormRequest request, UUID userId) {
        AnamneseForm form = anamneseFormRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AnamneseForm", id));

        String oldName = form.getName();
        form.setName(request.getName());
        form.setDescription(request.getDescription());
        form.setVersion(form.getVersion() + 1);
        form = anamneseFormRepository.save(form);

        // Delete old questions and insert new ones
        anamneseQuestionRepository.deleteByFormId(form.getId());
        List<AnamneseQuestion> questions = createQuestions(form.getId(), request.getQuestions());

        auditLogService.log("AnamneseForm", form.getId(), "UPDATED", userId, oldName, form.getName());
        log.info("Anamnese form updated: {} by user {}", form.getName(), userId);

        return toFormDto(form, questions);
    }

    @Transactional
    public void deactivateForm(UUID id, UUID userId) {
        AnamneseForm form = anamneseFormRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AnamneseForm", id));

        form.setActive(false);
        anamneseFormRepository.save(form);

        auditLogService.log("AnamneseForm", form.getId(), "DEACTIVATED", userId, "active", "inactive");
        log.info("Anamnese form deactivated: {} by user {}", form.getName(), userId);
    }

    // ── Submissions ────────────────────────────────────────────────────

    @Transactional
    public AnamneseSubmissionDto submitAnamnese(SubmitAnamneseRequest request, UUID userId) {
        // Validate form exists
        AnamneseForm form = anamneseFormRepository.findById(request.getFormId())
                .orElseThrow(() -> BusinessException.notFound("AnamneseForm", request.getFormId()));

        // Validate member exists
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> BusinessException.notFound("Member", request.getMemberId()));

        // Create submission
        AnamneseSubmission submission = AnamneseSubmission.builder()
                .formId(request.getFormId())
                .memberId(request.getMemberId())
                .appointmentId(request.getAppointmentId())
                .submittedBy(userId)
                .submittedAt(Instant.now())
                .notes(request.getNotes())
                .tenantId(TenantContext.getTenantId())
                .build();
        submission = anamneseSubmissionRepository.save(submission);

        // Create answers
        List<AnamneseAnswer> answers = createAnswers(submission.getId(), request.getAnswers());

        // Load questions for DTO mapping
        Map<UUID, AnamneseQuestion> questionMap = anamneseQuestionRepository
                .findByFormIdOrderBySortOrderAsc(form.getId())
                .stream()
                .collect(Collectors.toMap(AnamneseQuestion::getId, Function.identity()));

        auditLogService.log("AnamneseSubmission", submission.getId(), "SUBMITTED", userId, null,
                "Form: " + form.getName() + ", Member: " + member.getFirstName() + " " + member.getLastName());
        log.info("Anamnese submitted for form {} member {} by user {}", form.getName(), request.getMemberId(), userId);

        return toSubmissionDto(submission, form, member, answers, questionMap);
    }

    @Transactional(readOnly = true)
    public AnamneseSubmissionDto getSubmission(UUID id) {
        AnamneseSubmission submission = anamneseSubmissionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("AnamneseSubmission", id));

        AnamneseForm form = anamneseFormRepository.findById(submission.getFormId())
                .orElse(null);

        Member member = memberRepository.findById(submission.getMemberId())
                .orElse(null);

        List<AnamneseAnswer> answers = anamneseAnswerRepository.findBySubmissionId(submission.getId());

        Map<UUID, AnamneseQuestion> questionMap = anamneseQuestionRepository
                .findByFormIdOrderBySortOrderAsc(submission.getFormId())
                .stream()
                .collect(Collectors.toMap(AnamneseQuestion::getId, Function.identity()));

        return toSubmissionDto(submission, form, member, answers, questionMap);
    }

    @Transactional(readOnly = true)
    public Page<AnamneseSubmissionDto> getMemberSubmissions(UUID memberId, Pageable pageable) {
        return anamneseSubmissionRepository.findByMemberIdOrderBySubmittedAtDesc(memberId, pageable)
                .map(this::toSubmissionDtoSummary);
    }

    @Transactional(readOnly = true)
    public Page<AnamneseSubmissionDto> getFormSubmissions(UUID formId, Pageable pageable) {
        return anamneseSubmissionRepository.findByFormIdOrderBySubmittedAtDesc(formId, pageable)
                .map(this::toSubmissionDtoSummary);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private List<AnamneseQuestion> createQuestions(UUID formId, List<CreateQuestionRequest> requests) {
        AtomicInteger order = new AtomicInteger(0);
        List<AnamneseQuestion> questions = requests.stream()
                .map(req -> {
                    int sortOrder = req.getSortOrder() > 0 ? req.getSortOrder() : order.incrementAndGet();
                    return AnamneseQuestion.builder()
                            .formId(formId)
                            .questionText(req.getQuestionText())
                            .questionType(req.getQuestionType())
                            .options(req.getOptions())
                            .required(req.isRequired())
                            .sortOrder(sortOrder)
                            .section(req.getSection())
                            .tenantId(TenantContext.getTenantId())
                            .build();
                })
                .collect(Collectors.toList());
        return anamneseQuestionRepository.saveAll(questions);
    }

    private List<AnamneseAnswer> createAnswers(UUID submissionId, List<AnswerRequest> requests) {
        List<AnamneseAnswer> answers = requests.stream()
                .map(req -> AnamneseAnswer.builder()
                        .submissionId(submissionId)
                        .questionId(req.getQuestionId())
                        .answerText(req.getAnswerText())
                        .answerNumber(req.getAnswerNumber())
                        .answerBoolean(req.getAnswerBoolean())
                        .tenantId(TenantContext.getTenantId())
                        .build())
                .collect(Collectors.toList());
        return anamneseAnswerRepository.saveAll(answers);
    }

    private AnamneseFormDto toFormDto(AnamneseForm form, List<AnamneseQuestion> questions) {
        List<AnamneseQuestionDto> questionDtos = questions.stream()
                .map(this::toQuestionDto)
                .collect(Collectors.toList());

        return AnamneseFormDto.builder()
                .id(form.getId())
                .name(form.getName())
                .description(form.getDescription())
                .version(form.getVersion())
                .active(form.isActive())
                .questions(questionDtos)
                .createdAt(form.getCreatedAt())
                .build();
    }

    private AnamneseQuestionDto toQuestionDto(AnamneseQuestion question) {
        return AnamneseQuestionDto.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .options(question.getOptions())
                .required(question.isRequired())
                .sortOrder(question.getSortOrder())
                .section(question.getSection())
                .build();
    }

    private AnamneseSubmissionDto toSubmissionDto(AnamneseSubmission submission, AnamneseForm form,
                                                   Member member, List<AnamneseAnswer> answers,
                                                   Map<UUID, AnamneseQuestion> questionMap) {
        List<AnamneseAnswerDto> answerDtos = answers.stream()
                .map(answer -> {
                    AnamneseQuestion question = questionMap.get(answer.getQuestionId());
                    return AnamneseAnswerDto.builder()
                            .id(answer.getId())
                            .questionId(answer.getQuestionId())
                            .questionText(question != null ? question.getQuestionText() : null)
                            .answerText(answer.getAnswerText())
                            .answerNumber(answer.getAnswerNumber())
                            .answerBoolean(answer.getAnswerBoolean())
                            .build();
                })
                .collect(Collectors.toList());

        return AnamneseSubmissionDto.builder()
                .id(submission.getId())
                .formId(submission.getFormId())
                .formName(form != null ? form.getName() : null)
                .memberId(submission.getMemberId())
                .memberName(member != null ? member.getFirstName() + " " + member.getLastName() : null)
                .appointmentId(submission.getAppointmentId())
                .submittedBy(submission.getSubmittedBy())
                .submittedAt(submission.getSubmittedAt())
                .notes(submission.getNotes())
                .answers(answerDtos)
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private AnamneseSubmissionDto toSubmissionDtoSummary(AnamneseSubmission submission) {
        String formName = anamneseFormRepository.findById(submission.getFormId())
                .map(AnamneseForm::getName)
                .orElse(null);

        String memberName = memberRepository.findById(submission.getMemberId())
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);

        List<AnamneseAnswer> answers = anamneseAnswerRepository.findBySubmissionId(submission.getId());

        Map<UUID, AnamneseQuestion> questionMap = anamneseQuestionRepository
                .findByFormIdOrderBySortOrderAsc(submission.getFormId())
                .stream()
                .collect(Collectors.toMap(AnamneseQuestion::getId, Function.identity()));

        return toSubmissionDto(submission, null, null, answers, questionMap).toBuilder()
                .formName(formName)
                .memberName(memberName)
                .build();
    }
}
