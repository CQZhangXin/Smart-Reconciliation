<template>
  <div class="page-container">
    <el-card shadow="never" class="header-card">
      <h3 class="title">大模型配置</h3>
      <p class="subtitle">
        系统可接入国内大模型 DeepSeek、通义千问（Qwen）、Kimi（Moonshot）等，统一使用 OpenAI 兼容协议
      </p>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <span>当前接入状态</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="提供商">
              <el-tag :type="config.mockMode ? 'info' : 'success'">
                {{ config.displayName || config.provider || '-' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="实现类">
              {{ config.implementation || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="Base URL">
              {{ config.baseUrl || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="API Key">
              <el-tag :type="config.apiKeyConfigured ? 'success' : 'warning'" size="small">
                {{ config.apiKeyConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="默认模型">
              {{ config.defaultModel || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="轻量模型">
              {{ config.simpleModel || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="推理模型">
              {{ config.reasoningModel || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            class="mt-16"
            :type="config.mockMode ? 'info' : 'success'"
            :closable="false"
            :title="config.message || ''"
          />
          <div class="actions">
            <el-button type="primary" :loading="testing" @click="handleTest">
              连通性测试
            </el-button>
            <el-button @click="loadConfig">刷新</el-button>
          </div>
          <el-input
            v-if="testResult"
            class="mt-16"
            type="textarea"
            :rows="6"
            readonly
            :model-value="testResult"
          />
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span>支持的国内 / 国际大模型</span>
          </template>
          <el-table :data="providers" stripe>
            <el-table-column prop="name" label="提供商" width="160">
              <template #default="{ row }">
                <el-tag v-if="row.active" type="success" size="small">当前</el-tag>
                {{ row.name }}
              </template>
            </el-table-column>
            <el-table-column prop="code" label="配置值" width="100" />
            <el-table-column prop="baseUrl" label="默认 Base URL" min-width="220" show-overflow-tooltip />
            <el-table-column prop="tip" label="说明" min-width="220" show-overflow-tooltip />
          </el-table>

          <el-divider>切换方式（修改配置后重启后端）</el-divider>
          <pre class="config-sample">{{ configSample }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, listAiProviders, testAiConnection } from '@/api/ai'

defineOptions({ name: 'AiModelConfig' })

const loading = ref(false)
const testing = ref(false)
const testResult = ref('')
const providers = ref<Record<string, any>[]>([])
const config = reactive<Record<string, any>>({
  provider: 'mock',
  mockMode: true
})

const configSample = `# application.yml 或环境变量示例
ai:
  llm:
    provider: deepseek          # deepseek | qwen | kimi | openai | custom
    # 也可只配环境变量：
    # AI_PROVIDER=qwen
    # DASHSCOPE_API_KEY=sk-xxx

# DeepSeek
# AI_PROVIDER=deepseek
# DEEPSEEK_API_KEY=sk-xxx

# 通义千问
# AI_PROVIDER=qwen
# DASHSCOPE_API_KEY=sk-xxx

# Kimi
# AI_PROVIDER=kimi
# MOONSHOT_API_KEY=sk-xxx`

async function loadConfig() {
  loading.value = true
  try {
    const [cfg, list] = await Promise.all([getAiConfig(), listAiProviders()])
    Object.keys(config).forEach((k) => delete config[k])
    Object.assign(config, cfg || {})
    providers.value = list || cfg?.supportedProviders || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载 AI 配置失败')
  } finally {
    loading.value = false
  }
}

async function handleTest() {
  testing.value = true
  testResult.value = ''
  try {
    const res = await testAiConnection()
    testResult.value = JSON.stringify(res, null, 2)
    if (res.success) {
      ElMessage.success(res.message || '测试成功')
    } else {
      ElMessage.warning(res.message || '测试失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '测试失败')
  } finally {
    testing.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.header-card {
  margin-bottom: 16px;
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

.mt-16 {
  margin-top: 16px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}

.config-sample {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
