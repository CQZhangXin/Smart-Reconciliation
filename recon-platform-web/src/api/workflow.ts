import { get, post, put } from './request'
import type { WfProcessDefinition, WfApprovalRecord, PageResult, PageQuery } from '@/types'

/** 分页查询流程定义 */
export function pageProcessDef(params: PageQuery): Promise<PageResult<WfProcessDefinition>> {
  return get('/workflow/process-def/page', params)
}

/** 创建流程定义 */
export function createProcessDef(data: WfProcessDefinition): Promise<WfProcessDefinition> {
  return post('/workflow/process-def', data)
}

/** 发布流程定义 */
export function publishProcessDef(id: number): Promise<WfProcessDefinition> {
  return put(`/workflow/process-def/${id}/publish`)
}

/** 提交审批 */
export function submitApproval(data: Record<string, any>): Promise<WfApprovalRecord> {
  return post('/workflow/approval/submit', data)
}

/** 审批操作 */
export function approve(id: number, data: Record<string, any>): Promise<WfApprovalRecord> {
  return put(`/workflow/approval/${id}/approve`, data)
}

/** 查询我的审批 */
export function pageMyApprovals(params: PageQuery): Promise<PageResult<WfApprovalRecord>> {
  return get('/workflow/approval/my', params)
}

/** 分页查询审批记录 */
export function pageApprovals(params: PageQuery): Promise<PageResult<WfApprovalRecord>> {
  return get('/workflow/approval/page', params)
}
