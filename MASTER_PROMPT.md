# MASTER BUILD PROMPT — GYM MANAGEMENT PLATFORM (Magicline Clone)

You are building a **full-featured, production-grade gym and fitness studio management SaaS platform** — a complete functional clone of Magicline (magicline.com), Europe's largest cloud-based gym management software.

Do not ask clarifying questions. Do not build placeholders. Build the real thing. Every feature listed must be fully implemented end to end — database schema, backend API, frontend UI, and any required background jobs.

---

## TECH STACK — USE EXACTLY THIS

### Backend
- **Java 21** + **Spring Boot 3.x**
- **Spring Security** — JWT authentication + role-based access control (RBAC)
- **Spring Data JPA** + **Hibernate** — ORM and database access
- **Spring Batch** — scheduled payment runs, dunning jobs, bulk email dispatch
- **Spring WebSocket** — real-time check-in events and live occupancy updates
- **RabbitMQ** — async messaging for emails, SMS, push notifications, payment events
- **PostgreSQL** — primary relational database
- **Redis** — caching, session management, rate limiting
- **Liquibase** — database schema migrations (version controlled)
- **Swagger / OpenAPI 3** — auto-generated API documentation on all endpoints
- **Maven** — build tool

### Frontend (Web Dashboard)
- **React 18** + **TypeScript**
- **Vite** — build tool
- **TanStack Query (React Query)** — server state management and caching
- **Zustand** — lightweight local/global state
- **shadcn/ui** + **Tailwind CSS** — UI component library and styling
- **Recharts** — analytics charts and KPI dashboards
- **React Hook Form** + **Zod** — form handling and schema validation
- **Socket.io client** — real-time occupancy and check-in feed
- **React Router v6** — client-side routing

### Mobile (iOS & Android)
- **React Native** + **Expo** (SDK 51+) — single codebase for iOS and Android
- **Expo Router** — file-based navigation
- **TanStack Query** — server state, same patterns as the web frontend
- **Zustand** — local state, shared store patterns with web where possible
- **React Hook Form** + **Zod** — forms and validation, same schemas as web
- **Expo Notifications** + **Firebase Cloud Messaging** — push notifications
- **Expo SecureStore** — secure JWT token storage (never AsyncStorage for tokens)
- **React Native Reanimated** + **Gesture Handler** — smooth animations
- **Expo Camera** — QR code scanner for check-in
- **React Native MMKV** — fast local storage for offline data
- **WatermelonDB** — offline-capable training plan data sync
- **NativeWind** — Tailwind CSS for React Native (consistent styling with web)
- **Jest** + **React Native Testing Library** — unit and component tests
- **EAS Build** — Expo Application Services for CI/CD and app store submission

### Infrastructure & DevOps
- **Docker** + **Docker Compose** — local development (all services start with `docker-compose up`)
- **AWS** — ECS/EKS for containers, RDS for PostgreSQL, S3 for file storage, SES for email, CloudWatch for monitoring
- **Kubernetes manifests** — for production deployment
- **GitHub Actions** — CI/CD pipeline
- **Terraform** — infrastructure as code

### Payments
- **Stripe** — credit/debit card payments, subscriptions, invoicing
- **GoCardless** — SEPA Direct Debit for European gym membership fee collection

### Communications
- **AWS SES** — transactional email
- **Twilio** — SMS
- **Firebase Cloud Messaging** — mobile push notifications

---

## PROJECT STRUCTURE

```
/
├── backend/                          # Spring Boot (Java)
│   ├── src/main/java/com/gymplatform/
│   │   ├── config/                   # Security, RabbitMQ, Redis, Swagger configs
│   │   ├── modules/
│   │   │   ├── auth/                 # JWT, login, registration, RBAC
│   │   │   ├── member/               # Member profiles, anamnesis, notes
│   │   │   ├── contract/             # Memberships, tiers, freeze, cancel
│   │   │   ├── checkin/              # Access control, RFID/QR, occupancy
│   │   │   ├── finance/              # Payments, invoices, dunning, direct debit
│   │   │   ├── booking/              # Classes, courses, appointments
│   │   │   ├── training/             # Training plans, exercises, progress
│   │   │   ├── communication/        # Email/SMS templates, notification rules
│   │   │   ├── sales/                # Leads, pipeline, digital contracts
│   │   │   ├── staff/                # Employees, shifts, time tracking
│   │   │   ├── facility/             # Locations, rooms, equipment
│   │   │   ├── reporting/            # Analytics, KPIs, revenue reports
│   │   │   ├── api/                  # Open API partner endpoints
│   │   │   └── webhook/              # Inbound/outbound webhook handling
│   │   └── shared/                   # Common DTOs, exceptions, utilities
│   └── src/main/resources/
│       ├── application.yml
│       └── db/changelog/             # Liquibase migration files
├── frontend/                         # React + TypeScript
│   └── src/
│       ├── components/               # Shared UI components
│       ├── pages/                    # Route-level page components
│       ├── modules/                  # Feature-organized modules
│       ├── hooks/                    # Custom React hooks
│       ├── store/                    # Zustand stores
│       ├── api/                      # TanStack Query API hooks
│       └── types/                    # TypeScript type definitions
├── mobile/                           # React Native + Expo (iOS & Android)
│   └── src/
│       ├── app/                      # Expo Router file-based routes
│       ├── components/               # Shared RN components
│       ├── modules/                  # Feature modules (mirrors web structure)
│       ├── hooks/                    # Custom hooks (shared logic with web)
│       ├── store/                    # Zustand stores (shared with web)
│       ├── api/                      # TanStack Query hooks (shared with web)
│       └── types/                    # TypeScript types (shared with web)
├── infra/                            # Docker, K8s, Terraform
│   ├── docker-compose.yml
│   ├── k8s/
│   └── terraform/
└── docs/                             # API docs, architecture diagrams
```

