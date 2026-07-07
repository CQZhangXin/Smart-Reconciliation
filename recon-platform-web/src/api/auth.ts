import { post, get } from './request'

interface LoginParams {
  username: string
  password: string
}

interface LoginResult {
  token: string
  refreshToken: string
  user: {
    id: number
    username: string
    realName: string
    orgId: number
  }
}

/** 登录 */
export function login(data: LoginParams): Promise<LoginResult> {
  return post('/auth/login', data)
}

/** 登出 */
export function logout(): Promise<void> {
  return post('/auth/logout')
}

/** 获取当前用户信息 */
export function getUserInfo(): Promise<LoginResult['user']> {
  return get('/auth/user-info')
}

/** 刷新Token */
export function refreshToken(refreshToken: string): Promise<{ token: string }> {
  return post('/auth/refresh-token', { refreshToken })
}
