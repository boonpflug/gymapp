# Kieser Training AG (KTAG) — Plattform-Demo & Anforderungsabgleich

**Erstellt für:** Planungsmeeting mit KTAG IT Project Team
**Datum:** 2026-03-25
**Quelle:** "Anforderungen an eine Studioverwaltungssoftware" (Feb 2026, CONFIDENTIAL)

---

## Demo-Zugang

| | |
|---|---|
| **URL** | https://demo.arcwright.dev |
| **E-Mail** | kieser@demo.arcwright.dev |
| **Passwort** | KieserDemo2026 |
| **Rolle** | Studio Owner (voller Zugriff) |
| **Sprache** | Deutsch (umschaltbar auf EN/FR im Menü) |

> Die Demo enthält Beispieldaten: 50 Mitglieder, 3 Standorte (Berlin/München), Verträge, Check-ins, Kurse, Trainingspläne, Kieser-Maschinen, Treueprogramm und mehr.

---

## Skalierung / Nutzergruppen

KTAG operates at significant scale:
- **~50** backoffice/admin users (controlling, finance, marketing)
- **~2,000** operational staff (studio employees & managers)
- **~250,000** customers/patients (via app/platform)

---

## Requirement-by-Requirement Comparison

### 1. Training
**Requirement:** Trainingsplan, Trainingssteuerung, Trainingsdokumentation, Erfolgskontrollen, Anamnese, Kraftmessung

| Sub-requirement | Status | Notes |
|---|---|---|
| Training plans (create, publish, templates, catalog) | DONE | Full plan builder with exercises, sets/reps/weight, draft/publish/archive |
| Training control (Trainingssteuerung) | DONE | Session start/finish, exercise logging with target vs actual |
| Training documentation | DONE | TrainingSession + TrainingLog with full history per member |
| Success tracking (Erfolgskontrollen) | DONE | Progress history per exercise, goals with target/current tracking |
| Anamnese (health assessment) | PARTIAL | Entity mentioned in spec but no dedicated form builder yet |
| Kraftmessung (strength measurement) | GAP | No force/strength measurement integration — needed for Kieser machine sensorics (LE/CE biofeedback) |

### 2. CRM
**Requirement:** Kundenonboarding, Kundenstammdaten, Interessenten, Korrespondenz, Serienbriefe

| Sub-requirement | Status | Notes |
|---|---|---|
| Customer onboarding | DONE | Member creation, public portal trial booking, lead conversion |
| Customer master data (Stammdaten) | DONE | Full member profiles with contact, address, emergency contact, tags, notes |
| Prospects/leads (Interessenten) | DONE | Lead pipeline with stages, activities, conversion tracking |
| Correspondence | DONE | Communication templates (email/SMS/push), notification rules, send history |
| Serial letters (Serienbriefe) | PARTIAL | Campaign bulk email/SMS exists, but no **PDF letter generation** (physical post/print) |

### 3. Vertragsmanagement (Contract Management)
**Requirement:** Gesamter Lebenszyklus: Anlegen, Mutieren, Kündigen, (Automatisch) Verlängern

| Sub-requirement | Status | Notes |
|---|---|---|
| Create contracts | DONE | Contract creation with membership tier, billing cycle, amounts |
| Modify/mutate contracts | DONE | Update, freeze/unfreeze with billing adjustment |
| Cancel contracts | DONE | Cancellation with notice period enforcement, withdrawal option |
| Auto-renewal | GAP | No automatic renewal logic implemented yet |

### 4. Schnittstellen / Integrationen (Interfaces & Integrations)
**Requirement:** SMS, E-Mail, Post, seca (BIA Waage), Zutrittskontrolle (Gantner), Kieser Maschinen Sensorik (LE/CE), Website, EBICS, Azure-SSO, DATEV, ERP, Datentransfer vom Bestandsystem, Service-Center, Tarif 595, Physiotherapie

| Sub-requirement | Status | Notes |
|---|---|---|
| SMS | DONE | Channel type supported in communication module (Twilio) |
| E-Mail | DONE | Template-based email via notification rules |
| Post (physical mail) | GAP | No PDF letter generation or postal dispatch integration |
| seca BIA Waage (body composition scale) | GAP | Adapter interface pattern exists but no seca-specific implementation |
| Zutrittskontrolle / Gantner | PARTIAL | Generic AccessControlAdapter with HTTP REST + MQTT, but no Gantner-specific adapter |
| Kieser Maschinen Sensorik (LE/CE) | GAP | **Critical for Kieser** — no machine sensor data ingestion, no biofeedback integration |
| Website integration | DONE | Public studio portal with trial booking, contact forms |
| EBICS (banking) | GAP | No EBICS payment interface — we use Stripe/GoCardless |
| Azure SSO | GAP | No SSO/SAML/OIDC integration — JWT-only auth |
| DATEV (accounting export) | GAP | No DATEV export format |
| ERP integration | GAP | No ERP connector/API |
| Legacy data transfer (Bestandsystem) | GAP | No data migration/import tooling |
| Service-Center integration | GAP | Not implemented |
| Tarif 595 (Swiss insurance billing) | GAP | No Swiss health insurance billing |
| Physiotherapie (physio integration) | GAP | Scope TBD per KTAG's own note |

