<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width:140px">
            <el-option label="时间差异" value="TIME_DIFF" />
            <el-option label="手续费差异" value="FEE_DIFF" />
            <el-option label="汇率差异" value="EXCHANGE_DIFF" />
            <el-option label="人为错误" value="HUMAN_ERROR" />
            <el-option label="未达账项" value="UNREACHED" />
            <el-option label="重复交易" value="DUPLICATE" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="query.riskLevel" placeholder="全部" clearable style="width:120px">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="success" :loading="classifying" @click="handleBatchClassify">
        <el-icon><MagicStick /></el-icon> 批量AI分类
      </el-button>
      <el-dropdown @command="handleExportCommand" style="margin-left:8px">
        <el-button>
          导出 <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="excel">导出Excel</el-dropdown-item>
            <el-dropdown-item command="csv">导出CSV</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="side" label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.side === 'SOURCE_A' ? 'primary' : 'success'" size="small">
              {{ row.side === 'SOURCE_A' ? '仅A方' : '仅B方' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="AI分类" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.category" :type="categoryColor(row.category)" size="small">
              {{ categoryLabel(row.category) }}
            </el-tag>
            <el-tag v-else type="info" size="small">未分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">
            {{ row.amount?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="amountDiff" label="差异金额" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: (row.amountDiff ?? 0) !== 0 ? '#f56c6c' : '' }">
              {{ row.amountDiff?.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="70" align="center" />
        <el-table-column prop="riskLevel" label="风险等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="riskColor(row.riskLevel!)" size="small">{{ riskLabel(row.riskLevel!) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="100" />
        <el-table-column prop="slaDeadline" label="SLA截止" width="170">
          <template #default="{ row }">
            <span :style="{ color: isSlaOverdue(row.slaDeadline!) ? '#f56c6c' : '' }">
              {{ row.slaDeadline }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/discrepancy/${row.id}`)">
              详情
            </el-button>
            <el-button
              v-if="!row.category"
              type="success" link size="small"
              @click="handleClassify(row)"
            >AI分类</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning" link size="small"
              @click="handleAssign(row)"
            >分配</el-button>
            <el-button
              v-if="row.status === 'PROCESSING'"
              type="success" link size="small"
              @click="handleResolve(row)"
            >解决</el-button>
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

    <!-- 分配对话框 -->
    <el-dialog v-model="assignVisible" title="分配处理人" width="400px">
      <el-form :model="assignForm" label-width="80px">
        <el-form-item label="处理人">
          <el-select v-model="assignForm.handlerId" placeholder="请选择" style="width:100%">
            <el-option label="张三" :value="1" />
            <el-option label="李四" :value="2" />
            <el-option label="王五" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 解决对话框 -->
    <el-dialog v-model="resolveVisible" title="解决差异" width="500px">
      <el-form :model="resolveForm" label-width="90px">
        <el-form-item label="解决方式">
          <el-select v-model="resolveForm.resolution" style="width:100%">
            <el-option label="确认差异(接受)" value="ACCEPT" />
            <el-option label="创建调整分录" value="ADJUST" />
            <el-option label="标记为重复" value="DUPLICATE" />
            <el-option label="忽略" value="IGNORE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resolveForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResolveSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageDiscrepancy, classifyDiscrepancy, batchClassify, assignDiscrepancy, resolveDiscrepancy } from '@/api/discrepancy'
import { useAppStore } from '@/stores/app'
import type { ReconDiscrepancy } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const classifying = ref(false)
const total = ref(0)
const tableData = ref<ReconDiscrepancy[]>([])
const selectedRows = ref<ReconDiscrepancy[]>([])

const assignVisible = ref(false)
const resolveVisible = ref(false)
const currentRow = ref<ReconDiscrepancy | null>(null)

const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId, category: '', riskLevel: '', status: '' })
const assignForm = reactive({ handlerId: 1, handlerName: '张三' })
const resolveForm = reactive({ resolution: 'ACCEPT', note: '' })

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
function statusType(status: string): string {
  const map: Record<string, string> = { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', CLOSED: '' }
  return map[status] || 'info'
}
function statusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[status] || status
}
function isSlaOverdue(deadline: string): boolean {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

function handleSelectionChange(rows: ReconDiscrepancy[]) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageDiscrepancy(query)
    tableData.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error('加载差异记录失败: ' + (e?.message || '未知错误'))
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.category = ''; query.riskLevel = ''; query.status = ''; handleSearch() }

async function handleClassify(row: ReconDiscrepancy) {
  try {
    await classifyDiscrepancy(row.id!)
    ElMessage.success('AI分类完成')
    loadData()
  } catch (e: any) {
    ElMessage.error('AI分类失败: ' + (e?.message || '未知错误'))
  }
}

async function handleBatchClassify() {
  classifying.value = true
  try {
    await batchClassify(query.orgId!)
    ElMessage.success('批量分类已提交')
  } catch (e: any) {
    ElMessage.error('批量分类失败: ' + (e?.message || '未知错误'))
  }
  classifying.value = false
  loadData()
}

function handleAssign(row: ReconDiscrepancy) {
  currentRow.value = row
  assignVisible.value = true
}

async function handleAssignSubmit() {
  if (!currentRow.value) return
  try {
    await assignDiscrepancy(currentRow.value.id!, assignForm.handlerId, assignForm.handlerName)
    ElMessage.success('分配成功')
  } catch (e: any) {
    ElMessage.error('分配失败: ' + (e?.message || '未知错误'))
  }
  assignVisible.value = false
  loadData()
}

function handleResolve(row: ReconDiscrepancy) {
  currentRow.value = row
  resolveVisible.value = true
}

async function handleResolveSubmit() {
  if (!currentRow.value) return
  try {
    await resolveDiscrepancy(currentRow.value.id!, { ...resolveForm })
    ElMessage.success('已解决')
  } catch (e: any) {
    ElMessage.error('解决失败: ' + (e?.message || '未知错误'))
  }
  resolveVisible.value = false
  loadData()
}

function handleExportCommand(command: string) {
  ElMessage.info(`导出${command === 'excel' ? 'Excel' : 'CSV'}功能开发中`)
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1400px; }
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
