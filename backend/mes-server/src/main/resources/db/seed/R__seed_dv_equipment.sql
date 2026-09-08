-- Seed dv_* equipment demo data for yunshu-ui device center
-- File encoding: UTF-8 (no BOM)
-- Windows manual import (do NOT use PowerShell pipe — it corrupts Chinese):
--   scripts\seed-dv-equipment.cmd
-- Or: cmd /c "chcp 65001>nul && mysql ... --default-character-set=utf8mb4 fan_mes < R__seed_dv_equipment.sql"

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE fan_mes;

-- Clear demo seed rows (fixed IDs) so re-run fixes garbled text
DELETE FROM dv_repair_line WHERE line_id IN (1101, 1102);
DELETE FROM dv_repair WHERE repair_id IN (1001, 1002);
DELETE FROM dv_mainten_record_line WHERE line_id IN (901, 902);
DELETE FROM dv_mainten_record WHERE record_id IN (801);
DELETE FROM dv_check_record_line WHERE line_id IN (701, 702, 703);
DELETE FROM dv_check_record WHERE record_id IN (601);
DELETE FROM dv_check_subject WHERE record_id BETWEEN 501 AND 508;
DELETE FROM dv_check_machinery WHERE record_id BETWEEN 401 AND 405;
DELETE FROM dv_check_plan WHERE plan_id IN (301, 302);
DELETE FROM dv_subject WHERE subject_id BETWEEN 201 AND 208;
DELETE FROM dv_machinery WHERE machinery_id BETWEEN 101 AND 108;
DELETE FROM dv_machinery_type WHERE machinery_type_id BETWEEN 1 AND 5;

-- machinery types (tree: root + children)
INSERT INTO dv_machinery_type (machinery_type_id, machinery_type_code, machinery_type_name, parent_type_id, ancestors, enable_flag, create_time)
VALUES
  (1, 'MT-ROOT', '生产设备', 0, '0', 'Y', NOW(3)),
  (2, 'MT-ASM', '组装设备', 1, '0,1', 'Y', NOW(3)),
  (3, 'MT-TEST', '测试设备', 1, '0,1', 'Y', NOW(3)),
  (4, 'MT-PKG', '包装设备', 1, '0,1', 'Y', NOW(3)),
  (5, 'MT-AUX', '辅助设备', 0, '0', 'Y', NOW(3));

-- machinery linked to workshop WS-FAN-01
INSERT INTO dv_machinery (
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  machinery_type_id, machinery_type_code, machinery_type_name,
  workshop_id, workshop_code, workshop_name, status, create_time
)
SELECT 101, 'DV-M001', '电机装配机', '松下', 'ASM-200',
  2, 'MT-ASM', '组装设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 102, 'DV-M002', '扇叶压装机', '博世', 'PRS-150',
  2, 'MT-ASM', '组装设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 103, 'DV-M003', '老化测试台', 'ATE', 'AGE-48H',
  3, 'MT-TEST', '测试设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'STOP', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 104, 'DV-M004', '自动包装线', '永创', 'PKG-LINE-A',
  4, 'MT-PKG', '包装设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37',
  5, 'MT-AUX', '辅助设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 106, 'DV-M006', '螺丝供料机', '快克', 'SF-100',
  2, 'MT-ASM', '组装设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'REPAIR', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 107, 'DV-M007', '风速测试仪', '德图', 'TEST-450',
  3, 'MT-TEST', '测试设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 108, 'DV-M008', '传送带', '东元', 'CV-12M',
  5, 'MT-AUX', '辅助设备',
  w.workshop_id, w.workshop_code, w.workshop_name, 'STOP', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01';

-- subjects (8 items: check + mainten)
INSERT INTO dv_subject (subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, enable_flag, create_time)
VALUES
  (201, 'SUB-C01', '电机扭矩检查', 'CHECK', '检查电机装配扭矩是否符合标准', '15-18 Nm', 'Y', NOW(3)),
  (202, 'SUB-C02', '扇叶平衡检查', 'CHECK', '检查扇叶动平衡', 'G2.5级', 'Y', NOW(3)),
  (203, 'SUB-C03', '电气绝缘检查', 'CHECK', '检查绝缘电阻', '>=5 MOhm', 'Y', NOW(3)),
  (204, 'SUB-C04', '噪音检测', 'CHECK', '运行噪音检测', '<=45 dB', 'Y', NOW(3)),
  (205, 'SUB-M01', '润滑保养', 'MAINTEN', '导轨及传动部位润滑', '每月一次', 'Y', NOW(3)),
  (206, 'SUB-M02', '滤芯更换', 'MAINTEN', '更换空气滤芯', '每季度一次', 'Y', NOW(3)),
  (207, 'SUB-M03', '皮带张紧', 'MAINTEN', '检查并调整皮带张紧度', '松紧适中', 'Y', NOW(3)),
  (208, 'SUB-M04', '紧固件检查', 'MAINTEN', '检查关键紧固螺栓', '无松动', 'Y', NOW(3));

