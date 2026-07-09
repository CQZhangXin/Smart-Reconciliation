import request, { get, post, del } from './request'
import type { LicenseStatus } from '@/types'

/** 获取许可证状态 */
export function getLicenseStatus(): Promise<LicenseStatus> {
  return get('/license/status')
}

/** 激活许可证 (上传文件) */
export function activateLicense(file: File): Promise<LicenseStatus> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/license/activate', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data.data)
}

/** 激活许可证 (文本内容) */
export function activateLicenseText(licenseData: string): Promise<LicenseStatus> {
  return post('/license/activate/text', { licenseData })
}

/** 吊销许可证 */
export function revokeLicense(): Promise<string> {
  return del('/license/revoke')
}
