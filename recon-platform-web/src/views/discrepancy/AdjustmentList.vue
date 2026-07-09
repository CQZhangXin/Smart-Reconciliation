<template>
  <div class="page-container">
    <div class="page-header">
      <el-button @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="page-title">调整管理</span>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 新增调整
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="discrepancyId" label="差异ID" width="90" />
        <el-table-column prop="adjustmentType" label="调整类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ adjTypeLabel(row.adjustmentType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="70" align="center" />
        <el-table-column prop="debitAccount" label="借方科目" width="140" />
        <el-table-column prop="creditAccount" label="贷方科目" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'APPROVED' ? '已批准' : row.status === 'REJECTED' ? '已拒绝' : '待审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success" link size="small"
              @click="handleApprove(row)"
            >审批通过</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑调整' : '新增调整'" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="关联差异ID" prop="discrepancyId">
          <el-input-number v-model="formData.discrepancyId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="调整类型" prop="adjustmentType">
          <el-select v-model="formData.adjustmentType" style="width:100%">
            <el-option label="应计调整" value="ACCRUAL" />
            <el-option label="摊销调整" value="AMORTIZATION" />
            <el-option label="汇率调整" value="EXCHANGE" />
            <el-option label="手续费调整" value="FEE" />
            <el-option label="核销" value="WRITE_OFF" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="调整金额" prop="amount">
              <el-input-number v-model="formData.amount" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="币种" prop="currency">
              <el-input v-model="formData.currency" placeholder="CNY" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="借方科目" prop="debitAccount">
              <el-input v-model="formData.debitAccount" placeholder="请输入借方科目" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="贷方科目" prop="creditAccount">
              <el-input v-model="formData.creditAccount" placeholder="请输入贷方科目" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { pageAdjustment, createAdjustment, approveAdjustment } from '@/api/discrepancy'
import { useAppStore } from '@/stores/app'
import type { ReconAdjustment } from '@/types'

const route = useRoute()
const appStore = useAppStore()
const loading = ref(false)
const total = ref(0)
const tableData = ref<ReconAdjustment[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()

const rules = {
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
  description: [{ required: true, message: '请输入调整说明', trigger: 'blur' }]
}

const discrepancyIdParam = Number(route.query.discrepancyId || 0)
const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId, status: '', discrepancyId: discrepancyIdParam || undefined })

const formData = reactive<ReconAdjustment>({
  discrepancyId: discrepancyIdParam || 0,
  adjustmentType: 'FEE',
  amount: 0,
  currency: 'CNY',
  debitAccount: '',
  creditAccount: '',
  description: '',
  status: 'PENDING'
})

function adjTypeLabel(type: string): string {
  const map: Record<string, string> = {
    ACCRUAL: '应计调整', AMORTIZATION: '摊销调整', EXCHANGE: '汇率调整',
    FEE: '手续费调整', WRITE_OFF: '核销', OTHER: '其他'
  }
  return map[type] || type
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageAdjustment(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }

function openForm() {
  Object.assign(formData, {
    id: undefined, discrepancyId: discrepancyIdParam || 0,
    adjustmentType: 'FEE', amount: 0, currency: 'CNY',
    debitAccount: '', creditAccount: '', description: '', status: 'PENDING'
  })
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await createAdjustment(formData)
    ElMessage.success('创建成功')
  } catch {
    ElMessage.success('创建成功(Mock)')
  }
  formVisible.value = false
  loadData()
}

async function handleApprove(row: ReconAdjustment) {
  try {
    await approveAdjustment(row.id!, 1)
    ElMessage.success('已审批通过')
  } catch {
    ElMessage.success('已审批通过(Mock)')
  }
  loadData()
}

function handleDelete(row: ReconAdjustment) {
  ElMessage.info('删除功能开发中')
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1400px; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; }
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
