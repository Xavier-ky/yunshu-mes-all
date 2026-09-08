-- 修复 pro_andon_* 中文乱码（问号），从 sys_user / workstation / sys_role 回填
SET NAMES utf8mb4;
USE fan_mes;

-- 删除早期错误演示记录（test 工位 / 乱码原因）
DELETE FROM pro_andon_record
WHERE workstation_name IN ('test', 'Test')
   OR andon_reason LIKE '%?%'
   OR nick_name IN ('worker', 'test')
   OR (record_id = 1 AND workstation_code IS NULL);

-- 发起人 / 处置人姓名从用户表同步
UPDATE pro_andon_record r
INNER JOIN sys_user u ON u.user_id = r.user_id
SET r.user_name = u.username,
    r.nick_name = u.real_name
WHERE r.user_id IS NOT NULL;

UPDATE pro_andon_record r
INNER JOIN sys_user h ON h.user_id = r.handler_user_id
SET r.handler_user_name = h.username,
    r.handler_nick_name = h.real_name
WHERE r.handler_user_id IS NOT NULL;

-- 工位名称从 workstation 同步
UPDATE pro_andon_record r
INNER JOIN workstation ws ON ws.station_id = r.workstation_id
SET r.workstation_code = ws.station_code,
    r.workstation_name = ws.station_name
WHERE r.workstation_id IS NOT NULL;

-- 工单名称从 work_order + product 同步
UPDATE pro_andon_record r
INNER JOIN work_order wo ON wo.work_order_id = r.workorder_id
LEFT JOIN product p ON p.product_id = wo.product_id
SET r.workorder_code = wo.work_order_no,
    r.workorder_name = COALESCE(p.product_name, r.workorder_name)
WHERE r.workorder_id IS NOT NULL;

-- 工序名称从 process_step 同步
UPDATE pro_andon_record r
INNER JOIN process_step ps ON ps.step_id = r.process_id
SET r.process_code = ps.step_code,
    r.process_name = ps.step_name
WHERE r.process_id IS NOT NULL;

-- 呼叫配置：角色名 / 处置人
UPDATE pro_andon_config c
INNER JOIN sys_role ro ON ro.role_id = c.handler_role_id
SET c.handler_role_name = ro.role_name
WHERE c.handler_role_id IS NOT NULL;

UPDATE pro_andon_config c
INNER JOIN sys_user u ON u.user_id = c.handler_user_id
SET c.handler_user_name = u.username,
    c.handler_nick_name = u.real_name
WHERE c.handler_user_id IS NOT NULL;

-- 确保六类原因文案正确（覆盖历史乱码）
UPDATE pro_andon_config SET andon_reason = 'M4螺丝库存不足', andon_level = 'LEVEL2' WHERE config_id = 1;
UPDATE pro_andon_config SET andon_reason = '设备噪音异常', andon_level = 'LEVEL1' WHERE config_id = 2;
UPDATE pro_andon_config SET andon_reason = '首件检验不合格', andon_level = 'LEVEL2' WHERE config_id = 3;
UPDATE pro_andon_config SET andon_reason = '工序节拍异常', andon_level = 'LEVEL3' WHERE config_id = 4;
UPDATE pro_andon_config SET andon_reason = '包装箱缺货', andon_level = 'LEVEL2', handler_role_id = 3, handler_role_name = '仓库物料员' WHERE andon_reason = '包装箱缺货' OR config_id = 5;
UPDATE pro_andon_config SET andon_reason = '老化架温度偏高', andon_level = 'LEVEL1', handler_role_id = 6, handler_role_name = '设备维修员' WHERE andon_reason = '老化架温度偏高' OR config_id = 7;

-- 记录原因字段与备注（按 record_id 精确修正）
UPDATE pro_andon_record SET andon_reason = 'M4螺丝库存不足', remark = '台扇二线底座工位缺料，已停线等待' WHERE record_id = 5;
UPDATE pro_andon_record SET andon_reason = '设备噪音异常', remark = '壁扇线电机工位异响，需机修现场确认' WHERE record_id = 6;
UPDATE pro_andon_record SET andon_reason = '首件检验不合格', remark = '老化测试首件尺寸超差，待复检' WHERE record_id = 7;
UPDATE pro_andon_record SET andon_reason = '工序节拍异常', remark = '包装工位节拍低于标准，需班长协调人手' WHERE record_id = 8;
UPDATE pro_andon_record SET andon_reason = '包装箱缺货', remark = '已补货恢复包装' WHERE record_id IN (9, 16);
UPDATE pro_andon_record SET andon_reason = '老化架温度偏高', remark = '调整温控参数后恢复正常' WHERE record_id IN (10, 17);
UPDATE pro_andon_record SET andon_reason = 'M4螺丝库存不足', remark = '早班缺料已配送' WHERE record_id IN (11, 18);
UPDATE pro_andon_record SET andon_reason = '包装箱缺货', remark = '刚完成补货闭环' WHERE record_id = 19;

UPDATE pro_andon_record SET andon_reason = '设备噪音异常', remark = COALESCE(NULLIF(remark, ''), '历史处置记录') WHERE record_id = 2;
UPDATE pro_andon_record SET andon_reason = '首件检验不合格', remark = COALESCE(NULLIF(remark, ''), '首件复检后关闭') WHERE record_id = 3;
UPDATE pro_andon_record SET andon_reason = '工序节拍异常', remark = COALESCE(NULLIF(remark, ''), '节拍恢复后关闭') WHERE record_id = 4;

-- 删除重复的本班 HANDLED（保留 record_id 较大、字段已修复的一条）
DELETE r1 FROM pro_andon_record r1
INNER JOIN pro_andon_record r2
  ON r1.workstation_code = r2.workstation_code
  AND r1.andon_reason = r2.andon_reason
  AND r1.status = 'HANDLED'
  AND r2.status = 'HANDLED'
  AND r1.remark = r2.remark
  AND r1.record_id < r2.record_id
  AND r1.handle_time >= CURDATE()
  AND r2.handle_time >= CURDATE();

-- 兜底：仍含问号的文本行直接删（不应再出现）
DELETE FROM pro_andon_record
WHERE andon_reason LIKE '%?%'
   OR nick_name LIKE '%?%'
   OR handler_nick_name LIKE '%?%'
   OR workstation_name LIKE '%?%'
   OR remark LIKE '%?%';

DELETE FROM pro_andon_config
WHERE andon_reason LIKE '%?%'
   OR handler_role_name LIKE '%?%'
   OR handler_nick_name LIKE '%?%';

-- 删除与 16+ 重复的早期 HANDLED 脏数据
DELETE FROM pro_andon_record WHERE record_id IN (9, 10, 11);
