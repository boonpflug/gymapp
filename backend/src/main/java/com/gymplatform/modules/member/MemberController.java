package com.gymplatform.modules.member;

import com.gymplatform.modules.member.dto.*;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'TRAINER', 'RECEPTIONIST')")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<MemberDto>> createMember(
            @Valid @RequestBody CreateMemberRequest request) {
        MemberDto member = memberService.createMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(member));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberDto>>> searchMembers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) String email,
            Pageable pageable) {
        Page<MemberDto> page = memberService.searchMembers(name, status, email, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(), PageMeta.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberDto>> getMember(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<MemberDto>> updateMember(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deactivateMember(@PathVariable UUID id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<MemberNoteDto>> addNote(
            @PathVariable UUID id,
            @RequestBody String content,
            Authentication authentication) {
        UUID authorId = (UUID) authentication.getPrincipal();
        MemberNoteDto note = memberService.addNote(id, content, authorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(note));
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<MemberNoteDto>>> getNotes(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getNotes(id)));
    }
}
