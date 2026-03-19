package com.gymplatform.modules.marketing;

import com.gymplatform.modules.marketing.dto.AtRiskMemberDto;
import com.gymplatform.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing/at-risk")
@RequiredArgsConstructor
public class AtRiskController {

    private final AtRiskDetectionService atRiskDetectionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AtRiskMemberDto>>> getAtRiskMembers(
            @RequestParam(defaultValue = "14") int inactiveDays) {
        return ResponseEntity.ok(ApiResponse.success(
                atRiskDetectionService.detectAtRiskMembers(inactiveDays)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('STUDIO_OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSummary(
            @RequestParam(defaultValue = "14") int inactiveDays) {
        return ResponseEntity.ok(ApiResponse.success(
                atRiskDetectionService.getAtRiskSummary(inactiveDays)));
    }
}
