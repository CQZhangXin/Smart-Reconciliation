<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 新建流程
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="processName" label="流程名称" min-width="180" />
        <el-table-column prop="processKey" label="流程标识" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="审批步骤" width="200">
          <template #default="{ row }">
            <el-tag
              v-for="(step, i) in (row.steps || [])"
              :key="i"
              size="small"
              style="margin-right:4px"
            >{{ i + 1 }}. {{ step.name }}</el-tag>
            <span v-if="!row.steps?.length" class="text-muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'DRAFT' ? 'info' : 'danger'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : row.status === 'DRAFT' ? '草稿' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              type="success" link size="small"
              @click="handlePublish(row)"
            >发布</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 流程表单 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑流程' : '新建流程'" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="流程名称" prop="processName">
          <el-input v-model="formData.processName" placeholder="如: 差异审批流程" />
        </el-form-item>
        <el-form-item label="流程标识" prop="processKey">
          <el-input v-model="formData.processKey" placeholder="如: discrepancy_approval" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="审批步骤">
          <div class="step-list">
            <div v-for="(step, i) in formData.steps || []" :key="i" class="step-item">
              <span>{{ i + 1 }}.</span>
              <el-input v-model="step.name" placeholder="步骤名称" size="small" style="width:140px" />
              <el-input v-model="step.approverRole" placeholder="审批角色" size="small" style="width:120px" />
              <el-button type="danger" :icon="Delete" circle size="small" @click="removeStep(i)" />
            </div>
            <el-button type="primary" link size="small" @click="addStep">
              <el-icon><Plus /></el-icon> 添加步骤
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { pageProcessDef, createProcessDef, publishProcessDef } from '@/api/workflow'
import { useAppStore } from '@/stores/app'
import type { WfProcessDefinition, ProcessStep } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<WfProcessDefinition[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()

const rules = {
  processName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  processKey: [{ required: true, message: '请输入流程KEY', trigger: 'blur' }]
}

const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId })
const formData = reactive<WfProcessDefinition>({
  processName: '', processKey: '', description: '', steps: []
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageProcessDef(query)
    tableData.value = res.records
  } catch (e: any) {
    ElMessage.error('加载流程定义失败: ' + (e?.message || '未知错误'))
    tableData.value = []
  }
  loading.value = false
}

function openForm(row?: WfProcessDefinition) {
  if (row) {
    Object.assign(formData, { ...row, steps: row.steps ? [...row.steps] : [] })
  } else {
    Object.assign(formData, {
      id: undefined, processName: '', processKey: '', description: '', steps: []
    })
  }
  formVisible.value = true
}

function addStep() {
  if (!formData.steps) formData.steps = []
  formData.steps.push({ order: formData.steps.length + 1, name: '', approverRole: '' })
}

function removeStep(index: number) {
  formData.steps?.splice(index, 1)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await createProcessDef(formData)
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e?.message || '未知错误'))
  }
  formVisible.value = false
  loadData()
}

async function handlePublish(row: WfProcessDefinition) {
  try {
    await publishProcessDef(row.id!)
    ElMessage.success('已发布')
  } catch (e: any) {
    ElMessage.error('发布失败: ' + (e?.message || '未知错误'))
  }
  loadData()
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1200px; }
.toolbar { margin-bottom: 16px; }
.text-muted { color: #c0c4cc; }
.step-list { width: 100%; }
.step-item { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
</style>
