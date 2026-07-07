import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const instance: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200) {
      return response
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject(new Error(res.message || '未授权'))
    }
    // 许可证相关错误
    if (res.code === 1901 || res.code === 1902 || res.code === 1903) {
      // LICENSE_NOT_FOUND / LICENSE_EXPIRED / LICENSE_INVALID
      ElMessage.error(res.message || '许可证异常')
      // 跳转到许可证管理页
      const currentPath = window.location.pathname
      if (currentPath !== '/system/license') {
        window.location.href = '/system/license'
      }
      return Promise.reject(new Error(res.message || '许可证异常'))
    }
    if (res.code === 1904) {
      ElMessage.warning(res.message || '用户数已达上限')
      return Promise.reject(new Error(res.message || '用户数已达上限'))
    }
    if (res.code === 1905) {
      ElMessage.warning(res.message || '功能未授权')
      return Promise.reject(new Error(res.message || '功能未授权'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/** GET 请求 */
export function get<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  return instance.get(url, { params }).then(res => res.data.data)
}

/** POST 请求 */
export function post<T = any>(url: string, data?: Record<string, any>): Promise<T> {
  return instance.post(url, data).then(res => res.data.data)
}

/** PUT 请求 */
export function put<T = any>(url: string, data?: Record<string, any>): Promise<T> {
  return instance.put(url, data).then(res => res.data.data)
}

/** DELETE 请求 */
export function del<T = any>(url: string): Promise<T> {
  return instance.delete(url).then(res => res.data.data)
}

export default instance
