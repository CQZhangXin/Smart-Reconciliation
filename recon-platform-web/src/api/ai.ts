import { get, post } from './request'

/** 查询当前 AI 配置 */
export function getAiConfig(): Promise<Record<string, any>> {
  return get('/ai/config')
}

/** 查询支持的提供商 */
export function listAiProviders(): Promise<Record<string, any>[]> {
  return get('/ai/providers')
}

/** 测试大模型连通性 */
export function testAiConnection(prompt?: string): Promise<Record<string, any>> {
  return post('/ai/test', prompt ? { prompt } : {})
}
