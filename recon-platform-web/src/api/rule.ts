import { get, post, put, del } from './request'
import type { ReconRuleConfig, PageResult, PageQuery, RuleGenerationResult } from '@/types'

/** 分页查询规则 */
export function pageRule(params: PageQuery): Promise<PageResult<ReconRuleConfig>> {
  return get('/rule/page', params)
}

/** 根据ID查询规则 */
export function getRule(id: number): Promise<ReconRuleConfig> {
  return get(`/rule/${id}`)
}

/** 创建规则 */
export function createRule(data: ReconRuleConfig): Promise<ReconRuleConfig> {
  return post('/rule', data)
}

/** 更新规则 */
export function updateRule(id: number, data: ReconRuleConfig): Promise<ReconRuleConfig> {
  return put(`/rule/${id}`, data)
}

/** 删除规则 */
export function deleteRule(id: number): Promise<void> {
  return del(`/rule/${id}`)
}

/** 启用规则 */
export function enableRule(id: number): Promise<ReconRuleConfig> {
  return put(`/rule/${id}/enable`)
}

/** 禁用规则 */
export function disableRule(id: number): Promise<ReconRuleConfig> {
  return put(`/rule/${id}/disable`)
}

/** 查询活跃规则 */
export function listActiveRules(orgId: number): Promise<ReconRuleConfig[]> {
  return get('/rule/active', { orgId })
}

/** 自然语言生成规则 */
export function generateRuleFromNL(description: string, orgId?: number): Promise<RuleGenerationResult> {
  return post('/rule/generate-from-nl', { description, orgId: orgId?.toString() })
}

/** 保存AI生成的规则 */
export function saveGeneratedRule(data: Record<string, any>): Promise<ReconRuleConfig> {
  return post('/rule/save-generated', data)
}
