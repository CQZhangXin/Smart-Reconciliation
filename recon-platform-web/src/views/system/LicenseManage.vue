<template>
  <div class="page-container">
    <div class="page-header">
      <h2>许可证管理</h2>
      <p class="subtitle">管理系统许可证，激活后可正常使用所有功能</p>
    </div>

    <!-- 许可证状态卡片 -->
    <el-row :gutter="20" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>许可证状态</span></template>
          <div class="stat-content">
            <el-tag :type="statusTagType" size="large" effect="dark">
              {{ statusLabel }}
            </el-tag>
            <p v-if="licenseInfo.orgName" style="margin-top: 12px">
              授权方：{{ licenseInfo.orgName }}
            </p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>有效期</span></template>
          <div class="stat-content">
            <p v-if="licenseInfo.expireDate">
              到期日期：<strong>{{ licenseInfo.expireDate }}</strong>
            </p>
            <p v-if="licenseInfo.remainingDays !== undefined && licenseInfo.remainingDays !== null">
              剩余天数：
              <strong :style="{ color: licenseInfo.remainingDays <= 30 ? '#f56c6c' : '#67c23a' }">
                {{ licenseInfo.remainingDays }} 天
              </strong>
            </p>
            <p v-if="licenseInfo.issuedAt">
              签发日期：{{ licenseInfo.issuedAt }}
            </p>
            <p v-if="!licenseInfo.licensed" style="color: #909399">—</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span>功能授权</span></template>
          <div class="stat-content">
            <p v-if="licenseInfo.maxUsers !== undefined && licenseInfo.maxUsers > 0">
              最大用户数：<strong>{{ licenseInfo.maxUsers }}</strong>
            </p>
            <p>
              授权功能：
            </p>
            <div v-if="licenseInfo.features && licenseInfo.features.length > 0" class="feature-tags">
              <el-tag
                v-for="f in licenseInfo.features"
                :key="f"
                size="small"
                style="margin: 2px"
              >
                {{ featureLabel(f) }}
              </el-tag>
            </div>
            <p v-else style="color: #909399">全部功能</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 激活区域 -->
    <el-card shadow="never" style="margin-top: 20px">
      <template #header><span>激活许可证</span></template>

      <div v-if="licenseInfo.licensed && licenseInfo.status === 'ACTIVE'" style="margin-bottom: 16px">
        <el-alert
          title="系统已激活"
          type="success"
          :closable="false"
          show-icon
          description="当前许可证有效，如需更换许可证请先吊销当前许可证。"
        />
      </div>

      <div v-if="licenseInfo.licensed && licenseInfo.status === 'EXPIRING_SOON'" style="margin-bottom: 16px">
        <el-alert
          :title="`许可证即将到期（剩余 ${licenseInfo.remainingDays} 天）`"
          type="warning"
          :closable="false"
          show-icon
          description="请尽快续期以避免影响正常使用。"
        />
      </div>

      <div v-if="licenseInfo.licensed && licenseInfo.status === 'EXPIRED'" style="margin-bottom: 16px">
        <el-alert
          title="许可证已过期"
          type="error"
          :closable="false"
          show-icon
          description="系统功能已受限，请立即续期。"
        />
      </div>

      <div v-if="!licenseInfo.licensed || !licenseEnabled" style="margin-bottom: 16px">
        <el-alert
          v-if="!licenseInfo.licensed"
          title="未找到许可证"
          type="info"
          :closable="false"
          show-icon
          description="请上传 .lic 许可证文件完成激活。"
        />
        <el-alert
          v-if="!licenseEnabled"
          title="许可证校验已关闭"
          type="warning"
          :closable="false"
          show-icon
          description="当前环境未启用许可证校验，系统不受限制。"
        />
      </div>

      <el-form label-width="120px" style="max-width: 600px">
        <el-form-item label="许可证文件 (.lic)">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".lic,.txt"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将许可证文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持 .lic / .txt 格式的许可证文件
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="许可证文本">
          <el-input
            v-model="licenseText"
            type="textarea"
            :rows="4"
            placeholder="或直接粘贴许可证文本内容..."
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="activating"
            :disabled="!canActivate"
            @click="handleActivate"
          >
            {{ licenseInfo.licensed ? '重新激活' : '激活许可证' }}
          </el-button>
          <el-button
            v-if="licenseInfo.licensed"
            type="danger"
            plain
            :loading="revoking"
            @click="handleRevoke"
          >
            吊销许可证
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  getLicenseStatus,
  activateLicense,
  activateLicenseText,
  revokeLicense,
  type LicenseStatus
} from '@/api/license'

