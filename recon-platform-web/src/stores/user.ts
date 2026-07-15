import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo, refreshToken as refreshTokenApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

interface UserInfo {
  id: number
  username: string
  realName: string
  orgId: number
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')

  async function login(username: string, password: string) {
    try {
      const result = await loginApi({ username, password })
      token.value = result.accessToken
      refreshToken.value = result.refreshToken
      userInfo.value = result.userInfo
      localStorage.setItem('token', result.accessToken)
      localStorage.setItem('refreshToken', result.refreshToken)
      ElMessage.success(`欢迎回来，${result.userInfo.realName || result.userInfo.username}`)
      return true
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || '登录失败，请检查用户名和密码'
      ElMessage.error(message)
      return false
    }
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo()
      userInfo.value = info
    } catch {
      // token可能已过期，清除本地状态
      token.value = ''
      refreshToken.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    }
  }

  /** 应用初始化时恢复用户会话 */
  async function restoreSession() {
    const storedToken = localStorage.getItem('token')
    if (!storedToken) return
    token.value = storedToken
    refreshToken.value = localStorage.getItem('refreshToken') || ''
    await fetchUserInfo()
  }

  /** 使用refreshToken刷新accessToken */
  async function refreshAccessToken() {
    const storedRefreshToken = refreshToken.value || localStorage.getItem('refreshToken')
    if (!storedRefreshToken) {
      await logout()
      return false
    }
    try {
      const result = await refreshTokenApi(storedRefreshToken)
      token.value = result.accessToken
      localStorage.setItem('token', result.accessToken)
      return true
    } catch (err: any) {
      ElMessage.warning('登录状态已过期，请重新登录')
      await logout()
      return false
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出API错误
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLoggedIn,
    username,
    realName,
    login,
    fetchUserInfo,
    restoreSession,
    refreshAccessToken,
    logout
  }
})
