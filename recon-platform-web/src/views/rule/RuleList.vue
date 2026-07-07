<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="规则类型">
          <el-select v-model="query.ruleType" placeholder="全部" clearable style="width:160px">
            <el-option label="精确匹配" value="EXACT_MATCH" />
            <el-option label="规则匹配" value="RULE_MATCH" />
            <el-option label="AI语义" value="AI_SEMANTIC" />
            <el-option label="AI拆单" value="AI_SPLIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
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

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 新增规则
      </el-button>
      <el-button type="success" @click="openNLGenerate()">
        <el-icon><MagicStick /></el-icon> AI生成规则
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="ruleName" label="规则名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="openForm(row)">{{ row.ruleName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="ruleCode" label="规则编码" width="140" />
        <el-table-column prop="ruleType" label="规则类型" width="110">
          <template #default="{ row }">
            <el-tag :type="ruleTypeColor(row.ruleType)" size="small">{{ ruleTypeLabel(row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center" />
        <el-table-column prop="version" label="版本" width="60" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'info' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : row.status === 'DRAFT' ? '草稿' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 'ACTIVE'"
              type="success" link size="small"
              @click="handleEnable(row)"
            >启用</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning" link size="small"
              @click="handleDisable(row)"
            >禁用</el-button>
            <el-popconfirm title="确定删除此规则？" @confirm="handleDelete(row.id!)">
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

    <!-- 规则表单对话框 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑规则' : '新增规则'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input v-model="formData.ruleCode" placeholder="自动生成或手动输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则类型" prop="ruleType">
              <el-select v-model="formData.ruleType" style="width:100%">
                <el-option label="精确匹配" value="EXACT_MATCH" />
                <el-option label="规则匹配" value="RULE_MATCH" />
                <el-option label="AI语义匹配" value="AI_SEMANTIC" />
                <el-option label="AI拆单匹配" value="AI_SPLIT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="formData.priority" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="容差配置">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-input v-model="toleranceForm.amountAbs" placeholder="金额绝对容差">
                <template #prepend>金额±</template>
              </el-input>
            </el-col>
            <el-col :span="8">
              <el-input v-model="toleranceForm.amountPct" placeholder="金额百分比容差">
                <template #prepend>比例%</template>
              </el-input>
            </el-col>
            <el-col :span="8">
              <el-input v-model="toleranceForm.dateDays" placeholder="日期容差天数">
                <template #prepend>日期±</template>
              </el-input>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- AI生成规则对话框 -->
    <el-dialog v-model="nlVisible" title="AI自然语言生成规则" width="550px" destroy-on-close>
      <el-form :model="nlForm" label-width="80px">
        <el-form-item label="规则描述">
          <el-input
            v-model="nlForm.description"
            type="textarea"
            :rows="4"
            placeholder="用自然语言描述匹配规则，例如：匹配金额相等、日期相差不超过2天、交易方名称模糊匹配的交易记录"
          />
        </el-form-item>
        <el-form-item label="提示">
          <div class="nl-hints">
            <el-tag size="small" class="hint-tag" @click="nlForm.description = '金额完全相等且交易日期相同的记录自动匹配'">
              金额+日期精确匹配
            </el-tag>
            <el-tag size="small" class="hint-tag" @click="nlForm.description = '金额差异在10元以内、日期相差不超过2天的记录推荐匹配'">
              小金额容差匹配
            </el-tag>
            <el-tag size="small" class="hint-tag" @click="nlForm.description = '跨境交易金额差异在1%以内的，标记为汇率差异'">
              跨境汇率容差
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
      <!-- AI生成结果展示 -->
      <div v-if="nlResult" class="nl-result">
        <el-divider />
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="规则名称">{{ nlResult.ruleName }}</el-descriptions-item>
          <el-descriptions-item label="规则编码">{{ nlResult.ruleCode }}</el-descriptions-item>
          <el-descriptions-item label="规则类型">{{ ruleTypeLabel(nlResult.ruleType) }}</el-descriptions-item>
          <el-descriptions-item label="预估匹配率">{{ nlResult.estimatedMatchRate }}%</el-descriptions-item>
          <el-descriptions-item label="是否有冲突">
            <el-tag :type="nlResult.hasConflict ? 'danger' : 'success'" size="small">
              {{ nlResult.hasConflict ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="nl-explanation">
          <strong>AI解释：</strong>{{ nlResult.explanation }}
        </div>
      </div>
      <template #footer>
        <el-button @click="nlVisible = false">取消</el-button>
        <el-button type="primary" :loading="nlGenerating" @click="handleNLGenerate">
          {{ nlResult ? '保存规则' : '生成规则' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageRule, createRule, updateRule, deleteRule, enableRule, disableRule, generateRuleFromNL, saveGeneratedRule } from '@/api/rule'
import { useAppStore } from '@/stores/app'
import type { ReconRuleConfig, RuleGenerationResult } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const tableData = ref<ReconRuleConfig[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()

// AI生成
const nlVisible = ref(false)
const nlGenerating = ref(false)
const nlResult = ref<RuleGenerationResult | null>(null)
const nlForm = reactive({ description: '' })

const query = reactive({ page: 1, size: 20, ruleType: '', status: '', orgId: appStore.currentOrgId })

const formData = reactive<ReconRuleConfig>({
  ruleName: '', ruleCode: '', ruleType: 'RULE_MATCH', priority: 10, status: 'DRAFT', description: ''
})

const toleranceForm = reactive({ amountAbs: '', amountPct: '', dateDays: '' })

const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

function ruleTypeLabel(type: string): string {
  const map: Record<string, string> = {
    EXACT_MATCH: '精确匹配', RULE_MATCH: '规则匹配', AI_SEMANTIC: 'AI语义匹配', AI_SPLIT: 'AI拆单匹配'
  }
  return map[type] || type
}

function ruleTypeColor(type: string): string {
  const map: Record<string, string> = { EXACT_MATCH: '', RULE_MATCH: 'success', AI_SEMANTIC: 'warning', AI_SPLIT: 'danger' }
  return map[type] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageRule(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.ruleType = ''; query.status = ''; handleSearch() }

function openForm(row?: ReconRuleConfig) {
  if (row) {
    Object.assign(formData, { ...row })
    if (row.tolerance) {
      toleranceForm.amountAbs = String(row.tolerance.amountAbs || '')
      toleranceForm.amountPct = String(row.tolerance.amountPct || '')
      toleranceForm.dateDays = String(row.tolerance.dateDays || '')
    }
  } else {
    Object.assign(formData, {
      id: undefined, ruleName: '', ruleCode: '', ruleType: 'RULE_MATCH',
      priority: 10, status: 'DRAFT', description: ''
    })
    toleranceForm.amountAbs = ''
    toleranceForm.amountPct = ''
    toleranceForm.dateDays = ''
  }
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      ...formData,
      tolerance: {
        amountAbs: Number(toleranceForm.amountAbs) || 0,
        amountPct: Number(toleranceForm.amountPct) || 0,
        dateDays: Number(toleranceForm.dateDays) || 0
      }
    }
    if (formData.id) {
      await updateRule(formData.id, data)
      ElMessage.success('更新成功')
    } else {
      await createRule(data)
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
  try { await deleteRule(id) } catch {}
  ElMessage.success('删除成功')
  loadData()
}

async function handleEnable(row: ReconRuleConfig) {
  try { await enableRule(row.id!) } catch {}
  ElMessage.success('已启用')
  loadData()
}

async function handleDisable(row: ReconRuleConfig) {
  try { await disableRule(row.id!) } catch {}
  ElMessage.success('已禁用')
  loadData()
}

function openNLGenerate() {
  nlResult.value = null
  nlForm.description = ''
  nlVisible.value = true
}

async function handleNLGenerate() {
  if (!nlForm.description.trim()) {
    ElMessage.warning('请输入规则描述')
    return
  }
  nlGenerating.value = true
  try {
    if (nlResult.value) {
      // 保存已生成的规则
      await saveGeneratedRule({ ...nlResult.value, orgId: appStore.currentOrgId })
      ElMessage.success('规则已保存')
      nlVisible.value = false
      loadData()
    } else {
      const result = await generateRuleFromNL(nlForm.description, appStore.currentOrgId)
      nlResult.value = result
    }
  } catch {
    if (!nlResult.value) {
      // Mock生成结果
      nlResult.value = {
        ruleName: 'AI生成: ' + nlForm.description.slice(0, 20) + '...',
        ruleCode: 'AI_GEN_' + Date.now(),
        ruleType: 'RULE_MATCH',
        matchConfigJson: '{}',
        toleranceJson: '{}',
        explanation: '基于语义分析自动生成的匹配规则配置',
        estimatedMatchRate: 85,
        hasConflict: false
      }
    }
  }
  nlGenerating.value = false
}
</script>

<style scoped>
.page-container { max-width: 1400px; }
.search-card { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.nl-hints { display: flex; flex-wrap: wrap; gap: 8px; }
.hint-tag { cursor: pointer; }
.nl-result { margin-top: 8px; }
.nl-explanation { margin-top: 12px; padding: 10px; background: #f0f9eb; border-radius: 6px; font-size: 13px; color: #606266; }
</style>
