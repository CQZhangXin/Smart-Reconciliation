<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="操作模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width:130px">
            <el-option label="数据源" value="datasource" />
            <el-option label="规则" value="rule" />
            <el-option label="对账任务" value="recon" />
            <el-option label="差异" value="discrepancy" />
            <el-option label="系统" value="system" />
            <el-option label="工作流" value="workflow" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="130" />
        <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="审计日志详情" width="550px">
      <el-descriptions v-if="currentLog" :column="1" border>
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ moduleLabel(currentLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ currentLog.detail }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { pageAuditLog } from '@/api/system'
import { useAppStore } from '@/stores/app'
import type { SysAuditLog } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const total = ref(0)
const tableData = ref<SysAuditLog[]>([])
const detailVisible = ref(false)
const currentLog = ref<SysAuditLog | null>(null)
const dateRange = ref<string[]>([])

const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId, module: '', startTime: '', endTime: '' })

function moduleLabel(m: string): string {
  const map: Record<string, string> = {
    datasource: '数据源', rule: '规则', recon: '对账任务',
    discrepancy: '差异', system: '系统', workflow: '工作流'
  }
  return map[m] || m
}

async function loadData() {
  loading.value = true
  if (dateRange.value?.length === 2) {
    query.startTime = dateRange.value[0]
    query.endTime = dateRange.value[1]
  }
  try {
    const res = await pageAuditLog(query)
    tableData.value = res.records
    total.value = res.total
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.module = ''; dateRange.value = []; handleSearch() }

function showDetail(row: SysAuditLog) {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1200px; }
.search-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
