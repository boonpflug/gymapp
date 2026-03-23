package com.gymplatform.modules.appointment;

import com.gymplatform.modules.appointment.dto.*;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/anamnese")
@RequiredArgsConstructor
public class AnamneseController {

    private final AnamneseService anamneseService;

    // ── Forms ───────────────────────────────────────────────────────────

    @GetMapping("/forms")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<List<AnamneseFormDto>>> listForms() {
        return ResponseEntity.ok(ApiResponse.success(anamneseService.listForms()));
    }

    @PostMapping("/forms")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnamneseFormDto>> createForm(
            @Valid @RequestBody CreateAnamneseFormRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(anamneseService.createForm(req, userId)));
    }

    @GetMapping("/forms/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AnamneseFormDto>> getForm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(anamneseService.getForm(id)));
    }

    @PutMapping("/forms/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnamneseFormDto>> updateForm(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAnamneseFormRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(anamneseService.updateForm(id, req, userId)));
    }

    @DeleteMapping("/forms/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateForm(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        anamneseService.deactivateForm(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Submissions ─────────────────────────────────────────────────────

    @PostMapping("/submissions")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AnamneseSubmissionDto>> submitAnamnese(
            @Valid @RequestBody SubmitAnamneseRequest req,
            @AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return ResponseEntity.ok(ApiResponse.success(anamneseService.submitAnamnese(req, userId)));
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER')")
    public ResponseEntity<ApiResponse<AnamneseSubmissionDto>> getSubmission(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(anamneseService.getSubmission(id)));
    }

    @GetMapping("/submissions/member/{memberId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'MEMBER')")
    public ResponseEntity<ApiResponse<List<AnamneseSubmissionDto>>> getMemberSubmissions(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AnamneseSubmissionDto> result = anamneseService.getMemberSubmissions(memberId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }

    @GetMapping("/submissions/form/{formId}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AnamneseSubmissionDto>>> getFormSubmissions(
            @PathVariable UUID formId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AnamneseSubmissionDto> result = anamneseService.getFormSubmissions(formId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), PageMeta.from(result)));
    }
}
