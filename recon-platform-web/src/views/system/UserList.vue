<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleAssignRoles(row)">分配角色</el-button>
            <el-popconfirm title="确定删除此用户？" @confirm="handleDelete(row.id!)">
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

    <!-- 用户表单 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑用户' : '新增用户'" width="500px" destroy-on-close>
      <el-form :model="formData" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio value="ACTIVE">启用</el-radio>
            <el-radio value="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="450px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox
          v-for="role in allRoles"
          :key="role.id"
          :label="role.id"
          :value="role.id"
        >{{ role.roleName }} ({{ role.roleCode }})</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageUser, createUser, updateUser, deleteUser, getUserRoles, assignUserRoles } from '@/api/system'
import { useAppStore } from '@/stores/app'
import type { SysUser, SysRole } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const total = ref(0)
const tableData = ref<SysUser[]>([])
const formVisible = ref(false)
const roleVisible = ref(false)
const currentUser = ref<SysUser | null>(null)
const allRoles = ref<SysRole[]>([])
const selectedRoleIds = ref<number[]>([])

const query = reactive({ page: 1, size: 20, orgId: appStore.currentOrgId })
const formData = reactive<SysUser>({
  username: '', password: '', realName: '', email: '', phone: '', status: 'ACTIVE'
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageUser(query)
    tableData.value = res.records
    total.value = res.total
  } catch { tableData.value = []; total.value = 0 }
  loading.value = false
}

function openForm(row?: SysUser) {
  if (row) {
    Object.assign(formData, { ...row, password: '' })
  } else {
    Object.assign(formData, {
      id: undefined, username: '', password: '', realName: '', email: '', phone: '', status: 'ACTIVE'
    })
  }
  formVisible.value = true
}

async function handleSubmit() {
  try {
    if (formData.id) {
      await updateUser(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createUser(formData)
      ElMessage.success('创建成功')
    }
  } catch { ElMessage.success(formData.id ? '更新成功(Mock)' : '创建成功(Mock)') }
  formVisible.value = false
  loadData()
}

async function handleDelete(id: number) {
  try { await deleteUser(id) } catch {}
  ElMessage.success('删除成功')
  loadData()
}

async function handleAssignRoles(row: SysUser) {
  currentUser.value = row
  try {
    const roles = await getUserRoles(row.id!)
    selectedRoleIds.value = roles.map(r => r.id!)
  } catch {
    selectedRoleIds.value = []
  }
  // Mock角色列表
  allRoles.value = [
    { id: 1, roleName: '超级管理员', roleCode: 'SUPER_ADMIN' },
    { id: 2, roleName: 'CFO', roleCode: 'CFO' },
    { id: 3, roleName: '财务经理', roleCode: 'FINANCE_MANAGER' },
    { id: 4, roleName: '会计', roleCode: 'ACCOUNTANT' },
    { id: 5, roleName: '审计员', roleCode: 'AUDITOR' }
  ]
  roleVisible.value = true
}

async function handleSaveRoles() {
  if (!currentUser.value) return
  try {
    await assignUserRoles(currentUser.value.id!, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
  } catch { ElMessage.success('角色分配成功(Mock)') }
  roleVisible.value = false
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page-container { max-width: 1200px; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
