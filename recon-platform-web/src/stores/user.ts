import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
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

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')

  async function login(username: string, password: string) {
    try {
      const result = await loginApi({ username, password })
      token.value = result.token
      refreshToken.value = result.refreshToken
      userInfo.value = result.user
      localStorage.setItem('token', result.token)
      localStorage.setItem('refreshToken', result.refreshToken)
      ElMessage.success(`欢迎回来，${result.user.realName || result.user.username}`)
      return true
    } catch {
      return false
    }
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo()
      userInfo.value = info
    } catch {
      // token可能已过期
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
    logout
  }
})
