-- 大模型配置菜单权限（已有库可单独执行）
USE recon_platform;

INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, path) VALUES
(11, 7, '大模型配置', 'system:ai', 'MENU', '/system/ai');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, 11 FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 11
);