---

## MULTI-TENANCY

This is a **multi-tenant SaaS** platform. Every gym chain, franchise, or independent studio is a **separate tenant**.

- Use **schema-per-tenant** in PostgreSQL. Each tenant gets an isolated schema.
- Every API request must resolve the tenant from the subdomain or `X-Tenant-ID` header.
- A `TenantContext` thread-local must be set on every request and used by all JPA repositories.
- A `public` schema holds tenant registry and billing data.
- Cross-tenant data access must be impossible at the ORM level.

---

## AUTHENTICATION & ROLES

JWT-based authentication. Refresh token rotation.

**Roles (RBAC):**
- `SUPER_ADMIN` — platform operator (cross-tenant)
- `STUDIO_OWNER` — full access within their tenant
- `MANAGER` — manage staff, members, finances within assigned facilities
- `TRAINER` — access training, class, and member data; no finance
- `MEMBER` — access own profile, bookings, and training via member portal/app
- `RECEPTIONIST` — check-in, member lookup, basic sales

Each role must have fine-grained permission checks on every endpoint using Spring Security method-level annotations (`@PreAuthorize`).

---

## FULL FEATURE LIST — BUILD ALL OF THESE

### 1. MEMBER MANAGEMENT
- Member profiles: personal data, contact info, profile photo, emergency contact, health notes
- Member dashboard showing membership status, upcoming bookings, check-in history, outstanding balance
- Member search with filters: name, membership status, contract type, facility, check-in date
- Create, edit, deactivate member accounts
- Member notes and internal comments (staff-only)
- Member document storage (contracts, medical forms) via AWS S3
- Health anamnesis form builder and submission storage
- Member tags and segmentation for marketing
- Compensation / credit system: assign credits, members redeem via self-service
- Duplicate member detection

### 2. CONTRACT & MEMBERSHIP MANAGEMENT
- Flexible membership tier configuration: name, price, billing cycle, access rules, class allowances
- Digital contract creation — pre-fill member data, apply discount codes, add optional services
- Digital signature capture (canvas-based in browser, stored as PDF)
- Contract states: active, paused (freeze), pending cancellation, cancelled, expired
- Contract freeze / idle period: define start/end date, pro-rate billing
- Contract cancellation flow with configurable notice periods
- Withdraw cancellation option
- Multiple active contracts per member
- Day passes and trial training products
- Voucher / gift card creation and redemption
- Automatic contract renewal logic

### 3. ACCESS CONTROL & CHECK-IN
- Manual check-in via staff dashboard (member search + one-click check-in)
- Automated check-in via RFID transponder and QR code
- Hardware abstraction layer: generic `AccessDevice` interface with implementations for different vendor APIs (doors, gates, turnstiles, vending machines)
- Real-time occupancy counter per facility via WebSocket
- Occupancy limit enforcement — reject check-in if max capacity reached
- Event monitor: live log of all check-in events (success, failed, denied) with reason codes
- Check-in history per member
- Access restriction rules: block access if payment overdue, contract expired, or manual restriction set
- Automatic access restriction release when payment is settled
- Double check-in window prevention (configurable timeout)
- External QR code validation for partner networks (Urban Sports Club pattern)
- Vending machine integration: product catalog, tray assignment, member consumption credit payments
- Body composition scale integration: auto-push measurement results to member profile

### 4. CLASS & COURSE MANAGEMENT
- Class/course creation: name, description, category, trainer, room, capacity, recurrence rules
- Weekly schedule view and calendar view for staff
- Online booking portal for members (available via member app and web)
- Booking confirmation and cancellation with configurable cut-off times
- Waitlist management: auto-promote from waitlist when slot opens
- Trial training and day pass bookings accessible to non-members
- Class attendance tracking: mark present/absent per booking
- Zoom / virtual class link integration
- Class category management (yoga, HIIT, spin, etc.)
- Class utilization analytics

### 5. TRAINING WORLD
- Exercise library with 500+ exercises, each with:
  - Name, description, muscle groups targeted, exercise type
  - HD instructional video (stored on S3, streamed)
  - Execution tips and posture correction notes
- Filter/search exercises by muscle group, equipment, type
- Training plan builder:
  - Drag-and-drop exercise ordering
  - Configure sets, reps, weight, rest time per exercise
  - Add trainer comments per exercise
  - Save as draft
  - Publish to member
- Training plan templates: create once, apply to multiple members
- Training plan catalog: member self-selects from published catalog in app
- Anamnesis / fitness assessment: structured form, store results per member
- Member progress tracking: log actual sets/reps/weight per session
- Training goals: define target, track progress over time
- Members can independently modify weight/rep targets in app

