import { get, post, put, del } from './request'
import type { SysUser, SysRole, SysPermission, SysAuditLog, OrgOrganization, OrgAccount, PageResult, PageQuery } from '@/types'

// ========== 用户管理 ==========

export function pageUser(params: PageQuery): Promise<PageResult<SysUser>> {
  return get('/system/user/page', params)
}
export function getUser(id: number): Promise<SysUser> {
  return get(`/system/user/${id}`)
}
export function createUser(data: SysUser): Promise<SysUser> {
  return post('/system/user', data)
}
export function updateUser(id: number, data: SysUser): Promise<SysUser> {
  return put(`/system/user/${id}`, data)
}
export function deleteUser(id: number): Promise<void> {
  return del(`/system/user/${id}`)
}
export function assignUserRoles(userId: number, roleIds: number[]): Promise<void> {
  return put(`/system/user/${userId}/roles`, roleIds)
}
export function getUserRoles(userId: number): Promise<SysRole[]> {
  return get(`/system/user/${userId}/roles`)
}
export function getUserPermissions(userId: number): Promise<SysPermission[]> {
  return get(`/system/user/${userId}/permissions`)
}

// ========== 角色管理 ==========

export function pageRole(params: PageQuery): Promise<PageResult<SysRole>> {
  return get('/system/role/page', params)
}
export function createRole(data: SysRole): Promise<SysRole> {
  return post('/system/role', data)
}
export function assignRolePermissions(roleId: number, permIds: number[]): Promise<void> {
  return put(`/system/role/${roleId}/permissions`, permIds)
}

// ========== 权限管理 ==========

export function listAllPermissions(): Promise<SysPermission[]> {
  return get('/system/permission/list')
}

// ========== 审计日志 ==========

export function pageAuditLog(params: PageQuery): Promise<PageResult<SysAuditLog>> {
  return get('/system/audit-log/page', params)
}

// ========== 组织管理 ==========

export function pageOrg(params: PageQuery): Promise<PageResult<OrgOrganization>> {
  return get('/system/org/page', params)
}
export function createOrg(data: OrgOrganization): Promise<OrgOrganization> {
  return post('/system/org', data)
}

// ========== 账户管理 ==========

export function pageAccount(params: PageQuery): Promise<PageResult<OrgAccount>> {
  return get('/system/account/page', params)
}
export function createAccount(data: OrgAccount): Promise<OrgAccount> {
  return post('/system/account', data)
}
