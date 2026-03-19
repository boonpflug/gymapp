package com.gymplatform.modules.portal;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.modules.booking.ClassBookingService;
import com.gymplatform.modules.booking.ClassCategoryRepository;
import com.gymplatform.modules.booking.ClassScheduleService;
import com.gymplatform.modules.booking.dto.ClassBookingDto;
import com.gymplatform.modules.booking.dto.ClassCategoryDto;
import com.gymplatform.modules.booking.dto.ClassScheduleDto;
import com.gymplatform.modules.booking.dto.CreateBookingRequest;
import com.gymplatform.modules.facility.Facility;
import com.gymplatform.modules.facility.FacilityRepository;
import com.gymplatform.modules.portal.dto.*;
import com.gymplatform.modules.sales.Lead;
import com.gymplatform.modules.sales.LeadRepository;
import com.gymplatform.modules.sales.LeadSource;
import com.gymplatform.modules.sales.LeadStageRepository;
import com.gymplatform.modules.tenant.Tenant;
import com.gymplatform.modules.tenant.TenantRepository;
import com.gymplatform.shared.ApiResponse;
import com.gymplatform.shared.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPortalController {

    private final TenantRepository tenantRepository;
    private final FacilityRepository facilityRepository;
    private final ClassScheduleService scheduleService;
    private final ClassCategoryRepository categoryRepository;
    private final ClassBookingService bookingService;
    private final LeadRepository leadRepository;
    private final LeadStageRepository stageRepository;

    // --- Studio Profile ---

    @GetMapping("/{tenantSlug}/profile")
    public ResponseEntity<ApiResponse<StudioProfileDto>> getStudioProfile(
            @PathVariable String tenantSlug) {
        setTenantContext(tenantSlug);
        try {
            List<Facility> facilities = facilityRepository.findByActiveTrueOrderByNameAsc();
            Facility primary = facilities.isEmpty() ? null : facilities.get(0);

            StudioProfileDto profile = StudioProfileDto.builder()
                    .name(primary != null ? primary.getName() : tenantSlug)
                    .description(primary != null ? primary.getDescription() : null)
                    .street(primary != null ? primary.getStreet() : null)
                    .city(primary != null ? primary.getCity() : null)
                    .state(primary != null ? primary.getState() : null)
                    .postalCode(primary != null ? primary.getPostalCode() : null)
                    .country(primary != null ? primary.getCountry() : null)
                    .phone(primary != null ? primary.getPhone() : null)
                    .email(primary != null ? primary.getEmail() : null)
                    .websiteUrl(primary != null ? primary.getWebsiteUrl() : null)
                    .openingHours(primary != null ? primary.getOpeningHours() : null)
                    .logoUrl(primary != null ? primary.getLogoUrl() : null)
                    .brandColor(primary != null ? primary.getBrandColor() : null)
                    .bannerImageUrl(primary != null ? primary.getBannerImageUrl() : null)
                    .facilityCount(facilities.size())
                    .facilities(facilities.stream().map(f -> StudioProfileDto.FacilitySummary.builder()
                            .id(f.getId())
                            .name(f.getName())
                            .city(f.getCity())
                            .street(f.getStreet())
                            .phone(f.getPhone())
                            .openingHours(f.getOpeningHours())
                            .build()).toList())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(profile));
        } finally {
            TenantContext.clear();
        }
    }

    // --- Class Schedule (public browsing) ---

    @GetMapping("/{tenantSlug}/classes/categories")
    public ResponseEntity<ApiResponse<List<ClassCategoryDto>>> getCategories(
            @PathVariable String tenantSlug) {
        setTenantContext(tenantSlug);
        try {
            List<ClassCategoryDto> categories = categoryRepository.findByActiveTrueOrderByNameAsc()
                    .stream().map(c -> ClassCategoryDto.builder()
                            .id(c.getId()).name(c.getName())
                            .description(c.getDescription()).color(c.getColor())
                            .active(c.isActive()).createdAt(c.getCreatedAt())
                            .build()).toList();
            return ResponseEntity.ok(ApiResponse.success(categories));
        } finally {
            TenantContext.clear();
        }
    }

    @GetMapping("/{tenantSlug}/classes/schedule")
    public ResponseEntity<ApiResponse<List<ClassScheduleDto>>> getPublicSchedule(
            @PathVariable String tenantSlug,
            @RequestParam Instant weekStart) {
        setTenantContext(tenantSlug);
        try {
            return ResponseEntity.ok(ApiResponse.success(scheduleService.getWeeklySchedule(weekStart)));
        } finally {
            TenantContext.clear();
        }
    }

    // --- Public Trial Booking ---

    @PostMapping("/{tenantSlug}/booking/trial")
    public ResponseEntity<ApiResponse<ClassBookingDto>> bookTrial(
            @PathVariable String tenantSlug,
            @Valid @RequestBody PublicTrialBookingRequest req) {
        setTenantContext(tenantSlug);
        try {
            // Create lead from the booking
            createLeadFromBooking(req);

            // Book as guest
            CreateBookingRequest bookingReq = new CreateBookingRequest();
            bookingReq.setScheduleId(req.getScheduleId());
            bookingReq.setGuestName(req.getFirstName() + " " + req.getLastName());
            bookingReq.setGuestEmail(req.getEmail());
            bookingReq.setGuestPhone(req.getPhone());

            ClassBookingDto booking = bookingService.book(bookingReq, null);
            return ResponseEntity.ok(ApiResponse.success(booking));
        } finally {
            TenantContext.clear();
        }
    }

    // --- Lead Capture (contact form / interest form) ---

    @PostMapping("/{tenantSlug}/contact")
    public ResponseEntity<ApiResponse<Void>> submitContact(
            @PathVariable String tenantSlug,
            @Valid @RequestBody PublicContactRequest req) {
        setTenantContext(tenantSlug);
        try {
            var defaultStage = stageRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> BusinessException.badRequest("No default lead stage configured"));

            leadRepository.save(Lead.builder()
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .email(req.getEmail())
                    .phone(req.getPhone())
                    .source(LeadSource.WEBSITE)
                    .interest(req.getInterest())
                    .stageId(defaultStage.getId())
                    .notes(req.getMessage())
                    .tenantId(TenantContext.getTenantId())
                    .build());

            return ResponseEntity.ok(ApiResponse.success(null));
        } finally {
            TenantContext.clear();
        }
    }

    // --- Helpers ---

    private void setTenantContext(String tenantSlug) {
        String schemaName = "tenant_" + tenantSlug.replace("-", "_");
        tenantRepository.findBySchemaName(schemaName)
                .orElseThrow(() -> BusinessException.notFound("Studio", tenantSlug));
        TenantContext.setTenantId(schemaName);
    }

    private void createLeadFromBooking(PublicTrialBookingRequest req) {
        var defaultStage = stageRepository.findByIsDefaultTrue().orElse(null);
        if (defaultStage == null) return;

        // Only create lead if one doesn't already exist with this email
        if (leadRepository.findByEmail(req.getEmail()).isEmpty()) {
            leadRepository.save(Lead.builder()
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .email(req.getEmail())
                    .phone(req.getPhone())
                    .source(LeadSource.WEBSITE)
                    .interest("Trial booking")
                    .stageId(defaultStage.getId())
                    .tenantId(TenantContext.getTenantId())
                    .build());
        }
    }
}
