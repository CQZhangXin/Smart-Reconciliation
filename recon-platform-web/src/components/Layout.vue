<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo-container" @click="goHome">
        <el-icon :size="24" color="#409eff"><Money /></el-icon>
        <span v-show="!sidebarCollapsed" class="logo-text">财务对账平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
        :default-openeds="defaultOpeneds"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/datasource">
          <el-icon><Connection /></el-icon>
          <span>数据源管理</span>
        </el-menu-item>
        <el-menu-item index="/rule">
          <el-icon><SetUp /></el-icon>
          <span>规则引擎</span>
        </el-menu-item>
        <el-menu-item index="/custom-recon">
          <el-icon><Operation /></el-icon>
          <span>自定义对账</span>
        </el-menu-item>
        <el-menu-item index="/recon/task">
          <el-icon><Monitor /></el-icon>
          <span>对账工作台</span>
        </el-menu-item>
        <el-menu-item index="/discrepancy">
          <el-icon><Warning /></el-icon>
          <span>差异管理中心</span>
        </el-menu-item>
        <el-sub-menu index="analytics-group">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>智能分析</span>
          </template>
          <el-menu-item index="/analytics">分析看板</el-menu-item>
          <el-menu-item index="/analytics/nl-query">自然语言查询</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="workflow-group">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>审批工作流</span>
          </template>
          <el-menu-item index="/workflow/process">流程定义</el-menu-item>
          <el-menu-item index="/workflow/approval">我的审批</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system-group">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/license">许可证管理</el-menu-item>
          <el-menu-item index="/system/ai">大模型配置</el-menu-item>
          <el-menu-item index="/system/audit-log">审计日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="appStore.toggleSidebar()"
          >
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="3" :max="99" class="notification-badge">
            <el-icon :size="18"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="user-info">
              <el-icon :size="18"><UserFilled /></el-icon>
              <span class="username">{{ userStore.realName || userStore.username || '管理员' }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed(() => appStore.sidebarCollapsed)
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/custom-recon')) return '/custom-recon'
  if (path.startsWith('/recon/match')) return '/recon/task'
  if (path.startsWith('/discrepancy/') && path !== '/discrepancy/adjustment') return '/discrepancy'
  return path
})
const currentTitle = computed(() => route.meta?.title as string || '')

const defaultOpeneds = computed(() => {
  const path = route.path
  const opened: string[] = []
  if (path.startsWith('/analytics')) opened.push('analytics-group')
  if (path.startsWith('/workflow')) opened.push('workflow-group')
  if (path.startsWith('/system')) opened.push('system-group')
  if (path.startsWith('/recon')) opened.push('recon-group')
  if (path.startsWith('/discrepancy')) opened.push('discrepancy-group')
  return opened
})

function goHome() {
  router.push('/dashboard')
}

function handleUserCommand(command: string) {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await userStore.logout()
        router.push('/login')
      }).catch(() => {})
      break
    case 'profile':
      ElMessageBox.alert('个人信息功能开发中', '提示')
      break
    case 'password':
      ElMessageBox.alert('修改密码功能开发中', '提示')
      break
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
}

.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}
.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
}

.username {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>
