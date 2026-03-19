package com.gymplatform.modules.portal;

import com.gymplatform.modules.checkin.CheckIn;
import com.gymplatform.modules.checkin.CheckInRepository;
import com.gymplatform.modules.checkin.CheckInService;
import com.gymplatform.modules.checkin.OccupancyService;
import com.gymplatform.modules.checkin.dto.CheckInDto;
import com.gymplatform.modules.checkin.dto.CheckOutRequest;
import com.gymplatform.modules.checkin.dto.ManualCheckInRequest;
import com.gymplatform.modules.contract.ContractService;
import com.gymplatform.modules.contract.dto.CancelContractRequest;
import com.gymplatform.modules.contract.dto.ContractDto;
import com.gymplatform.modules.finance.InvoiceService;
import com.gymplatform.modules.finance.dto.InvoiceDto;
import com.gymplatform.modules.member.Member;
import com.gymplatform.modules.member.MemberRepository;
import com.gymplatform.modules.member.MemberService;
import com.gymplatform.modules.member.dto.MemberDto;
import com.gymplatform.modules.member.dto.UpdateMemberRequest;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.BusinessException;
import com.gymplatform.shared.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('MEMBER')")
@RequiredArgsConstructor
public class MemberPortalController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final CheckInRepository checkInRepository;
    private final CheckInService checkInService;
    private final OccupancyService occupancyService;

    // --- Profile ---

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MemberDto>> getProfile(@AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(member.getId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<MemberDto>> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdateMemberRequest request) {
        Member member = resolveCurrentMember(user);
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMember(member.getId(), request)));
    }

    // --- Contracts ---

    @GetMapping("/contracts")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getContracts(
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        return ResponseEntity.ok(ApiResponse.success(contractService.getContractsByMember(member.getId())));
    }

    @PostMapping("/contracts/{contractId}/cancel")
    public ResponseEntity<ApiResponse<ContractDto>> cancelContract(
            @PathVariable UUID contractId,
            @RequestBody CancelContractRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        ContractDto contract = contractService.getContract(contractId);
        if (!contract.getMemberId().equals(member.getId())) {
            throw BusinessException.badRequest("You can only cancel your own contracts");
        }
        return ResponseEntity.ok(ApiResponse.success(contractService.requestCancellation(contractId, request)));
    }

    @PostMapping("/contracts/{contractId}/withdraw-cancellation")
    public ResponseEntity<ApiResponse<ContractDto>> withdrawCancellation(
            @PathVariable UUID contractId,
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        ContractDto contract = contractService.getContract(contractId);
        if (!contract.getMemberId().equals(member.getId())) {
            throw BusinessException.badRequest("You can only modify your own contracts");
        }
        return ResponseEntity.ok(ApiResponse.success(contractService.withdrawCancellation(contractId)));
    }

    // --- Invoices ---

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getInvoices(
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByMember(member.getId())));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        InvoiceDto invoice = invoiceService.getInvoice(invoiceId);
        if (!invoice.getMemberId().equals(member.getId())) {
            throw BusinessException.badRequest("You can only view your own invoices");
        }
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    // --- Self Check-in / Check-out ---

    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<CheckInDto>> selfCheckIn(
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        ManualCheckInRequest request = new ManualCheckInRequest();
        request.setMemberId(member.getId());
        CheckInDto result = checkInService.manualCheckIn(request, null);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Void>> selfCheckOut(
            @AuthenticationPrincipal UserDetails user) {
        Member member = resolveCurrentMember(user);
        CheckOutRequest request = new CheckOutRequest();
        request.setMemberId(member.getId());
        checkInService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- Check-in history ---

    @GetMapping("/checkins")
    public ResponseEntity<ApiResponse<List<CheckInDto>>> getCheckInHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Member member = resolveCurrentMember(user);
        Page<CheckIn> result = checkInRepository.findByMemberIdOrderByCheckInTimeDesc(
                member.getId(), PageRequest.of(page, size));
        List<CheckInDto> dtos = result.getContent().stream().map(this::toCheckInDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos, PageMeta.from(result)));
    }

    // --- Occupancy ---

    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<Object>> getOccupancy() {
        return ResponseEntity.ok(ApiResponse.success(occupancyService.getCurrentOccupancy(null)));
    }

    // --- Helper ---

    private Member resolveCurrentMember(UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        return memberRepository.findByUserId(userId)
                .orElseThrow(() -> BusinessException.badRequest("No member profile linked to this account"));
    }

    private CheckInDto toCheckInDto(CheckIn c) {
        return CheckInDto.builder()
                .id(c.getId())
                .memberId(c.getMemberId())
                .deviceId(c.getDeviceId())
                .method(c.getMethod())
                .status(c.getStatus())
                .denialReason(c.getDenialReason())
                .staffId(c.getStaffId())
                .checkInTime(c.getCheckInTime())
                .checkOutTime(c.getCheckOutTime())
                .build();
    }
}
