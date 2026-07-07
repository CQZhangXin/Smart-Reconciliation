import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentOrgId = ref<number>(1)  // 默认组织ID

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setOrgId(orgId: number) {
    currentOrgId.value = orgId
  }

  return {
    sidebarCollapsed,
    currentOrgId,
    toggleSidebar,
    setOrgId
  }
})
