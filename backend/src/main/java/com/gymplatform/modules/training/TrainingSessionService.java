package com.gymplatform.modules.training;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.training.dto.*;
import com.gymplatform.shared.AuditLogService;
import com.gymplatform.shared.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingSessionService {

    private final TrainingSessionRepository sessionRepository;
    private final TrainingLogRepository logRepository;
    private final TrainingPlanRepository planRepository;
    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public TrainingSessionDto startSession(StartSessionRequest req, UUID userId) {
        if (req.getPlanId() != null) {
            planRepository.findById(req.getPlanId())
                    .orElseThrow(() -> BusinessException.notFound("TrainingPlan", req.getPlanId()));
        }

        TrainingSession session = TrainingSession.builder()
                .memberId(req.getMemberId())
                .planId(req.getPlanId())
                .startedAt(Instant.now())
                .tenantId(TenantContext.getTenantId())
                .build();
        session = sessionRepository.save(session);

        auditLogService.log("TrainingSession", session.getId(), "START", userId, null, null);
        return toSessionDto(session, false);
    }

    @Transactional
    public TrainingSessionDto finishSession(UUID sessionId, FinishSessionRequest req, UUID userId) {
        TrainingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound("TrainingSession", sessionId));

        if (session.getFinishedAt() != null) {
            throw BusinessException.badRequest("Session is already finished");
        }

        Instant now = Instant.now();
        session.setFinishedAt(now);
        session.setDurationMinutes((int) Duration.between(session.getStartedAt(), now).toMinutes());
        if (req.getNotes() != null) session.setNotes(req.getNotes());
        if (req.getRating() != null) session.setRating(req.getRating());
        session = sessionRepository.save(session);

        auditLogService.log("TrainingSession", session.getId(), "FINISH", userId, null, null);
        publishSessionEvent(session, "training.session.completed");

        return toSessionDto(session, true);
    }

    @Transactional
    public TrainingLogDto logExercise(LogExerciseRequest req, UUID userId) {
        TrainingSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> BusinessException.notFound("TrainingSession", req.getSessionId()));

        if (session.getFinishedAt() != null) {
            throw BusinessException.badRequest("Cannot log exercises to a finished session");
        }

        exerciseRepository.findById(req.getExerciseId())
                .orElseThrow(() -> BusinessException.notFound("Exercise", req.getExerciseId()));

        TrainingLog trainingLog = TrainingLog.builder()
                .sessionId(req.getSessionId())
                .exerciseId(req.getExerciseId())
                .planExerciseId(req.getPlanExerciseId())
                .setNumber(req.getSetNumber())
                .targetReps(req.getTargetReps())
                .actualReps(req.getActualReps())
                .targetWeight(req.getTargetWeight())
                .actualWeight(req.getActualWeight())
                .durationSeconds(req.getDurationSeconds())
                .notes(req.getNotes())
                .completed(req.isCompleted())
                .tenantId(TenantContext.getTenantId())
                .build();
        trainingLog = logRepository.save(trainingLog);

        return toLogDto(trainingLog);
    }

    public TrainingSessionDto getById(UUID sessionId) {
        TrainingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> BusinessException.notFound("TrainingSession", sessionId));
        return toSessionDto(session, true);
    }

    public Page<TrainingSessionDto> getMemberSessions(UUID memberId, Pageable pageable) {
        return sessionRepository.findByMemberIdOrderByStartedAtDesc(memberId, pageable)
                .map(s -> toSessionDto(s, false));
    }

    public List<TrainingLogDto> getExerciseHistory(UUID memberId, UUID exerciseId) {
        return logRepository.findMemberExerciseHistory(memberId, exerciseId)
                .stream().map(this::toLogDto).collect(Collectors.toList());
    }

    public long getSessionCount(UUID memberId, Instant since) {
        return sessionRepository.countSessionsSince(memberId, since);
    }

    private void publishSessionEvent(TrainingSession session, String routingKey) {
        try {
            Map<String, Object> event = Map.of(
                    "sessionId", session.getId().toString(),
                    "memberId", session.getMemberId().toString(),
                    "durationMinutes", session.getDurationMinutes() != null ? session.getDurationMinutes() : 0,
                    "tenantId", TenantContext.getTenantId(),
                    "timestamp", Instant.now().toString()
            );
            rabbitTemplate.convertAndSend("member.events", routingKey, event);
        } catch (Exception e) {
            log.warn("Failed to publish session event: {}", e.getMessage());
        }
    }

    private TrainingSessionDto toSessionDto(TrainingSession s, boolean includeLogs) {
        String memberName = getMemberName(s.getMemberId());
        String planName = s.getPlanId() != null ?
                planRepository.findById(s.getPlanId()).map(TrainingPlan::getName).orElse(null) : null;

        List<TrainingLogDto> logs = null;
        if (includeLogs) {
            logs = logRepository.findBySessionIdOrderBySetNumberAsc(s.getId())
                    .stream().map(this::toLogDto).collect(Collectors.toList());
        }

        return TrainingSessionDto.builder()
                .id(s.getId())
                .memberId(s.getMemberId())
                .memberName(memberName)
                .planId(s.getPlanId())
                .planName(planName)
                .startedAt(s.getStartedAt())
                .finishedAt(s.getFinishedAt())
                .durationMinutes(s.getDurationMinutes())
                .notes(s.getNotes())
                .rating(s.getRating())
                .logs(logs)
                .createdAt(s.getCreatedAt())
                .build();
    }

    private TrainingLogDto toLogDto(TrainingLog l) {
        String exerciseName = exerciseRepository.findById(l.getExerciseId())
                .map(Exercise::getName).orElse(null);

        return TrainingLogDto.builder()
                .id(l.getId())
                .sessionId(l.getSessionId())
                .exerciseId(l.getExerciseId())
                .exerciseName(exerciseName)
                .planExerciseId(l.getPlanExerciseId())
                .setNumber(l.getSetNumber())
                .targetReps(l.getTargetReps())
                .actualReps(l.getActualReps())
                .targetWeight(l.getTargetWeight())
                .actualWeight(l.getActualWeight())
                .durationSeconds(l.getDurationSeconds())
                .notes(l.getNotes())
                .completed(l.isCompleted())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private String getMemberName(UUID memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId)
                .map(m -> m.getFirstName() + " " + m.getLastName())
                .orElse(null);
    }
}
