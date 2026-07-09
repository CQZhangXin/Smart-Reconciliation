import { get, post, put, del } from './request'
import type { ReconTask, ReconMatch, PageResult, PageQuery } from '@/types'

// ========== 任务管理 ==========

/** 分页查询对账任务 */
export function pageTask(params: PageQuery): Promise<PageResult<ReconTask>> {
  return get('/recon/task/page', params)
}

/** 根据ID查询任务 */
export function getTask(id: number): Promise<ReconTask> {
  return get(`/recon/task/${id}`)
}

/** 查询任务汇总 */
export function getTaskSummary(id: number): Promise<ReconTask> {
  return get(`/recon/task/${id}/summary`)
}

/** 创建任务 */
export function createTask(data: ReconTask): Promise<ReconTask> {
  return post('/recon/task', data)
}

/** 更新任务 */
export function updateTask(id: number, data: ReconTask): Promise<ReconTask> {
  return put(`/recon/task/${id}`, data)
}

/** 删除任务 */
export function deleteTask(id: number): Promise<void> {
  return del(`/recon/task/${id}`)
}

/** 同步执行任务 */
export function executeTask(id: number): Promise<ReconTask> {
  return post(`/recon/task/${id}/execute`)
}

/** 异步执行任务 */
export function executeTaskAsync(id: number): Promise<string> {
  return post(`/recon/task/${id}/execute-async`)
}

// ========== 匹配结果管理 ==========

/** 分页查询匹配结果 */
export function pageMatch(params: PageQuery): Promise<PageResult<ReconMatch>> {
  return get('/recon/match/page', params)
}

/** 确认匹配 */
export function confirmMatch(id: number, reviewedBy: number): Promise<ReconMatch> {
  return put(`/recon/match/${id}/confirm`, { reviewedBy })
}

/** 拒绝匹配 */
export function rejectMatch(id: number, reviewedBy: number, comment?: string): Promise<ReconMatch> {
  return put(`/recon/match/${id}/reject`, { reviewedBy, comment })
}

/** 查询待审核匹配 */
export function listPendingReview(taskId: number): Promise<ReconMatch[]> {
  return get('/recon/match/pending-review', { taskId })
}
