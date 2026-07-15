<template>
  <el-dialog
    :model-value="visible"
    title="智能创建对账方案"
    width="680px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <!-- 输入阶段 -->
    <template v-if="!result && !parsingError">
      <div class="nl-hint">
        用自然语言描述你的对账需求，AI 将自动为你生成对账方案配置。
      </div>

      <el-input
        v-model="description"
        type="textarea"
        :rows="4"
        placeholder="例如：对账工商银行流水和SAP系统数据，按金额精确匹配，日期允许1天差异"
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
        v-if="result.aiExplanation"
        :title="result.aiExplanation"
        type="info"
        :closable="false"
        class="nl-explanation"
      />

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="方案名称">
          {{ result.definition.defName }}
        </el-descriptions-item>
        <el-descriptions-item label="方案编码">
          {{ result.definition.defCode }}
        </el-descriptions-item>
        <el-descriptions-item label="主数据源A">
          <el-tag v-if="result.definition.sourceAId" type="primary" size="small">
            {{ getSourceName(result.definition.sourceAId) }}
          </el-tag>
          <el-tag v-else type="danger" size="small">未匹配</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对账方B">
          <template v-if="result.definition.sourceBIds?.length">
            <el-tag
              v-for="bid in result.definition.sourceBIds"
              :key="bid"
              type="success"
              size="small"
              class="source-b-tag"
            >
              {{ getSourceName(bid) }}
            </el-tag>
          </template>
          <el-tag v-else type="danger" size="small">未匹配</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="期间类型">
          {{ result.definition.periodType === 'DAILY' ? '每日' : result.definition.periodType === 'MONTHLY' ? '每月' : '自定义' }}
        </el-descriptions-item>
        <el-descriptions-item label="默认期间">
          {{ result.definition.defaultPeriod || '未指定' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 未匹配警告 -->
      <div v-if="result.unresolvedSources?.length || result.unresolvedRules?.length" class="nl-warnings">
        <el-alert
          v-if="result.unresolvedSources?.length"
          :title="'未匹配的数据源: ' + result.unresolvedSources.join('、') + '，请进入向导后手动选择'"
          type="warning"
          :closable="false"
        />
        <el-alert
          v-if="result.unresolvedRules?.length"
          :title="'未匹配的规则: ' + result.unresolvedRules.join('、') + '，请进入向导后手动选择'"
          type="warning"
          :closable="false"
          style="margin-top: 8px"
        />
      </div>
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
          进入配置向导
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { nlParseCustomRecon } from '@/api/customRecon'
import type { NLParseResult } from '@/types'

const props = defineProps<{
  visible: boolean
  orgId: number
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [result: NLParseResult]
}>()

const description = ref('')
const parsing = ref(false)
const result = ref<NLParseResult | null>(null)
const parsingError = ref('')

const quickHints = [
  '对账工商银行流水和SAP系统数据，按金额精确匹配',
  '每月对比支付宝和微信支付的交易记录，金额和日期都要匹配',
  '每日对账银行API流水与ERP数据库的收款记录，允许1天时间差',
  '对账Oracle应付账款与供应商对账单，需要AI语义匹配'
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
    result.value = await nlParseCustomRecon({
      description: description.value.trim(),
      orgId: props.orgId
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

function getSourceName(sourceId: number): string {
  return '数据源 #' + sourceId
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

.nl-warnings {
  margin-top: 16px;
}

.source-b-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}
</style>
