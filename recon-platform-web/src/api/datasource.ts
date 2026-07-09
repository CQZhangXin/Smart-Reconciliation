import { get, post, put, del } from './request'
import type { DataSource, FieldMappingSuggestion, PageResult, PageQuery } from '@/types'

/** 分页查询数据源 */
export function pageDataSource(params: PageQuery): Promise<PageResult<DataSource>> {
  return get('/datasource/page', params)
}

/** 根据ID查询数据源 */
export function getDataSource(id: number): Promise<DataSource> {
  return get(`/datasource/${id}`)
}

/** 创建数据源 */
export function createDataSource(data: DataSource): Promise<DataSource> {
  return post('/datasource', data)
}

/** 更新数据源 */
export function updateDataSource(id: number, data: DataSource): Promise<DataSource> {
  return put(`/datasource/${id}`, data)
}

/** 删除数据源 */
export function deleteDataSource(id: number): Promise<void> {
  return del(`/datasource/${id}`)
}

/** 测试数据源连接 */
export function testConnection(id: number): Promise<boolean> {
  return post(`/datasource/${id}/test-connection`)
}

/** 同步数据源 */
export function syncDataSource(id: number): Promise<any> {
  return post(`/datasource/${id}/sync`)
}

/** 分页查询原始记录 */
export function pageRecords(sourceId: number, params: PageQuery): Promise<PageResult<any>> {
  return get(`/datasource/${sourceId}/records/page`, params)
}

/** 获取字段映射建议 */
export function getFieldMappingSuggestion(sourceId: number): Promise<FieldMappingSuggestion[]> {
  return get(`/datasource/${sourceId}/field-mapping`)
}
