<template>
  <div class="page-container">
    <el-card shadow="never" class="header-card">
      <div class="header-row">
        <div>
          <h3 class="title">{{ isEdit ? '编辑对账方案' : '新建对账方案' }}</h3>
          <p class="subtitle">
            自由组合多数据源（银行API / HTTP接口 / ERP / 文件 / 数据库等）与自定义规则，一键发起对账
          </p>
        </div>
        <el-button @click="router.push('/custom-recon')">返回列表</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-steps :active="step" finish-status="success" align-center class="steps">
        <el-step title="基本信息" />
        <el-step title="选择数据源" />
        <el-step title="配置规则" />
        <el-step title="确认保存" />
      </el-steps>

      <!-- Step 1 -->
      <div v-show="step === 0" class="step-body">
        <el-form ref="basicFormRef" :model="form" :rules="basicRules" label-width="120px" style="max-width:640px">
          <el-form-item label="方案名称" prop="defName">
            <el-input v-model="form.defName" placeholder="如：银行流水 vs ERP+支付多源对账" />
          </el-form-item>
          <el-form-item label="方案编码" prop="defCode">
            <el-input v-model="form.defCode" placeholder="唯一编码，如 CUSTOM_BANK_MULTI" />
          </el-form-item>
          <el-form-item label="期间类型" prop="periodType">
            <el-radio-group v-model="form.periodType">
              <el-radio value="DAILY">日</el-radio>
              <el-radio value="MONTHLY">月</el-radio>
              <el-radio value="CUSTOM">自定义</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="默认期间">
            <el-input v-model="form.defaultPeriod" placeholder="如 2026-07" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="方案说明" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2 -->
      <div v-show="step === 1" class="step-body">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="支持多数据源对账：选择一个主数据源 A，可选择一个或多个对账方数据源 B（HTTP接口、银行API、ERP、文件、数据库等）"
          class="mb-16"
        />
        <el-form label-width="120px" style="max-width:720px">
          <el-form-item label="主数据源 A" required>
            <el-select
              v-model="form.sourceAId"
              filterable
              placeholder="请选择主数据源"
              style="width:100%"
            >
              <el-option
                v-for="ds of sourceOptions"
                :key="ds.id"
                :label="`${ds.dsName} (${dsTypeLabel(ds.dsType)})`"
                :value="ds.id!"
                :disabled="form.sourceBIds.includes(ds.id!)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="对账方 B" required>
            <el-select
              v-model="form.sourceBIds"
              multiple
              filterable
              placeholder="可多选，支持接口/文件/库表等多源"
              style="width:100%"
            >
              <el-option
                v-for="ds of sourceOptions"
                :key="ds.id"
                :label="`${ds.dsName} (${dsTypeLabel(ds.dsType)})`"
                :value="ds.id!"
                :disabled="ds.id === form.sourceAId"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button link type="primary" @click="router.push('/datasource')">
              去数据源管理新增接口/文件等连接
            </el-button>
            <el-button link type="primary" @click="loadSources">刷新数据源</el-button>
          </el-form-item>
        </el-form>

        <el-table v-if="selectedSources.length" :data="selectedSources" size="small" stripe>
          <el-table-column prop="role" label="角色" width="80" />
          <el-table-column prop="dsName" label="名称" min-width="160" />
          <el-table-column prop="dsType" label="类型" width="120">
            <template #default="{ row }">
              {{ dsTypeLabel(row.dsType) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" />
          <el-table-column prop="healthStatus" label="健康" width="100" />
        </el-table>
      </div>

      <!-- Step 3 -->
      <div v-show="step === 2" class="step-body">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="可勾选本方案专用规则；不选则使用组织下全部启用规则。也可跳转规则引擎用自然语言生成新规则。"
          class="mb-16"
        />
        <div class="toolbar-inline">
          <el-button link type="primary" @click="router.push('/rule')">规则引擎</el-button>
          <el-button link type="primary" @click="loadRules">刷新规则</el-button>
        </div>
        <el-table
          ref="ruleTableRef"
          v-loading="ruleLoading"
          :data="ruleOptions"
          stripe
          @selection-change="onRuleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="ruleName" label="规则名称" min-width="160" />
          <el-table-column prop="ruleCode" label="编码" width="140" />
          <el-table-column prop="ruleType" label="类型" width="120" />
          <el-table-column prop="priority" label="优先级" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>

        <el-divider>匹配层</el-divider>
        <el-checkbox v-model="form.matchLayers.exact">精确匹配</el-checkbox>
        <el-checkbox v-model="form.matchLayers.rule">规则匹配</el-checkbox>
        <el-checkbox v-model="form.matchLayers.ai">AI语义匹配</el-checkbox>
        <el-checkbox v-model="form.matchLayers.split">拆单匹配</el-checkbox>
      </div>

      <!-- Step 4 -->
      <div v-show="step === 3" class="step-body">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="方案名称">{{ form.defName }}</el-descriptions-item>
          <el-descriptions-item label="方案编码">{{ form.defCode }}</el-descriptions-item>
          <el-descriptions-item label="期间类型">{{ form.periodType }}</el-descriptions-item>
          <el-descriptions-item label="默认期间">{{ form.defaultPeriod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主数据源A">
            {{ sourceName(form.sourceAId) }}
          </el-descriptions-item>
          <el-descriptions-item label="对账方B">
            {{ form.sourceBIds.map((id) => sourceName(id)).join('、') }}
          </el-descriptions-item>
          <el-descriptions-item label="选用规则" :span="2">
            {{ form.ruleIds.length ? form.ruleIds.map((id) => ruleName(id)).join('、') : '组织全部启用规则' }}
          </el-descriptions-item>
          <el-descriptions-item label="匹配层" :span="2">
            {{ matchLayerText }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ form.description || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="footer-actions">
        <el-button v-if="step > 0" @click="step -= 1">上一步</el-button>
        <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="step === 3" type="primary" :loading="saving" @click="handleSave">
          保存方案
        </el-button>
        <el-button v-if="step === 3 && isEdit" type="success" :loading="saving" @click="handleSaveAndRun">
          保存并执行
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, TableInstance } from 'element-plus'
import { pageDataSource } from '@/api/datasource'
import { pageRule } from '@/api/rule'
import {
  createCustomRecon,
  getCustomRecon,
  updateCustomRecon,
  runCustomRecon
} from '@/api/customRecon'
import { useAppStore } from '@/stores/app'
import { DATA_SOURCE_TYPE_LABEL } from '@/utils/constants'
import type { CustomReconDefinition, DataSource, ReconRuleConfig } from '@/types'

defineOptions({ name: 'CustomReconWizard' })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const step = ref(0)
const saving = ref(false)
const ruleLoading = ref(false)
const basicFormRef = ref<FormInstance>()
const ruleTableRef = ref<TableInstance>()
const sourceOptions = ref<DataSource[]>([])
const ruleOptions = ref<ReconRuleConfig[]>([])

const editId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})
const isEdit = computed(() => !!editId.value)

const form = reactive({
  defName: '',
  defCode: '',
  description: '',
  sourceAId: undefined as number | undefined,
  sourceBIds: [] as number[],
  ruleIds: [] as number[],
  matchLayers: {
    exact: true,
    rule: true,
    ai: true,
    split: true
  },
  periodType: 'MONTHLY',
  defaultPeriod: '',
  status: 'DRAFT'
})

const basicRules: FormRules = {
  defName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  defCode: [{ required: true, message: '请输入方案编码', trigger: 'blur' }],
  periodType: [{ required: true, message: '请选择期间类型', trigger: 'change' }]
}

const selectedSources = computed(() => {
  const rows: Array<DataSource & { role: string }> = []
  const a = sourceOptions.value.find((s) => s.id === form.sourceAId)
  if (a) rows.push({ ...a, role: 'A' })
  for (const id of form.sourceBIds) {
    const b = sourceOptions.value.find((s) => s.id === id)
    if (b) rows.push({ ...b, role: 'B' })
  }
  return rows
})

const matchLayerText = computed(() => {
  const parts: string[] = []
  if (form.matchLayers.exact) parts.push('精确')
  if (form.matchLayers.rule) parts.push('规则')
  if (form.matchLayers.ai) parts.push('AI语义')
  if (form.matchLayers.split) parts.push('拆单')
  return parts.join(' / ') || '-'
})

function dsTypeLabel(type?: string) {
  return DATA_SOURCE_TYPE_LABEL[type || ''] || type || '-'
}

function sourceName(id?: number) {
  if (!id) return '-'
  const ds = sourceOptions.value.find((s) => s.id === id)
  return ds ? `${ds.dsName}(${dsTypeLabel(ds.dsType)})` : String(id)
}

function ruleName(id: number) {
  const rule = ruleOptions.value.find((r) => r.id === id)
  return rule?.ruleName || String(id)
}

async function loadSources() {
  const res = await pageDataSource({
    page: 1,
    size: 200,
    orgId: appStore.currentOrgId,
    status: 'ACTIVE'
  })
  sourceOptions.value = res.records || []
}

async function loadRules() {
  ruleLoading.value = true
  try {
    const res = await pageRule({
      page: 1,
      size: 200,
      orgId: appStore.currentOrgId
    })
    ruleOptions.value = res.records || []
    await nextTick()
    restoreRuleSelection()
  } finally {
    ruleLoading.value = false
  }
}

function restoreRuleSelection() {
  if (!ruleTableRef.value) return
  ruleTableRef.value.clearSelection()
  for (const rule of ruleOptions.value) {
    if (rule.id && form.ruleIds.includes(rule.id)) {
      ruleTableRef.value.toggleRowSelection(rule, true)
    }
  }
}

function onRuleSelectionChange(rows: ReconRuleConfig[]) {
  form.ruleIds = rows.map((r) => r.id!).filter(Boolean)
}

async function nextStep() {
  if (step.value === 0) {
    const valid = await basicFormRef.value?.validate().catch(() => false)
    if (!valid) return
  }
  if (step.value === 1) {
    if (!form.sourceAId) {
      ElMessage.warning('请选择主数据源 A')
      return
    }
    if (!form.sourceBIds.length) {
      ElMessage.warning('请至少选择一个对账方数据源 B')
      return
    }
  }
  step.value += 1
  if (step.value === 2) {
    await nextTick()
    restoreRuleSelection()
  }
}

function buildPayload(): CustomReconDefinition {
  return {
    defName: form.defName,
    defCode: form.defCode,
    description: form.description,
    sourceAId: form.sourceAId!,
    sourceBIds: [...form.sourceBIds],
    ruleIds: [...form.ruleIds],
    matchLayers: { ...form.matchLayers },
    periodType: form.periodType,
    defaultPeriod: form.defaultPeriod || undefined,
    status: form.status,
    orgId: appStore.currentOrgId
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && editId.value) {
      await updateCustomRecon(editId.value, payload)
      ElMessage.success('方案已更新')
    } else {
      const created = await createCustomRecon(payload)
      ElMessage.success('方案已创建')
      router.replace(`/custom-recon/wizard/${created.id}`)
    }
    router.push('/custom-recon')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveAndRun() {
  if (!editId.value) return
  saving.value = true
  try {
    await updateCustomRecon(editId.value, buildPayload())
    const result = await runCustomRecon(editId.value, {
      reconPeriod: form.defaultPeriod || undefined,
      async: true
    })
    ElMessage.success(result.message || '已开始执行')
    router.push(`/recon/match/${result.primaryTaskId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存或执行失败')
  } finally {
    saving.value = false
  }
}

async function loadDetail() {
  if (!editId.value) return
  const detail = await getCustomRecon(editId.value)
  form.defName = detail.defName
  form.defCode = detail.defCode
  form.description = detail.description || ''
  form.sourceAId = detail.sourceAId
  form.sourceBIds = detail.sourceBIds || []
  form.ruleIds = detail.ruleIds || []
  form.periodType = detail.periodType || 'MONTHLY'
  form.defaultPeriod = detail.defaultPeriod || ''
  form.status = detail.status || 'DRAFT'
  if (detail.matchLayers) {
    form.matchLayers.exact = detail.matchLayers.exact !== false
    form.matchLayers.rule = detail.matchLayers.rule !== false
    form.matchLayers.ai = detail.matchLayers.ai !== false
    form.matchLayers.split = detail.matchLayers.split !== false
  }
}

function applyNLParseResult(result: any) {
  const def = result.definition
  form.defName = def.defName || ''
  form.defCode = def.defCode || ''
  form.description = def.description || ''
  form.sourceAId = def.sourceAId
  form.sourceBIds = def.sourceBIds || []
  form.ruleIds = def.ruleIds || []
  form.periodType = def.periodType || 'MONTHLY'
  form.defaultPeriod = def.defaultPeriod || ''
  form.status = 'DRAFT'
  if (def.matchLayers) {
    form.matchLayers.exact = def.matchLayers.exact !== false
    form.matchLayers.rule = def.matchLayers.rule !== false
    form.matchLayers.ai = def.matchLayers.ai !== false
    form.matchLayers.split = def.matchLayers.split !== false
  }
  nextTick(() => restoreRuleSelection())
  const warnings: string[] = []
  if (result.aiExplanation) {
    warnings.push('AI解析: ' + result.aiExplanation)
  }
  if (result.unresolvedSources?.length) {
    warnings.push('未匹配的数据源: ' + result.unresolvedSources.join('、') + '，请手动选择')
  }
  if (result.unresolvedRules?.length) {
    warnings.push('未匹配的规则: ' + result.unresolvedRules.join('、') + '，请手动选择')
  }
  if (warnings.length > 0) {
    ElMessage.warning(warnings.join('\n'))
  } else if (result.aiExplanation) {
    ElMessage.success('AI已自动填充方案配置，请检查确认')
  }
}

const NL_PARSE_STORAGE_KEY = 'CUSTOM_RECON_NL_PARSE'

onMounted(async () => {
  await Promise.all([loadSources(), loadRules()])
  if (isEdit.value) {
    await loadDetail()
    await nextTick()
    restoreRuleSelection()
  } else {
    const nlData = sessionStorage.getItem(NL_PARSE_STORAGE_KEY)
    if (nlData) {
      try {
        const parseResult = JSON.parse(nlData)
        applyNLParseResult(parseResult)
      } catch (e) {
        console.warn('解析NL预填数据失败', e)
      } finally {
        sessionStorage.removeItem(NL_PARSE_STORAGE_KEY)
      }
    }
  }
})
</script>

<style scoped>
.header-card {
  margin-bottom: 12px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title {
  margin: 0 0 6px;
  font-size: 18px;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.steps {
  margin-bottom: 28px;
}

.step-body {
  min-height: 280px;
  padding: 0 12px 12px;
}

.mb-16 {
  margin-bottom: 16px;
}

.toolbar-inline {
  margin-bottom: 12px;
}

.footer-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
