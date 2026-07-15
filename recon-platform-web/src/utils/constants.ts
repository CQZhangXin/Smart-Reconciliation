// ============ 任务类型 ============

export const TASK_TYPE_LABEL: Record<string, string> = {
  BANK: '银行对账',
  THIRD_PAYMENT: '三方支付对账',
  AR: '应收对账',
  AP: '应付对账',
  INTERNAL: '内部对账',
  CROSS_SYSTEM: '跨系统对账',
  DAILY: '日对账',
  MONTHLY: '月对账',
  CUSTOM: '自定义'
}

// ============ 任务状态 ============

export const TASK_STATUS_LABEL: Record<string, string> = {
  PENDING: '待执行',
  RUNNING: '运行中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

export const TASK_STATUS_COLOR: Record<string, string> = {
  PENDING: 'info',
  RUNNING: 'warning',
  COMPLETED: 'success',
  FAILED: 'danger'
}

// ============ 匹配状态 ============

export const MATCH_STATUS_LABEL: Record<string, string> = {
  AUTO_CONFIRMED: '自动确认',
  PENDING_REVIEW: '待审核',
  MANUAL_CONFIRMED: '人工确认',
  REJECTED: '已驳回'
}

export const MATCH_STATUS_COLOR: Record<string, string> = {
  AUTO_CONFIRMED: 'success',
  PENDING_REVIEW: 'warning',
  MANUAL_CONFIRMED: 'info',
  REJECTED: 'danger'
}

// ============ 差异分类 ============

export const DISCREPANCY_CATEGORY_LABEL: Record<string, string> = {
  TIME_DIFF: '时间差',
  FEE_DIFF: '手续费差异',
  EXCHANGE_DIFF: '汇率差异',
  DATA_ENTRY_ERROR: '录入错误',
  UNREACHED: '未达账项',
  DUPLICATE: '重复记账',
  OTHER_SIDE_UNRECORDED: '对方未入账',
  UNKNOWN: '未知差异'
}

export const DISCREPANCY_CATEGORY_COLOR: Record<string, string> = {
  TIME_DIFF: 'info',
  FEE_DIFF: 'warning',
  EXCHANGE_DIFF: 'warning',
  DATA_ENTRY_ERROR: 'danger',
  UNREACHED: '',
  DUPLICATE: 'danger',
  OTHER_SIDE_UNRECORDED: '',
  UNKNOWN: 'info'
}

// ============ 风险等级 ============

export const RISK_LEVEL_LABEL: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  CRITICAL: '严重'
}

export const RISK_LEVEL_COLOR: Record<string, string> = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'danger'
}

// ============ 数据源类型 ============

export const DATA_SOURCE_TYPE_LABEL: Record<string, string> = {
  BANK_API: '银行API',
  THIRD_PAYMENT: '第三方支付',
  ERP: 'ERP系统',
  FILE_IMPORT: '文件导入',
  DATABASE: '数据库直连',
  HTTP_API: 'HTTP接口',
  MANUAL: '手工录入',
  // 兼容旧值
  BANK: '银行接口',
  FILE: '文件导入',
  API: 'API对接'
}

export const CUSTOM_RECON_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '启用',
  INACTIVE: '停用'
}

export const CUSTOM_RECON_STATUS_COLOR: Record<string, string> = {
  DRAFT: 'info',
  ACTIVE: 'success',
  INACTIVE: 'danger'
}

// ============ 差异状态 ============

export const DISCREPANCY_STATUS_LABEL: Record<string, string> = {
  OPEN: '待处理',
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
}

export const DISCREPANCY_STATUS_COLOR: Record<string, string> = {
  OPEN: 'warning',
  PENDING: 'warning',
  IN_PROGRESS: 'info',
  PROCESSING: 'info',
  RESOLVED: 'success',
  CLOSED: ''
}

// ============ 下拉选项 ============

export const TASK_TYPE_OPTIONS = [
  { label: '银行对账', value: 'BANK' },
  { label: '三方支付对账', value: 'THIRD_PAYMENT' },
  { label: '应收对账', value: 'AR' },
  { label: '应付对账', value: 'AP' },
  { label: '内部对账', value: 'INTERNAL' },
  { label: '跨系统对账', value: 'CROSS_SYSTEM' },
  { label: '自定义对账', value: 'CUSTOM' }
]

export const MATCH_TYPE_OPTIONS = [
  { label: '精准匹配', value: 'EXACT' },
  { label: '规则匹配', value: 'RULE' },
  { label: 'AI语义匹配', value: 'AI_SEMANTIC' },
  { label: '拆单匹配', value: 'AI_SPLIT' }
]
