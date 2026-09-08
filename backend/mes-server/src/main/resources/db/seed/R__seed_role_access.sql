-- 角色权限打通：确保 role_key 与 role_code 一致，演示用户单角色绑定
UPDATE sys_role SET role_key = role_code WHERE role_key IS NULL OR role_key = '';

-- 清理演示账号的多角色绑定（保留最早一条）
DELETE ur FROM sys_user_role ur
INNER JOIN (
    SELECT user_id, MIN(role_id) AS keep_role_id
    FROM sys_user_role
    GROUP BY user_id
    HAVING COUNT(*) > 1
) dup ON ur.user_id = dup.user_id AND ur.role_id <> dup.keep_role_id;

-- 业务角色：分配基础 system 菜单（个人相关只读），MANAGER/TESTER 已在 R__seed_sys_menu_dict.sql
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN (
    SELECT 100 AS menu_id UNION ALL SELECT 1001
) m
WHERE r.role_code IN (
    'PROD_SUPERVISOR',
    'WAREHOUSE_CLERK',
    'QUALITY_INSPECTOR',
    'EQUIPMENT_MAINTAINER',
    'LINE_OPERATOR'
);

-- 生产主管额外开放常用 MES 模块按钮权限占位（与前端 permissionPrefixes 配合）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'PROD_SUPERVISOR'
  AND m.perms LIKE 'system:%'
  AND m.menu_type = 'F';
