<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:130px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="businessType" label="业务类型" width="100">
          <template #default="{ row }">{{ bizTypeLabel(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="businessId" label="业务ID" width="90" />
        <el-table-column prop="currentStep" label="当前步骤" width="80" align="center" />
        <el-table-column prop="approverName" label="提交人" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'APPROVED' ? '已通过' : row.status === 'REJECTED' ? '已拒绝' : '待审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="审批意见" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link size="small" @click="handleAction(row, 'APPROVE')">通过</el-button>
              <el-button type="danger" link size="small" @click="handleAction(row, 'REJECT')">拒绝</el-button>
            </template>
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMyApprovals, approve } from '@/api/workflow'
import type { WfApprovalRecord } from '@/types'

const loading = ref(false)
const total = ref(0)
const tableData = ref<WfApprovalRecord[]>([])

const query = reactive({ page: 1, size: 20, approverId: 1, status: '' })

function bizTypeLabel(type: string): string {
  const map: Record<string, string> = { DISCREPANCY: '差异', ADJUSTMENT: '调整', EXPORT: '导出' }
  return map[type] || type
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageMyApprovals(query)
    tableData.value = res.records
    total.value = res.total
  } catch (e: any) {
    ElMessage.error('加载审批列表失败: ' + (e?.message || '未知错误'))
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }

async function handleAction(row: WfApprovalRecord, action: string) {
  let comment: string | undefined
  try {
    const result = await ElMessageBox.prompt(
      `请输入${action === 'APPROVE' ? '审批' : '拒绝'}意见`,
      action === 'APPROVE' ? '审批通过' : '拒绝',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    comment = result.value || ''
  } catch {
    // 用户取消操作
    return
  }
  try {
    await approve(row.id!, {
      approverId: 1, approverName: '当前用户',
      action, comment: comment || ''
    })
    ElMessage.success(action === 'APPROVE' ? '已通过' : '已拒绝')
    loadData()
  } catch (e: any) {
    ElMessage.error('操作失败: ' + (e?.message || '未知错误'))
  }
}

function showDetail(row: WfApprovalRecord) {
  ElMessageBox.alert(
    `业务类型: ${bizTypeLabel(row.businessType)}\n业务ID: ${row.businessId}\n当前步骤: ${row.currentStep}\n状态: ${row.status}`,
    '审批详情'
  )
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1200px; }
.search-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