### 5. Studiomanagement (Studio Management)
**Requirement:** Personen-, Schicht- und Ressourcenplanung, Tagesplan, Termine managen, Agenda, Maschineninventar, Personalisiertes Zutrittsmedium (RFID-Karte)

| Sub-requirement | Status | Notes |
|---|---|---|
| Staff/shift planning | DONE | Employee profiles, shift scheduling, weekly schedule |
| Resource planning | PARTIAL | Rooms in class scheduling, but no general resource/room booking |
| Day plan / agenda | GAP | No daily agenda/appointment calendar view for staff |
| Appointment management (Termine) | GAP | No 1-on-1 appointment booking system (only group classes) |
| Machine inventory (Maschineninventar) | GAP | **Important for Kieser** — no equipment/machine inventory management |
| RFID access medium | DONE | Access devices with RFID/QR support, personalized check-in |

### 6. Finanzen (Finance)
**Requirement:** Rechnungen, Mahnungen inkl. Inkasso, Gutschriften, automatisierter Zahlungsverkehr (QR-Rechnung und LSV), Abgrenzungsmodelle, GOAv Abrechnungen, Split-Rechnungen

| Sub-requirement | Status | Notes |
|---|---|---|
| Invoices (Rechnungen) | DONE | Auto-generated invoices with VAT |
| Dunning incl. collections (Mahnungen/Inkasso) | DONE | Multi-level dunning escalation with notifications |
| Credits (Gutschriften) | GAP | No credit note / refund document generation |
| QR-Rechnung (Swiss QR invoice) | GAP | **Swiss-specific** — no QR-bill format (ISO 20022) |
| LSV (Swiss direct debit) | GAP | **Swiss-specific** — no LSV/LSV+ integration, we use GoCardless SEPA |
| Revenue recognition (Abgrenzungsmodelle) | GAP | No deferred revenue / accrual accounting |
| GOAv billing (medical fee schedule) | GAP | No medical/physio billing codes |
| Split invoices | GAP | No split invoice support |

### 7. Controlling
**Requirement:** Analyse, Reporting, Kennzahlen, Dashboards, (Roh-)Daten Exporte, Sales Performance/Funnel Analysen

| Sub-requirement | Status | Notes |
|---|---|---|
| Analysis & reporting | PARTIAL | Consolidated dashboard exists, campaign stats, at-risk detection |
| KPIs & dashboards | PARTIAL | Basic stats in dashboard, but no dedicated analytics module |
| Raw data exports | GAP | No CSV/PDF export functionality |
| Sales performance/funnel | PARTIAL | Pipeline summary with conversion rate exists |

### 8. Kunden-App (Customer App)
**Requirement:** Trainingsplan, Dokumente, Termin Management, Vertragsverlängerungen, Push Nachrichten

| Sub-requirement | Status | Notes |
|---|---|---|
| Training plan in app | PARTIAL | Web portal has training plans; **native mobile app not built** |
| Documents | GAP | No document viewing/download in portal |
| Appointment management | GAP | No appointment booking in portal (only classes) |
| Contract renewals | GAP | No self-service renewal/extension |
| Push notifications | GAP | Architecture ready (FCM channel type) but no native app to receive them |

### 9. Compliance
**Requirement:** Berechtigungskonzept, DSG/DSGVO, Revisionskonformität, Barrierefreiheit, IDW PS 880, NIS2, TSE, E-Rechnung

| Sub-requirement | Status | Notes |
|---|---|---|
| Authorization concept (RBAC) | DONE | 6 roles with method-level @PreAuthorize |
| DSG / DSGVO (data protection) | PARTIAL | Soft-delete, audit logging, but no GDPR erasure/export endpoints |
| Audit trail (Revisionskonformität) | DONE | AuditLog entity on all mutations |
| Accessibility (Barrierefreiheit) | GAP | No WCAG/accessibility compliance work done |
| IDW PS 880 (software audit standard) | GAP | No compliance documentation |
| NIS2 (cybersecurity directive) | GAP | No NIS2 compliance measures |
| TSE (fiscal security device, Germany) | GAP | No TSE/cash register integration |
| E-Rechnung (e-invoicing, ZUGFeRD/XRechnung) | GAP | No structured electronic invoice format |

### 10. Franchise
**Requirement:** Mandantenfähigkeit, globaler Produkt-/Vertragskatalog, Gebührenabrechnung, konsolidiertes Reporting

| Sub-requirement | Status | Notes |
|---|---|---|
| Multi-tenancy (Mandantenfähigkeit) | DONE | Schema-per-tenant, full isolation |
| Global product/contract catalog | GAP | No cross-tenant shared catalog |
| Fee billing (franchise fees) | GAP | No franchise fee calculation or billing |
| Consolidated reporting | PARTIAL | Consolidated dashboard exists but limited to single tenant's facilities |

