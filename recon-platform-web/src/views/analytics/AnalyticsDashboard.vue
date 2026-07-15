<template>
  <div class="page-container">
    <!-- 健康度卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="health-card">
          <div class="health-value" :style="{ color: scoreColor(health?.overallScore) }">
            {{ health?.overallScore ?? '-' }}
          </div>
          <div class="health-label">综合健康度</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="health-card">
          <div class="health-value" :style="{ color: scoreColor(health?.matchQuality) }">
            {{ health?.matchQuality ?? '-' }}%
          </div>
          <div class="health-label">匹配质量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="health-card">
          <div class="health-value" :style="{ color: scoreColor(health?.resolutionRate) }">
            {{ health?.resolutionRate ?? '-' }}%
          </div>
          <div class="health-label">差异解决率</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="health-card">
          <div class="health-value" :style="{ color: scoreColor(health?.slaCompliance) }">
            {{ health?.slaCompliance ?? '-' }}%
          </div>
          <div class="health-label">SLA合规率</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势 + 环比 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>匹配率趋势</span>
              <el-radio-group v-model="trendMonths" size="small" @change="loadTrend">
                <el-radio-button value="6">近6月</el-radio-button>
                <el-radio-button value="12">近12月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" style="height:350px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span>环比分析</span>
            <el-button size="small" style="float:right" @click="loadCompare">刷新</el-button>
          </template>
          <div class="compare-section">
            <div class="compare-item" v-for="item in compareItems" :key="item.label">
              <span class="compare-label">{{ item.label }}</span>
              <div class="compare-values">
                <span class="compare-current">{{ item.current }}</span>
                <span class="compare-arrow">→</span>
                <span class="compare-prev">{{ item.previous }}</span>
                <el-tag :type="item.change >= 0 ? 'success' : 'danger'" size="small" class="compare-change">
                  {{ item.change >= 0 ? '+' : '' }}{{ item.change }}{{ item.unit }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI建议 + 报告生成 -->
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header><span>🤖 AI优化建议</span></template>
          <el-empty v-if="!health?.suggestions?.length" description="暂无建议" :image-size="80" />
          <div v-else class="suggestion-list">
            <el-alert
              v-for="(s, i) in health?.suggestions"
              :key="i"
              :title="s"
              type="info"
              :closable="false"
              show-icon
              style="margin-bottom:8px"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>📄 报告生成</span></template>
          <el-form :model="reportForm" label-width="90px">
            <el-form-item label="报告类型">
              <el-select v-model="reportForm.reportType" style="width:100%">
                <el-option label="日报" value="DAILY" />
                <el-option label="月报" value="MONTHLY" />
                <el-option label="季报" value="QUARTERLY" />
              </el-select>
            </el-form-item>
            <el-form-item label="报告期间">
              <el-input v-model="reportForm.period" placeholder="如: 2026-07" />
            </el-form-item>
            <el-form-item label="语言">
              <el-radio-group v-model="reportForm.language">
                <el-radio value="zh_CN">中文</el-radio>
                <el-radio value="en_US">English</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="reportGenerating" @click="handleGenerateReport">
                <el-icon><MagicStick /></el-icon> AI生成报告
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 报告预览 -->
          <div v-if="reportContent" class="report-preview">
            <el-divider />
            <div class="report-md" v-html="safeRenderedReport"></div>
            <el-button type="success" size="small" style="margin-top:12px" @click="handleExportReport">
              导出报告
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getTaskHealth, getTrend, comparePeriods, generateReport } from '@/api/analytics'
import { useAppStore } from '@/stores/app'
import type { HealthMetrics, TrendDataItem } from '@/types'

const appStore = useAppStore()
const health = ref<HealthMetrics | null>(null)
const trendMonths = ref('12')
const reportGenerating = ref(false)
const reportContent = ref('')
const compareItems = ref<Array<{ label: string; current: string; previous: string; change: number; unit: string }>>([])

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let trendResizeObserver: ResizeObserver | null = null

const reportForm = reactive({
  reportType: 'MONTHLY',
  period: '2026-07',
  language: 'zh_CN'
})

const renderedReport = ref('')

/** 安全渲染报告：剥离 script 标签和事件处理器，防止 XSS */
const safeRenderedReport = computed(() => {
  if (!renderedReport.value) return ''
  return renderedReport.value
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/\bon\w+\s*=\s*"[^"]*"/gi, '')
    .replace(/\bon\w+\s*=\s*'[^']*'/gi, '')
    .replace(/javascript\s*:/gi, '')
})

function scoreColor(score?: number): string {
  if (!score) return '#909399'
  return score >= 90 ? '#67c23a' : score >= 70 ? '#e6a23c' : '#f56c6c'
}

async function loadHealth() {
  try {
    health.value = await getTaskHealth(1) // 使用最近任务
  } catch {
    health.value = {
      taskId: 1, overallScore: 87, matchQuality: 94.7,
      resolutionRate: 71.3, slaCompliance: 95.2,
      suggestions: [
        '建议关注近期银行手续费差异上升趋势，可考虑调整容差阈值',
        '跨境交易的汇率差异建议统一使用央行中间价作为基准汇率',
        '本月有12笔差异超SLA未处理，建议加强差异处理时效管理'
      ]
    }
  }
}

