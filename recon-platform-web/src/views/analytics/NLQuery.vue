<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span>🔍 自然语言查询 (Text-to-SQL)</span>
      </template>

      <!-- 查询输入 -->
      <div class="query-input-area">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="用自然语言描述你想查询的内容，例如：&#10;• 查询本月金额大于10000的未匹配差异记录&#10;• 统计各分类差异的数量和金额分布&#10;• 查询张三处理的超SLA差异"
          @keyup.enter.ctrl="handleQuery"
        />
        <div class="query-actions">
          <div class="query-hints">
            <span class="hint-label">快捷查询:</span>
            <el-tag
              v-for="hint in quickHints"
              :key="hint"
              size="small"
              class="hint-tag"
              @click="question = hint"
            >{{ hint }}</el-tag>
          </div>
          <el-button type="primary" :loading="querying" @click="handleQuery">
            <el-icon><Search /></el-icon> 查询
          </el-button>
        </div>
      </div>

      <!-- 查询结果 -->
      <div v-if="result" class="query-result">
        <el-divider />

        <!-- 生成的SQL -->
        <div class="result-section">
          <div class="section-header">
            <span>📝 生成SQL</span>
            <el-button size="small" @click="copySQL">复制SQL</el-button>
          </div>
          <pre class="sql-block">{{ result.generatedSql }}</pre>
        </div>

        <!-- AI回答 -->
        <div class="result-section">
          <div class="section-header">
            <span>💬 AI回答</span>
          </div>
          <el-alert :title="result.answer" type="success" :closable="false" show-icon />
        </div>

        <!-- 提取的实体 -->
        <div v-if="result.extractedEntities && Object.keys(result.extractedEntities).length > 0" class="result-section">
          <div class="section-header"><span>🏷️ 识别实体</span></div>
          <div class="entity-tags">
            <el-tag
              v-for="(val, key) in result.extractedEntities"
              :key="key"
              type="info"
              style="margin-right:8px;margin-bottom:4px"
            >{{ key }}: {{ val }}</el-tag>
          </div>
        </div>

        <!-- 数据表格 -->
        <div v-if="result.data && result.data.length > 0" class="result-section">
          <div class="section-header">
            <span>📊 查询结果 ({{ result.data.length }} 条, 耗时 {{ result.queryTimeMs }}ms)</span>
          </div>
          <el-table :data="result.data" border stripe max-height="400">
            <el-table-column
              v-for="col in dataColumns"
              :key="col"
              :prop="col"
              :label="col"
              min-width="120"
              show-overflow-tooltip
            />
          </el-table>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!result && !querying" description="输入问题后点击查询，AI将自动生成SQL并执行" :image-size="120" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { nlQuery } from '@/api/analytics'
import { useAppStore } from '@/stores/app'
import type { NLQueryResult } from '@/types'

const appStore = useAppStore()
const question = ref('')
const querying = ref(false)
const result = ref<NLQueryResult | null>(null)

const quickHints = [
  '查询本月所有差异记录',
  '统计各分类差异数量',
  '查询金额大于10000的差异',
  '查询超SLA未处理的差异',
  '统计本月匹配率'
]

const dataColumns = computed(() => {
  if (!result.value?.data?.length) return []
  return Object.keys(result.value.data[0])
})

async function handleQuery() {
  if (!question.value.trim()) {
    ElMessage.warning('请输入查询问题')
    return
  }
  querying.value = true
  result.value = null
  try {
    result.value = await nlQuery(question.value, appStore.currentOrgId)
  } catch (e: any) {
    ElMessage.error('查询失败: ' + (e?.message || '未知错误'))
  }
  querying.value = false
}

function copySQL() {
  if (result.value?.generatedSql) {
    navigator.clipboard.writeText(result.value.generatedSql)
    ElMessage.success('SQL已复制到剪贴板')
  }
}
</script>

<style scoped>
.page-container { max-width: 1000px; margin: 0 auto; }
.query-input-area { }
.query-actions { display: flex; align-items: flex-start; justify-content: space-between; margin-top: 12px; }
.query-hints { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; flex: 1; }
.hint-label { font-size: 12px; color: #909399; white-space: nowrap; }
.hint-tag { cursor: pointer; }
.query-result { margin-top: 8px; }
.result-section { margin-bottom: 20px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 600; color: #303133; }
.sql-block { background: #1e1e1e; color: #d4d4d4; padding: 12px 16px; border-radius: 8px; font-size: 13px; line-height: 1.6; overflow-x: auto; margin: 0; }
.entity-tags { padding: 4px 0; }
</style>
