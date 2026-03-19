export interface ApiResponse<T> {
  data: T
  meta?: PageMeta
  errors?: ApiError[]
}

export interface PageMeta {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiError {
  code: string
  message: string
}

export interface UserDto {
  id: string
  email: string
  firstName: string
  lastName: string
  role: Role
}

export type Role =
  | 'SUPER_ADMIN'
  | 'STUDIO_OWNER'
  | 'MANAGER'
  | 'TRAINER'
  | 'MEMBER'
  | 'RECEPTIONIST'

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserDto
}

export interface MemberDto {
  id: string
  memberNumber: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  dateOfBirth?: string
  gender?: string
  street?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  profilePhotoUrl?: string
  healthNotes?: string
  status: 'ACTIVE' | 'INACTIVE' | 'DELETED'
  joinDate?: string
  createdAt: string
}

export interface ContractDto {
  id: string
  memberId: string
  membershipTierId: string
  membershipTierName: string
  status: 'ACTIVE' | 'PAUSED' | 'PENDING_CANCELLATION' | 'CANCELLED' | 'EXPIRED'
  startDate: string
  endDate?: string
  nextBillingDate?: string
  monthlyAmount: number
  discountCode?: string
  cancellationDate?: string
  cancellationEffectiveDate?: string
  cancellationReason?: string
  autoRenew: boolean
}

export interface MembershipTierDto {
  id: string
  name: string
  description?: string
  monthlyPrice: number
  billingCycle: 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  minimumTermMonths: number
  noticePeriodDays: number
  classAllowance?: number
  accessRules?: string
  active: boolean
}

export interface InvoiceDto {
  id: string
  memberId: string
  contractId?: string
  invoiceNumber: string
  amount: number
  vatAmount?: number
  totalAmount: number
  currency: string
  status: 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'CANCELLED'
  issuedAt: string
  dueDate: string
  paidAt?: string
  pdfUrl?: string
}

export interface CheckInDto {
  id: string
  memberId: string
  memberName?: string
  memberNumber?: string
  deviceId?: string
  deviceName?: string
  method: 'MANUAL' | 'RFID' | 'QR'
  status: 'SUCCESS' | 'DENIED'
  denialReason?: string
  staffId?: string
  checkInTime: string
  checkOutTime?: string
}

export interface OccupancyDto {
  currentCount: number
  maxCapacity?: number
  atCapacity: boolean
}

export interface AccessDeviceDto {
  id: string
  name: string
  deviceType: string
  mode: string
  locationDescription?: string
  ipAddress?: string
  apiEndpoint?: string
  maxOccupancy?: number
  active: boolean
  lastHeartbeatAt?: string
}

export interface AccessEventDto {
  id: string
  deviceId?: string
  deviceName?: string
  memberId?: string
  memberName?: string
  eventType: string
  reasonCode?: string
  details?: string
  createdAt: string
}

export interface AccessRestrictionDto {
  id: string
  memberId: string
  reason: string
  description?: string
  active: boolean
  restrictedAt: string
  releasedAt?: string
}

// Booking module types

export interface ClassCategoryDto {
  id: string
  name: string
  description?: string
  color?: string
  active: boolean
  createdAt: string
}

export interface ClassDefinitionDto {
  id: string
  name: string
  description?: string
  categoryId?: string
  categoryName?: string
  trainerId?: string
  trainerName?: string
  room?: string
  capacity: number
  durationMinutes: number
  virtualLink?: string
  allowWaitlist: boolean
  bookingCutoffMinutes: number
  cancellationCutoffMinutes: number
  allowTrial: boolean
  active: boolean
  createdAt: string
}

export interface ClassScheduleDto {
  id: string
  classId: string
  className?: string
  categoryName?: string
  categoryColor?: string
  trainerId?: string
  trainerName?: string
  startTime: string
  endTime: string
  room?: string
  capacity: number
  bookedCount: number
  waitlistCount: number
  virtualLink?: string
  cancelled: boolean
  cancellationReason?: string
  recurrenceRule?: string
  recurrenceGroupId?: string
  createdAt: string
}

export interface ClassBookingDto {
  id: string
  scheduleId: string
  className?: string
  classStartTime?: string
  memberId?: string
  memberName?: string
  guestName?: string
  guestEmail?: string
  guestPhone?: string
  status: 'CONFIRMED' | 'CANCELLED' | 'ATTENDED' | 'NO_SHOW' | null
  bookedAt: string
  cancelledAt?: string
  attendanceMarkedAt?: string
}

