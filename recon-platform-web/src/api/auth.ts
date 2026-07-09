import { post, get } from './request'

interface LoginParams {
  username: string
  password: string
}

interface UserInfo {
  id: number
  username: string
  realName: string
  orgId: number
}

interface LoginResult {
  accessToken: string
  refreshToken: string
  userInfo: UserInfo
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
export function getUserInfo(): Promise<UserInfo> {
  return get('/auth/me')
}

/** 刷新Token */
export function refreshToken(refreshToken: string): Promise<{ accessToken: string }> {
  return post('/auth/refresh', { refreshToken })
}
