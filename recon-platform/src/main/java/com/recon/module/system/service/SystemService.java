package com.recon.module.system.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import com.recon.module.system.entity.*;
import com.recon.module.system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理服务 — 用户、角色、权限、组织、账套、账户、审计日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final OrgOrganizationMapper orgMapper;
    private final OrgLedgerMapper ledgerMapper;
    private final OrgAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    // ============================================================
    // User
    // ============================================================

    /**
     * 创建用户 — BCrypt编码密码, 默认ACTIVE状态。
     *
     * <p>不修改输入参数: 内部创建副本进行密码编码和插入操作。</p>
     */
    @Transactional
    public SysUser createUser(SysUser user) {
        // 创建副本以避免修改调用方的对象
        SysUser create = new SysUser();
        BeanUtil.copyProperties(user, create);
        create.setPassword(passwordEncoder.encode(user.getPassword()));
        if (create.getStatus() == null || create.getStatus().isBlank()) {
            create.setStatus("ACTIVE");
        }
        userMapper.insert(create);
        // 返回结果中清空密码
        create.setPassword(null);
        return create;
    }

    /**
     * 按ID查询用户（不清除密码，供认证流程使用）
     */
    public SysUser getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 更新用户登录信息
     */
    public void updateLoginInfo(Long userId, LocalDateTime lastLoginAt, String clientIp) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(lastLoginAt);
            user.setLastLoginIp(clientIp);
            userMapper.updateById(user);
        }
    }

    /**
     * 按用户名查询
     */
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 分页查询用户 — 清除密码字段
     */
    public IPage<SysUser> pageUsers(int page, int size, Long orgId, String status) {
        Page<SysUser> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(SysUser::getOrgId, orgId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        IPage<SysUser> result = userMapper.selectPage(pageObj, wrapper);
        for (SysUser user : result.getRecords()) {
            user.setPassword(null);
        }
        return result;
    }

    /**
     * 更新用户 — 密码非空时重新编码, 其他非空字段拷贝
     */
    public SysUser updateUser(Long id, SysUser update) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(update.getPassword()));
        }
        // 将密码置空避免被 copyProperties 覆盖为明文
        String rawPassword = update.getPassword();
        update.setPassword(null);

        BeanUtil.copyProperties(update, existing, CopyOptions.create().ignoreNullValue());

        // 恢复 update 对象的密码字段(调用方可能需要)
        update.setPassword(rawPassword);

        userMapper.updateById(existing);

        SysUser result = userMapper.selectById(id);
        result.setPassword(null);
        return result;
    }

    /**
     * 逻辑删除用户 — 不允许删除超级管理员(id=1)
     */
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (id == 1L) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不允许删除超级管理员");
        }
        userMapper.deleteById(id);
        // 同时清理用户-角色关联
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, id);
        userRoleMapper.delete(urWrapper);
    }

    // ============================================================
    // User-Role
    // ============================================================

    /**
     * 为用户分配角色 — 先删后批量插
     */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        userRoleMapper.delete(wrapper);

        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> newRows = roleIds.stream()
                    .map(rid -> new SysUserRole()
                            .setUserId(userId)
                            .setRoleId(rid)
                            .setCreatedAt(LocalDateTime.now()))
                    .collect(Collectors.toList());
            Db.saveBatch(newRows);
        }
    }

    /**
     * 获取用户拥有的角色列表
     */
    public List<SysRole> getUserRoles(Long userId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> urList = userRoleMapper.selectList(wrapper);

        List<Long> roleIds = urList.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    // ============================================================
    // Role
    // ============================================================

    /**
     * 创建角色
     */
    public SysRole createRole(SysRole role) {
        roleMapper.insert(role);
        return role;
    }

    /**
     * 更新角色
     */
    public SysRole updateRole(Long id, SysRole role) {
        role.setId(id);
        roleMapper.updateById(role);
        return roleMapper.selectById(id);
    }

    /**
     * 删除角色
     */
    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }

    /**
     * 分页查询角色
     */
    public IPage<SysRole> pageRoles(int page, int size, Long orgId) {
        Page<SysRole> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(SysRole::getOrgId, orgId);
        }
        wrapper.orderByDesc(SysRole::getCreatedAt);
        return roleMapper.selectPage(pageObj, wrapper);
    }

    // ============================================================
    // Role-Permission
    // ============================================================

    /**
     * 为角色分配权限 — 先删后批量插
     */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permIds) {
        LambdaQueryWrapper<SysRolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRolePermission::getRoleId, roleId);
        rolePermMapper.delete(wrapper);

        if (permIds != null && !permIds.isEmpty()) {
            List<SysRolePermission> newRows = permIds.stream()
                    .map(pid -> new SysRolePermission()
                            .setRoleId(roleId)
                            .setPermissionId(pid)
                            .setCreatedAt(LocalDateTime.now()))
                    .collect(Collectors.toList());
            Db.saveBatch(newRows);
        }
    }

    /**
     * 获取所有权限, 按排序号升序
     */
    public List<SysPermission> getAllPermissions() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysPermission::getSortOrder);
        return permMapper.selectList(wrapper);
    }

    /**
     * 获取用户权限 — 用户→角色→权限三级联查, 去重
     */
    public List<SysPermission> getUserPermissions(Long userId) {
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, userId);
        List<Long> roleIds = userRoleMapper.selectList(urWrapper).stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SysRolePermission> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.in(SysRolePermission::getRoleId, roleIds);
        List<Long> permIds = rolePermMapper.selectList(rpWrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        if (permIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysPermission> permissions = permMapper.selectBatchIds(permIds);
        permissions.sort(Comparator.comparing(
                p -> p.getSortOrder() != null ? p.getSortOrder() : 0));
        return permissions;
    }

    // ============================================================
    // Audit Log
    // ============================================================

    /**
     * 记录审计日志
     */
    public void recordAuditLog(Long orgId, Long userId, String username,
                               String operation, String module,
                               String targetType, String targetId,
                               String requestUrl, String requestMethod,
                               int responseStatus, String clientIp,
                               long durationMs, String detail) {
        SysAuditLog log = new SysAuditLog()
                .setOrgId(orgId)
                .setUserId(userId)
                .setUsername(username)
                .setOperation(operation)
                .setModule(module)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setRequestUrl(requestUrl)
                .setRequestMethod(requestMethod)
                .setResponseStatus(responseStatus)
                .setClientIp(clientIp)
                .setDurationMs(durationMs)
                .setDetail(detail)
                .setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /**
     * 分页查询审计日志 — 支持多条件筛选
     */
    public IPage<SysAuditLog> pageAuditLogs(int page, int size, Long orgId,
                                             Long userId, String module,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime) {
        Page<SysAuditLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (orgId != null) {
            wrapper.eq(SysAuditLog::getOrgId, orgId);
        }
        if (userId != null) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (module != null && !module.isBlank()) {
            wrapper.eq(SysAuditLog::getModule, module);
        }
        if (startTime != null) {
            wrapper.ge(SysAuditLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysAuditLog::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(SysAuditLog::getCreatedAt);
        return auditLogMapper.selectPage(pageObj, wrapper);
    }

    // ============================================================
    // Org
    // ============================================================

    /**
     * 创建组织
     */
    public OrgOrganization createOrg(OrgOrganization org) {
        orgMapper.insert(org);
        return org;
    }

    /**
     * 更新组织
     */
    public OrgOrganization updateOrg(Long id, OrgOrganization org) {
        org.setId(id);
        orgMapper.updateById(org);
        return orgMapper.selectById(id);
    }

    /**
     * 删除组织
     */
    public void deleteOrg(Long id) {
        orgMapper.deleteById(id);
    }

    /**
     * 分页查询组织
     */
    public IPage<OrgOrganization> pageOrgs(int page, int size) {
        Page<OrgOrganization> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<OrgOrganization> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(OrgOrganization::getSortOrder)
                .orderByDesc(OrgOrganization::getCreatedAt);
        return orgMapper.selectPage(pageObj, wrapper);
    }

    // ============================================================
    // Ledger
    // ============================================================

    /**
     * 创建账套
     */
    public OrgLedger createLedger(OrgLedger ledger) {
        ledgerMapper.insert(ledger);
        return ledger;
    }

    // ============================================================
    // Account
    // ============================================================

    /**
     * 分页查询账户
     */
    public IPage<OrgAccount> pageAccounts(int page, int size, Long ledgerId, Long orgId) {
        Page<OrgAccount> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<OrgAccount> wrapper = new LambdaQueryWrapper<>();
        if (ledgerId != null) {
            wrapper.eq(OrgAccount::getLedgerId, ledgerId);
        }
        if (orgId != null) {
            wrapper.eq(OrgAccount::getOrgId, orgId);
        }
        wrapper.orderByDesc(OrgAccount::getCreatedAt);
        return accountMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 创建账户
     */
    public OrgAccount createAccount(OrgAccount account) {
        accountMapper.insert(account);
        return account;
    }
}