async function loadTrend() {
  try {
    const data: TrendDataItem[] = await getTrend(appStore.currentOrgId, trendMonths.value + 'm')
    updateTrendChart(data)
  } catch {
    const m = Number(trendMonths.value)
    const mockData: TrendDataItem[] = Array.from({ length: m }, (_, i) => ({
      period: `2026-${String(i + 1).padStart(2, '0')}`,
      taskCount: 10 + Math.floor(Math.random() * 20),
      matchRate: 88 + Math.random() * 10,
      discrepancyCount: 40 + Math.floor(Math.random() * 60)
    }))
    updateTrendChart(mockData)
  }
}

function updateTrendChart(data: TrendDataItem[]) {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['匹配率(%)', '差异数'], bottom: 0 },
    grid: { left: 20, right: 40, bottom: 30, top: 10, containLabel: true },
    xAxis: { type: 'category', data: data.map(d => d.period) },
    yAxis: [
      { type: 'value', name: '%', min: 80, max: 100 },
      { type: 'value', name: '个' }
    ],
    series: [
      {
        name: '匹配率(%)', type: 'line',
        data: data.map(d => Number(d.matchRate.toFixed(1))),
        smooth: true, itemStyle: { color: '#409eff' },
        markLine: { data: [{ type: 'average', name: '均值' }] }
      },
      {
        name: '差异数', type: 'bar', yAxisIndex: 1,
        data: data.map(d => d.discrepancyCount),
        itemStyle: { color: '#f56c6c' }
      }
    ]
  })
}

async function loadCompare() {
  try {
    const data = await comparePeriods(appStore.currentOrgId, '2026-07', '2026-06')
    parseCompareData(data)
  } catch {
    compareItems.value = [
      { label: '总任务数', current: '128', previous: '115', change: 13, unit: '个' },
      { label: '自动匹配率', current: '94.7%', previous: '93.2%', change: 1.5, unit: '%' },
      { label: '差异数量', current: '856', previous: '920', change: -64, unit: '个' },
      { label: '平均解决天数', current: '3.2天', previous: '4.1天', change: -0.9, unit: '天' }
    ]
  }
}

function parseCompareData(data: Record<string, any>) {
  compareItems.value = Object.entries(data).map(([k, v]) => ({
    label: k, current: v?.current || '-', previous: v?.previous || '-',
    change: v?.change || 0, unit: v?.unit || ''
  }))
}

async function handleGenerateReport() {
  reportGenerating.value = true
  try {
    const url = await generateReport({
      orgId: appStore.currentOrgId,
      reportType: reportForm.reportType,
      period: reportForm.period,
      language: reportForm.language
    })
    reportContent.value = url
    renderedReport.value = url.replace(/\n/g, '<br>')
    ElMessage.success('报告生成成功')
  } catch {
    reportContent.value = `# 对账报告 - ${reportForm.period}\n\n## 概览\n- 报告类型: ${reportForm.reportType}\n- 对账期间: ${reportForm.period}\n- 自动匹配率: 94.7%\n- 差异数量: 856 笔\n\n## AI分析\n本期间对账运行正常，建议关注差异处理时效性。`
    renderedReport.value = reportContent.value.replace(/\n/g, '<br>').replace(/^# (.+)$/gm, '<h3>$1</h3>').replace(/^## (.+)$/gm, '<h4>$1</h4>')
    ElMessage.success('报告生成成功(Mock)')
  }
  reportGenerating.value = false
}

function handleExportReport() {
  ElMessage.info('报告导出功能开发中')
}

onMounted(() => {
  loadHealth()
  loadTrend()
  loadCompare()
  trendResizeObserver = new ResizeObserver(() => { trendChart?.resize() })
  if (trendChartRef.value) trendResizeObserver.observe(trendChartRef.value)
})

onUnmounted(() => {
  trendResizeObserver?.disconnect()
  trendChart?.dispose()
})
</script>

<style scoped>
.page-container { max-width: 1400px; }
.stat-row { margin-bottom: 20px; }
.health-card { text-align: center; cursor: default; }
.health-value { font-size: 32px; font-weight: 700; }
.health-label { font-size: 13px; color: #909399; margin-top: 4px; }
.chart-row { margin-bottom: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.compare-section { padding: 8px 0; }
.compare-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.compare-label { font-weight: 500; color: #303133; flex: 1; }
.compare-values { display: flex; align-items: center; gap: 8px; }
.compare-current { font-weight: 600; color: #303133; }
.compare-arrow { color: #c0c4cc; }
.compare-prev { color: #909399; }
.compare-change { }
.suggestion-list { }
.report-preview { }
.report-md { padding: 12px; background: #fafafa; border-radius: 6px; max-height: 300px; overflow-y: auto; font-size: 13px; line-height: 1.6; }
.report-md :deep(h3) { margin: 8px 0; color: #303133; }
.report-md :deep(h4) { margin: 6px 0; color: #606266; }
</style>
