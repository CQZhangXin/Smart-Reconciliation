<template>
  <div class="page-container">
    <!-- 返回 + 标题 -->
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="page-title">匹配结果审核 - 任务 #{{ taskId }}</span>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="匹配类型">
          <el-select v-model="query.matchType" placeholder="全部" clearable style="width:130px">
            <el-option label="精确匹配" value="EXACT" />
            <el-option label="规则匹配" value="RULE" />
            <el-option label="AI语义" value="AI_SEMANTIC" />
            <el-option label="AI拆单" value="AI_SPLIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
            <el-option label="自动确认" value="AUTO_CONFIRMED" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="已确认" value="MANUAL_CONFIRMED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="matchType" label="匹配类型" width="110">
          <template #default="{ row }">
            <el-tag :type="matchTypeColor(row.matchType)" size="small">
              {{ matchTypeLabel(row.matchType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额(A/B/差异)" width="220">
          <template #default="{ row }">
            <span>{{ row.amountA?.toFixed(2) }}</span>
            <span style="margin:0 4px;color:#909399;">/</span>
            <span>{{ row.amountB?.toFixed(2) }}</span>
            <el-tag v-if="row.amountDiff !== 0" size="small" :type="row.amountDiff! > 0 ? 'danger' : 'success'" style="margin-left:8px">
              {{ (row.amountDiff ?? 0) > 0 ? '+' : '' }}{{ row.amountDiff?.toFixed(2) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dateDiffDays" label="日期差(天)" width="100" align="center" />
        <el-table-column label="匹配维度得分" min-width="300">
          <template #default="{ row }">
            <div class="dimension-scores" v-if="row.matchDimensions">
              <span v-for="(score, dim) in parseDimensions(row.matchDimensions)" :key="dim" class="dim-tag">
                {{ dim }}: {{ (Number(score) * 100).toFixed(0) }}%
              </span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="confidenceType(row.confidence!)" size="small">
              {{ (row.confidence ?? 0).toFixed(1) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button type="success" link size="small" @click="handleConfirm(row)">确认</el-button>
              <el-button type="danger" link size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="匹配详情" width="600px">
      <el-descriptions v-if="currentMatch" :column="2" border>
        <el-descriptions-item label="匹配ID">{{ currentMatch.id }}</el-descriptions-item>
        <el-descriptions-item label="匹配类型">{{ matchTypeLabel(currentMatch.matchType) }}</el-descriptions-item>
        <el-descriptions-item label="A方金额">{{ currentMatch.amountA?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="B方金额">{{ currentMatch.amountB?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="金额差异">{{ currentMatch.amountDiff?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="日期差异">{{ currentMatch.dateDiffDays }} 天</el-descriptions-item>
        <el-descriptions-item label="置信度">{{ (currentMatch.confidence ?? 0).toFixed(1) }}%</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ statusLabel(currentMatch.status) }}</el-descriptions-item>
        <el-descriptions-item label="AI解释" :span="2">{{ currentMatch.aiExplanation || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMatch, confirmMatch, rejectMatch } from '@/api/recon'
import type { ReconMatch } from '@/types'

const route = useRoute()
const taskId = Number(route.params.taskId)
const loading = ref(false)
const total = ref(0)
const tableData = ref<ReconMatch[]>([])
const selectedRows = ref<ReconMatch[]>([])
const detailVisible = ref(false)
const currentMatch = ref<ReconMatch | null>(null)

const query = reactive({ page: 1, size: 20, taskId, matchType: '', status: '' })

function matchTypeLabel(type: string): string {
  const map: Record<string, string> = { EXACT: '精确匹配', RULE: '规则匹配', AI_SEMANTIC: 'AI语义', AI_SPLIT: 'AI拆单', MANUAL: '手工匹配' }
  return map[type] || type
}
function matchTypeColor(type: string): string {
  const map: Record<string, string> = { EXACT: '', RULE: 'success', AI_SEMANTIC: 'warning', AI_SPLIT: 'danger' }
  return map[type] || 'info'
}
function statusType(status: string): string {
  const map: Record<string, string> = { AUTO_CONFIRMED: 'success', PENDING_REVIEW: 'warning', MANUAL_CONFIRMED: '', REJECTED: 'danger' }
  return map[status] || 'info'
}
function statusLabel(status: string): string {
  const map: Record<string, string> = { AUTO_CONFIRMED: '自动确认', PENDING_REVIEW: '待审核', MANUAL_CONFIRMED: '已确认', REJECTED: '已拒绝' }
  return map[status] || status
}
function confidenceType(conf: number): string {
  return conf >= 90 ? 'success' : conf >= 70 ? 'warning' : 'danger'
}

function parseDimensions(dims: Record<string, number> | string): Record<string, number> {
  if (typeof dims === 'string') {
    try { return JSON.parse(dims) } catch { return {} }
  }
  return dims || {}
}

function handleSelectionChange(rows: ReconMatch[]) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageMatch(query)
    tableData.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error('加载匹配结果失败: ' + (e?.message || '未知错误'))
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }

async function handleConfirm(row: ReconMatch) {
  try {
    await confirmMatch(row.id!, 1) // userId from store
    ElMessage.success('已确认匹配')
    loadData()
  } catch (e: any) {
    ElMessage.error('确认匹配失败: ' + (e?.message || '未知错误'))
  }
}

async function handleReject(row: ReconMatch) {
  let comment: string | undefined
  try {
    const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝匹配', {
      confirmButtonText: '确定', cancelButtonText: '取消'
    })
    comment = result.value || undefined
  } catch {
    // 用户取消操作
    return
  }
  try {
    await rejectMatch(row.id!, 1, comment)
    ElMessage.success('已拒绝匹配')
    loadData()
  } catch (e: any) {
    ElMessage.error('拒绝匹配失败: ' + (e?.message || '未知错误'))
  }
}

function showDetail(row: ReconMatch) {
  currentMatch.value = row
  detailVisible.value = true
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1400px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; color: #303133; }
.search-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.dimension-scores { display: flex; flex-wrap: wrap; gap: 4px; }
.dim-tag { font-size: 12px; padding: 0 8px; background: #f0f2f5; border-radius: 4px; color: #606266; }
.text-muted { color: #c0c4cc; }
</style>
