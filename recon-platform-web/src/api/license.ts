import { get, post } from './request'

export interface LicenseStatus {
  licensed: boolean
  licenseEnabled: boolean
  status: 'UNLICENSED' | 'ACTIVE' | 'EXPIRED' | 'EXPIRING_SOON' | 'INVALID' | 'DISABLED'
  orgName?: string
  orgCode?: string
  maxUsers?: number
  expireDate?: string
  remainingDays?: number
  features?: string[]
  issuedAt?: string
  message?: string
}

/** 获取许可证状态 */
export function getLicenseStatus(): Promise<LicenseStatus> {
  return get('/license/status')
}

/** 激活许可证 (上传文件) */
export function activateLicense(file: File): Promise<LicenseStatus> {
  const formData = new FormData()
  formData.append('file', file)
  return fetch('/api/v1/license/activate', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('token')}`
    },
    body: formData
  }).then(res => res.json()).then(res => res.data)
}

/** 激活许可证 (文本内容) */
export function activateLicenseText(licenseData: string): Promise<LicenseStatus> {
  return post('/license/activate/text', { licenseData })
}

/** 吊销许可证 */
export function revokeLicense(): Promise<string> {
  return new Promise((resolve, reject) => {
    fetch('/api/v1/license/revoke', {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        'Content-Type': 'application/json'
      }
    })
      .then(res => res.json())
      .then(res => {
        if (res.code === 200) resolve(res.data)
        else reject(new Error(res.message))
      })
      .catch(reject)
  })
}