### 6. FINANCE & PAYMENT MANAGEMENT
- **Payment methods:** SEPA Direct Debit (GoCardless), credit/debit card (Stripe), cash
- Payment run scheduler (Spring Batch): configurable run day, auto-collect all due membership fees
- Invoice generation: PDF invoices auto-generated per payment, stored on S3, emailed to member
- Failed payment handling:
  - Return debit detection via GoCardless webhook
  - Automatic dunning level escalation
  - Configurable dunning levels (1st reminder, 2nd reminder, collections handoff)
  - Access restriction trigger on overdue balance
- Online payment links: generate unique payment URL + QR code for outstanding balances (embed in email/letter)
- Members pay outstanding balance via Stripe-hosted page, access restriction auto-lifted on success
- Cash register / POS:
  - Sell day tickets, supplements, merchandise at front desk
  - Assign products to categories
  - Daily cash register report
- Consumption credit: top up balance, deduct on vending machine purchase or class booking
- MemberCash / premium collection service integration (webhook-based)
- Revenue dashboard: total revenue, monthly recurring revenue, outstanding receivables, payment success rate
- Multi-currency support (EUR primary)
- VAT configuration per product and per country
- Refund processing

### 7. CRM & COMMUNICATION
- Communication centre: unified inbox/outbox for all member communications
- Template builder:
  - Rich text editor with variable interpolation (`{{member.firstName}}`, `{{studio.name}}`, etc.)
  - HTML template import support
  - Logo upload, brand colours, font styles
  - Embed images, links, payment QR codes
  - Preview before send
- Template types: email, SMS, letter (PDF), push notification
- Notification rules (automations):
  - Trigger: birthday, new membership signed, payment failed, appointment reminder, contract expiry, trial class booked, membership anniversary
  - Action: send email/SMS/push via RabbitMQ consumer
  - Configurable delay: send immediately, or X days before/after trigger
- Serial mailing: select template + recipient segment + schedule, dispatch in bulk via Spring Batch
- Failed message notification: dashboard alert if send fails
- Push notifications via Firebase to member app
- WhatsApp integration via Twilio WhatsApp API
- Full send history per member with status (sent, delivered, failed, opened)

### 8. SALES & LEAD MANAGEMENT
- Lead profiles: name, contact, source, interest, assigned staff member
- Lead pipeline with configurable stages (new, contacted, trial booked, proposal sent, converted, lost)
- Lead communication log: every call, email, visit logged with outcome
- Staff task assignment: follow-up reminders with due dates
- Conversion tracking: lead → trial → member conversion rates
- Digital sales tool:
  - Tablet/mobile optimised sales flow
  - Browse membership tiers, apply discounts, add optional services
  - Digital contract creation and signature in one flow
  - Works offline (queue sync)
- Landing pages: configurable per-offer landing page for lead capture (embed on gym website)
- Trial training / day pass offer pages: public booking without login
- Promo codes: create codes with discount type (%, fixed), expiry, usage limit
- Referral program:
  - Member shares referral code
  - Referred friend signs up → referrer earns bonus (credit, discount, gift)
  - Configurable bonus rules
- Voucher sales: sell gift vouchers online and at POS, recipient redeems at signup

### 9. STAFF & HR MANAGEMENT
- Employee profiles: personal data, contact, role, assigned facilities, professional competencies
- Role and permission management per employee (use RBAC roles defined above)
- Shift planning: weekly schedule builder, assign employees to shifts and facilities
- Time tracking: clock in/out, log working hours, link to shift plan
- Shift vs actual hours report
- Multi-facility staff assignment: one employee can work across multiple locations
- Freelance instructor management: create credit notes via integration (Payforce pattern)

### 10. MULTI-LOCATION & CHAIN MANAGEMENT
- Facility (location) management: name, address, timezone, opening hours, contact, branding
- All features operate per-facility: memberships, check-ins, classes, staff assignments
- Cross-facility member check-in: members with eligible memberships can check in at any linked facility
- Consolidated owner dashboard: revenue, occupancy, new members, check-ins across all locations
- Per-location or global configuration for: membership tiers, access rules, notification templates
- Franchise support: parent account manages child studio configurations

### 11. MEMBER SELF-SERVICE PORTAL & APP

**Web self-service (React, accessible at `/portal`):**
- Login with email/password or magic link
- View and edit personal data (name, address, contact, payment method)
- View active contracts, contract status, next billing date
- Cancel contract (with freeze suggestion popup before confirming)
- Withdraw pending cancellation
- View and download invoices
- Pay outstanding balance online
- Top up consumption credit
- View and book classes
- View training plan and log training sessions
- View check-in history
- Upload COVID / health certificates
- View and redeem compensation credits
- News/announcements from the studio
- Studio occupancy indicator (live)

**Mobile App (React Native + Expo — iOS & Android):**
- All self-service features above, native mobile UI using NativeWind
- Push notifications via Expo Notifications + Firebase Cloud Messaging
- QR code display for check-in (generated from API, time-limited, single-use)
- QR code scanner via Expo Camera (for staff check-in flow on tablet)
- Offline-capable training plan viewing via WatermelonDB local sync
- JWT tokens stored securely in Expo SecureStore
- Biometric authentication (Face ID / fingerprint) via Expo LocalAuthentication
- Deep linking support for push notification tap-through
- Smooth swipe gestures and animations via Reanimated + Gesture Handler
- Submit to both Apple App Store and Google Play via EAS Build

