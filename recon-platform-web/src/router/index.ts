import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

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
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      // 数据源管理
      {
        path: 'datasource',
        name: 'DataSource',
        component: () => import('@/views/datasource/DataSourceList.vue'),
        meta: { title: '数据源管理', icon: 'Connection' }
      },
      // 规则引擎
      {
        path: 'rule',
        name: 'Rule',
        component: () => import('@/views/rule/RuleList.vue'),
        meta: { title: '规则引擎', icon: 'SetUp' }
      },
      // 对账工作台
      {
        path: 'recon/task',
        name: 'ReconTask',
        component: () => import('@/views/recon/TaskList.vue'),
        meta: { title: '对账工作台', icon: 'Monitor' }
      },
      {
        path: 'recon/match/:taskId',
        name: 'MatchReview',
        component: () => import('@/views/recon/MatchReview.vue'),
        meta: { title: '匹配审核', icon: 'View' }
      },
      // 差异管理中心
      {
        path: 'discrepancy',
        name: 'Discrepancy',
        component: () => import('@/views/discrepancy/DiscrepancyList.vue'),
        meta: { title: '差异管理中心', icon: 'Warning' }
      },
      {
        path: 'discrepancy/:id',
        name: 'DiscrepancyDetail',
        component: () => import('@/views/discrepancy/DiscrepancyDetail.vue'),
        meta: { title: '差异详情', icon: 'Document' }
      },
      {
        path: 'discrepancy/adjustment',
        name: 'Adjustment',
        component: () => import('@/views/discrepancy/AdjustmentList.vue'),
        meta: { title: '调整管理', icon: 'Edit' }
      },
      // 智能分析
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('@/views/analytics/AnalyticsDashboard.vue'),
        meta: { title: '智能分析', icon: 'DataAnalysis' }
      },
      {
        path: 'analytics/nl-query',
        name: 'NLQuery',
        component: () => import('@/views/analytics/NLQuery.vue'),
        meta: { title: '自然语言查询', icon: 'ChatDotRound' }
      },
      // 审批工作流
      {
        path: 'workflow/process',
        name: 'ProcessDef',
        component: () => import('@/views/workflow/ProcessDefList.vue'),
        meta: { title: '流程定义', icon: 'Share' }
      },
      {
        path: 'workflow/approval',
        name: 'Approval',
        component: () => import('@/views/workflow/ApprovalList.vue'),
        meta: { title: '我的审批', icon: 'Checked' }
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar' }
      },
      {
        path: 'system/license',
        name: 'LicenseManage',
        component: () => import('@/views/system/LicenseManage.vue'),
        meta: { title: '许可证管理', icon: 'Key' }
      },
      {
        path: 'system/audit-log',
        name: 'AuditLog',
        component: () => import('@/views/system/AuditLog.vue'),
        meta: { title: '审计日志', icon: 'DocumentChecked' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.noAuth) {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