### 11. Internationalisierung (Internationalization)
**Requirement:** Verfügbarkeit DACH & Luxemburg, Applikations- und Korrespondenzsprachen: Deutsch, Französisch, Englisch

| Sub-requirement | Status | Notes |
|---|---|---|
| DACH + Luxembourg availability | GAP | No locale-specific features (Swiss payments, German TSE, Austrian specifics) |
| App language: DE, FR, EN | GAP | No i18n framework — UI is English-only |
| Correspondence language: DE, FR, EN | GAP | Templates are not multi-language |

### 12. Marketing & Sales
**Requirement:** Artikel, Dienstleistung, Gutscheine, Marketing Automatisation, Kooperationsmanagement, Promotion/Kampagnen, Loyalitätsprogramm, Kunden werben Kunden

| Sub-requirement | Status | Notes |
|---|---|---|
| Products/services (Artikel) | GAP | No product catalog / POS |
| Vouchers (Gutscheine) | GAP | Promo codes exist but no purchasable gift vouchers |
| Marketing automation | DONE | Campaigns, audience builder, scheduling, at-risk detection |
| Cooperation management | GAP | No partner/cooperation management |
| Promotions/campaigns | DONE | Full campaign module with performance tracking |
| Loyalty program | GAP | **Not yet built** — next on roadmap (step 22) |
| Referral program (Kunden werben Kunden) | GAP | Referral link in spec but not implemented |

---

## Summary Scorecard

| KTAG Module | Coverage | Rating |
|---|---|---|
| Training | Strong core, gaps in Anamnese & Kraftmessung | PARTIAL |
| CRM | Well covered, missing physical mail | GOOD |
| Contract Management | Missing auto-renewal | GOOD |
| Integrations | **Major gaps** — Gantner, seca, EBICS, Azure SSO, DATEV, Kieser sensors | WEAK |
| Studio Management | Missing appointments, machine inventory, day plan | PARTIAL |
| Finance | **Major gaps** — Swiss payments (QR/LSV), credits, split invoices, GOAv | WEAK |
| Controlling | Basic dashboards exist, no exports or deep analytics | PARTIAL |
| Customer App | Web portal exists, **no native mobile app** | WEAK |
| Compliance | RBAC + audit done, **many regulatory gaps** (TSE, NIS2, DSG, IDW PS 880) | WEAK |
| Franchise | Multi-tenancy done, missing global catalog & franchise billing | PARTIAL |
| Internationalization | **Not started** — no i18n, no DACH-specific features | GAP |
| Marketing & Sales | Campaigns done, missing loyalty, vouchers, referrals | PARTIAL |

---

## Top Priority Gaps for KTAG

These are the items most critical to Kieser's business that we currently don't cover:

### Must-Have (Blockers)
1. **Kieser machine sensor integration (LE/CE)** — core to their training model
2. **Swiss payment methods** — QR-Rechnung + LSV (they're a Swiss company)
3. **i18n (DE/FR/EN)** — operating across DACH + Luxembourg
4. **Azure SSO** — enterprise auth requirement
5. **Auto-renewal of contracts** — basic contract lifecycle gap
6. **DATEV export** — German accounting standard
7. **Appointment/booking system** — 1-on-1 training sessions, not just group classes

### Should-Have (Important)
8. **Gantner access control adapter** — their specific hardware vendor
9. **seca BIA scale integration** — body composition measurement
10. **Machine inventory management** — tracking ~44 proprietary machines per studio
11. **Native mobile app** — 250k customers expect an app
12. **Loyalty program** — explicitly required, next on our roadmap
13. **TSE / E-Rechnung** — German fiscal compliance
14. **Data migration tooling** — they have an existing system (Bestandsystem)

### Nice-to-Have (Later)
15. Physical mail/letter generation
16. Split invoices, credit notes
17. Cooperation management
18. GOAv medical billing
19. IDW PS 880 / NIS2 compliance documentation
20. EBICS banking interface

---

## Recommendation for the Meeting

**We cover ~50-60% of KTAG's requirements today.** The core modules (training, CRM, contracts, members, staff, multi-tenancy) are solid. The biggest gaps are:

1. **Switzerland-specific** — payments (QR-Rechnung, LSV), Tarif 595, i18n
2. **Kieser-specific** — machine sensorics, strength measurement, machine inventory
3. **Enterprise-grade** — Azure SSO, DATEV, compliance certifications, data migration
4. **Missing modules** — appointments, native mobile app, loyalty, analytics exports

Suggest framing the meeting around:
- Which integrations are **Day 1 launch blockers** vs. phased rollout
- Whether KTAG can pilot with a subset of studios before full DACH rollout
- Clarifying the "Physiotherapie" integration scope (they flagged it as TBD)
- Timeline expectations for the native mobile app (250k users)