export interface WaitlistEntryDto {
  id: string
  scheduleId: string
  className?: string
  classStartTime?: string
  memberId: string
  memberName?: string
  position: number
  status: 'WAITING' | 'PROMOTED' | 'EXPIRED' | 'CANCELLED'
  joinedAt: string
  promotedAt?: string
}

// Training module types

export type ExerciseType =
  | 'STRENGTH'
  | 'CARDIO'
  | 'FLEXIBILITY'
  | 'BALANCE'
  | 'PLYOMETRIC'
  | 'BODYWEIGHT'
  | 'MACHINE'
  | 'FREE_WEIGHT'
  | 'CABLE'
  | 'RESISTANCE_BAND'
  | 'FUNCTIONAL'
  | 'STRETCHING'

export type MuscleGroup =
  | 'CHEST'
  | 'BACK'
  | 'SHOULDERS'
  | 'BICEPS'
  | 'TRICEPS'
  | 'FOREARMS'
  | 'ABS'
  | 'OBLIQUES'
  | 'LOWER_BACK'
  | 'QUADRICEPS'
  | 'HAMSTRINGS'
  | 'GLUTES'
  | 'CALVES'
  | 'HIP_FLEXORS'
  | 'ADDUCTORS'
  | 'ABDUCTORS'
  | 'TRAPS'
  | 'LATS'
  | 'FULL_BODY'
  | 'CARDIO'

export type TrainingPlanStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export type GoalType =
  | 'WEIGHT_LOSS'
  | 'MUSCLE_GAIN'
  | 'STRENGTH'
  | 'ENDURANCE'
  | 'FLEXIBILITY'
  | 'GENERAL_FITNESS'
  | 'SPORT_SPECIFIC'
  | 'REHABILITATION'
  | 'BODY_COMPOSITION'
  | 'CUSTOM'

export type GoalStatus = 'ACTIVE' | 'ACHIEVED' | 'ABANDONED'

export interface ExerciseDto {
  id: string
  name: string
  description?: string
  exerciseType: ExerciseType
  primaryMuscleGroup: MuscleGroup
  secondaryMuscleGroup?: MuscleGroup
  equipment?: string
  videoUrl?: string
  thumbnailUrl?: string
  executionTips?: string
  postureNotes?: string
  difficultyLevel?: string
  active: boolean
  global: boolean
  createdAt: string
}

export interface TrainingPlanExerciseDto {
  id: string
  exerciseId: string
  exerciseName?: string
  exerciseThumbnailUrl?: string
  primaryMuscleGroup?: string
  sortOrder: number
  sets: number
  reps: number
  weight?: number
  restSeconds?: number
  trainerComment?: string
  supersetGroup?: number
}

export interface TrainingPlanDto {
  id: string
  name: string
  description?: string
  memberId?: string
  memberName?: string
  trainerId?: string
  trainerName?: string
  status: TrainingPlanStatus
  template: boolean
  catalog: boolean
  category?: string
  estimatedDurationMinutes?: number
  difficultyLevel?: string
  exerciseCount: number
  exercises?: TrainingPlanExerciseDto[]
  createdAt: string
  updatedAt: string
}

export interface TrainingSessionDto {
  id: string
  memberId: string
  memberName?: string
  planId?: string
  planName?: string
  startedAt: string
  finishedAt?: string
  durationMinutes?: number
  notes?: string
  rating?: number
  logs?: TrainingLogDto[]
  createdAt: string
}

export interface TrainingLogDto {
  id: string
  sessionId: string
  exerciseId: string
  exerciseName?: string
  planExerciseId?: string
  setNumber: number
  targetReps?: number
  actualReps?: number
  targetWeight?: number
  actualWeight?: number
  durationSeconds?: number
  notes?: string
  completed: boolean
  createdAt: string
}

// Communication module types

export type ChannelType = 'EMAIL' | 'SMS' | 'PUSH' | 'LETTER' | 'WHATSAPP'

export type MessageStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'OPENED'

export type TriggerEvent =
  | 'BIRTHDAY'
  | 'NEW_MEMBERSHIP'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_SUCCESS'
  | 'APPOINTMENT_REMINDER'
  | 'CONTRACT_EXPIRY'
  | 'CONTRACT_CANCELLATION'
  | 'TRIAL_BOOKED'
  | 'MEMBERSHIP_ANNIVERSARY'
  | 'CHECKIN_MILESTONE'
  | 'CLASS_REMINDER'
  | 'TRAINING_PLAN_PUBLISHED'
  | 'WELCOME'
  | 'CUSTOM'

