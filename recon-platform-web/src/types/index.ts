// ============ 通用类型 ============

/** 统一API响应 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页结果 */
export interface PageResult<T = any> {
  total: number
  page: number
  size: number
  records: T[]
}

/** 分页查询参数 */
export interface PageQuery {
  page: number
  size: number
  [key: string]: any
}

// ============ 数据源 ============

export interface DataSource {
  id?: number
  ledgerId?: number
  dsName: string
  dsType: string
  dsCategory: string      // SOURCE_A / SOURCE_B
  provider?: string
  connConfig?: Record<string, any>
  fieldMapping?: Record<string, string>
  syncStrategy?: string   // FULL / INCREMENTAL / MANUAL / SCHEDULED
  syncCron?: string
  lastSyncAt?: string
  lastSyncStatus?: string
  healthStatus?: string   // HEALTHY / UNHEALTHY / UNKNOWN
  status: string           // ACTIVE / INACTIVE
  description?: string
  createdBy?: number
  updatedBy?: number
  createdAt?: string
  updatedAt?: string
}

// ============ 规则引擎 ============

export interface ReconRuleConfig {
  id?: number
  ruleName: string
  ruleCode: string
  ruleType: string         // RULE_MATCH / AI_MATCH / HYBRID
  templateType?: string
  matchConfig?: MatchConfigItem[]
  priority?: number
  tolerance?: ToleranceConfig
  aiConfig?: AIConfig
  status: string           // DRAFT / ACTIVE / INACTIVE
  description?: string
  version?: number
  createdBy?: number
  updatedBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface MatchConfigItem {
  field: string
  operator: string         // eq / contains / fuzzy / range
  value?: any
  weight?: number
}

export interface ToleranceConfig {
  amountAbs?: number       // 金额绝对容差
  amountPct?: number       // 金额百分比容差
  dateDays?: number        // 日期容差天数
}

export interface AIConfig {
  enabled: boolean
  model?: string
  threshold?: number
}

// ============ 对账任务 ============

export interface ReconTask {
  id?: number
  ledgerId?: number
  taskName: string
  taskType: 'BANK' | 'THIRD_PAYMENT' | 'AR' | 'AP' | 'INTERNAL' | 'CROSS_SYSTEM' | 'DAILY' | 'MONTHLY' | 'CUSTOM'
  sourceAId?: number
  sourceBId?: number
  ruleIds?: number[]
  reconPeriod?: string
  periodStart?: string
  periodEnd?: string
  status?: string          // PENDING / RUNNING / COMPLETED / FAILED
  totalACount?: number
  totalBCount?: number
  matchedCount?: number
  unmatchedCount?: number
  discrepancyCount?: number
  matchRate?: number
  matchSummary?: Record<string, any>
  errorMsg?: string
  priority?: string        // HIGH / MEDIUM / LOW
  startedAt?: string
  completedAt?: string
  durationMs?: number
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

// ============ 自定义对账 ============

export interface CustomReconDefinition {
  id?: number
  orgId?: number
  ledgerId?: number
  defName: string
  defCode: string
  description?: string
  sourceAId: number
  sourceBIds: number[]
  ruleIds?: number[]
  matchLayers?: {
    exact?: boolean
    rule?: boolean
    ai?: boolean
    split?: boolean
  }
  periodType?: string
  defaultPeriod?: string
  status?: string
  lastRunTaskId?: number
  lastRunAt?: string
  createdBy?: number
  updatedBy?: number
  createdAt?: string
  updatedAt?: string
}

export interface CustomReconValidateResult {
  valid: boolean
  errors: string[]
  warnings: string[]
  sourceAInfo?: Record<string, any>
  sourceBInfoList?: Record<string, any>[]
  activeRuleCount: number
  pendingRecordA: number
  pendingRecordB: number
}

export interface CustomReconRunRequest {
  reconPeriod?: string
  periodStart?: string
  periodEnd?: string
  async?: boolean
  ruleIds?: number[]
  sourceBIds?: number[]
}

export interface CustomReconRunResult {
  definitionId: number
  defName: string
  primaryTaskId: number
  taskIds: number[]
  async: boolean
  message: string
}

// ============ 自然语言创建对账方案 ============

/** 自然语言解析请求 */
export interface NLParseRequest {
  description: string
  orgId: number
}

/** 自然语言解析响应 */
export interface NLParseResult {
  definition: CustomReconDefinition
  unresolvedSources: string[]
  unresolvedRules: string[]
  aiExplanation: string
}

// ============ 自然语言创建流程定义 ============

/** 自然语言解析流程定义请求 */
export interface NLWorkflowParseRequest {
  description: string
}

/** 流程步骤项（AI返回） */
export interface NLProcessStepItem {
  order: number
  name: string
  approverRole: string
}

/** 自然语言解析的流程定义 */
export interface NLProcessDefData {
  processName: string
  processKey: string
  description: string
  steps: NLProcessStepItem[]
  aiExplanation: string
}

/** 自然语言解析流程定义响应 */
export interface NLWorkflowParseResult {
  definition: NLProcessDefData
  warning?: string
}

// ============ 匹配结果 ============

export interface ReconMatch {
  id?: number
  taskId: number
  orgId?: number
  recordAId: number
  recordBId: number
  matchType: string        // EXACT / RULE / AI / MANUAL
  confidence?: number
  matchDimensions?: Record<string, number>
  aiExplanation?: string
  amountA?: number
  amountB?: number
  amountDiff?: number
  dateDiffDays?: number
  status: string           // PENDING_REVIEW / CONFIRMED / REJECTED
  reviewedBy?: number
  reviewedAt?: string
  reviewComment?: string
  createdAt?: string
  updatedAt?: string
}

// ============ 差异管理 ============

export interface ReconDiscrepancy {
  id?: number
  taskId: number
  recordId: number
  relatedRecordId?: number
  side?: string            // A_ONLY / B_ONLY / DIFF
  category?: string        // TIME_DIFF / FEE_DIFF / EXCHANGE_DIFF / UNKNOWN
  aiRootCause?: string
  aiSuggestion?: Record<string, any>
  amount?: number
  amountDiff?: number
  currency?: string
  riskLevel?: string       // HIGH / MEDIUM / LOW
  handlerId?: number
  handlerName?: string
  status: string           // OPEN / IN_PROGRESS / RESOLVED / CLOSED
  resolution?: string
  resolutionNote?: string
  resolvedBy?: number
  resolvedAt?: string
  slaDeadline?: string
  createdAt?: string
  updatedAt?: string
}

export interface ReconAdjustment {
  id?: number
  discrepancyId: number
  ledgerId?: number
  adjustmentType: string   // DEBIT / CREDIT / REVERSAL
  amount: number
  currency?: string
  debitAccount?: string
  creditAccount?: string
  description?: string
  attachmentUrls?: string[]
  status: string           // PENDING / APPROVED / REJECTED
  approvedBy?: number
  approvedAt?: string
  createdBy?: number
  createdAt?: string
  updatedAt?: string
}

// ============ AI服务 ============

export interface MatchScoreRequest {
  recordAText: string
  recordBText: string
  amountA?: number
  amountB?: number
  dateA?: string
  dateB?: string
  partyA?: string
  partyB?: string
  descriptionA?: string
  descriptionB?: string
  refA?: string
  refB?: string
  currency?: string
  accountType?: string
}

export interface MatchScoreResult {
  overallScore: number
  amountScore: number
  dateScore: number
  partyScore: number
  descriptionScore: number
  referenceScore: number
  explanation: string
  recommendedAction: string  // AUTO_CONFIRM / RECOMMEND / UNMATCHED
  modelUsed: string
  tokensUsed: number
  latencyMs: number
}

export interface DiscrepancyClassifyRequest {
  recordId?: number
  amountA?: number
  amountB?: number
  amountDiff?: number
  currency?: string
  dateA?: string
  dateB?: string
  descriptionA?: string
  descriptionB?: string
  partyA?: string
  partyB?: string
  transactionRefA?: string
  transactionRefB?: string
  direction?: string
  accountType?: string
  crossBorder?: boolean
  hasFee?: boolean
}

export interface RootCauseRequest {
  discrepancyId: number
  category?: string
  recordAJson?: string
  recordBJson?: string
  accountType?: string
  currency?: string
  industryType?: string
}

export interface RootCauseResult {
  rootCauseCategory: string
  analysisSteps: string
  suggestion: string
  riskLevel: string
  suggestedAdjustment?: string
  confidence: number
  modelUsed: string
  tokensUsed: number
}

export interface NLQueryResult {
  question: string
  generatedSql: string
  data: Record<string, any>[]
  answer: string
  extractedEntities: Record<string, any>
  queryTimeMs: number
}

export interface RuleGenerationResult {
  ruleName: string
  ruleCode: string
  ruleType: string
  matchConfigJson: string
  toleranceJson: string
  explanation: string
  estimatedMatchRate: number
  hasConflict: boolean
  conflictDetail?: string
}

export interface FieldMappingSuggestion {
  sourceField: string
  targetField: string
  confidence: number
  fieldType: string
  formatHint?: string
  explanation: string
}

export interface ReportGenerateRequest {
  orgId: number
  taskId?: number
  reportType: string       // DAILY / MONTHLY / QUARTERLY
  period: string
  matchCount?: number
  discrepancyCount?: number
  matchRate?: number
  trendData?: Array<any>
  language?: string        // zh_CN / en_US
}

// ============ 工作流 ============

export interface WfProcessDefinition {
  id?: number
  processName: string
  processKey: string
  description?: string
  steps?: ProcessStep[]
  status?: string          // DRAFT / PUBLISHED / DISABLED
  createdAt?: string
  updatedAt?: string
}

export interface ProcessStep {
  order: number
  name: string
  approverRole?: string
  approverId?: number
}

export interface WfApprovalRecord {
  id?: number
  orgId: number
  businessType: string
  businessId: number
  processDefId: number
  currentStep?: number
  status: string           // PENDING / APPROVED / REJECTED
  approverId?: number
  approverName?: string
  action?: string
  comment?: string
  createdAt?: string
  updatedAt?: string
}

// ============ 系统管理 ============

export interface SysUser {
  id?: number
  orgId?: number
  username: string
  /** @deprecated password is only present in request payloads for user creation, never in API responses. */
  password?: string
  realName?: string
  email?: string
  phone?: string
  status: string
  roleIds?: number[]
  createdAt?: string
  updatedAt?: string
}

export interface SysRole {
  id?: number
  orgId?: number
  roleName: string
  roleCode: string
  description?: string
  status?: string
  permIds?: number[]
  createdAt?: string
}

export interface SysPermission {
  id?: number
  permName: string
  permCode: string
  permType: string         // MENU / BUTTON / API
  parentId?: number
  path?: string
  icon?: string
  sort?: number
  children?: SysPermission[]
}

export interface SysAuditLog {
  id?: number
  orgId?: number
  userId?: number
  username?: string
  module: string
  action: string
  detail?: string
  ipAddress?: string
  createdAt?: string
}

export interface OrgOrganization {
  id?: number
  orgName: string
  orgCode: string
  contactName?: string
  contactPhone?: string
  status?: string
  createdAt?: string
}

export interface OrgAccount {
  id?: number
  ledgerId?: number
  orgId?: number
  accountName: string
  accountCode: string
  currency?: string
  accountType?: string
  status?: string
  createdAt?: string
}

// ============ 许可证 ============

export interface LicenseStatus {
  licensed: boolean
  licenseEnabled: boolean
  status: 'UNLICENSED' | 'ACTIVE' | 'EXPIRED' | 'EXPIRING_SOON' | 'INVALID' | 'DISABLED'
  orgName?: string
  orgCode?: string
  maxUsers?: number
  expireDate?: string
  remainingDays?: number
  features?: string[]
  issuedAt?: string
  message?: string
}

// ============ 仪表盘 ============

export interface DashboardData {
  taskStats: {
    total: number
    running: number
    completed: number
    failed: number
  }
  matchStats: {
    totalMatched: number
    matchRate: number
    avgConfidence: number
  }
  discrepancyStats: {
    total: number
    open: number
    slaOverdue: number
    resolved: number
  }
  recentTasks: ReconTask[]
  categoryDistribution: Record<string, number>
}

export interface TrendDataItem {
  period: string
  taskCount: number
  matchRate: number
  discrepancyCount: number
}

export interface HealthMetrics {
  taskId: number
  overallScore: number
  matchQuality: number
  resolutionRate: number
  slaCompliance: number
  suggestions: string[]
}