### 12. STAFF & TRAINER FLOOR APP

A dedicated **staff-facing mobile experience** (React Native, same app as member app but with a staff role view) optimised for use on a tablet or phone on the gym floor. This is separate from the back-office web dashboard — it's what trainers and receptionists use while moving around the gym.

**Trainer floor mode:**
- Quick member lookup by name, membership number, or QR scan
- View full member profile on the floor: photo, membership status, active training plan, last check-in, health notes, outstanding balance
- Create and edit training plans tableside — full drag-and-drop plan builder in the mobile UI
- Conduct and record anamnesis (health assessment) directly in the app
- View member's training history and progress charts
- Add training session notes and posture correction comments in real time
- Assign or swap a training plan template to a member instantly
- View the day's class schedule and manage class check-ins (mark attendance)
- Trainer's own schedule: assigned classes and personal training appointments for the day

**Receptionist / sales floor mode:**
- Manual member check-in via member search or QR/RFID scan result
- Sell day tickets, membership upgrades, and supplements at the front desk (connects to POS)
- Open and complete a digital contract signing flow on tablet (full sales flow, works offline)
- View current gym occupancy live
- View upcoming class bookings and walk-in availability
- Handle member queries: look up invoices, check payment status, apply credits

**Shared staff app features:**
- Role-aware navigation — trainer sees training features, receptionist sees check-in and sales, manager sees everything
- Push notifications for staff: class starting alerts, member arrival alerts, task reminders
- Works offline for training plan creation and anamnesis; syncs when connection restored
- Biometric login (Face ID / fingerprint) for quick access between members on the floor

### 13. PUBLIC STUDIO DISCOVERY & BOOKING PORTAL

This is a **public-facing website** (no login required) equivalent to MySports.com — the studio's public presence for attracting and converting new members.

- Public studio profile page: studio name, description, photos, facilities, opening hours, contact info, Google Maps embed
- Class schedule browser: view all upcoming classes, filter by category, trainer, time — publicly visible
- Public online booking:
  - Book a trial training session without an account (just name + email + phone)
  - Purchase a day pass online
  - Book individual coaching sessions
  - Book specific classes (prompts account creation on confirmation)
- Studio news/announcements section: visible to the public and logged-in members
- Offer / promotion landing pages: configurable per-campaign, embeddable on external websites via iframe or link
- No commission model: 100% of revenue from public bookings goes directly to the studio
- Studio branding: custom logo, colours, banner image per tenant
- Mobile-responsive design — works perfectly on phone browsers
- SEO-friendly pages with proper meta tags and structured data
- All public bookings instantly appear in the staff dashboard with member/lead record auto-created
- Trial training confirmation email auto-sent to prospect via notification rule

### 14. MARKETING TOOLS

- **Marketing campaign management:**
  - Create and track campaigns targeting leads and members
  - Campaign types: email blast, SMS campaign, push notification campaign
  - Audience builder: segment by membership status, check-in frequency, contract expiry, tags, facility, last visit date
  - Campaign scheduler: send now or schedule for future date/time
  - Campaign performance tracking: sent, delivered, opened, clicked, converted
- **Activity-based reminders and pattern management:**
  - Detect inactive members (configurable: no check-in for X days)
  - Auto-trigger win-back communication sequence
  - Visit frequency pattern analysis per member
  - At-risk member alerts on staff dashboard (members likely to churn)
- **Facial recognition integration pattern:**
  - `FaceRecognitionAdapter` interface for connecting facial recognition hardware (FaceForce/Facetronic pattern)
  - On successful face match: auto check-in member, log event
  - Anti-spoofing / misuse detection flag
- **Social media integration:**
  - Share studio news to Facebook and Instagram via API
  - UTM link generator for tracking campaign traffic sources
- **Google Ads / Meta Ads lead form integration:**
  - Inbound webhook to receive leads from Google Lead Form Extensions and Meta Lead Ads
  - Auto-create lead in pipeline with source attribution

### 15. MEMBER LOYALTY & REWARDS PROGRAM

- **Points system:**
  - Members earn points for: check-ins, booking classes, completing training sessions, referring friends, membership anniversaries, hitting training goals, birthday bonus
  - Configurable points per action (e.g. 10 points per check-in, 50 points per referral)
  - Points ledger per member with full history
- **Rewards / redemption:**
  - Define rewards: free month, merchandise discount, class credit, consumption credit top-up
  - Members browse and redeem rewards via self-service portal and app
  - Redemption auto-applies to account (credit posted, discount code generated)
- **Tiers / levels:**
  - Configurable loyalty tiers (e.g. Bronze, Silver, Gold, Platinum)
  - Members progress through tiers based on cumulative points or check-in milestones
  - Tier-specific perks: priority class booking, guest passes, exclusive classes
