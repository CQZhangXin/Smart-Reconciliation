<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6f7ff;">
              <el-icon :size="28" color="#1890ff"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">对账任务</div>
              <div class="stat-value">{{ dashboardData?.taskStats?.total || 0 }}</div>
              <div class="stat-sub">
                运行中: {{ dashboardData?.taskStats?.running || 0 }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f0f9eb;">
              <el-icon :size="28" color="#67c23a"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">自动匹配率</div>
              <div class="stat-value">{{ (dashboardData?.matchStats?.matchRate ?? 0).toFixed(1) }}%</div>
              <div class="stat-sub">
                已匹配: {{ dashboardData?.matchStats?.totalMatched || 0 }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fef0f0;">
              <el-icon :size="28" color="#f56c6c"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">差异数量</div>
              <div class="stat-value">{{ dashboardData?.discrepancyStats?.total || 0 }}</div>
              <div class="stat-sub danger">
                SLA超期: {{ dashboardData?.discrepancyStats?.slaOverdue || 0 }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #fdf6ec;">
              <el-icon :size="28" color="#e6a23c"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">差异解决率</div>
              <div class="stat-value">
                {{ resolveRate }}%
              </div>
              <div class="stat-sub">
                已解决: {{ dashboardData?.discrepancyStats?.resolved || 0 }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>对账趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
                <el-radio-button value="6m">近6月</el-radio-button>
                <el-radio-button value="12m">近12月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 320px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>差异分类分布</span>
          </template>
          <div ref="categoryChartRef" style="height: 320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近任务 -->
    <el-row :gutter="20" class="table-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近对账任务</span>
              <el-button type="primary" size="small" @click="$router.push('/recon/task')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="dashboardData?.recentTasks || []" stripe>
            <el-table-column prop="taskName" label="任务名称" min-width="180" />
            <el-table-column prop="taskType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="taskTypeColor(row.taskType)" size="small">
                  {{ taskTypeLabel(row.taskType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reconPeriod" label="对账期间" width="140" />
            <el-table-column prop="matchedCount" label="匹配数" width="90" />
            <el-table-column prop="discrepancyCount" label="差异数" width="90" />
            <el-table-column prop="matchRate" label="匹配率" width="100">
              <template #default="{ row }">
                <el-progress
                  :percentage="Number((row.matchRate ?? 0).toFixed(1))"
                  :stroke-width="8"
                  :status="row.matchRate >= 90 ? 'success' : row.matchRate >= 70 ? '' : 'exception'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="completedAt" label="完成时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, getTrend } from '@/api/analytics'
import { useAppStore } from '@/stores/app'
import type { DashboardData, TrendDataItem } from '@/types'

const appStore = useAppStore()
const dashboardData = ref<DashboardData | null>(null)
const trendData = ref<TrendDataItem[]>([])
const trendPeriod = ref('12m')

const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let trendResizeObserver: ResizeObserver | null = null
let categoryResizeObserver: ResizeObserver | null = null

const resolveRate = computed(() => {
  const stats = dashboardData.value?.discrepancyStats
  if (!stats || stats.total === 0) return '0.0'
  return ((stats.resolved / stats.total) * 100).toFixed(1)
})

function taskTypeLabel(type: string): string {
  const map: Record<string, string> = { BANK: '银行', THIRD_PAYMENT: '第三方', AR: '应收', AP: '应付', CROSS_SYSTEM: '跨系统', INTERCOMPANY: '内部往来' }
  return map[type] || type
}

function taskTypeColor(type: string): string {
  const map: Record<string, string> = { BANK: 'primary', THIRD_PAYMENT: 'success', AR: 'warning', AP: 'danger' }
  return map[type] || 'info'
}

function statusType(status: string): string {
  const map: Record<string, string> = { PENDING: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待执行', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败', CANCELLED: '已取消' }
  return map[status] || status
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const existingInstance = echarts.getInstanceByDom(trendChartRef.value)
  if (existingInstance) existingInstance.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const hasData = trendData.value.length > 0

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['任务数', '匹配率(%)', '差异数'], bottom: 0 },
    grid: { left: 20, right: 20, bottom: 30, top: 10, containLabel: true },
    xAxis: {
      type: 'category',
      data: hasData ? trendData.value.map(t => t.period) : [],
      axisLabel: { rotate: 30 }
    },
    yAxis: [
      { type: 'value', name: '数量' },
      { type: 'value', name: '%', min: 0, max: 100 }
    ],
    series: [
      {
        name: '任务数', type: 'bar',
        data: hasData ? trendData.value.map(t => t.taskCount) : [],
        itemStyle: { color: '#409eff' }
      },
      {
        name: '匹配率(%)', type: 'line', yAxisIndex: 1,
        data: hasData ? trendData.value.map(t => t.matchRate) : [],
        itemStyle: { color: '#67c23a' },
        smooth: true
      },
      {
        name: '差异数', type: 'bar',
        data: hasData ? trendData.value.map(t => t.discrepancyCount) : [],
        itemStyle: { color: '#f56c6c' }
      }
    ]
  })
}

function initCategoryChart() {
  if (!categoryChartRef.value) return
  categoryChart = echarts.init(categoryChartRef.value)
  const dist = dashboardData.value?.categoryDistribution || {}

  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontWeight: 'bold' } },
      data: Object.keys(dist).length > 0
        ? Object.entries(dist).map(([name, value]) => ({ name: categoryLabel(name), value }))
        : [{ name: '暂无数据', value: 1 }]
    }]
  })
}

