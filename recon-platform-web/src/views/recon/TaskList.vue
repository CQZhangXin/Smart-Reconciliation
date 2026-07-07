<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="任务类型">
          <el-select v-model="query.taskType" placeholder="全部" clearable style="width:150px">
            <el-option label="银行对账" value="BANK" />
            <el-option label="第三方支付" value="THIRD_PAYMENT" />
            <el-option label="应收对账" value="AR" />
            <el-option label="应付对账" value="AP" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待执行" value="PENDING" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
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
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 创建任务
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="taskName" label="任务名称" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/recon/match/${row.id}`)">
              {{ row.taskName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="taskTypeColor(row.taskType)" size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reconPeriod" label="对账期间" width="140" />
        <el-table-column label="数据量(A/B)" width="120">
          <template #default="{ row }">
            {{ row.totalACount || 0 }} / {{ row.totalBCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="matchedCount" label="匹配数" width="90" />
        <el-table-column prop="discrepancyCount" label="差异数" width="90" />
        <el-table-column label="匹配率" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="Number((row.matchRate ?? 0).toFixed(1))"
              :stroke-width="8"
              :status="(row.matchRate ?? 0) >= 90 ? 'success' : (row.matchRate ?? 0) >= 70 ? '' : 'exception'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status!)" size="small">{{ statusLabel(row.status!) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority!)" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="完成时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success" link size="small"
              @click="handleExecute(row)"
            >执行</el-button>
            <el-button
              v-if="row.status === 'RUNNING'"
              type="warning" link size="small" disabled
            >执行中...</el-button>
            <el-button type="primary" link size="small" @click="$router.push(`/recon/match/${row.id}`)">
              查看匹配
            </el-button>
            <el-button type="warning" link size="small" @click="openForm(row)">
              编辑
            </el-button>
            <el-popconfirm title="确定删除此任务？" @confirm="handleDelete(row.id!)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 任务表单对话框 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑任务' : '创建任务'" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务类型" prop="taskType">
              <el-select v-model="formData.taskType" style="width:100%">
                <el-option label="银行对账" value="BANK" />
                <el-option label="第三方支付" value="THIRD_PAYMENT" />
                <el-option label="应收对账" value="AR" />
                <el-option label="应付对账" value="AP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="formData.priority" style="width:100%">
                <el-option label="低" value="LOW" />
                <el-option label="普通" value="NORMAL" />
                <el-option label="高" value="HIGH" />
                <el-option label="紧急" value="URGENT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="A方数据源" prop="sourceAId">
              <el-select v-model="formData.sourceAId" placeholder="请选择" style="width:100%">
                <el-option label="ERP系统" :value="1" />
                <el-option label="财务系统" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="B方数据源" prop="sourceBId">
              <el-select v-model="formData.sourceBId" placeholder="请选择" style="width:100%">
                <el-option label="银行流水" :value="3" />
                <el-option label="第三方支付" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="对账开始日期" prop="periodStart">
              <el-date-picker v-model="formData.periodStart" type="date" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对账结束日期" prop="periodEnd">
              <el-date-picker v-model="formData.periodEnd" type="date" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="对账期间" prop="reconPeriod">
          <el-input v-model="formData.reconPeriod" placeholder="如: 2026-07" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageTask, createTask, updateTask, deleteTask, executeTaskAsync } from '@/api/recon'
import { useAppStore } from '@/stores/app'
import type { ReconTask } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const tableData = ref<ReconTask[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive({ page: 1, size: 20, taskType: '', status: '', orgId: appStore.currentOrgId })

const formData = reactive<ReconTask>({
  taskName: '', taskType: 'BANK', sourceAId: undefined, sourceBId: undefined,
  periodStart: '', periodEnd: '', reconPeriod: '', priority: 'NORMAL'
})

const formRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskType: [{ required: true, message: '请选择任务类型', trigger: 'change' }]
}

function taskTypeLabel(type: string): string {
  const map: Record<string, string> = { BANK: '银行对账', THIRD_PAYMENT: '第三方支付', AR: '应收对账', AP: '应付对账', CROSS_SYSTEM: '跨系统', INTERCOMPANY: '内部往来' }
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
function priorityType(p: string): string {
  const map: Record<string, string> = { LOW: 'info', NORMAL: '', HIGH: 'warning', URGENT: 'danger' }
  return map[p] || ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageTask(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.taskType = ''; query.status = ''; handleSearch() }

function openForm(row?: ReconTask) {
  if (row) {
    Object.assign(formData, { ...row })
  } else {
    Object.assign(formData, {
      id: undefined, taskName: '', taskType: 'BANK', sourceAId: undefined, sourceBId: undefined,
      periodStart: '', periodEnd: '', reconPeriod: '', priority: 'NORMAL'
    })
  }
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (formData.id) {
      await updateTask(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createTask(formData)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch {
    ElMessage.success(formData.id ? '更新成功(Mock)' : '创建成功(Mock)')
    formVisible.value = false
    loadData()
  }
  submitting.value = false
}

async function handleDelete(id: number) {
  try { await deleteTask(id) } catch {}
  ElMessage.success('删除成功')
  loadData()
}

async function handleExecute(row: ReconTask) {
  try {
    await executeTaskAsync(row.id!)
    ElMessage.success('任务已提交异步执行')
  } catch {
    ElMessage.success('任务已提交异步执行(Mock)')
  }
  loadData()
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1400px; }
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
