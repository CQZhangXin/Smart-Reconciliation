import { get, post } from './request'
import type { DashboardData, TrendDataItem, HealthMetrics, NLQueryResult, ReportGenerateRequest } from '@/types'

/** 获取仪表盘数据 */
export function getDashboard(orgId: number): Promise<DashboardData> {
  return get('/analytics/dashboard', { orgId })
}

/** 获取任务健康度 */
export function getTaskHealth(taskId: number): Promise<HealthMetrics> {
  return get('/analytics/health', { taskId })
}

/** 获取趋势数据 */
export function getTrend(orgId: number, months?: string): Promise<TrendDataItem[]> {
  return get('/analytics/trend', { orgId, months })
}

/** 环比分析 */
export function comparePeriods(orgId: number, period1: string, period2: string): Promise<Record<string, any>> {
  return post('/analytics/compare', { orgId, period1, period2 })
}

/** 生成报告 */
export function generateReport(data: ReportGenerateRequest): Promise<string> {
  return post('/analytics/report', data)
}

/** 自然语言查询 */
export function nlQuery(question: string, orgId: number): Promise<NLQueryResult> {
  return post('/analytics/nl-query', { question, orgId })
}
