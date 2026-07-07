<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="数据源类型">
          <el-select v-model="query.dsType" placeholder="全部" clearable style="width: 150px;">
            <el-option label="银行API" value="BANK_API" />
            <el-option label="第三方支付" value="THIRD_PAYMENT" />
            <el-option label="ERP系统" value="ERP" />
            <el-option label="文件导入" value="FILE_IMPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.dsCategory" placeholder="全部" clearable style="width: 120px;">
            <el-option label="A方" value="SOURCE_A" />
            <el-option label="B方" value="SOURCE_B" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px;">
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
        <el-icon><Plus /></el-icon> 新增数据源
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="dsName" label="数据源名称" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="openForm(row)">{{ row.dsName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="dsType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ dsTypeLabel(row.dsType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dsCategory" label="分类" width="80">
          <template #default="{ row }">
            <el-tag :type="row.dsCategory === 'SOURCE_A' ? 'primary' : 'success'" size="small">
              {{ row.dsCategory === 'SOURCE_A' ? 'A方' : 'B方' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthStatus" label="健康状态" width="100">
          <template #default="{ row }">
            <el-tag :type="healthType(row.healthStatus)" size="small">
              {{ healthLabel(row.healthStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ACTIVE'"
              @change="(val: boolean) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncAt" label="最后同步时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleTestConnection(row)">
              测试连接
            </el-button>
            <el-button type="success" link size="small" @click="handleSync(row)">
              同步
            </el-button>
            <el-button type="warning" link size="small" @click="openForm(row)">
              编辑
            </el-button>
            <el-popconfirm title="确定删除此数据源？" @confirm="handleDelete(row.id!)">
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <el-dialog
      v-model="formVisible"
      :title="formData.id ? '编辑数据源' : '新增数据源'"
      width="650px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="dsName">
              <el-input v-model="formData.dsName" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源类型" prop="dsType">
              <el-select v-model="formData.dsType" placeholder="请选择类型" style="width:100%">
                <el-option label="银行API" value="BANK_API" />
                <el-option label="第三方支付" value="THIRD_PAYMENT" />
                <el-option label="ERP系统" value="ERP" />
                <el-option label="文件导入" value="FILE_IMPORT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="dsCategory">
              <el-radio-group v-model="formData.dsCategory">
                <el-radio value="SOURCE_A">A方（本方）</el-radio>
                <el-radio value="SOURCE_B">B方（对方）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="同步策略" prop="syncStrategy">
              <el-select v-model="formData.syncStrategy" style="width:100%">
                <el-option label="全量同步" value="FULL" />
                <el-option label="增量同步" value="INCREMENTAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageDataSource, createDataSource, updateDataSource, deleteDataSource, testConnection, syncDataSource } from '@/api/datasource'
import { useAppStore } from '@/stores/app'
import type { DataSource } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const submitting = ref(false)
const total = ref(0)
const tableData = ref<DataSource[]>([])
const formVisible = ref(false)
const formRef = ref<FormInstance>()

const query = reactive({ page: 1, size: 20, dsType: '', dsCategory: '', status: '', orgId: appStore.currentOrgId })

const formData = reactive<DataSource>({
  dsName: '', dsType: '', dsCategory: 'SOURCE_A', syncStrategy: 'FULL', status: 'ACTIVE', description: ''
})

const formRules: FormRules = {
  dsName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  dsType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  dsCategory: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function dsTypeLabel(type: string): string {
  const map: Record<string, string> = { BANK_API: '银行API', THIRD_PAYMENT: '第三方支付', ERP: 'ERP系统', FILE_IMPORT: '文件导入' }
  return map[type] || type
}

function healthType(status: string): string {
  const map: Record<string, string> = { HEALTHY: 'success', UNHEALTHY: 'danger', UNKNOWN: 'info' }
  return map[status] || 'info'
}

function healthLabel(status: string): string {
  const map: Record<string, string> = { HEALTHY: '健康', UNHEALTHY: '异常', UNKNOWN: '未知' }
  return map[status] || status || '未知'
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageDataSource(query)
    tableData.value = res.records
    total.value = res.total
  } catch {
    // 接口未就绪时使用模拟数据
    tableData.value = []
    total.value = 0
  }
  loading.value = false
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.dsType = ''
  query.dsCategory = ''
  query.status = ''
  handleSearch()
}

function openForm(row?: DataSource) {
  if (row) {
    Object.assign(formData, { ...row })
  } else {
    Object.assign(formData, {
      id: undefined, dsName: '', dsType: '', dsCategory: 'SOURCE_A',
      syncStrategy: 'FULL', status: 'ACTIVE', description: ''
    })
  }
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (formData.id) {
      await updateDataSource(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createDataSource(formData)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch {
    // mock模式
    ElMessage.success(formData.id ? '更新成功(Mock)' : '创建成功(Mock)')
    formVisible.value = false
    loadData()
  }
  submitting.value = false
}

async function handleDelete(id: number) {
  try {
    await deleteDataSource(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.success('删除成功(Mock)')
    loadData()
  }
}

async function handleTestConnection(row: DataSource) {
  try {
    const ok = await testConnection(row.id!)
    ElMessage[ok ? 'success' : 'error'](ok ? '连接测试成功' : '连接测试失败')
  } catch {
    ElMessage.success('连接测试成功(Mock)')
  }
}

async function handleSync(row: DataSource) {
  try {
    await syncDataSource(row.id!)
    ElMessage.success('同步任务已提交')
  } catch {
    ElMessage.success('同步任务已提交(Mock)')
  }
}

function handleToggleStatus(row: DataSource, val: boolean) {
  ElMessage.info(`${val ? '启用' : '停用'}数据源: ${row.dsName} (Mock)`)
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container {
  max-width: 1400px;
}
.search-card {
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
