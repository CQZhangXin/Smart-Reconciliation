import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(localStorage.getItem('sidebarCollapsed') === 'true')
  const currentOrgId = ref<number>(Number(localStorage.getItem('currentOrgId')) || 1)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('sidebarCollapsed', String(sidebarCollapsed.value))
  }

  function setOrgId(orgId: number) {
    currentOrgId.value = orgId
  }

  function setCurrentOrgId(orgId: number) {
    currentOrgId.value = orgId
    localStorage.setItem('currentOrgId', String(orgId))
  }

  return {
    sidebarCollapsed,
    currentOrgId,
    toggleSidebar,
    setOrgId,
    setCurrentOrgId
  }
})
