import { get, post, put, del } from './request'
import type {
  CustomReconDefinition,
  CustomReconValidateResult,
  CustomReconRunRequest,
  CustomReconRunResult,
  NLParseRequest,
  NLParseResult,
  PageResult,
  PageQuery
} from '@/types'

/** 分页查询自定义对账方案 */
export function pageCustomRecon(params: PageQuery): Promise<PageResult<CustomReconDefinition>> {
  return get('/custom-recon/definition/page', params)
}

/** 查询方案详情 */
export function getCustomRecon(id: number): Promise<CustomReconDefinition> {
  return get(`/custom-recon/definition/${id}`)
}

/** 创建方案 */
export function createCustomRecon(data: CustomReconDefinition): Promise<CustomReconDefinition> {
  return post('/custom-recon/definition', data)
}

/** 更新方案 */
export function updateCustomRecon(id: number, data: Partial<CustomReconDefinition>): Promise<CustomReconDefinition> {
  return put(`/custom-recon/definition/${id}`, data)
}

/** 删除方案 */
export function deleteCustomRecon(id: number): Promise<void> {
  return del(`/custom-recon/definition/${id}`)
}

/** 启用方案 */
export function enableCustomRecon(id: number): Promise<CustomReconDefinition> {
  return put(`/custom-recon/definition/${id}/enable`)
}

/** 停用方案 */
export function disableCustomRecon(id: number): Promise<CustomReconDefinition> {
  return put(`/custom-recon/definition/${id}/disable`)
}

/** 预检方案 */
export function validateCustomRecon(id: number): Promise<CustomReconValidateResult> {
  return post(`/custom-recon/definition/${id}/validate`)
}

/** 执行方案 */
export function runCustomRecon(id: number, data?: CustomReconRunRequest): Promise<CustomReconRunResult> {
  return post(`/custom-recon/definition/${id}/run`, data || {})
}

/** 自然语言解析对账方案 */
export function nlParseCustomRecon(data: NLParseRequest): Promise<NLParseResult> {
  return post('/custom-recon/definition/nl-parse', data)
}