- **Referral program** (enhanced from sales module):
  - Member receives unique referral link and QR code
  - Referred friend signs up → both parties earn points + configurable bonus (credit, discount, gift)
  - Referral tracking dashboard for staff
  - Referral leaderboard (optional gamification)
- **Gamification:**
  - Streak tracking: consecutive days/weeks of check-ins
  - Badges for milestones (first check-in, 100 check-ins, 1 year member, etc.)
  - Members view badges and streaks in the app
- **Loyalty dashboard for staff:**
  - Points issued this month
  - Rewards redeemed this month
  - Top loyalty members
  - Program ROI estimate

### 16. OPEN API & PARTNER MARKETPLACE
- Full REST Open API:
  - Tenant-scoped base URL: `https://{tenant}.open-api.platform.com/v1`
  - API key authentication (`x-api-key` header)
  - Endpoints for: customers, memberships, check-ins, classes, bookings, payments
  - Rate limiting per API key (configurable per tier)
  - Response pagination, filtering, sorting on all list endpoints
  - `Accept-Language` header support for localised error messages
  - Full OpenAPI 3 specification auto-generated and served at `/v1/api-docs`
- Webhook system:
  - Outbound webhooks: partner registers URL, platform POSTs events (member.created, checkin.completed, payment.failed, booking.created, etc.)
  - Retry logic with exponential backoff (via RabbitMQ dead letter queue)
  - Webhook event log with delivery status
- Partner/integration management:
  - Marketplace UI in admin dashboard listing available integrations
  - Enable/disable per-tenant with per-integration config
  - Partner categories: marketing, finance, member apps, smart equipment, body measurement, access control
- Connect API for bi-directional data sync: master data, course bookings, training results

### 17. ANALYTICS & REPORTING
- Studio dashboard (per facility):
  - Today's check-ins (live counter)
  - Current occupancy (live)
  - New members this month
  - Revenue this month vs last month
  - Upcoming classes today
  - Outstanding payments count
- Revenue reports:
  - Monthly recurring revenue (MRR) trend
  - Revenue by membership tier
  - Revenue by facility
  - Payment success rate
  - Outstanding receivables aging
- Member reports:
  - Active member count over time
  - New member acquisition trend
  - Member churn rate and cancellation reasons
  - Retention cohort analysis
- Check-in analytics:
  - Peak hours heatmap (hour × day of week)
  - Check-ins per facility over time
  - Most active members
- Class analytics:
  - Attendance rate per class
  - Most/least popular classes
  - Trainer utilisation
- Lead & sales reports:
  - Lead-to-member conversion rate
  - Sales funnel by stage
  - Referral program performance
- Export all reports as CSV and PDF

### 18. PLATFORM-LEVEL SAAS ONBOARDING & SUPER ADMIN

This is the **meta-layer** of the platform — how new gyms sign up, self-onboard, manage their subscription, and how the platform operator (you) administers everything. Accessible only to `SUPER_ADMIN` role and to gym owners during their own onboarding flow.