const licenseInfo = ref<LicenseStatus>({
  licensed: false,
  licenseEnabled: true,
  status: 'UNLICENSED'
})

const selectedFile = ref<File | null>(null)
const licenseText = ref('')
const activating = ref(false)
const revoking = ref(false)
const licenseEnabled = ref(true)

const statusLabel = computed(() => {
  switch (licenseInfo.value.status) {
    case 'ACTIVE': return '已激活'
    case 'EXPIRED': return '已过期'
    case 'EXPIRING_SOON': return '即将过期'
    case 'UNLICENSED': return '未授权'
    case 'INVALID': return '无效许可证'
    case 'DISABLED': return '已关闭校验'
    default: return licenseInfo.value.status
  }
})

const statusTagType = computed(() => {
  switch (licenseInfo.value.status) {
    case 'ACTIVE': return 'success'
    case 'EXPIRED': return 'danger'
    case 'EXPIRING_SOON': return 'warning'
    case 'UNLICENSED': return 'info'
    case 'INVALID': return 'danger'
    case 'DISABLED': return 'info'
    default: return 'info'
  }
})

const canActivate = computed(() => {
  return !!selectedFile.value || licenseText.value.trim().length > 0
})

function featureLabel(feature: string): string {
  const map: Record<string, string> = {
    ALL: '全部功能',
    BANK_RECON: '银行对账',
    THIRD_PAYMENT: '三方支付对账',
    AR_AP_RECON: '应收应付对账',
    CROSS_SYSTEM: '跨系统对账',
    AI_MATCH: 'AI 语义匹配',
    NL_QUERY: '自然语言查询',
    AI_ANALYSIS: '智能分析',
    WORKFLOW: '审批工作流',
    OPEN_API: '开放 API'
  }
  return map[feature] || feature
}

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

async function handleActivate() {
  activating.value = true
  try {
    if (selectedFile.value) {
      const result = await activateLicense(selectedFile.value)
      licenseInfo.value = result
      ElMessage.success('许可证激活成功')
    } else if (licenseText.value.trim()) {
      const result = await activateLicenseText(licenseText.value.trim())
      licenseInfo.value = result
      ElMessage.success('许可证激活成功')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '激活失败')
  } finally {
    activating.value = false
  }
}

async function handleRevoke() {
  try {
    await ElMessageBox.confirm(
      '吊销许可证后系统将无法使用业务功能，确定继续？',
      '确认吊销',
      { type: 'warning', confirmButtonText: '确定吊销', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  revoking.value = true
  try {
    await revokeLicense()
    ElMessage.success('许可证已吊销')
    await loadStatus()
  } catch (e: any) {
    ElMessage.error(e.message || '吊销失败')
  } finally {
    revoking.value = false
  }
}

async function loadStatus() {
  try {
    const status = await getLicenseStatus()
    licenseInfo.value = status
    licenseEnabled.value = status.licenseEnabled
  } catch (e: any) {
    console.error('获取许可证状态失败:', e)
    licenseInfo.value = { licensed: false, licenseEnabled: true, status: 'UNLICENSED' }
  }
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 8px;
}
.page-header .subtitle {
  color: #909399;
  font-size: 14px;
  margin-top: 4px;
}
.stat-content {
  min-height: 80px;
}
.feature-tags {
  margin-top: 4px;
}
</style>