function categoryLabel(cat: string): string {
  const map: Record<string, string> = {
    TIME_DIFF: '时间差异', FEE_DIFF: '手续费差异', EXCHANGE_DIFF: '汇率差异',
    HUMAN_ERROR: '人为错误', UNREACHED: '未达账项', DUPLICATE: '重复交易', UNKNOWN: '未知'
  }
  return map[cat] || cat
}

async function loadDashboard() {
  try {
    dashboardData.value = await getDashboard(appStore.currentOrgId)
    await nextTick()
    initCategoryChart()
  } catch {
    // 使用模拟数据（API未就绪时）
    dashboardData.value = {
      taskStats: { total: 128, running: 3, completed: 120, failed: 5 },
      matchStats: { totalMatched: 15230, matchRate: 94.7, avgConfidence: 92.3 },
      discrepancyStats: { total: 856, open: 234, slaOverdue: 12, resolved: 610 },
      recentTasks: [],
      categoryDistribution: { TIME_DIFF: 320, FEE_DIFF: 210, EXCHANGE_DIFF: 156, UNKNOWN: 98, HUMAN_ERROR: 42, DUPLICATE: 30 }
    }
    await nextTick()
    initCategoryChart()
  }
}

async function loadTrendData() {
  try {
    trendData.value = await getTrend(appStore.currentOrgId, trendPeriod.value)
  } catch {
    // 模拟趋势数据
    const months = trendPeriod.value === '6m' ? 6 : 12
    trendData.value = Array.from({ length: months }, (_, i) => ({
      period: `2026-${String(i + 1).padStart(2, '0')}`,
      taskCount: 8 + Math.floor(Math.random() * 15),
      matchRate: 90 + Math.random() * 8,
      discrepancyCount: 30 + Math.floor(Math.random() * 80)
    }))
  }
  initTrendChart()
}

onMounted(async () => {
  await loadDashboard()
  await loadTrendData()
  trendResizeObserver = new ResizeObserver(() => { trendChart?.resize() })
  categoryResizeObserver = new ResizeObserver(() => { categoryChart?.resize() })
  if (trendChartRef.value) trendResizeObserver.observe(trendChartRef.value)
  if (categoryChartRef.value) categoryResizeObserver.observe(categoryChartRef.value)
})

onUnmounted(() => {
  trendResizeObserver?.disconnect()
  categoryResizeObserver?.disconnect()
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: default;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.stat-sub.danger {
  color: #f56c6c;
}

.chart-row {
  margin-bottom: 20px;
}

.table-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
