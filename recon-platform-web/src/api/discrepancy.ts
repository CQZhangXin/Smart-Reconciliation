import { get, post, put } from './request'
import type { ReconDiscrepancy, ReconAdjustment, PageResult, PageQuery } from '@/types'

// ========== 差异管理 ==========

/** 分页查询差异记录 */
export function pageDiscrepancy(params: PageQuery): Promise<PageResult<ReconDiscrepancy>> {
  return get('/discrepancy/page', params)
}

/** 根据ID查询差异 */
export function getDiscrepancy(id: number): Promise<ReconDiscrepancy> {
  return get(`/discrepancy/${id}`)
}

/** AI智能分类 */
export function classifyDiscrepancy(id: number): Promise<ReconDiscrepancy> {
  return post(`/discrepancy/${id}/classify`)
}

/** 批量AI分类 */
export function batchClassify(taskId: number): Promise<void> {
  return post('/discrepancy/batch-classify', null, { params: { taskId } })
}

/** 分配处理人 */
export function assignDiscrepancy(id: number, handlerId: number, handlerName: string): Promise<ReconDiscrepancy> {
  return put(`/discrepancy/${id}/assign`, null, { params: { handlerId, handlerName } })
}

/** 解决差异 */
export function resolveDiscrepancy(id: number, body: Record<string, any>): Promise<ReconDiscrepancy> {
  return put(`/discrepancy/${id}/resolve`, body)
}

/** 关闭差异 */
export function closeDiscrepancy(id: number, resolvedBy: number): Promise<ReconDiscrepancy> {
  return put(`/discrepancy/${id}/close`, null, { params: { resolvedBy } })
}

/** 查询SLA超期差异 */
export function listSlaOverdue(orgId: number): Promise<ReconDiscrepancy[]> {
  return get('/discrepancy/sla-overdue', { orgId })
}

/** 查询差异统计 */
export function getDiscrepancyStats(taskId: number): Promise<Record<string, number>> {
  return get('/discrepancy/stats', { taskId })
}

// ========== 调整管理 ==========

/** 分页查询调整记录 */
export function pageAdjustment(params: PageQuery): Promise<PageResult<ReconAdjustment>> {
  return get('/discrepancy/adjustment/page', params)
}

/** 创建调整 */
export function createAdjustment(data: ReconAdjustment): Promise<ReconAdjustment> {
  return post('/discrepancy/adjustment', data)
}

/** 审批调整 */
export function approveAdjustment(id: number, approvedBy: number): Promise<ReconAdjustment> {
  return put(`/discrepancy/adjustment/${id}/approve`, null, { params: { approvedBy } })
}
