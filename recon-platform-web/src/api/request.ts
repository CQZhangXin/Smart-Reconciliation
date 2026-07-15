import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'
import router from '@/router'

const instance: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 标记是否正在刷新token，避免并发请求同时刷新
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

function onTokenRefreshed(newToken: string) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

/** 使用refreshToken获取新token，返回新的accessToken或null */
async function tryRefreshAccessToken(): Promise<string | null> {
  const storedRefreshToken = localStorage.getItem('refreshToken')
  if (!storedRefreshToken) return null
  try {
    // 使用独立的axios调用，避免被此拦截器拦截造成无限循环
    const rawResponse = await axios.post(
      '/api/v1/auth/refresh',
      { refreshToken: storedRefreshToken },
      { headers: { 'Content-Type': 'application/json' } }
    )
    const newToken = rawResponse.data?.data?.accessToken
    if (newToken) {
      localStorage.setItem('token', newToken)
      return newToken
    }
    return null
  } catch {
    return null
  }
}

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
      localStorage.removeItem('refreshToken')
      router.push('/login')
      return Promise.reject(new Error(res.message || '未授权'))
    }
    // 许可证相关错误
    if (res.code === 1901 || res.code === 1902 || res.code === 1903) {
      // LICENSE_NOT_FOUND / LICENSE_EXPIRED / LICENSE_INVALID
      ElMessage.error(res.message || '许可证异常')
      // 跳转到许可证管理页
      const currentPath = router.currentRoute.value.path
      if (currentPath !== '/system/license') {
        router.push('/system/license')
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
    if (res.code === 403) {
      router.push('/403')
      return Promise.reject(new Error(res.message || '无权限'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async (error) => {
    if (error.response?.status === 401) {
      // 尝试使用refreshToken刷新token
      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await tryRefreshAccessToken()
        if (newToken) {
          onTokenRefreshed(newToken)
          isRefreshing = false
          // 用新token重试原始请求
          const originalRequest = error.config
          if (originalRequest?.headers) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
          }
          return instance(originalRequest)
        } else {
          isRefreshing = false
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          router.push('/login')
          return Promise.reject(error)
        }
      } else {
        // 其他请求正在刷新token，等待刷新完成后用新token重试
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            const originalRequest = error.config
            if (originalRequest?.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(instance(originalRequest))
          })
        })
      }
    }
    if (error.response?.status === 403) {
      router.push('/403')
      return Promise.reject(error)
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
