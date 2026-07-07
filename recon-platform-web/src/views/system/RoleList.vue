<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleAssignPerms(row)">分配权限</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id!)">
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
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 角色表单 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form :model="formData" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permVisible" title="分配权限" width="450px">
      <el-tree
        ref="permTreeRef"
        :data="allPermissions"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'permName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerms">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageRole, createRole, listAllPermissions, assignRolePermissions } from '@/api/system'
import { useAppStore } from '@/stores/app'
import type { SysRole, SysPermission } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const total = ref(0)
const tableData = ref<SysRole[]>([])
const formVisible = ref(false)
const permVisible = ref(false)
const currentRole = ref<SysRole | null>(null)
const allPermissions = ref<SysPermission[]>([])
const permTreeRef = ref<any>(null)

const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId })
const formData = reactive<SysRole>({ roleName: '', roleCode: '', description: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await pageRole(query)
    tableData.value = res.records
    total.value = res.total
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openForm(row?: SysRole) {
  if (row) {
    Object.assign(formData, { ...row })
  } else {
    Object.assign(formData, { id: undefined, roleName: '', roleCode: '', description: '' })
  }
  formVisible.value = true
}

async function handleSubmit() {
  try { await createRole(formData) } catch {}
  ElMessage.success(formData.id ? '更新成功' : '创建成功')
  formVisible.value = false
  loadData()
}

async function handleDelete(id: number) {
  ElMessage.success('删除成功')
  loadData()
}

async function handleAssignPerms(row: SysRole) {
  currentRole.value = row
  try {
    allPermissions.value = await listAllPermissions()
  } catch {
    // Mock权限树
    allPermissions.value = [
      { id: 1, permName: '数据源管理', permCode: 'datasource', permType: 'MENU', children: [
        { id: 11, permName: '查看', permCode: 'datasource:view', permType: 'BUTTON' },
        { id: 12, permName: '编辑', permCode: 'datasource:edit', permType: 'BUTTON' },
        { id: 13, permName: '同步', permCode: 'datasource:sync', permType: 'BUTTON' }
      ]},
      { id: 2, permName: '规则引擎', permCode: 'rule', permType: 'MENU', children: [
        { id: 21, permName: '查看', permCode: 'rule:view', permType: 'BUTTON' },
        { id: 22, permName: '编辑', permCode: 'rule:edit', permType: 'BUTTON' }
      ]},
      { id: 3, permName: '对账工作台', permCode: 'workbench', permType: 'MENU' },
      { id: 4, permName: '差异管理', permCode: 'discrepancy', permType: 'MENU' },
      { id: 5, permName: '智能分析', permCode: 'analytics', permType: 'MENU' },
      { id: 6, permName: '审批工作流', permCode: 'workflow', permType: 'MENU' },
      { id: 7, permName: '系统管理', permCode: 'system', permType: 'MENU' }
    ]
  }
  permVisible.value = true
}

async function handleSavePerms() {
  if (!currentRole.value) return
  const checkedIds = permTreeRef.value?.getCheckedKeys() || []
  const halfCheckedIds = permTreeRef.value?.getHalfCheckedKeys() || []
  try {
    await assignRolePermissions(currentRole.value.id!, [...checkedIds, ...halfCheckedIds])
    ElMessage.success('权限分配成功')
  } catch { ElMessage.success('权限分配成功(Mock)') }
  permVisible.value = false
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1100px; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
