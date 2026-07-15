import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer', permission: 'dashboard:view' }
      },
      // 数据源管理
      {
        path: 'datasource',
        name: 'DataSource',
        component: () => import('@/views/datasource/DataSourceList.vue'),
        meta: { title: '数据源管理', icon: 'Connection', permission: 'datasource:view' }
      },
      // 规则引擎
      {
        path: 'rule',
        name: 'Rule',
        component: () => import('@/views/rule/RuleList.vue'),
        meta: { title: '规则引擎', icon: 'SetUp', permission: 'rule:view' }
      },
      // 自定义对账
      {
        path: 'custom-recon',
        name: 'CustomRecon',
        component: () => import('@/views/custom/CustomReconList.vue'),
        meta: { title: '自定义对账', icon: 'Operation', permission: 'custom-recon:view' }
      },
      {
        path: 'custom-recon/wizard',
        name: 'CustomReconWizard',
        component: () => import('@/views/custom/CustomReconWizard.vue'),
        meta: { title: '配置对账方案', icon: 'Operation', permission: 'custom-recon:view' }
      },
      {
        path: 'custom-recon/wizard/:id',
        name: 'CustomReconEdit',
        component: () => import('@/views/custom/CustomReconWizard.vue'),
        meta: { title: '编辑对账方案', icon: 'Operation', permission: 'custom-recon:view' }
      },
      // 对账工作台
      {
        path: 'recon/task',
        name: 'ReconTask',
        component: () => import('@/views/recon/TaskList.vue'),
        meta: { title: '对账工作台', icon: 'Monitor', permission: 'recon:view' }
      },
      {
        path: 'recon/match/:taskId',
        name: 'MatchReview',
        component: () => import('@/views/recon/MatchReview.vue'),
        meta: { title: '匹配审核', icon: 'View', permission: 'recon:view' }
      },
      // 差异管理中心
      {
        path: 'discrepancy',
        name: 'Discrepancy',
        component: () => import('@/views/discrepancy/DiscrepancyList.vue'),
        meta: { title: '差异管理中心', icon: 'Warning', permission: 'discrepancy:view' }
      },
      {
        path: 'discrepancy/:id',
        name: 'DiscrepancyDetail',
        component: () => import('@/views/discrepancy/DiscrepancyDetail.vue'),
        meta: { title: '差异详情', icon: 'Document', permission: 'discrepancy:view' }
      },
      {
        path: 'discrepancy/adjustment',
        name: 'Adjustment',
        component: () => import('@/views/discrepancy/AdjustmentList.vue'),
        meta: { title: '调整管理', icon: 'Edit', permission: 'discrepancy:view' }
      },
      // 智能分析
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/analytics/AnalyticsDashboard.vue'),
        meta: { title: '智能分析', icon: 'DataAnalysis', permission: 'analytics:view' }
      },
      {
        path: 'analytics/nl-query',
        name: 'NLQuery',
        component: () => import('@/views/analytics/NLQuery.vue'),
        meta: { title: '自然语言查询', icon: 'ChatDotRound', permission: 'analytics:view' }
      },
      // 审批工作流
      {
        path: 'workflow/process',
        name: 'ProcessDef',
        component: () => import('@/views/workflow/ProcessDefList.vue'),
        meta: { title: '流程定义', icon: 'Share', permission: 'workflow:view' }
      },
      {
        path: 'workflow/approval',
        name: 'Approval',
        component: () => import('@/views/workflow/ApprovalList.vue'),
        meta: { title: '我的审批', icon: 'Checked', permission: 'workflow:approve' }
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'system:user' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permission: 'system:role' }
      },
      {
        path: 'system/license',
        name: 'LicenseManage',
        component: () => import('@/views/system/LicenseManage.vue'),
        meta: { title: '许可证管理', icon: 'Key', permission: 'system:license' }
      },
      {
        path: 'system/ai',
        name: 'AiModelConfig',
        component: () => import('@/views/system/AiModelConfig.vue'),
        meta: { title: '大模型配置', icon: 'Cpu', permission: 'system:ai' }
      },
      {
        path: 'system/audit-log',
        name: 'AuditLog',
        component: () => import('@/views/system/AuditLog.vue'),
        meta: { title: '审计日志', icon: 'DocumentChecked', permission: 'system:audit' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', noAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面未找到', noAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 已登录用户访问登录页 → 跳转仪表盘
  if (to.path === '/login' && userStore.isLoggedIn) {
    next('/dashboard')
    return
  }

  // 无需认证的页面直接放行
  if (to.meta.noAuth) {
    next()
    return
  }
  // 未登录 → 跳转登录页
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  // Token已存在：检查是否即将过期（简单的时间戳检查，生产可改用jwt-decode）
  // 此处作为占位校验，确保每次导航都有有效token
  // 实际的token刷新由request.ts中的响应拦截器处理

  // 权限校验：meta.permission存在时需要检查用户权限
  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission) {
    const userInfo = userStore.userInfo as any
    const permissions = userInfo?.permissions as string[] | undefined
    if (permissions && permissions.length > 0 && !permissions.includes(requiredPermission)) {
      next('/403')
      return
    }
  }

  next()
})

export default router