export type DelayDirection = 'BEFORE' | 'AFTER' | 'IMMEDIATE'

export interface CommunicationTemplateDto {
  id: string
  name: string
  channelType: ChannelType
  subject?: string
  bodyHtml?: string
  bodyText?: string
  category?: string
  locale?: string
  logoUrl?: string
  brandColor?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface NotificationRuleDto {
  id: string
  name: string
  triggerEvent: TriggerEvent
  templateId: string
  templateName?: string
  channelType: ChannelType
  delayDays: number
  delayDirection: DelayDirection
  description?: string
  active: boolean
  createdAt: string
}

export interface SentMessageDto {
  id: string
  memberId: string
  memberName?: string
  templateId?: string
  templateName?: string
  channelType: ChannelType
  subject?: string
  bodyPreview?: string
  recipientAddress?: string
  status: MessageStatus
  sentAt?: string
  deliveredAt?: string
  failedAt?: string
  openedAt?: string
  errorMessage?: string
  triggerEvent?: string
  createdAt: string
}

export interface MessageStatsDto {
  totalSent: number
  delivered: number
  failed: number
  opened: number
  pending: number
}

// Sales module types

export type LeadSource =
  | 'WEBSITE' | 'WALK_IN' | 'REFERRAL' | 'SOCIAL_MEDIA'
  | 'GOOGLE_ADS' | 'META_ADS' | 'PARTNER' | 'PHONE' | 'EVENT' | 'OTHER'

export type LeadActivityType =
  | 'CALL' | 'EMAIL' | 'VISIT' | 'NOTE' | 'TASK' | 'SMS'
  | 'MEETING' | 'TRIAL_BOOKED' | 'PROPOSAL_SENT' | 'CONTRACT_SIGNED'

export type DiscountType = 'PERCENTAGE' | 'FIXED'

export interface LeadStageDto {
  id: string
  name: string
  sortOrder: number
  color?: string
  isDefault: boolean
  isClosed: boolean
  isWon: boolean
  leadCount: number
}

export interface LeadDto {
  id: string
  firstName: string
  lastName: string
  email?: string
  phone?: string
  source: LeadSource
  interest?: string
  stageId: string
  stageName?: string
  stageColor?: string
  assignedStaffId?: string
  assignedStaffName?: string
  notes?: string
  convertedMemberId?: string
  referralMemberId?: string
  activityCount: number
  createdAt: string
  updatedAt: string
}

export interface LeadActivityDto {
  id: string
  leadId: string
  activityType: LeadActivityType
  description?: string
  outcome?: string
  staffId?: string
  staffName?: string
  dueDate?: string
  completedAt?: string
  createdAt: string
}

export interface PromoCodeDto {
  id: string
  code: string
  description?: string
  discountType: DiscountType
  discountValue: number
  expiresAt?: string
  maxUsages?: number
  currentUsages: number
  active: boolean
  expired: boolean
  exhausted: boolean
  createdAt: string
}

export interface SalesPipelineDto {
  stages: LeadStageDto[]
  totalLeads: number
  convertedLeads: number
  conversionRate: number
}

// Staff module types

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'FREELANCE' | 'INTERN'

export type ShiftStatus = 'SCHEDULED' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

export interface EmployeeDto {
  id: string
  userId?: string
  firstName: string
  lastName: string
  email?: string
  phone?: string
  role?: string
  employmentType: EmploymentType
  position?: string
  hourlyRate?: number
  monthlySalary?: number
  hireDate?: string
  terminationDate?: string
  competencies?: string
  active: boolean
  facilityIds?: string[]
  createdAt: string
}

export interface ShiftDto {
  id: string
  employeeId: string
  employeeName?: string
  facilityId?: string
  startTime: string
  endTime: string
  status: ShiftStatus
  notes?: string
  durationMinutes: number
  createdAt: string
}

export interface TimeEntryDto {
  id: string
  employeeId: string
  employeeName?: string
  shiftId?: string
  clockIn: string
  clockOut?: string
  breakMinutes?: number
  totalMinutes?: number
  notes?: string
  createdAt: string
}

export interface ShiftReportDto {
  employeeId: string
  employeeName?: string
  scheduledMinutes: number
  actualMinutes: number
  difference: number
  shiftCount: number
  timeEntryCount: number
}

// Facility / Multi-location types

export interface FacilityDto {
  id: string
  name: string
  description?: string
  street?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  timezone?: string
  phone?: string
  email?: string
  websiteUrl?: string
  openingHours?: string
  logoUrl?: string
  brandColor?: string
  bannerImageUrl?: string
  maxOccupancy?: number
  parentFacilityId?: string
  parentFacilityName?: string
  active: boolean
  memberCount: number
  employeeCount: number
  childFacilities?: FacilityDto[]
  createdAt: string
}

export interface FacilityConfigDto {
  id: string
  facilityId: string
  configKey: string
  configValue?: string
  description?: string
}

export interface MemberFacilityAccessDto {
  id: string
  memberId: string
  memberName?: string
  facilityId: string
  facilityName?: string
  homeFacility: boolean
  crossFacilityAccess: boolean
}

export interface FacilitySummaryDto {
  facility: FacilityDto
  activeMembers: number
  checkInsToday: number
  newMembersThisMonth: number
  revenueThisMonth: number
  currentOccupancy: number
}

export interface ConsolidatedDashboardDto {
  totalFacilities: number
  totalActiveMembers: number
  totalCheckInsToday: number
  totalNewMembersThisMonth: number
  totalRevenueThisMonth: number
  totalOutstandingPayments: number
  facilitySummaries: FacilitySummaryDto[]
}

export interface TrainingGoalDto {
  id: string
  memberId: string
  goalType: GoalType
  title: string
  description?: string
  targetValue?: number
  currentValue?: number
  unit?: string
  startDate?: string
  targetDate?: string
  status: GoalStatus
  progressPercent?: number
  createdAt: string
  updatedAt: string
}

// ── Marketing ──────────────────────────────────────────

export type CampaignStatus = 'DRAFT' | 'SCHEDULED' | 'SENDING' | 'SENT' | 'CANCELLED'
export type CampaignType = 'EMAIL' | 'SMS' | 'PUSH'
export type CampaignEventType = 'SENT' | 'DELIVERED' | 'OPENED' | 'CLICKED' | 'CONVERTED' | 'FAILED' | 'BOUNCED'
export type RiskLevel = 'HIGH' | 'MEDIUM' | 'LOW'

export interface CampaignDto {
  id: string
  name: string
  description?: string
  campaignType: CampaignType
  status: CampaignStatus
  templateId?: string
  templateName?: string
  subject?: string
  bodyHtml?: string
  bodyText?: string
  audienceCriteria?: string
  scheduledAt?: string
  sentAt?: string
  totalRecipients?: number
  sentCount?: number
  deliveredCount?: number
  openedCount?: number
  clickedCount?: number
  failedCount?: number
  convertedCount?: number
  deliveryRate?: number
  openRate?: number
  clickRate?: number
  createdBy?: string
  createdAt: string
  updatedAt: string
}

export interface CampaignRecipientDto {
  id: string
  campaignId: string
  memberId: string
  memberName?: string
  memberEmail?: string
  recipientAddress?: string
  status?: CampaignEventType
  sentAt?: string
  deliveredAt?: string
  openedAt?: string
  clickedAt?: string
  errorMessage?: string
  createdAt: string
}

export interface CampaignStatsDto {
  totalCampaigns: number
  activeCampaigns: number
  totalSent: number
  totalDelivered: number
  totalOpened: number
  totalClicked: number
  totalFailed: number
  avgDeliveryRate: number
  avgOpenRate: number
  avgClickRate: number
}

export interface AudienceCriteria {
  memberStatuses?: string[]
  minCheckInFrequencyDays?: number
  maxCheckInFrequencyDays?: number
  noCheckInDays?: number
  tags?: string[]
  facilityIds?: string[]
  contractStatus?: string
  contractExpiresWithinDays?: number
  joinedAfter?: string
  joinedBefore?: string
  gender?: string
  minAge?: number
  maxAge?: number
}

export interface AudiencePreviewDto {
  totalCount: number
  sample: { memberId: string; firstName: string; lastName: string; email: string; status: string }[]
}

export interface AtRiskMemberDto {
  memberId: string
  firstName: string
  lastName: string
  email?: string
  phone?: string
  memberStatus: string
  contractStatus: string
  lastCheckIn?: string
  daysSinceLastCheckIn: number
  avgWeeklyVisits: number
  visitTrend: number
  riskLevel: RiskLevel
  riskReason: string
}
