<template>
  <div class="page-container">
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="page-title">差异详情 #{{ id }}</span>
    </div>

    <el-row :gutter="20">
      <!-- 差异基本信息 -->
      <el-col :span="16">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span>差异信息</span>
            <el-tag
              :type="discrepancy.status === 'RESOLVED' ? 'success' : discrepancy.status === 'PROCESSING' ? 'warning' : 'info'"
              style="margin-left:12px"
            >{{ statusLabel(discrepancy.status) }}</el-tag>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="差异ID">{{ discrepancy.id }}</el-descriptions-item>
            <el-descriptions-item label="关联任务ID">{{ discrepancy.taskId }}</el-descriptions-item>
            <el-descriptions-item label="方向">
              <el-tag :type="discrepancy.side === 'SOURCE_A' ? 'primary' : 'success'" size="small">
                {{ discrepancy.side === 'SOURCE_A' ? '仅A方' : '仅B方' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="金额">{{ discrepancy.amount?.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="差异金额">
              <span :style="{ color: (discrepancy.amountDiff ?? 0) !== 0 ? '#f56c6c' : '' }">
                {{ discrepancy.amountDiff?.toFixed(2) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="币种">{{ discrepancy.currency || 'CNY' }}</el-descriptions-item>
            <el-descriptions-item label="AI分类">
              <el-tag v-if="discrepancy.category" :type="categoryColor(discrepancy.category)" size="small">
                {{ categoryLabel(discrepancy.category) }}
              </el-tag>
              <el-tag v-else type="info" size="small">未分类</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="riskColor(discrepancy.riskLevel!)" size="small">
                {{ riskLabel(discrepancy.riskLevel!) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理人">{{ discrepancy.handlerName || '未分配' }}</el-descriptions-item>
          </el-descriptions>

          <div class="action-buttons">
            <el-button v-if="!discrepancy.category" type="success" @click="handleClassify">
              <el-icon><MagicStick /></el-icon> AI智能分类
            </el-button>
            <el-button v-if="discrepancy.category" type="warning" @click="handleRootCause">
              <el-icon><Search /></el-icon> AI根因分析
            </el-button>
            <el-button
              v-if="discrepancy.status === 'PROCESSING'"
              type="primary"
              @click="$router.push(`/discrepancy/adjustment?discrepancyId=${discrepancy.id}`)"
            >
              <el-icon><Edit /></el-icon> 创建调整
            </el-button>
          </div>
        </el-card>

        <!-- AI根因分析结果 -->
        <el-card v-if="rootCauseResult" shadow="never" class="detail-card">
          <template #header>
            <span>🔍 AI根因分析结果</span>
            <el-tag type="warning" size="small" style="margin-left:12px">
              模型: {{ rootCauseResult.modelUsed }}
            </el-tag>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="根因类别">{{ rootCauseCategoryLabel(rootCauseResult.rootCauseCategory) }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ rootCauseResult.confidence }}%</el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="riskColor(rootCauseResult.riskLevel)" size="small">{{ rootCauseResult.riskLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Token消耗">{{ rootCauseResult.tokensUsed }}</el-descriptions-item>
          </el-descriptions>
          <div class="analysis-block">
            <h4>分析过程 (Chain-of-Thought)</h4>
            <pre class="analysis-text">{{ rootCauseResult.analysisSteps }}</pre>
          </div>
          <div class="analysis-block">
            <h4>处理建议</h4>
            <el-alert :title="rootCauseResult.suggestion" type="success" :closable="false" show-icon />
          </div>
          <div v-if="rootCauseResult.suggestedAdjustment" class="analysis-block">
            <h4>建议调整分录</h4>
            <pre class="analysis-text">{{ rootCauseResult.suggestedAdjustment }}</pre>
          </div>
        </el-card>
      </el-col>

      <!-- 侧边操作 -->
      <el-col :span="8">
        <el-card shadow="never" class="detail-card">
          <template #header><span>操作面板</span></template>
          <el-steps direction="vertical" :active="activeStep" process-status="finish" finish-status="success">
            <el-step title="识别差异" description="对账引擎自动发现" />
            <el-step title="AI分类" :description="discrepancy.category ? categoryLabel(discrepancy.category) : '待分类'" />
            <el-step title="根因分析" description="AI分析根本原因" />
            <el-step title="分配处理" :description="discrepancy.handlerName || '待分配'" />
            <el-step title="解决关闭" :description="discrepancy.status === 'RESOLVED' || discrepancy.status === 'CLOSED' ? '已完成' : '待处理'" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDiscrepancy, classifyDiscrepancy } from '@/api/discrepancy'
import type { ReconDiscrepancy, RootCauseResult } from '@/types'

const route = useRoute()
const id = Number(route.params.id)
const discrepancy = ref<ReconDiscrepancy>({} as ReconDiscrepancy)
const rootCauseResult = ref<RootCauseResult | null>(null)

const activeStep = computed(() => {
  if (discrepancy.value.status === 'RESOLVED' || discrepancy.value.status === 'CLOSED') return 5
  if (discrepancy.value.handlerId) return 4
  if (discrepancy.value.aiRootCause) return 3
  if (discrepancy.value.category) return 2
  return 1
})

function categoryLabel(cat: string): string {
  const map: Record<string, string> = {
    TIME_DIFF: '时间差异', FEE_DIFF: '手续费差异', EXCHANGE_DIFF: '汇率差异',
    HUMAN_ERROR: '人为错误', UNREACHED: '未达账项', DUPLICATE: '重复交易', UNKNOWN: '未知'
  }
  return map[cat] || cat
}
function categoryColor(cat: string): string {
  const map: Record<string, string> = {
    TIME_DIFF: 'warning', FEE_DIFF: '', EXCHANGE_DIFF: 'primary',
    HUMAN_ERROR: 'danger', UNREACHED: 'info', DUPLICATE: 'success', UNKNOWN: 'info'
  }
  return map[cat] || 'info'
}
function riskColor(risk: string): string {
  const map: Record<string, string> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[risk] || 'info'
}
function riskLabel(risk: string): string {
  const map: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
  return map[risk] || risk
}
function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[status] || status
}
function rootCauseCategoryLabel(cat: string): string {
  return categoryLabel(cat) || cat
}

async function loadDetail() {
  try {
    discrepancy.value = await getDiscrepancy(id)
  } catch {
    // Mock数据
    discrepancy.value = {
      id, taskId: 1, recordId: 100, relatedRecordId: 200,
      side: 'SOURCE_A', amount: 10500.00, amountDiff: 50.00,
      currency: 'CNY', category: '', riskLevel: 'MEDIUM',
      handlerId: undefined, handlerName: '', status: 'PENDING',
      slaDeadline: '2026-07-15 18:00:00'
    }
  }
}

async function handleClassify() {
  try {
    const result = await classifyDiscrepancy(id)
    discrepancy.value = result
    ElMessage.success(`AI分类完成: ${categoryLabel(result.category!)}`)
  } catch {
    ElMessage.success('AI分类完成(Mock): 手续费差异')
    discrepancy.value.category = 'FEE_DIFF'
    discrepancy.value.riskLevel = 'LOW'
  }
}

async function handleRootCause() {
  try {
    // 模拟根因分析结果
    rootCauseResult.value = {
      rootCauseCategory: discrepancy.value.category || 'UNKNOWN',
      analysisSteps: 'Step 1: 分析金额差异特征 → 差异金额50.00元，在合理范围内\nStep 2: 检查交易时间 → 时间差在T+1结算周期内\nStep 3: 综合判断 → 差异可能由银行手续费导致',
      suggestion: '建议核实银行手续费标准，与银行对账单中的手续费明细进行核对。如确认是手续费差异，可创建调整分录平账。',
      riskLevel: 'LOW',
      suggestedAdjustment: '借: 财务费用-手续费 50.00\n贷: 银行存款 50.00',
      confidence: 85,
      modelUsed: 'gpt-4o',
      tokensUsed: 320
    }
    ElMessage.success('根因分析完成')
  } catch {
    ElMessage.success('根因分析完成(Mock)')
  }
}

onMounted(() => { loadDetail() })
</script>

<style scoped>
.page-container { max-width: 1400px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; }
.detail-card { margin-bottom: 16px; }
.action-buttons { margin-top: 16px; display: flex; gap: 10px; }
.analysis-block { margin-top: 16px; }
.analysis-block h4 { margin: 0 0 8px; font-size: 14px; color: #303133; }
.analysis-text { background: #f5f7fa; padding: 12px; border-radius: 6px; font-size: 13px; line-height: 1.6; white-space: pre-wrap; color: #606266; margin: 0; }
</style>