**Self-service gym onboarding:**
- Public signup page: gym name, owner name, email, password, country, number of active members
- On signup: tenant automatically provisioned (new PostgreSQL schema created, seed config applied), welcome email sent, 30-day free trial started — zero manual intervention required
- Onboarding wizard (step-by-step, can't be skipped):
  1. Studio profile setup: name, logo, address, timezone, opening hours, phone, website
  2. First facility creation
  3. Membership tier creation (guided: at least one tier required)
  4. Payment method setup: connect Stripe account (Stripe Connect), configure GoCardless for SEPA
  5. Invite first staff member (optional)
  6. Connect first access device (optional, skippable)
  7. Done — redirect to main dashboard with checklist of remaining optional steps
- Onboarding progress checklist visible in dashboard until all steps complete
- In-app tips and contextual help during onboarding

**Subscription & billing tiers:**
- Define platform pricing tiers (e.g. Starter, Professional, Ultimate) with:
  - Max active member count per tier
  - Features enabled/disabled per tier (e.g. Open API only on Ultimate)
  - Monthly price per tier
  - Price per additional partner integration (e.g. €39/month each)
- Gym owner can view current plan, usage (member count vs limit), and billing history in settings
- **Auto-upgrade logic:** when active member count exceeds current tier limit, automatically upgrade tenant to next tier, charge difference, notify owner via email
- **Trial management:**
  - 30-day free trial on signup, all features unlocked
  - Trial countdown banner in dashboard
  - Day 25: warning email — "5 days left on your trial"
  - Day 30: trial ends, prompt to enter payment details or account locked
  - Grace period: 3 days after trial end before full lockout
- Subscription managed via **Stripe Billing** (recurring monthly charges to gym owner's card)
- Gym owner can upgrade, downgrade, or cancel subscription from settings
- On cancellation: account remains active until end of billing period, then locks (data retained for 90 days)
- Invoice for platform subscription emailed to gym owner monthly

**Super admin dashboard (SUPER_ADMIN only, separate URL `/superadmin`):**
- **Tenant management:**
  - List all tenants with: name, plan tier, member count, MRR, trial status, created date, last login
  - Search and filter tenants by plan, status, country, member count
  - View any tenant's full account (impersonate for support purposes, logged in audit trail)
  - Manually upgrade/downgrade/suspend/delete a tenant
  - Extend trial period for a specific tenant
  - Reset a tenant's password or send login link
- **Platform revenue dashboard:**
  - Total platform MRR across all tenants
  - New tenants this month (trial starts)
  - Trial-to-paid conversion rate
  - Churn rate (cancellations this month)
  - Revenue by pricing tier
  - Top 10 tenants by member count and by MRR
- **Feature flag management:**
  - Enable or disable specific features per tenant (override tier defaults)
  - Roll out new features to a subset of tenants (beta group)
- **Global announcement system:**
  - Post a platform-wide maintenance notice or feature announcement
  - Shown as a banner in all tenant dashboards
- **Platform health:**
  - RabbitMQ queue depths
  - Failed job counts (payment runs, email sends)
  - API error rates across all tenants
  - Active WebSocket connections
- **Support tools:**
  - View any tenant's audit log
  - Manually trigger a payment run for a tenant
  - Manually re-send a failed notification
  - View and replay failed webhook events

**Tenant settings (gym owner):**
- Studio profile: name, logo, address, contact, timezone, locale, currency
- Branding: primary colour, font, email header/footer design
- Subscription & billing: current plan, usage meter, payment method, invoice history, upgrade/downgrade/cancel
- Integrations: manage enabled partner integrations, add/remove, configure API keys per integration
- API access: generate and manage Open API keys, view usage per key
- Webhook management: register partner URLs, view event log
- User management: invite staff, assign roles, deactivate accounts
- Notification settings: configure which automated notifications are active
- Data & privacy: GDPR data export, member erasure requests, data retention policy
### 19. HARDWARE INTEGRATIONS

- `AccessControlAdapter` interface:
  - `checkIn(memberId, deviceId)` → returns CheckInResult
  - `checkOut(memberId, deviceId)`
  - `getDeviceStatus(deviceId)`
- Concrete implementations:
  - HTTP REST adapter (for IP-connected devices)
  - MQTT adapter (for IoT devices)
  - **Gantner adapter** — concrete implementation for Gantner access control hardware (doors, turnstiles, RFID readers) used by Kieser Training
- **seca BIA scale integration:**
  - `BodyCompositionAdapter` interface for seca body composition analyzers
  - Auto-push measurement results (weight, body fat %, muscle mass, BMI, etc.) to member profile
  - Measurement history per member with trend charts
- Device manager UI: register devices, assign to facility, configure QR/RFID mode
- Supported device types: door/gate controller, turnstile, RFID reader, QR scanner, vending machine, body composition scale
- Event monitor: real-time stream of all device events with error code display

### 20. KIESER MACHINE SENSOR INTEGRATION (KTAG-specific)

Kieser Training uses proprietary machines with computer-assisted biofeedback (LE Lumbar Extension, CE Cervical Extension, etc.). The platform must ingest and display sensor data from these machines.

- **Machine Sensor Adapter:**
  - `MachineSensorAdapter` interface: `getMachineStatus(machineId)`, `getSessionData(machineId, sessionId)`, `streamLiveData(machineId)`
  - Concrete implementation for Kieser LE/CE sensor protocol
  - Real-time data ingestion: force curves, range of motion, repetition counts, isometric hold times
- **Strength measurement (Kraftmessung):**
  - Isometric and dynamic force measurement recording
  - Store measurement results linked to member + exercise + session
  - Progress tracking: force output over time per exercise/muscle group
  - Comparison view: initial assessment vs. current vs. target
- **Machine-member session linking:**
  - When a member starts a machine session, sensor data auto-links to their active TrainingSession
  - TrainingLog extended with: `sensorData` (JSON), `peakForce`, `avgForce`, `rangeOfMotion`, `timeUnderTension`
- **Machine inventory management:**
  - Machine entity: serial number, model, facility, installation date, last maintenance, status, firmware version
  - Machine maintenance scheduling and history
  - Machine utilization analytics per facility
- **Kieser machine data:** 44 proprietary machines defined in `/kieser.json` with full details

### 21. APPOINTMENT & AGENDA SYSTEM

1-on-1 appointment booking separate from group class scheduling. Required for personal training sessions, assessments, physiotherapy (KTAG).

- **Appointment types:** personal training, initial assessment, anamnesis, follow-up, physiotherapy, consultation
- **Appointment entity:** member, staff/trainer, facility, room (optional), type, start/end time, status (SCHEDULED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED/NO_SHOW), notes, recurring rule
- **Staff agenda / day plan view:**
  - Daily calendar view per staff member showing all appointments + classes
  - Weekly overview with drag-and-drop rescheduling
  - Availability management: staff set available time slots
- **Member booking:**
  - Members book available slots via portal/app
  - Booking confirmation + reminder notifications
  - Cancellation with configurable cut-off
- **Anamnese (health assessment) form builder:**
  - Configurable questionnaire forms (questions, types: text/number/choice/scale/date)
  - Store completed assessments linked to member + appointment
  - Assessment results feed into training plan recommendations

### 22. INTERNATIONALIZATION (i18n)

KTAG operates across DACH (Germany, Austria, Switzerland) + Luxembourg. Full internationalization required.

- **Application languages:** German (de), French (fr), English (en)
- **Backend:**
  - `Accept-Language` header support on all API responses
  - Localized error messages and validation messages
  - Message bundles: `messages_de.properties`, `messages_fr.properties`, `messages_en.properties`
- **Frontend:**
  - react-i18next integration with language switcher
  - All UI strings externalized to translation files
  - Locale-aware formatting: dates (DD.MM.YYYY for DE), currencies (CHF/EUR), numbers (1.000,00 vs 1,000.00)
- **Correspondence templates:** multi-language template variants (DE/FR/EN) per template, auto-select based on member locale preference
- **Country-specific features:**
  - Switzerland: QR-Rechnung, LSV, CHF currency, Tarif 595
  - Germany: SEPA, EUR, TSE, DATEV, E-Rechnung (ZUGFeRD/XRechnung)
  - Austria: SEPA, EUR
  - Luxembourg: SEPA, EUR, FR/DE bilingual

### 23. SWISS PAYMENT METHODS

Swiss-specific payment integrations required for KTAG (headquartered in Switzerland).

- **QR-Rechnung (QR-bill):**
  - Generate Swiss QR invoices per ISO 20022 standard
  - QR code with structured payment data (IBAN, amount, reference)
  - PDF invoice with QR payment slip section
  - Support for QR-IBAN + Creditor Reference (SCOR)
- **LSV/LSV+ (Lastschriftverfahren — Swiss direct debit):**
  - LSV mandate management: create, sign, activate, cancel
  - Batch collection file generation (LSV+ format)
  - Integration with Swiss clearing system (SIX SIC/euroSIC)
  - Return debit handling
- **EBICS (Electronic Banking Internet Communication Standard):**
  - Bank communication for automated payment file upload/download
  - pain.001 (credit transfer), pain.008 (direct debit), camt.053 (account statement)

### 24. ENTERPRISE AUTH & COMPLIANCE

- **Azure SSO (SAML/OIDC):**
  - Spring Security SAML2 or OIDC integration alongside existing JWT auth
  - Azure AD tenant configuration per gym tenant
  - Auto-provision user on first SSO login, map Azure AD groups to platform roles
  - Fallback to username/password for non-SSO users
- **DATEV export:**
  - Export invoices, payments, and journal entries in DATEV format (ASCII CSV)
  - DATEV Unternehmen Online compatible
  - Configurable account mapping (Kontenrahmen SKR03/SKR04)
  - Scheduled or on-demand export with date range filter
- **TSE (Technische Sicherheitseinrichtung):**
  - German fiscal security device integration for cash register/POS transactions
  - Transaction signing and audit-proof storage
  - DSFinV-K export format
- **E-Rechnung:**
  - ZUGFeRD 2.x (PDF/A-3 with embedded XML)
  - XRechnung (pure XML, Peppol BIS compliant)
  - Auto-generate on invoice creation based on recipient country/preference
- **IDW PS 880 / NIS2:** compliance documentation and security controls (phased)

### 25. CONTRACT AUTO-RENEWAL & LIFECYCLE EXTENSIONS

- **Auto-renewal logic:**
  - Configurable per membership tier: auto-renew yes/no, renewal term, renewal notice period
  - Scheduled job: detect contracts approaching end date, auto-extend if renewal conditions met
  - Member notification before renewal (configurable days before)
  - Opt-out window: member can cancel before renewal date
- **Credit notes (Gutschriften):**
  - Generate credit note documents linked to original invoice
  - Credit balance per member, auto-apply to next invoice or manual refund
- **Split invoices:**
  - Split invoice across multiple payers (e.g., employer + employee, family members)
  - Configurable split ratios per contract

### 26. DATA MIGRATION & LEGACY IMPORT

- **Import tooling for KTAG's existing system (Bestandsystem):**
  - CSV/JSON import endpoints for: members, contracts, invoices, payment history, training data
  - Validation + error reporting on import
  - Dry-run mode: preview import results before committing
  - Idempotent imports: re-runnable without duplicating data
  - Field mapping configuration per source system

---

## DATABASE — CORE ENTITIES

Design a complete PostgreSQL schema. Key tables (build all relationships):

```
tenants, facilities, users, roles, permissions,
members, member_notes, member_documents, member_tags,
contracts, contract_tiers, membership_tiers, idle_periods,
check_ins, access_devices, access_events, access_restrictions,
classes, class_schedules, class_bookings, waitlist_entries,
exercises, training_plans, training_plan_exercises, training_sessions, training_logs,
invoices, payments, payment_methods, dunning_runs, dunning_levels,
leads, lead_activities, lead_stages,
employees, shifts, time_entries, employee_facilities,
communication_templates, notification_rules, sent_messages,
campaigns, campaign_recipients, campaign_events,
promo_codes, promo_code_usages, referrals, vouchers,
loyalty_points, loyalty_transactions, loyalty_rewards, loyalty_redemptions,
loyalty_tiers, loyalty_badges, member_badges, member_streaks,
public_studio_profiles, public_bookings, public_offers,
face_recognition_events,
platform_plans, platform_plan_features, tenant_subscriptions,
tenant_invoices, tenant_usage_snapshots, feature_flags,
onboarding_progress, platform_announcements, support_audit_log,
api_keys, webhook_registrations, webhook_events,
audit_logs,
machines, machine_maintenance_logs, machine_sensor_sessions,
strength_measurements, body_composition_measurements,
appointments, appointment_types, staff_availability,
anamnese_forms, anamnese_questions, anamnese_submissions, anamnese_answers,
credit_notes, invoice_splits,
import_jobs, import_mappings, import_errors
```

Every table must have: `id` (UUID), `created_at`, `updated_at`, `tenant_id` (FK to tenants except on tenants table itself).

---

## NON-FUNCTIONAL REQUIREMENTS

- **All API responses** follow a consistent envelope: `{ "data": ..., "meta": ..., "errors": [] }`
- **All list endpoints** support pagination (`page`, `size`), sorting (`sort`, `direction`), and filtering
- **Audit logging**: every create/update/delete on member, contract, payment, and access records writes to `audit_logs`
- **GDPR**: soft-delete all member data (mark deleted, anonymise PII on erasure request), right-to-export endpoint
- **Validation**: all inputs validated server-side with meaningful error messages
- **Error handling**: global exception handler returning structured error responses
- **Timezone**: all datetimes stored as UTC, converted to facility timezone on display
- **Localisation**: API supports `de`, `en`, `fr`, `es`, `it`, `nl` languages
- **Security**: HTTPS only, CORS configured, SQL injection prevented by JPA, XSS headers set, rate limiting on auth endpoints
- **Docker Compose**: running `docker-compose up` starts PostgreSQL, Redis, RabbitMQ, backend, and frontend — fully working local environment with seed data

---

## SEED DATA

On first run, seed:
- 1 demo tenant (`demo-gym`)
- 3 facilities (main location, second location, franchise location)
- 4 membership tiers (Basic €29/mo, Standard €49/mo, Premium €79/mo, VIP €129/mo)
- 50 demo members with realistic names, mix of active/frozen/cancelled contracts
- 500 exercises in the exercise library with categories and muscle groups
- 10 class templates across 5 categories
- 5 staff users (owner, manager, 2 trainers, receptionist)
- 30 days of historical check-in data
- Sample invoices and payment history
- 20 leads in various pipeline stages
- Sample communication templates (welcome email, birthday, payment reminder, appointment reminder)
- Sample notification rules

---

## START HERE

Build in this exact order:

1. `docker-compose.yml` — PostgreSQL + Redis + RabbitMQ + backend + frontend, all wired together
2. Spring Boot project with all dependencies, `application.yml`, Liquibase baseline migration
3. Multi-tenancy infrastructure — `TenantContext`, tenant resolver, schema routing, auto-provisioning on signup
4. **Platform onboarding & super admin** — public signup, trial management, Stripe Billing for platform subscriptions, auto-upgrade logic, `/superadmin` dashboard, tenant management, feature flags, platform revenue metrics
5. Auth module — register, login, JWT issue/refresh, RBAC, Spring Security config
6. Onboarding wizard — 7-step guided setup flow for new gyms, progress checklist in dashboard
7. Core entities — Liquibase migrations for all tables listed above
8. Member module — full CRUD, search, profile management
9. Contract module — membership tiers, contract lifecycle, freeze, cancel
10. Check-in module — manual check-in, hardware adapter layer, WebSocket occupancy
11. Finance module — Stripe + GoCardless integration, payment runs, invoicing, dunning
12. Class booking module — scheduling, online booking, waitlist
13. Training module — exercise library, drag-and-drop plan builder, progress tracking, goals, drafts
14. Communication module — templates, notification rules, RabbitMQ consumers
15. Sales module — leads, pipeline, digital contract sales flow
16. Staff module — employees, shifts, time tracking
17. Multi-location — cross-facility features, consolidated dashboard
18. Member self-service portal (React `/portal` route)
19. Staff & trainer floor app mode (React Native role-aware staff view)
20. Public studio discovery & booking portal (public-facing, no login required)
21. Marketing tools — campaigns, audience builder, activity reminders, at-risk detection
22. Loyalty & rewards program — points, redemption, tiers, badges, streaks, referral program (Kunden werben Kunden)
23. Appointment & agenda system — 1-on-1 bookings, staff day plan, anamnese form builder
24. Kieser machine sensor integration — LE/CE biofeedback, strength measurement, machine inventory
25. Internationalization (i18n) — DE/FR/EN UI + correspondence, locale-aware formatting
26. Swiss payment methods — QR-Rechnung, LSV/LSV+, EBICS
27. Enterprise auth & compliance — Azure SSO, DATEV export, TSE, E-Rechnung
28. Contract lifecycle extensions — auto-renewal, credit notes, split invoices
29. Open API + webhook system
30. Analytics & reporting dashboards with CSV/PDF export
31. Data migration & legacy import tooling
32. Mobile app (React Native + Expo) — member self-service + staff floor mode in one app, role-aware navigation, EAS build config
33. Staff & trainer floor app mode (React Native role-aware staff view)
34. Seed data script — full spec (500 exercises, Kieser machines, sample payment history)
35. Platform SaaS onboarding — self-service signup, trial, Stripe Billing
36. Super admin dashboard — /superadmin tenant management, feature flags

Build each module completely before moving to the next. Do not leave TODOs. Do not use placeholder implementations. Every endpoint must work.
