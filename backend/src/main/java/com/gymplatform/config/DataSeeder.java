package com.gymplatform.config;

import com.gymplatform.config.multitenancy.TenantContext;
import com.gymplatform.config.multitenancy.TenantProvisioningService;
import com.gymplatform.modules.auth.Role;
import com.gymplatform.modules.auth.User;
import com.gymplatform.modules.auth.UserRepository;
import com.gymplatform.modules.booking.*;
import com.gymplatform.modules.checkin.*;
import com.gymplatform.modules.communication.*;
import com.gymplatform.modules.contract.*;
import com.gymplatform.modules.facility.*;
import com.gymplatform.modules.finance.*;
import com.gymplatform.modules.member.*;
import com.gymplatform.modules.sales.*;
import com.gymplatform.modules.staff.*;
import com.gymplatform.modules.tenant.Tenant;
import com.gymplatform.modules.tenant.TenantRepository;
import com.gymplatform.modules.training.*;
import com.gymplatform.modules.loyalty.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    // Tenant-schema repositories (used after setting TenantContext)
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository tierRepository;
    private final ContractRepository contractRepository;
    private final FacilityRepository facilityRepository;
    private final MemberFacilityAccessRepository memberFacilityAccessRepository;
    private final CheckInRepository checkInRepository;
    private final AccessDeviceRepository deviceRepository;
    private final ClassCategoryRepository categoryRepository;
    private final ClassDefinitionRepository classDefRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final ExerciseRepository exerciseRepository;
    private final TrainingPlanRepository planRepository;
    private final TrainingPlanExerciseRepository planExerciseRepository;
    private final LeadStageRepository stageRepository;
    private final LeadRepository leadRepository;
    private final CommunicationTemplateRepository templateRepository;
    private final NotificationRuleRepository ruleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeFacilityRepository empFacilityRepository;
    private final InvoiceRepository invoiceRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final LoyaltyConfigRepository loyaltyConfigRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final LoyaltyRewardRepository loyaltyRewardRepository;
    private final LoyaltyBadgeRepository loyaltyBadgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final MemberStreakRepository memberStreakRepository;
    private final ReferralRepository referralRepository;

    private static final String TENANT_SUBDOMAIN = "demo-gym";
    private static final String TENANT_SCHEMA = "tenant_demo_gym";
    private static final String TENANT_ID = TENANT_SCHEMA; // TenantContext uses schema name
    private static final String DEFAULT_PASSWORD = "password123";

    @Override
    public void run(ApplicationArguments args) {
        if (tenantRepository.existsBySubdomain(TENANT_SUBDOMAIN)) {
            log.info("Demo tenant already exists, checking exercises");
            TenantContext.setTenantId(TENANT_ID);
            try {
                setSearchPath(TENANT_SCHEMA);
                // Check if Kieser exercises exist (they have "Kieser" in equipment)
                boolean hasKieserExercises = exerciseRepository.findAll().stream()
                        .anyMatch(e -> e.getEquipment() != null && e.getEquipment().contains("Kieser"));
                if (!hasKieserExercises) {
                    log.info("No Kieser exercises found, clearing old exercises and seeding...");
                    planExerciseRepository.deleteAll();
                    planRepository.deleteAll();
                    exerciseRepository.deleteAll();
                    List<Member> members = memberRepository.findAll();
                    seedExercisesAndPlans(members);
                    log.info("Kieser exercises seeded successfully");
                } else {
                    log.info("Kieser exercises already exist, skipping");
                }
            } finally {
                TenantContext.clear();
            }
            return;
        }

        log.info("=== SEEDING DEMO DATA ===");

        // 1. Create tenant in public schema
        Tenant tenant = Tenant.builder()
                .name("FitLife Studio")
                .subdomain(TENANT_SUBDOMAIN)
                .schemaName("tenant_demo_gym")
                .status(Tenant.TenantStatus.ACTIVE)
                .planTier("PROFESSIONAL")
                .ownerEmail("owner@fitlife.com")
                .trialEndsAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .build();
        tenantRepository.save(tenant);

        // 2. Provision schema (runs Liquibase migrations)
        // provisionTenant prepends "tenant_" so pass subdomain with hyphens replaced
        provisioningService.provisionTenant(TENANT_SUBDOMAIN.replace("-", "_"));

        // 3. Set tenant context to schema name so Hibernate can set search_path
        TenantContext.setTenantId(TENANT_ID);

        try {
            setSearchPath(TENANT_SCHEMA);

            seedUsers();
            List<UUID> facilityIds = seedFacilities();
            List<MembershipTier> tiers = seedMembershipTiers();
            List<Member> members = seedMembers(facilityIds);
            seedContracts(members, tiers);
            seedAccessDevices(facilityIds);
            seedCheckIns(members);
            seedClassesAndSchedules(facilityIds);
            seedExercisesAndPlans(members);
            seedLeadPipeline();
            seedCommunicationTemplates();
            seedEmployees(facilityIds);
            seedInvoices(members, tiers);
            seedLoyaltyProgram(members);

            log.info("=== DEMO DATA SEEDED SUCCESSFULLY ===");
            log.info("Login credentials:");
            log.info("  Owner:        owner@fitlife.com / {}", DEFAULT_PASSWORD);
            log.info("  Manager:      manager@fitlife.com / {}", DEFAULT_PASSWORD);
            log.info("  Trainer 1:    anna.trainer@fitlife.com / {}", DEFAULT_PASSWORD);
            log.info("  Trainer 2:    bob.trainer@fitlife.com / {}", DEFAULT_PASSWORD);
            log.info("  Receptionist: lisa.reception@fitlife.com / {}", DEFAULT_PASSWORD);
            log.info("  Member:       max.member@example.com / {}", DEFAULT_PASSWORD);
            log.info("  Tenant ID header: {}", TENANT_ID);
        } finally {
            TenantContext.clear();
        }
    }

    private void setSearchPath(String schema) {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + schema);
        } catch (Exception e) {
            log.warn("Could not set search_path: {}", e.getMessage());
        }
    }

    // ---- USERS ----

    private void seedUsers() {
        createUser("owner@fitlife.com", "Stefan", "Müller", Role.STUDIO_OWNER);
        createUser("manager@fitlife.com", "Sarah", "Schmidt", Role.MANAGER);
        createUser("anna.trainer@fitlife.com", "Anna", "Weber", Role.TRAINER);
        createUser("bob.trainer@fitlife.com", "Bob", "Fischer", Role.TRAINER);
        createUser("lisa.reception@fitlife.com", "Lisa", "Wagner", Role.RECEPTIONIST);

        // Member users
        String[] firstNames = {"Max", "Julia", "Thomas", "Laura", "Daniel", "Sophie", "Markus", "Nina",
                "Felix", "Clara", "Jan", "Marie", "Lukas", "Lena", "Nico", "Hannah", "Tim", "Eva",
                "Paul", "Mia", "David", "Emma", "Leon", "Lea", "Moritz", "Sarah", "Jonas", "Klara",
                "Finn", "Lisa", "Erik", "Anna", "Ben", "Sophia", "Noah", "Emily", "Elias", "Amelie",
                "Oscar", "Charlotte", "Alex", "Johanna", "Rafael", "Helena", "Tobias", "Franziska",
                "Sebastian", "Katharina", "Michael", "Olivia"};
        String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
                "Becker", "Schulz", "Hoffmann", "Schäfer", "Koch", "Bauer", "Richter", "Klein",
                "Wolf", "Schröder", "Neumann", "Schwarz", "Zimmermann", "Braun", "Krüger", "Hofmann",
                "Hartmann", "Lange", "Schmitt", "Werner", "Schmitz", "Krause", "Meier", "Lehmann",
                "Schmid", "Schulze", "Maier", "Köhler", "Herrmann", "König", "Walter", "Mayer",
                "Huber", "Kaiser", "Fuchs", "Peters", "Lang", "Scholz", "Möller", "Weiß",
                "Jung", "Hahn", "Keller"};

        for (int i = 0; i < 50; i++) {
            String email = firstNames[i].toLowerCase() + "." + lastNames[i].toLowerCase().replace("ä", "ae")
                    .replace("ö", "oe").replace("ü", "ue").replace("ß", "ss") + "@example.com";
            createUser(email, firstNames[i], lastNames[i], Role.MEMBER);
        }
        log.info("Seeded 55 users");
    }

    private User createUser(String email, String firstName, String lastName, Role role) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .tenantId(TENANT_ID)
                .enabled(true)
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }

    // ---- FACILITIES ----

    private List<UUID> seedFacilities() {
        Facility main = facilityRepository.save(Facility.builder()
                .name("FitLife Downtown")
                .description("Our flagship location in the heart of the city")
                .street("Hauptstraße 42")
                .city("Berlin").state("Berlin").postalCode("10115").country("Germany")
                .timezone("Europe/Berlin").phone("+49 30 1234567").email("downtown@fitlife.com")
                .openingHours("{\"mon\":\"06:00-23:00\",\"tue\":\"06:00-23:00\",\"wed\":\"06:00-23:00\",\"thu\":\"06:00-23:00\",\"fri\":\"06:00-22:00\",\"sat\":\"08:00-20:00\",\"sun\":\"09:00-18:00\"}")
                .brandColor("#4f46e5").maxOccupancy(200).active(true).tenantId(TENANT_ID).build());

        Facility second = facilityRepository.save(Facility.builder()
                .name("FitLife West")
                .description("Modern facility in the western district")
                .street("Kantstraße 15")
                .city("Berlin").state("Berlin").postalCode("10623").country("Germany")
                .timezone("Europe/Berlin").phone("+49 30 7654321").email("west@fitlife.com")
                .openingHours("{\"mon\":\"07:00-22:00\",\"tue\":\"07:00-22:00\",\"wed\":\"07:00-22:00\",\"thu\":\"07:00-22:00\",\"fri\":\"07:00-21:00\",\"sat\":\"09:00-18:00\",\"sun\":\"10:00-16:00\"}")
                .brandColor("#059669").maxOccupancy(120).active(true).tenantId(TENANT_ID).build());

        Facility franchise = facilityRepository.save(Facility.builder()
                .name("FitLife Partner Munich")
                .description("Franchise partner location in Munich")
                .street("Leopoldstraße 88")
                .city("Munich").state("Bavaria").postalCode("80802").country("Germany")
                .timezone("Europe/Berlin").phone("+49 89 9876543").email("munich@fitlife.com")
                .openingHours("{\"mon\":\"06:00-22:00\",\"tue\":\"06:00-22:00\",\"wed\":\"06:00-22:00\",\"thu\":\"06:00-22:00\",\"fri\":\"06:00-21:00\",\"sat\":\"08:00-18:00\",\"sun\":\"09:00-16:00\"}")
                .brandColor("#dc2626").maxOccupancy(150).parentFacilityId(main.getId())
                .active(true).tenantId(TENANT_ID).build());

        log.info("Seeded 3 facilities");
        return List.of(main.getId(), second.getId(), franchise.getId());
    }

    // ---- MEMBERSHIP TIERS ----

    private List<MembershipTier> seedMembershipTiers() {
        MembershipTier basic = tierRepository.save(MembershipTier.builder()
                .name("Basic").description("Access during off-peak hours (9am-4pm)")
                .monthlyPrice(new BigDecimal("29.00")).billingCycle(BillingCycle.MONTHLY)
                .minimumTermMonths(12).noticePeriodDays(30).classAllowance(2)
                .accessRules("Off-peak only: 9am-4pm weekdays")
                .active(true).tenantId(TENANT_ID).build());

        MembershipTier standard = tierRepository.save(MembershipTier.builder()
                .name("Standard").description("Full access to all facilities during opening hours")
                .monthlyPrice(new BigDecimal("49.00")).billingCycle(BillingCycle.MONTHLY)
                .minimumTermMonths(6).noticePeriodDays(30).classAllowance(8)
                .accessRules("Full access during opening hours")
                .active(true).tenantId(TENANT_ID).build());

        MembershipTier premium = tierRepository.save(MembershipTier.builder()
                .name("Premium").description("Full access + sauna + group classes")
                .monthlyPrice(new BigDecimal("79.00")).billingCycle(BillingCycle.MONTHLY)
                .minimumTermMonths(3).noticePeriodDays(14).classAllowance(0) // unlimited
                .accessRules("Full access + premium areas")
                .active(true).tenantId(TENANT_ID).build());

        MembershipTier vip = tierRepository.save(MembershipTier.builder()
                .name("VIP").description("Everything + personal trainer sessions + priority booking")
                .monthlyPrice(new BigDecimal("129.00")).billingCycle(BillingCycle.MONTHLY)
                .minimumTermMonths(1).noticePeriodDays(7)
                .accessRules("24/7 access + all premium features")
                .active(true).tenantId(TENANT_ID).build());

        log.info("Seeded 4 membership tiers");
        return List.of(basic, standard, premium, vip);
    }

    // ---- MEMBERS ----

    private List<Member> seedMembers(List<UUID> facilityIds) {
        List<User> memberUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MEMBER).toList();

        List<Member> members = new ArrayList<>();
        Random rng = new Random(42);
        String[] cities = {"Berlin", "Munich", "Hamburg", "Cologne", "Frankfurt"};

        for (int i = 0; i < memberUsers.size(); i++) {
            User u = memberUsers.get(i);
            MemberStatus status;
            if (i < 35) status = MemberStatus.ACTIVE;
            else if (i < 45) status = MemberStatus.INACTIVE;
            else status = MemberStatus.ACTIVE;

            LocalDate joinDate = LocalDate.now().minusDays(rng.nextInt(365 * 2));

            Member m = memberRepository.save(Member.builder()
                    .userId(u.getId())
                    .memberNumber("MBR-" + (10001 + i))
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .email(u.getEmail())
                    .phone("+49 170 " + (1000000 + rng.nextInt(9000000)))
                    .dateOfBirth(LocalDate.of(1970 + rng.nextInt(35), 1 + rng.nextInt(12), 1 + rng.nextInt(28)))
                    .gender(i % 3 == 0 ? "MALE" : i % 3 == 1 ? "FEMALE" : "OTHER")
                    .address(new Address("Musterstraße " + (1 + rng.nextInt(200)),
                            cities[rng.nextInt(cities.length)], "Germany",
                            String.valueOf(10000 + rng.nextInt(90000)), "Germany"))
                    .status(status)
                    .joinDate(joinDate)
                    .tenantId(TENANT_ID)
                    .build());
            members.add(m);

            // Assign to facility
            UUID facilityId = facilityIds.get(i % facilityIds.size());
            memberFacilityAccessRepository.save(MemberFacilityAccess.builder()
                    .memberId(m.getId()).facilityId(facilityId)
                    .homeFacility(true).crossFacilityAccess(i < 20)
                    .tenantId(TENANT_ID).build());
        }
        log.info("Seeded {} members", members.size());
        return members;
    }

    // ---- CONTRACTS ----

    private void seedContracts(List<Member> members, List<MembershipTier> tiers) {
        Random rng = new Random(42);
        int count = 0;
        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);
            MembershipTier tier = tiers.get(i % tiers.size());

            ContractStatus status;
            if (m.getStatus() == MemberStatus.ACTIVE) {
                if (i % 10 == 0) status = ContractStatus.PAUSED;
                else if (i % 15 == 0) status = ContractStatus.PENDING_CANCELLATION;
                else status = ContractStatus.ACTIVE;
            } else {
                status = ContractStatus.CANCELLED;
            }

            LocalDate start = m.getJoinDate();
            Contract.ContractBuilder builder = Contract.builder()
                    .memberId(m.getId())
                    .membershipTierId(tier.getId())
                    .status(status)
                    .startDate(start)
                    .billingStartDate(start)
                    .nextBillingDate(start.plusMonths((ChronoUnit.MONTHS.between(start, LocalDate.now()) + 1)))
                    .monthlyAmount(tier.getMonthlyPrice())
                    .autoRenew(status == ContractStatus.ACTIVE)
                    .tenantId(TENANT_ID);

            if (tier.getMinimumTermMonths() > 0) {
                builder.endDate(start.plusMonths(tier.getMinimumTermMonths()));
            }
            if (status == ContractStatus.PENDING_CANCELLATION) {
                builder.cancellationDate(LocalDate.now().minusDays(5));
                builder.cancellationEffectiveDate(LocalDate.now().plusDays(tier.getNoticePeriodDays() - 5));
                builder.cancellationReason("Moving to a different city");
            }
            if (status == ContractStatus.CANCELLED) {
                builder.cancellationDate(LocalDate.now().minusMonths(1));
                builder.cancellationEffectiveDate(LocalDate.now().minusDays(1));
                builder.cancellationReason("Personal reasons");
            }

            contractRepository.save(builder.build());
            count++;
        }
        log.info("Seeded {} contracts", count);
    }

    // ---- ACCESS DEVICES ----

    private void seedAccessDevices(List<UUID> facilityIds) {
        String[] names = {"Main Entrance Gate", "Side Door RFID", "Turnstile A"};
        DeviceType[] types = {DeviceType.GATE, DeviceType.DOOR_CONTROLLER, DeviceType.TURNSTILE};
        for (int i = 0; i < facilityIds.size(); i++) {
            deviceRepository.save(AccessDevice.builder()
                    .name(names[i % names.length] + " - Loc " + (i + 1))
                    .deviceType(types[i % types.length])
                    .mode(DeviceMode.QR)
                    .locationDescription("Facility " + (i + 1) + " entrance")
                    .ipAddress("192.168.1." + (100 + i))
                    .maxOccupancy(i == 0 ? 200 : 120)
                    .active(true)
                    .tenantId(TENANT_ID)
                    .build());
        }
        log.info("Seeded {} access devices", facilityIds.size());
    }

    // ---- CHECK-INS (30 days of history) ----

    private void seedCheckIns(List<Member> members) {
        Random rng = new Random(42);
        List<Member> activeMembers = members.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE).toList();
        int count = 0;

        for (int day = 30; day >= 0; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            // Each active member checks in with some probability
            for (Member m : activeMembers) {
                if (rng.nextDouble() < 0.35) { // 35% chance per day
                    int hour = 6 + rng.nextInt(16); // between 6am and 10pm
                    int minute = rng.nextInt(60);
                    Instant checkInTime = date.atTime(hour, minute)
                            .atZone(ZoneId.of("Europe/Berlin")).toInstant();
                    Instant checkOutTime = checkInTime.plus(45 + rng.nextInt(90), ChronoUnit.MINUTES);

                    checkInRepository.save(CheckIn.builder()
                            .memberId(m.getId())
                            .method(rng.nextBoolean() ? CheckInMethod.QR : CheckInMethod.RFID)
                            .status(CheckInStatus.SUCCESS)
                            .checkInTime(checkInTime)
                            .checkOutTime(day == 0 && hour > LocalTime.now().getHour() - 2 ? null : checkOutTime)
                            .tenantId(TENANT_ID)
                            .build());
                    count++;
                }
            }
        }
        log.info("Seeded {} check-ins over 30 days", count);
    }

    // ---- CLASSES ----

    private void seedClassesAndSchedules(List<UUID> facilityIds) {
        ClassCategory yoga = categoryRepository.save(ClassCategory.builder()
                .name("Yoga").description("Mind-body flexibility").color("#8b5cf6")
                .active(true).tenantId(TENANT_ID).build());
        ClassCategory hiit = categoryRepository.save(ClassCategory.builder()
                .name("HIIT").description("High Intensity Interval Training").color("#ef4444")
                .active(true).tenantId(TENANT_ID).build());
        ClassCategory spin = categoryRepository.save(ClassCategory.builder()
                .name("Spin").description("Indoor cycling").color("#f59e0b")
                .active(true).tenantId(TENANT_ID).build());
        ClassCategory pilates = categoryRepository.save(ClassCategory.builder()
                .name("Pilates").description("Core strength and flexibility").color("#06b6d4")
                .active(true).tenantId(TENANT_ID).build());
        ClassCategory boxing = categoryRepository.save(ClassCategory.builder()
                .name("Boxing").description("Boxing fitness").color("#dc2626")
                .active(true).tenantId(TENANT_ID).build());

        User trainer1 = userRepository.findByEmail("anna.trainer@fitlife.com").orElse(null);
        User trainer2 = userRepository.findByEmail("bob.trainer@fitlife.com").orElse(null);
        UUID t1 = trainer1 != null ? trainer1.getId() : null;
        UUID t2 = trainer2 != null ? trainer2.getId() : null;

        Object[][] classDefs = {
                {"Morning Flow Yoga", yoga.getId(), t1, 20, 60, "Studio A"},
                {"Power HIIT", hiit.getId(), t2, 25, 45, "Main Floor"},
                {"Spin Express", spin.getId(), t1, 30, 30, "Spin Room"},
                {"Core Pilates", pilates.getId(), t1, 15, 50, "Studio B"},
                {"Boxing Basics", boxing.getId(), t2, 20, 60, "Boxing Area"},
                {"Evening Yoga", yoga.getId(), t2, 20, 75, "Studio A"},
                {"Tabata Blast", hiit.getId(), t1, 30, 30, "Main Floor"},
                {"Endurance Spin", spin.getId(), t2, 30, 45, "Spin Room"},
                {"Pilates Plus", pilates.getId(), t2, 15, 50, "Studio B"},
                {"Fight Fit", boxing.getId(), t1, 20, 45, "Boxing Area"},
        };

        List<ClassDefinition> classes = new ArrayList<>();
        for (Object[] cd : classDefs) {
            classes.add(classDefRepository.save(ClassDefinition.builder()
                    .name((String) cd[0]).categoryId((UUID) cd[1]).trainerId((UUID) cd[2])
                    .capacity((int) cd[3]).durationMinutes((int) cd[4]).room((String) cd[5])
                    .allowWaitlist(true).bookingCutoffMinutes(60).cancellationCutoffMinutes(120)
                    .allowTrial(true).active(true).tenantId(TENANT_ID).build()));
        }

        // Create schedules for next 2 weeks
        int schedCount = 0;
        for (int week = 0; week < 2; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = LocalDate.now().plusDays(week * 7L + day - LocalDate.now().getDayOfWeek().getValue() + 1);
                if (date.isBefore(LocalDate.now().minusDays(1))) continue;

                // 2-3 classes per day
                int classesPerDay = day < 5 ? 3 : 2;
                int[] hours = {7, 12, 18};
                for (int c = 0; c < classesPerDay; c++) {
                    ClassDefinition cls = classes.get((day * 3 + c + week) % classes.size());
                    Instant startTime = date.atTime(hours[c], 0).atZone(ZoneId.of("Europe/Berlin")).toInstant();
                    Instant endTime = startTime.plus(cls.getDurationMinutes(), ChronoUnit.MINUTES);

                    scheduleRepository.save(ClassSchedule.builder()
                            .classId(cls.getId()).trainerId(cls.getTrainerId())
                            .startTime(startTime).endTime(endTime)
                            .room(cls.getRoom()).cancelled(false)
                            .tenantId(TENANT_ID).build());
                    schedCount++;
                }
            }
        }
        log.info("Seeded 5 categories, {} classes, {} schedules", classes.size(), schedCount);
    }

    // ---- EXERCISES & TRAINING PLANS ----

    private static final Map<String, MuscleGroup> CATEGORY_TO_MUSCLE = Map.ofEntries(
            Map.entry("Back / Spine", MuscleGroup.LOWER_BACK),
            Map.entry("Neck / Cervical Spine", MuscleGroup.NECK),
            Map.entry("Neck", MuscleGroup.NECK),
            Map.entry("Neck / Upper Traps", MuscleGroup.TRAPS),
            Map.entry("Hip", MuscleGroup.GLUTES),
            Map.entry("Abdomen / Hip Flexors", MuscleGroup.ABS),
            Map.entry("Pelvic Floor", MuscleGroup.PELVIC_FLOOR),
            Map.entry("Legs (Quads)", MuscleGroup.QUADRICEPS),
            Map.entry("Legs (Hamstrings)", MuscleGroup.HAMSTRINGS),
            Map.entry("Legs (Compound)", MuscleGroup.QUADRICEPS),
            Map.entry("Legs (Compound / Infimetric)", MuscleGroup.QUADRICEPS),
            Map.entry("Ankle", MuscleGroup.CALVES),
            Map.entry("Lower Leg", MuscleGroup.SHINS),
            Map.entry("Lower Back", MuscleGroup.LOWER_BACK),
            Map.entry("Back (Lats)", MuscleGroup.LATS),
            Map.entry("Back (Vertical Pull)", MuscleGroup.LATS),
            Map.entry("Back (Horizontal Pull)", MuscleGroup.BACK),
            Map.entry("Back / Biceps", MuscleGroup.BACK),
            Map.entry("Back / Biceps (Assisted)", MuscleGroup.BACK),
            Map.entry("Chest", MuscleGroup.CHEST),
            Map.entry("Chest / Triceps", MuscleGroup.CHEST),
            Map.entry("Chest / Triceps (Assisted)", MuscleGroup.CHEST),
            Map.entry("Shoulders", MuscleGroup.SHOULDERS),
            Map.entry("Rotator Cuff", MuscleGroup.ROTATOR_CUFF),
            Map.entry("Core (Abs)", MuscleGroup.ABS),
            Map.entry("Core / Obliques", MuscleGroup.OBLIQUES),
            Map.entry("Core / Lateral", MuscleGroup.OBLIQUES),
            Map.entry("Arms (Biceps)", MuscleGroup.BICEPS),
            Map.entry("Arms (Triceps)", MuscleGroup.TRICEPS),
            Map.entry("Forearm / Grip", MuscleGroup.FOREARMS)
    );

    private List<Exercise> seedKieserExercises() {
        List<Exercise> savedExercises = new ArrayList<>();
        try {
            InputStream is = new ClassPathResource("kieser.json").getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            JsonNode machines = root.get("machines");

            for (JsonNode machine : machines) {
                String name = machine.get("full_name").asText();
                String category = machine.get("category").asText();
                String description = machine.has("description") ? machine.get("description").asText() : "";

                MuscleGroup primaryMuscle = CATEGORY_TO_MUSCLE.getOrDefault(category, MuscleGroup.FULL_BODY);

                // Get thumbnail URL
                String thumbnailUrl = null;
                if (machine.has("images")) {
                    JsonNode images = machine.get("images");
                    if (images.has("machine_thumbnail_1920x1080")) {
                        thumbnailUrl = images.get("machine_thumbnail_1920x1080").asText();
                    } else if (images.has("machine_thumbnail_1080x1080")) {
                        thumbnailUrl = images.get("machine_thumbnail_1080x1080").asText();
                    } else if (images.has("machine_detail_1080x1080")) {
                        thumbnailUrl = images.get("machine_detail_1080x1080").asText();
                    } else if (images.has("in_action_man_on_machine")) {
                        thumbnailUrl = images.get("in_action_man_on_machine").asText();
                    } else if (images.has("in_action_woman_on_ce")) {
                        thumbnailUrl = images.get("in_action_woman_on_ce").asText();
                    }
                }

                // Get video URL
                String videoUrl = null;
                if (machine.has("videos")) {
                    JsonNode videos = machine.get("videos");
                    if (videos.has("brand_overview_mp4")) {
                        videoUrl = videos.get("brand_overview_mp4").asText();
                    }
                }

                // Build equipment string from machine code and name
                String code = machine.get("code").asText();
                String equipment = "Kieser " + code + " Machine";

                // Build posture notes from primary muscles
                StringBuilder muscles = new StringBuilder("Primary muscles: ");
                JsonNode primaryMuscles = machine.get("primary_muscles");
                if (primaryMuscles != null) {
                    List<String> muscleNames = new ArrayList<>();
                    for (JsonNode m : primaryMuscles) muscleNames.add(m.asText());
                    muscles.append(String.join(", ", muscleNames));
                }

                String difficulty = "INTERMEDIATE";
                if (machine.has("is_computer_assisted") && machine.get("is_computer_assisted").asBoolean()) {
                    difficulty = "ADVANCED";
                }

                savedExercises.add(exerciseRepository.save(Exercise.builder()
                        .name(name)
                        .exerciseType(ExerciseType.MACHINE)
                        .primaryMuscleGroup(primaryMuscle)
                        .description(description)
                        .executionTips(description)
                        .postureNotes(muscles.toString())
                        .equipment(equipment)
                        .thumbnailUrl(thumbnailUrl)
                        .videoUrl(videoUrl)
                        .difficultyLevel(difficulty)
                        .active(true)
                        .global(true)
                        .tenantId(TENANT_ID)
                        .build()));
            }
            log.info("Seeded {} Kieser exercises from kieser.json", savedExercises.size());
        } catch (Exception e) {
            log.error("Failed to load kieser.json: {}", e.getMessage());
        }
        return savedExercises;
    }

    private void seedExercisesAndPlans(List<Member> members) {
        List<Exercise> savedExercises = seedKieserExercises();

        if (savedExercises.isEmpty()) {
            log.warn("No exercises were seeded, skipping training plans");
            return;
        }

        // Create 3 template training plans
        User trainer1 = userRepository.findByEmail("anna.trainer@fitlife.com").orElse(null);
        UUID trainerId = trainer1 != null ? trainer1.getId() : null;

        // Back & Core plan (Kieser specialty)
        TrainingPlan backPlan = planRepository.save(TrainingPlan.builder()
                .name("Back & Core Fundamentals").description("Kieser back training program targeting spinal health and core stability")
                .trainerId(trainerId).status(TrainingPlanStatus.PUBLISHED)
                .template(true).catalog(true)
                .category("Back Health").difficultyLevel("BEGINNER")
                .estimatedDurationMinutes(45)
                .tenantId(TENANT_ID).build());

        // Full Body plan
        TrainingPlan fullPlan = planRepository.save(TrainingPlan.builder()
                .name("Full Body Strength").description("Complete Kieser circuit hitting all major muscle groups")
                .trainerId(trainerId).status(TrainingPlanStatus.PUBLISHED)
                .template(true).catalog(true)
                .category("Strength").difficultyLevel("INTERMEDIATE")
                .estimatedDurationMinutes(60)
                .tenantId(TENANT_ID).build());

        // Rehabilitation plan
        TrainingPlan rehabPlan = planRepository.save(TrainingPlan.builder()
                .name("Joint & Rehab Program").description("Targeted rehabilitation program for joint stability and injury prevention")
                .trainerId(trainerId).status(TrainingPlanStatus.PUBLISHED)
                .template(true).catalog(true)
                .category("Rehabilitation").difficultyLevel("BEGINNER")
                .estimatedDurationMinutes(40)
                .tenantId(TENANT_ID).build());

        // Assign exercises to plans by finding machines by name patterns
        addExercisesToPlan(backPlan, savedExercises, List.of("Lumbal", "Cervical", "Lower Back", "Torso Flexion", "Rotary Torso", "Abdominal", "Pullover"));
        addExercisesToPlan(fullPlan, savedExercises, List.of("Leg Press", "Leg Extension", "Leg Curl", "Chest Press", "Pullover", "Overhead Press", "Rowing", "Lumbal", "Abdominal", "Calf"));
        addExercisesToPlan(rehabPlan, savedExercises, List.of("Lumbal", "Cervical", "Pelvic Floor", "Hip Abduction", "Hip Adduction", "Ankle", "Shoulder Rotation"));

        // Assign plans to first 10 members
        for (int i = 0; i < Math.min(10, members.size()); i++) {
            Member m = members.get(i);
            TrainingPlan memberPlan = planRepository.save(TrainingPlan.builder()
                    .name("Personal Plan - " + m.getFirstName())
                    .description("Customized Kieser training plan")
                    .memberId(m.getId()).trainerId(trainerId)
                    .status(TrainingPlanStatus.PUBLISHED)
                    .template(false).catalog(false)
                    .category("General").difficultyLevel("INTERMEDIATE")
                    .estimatedDurationMinutes(50)
                    .tenantId(TENANT_ID).build());

            for (int e = 0; e < 5; e++) {
                int exIdx = (i * 5 + e) % savedExercises.size();
                planExerciseRepository.save(TrainingPlanExercise.builder()
                        .planId(memberPlan.getId())
                        .exerciseId(savedExercises.get(exIdx).getId())
                        .sortOrder(e + 1).sets(1).reps(12)
                        .weight(new BigDecimal(10 + e * 5 + ".0")).restSeconds(90)
                        .tenantId(TENANT_ID).build());
            }
        }
        log.info("Seeded {} exercises, 3 template plans, 10 member plans", savedExercises.size());
    }

    private void addExercisesToPlan(TrainingPlan plan, List<Exercise> exercises, List<String> namePatterns) {
        int order = 1;
        for (String pattern : namePatterns) {
            for (Exercise ex : exercises) {
                if (ex.getName().toLowerCase().contains(pattern.toLowerCase())) {
                    planExerciseRepository.save(TrainingPlanExercise.builder()
                            .planId(plan.getId())
                            .exerciseId(ex.getId())
                            .sortOrder(order++).sets(1).reps(12)
                            .weight(new BigDecimal("20.0")).restSeconds(90)
                            .tenantId(TENANT_ID).build());
                    break;
                }
            }
        }
    }

    // ---- LEADS ----

    private void seedLeadPipeline() {
        // Default stages
        LeadStage newStage = stageRepository.save(LeadStage.builder()
                .name("New").sortOrder(1).color("#6b7280").isDefault(true).isClosed(false).isWon(false).tenantId(TENANT_ID).build());
        LeadStage contacted = stageRepository.save(LeadStage.builder()
                .name("Contacted").sortOrder(2).color("#3b82f6").isDefault(false).isClosed(false).isWon(false).tenantId(TENANT_ID).build());
        LeadStage trialBooked = stageRepository.save(LeadStage.builder()
                .name("Trial Booked").sortOrder(3).color("#f59e0b").isDefault(false).isClosed(false).isWon(false).tenantId(TENANT_ID).build());
        LeadStage proposalSent = stageRepository.save(LeadStage.builder()
                .name("Proposal Sent").sortOrder(4).color("#8b5cf6").isDefault(false).isClosed(false).isWon(false).tenantId(TENANT_ID).build());
        LeadStage converted = stageRepository.save(LeadStage.builder()
                .name("Converted").sortOrder(5).color("#10b981").isDefault(false).isClosed(true).isWon(true).tenantId(TENANT_ID).build());
        LeadStage lost = stageRepository.save(LeadStage.builder()
                .name("Lost").sortOrder(6).color("#ef4444").isDefault(false).isClosed(true).isWon(false).tenantId(TENANT_ID).build());

        LeadStage[] stages = {newStage, newStage, contacted, contacted, contacted,
                trialBooked, trialBooked, proposalSent, proposalSent, proposalSent,
                converted, converted, converted, converted, lost, lost, lost,
                newStage, contacted, trialBooked};
        LeadSource[] sources = LeadSource.values();
        String[][] leadNames = {
                {"Peter", "Maurer"}, {"Sabine", "Lorenz"}, {"Jürgen", "Seidel"},
                {"Petra", "Scholz"}, {"Ralf", "Baumann"}, {"Birgit", "Schubert"},
                {"Dieter", "Geiger"}, {"Monika", "Reuter"}, {"Uwe", "Hofer"},
                {"Andrea", "Berger"}, {"Frank", "Kessler"}, {"Karin", "Ernst"},
                {"Martin", "Haas"}, {"Heike", "Pohl"}, {"Jens", "Kraft"},
                {"Stefanie", "Arnold"}, {"Bernd", "Roth"}, {"Claudia", "Franke"},
                {"Matthias", "Voigt"}, {"Katrin", "Fuchs"}
        };

        for (int i = 0; i < 20; i++) {
            leadRepository.save(Lead.builder()
                    .firstName(leadNames[i][0]).lastName(leadNames[i][1])
                    .email(leadNames[i][0].toLowerCase() + "." + leadNames[i][1].toLowerCase() + "@mail.de")
                    .phone("+49 160 " + (1000000 + i * 111111))
                    .source(sources[i % sources.length])
                    .interest(i % 3 == 0 ? "Weight loss" : i % 3 == 1 ? "Muscle building" : "General fitness")
                    .stageId(stages[i].getId())
                    .tenantId(TENANT_ID).build());
        }
        log.info("Seeded 6 lead stages, 20 leads");
    }

    // ---- COMMUNICATION TEMPLATES ----

    private void seedCommunicationTemplates() {
        Object[][] templates = {
                {"Welcome Email", ChannelType.EMAIL, "Welcome to FitLife!", "welcome",
                        "<h1>Welcome {{member.firstName}}!</h1><p>We're thrilled to have you as a member of FitLife Studio.</p>"},
                {"Birthday Greeting", ChannelType.EMAIL, "Happy Birthday {{member.firstName}}!", "birthday",
                        "<h1>Happy Birthday!</h1><p>Wishing you a fantastic day, {{member.firstName}}!</p>"},
                {"Payment Reminder", ChannelType.EMAIL, "Payment Reminder", "billing",
                        "<p>Dear {{member.firstName}}, your payment is due. Please ensure your payment method is up to date.</p>"},
                {"Appointment Reminder", ChannelType.SMS, null, "booking",
                        "Hi {{member.firstName}}, reminder: you have a class booking tomorrow. See you at FitLife!"},
                {"Class Cancellation", ChannelType.EMAIL, "Class Cancelled", "booking",
                        "<p>Dear {{member.firstName}}, unfortunately your upcoming class has been cancelled. We apologize for the inconvenience.</p>"},
                {"Contract Expiry Notice", ChannelType.EMAIL, "Your contract is expiring soon", "contract",
                        "<p>Dear {{member.firstName}}, your membership contract will expire soon. Visit us to renew!</p>"},
        };

        for (Object[] t : templates) {
            CommunicationTemplate tmpl = CommunicationTemplate.builder()
                    .name((String) t[0]).channelType((ChannelType) t[1])
                    .subject((String) t[2]).category((String) t[3])
                    .bodyHtml((String) t[4]).bodyText(((String) t[4]).replaceAll("<[^>]*>", ""))
                    .locale("de").brandColor("#4f46e5")
                    .active(true).tenantId(TENANT_ID).build();
            templateRepository.save(tmpl);
        }

        // Add notification rules for some templates
        List<CommunicationTemplate> savedTemplates = templateRepository.findAll();
        if (!savedTemplates.isEmpty()) {
            ruleRepository.save(NotificationRule.builder()
                    .name("Welcome new members").triggerEvent(TriggerEvent.WELCOME)
                    .templateId(savedTemplates.get(0).getId()).channelType(ChannelType.EMAIL)
                    .delayDays(0).delayDirection(DelayDirection.IMMEDIATE)
                    .active(true).tenantId(TENANT_ID).build());
            ruleRepository.save(NotificationRule.builder()
                    .name("Birthday greetings").triggerEvent(TriggerEvent.BIRTHDAY)
                    .templateId(savedTemplates.get(1).getId()).channelType(ChannelType.EMAIL)
                    .delayDays(0).delayDirection(DelayDirection.IMMEDIATE)
                    .active(true).tenantId(TENANT_ID).build());
            ruleRepository.save(NotificationRule.builder()
                    .name("Payment failed alert").triggerEvent(TriggerEvent.PAYMENT_FAILED)
                    .templateId(savedTemplates.get(2).getId()).channelType(ChannelType.EMAIL)
                    .delayDays(1).delayDirection(DelayDirection.AFTER)
                    .active(true).tenantId(TENANT_ID).build());
        }
        log.info("Seeded {} communication templates, 3 notification rules", templates.length);
    }

    // ---- EMPLOYEES ----

    private void seedEmployees(List<UUID> facilityIds) {
        User owner = userRepository.findByEmail("owner@fitlife.com").orElse(null);
        User manager = userRepository.findByEmail("manager@fitlife.com").orElse(null);
        User trainer1 = userRepository.findByEmail("anna.trainer@fitlife.com").orElse(null);
        User trainer2 = userRepository.findByEmail("bob.trainer@fitlife.com").orElse(null);
        User receptionist = userRepository.findByEmail("lisa.reception@fitlife.com").orElse(null);

        Object[][] empData = {
                {owner, "Stefan", "Müller", "STUDIO_OWNER", EmploymentType.FULL_TIME, "Owner & CEO", null, new BigDecimal("8000")},
                {manager, "Sarah", "Schmidt", "MANAGER", EmploymentType.FULL_TIME, "General Manager", null, new BigDecimal("4500")},
                {trainer1, "Anna", "Weber", "TRAINER", EmploymentType.FULL_TIME, "Senior Trainer", new BigDecimal("35.00"), null},
                {trainer2, "Bob", "Fischer", "TRAINER", EmploymentType.FREELANCE, "Freelance Trainer", new BigDecimal("45.00"), null},
                {receptionist, "Lisa", "Wagner", "RECEPTIONIST", EmploymentType.PART_TIME, "Front Desk", new BigDecimal("15.00"), null},
        };

        for (int i = 0; i < empData.length; i++) {
            Object[] d = empData[i];
            User u = (User) d[0];
            Employee emp = employeeRepository.save(Employee.builder()
                    .userId(u != null ? u.getId() : null)
                    .firstName((String) d[1]).lastName((String) d[2])
                    .email(u != null ? u.getEmail() : null)
                    .role((String) d[3]).employmentType((EmploymentType) d[4])
                    .position((String) d[5]).hourlyRate((BigDecimal) d[6])
                    .monthlySalary((BigDecimal) d[7])
                    .hireDate(LocalDate.now().minusMonths(6 + i * 3))
                    .active(true).tenantId(TENANT_ID).build());

            // Assign to facilities
            empFacilityRepository.save(EmployeeFacility.builder()
                    .employeeId(emp.getId()).facilityId(facilityIds.get(0))
                    .primary(true).tenantId(TENANT_ID).build());
            if (i < 2) { // Owner and manager at all facilities
                for (int f = 1; f < facilityIds.size(); f++) {
                    empFacilityRepository.save(EmployeeFacility.builder()
                            .employeeId(emp.getId()).facilityId(facilityIds.get(f))
                            .primary(false).tenantId(TENANT_ID).build());
                }
            }
        }
        log.info("Seeded 5 employees with facility assignments");
    }

    // ---- INVOICES ----

    private void seedInvoices(List<Member> members, List<MembershipTier> tiers) {
        Random rng = new Random(42);
        int count = 0;
        long invoiceNum = 10001;
        for (int i = 0; i < Math.min(30, members.size()); i++) {
            Member m = members.get(i);
            MembershipTier tier = tiers.get(i % tiers.size());
            // Generate 2-3 months of invoices
            for (int month = 2; month >= 0; month--) {
                BigDecimal amount = tier.getMonthlyPrice();
                BigDecimal vat = amount.multiply(new BigDecimal("0.19"));
                BigDecimal total = amount.add(vat);
                Instant issuedAt = LocalDate.now().minusMonths(month).atStartOfDay(ZoneId.of("Europe/Berlin")).toInstant();

                InvoiceStatus status;
                if (month == 0 && rng.nextDouble() < 0.2) status = InvoiceStatus.OVERDUE;
                else if (month == 0) status = InvoiceStatus.ISSUED;
                else status = InvoiceStatus.PAID;

                Invoice inv = Invoice.builder()
                        .memberId(m.getId())
                        .invoiceNumber("INV-" + (invoiceNum++))
                        .amount(amount).vatAmount(vat).totalAmount(total)
                        .currency("EUR").status(status)
                        .issuedAt(issuedAt)
                        .dueDate(LocalDate.now().minusMonths(month).plusDays(14))
                        .paidAt(status == InvoiceStatus.PAID ? issuedAt.plus(5, ChronoUnit.DAYS) : null)
                        .tenantId(TENANT_ID).build();
                invoiceRepository.save(inv);
                count++;
            }
        }
        log.info("Seeded {} invoices", count);
    }

    // ---- LOYALTY PROGRAM ----

    private void seedLoyaltyProgram(List<Member> members) {
        // 1. Points configuration
        Map<String, Integer> pointsConfig = Map.of(
                "points.CHECK_IN", 10,
                "points.CLASS_BOOKING", 15,
                "points.SESSION_COMPLETE", 20,
                "points.REFERRAL", 50,
                "points.BIRTHDAY", 100,
                "points.ANNIVERSARY", 200,
                "points.GOAL_ACHIEVED", 30
        );
        for (var entry : pointsConfig.entrySet()) {
            loyaltyConfigRepository.save(LoyaltyConfig.builder()
                    .configKey(entry.getKey())
                    .configValue(String.valueOf(entry.getValue()))
                    .description("Points for " + entry.getKey().replace("points.", ""))
                    .tenantId(TENANT_ID).build());
        }

        // 2. Loyalty tiers
        LoyaltyTier bronze = loyaltyTierRepository.save(LoyaltyTier.builder()
                .name("Bronze").minPoints(0).color("#CD7F32").icon("shield")
                .perks("Basic member benefits").sortOrder(0).active(true).tenantId(TENANT_ID).build());
        LoyaltyTier silver = loyaltyTierRepository.save(LoyaltyTier.builder()
                .name("Silver").minPoints(500).color("#C0C0C0").icon("star")
                .perks("Priority class booking, 5% shop discount").sortOrder(1).active(true).tenantId(TENANT_ID).build());
        LoyaltyTier gold = loyaltyTierRepository.save(LoyaltyTier.builder()
                .name("Gold").minPoints(1500).color("#FFD700").icon("trophy")
                .perks("Priority booking, 10% discount, 1 free guest pass/month").sortOrder(2).active(true).tenantId(TENANT_ID).build());
        LoyaltyTier platinum = loyaltyTierRepository.save(LoyaltyTier.builder()
                .name("Platinum").minPoints(5000).color("#E5E4E2").icon("crown")
                .perks("All Gold perks, 20% discount, unlimited guest passes, exclusive classes").sortOrder(3).active(true).tenantId(TENANT_ID).build());

        // 3. Rewards catalog
        loyaltyRewardRepository.save(LoyaltyReward.builder()
                .name("Free Month").description("One month membership extension at no cost")
                .rewardType(RewardType.FREE_MONTH).pointsCost(2000).value(BigDecimal.valueOf(49))
                .active(true).totalAvailable(50).totalRedeemed(0).tenantId(TENANT_ID).build());
        loyaltyRewardRepository.save(LoyaltyReward.builder()
                .name("10% Discount").description("10% off next month's membership fee")
                .rewardType(RewardType.DISCOUNT).pointsCost(500).value(BigDecimal.valueOf(4.90))
                .active(true).totalRedeemed(0).tenantId(TENANT_ID).build());
        loyaltyRewardRepository.save(LoyaltyReward.builder()
                .name("Free Class Credit").description("One free class booking of your choice")
                .rewardType(RewardType.CLASS_CREDIT).pointsCost(300).value(BigDecimal.valueOf(15))
                .active(true).totalRedeemed(0).tenantId(TENANT_ID).build());
        loyaltyRewardRepository.save(LoyaltyReward.builder()
                .name("Branded Water Bottle").description("Premium stainless steel FitLife water bottle")
                .rewardType(RewardType.MERCH).pointsCost(750).value(BigDecimal.valueOf(25))
                .active(true).totalAvailable(100).totalRedeemed(3).tenantId(TENANT_ID).build());
        loyaltyRewardRepository.save(LoyaltyReward.builder()
                .name("Personal Training Session").description("One 60-minute session with a trainer")
                .rewardType(RewardType.CUSTOM).pointsCost(1500).value(BigDecimal.valueOf(80))
                .active(true).totalRedeemed(0).tenantId(TENANT_ID).build());

        // 4. Badges
        LoyaltyBadge firstCheckin = loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("First Steps").description("Complete your first check-in").icon("footprints")
                .category(BadgeCategory.CHECKIN).criteriaType(BadgeCriteriaType.CHECKIN_COUNT).criteriaValue(1)
                .active(true).tenantId(TENANT_ID).build());
        LoyaltyBadge regularBadge = loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Regular").description("Check in 50 times").icon("repeat")
                .category(BadgeCategory.CHECKIN).criteriaType(BadgeCriteriaType.CHECKIN_COUNT).criteriaValue(50)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Century Club").description("Check in 100 times").icon("100")
                .category(BadgeCategory.CHECKIN).criteriaType(BadgeCriteriaType.CHECKIN_COUNT).criteriaValue(100)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Training Rookie").description("Complete 10 training sessions").icon("dumbbell")
                .category(BadgeCategory.TRAINING).criteriaType(BadgeCriteriaType.SESSION_COUNT).criteriaValue(10)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Iron Will").description("Complete 50 training sessions").icon("fire")
                .category(BadgeCategory.TRAINING).criteriaType(BadgeCriteriaType.SESSION_COUNT).criteriaValue(50)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Social Butterfly").description("Refer 3 friends").icon("users")
                .category(BadgeCategory.SOCIAL).criteriaType(BadgeCriteriaType.REFERRAL_COUNT).criteriaValue(3)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("Streak Master").description("Maintain a 30-day check-in streak").icon("flame")
                .category(BadgeCategory.CHECKIN).criteriaType(BadgeCriteriaType.STREAK_DAYS).criteriaValue(30)
                .active(true).tenantId(TENANT_ID).build());
        loyaltyBadgeRepository.save(LoyaltyBadge.builder()
                .name("One Year Strong").description("Be a member for 12 months").icon("calendar")
                .category(BadgeCategory.MEMBERSHIP).criteriaType(BadgeCriteriaType.MEMBER_DURATION_MONTHS).criteriaValue(12)
                .active(true).tenantId(TENANT_ID).build());

        // 5. Award points and streaks to first 20 members (simulating activity)
        Random rng = new Random(42);
        int txCount = 0;
        for (int i = 0; i < Math.min(20, members.size()); i++) {
            Member m = members.get(i);
            int balance = 0;

            // Simulate check-in points (5-25 check-ins per member)
            int checkins = 5 + rng.nextInt(21);
            for (int c = 0; c < checkins; c++) {
                balance += 10;
                loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                        .memberId(m.getId()).points(10).balanceAfter(balance)
                        .transactionType(TransactionType.EARN).action(LoyaltyAction.CHECK_IN)
                        .description("Check-in points").tenantId(TENANT_ID).build());
                txCount++;
            }

            // Simulate some class bookings (0-5)
            int bookings = rng.nextInt(6);
            for (int b = 0; b < bookings; b++) {
                balance += 15;
                loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                        .memberId(m.getId()).points(15).balanceAfter(balance)
                        .transactionType(TransactionType.EARN).action(LoyaltyAction.CLASS_BOOKING)
                        .description("Class booking points").tenantId(TENANT_ID).build());
                txCount++;
            }

            // Simulate training sessions (0-8)
            int sessions = rng.nextInt(9);
            for (int s = 0; s < sessions; s++) {
                balance += 20;
                loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                        .memberId(m.getId()).points(20).balanceAfter(balance)
                        .transactionType(TransactionType.EARN).action(LoyaltyAction.SESSION_COMPLETE)
                        .description("Training session points").tenantId(TENANT_ID).build());
                txCount++;
            }

            // Award "First Steps" badge to all 20 members
            memberBadgeRepository.save(MemberBadge.builder()
                    .memberId(m.getId()).badgeId(firstCheckin.getId())
                    .earnedAt(Instant.now().minus(rng.nextInt(30), ChronoUnit.DAYS))
                    .tenantId(TENANT_ID).build());

            // Award "Regular" badge to top active members
            if (checkins >= 15) {
                memberBadgeRepository.save(MemberBadge.builder()
                        .memberId(m.getId()).badgeId(regularBadge.getId())
                        .earnedAt(Instant.now().minus(rng.nextInt(10), ChronoUnit.DAYS))
                        .tenantId(TENANT_ID).build());
            }

            // Create streaks for first 10 members
            if (i < 10) {
                int streak = 3 + rng.nextInt(25);
                int longest = streak + rng.nextInt(10);
                memberStreakRepository.save(MemberStreak.builder()
                        .memberId(m.getId()).streakType(StreakType.DAILY_CHECKIN)
                        .currentStreak(streak).longestStreak(longest)
                        .lastActivityDate(LocalDate.now().minusDays(rng.nextInt(3)))
                        .streakStartDate(LocalDate.now().minusDays(streak))
                        .tenantId(TENANT_ID).build());
            }
        }

        // 6. Create a few referrals
        if (members.size() >= 5) {
            referralRepository.save(Referral.builder()
                    .referrerMemberId(members.get(0).getId())
                    .referredMemberId(members.get(3).getId())
                    .referredEmail("referred1@example.com")
                    .referralCode("FITLIFE01")
                    .status(ReferralStatus.CONVERTED)
                    .referrerPointsAwarded(50).referredPointsAwarded(50)
                    .convertedAt(Instant.now().minus(15, ChronoUnit.DAYS))
                    .tenantId(TENANT_ID).build());
            referralRepository.save(Referral.builder()
                    .referrerMemberId(members.get(1).getId())
                    .referredEmail("prospect@example.com")
                    .referralCode("FITLIFE02")
                    .status(ReferralStatus.PENDING)
                    .referrerPointsAwarded(0).referredPointsAwarded(0)
                    .tenantId(TENANT_ID).build());
            referralRepository.save(Referral.builder()
                    .referrerMemberId(members.get(0).getId())
                    .referredEmail("friend@example.com")
                    .referralCode("FITLIFE03")
                    .status(ReferralStatus.SIGNED_UP)
                    .referrerPointsAwarded(0).referredPointsAwarded(0)
                    .tenantId(TENANT_ID).build());
        }

        log.info("Seeded loyalty program: 4 tiers, 5 rewards, 8 badges, {} transactions, streaks, 3 referrals", txCount);
    }
}
