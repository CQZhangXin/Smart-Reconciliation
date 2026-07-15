<template>
  <el-dialog
    :model-value="visible"
    title="智能创建审批流程"
    width="680px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <!-- 输入阶段 -->
    <template v-if="!result && !parsingError">
      <div class="nl-hint">
        用自然语言描述你的审批流程需求，AI 将自动为你生成审批流程配置。
      </div>

      <el-input
        v-model="description"
        type="textarea"
        :rows="4"
        placeholder="例如：差异处理需要先部门经理审批，然后财务总监复核"
        maxlength="500"
        show-word-limit
        @keyup.enter.ctrl="handleParse"
      />

      <div class="quick-hints">
        <span
          v-for="hint in quickHints"
          :key="hint"
          class="hint-tag"
          @click="description = hint"
        >{{ hint }}</span>
      </div>
    </template>

    <!-- 加载中 -->
    <div v-else-if="parsing" class="nl-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>AI 正在解析你的描述...</p>
    </div>

    <!-- 解析错误 -->
    <div v-else-if="parsingError" class="nl-error">
      <el-result icon="error" title="解析失败" :sub-title="parsingError" />
      <el-button type="primary" @click="resetInput">重新描述</el-button>
    </div>

    <!-- 解析成功 - 展示结果摘要 -->
    <template v-else>
      <el-alert
        v-if="result.definition.aiExplanation"
        :title="result.definition.aiExplanation"
        type="info"
        :closable="false"
        class="nl-explanation"
      />

      <el-alert
        v-if="result.warning"
        :title="result.warning"
        type="warning"
        :closable="false"
        class="nl-warning"
      />

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="流程名称">
          {{ result.definition.processName }}
        </el-descriptions-item>
        <el-descriptions-item label="流程标识">
          {{ result.definition.processKey }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ result.definition.description || '未指定' }}
        </el-descriptions-item>
        <el-descriptions-item label="审批步骤" :span="2">
          <template v-if="result.definition.steps?.length">
            <el-tag
              v-for="(step, i) in result.definition.steps"
              :key="i"
              size="small"
              style="margin-right:8px;margin-bottom:4px"
            >
              {{ step.order }}. {{ step.name }}
              <template v-if="step.approverRole">
                ({{ step.approverRole }})
              </template>
            </el-tag>
          </template>
          <span v-else class="text-muted">未解析出审批步骤</span>
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <template #footer>
      <template v-if="!result && !parsingError">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button
          type="primary"
          :loading="parsing"
          :disabled="!description.trim()"
          @click="handleParse"
        >
          智能解析
        </el-button>
      </template>
      <template v-else-if="parsingError">
        <el-button @click="$emit('update:visible', false)">取消</el-button>
        <el-button type="primary" @click="resetInput">重新描述</el-button>
      </template>
      <template v-else>
        <el-button @click="resetInput">重新描述</el-button>
        <el-button type="primary" @click="handleConfirm">
          应用配置
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { nlParseProcessDef } from '@/api/workflow'
import type { NLWorkflowParseResult } from '@/types'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [result: NLWorkflowParseResult]
}>()

const description = ref('')
const parsing = ref(false)
const result = ref<NLWorkflowParseResult | null>(null)
const parsingError = ref('')

const quickHints = [
  '差异处理需要先部门经理审批，然后财务总监复核',
  '报销审批：部门负责人 → 财务审核 → 总经理批准',
  '对账结果确认：财务专员初审 → 财务经理终审',
  '数据源变更申请：运维审核 → 管理员审批'
]

// 恢复初始状态
watch(() => props.visible, (val) => {
  if (val) {
    description.value = ''
    result.value = null
    parsingError.value = ''
    parsing.value = false
  }
})

async function handleParse() {
  if (!description.value.trim() || parsing.value) return
  parsing.value = true
  parsingError.value = ''
  try {
    result.value = await nlParseProcessDef({
      description: description.value.trim()
    })
  } catch (e: any) {
    parsingError.value = e?.message || '解析失败，请检查网络或AI服务状态'
  }
  parsing.value = false
}

function handleConfirm() {
  if (result.value) {
    emit('success', result.value)
  }
}

function resetInput() {
  result.value = null
  parsingError.value = ''
  description.value = ''
}
</script>

<style scoped>
.nl-hint {
  color: #909399;
  margin-bottom: 12px;
  font-size: 14px;
}

.quick-hints {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hint-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: background .2s;
}

.hint-tag:hover {
  background: #e6f0ff;
  color: #409eff;
}

.nl-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 0;
  color: #909399;
}

.nl-error {
  padding: 20px 0;
}

.nl-explanation {
  margin-bottom: 16px;
}

.nl-warning {
  margin-bottom: 16px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