-- check plans (2: CHECK + MAINTEN)
INSERT INTO dv_check_plan (plan_id, plan_code, plan_name, plan_type, start_date, end_date, cycle_type, cycle_count, status, create_time)
VALUES
  (301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'DAY', 1, 'FINISHED', NOW(3)),
  (302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'MONTH', 1, 'FINISHED', NOW(3));

-- plan machinery
INSERT INTO dv_check_machinery (record_id, plan_id, machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, create_time)
VALUES
  (401, 301, 101, 'DV-M001', '电机装配机', '松下', 'ASM-200', NOW(3)),
  (402, 301, 102, 'DV-M002', '扇叶压装机', '博世', 'PRS-150', NOW(3)),
  (403, 301, 106, 'DV-M006', '螺丝供料机', '快克', 'SF-100', NOW(3)),
  (404, 302, 101, 'DV-M001', '电机装配机', '松下', 'ASM-200', NOW(3)),
  (405, 302, 105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37', NOW(3));

-- plan subjects
INSERT INTO dv_check_subject (record_id, plan_id, subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, create_time)
VALUES
  (501, 301, 201, 'SUB-C01', '电机扭矩检查', 'CHECK', '检查电机装配扭矩是否符合标准', '15-18 Nm', NOW(3)),
  (502, 301, 202, 'SUB-C02', '扇叶平衡检查', 'CHECK', '检查扇叶动平衡', 'G2.5级', NOW(3)),
  (503, 301, 203, 'SUB-C03', '电气绝缘检查', 'CHECK', '检查绝缘电阻', '>=5 MOhm', NOW(3)),
  (504, 301, 204, 'SUB-C04', '噪音检测', 'CHECK', '运行噪音检测', '<=45 dB', NOW(3)),
  (505, 302, 205, 'SUB-M01', '润滑保养', 'MAINTEN', '导轨及传动部位润滑', '每月一次', NOW(3)),
  (506, 302, 206, 'SUB-M02', '滤芯更换', 'MAINTEN', '更换空气滤芯', '每季度一次', NOW(3)),
  (507, 302, 207, 'SUB-M03', '皮带张紧', 'MAINTEN', '检查并调整皮带张紧度', '松紧适中', NOW(3)),
  (508, 302, 208, 'SUB-M04', '紧固件检查', 'MAINTEN', '检查关键紧固螺栓', '无松动', NOW(3));

-- sample check record + lines
INSERT INTO dv_check_record (
  record_id, plan_id, plan_code, plan_name, plan_type,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  check_time, user_name, nick_name, status, create_time
)
VALUES (
  601, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK',
  101, 'DV-M001', '电机装配机', '松下', 'ASM-200',
  NOW(3), 'repair', '设备维修员', 'FINISHED', NOW(3)
);

INSERT INTO dv_check_record_line (
  line_id, record_id, subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, check_status, create_time
)
VALUES
  (701, 601, 201, 'SUB-C01', '电机扭矩检查', 'CHECK', '检查电机装配扭矩是否符合标准', '15-18 Nm', 'Y', NOW(3)),
  (702, 601, 202, 'SUB-C02', '扇叶平衡检查', 'CHECK', '检查扇叶动平衡', 'G2.5级', 'Y', NOW(3)),
  (703, 601, 203, 'SUB-C03', '电气绝缘检查', 'CHECK', '检查绝缘电阻', '>=5 MOhm', 'Y', NOW(3));

-- sample mainten record + lines
INSERT INTO dv_mainten_record (
  record_id, plan_id, plan_code, plan_name, plan_type,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  mainten_time, user_name, nick_name, status, create_time
)
VALUES (
  801, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN',
  105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37',
  NOW(3), 'repair', '设备维修员', 'FINISHED', NOW(3)
);

INSERT INTO dv_mainten_record_line (
  line_id, record_id, subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, mainten_status, create_time
)
VALUES
  (901, 801, 205, 'SUB-M01', '润滑保养', 'MAINTEN', '导轨及传动部位润滑', '每月一次', 'Y', NOW(3)),
  (902, 801, 206, 'SUB-M02', '滤芯更换', 'MAINTEN', '更换空气滤芯', '每季度一次', 'Y', NOW(3));

-- repairs (2)
INSERT INTO dv_repair (
  repair_id, repair_code, repair_name,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id,
  require_date, status, create_time
)
VALUES
  (1001, 'REP-20260701', '螺丝供料机卡料维修',
   106, 'DV-M006', '螺丝供料机', '快克', 'SF-100', 2,
   '2026-07-01 09:30:00', 'PREPARE', NOW(3)),
  (1002, 'REP-20260705', '老化测试台温控故障',
   103, 'DV-M003', '老化测试台', 'ATE', 'AGE-48H', 3,
   '2026-07-05 14:00:00', 'FINISHED', NOW(3));

INSERT INTO dv_repair_line (line_id, repair_id, malfunction, repair_des, create_time)
VALUES
  (1101, 1001, '供料轨道卡料，传感器误报', NULL, NOW(3)),
  (1102, 1002, '温控模块失效，无法升温', '更换温控模块并校准', NOW(3));
