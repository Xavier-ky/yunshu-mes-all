-- Repeatable seed: 云枢系统管理菜单/字典/公告/操作日志
SET NAMES utf8mb4;

USE fan_mes;

-- ---------------------------------------------------------------------------
-- System management menus (directory + pages + buttons + autocode)
-- ---------------------------------------------------------------------------
DELETE FROM sys_role_menu
WHERE menu_id IN (1,100,101,102,103,104,105,106,107,108,500,501,2003,2004,2005,2006,2007,
  1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1012,1013,1014,1015,1016,1017,1018,1019,1020,
  1021,1022,1023,1024,1025,1026,1027,1028,1029,1030,1031,1032,1033,1034,1035,1036,1037,1038,1039,1040,
  1041,1042,1043,1044,1045);

DELETE FROM sys_menu
WHERE menu_id IN (1,100,101,102,103,104,105,106,107,108,500,501,2003,2004,2005,2006,2007,
  1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,1012,1013,1014,1015,1016,1017,1018,1019,1020,
  1021,1022,1023,1024,1025,1026,1027,1028,1029,1030,1031,1032,1033,1034,1035,1036,1037,1038,1039,1040,
  1041,1042,1043,1044,1045);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
(1, '系统管理', 0, 1, 'system', NULL, '', 1, 0, 'M', '0', '0', '', 'system', 'admin', NOW(), '系统管理目录'),
(100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', NOW(), '用户管理菜单'),
(101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', NOW(), '角色管理菜单'),
(102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', NOW(), '菜单管理菜单'),
(103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', NOW(), '部门管理菜单'),
(104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', NOW(), '岗位管理菜单'),
(105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', NOW(), '字典管理菜单'),
(106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', NOW(), '参数设置菜单'),
(107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', NOW(), '通知公告菜单'),
(108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', NOW(), '日志管理菜单'),
(500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', NOW(), '操作日志菜单'),
(501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', NOW(), '登录日志菜单'),
(2003, '编码规则', 1, 10, 'autocodeRule', 'system/autocode/index', NULL, 1, 0, 'C', '0', '0', 'system:autocode:rule', 'code', 'admin', NOW(), '编码规则菜单'),
(1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', NOW(), ''),
(1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', NOW(), ''),
(1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', NOW(), ''),
(1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', NOW(), ''),
(1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', NOW(), ''),
(1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', NOW(), ''),
(1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', NOW(), ''),
(1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', NOW(), ''),
(1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', NOW(), ''),
(1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', NOW(), ''),
(1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', NOW(), ''),
(1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', NOW(), ''),
(1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', NOW(), ''),
(1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', NOW(), ''),
(1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', NOW(), ''),
(1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', NOW(), ''),
(1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', NOW(), ''),
(1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', NOW(), ''),
(1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', NOW(), ''),
(1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', NOW(), ''),
(1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', NOW(), ''),
(1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', NOW(), ''),
(1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', NOW(), ''),
(1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', NOW(), ''),
(1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', NOW(), ''),
(1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', NOW(), ''),
(1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', NOW(), ''),
(1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', NOW(), ''),
(1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', NOW(), ''),
(1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', NOW(), ''),
(1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', NOW(), ''),
(1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', NOW(), ''),
(1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', NOW(), ''),
(1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', NOW(), ''),
(1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', NOW(), ''),
(1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', NOW(), ''),
(1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', NOW(), ''),
(1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', NOW(), ''),
(1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', NOW(), ''),
(1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', NOW(), ''),
(1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', NOW(), ''),
(1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', NOW(), ''),
(1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', NOW(), ''),
(1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', NOW(), ''),
(1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', NOW(), ''),
(2004, '编码规则查询', 2003, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:list', '#', 'admin', NOW(), ''),
(2005, '编码规则新增', 2003, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:add', '#', 'admin', NOW(), ''),
(2006, '编码规则更新', 2003, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:edit', '#', 'admin', NOW(), ''),
(2007, '编码规则删除', 2003, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:autocode:rule:remove', '#', 'admin', NOW(), '');

-- ---------------------------------------------------------------------------
-- sys_post (4 default posts)
-- ---------------------------------------------------------------------------
DELETE FROM sys_user_post WHERE post_id IN (1, 2, 3, 4);
DELETE FROM sys_post WHERE post_id IN (1, 2, 3, 4);

INSERT INTO sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time, remark) VALUES
(1, 'ceo', '董事长', 1, '0', 'admin', NOW(), ''),
(2, 'se', '项目经理', 2, '0', 'admin', NOW(), ''),
(3, 'hr', '人力资源', 3, '0', 'admin', NOW(), ''),
(4, 'user', '普通员工', 4, '0', 'admin', NOW(), '');

INSERT IGNORE INTO sys_user_post (user_id, post_id)
SELECT u.user_id, 1 FROM sys_user u WHERE u.username = 'admin';

-- ---------------------------------------------------------------------------
-- sys_config
-- ---------------------------------------------------------------------------
DELETE FROM sys_config WHERE config_id IN (1, 2, 3, 4, 5);

INSERT INTO sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, remark) VALUES
(1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', NOW(), '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),
(2, '用户管理-账号初始密码', 'sys.user.initPassword', 'admin123', 'Y', 'admin', NOW(), '初始化密码 admin123'),
(3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', NOW(), '深色主题theme-dark，浅色主题theme-light'),
(4, '账号自助-验证码开关', 'sys.account.captchaOnOff', 'true', 'Y', 'admin', NOW(), '是否开启验证码功能'),
(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', NOW(), '是否开启注册用户功能');

-- ---------------------------------------------------------------------------
-- sys_dict_type + sys_dict_data (>=15 types for DictController)
-- ---------------------------------------------------------------------------
DELETE FROM sys_dict_data WHERE dict_type IN (
  'sys_user_sex', 'sys_normal_disable', 'sys_yes_no', 'sys_notice_type', 'sys_notice_status',
  'sys_oper_type', 'sys_common_status',
  'mes_order_status', 'mes_workorder_type', 'mes_workorder_sourcetype', 'mes_task_status',
  'mes_item_product', 'mes_issue_status', 'mes_qc_type', 'mes_qc_result', 'mes_machinery_status',
  'mes_feedback_type', 'mes_andon_status', 'mes_barcode_type', 'mes_client_type'
);
DELETE FROM sys_dict_type WHERE dict_type IN (
  'sys_user_sex', 'sys_normal_disable', 'sys_yes_no', 'sys_notice_type', 'sys_notice_status',
  'sys_oper_type', 'sys_common_status',
  'mes_order_status', 'mes_workorder_type', 'mes_workorder_sourcetype', 'mes_task_status',
  'mes_item_product', 'mes_issue_status', 'mes_qc_type', 'mes_qc_result', 'mes_machinery_status',
  'mes_feedback_type', 'mes_andon_status', 'mes_barcode_type', 'mes_client_type'
);

INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark) VALUES
(1, '用户性别', 'sys_user_sex', '0', 'admin', NOW(), '用户性别列表'),
(2, '系统开关', 'sys_normal_disable', '0', 'admin', NOW(), '系统开关列表'),
(3, '系统是否', 'sys_yes_no', '0', 'admin', NOW(), '系统是否列表'),
(4, '通知类型', 'sys_notice_type', '0', 'admin', NOW(), '通知类型列表'),
(5, '通知状态', 'sys_notice_status', '0', 'admin', NOW(), '通知状态列表'),
(6, '操作类型', 'sys_oper_type', '0', 'admin', NOW(), '操作类型列表'),
(7, '系统状态', 'sys_common_status', '0', 'admin', NOW(), '登录状态列表'),
(100, '单据状态', 'mes_order_status', '0', 'admin', NOW(), 'MES单据状态'),
(101, '工单类型', 'mes_workorder_type', '0', 'admin', NOW(), '生产工单类型'),
(102, '工单来源', 'mes_workorder_sourcetype', '0', 'admin', NOW(), '生产工单来源'),
(103, '任务状态', 'mes_task_status', '0', 'admin', NOW(), '生产任务状态'),
(104, '物料产品', 'mes_item_product', '0', 'admin', NOW(), '物料或产品'),
(105, '领料单状态', 'mes_issue_status', '0', 'admin', NOW(), '生产领料单状态'),
(106, '检验类型', 'mes_qc_type', '0', 'admin', NOW(), '质量检验类型'),
(107, '检验结果', 'mes_qc_result', '0', 'admin', NOW(), '质量检验结果'),
(108, '设备状态', 'mes_machinery_status', '0', 'admin', NOW(), '设备运行状态'),
(109, '报工类型', 'mes_feedback_type', '0', 'admin', NOW(), '生产报工类型'),
(110, '安灯状态', 'mes_andon_status', '0', 'admin', NOW(), '安灯事件状态'),
(111, '条码类型', 'mes_barcode_type', '0', 'admin', NOW(), '条码类型'),
(112, '客户类型', 'mes_client_type', '0', 'admin', NOW(), '客户类型');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_by, create_time) VALUES
(1, 1, '男', '0', 'sys_user_sex', 'default', 'Y', '0', 'admin', NOW()),
(2, 2, '女', '1', 'sys_user_sex', 'default', 'N', '0', 'admin', NOW()),
(3, 3, '未知', '2', 'sys_user_sex', 'default', 'N', '0', 'admin', NOW()),
(4, 1, '正常', '0', 'sys_normal_disable', 'primary', 'Y', '0', 'admin', NOW()),
(5, 2, '停用', '1', 'sys_normal_disable', 'danger', 'N', '0', 'admin', NOW()),
(6, 1, '是', 'Y', 'sys_yes_no', 'primary', 'Y', '0', 'admin', NOW()),
(7, 2, '否', 'N', 'sys_yes_no', 'danger', 'N', '0', 'admin', NOW()),
(8, 1, '通知', '1', 'sys_notice_type', 'warning', 'Y', '0', 'admin', NOW()),
(9, 2, '公告', '2', 'sys_notice_type', 'success', 'N', '0', 'admin', NOW()),
(10, 1, '正常', '0', 'sys_notice_status', 'primary', 'Y', '0', 'admin', NOW()),
(11, 2, '关闭', '1', 'sys_notice_status', 'danger', 'N', '0', 'admin', NOW()),
(12, 1, '新增', '1', 'sys_oper_type', 'info', 'N', '0', 'admin', NOW()),
(13, 2, '修改', '2', 'sys_oper_type', 'info', 'N', '0', 'admin', NOW()),
(14, 3, '删除', '3', 'sys_oper_type', 'danger', 'N', '0', 'admin', NOW()),
(15, 4, '导出', '5', 'sys_oper_type', 'warning', 'N', '0', 'admin', NOW()),
(16, 1, '成功', '0', 'sys_common_status', 'primary', 'Y', '0', 'admin', NOW()),
(17, 2, '失败', '1', 'sys_common_status', 'danger', 'N', '0', 'admin', NOW()),
(100, 1, '草稿', 'PREPARE', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(101, 2, '已确认', 'CONFIRMED', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(102, 3, '审批中', 'APPROVING', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(103, 4, '已审批', 'APPROVED', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(104, 5, '已完成', 'FINISHED', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(105, 6, '已取消', 'CANCELED', 'mes_order_status', 'default', 'N', '0', 'admin', NOW()),
(106, 1, '自制', 'SELF', 'mes_workorder_type', 'default', 'Y', '0', 'admin', NOW()),
(107, 2, '外协', 'OUTSOURCE', 'mes_workorder_type', 'default', 'N', '0', 'admin', NOW()),
(108, 3, '采购', 'PURCHASE', 'mes_workorder_type', 'default', 'N', '0', 'admin', NOW()),
(109, 1, '客户订单', 'ORDER', 'mes_workorder_sourcetype', 'default', 'Y', '0', 'admin', NOW()),
(110, 2, '库存备货', 'STORE', 'mes_workorder_sourcetype', 'default', 'N', '0', 'admin', NOW()),
(111, 1, '未开始', 'PREPARE', 'mes_task_status', 'default', 'N', '0', 'admin', NOW()),
(112, 2, '进行中', 'NORMAL', 'mes_task_status', 'default', 'N', '0', 'admin', NOW()),
(113, 3, '已完成', 'FINISHED', 'mes_task_status', 'default', 'N', '0', 'admin', NOW()),
(114, 4, '已取消', 'CANCELED', 'mes_task_status', 'default', 'N', '0', 'admin', NOW()),
(115, 1, '物料', 'ITEM', 'mes_item_product', 'default', 'Y', '0', 'admin', NOW()),
(116, 2, '产品', 'PRODUCT', 'mes_item_product', 'default', 'N', '0', 'admin', NOW()),
(117, 1, '草稿', 'PREPARE', 'mes_issue_status', 'default', 'N', '0', 'admin', NOW()),
(118, 2, '待拣货', 'APPROVING', 'mes_issue_status', 'default', 'N', '0', 'admin', NOW()),
(119, 3, '待执行', 'APPROVED', 'mes_issue_status', 'default', 'N', '0', 'admin', NOW()),
(120, 4, '已完成', 'FINISHED', 'mes_issue_status', 'default', 'N', '0', 'admin', NOW()),
(121, 1, '来料检验', 'IQC', 'mes_qc_type', 'default', 'N', '0', 'admin', NOW()),
(122, 2, '过程检验', 'PQC', 'mes_qc_type', 'default', 'N', '0', 'admin', NOW()),
(123, 3, '出货检验', 'OQC', 'mes_qc_type', 'default', 'N', '0', 'admin', NOW()),
(124, 4, '退料检验', 'RQC', 'mes_qc_type', 'default', 'N', '0', 'admin', NOW()),
(125, 1, '合格', 'ACCEPT', 'mes_qc_result', 'default', 'Y', '0', 'admin', NOW()),
(126, 2, '不合格', 'REJECT', 'mes_qc_result', 'default', 'N', '0', 'admin', NOW()),
(127, 1, '正常', 'RUNNING', 'mes_machinery_status', 'default', 'N', '0', 'admin', NOW()),
(128, 2, '运行', 'WORKING', 'mes_machinery_status', 'default', 'N', '0', 'admin', NOW()),
(129, 3, '停机', 'STOP', 'mes_machinery_status', 'default', 'N', '0', 'admin', NOW()),
(130, 4, '维修', 'REPAIR', 'mes_machinery_status', 'default', 'N', '0', 'admin', NOW()),
(131, 1, '自行报工', 'SELF', 'mes_feedback_type', 'default', 'Y', '0', 'admin', NOW()),
(132, 2, '统一报工', 'UNIFY', 'mes_feedback_type', 'default', 'N', '0', 'admin', NOW()),
(133, 1, '待处置', 'ACTIVE', 'mes_andon_status', 'default', 'N', '0', 'admin', NOW()),
(134, 2, '已处置', 'HANDLED', 'mes_andon_status', 'default', 'N', '0', 'admin', NOW()),
(135, 1, '物料产品条码', 'ITEM', 'mes_barcode_type', 'default', 'Y', '0', 'admin', NOW()),
(136, 2, '库存条码', 'STOCK', 'mes_barcode_type', 'default', 'N', '0', 'admin', NOW()),
(137, 3, '生产工单条码', 'WORKORDER', 'mes_barcode_type', 'default', 'N', '0', 'admin', NOW()),
(138, 4, '设备条码', 'MACHINERY', 'mes_barcode_type', 'default', 'N', '0', 'admin', NOW()),
(139, 1, '企业', 'ENTERPRISE', 'mes_client_type', 'default', 'Y', '0', 'admin', NOW()),
(140, 2, '个人', 'PERSON', 'mes_client_type', 'default', 'N', '0', 'admin', NOW());

-- ---------------------------------------------------------------------------
-- sys_auto_code_rule + parts
-- ---------------------------------------------------------------------------
DELETE FROM sys_auto_code_part WHERE rule_id IN (1, 2, 3);
DELETE FROM sys_auto_code_result WHERE rule_id IN (1, 2, 3);
DELETE FROM sys_auto_code_rule WHERE rule_id IN (1, 2, 3);

INSERT INTO sys_auto_code_rule (rule_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'DEPT_CODE', '部门编码规则', '部门编码自动生成', 6, 'N', NULL, 'L', 'Y', 'admin', NOW()),
(2, 'PRINTER_CLIENT_CODE', '打印客户端编码规则', '打印客户端编码', 8, 'N', NULL, 'L', 'Y', 'admin', NOW()),
(3, 'REPORT_CHART_CODE', '报表图表编码规则', '报表图表编码', 14, 'N', NULL, 'L', 'Y', 'admin', NOW());

INSERT INTO sys_auto_code_part (part_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, seria_start_no, seria_step, cycle_flag, create_by, create_time) VALUES
(1, 1, 1, 'FIXCHAR', 'PREFIX', '前缀', 1, 'D', NULL, NULL, NULL, 'admin', NOW()),
(2, 1, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, 1, 1, 'N', 'admin', NOW()),
(3, 2, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, 'PC', NULL, NULL, NULL, 'admin', NOW()),
(4, 2, 2, 'SERIALNO', 'SERIAL', '流水号', 4, NULL, 1, 1, 'N', 'admin', NOW()),
(5, 3, 1, 'FIXCHAR', 'PREFIX', '前缀', 2, 'RC', NULL, NULL, NULL, 'admin', NOW()),
(6, 3, 2, 'NOWDATE', 'DATE', '日期', 8, NULL, NULL, NULL, NULL, 'admin', NOW()),
(7, 3, 3, 'SERIALNO', 'SERIAL', '流水号', 3, NULL, 1, 1, 'Y', 'admin', NOW());

UPDATE sys_auto_code_part SET date_format = 'yyyyMMdd' WHERE part_id = 6;

-- ---------------------------------------------------------------------------
-- sys_role_menu: MANAGER + TESTER get all seeded system menus
-- ---------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN (
  SELECT 1 AS menu_id UNION ALL SELECT 100 UNION ALL SELECT 101 UNION ALL SELECT 102 UNION ALL SELECT 103 UNION ALL SELECT 104 UNION ALL SELECT 105 UNION ALL SELECT 106 UNION ALL SELECT 107 UNION ALL SELECT 108 UNION ALL SELECT 500 UNION ALL SELECT 501 UNION ALL SELECT 2003 UNION ALL SELECT 2004 UNION ALL SELECT 2005 UNION ALL SELECT 2006 UNION ALL SELECT 2007 UNION ALL
  SELECT 1001 UNION ALL SELECT 1002 UNION ALL SELECT 1003 UNION ALL SELECT 1004 UNION ALL SELECT 1005 UNION ALL SELECT 1006 UNION ALL SELECT 1007 UNION ALL SELECT 1008 UNION ALL SELECT 1009 UNION ALL SELECT 1010 UNION ALL SELECT 1011 UNION ALL SELECT 1012 UNION ALL SELECT 1013 UNION ALL SELECT 1014 UNION ALL SELECT 1015 UNION ALL SELECT 1016 UNION ALL SELECT 1017 UNION ALL SELECT 1018 UNION ALL SELECT 1019 UNION ALL SELECT 1020 UNION ALL
  SELECT 1021 UNION ALL SELECT 1022 UNION ALL SELECT 1023 UNION ALL SELECT 1024 UNION ALL SELECT 1025 UNION ALL SELECT 1026 UNION ALL SELECT 1027 UNION ALL SELECT 1028 UNION ALL SELECT 1029 UNION ALL SELECT 1030 UNION ALL SELECT 1031 UNION ALL SELECT 1032 UNION ALL SELECT 1033 UNION ALL SELECT 1034 UNION ALL SELECT 1035 UNION ALL SELECT 1036 UNION ALL SELECT 1037 UNION ALL SELECT 1038 UNION ALL SELECT 1039 UNION ALL SELECT 1040 UNION ALL
  SELECT 1041 UNION ALL SELECT 1042 UNION ALL SELECT 1043 UNION ALL SELECT 1044 UNION ALL SELECT 1045
) m
WHERE r.role_code IN ('MANAGER', 'TESTER');

-- TESTER: also grant every other menu already in sys_menu (full access)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'TESTER';

-- ---------------------------------------------------------------------------
-- sys_notice (sample)
-- ---------------------------------------------------------------------------
DELETE FROM sys_notice WHERE notice_id IN (1, 2);

INSERT INTO sys_notice (notice_id, notice_title, notice_type, notice_content, status, create_by, create_time, remark) VALUES
(1, '云枢智造 MES 系统上线通知', '2', '云枢智造 MES 系统已完成部署，请各部门按权限登录使用。', '0', 'admin', NOW(), '管理员'),
(2, '系统维护公告', '1', '本周六 02:00-04:00 将进行数据库维护，期间系统可能短暂不可用。', '0', 'admin', NOW(), '管理员');

-- ---------------------------------------------------------------------------
-- sys_oper_log (sample)
-- ---------------------------------------------------------------------------
DELETE FROM sys_oper_log WHERE oper_id IN (1, 2, 3);

INSERT INTO sys_oper_log (oper_id, title, business_type, method, request_method, operator_type, oper_name, dept_name, oper_url, oper_ip, oper_location, oper_param, json_result, status, oper_time) VALUES
(1, '用户管理', 2, 'com.yunshu.mes.system.UserController.edit()', 'PUT', 1, 'admin', '管理部', '/api/system/user', '127.0.0.1', '内网IP', '{"userId":1}', '{"code":200,"msg":"操作成功"}', 0, NOW()),
(2, '角色管理', 1, 'com.yunshu.mes.system.RoleController.add()', 'POST', 1, 'admin', '管理部', '/api/system/role', '127.0.0.1', '内网IP', '{"roleName":"测试角色"}', '{"code":200,"msg":"操作成功"}', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(3, '字典管理', 0, 'com.yunshu.mes.system.DictController.list()', 'GET', 1, 'tester', '管理部', '/api/system/dict/data/type/mes_order_status', '192.168.1.10', '内网IP', '{}', '{"code":200,"rows":[]}', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR));
