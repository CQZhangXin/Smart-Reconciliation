<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="方案名称/编码"
            clearable
            style="width:180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="toolbar">
      <el-button type="primary" @click="goWizard()">
        <el-icon><Plus /></el-icon>
        新建对账方案
      </el-button>
      <el-button type="success" @click="nlPromptVisible = true">
        <el-icon><MagicStick /></el-icon>
        智能创建
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="defName" label="方案名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="goWizard(row.id)">
              {{ row.defName }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="defCode" label="方案编码" width="140" />
        <el-table-column label="数据源" min-width="200">
          <template #default="{ row }">
            <span>A:{{ row.sourceAId }}</span>
            <el-divider direction="vertical" />
            <span>B×{{ (row.sourceBIds || []).length }}</span>
          </template>
        </el-table-column>
        <el-table-column label="规则数" width="80" align="center">
          <template #default="{ row }">
            {{ (row.ruleIds || []).length || '全部' }}
          </template>
        </el-table-column>
        <el-table-column prop="periodType" label="期间类型" width="100">
          <template #default="{ row }">
            {{ periodTypeLabel(row.periodType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastRunAt" label="最近执行" width="170" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goWizard(row.id)">
              编辑
            </el-button>
            <el-button type="success" link size="small" @click="handleValidate(row)">
              预检
            </el-button>
            <el-button type="warning" link size="small" @click="openRunDialog(row)">
              执行
            </el-button>
            <el-button
              v-if="row.status !== 'ACTIVE'"
              type="success"
              link
              size="small"
              @click="handleEnable(row)"
            >
              启用
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="info"
              link
              size="small"
              @click="handleDisable(row)"
            >
              停用
            </el-button>
            <el-button
              v-if="row.lastRunTaskId"
              type="primary"
              link
              size="small"
              @click="goMatchReview(row.lastRunTaskId)"
            >
              结果
            </el-button>
            <el-popconfirm title="确定删除此方案？" @confirm="handleDelete(row.id!)">
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

    <CustomReconNLPrompt
      v-model:visible="nlPromptVisible"
      :org-id="appStore.currentOrgId"
      @success="onNLCreateSuccess"
    />

    <el-dialog v-model="runVisible" title="执行自定义对账" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="方案">
          <span>{{ currentRow?.defName }}</span>
        </el-form-item>
        <el-form-item label="对账期间">
          <el-input v-model="runForm.reconPeriod" placeholder="如 2026-07" />
        </el-form-item>
        <el-form-item label="异步执行">
          <el-switch v-model="runForm.async" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="runVisible = false">取消</el-button>
        <el-button type="primary" :loading="running" @click="handleRun">开始执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageCustomRecon,
  deleteCustomRecon,
  enableCustomRecon,
  disableCustomRecon,
  validateCustomRecon,
  runCustomRecon
} from '@/api/customRecon'
import { useAppStore } from '@/stores/app'
import {
  CUSTOM_RECON_STATUS_LABEL,
  CUSTOM_RECON_STATUS_COLOR
} from '@/utils/constants'
import type { CustomReconDefinition, NLParseResult } from '@/types'
import CustomReconNLPrompt from './CustomReconNLPrompt.vue'

defineOptions({ name: 'CustomReconList' })

const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const running = ref(false)
const total = ref(0)
const tableData = ref<CustomReconDefinition[]>([])
const runVisible = ref(false)
const currentRow = ref<CustomReconDefinition | null>(null)
const nlPromptVisible = ref(false)

const NL_PARSE_STORAGE_KEY = 'CUSTOM_RECON_NL_PARSE'

function onNLCreateSuccess(result: NLParseResult) {
  sessionStorage.setItem(NL_PARSE_STORAGE_KEY, JSON.stringify(result))
  nlPromptVisible.value = false
  router.push('/custom-recon/wizard')
}

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  status: '',
  orgId: appStore.currentOrgId
})

const runForm = reactive({
  reconPeriod: '',
  async: true
})

function statusLabel(status?: string) {
  return CUSTOM_RECON_STATUS_LABEL[status || ''] || status || '-'
}

function statusColor(status?: string) {
  return (CUSTOM_RECON_STATUS_COLOR[status || ''] || 'info') as any
}

function periodTypeLabel(type?: string) {
  const map: Record<string, string> = {
    DAILY: '日',
    MONTHLY: '月',
    CUSTOM: '自定义'
  }
  return map[type || ''] || type || '-'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageCustomRecon({ ...query })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  loadData()
}

function goWizard(id?: number) {
  if (id) {
    router.push(`/custom-recon/wizard/${id}`)
  } else {
    router.push('/custom-recon/wizard')
  }
}

function goMatchReview(taskId: number) {
  router.push(`/recon/match/${taskId}`)
}

async function handleDelete(id: number) {
  try {
    await deleteCustomRecon(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

async function handleEnable(row: CustomReconDefinition) {
  try {
    await enableCustomRecon(row.id!)
    ElMessage.success('启用成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '启用失败')
  }
}

async function handleDisable(row: CustomReconDefinition) {
  try {
    await disableCustomRecon(row.id!)
    ElMessage.success('已停用')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '停用失败')
  }
}

async function handleValidate(row: CustomReconDefinition) {
  try {
    const result = await validateCustomRecon(row.id!)
    const lines = [
      result.valid ? '预检通过' : '预检未通过',
      `待对账记录 A:${result.pendingRecordA} / B:${result.pendingRecordB}`,
      `启用规则数: ${result.activeRuleCount}`
    ]
    if (result.errors?.length) {
      lines.push('错误: ' + result.errors.join('；'))
    }
    if (result.warnings?.length) {
      lines.push('警告: ' + result.warnings.join('；'))
    }
    await ElMessageBox.alert(lines.join('\n'), '方案预检', { type: result.valid ? 'success' : 'warning' })
  } catch (e: any) {
    ElMessage.error(e?.message || '预检失败')
  }
}

function openRunDialog(row: CustomReconDefinition) {
  currentRow.value = row
  runForm.reconPeriod = row.defaultPeriod || ''
  runForm.async = true
  runVisible.value = true
}

async function handleRun() {
  if (!currentRow.value?.id) return
  running.value = true
  try {
    const result = await runCustomRecon(currentRow.value.id, {
      reconPeriod: runForm.reconPeriod || undefined,
      async: runForm.async
    })
    ElMessage.success(result.message || '执行已启动')
    runVisible.value = false
    loadData()
    if (result.primaryTaskId) {
      router.push(`/recon/match/${result.primaryTaskId}`)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '执行失败')
  } finally {
    running.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  padding: 0;
}

.search-card {
  margin-bottom: 12px;
}

.toolbar {
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
