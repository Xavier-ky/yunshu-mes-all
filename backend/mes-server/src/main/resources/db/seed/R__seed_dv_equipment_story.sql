-- 设备管理板块 — 故事化扩充演示数据（依赖 R__seed_dv_equipment.sql）
-- 可重复执行：固定 ID 段 DELETE + INSERT

SET NAMES utf8mb4;
USE fan_mes;

DELETE FROM dv_repair_line WHERE line_id BETWEEN 1103 AND 1114;
DELETE FROM dv_repair WHERE repair_id BETWEEN 1003 AND 1008;
DELETE FROM dv_mainten_record_line WHERE line_id BETWEEN 903 AND 920;
DELETE FROM dv_mainten_record WHERE record_id BETWEEN 802 AND 809;
DELETE FROM dv_check_record_line WHERE line_id BETWEEN 704 AND 750;
DELETE FROM dv_check_record WHERE record_id BETWEEN 602 AND 614;
DELETE FROM dv_check_subject WHERE record_id BETWEEN 509 AND 530;
DELETE FROM dv_check_machinery WHERE record_id BETWEEN 406 AND 425;
DELETE FROM dv_check_plan WHERE plan_id IN (303, 304, 305);
DELETE FROM dv_subject WHERE subject_id BETWEEN 209 AND 218;
DELETE FROM dv_machinery WHERE machinery_id BETWEEN 109 AND 118;

-- 新增 10 台设备
INSERT INTO dv_machinery (
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  machinery_type_id, machinery_type_code, machinery_type_name,
  workshop_id, workshop_code, workshop_name, status, create_time
)
SELECT 109, 'DV-M009', '扇叶动平衡机', ' Schenck', 'BAL-300',
  2, 'MT-ASM', '组装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 110, 'DV-M010', '网罩压装机', '永创', 'NET-PRS-80',
  2, 'MT-ASM', '组装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 111, 'DV-M011', '台扇机芯装配线', '松下', 'CORE-LINE-B',
  2, 'MT-ASM', '组装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 112, 'DV-M012', '落地扇立柱旋紧机', '博世', 'TIGHT-120',
  2, 'MT-ASM', '组装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'STOP', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 113, 'DV-M013', '电气安规测试仪', 'Chroma', 'SAF-19032',
  3, 'MT-TEST', '测试设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 114, 'DV-M014', '功率测试台', 'ATE', 'PWR-2KW',
  3, 'MT-TEST', '测试设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 115, 'DV-M015', '贴标机', '永创', 'LBL-200',
  4, 'MT-PKG', '包装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 116, 'DV-M016', '热缩膜机', '东元', 'SHRINK-450',
  4, 'MT-PKG', '包装设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 117, 'DV-M017', '冷却循环水机', '格力', 'CW-20HP',
  5, 'MT-AUX', '辅助设备', w.workshop_id, w.workshop_code, w.workshop_name, 'REPAIR', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01'
UNION ALL
SELECT 118, 'DV-M018', 'AGV小车#1', '海康', 'AGV-500',
  5, 'MT-AUX', '辅助设备', w.workshop_id, w.workshop_code, w.workshop_name, 'WORKING', NOW(3)
FROM workshop w WHERE w.workshop_code = 'WS-FAN-01';

-- 点检保养项目扩充
INSERT INTO dv_subject (subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, enable_flag, create_time)
VALUES
  (209, 'SUB-C05', '网罩外观检查', 'CHECK', '检查网罩无变形、毛刺', '表面平整无锐边', 'Y', NOW(3)),
  (210, 'SUB-C06', '安规接地检查', 'CHECK', '接地电阻测试', '<=0.1 Ohm', 'Y', NOW(3)),
  (211, 'SUB-C07', '功率偏差检查', 'CHECK', '额定功率偏差', '+/-5%', 'Y', NOW(3)),
  (212, 'SUB-C08', '贴标位置检查', 'CHECK', '标签位置与内容', '符合BOM', 'Y', NOW(3)),
  (213, 'SUB-M05', '传动链润滑', 'MAINTEN', '链条/导轨润滑', '每两周', 'Y', NOW(3)),
  (214, 'SUB-M06', '真空泵保养', 'MAINTEN', '真空泵油更换', '每季度', 'Y', NOW(3)),
  (215, 'SUB-M07', '水冷系统清洗', 'MAINTEN', '冷却水路清洗', '每半年', 'Y', NOW(3)),
  (216, 'SUB-M08', 'AGV电池维护', 'MAINTEN', '电池充放电校准', '每月', 'Y', NOW(3)),
  (217, 'SUB-C09', '气压表读数', 'CHECK', '主气路压力', '0.5-0.7 MPa', 'Y', NOW(3)),
  (218, 'SUB-M09', '安全门联锁测试', 'MAINTEN', '联锁功能验证', '每月', 'Y', NOW(3));

-- 计划扩充
INSERT INTO dv_check_plan (plan_id, plan_code, plan_name, plan_type, start_date, end_date, cycle_type, cycle_count, status, create_time)
VALUES
  (303, 'PLAN-CHECK-02', '测试线日点检', 'CHECK', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'DAY', 1, 'FINISHED', NOW(3)),
  (304, 'PLAN-CHECK-03', '包装线周点检', 'CHECK', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'WEEK', 1, 'FINISHED', NOW(3)),
  (305, 'PLAN-MAINTEN-02', '辅助设备季保养', 'MAINTEN', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'MONTH', 3, 'FINISHED', NOW(3));

INSERT INTO dv_check_machinery (record_id, plan_id, machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, create_time)
VALUES
  (406, 303, 113, 'DV-M013', '电气安规测试仪', 'Chroma', 'SAF-19032', NOW(3)),
  (407, 303, 114, 'DV-M014', '功率测试台', 'ATE', 'PWR-2KW', NOW(3)),
  (408, 304, 115, 'DV-M015', '贴标机', '永创', 'LBL-200', NOW(3)),
  (409, 304, 116, 'DV-M016', '热缩膜机', '东元', 'SHRINK-450', NOW(3)),
  (410, 305, 105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37', NOW(3)),
  (411, 305, 117, 'DV-M017', '冷却循环水机', '格力', 'CW-20HP', NOW(3)),
  (412, 301, 109, 'DV-M009', '扇叶动平衡机', ' Schenck', 'BAL-300', NOW(3)),
  (413, 301, 110, 'DV-M010', '网罩压装机', '永创', 'NET-PRS-80', NOW(3));

INSERT INTO dv_check_subject (record_id, plan_id, subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard, create_time)
VALUES
  (509, 303, 210, 'SUB-C06', '安规接地检查', 'CHECK', '接地电阻测试', '<=0.1 Ohm', NOW(3)),
  (510, 303, 211, 'SUB-C07', '功率偏差检查', 'CHECK', '额定功率偏差', '+/-5%', NOW(3)),
  (511, 304, 212, 'SUB-C08', '贴标位置检查', 'CHECK', '标签位置与内容', '符合BOM', NOW(3)),
  (512, 304, 209, 'SUB-C05', '网罩外观检查', 'CHECK', '检查网罩无变形、毛刺', '表面平整无锐边', NOW(3)),
  (513, 305, 214, 'SUB-M06', '真空泵保养', 'MAINTEN', '真空泵油更换', '每季度', NOW(3)),
  (514, 305, 215, 'SUB-M07', '水冷系统清洗', 'MAINTEN', '冷却水路清洗', '每半年', NOW(3));

-- 点检记录：3 条待办 + 11 条已完成
INSERT INTO dv_check_record (
  record_id, plan_id, plan_code, plan_name, plan_type,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  check_time, user_name, nick_name, status, create_time
)
VALUES
  (602, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 102, 'DV-M002', '扇叶压装机', '博世', 'PRS-150', NOW(3), 'repair', '设备维修员', 'PREPARE', NOW(3)),
  (603, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 101, 'DV-M001', '电机装配机', '松下', 'ASM-200', NOW(3), 'repair', '设备维修员', 'PREPARE', NOW(3)),
  (604, 303, 'PLAN-CHECK-02', '测试线日点检', 'CHECK', 113, 'DV-M013', '电气安规测试仪', 'Chroma', 'SAF-19032', NOW(3), 'repair', '设备维修员', 'PREPARE', NOW(3)),
  (605, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 106, 'DV-M006', '螺丝供料机', '快克', 'SF-100', DATE_SUB(NOW(3), INTERVAL 1 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 1 DAY)),
  (606, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 107, 'DV-M007', '风速测试仪', '德图', 'TEST-450', DATE_SUB(NOW(3), INTERVAL 2 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 2 DAY)),
  (607, 303, 'PLAN-CHECK-02', '测试线日点检', 'CHECK', 114, 'DV-M014', '功率测试台', 'ATE', 'PWR-2KW', DATE_SUB(NOW(3), INTERVAL 3 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 3 DAY)),
  (608, 304, 'PLAN-CHECK-03', '包装线周点检', 'CHECK', 115, 'DV-M015', '贴标机', '永创', 'LBL-200', DATE_SUB(NOW(3), INTERVAL 4 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 4 DAY)),
  (609, 304, 'PLAN-CHECK-03', '包装线周点检', 'CHECK', 104, 'DV-M004', '自动包装线', '永创', 'PKG-LINE-A', DATE_SUB(NOW(3), INTERVAL 5 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 5 DAY)),
  (610, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 109, 'DV-M009', '扇叶动平衡机', ' Schenck', 'BAL-300', DATE_SUB(NOW(3), INTERVAL 6 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 6 DAY)),
  (611, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 110, 'DV-M010', '网罩压装机', '永创', 'NET-PRS-80', DATE_SUB(NOW(3), INTERVAL 7 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 7 DAY)),
  (612, 303, 'PLAN-CHECK-02', '测试线日点检', 'CHECK', 103, 'DV-M003', '老化测试台', 'ATE', 'AGE-48H', DATE_SUB(NOW(3), INTERVAL 8 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 8 DAY)),
  (613, 301, 'PLAN-CHECK-01', '组装线日点检计划', 'CHECK', 111, 'DV-M011', '台扇机芯装配线', '松下', 'CORE-LINE-B', DATE_SUB(NOW(3), INTERVAL 9 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 9 DAY)),
  (614, 304, 'PLAN-CHECK-03', '包装线周点检', 'CHECK', 116, 'DV-M016', '热缩膜机', '东元', 'SHRINK-450', DATE_SUB(NOW(3), INTERVAL 10 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 10 DAY));

-- 保养记录：2 待办 + 6 完成
INSERT INTO dv_mainten_record (
  record_id, plan_id, plan_code, plan_name, plan_type,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
  mainten_time, user_name, nick_name, status, create_time
)
VALUES
  (802, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', 105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37', NOW(3), 'repair', '设备维修员', 'PREPARE', NOW(3)),
  (803, 305, 'PLAN-MAINTEN-02', '辅助设备季保养', 'MAINTEN', 117, 'DV-M017', '冷却循环水机', '格力', 'CW-20HP', NOW(3), 'repair', '设备维修员', 'PREPARE', NOW(3)),
  (804, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', 101, 'DV-M001', '电机装配机', '松下', 'ASM-200', DATE_SUB(NOW(3), INTERVAL 15 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 15 DAY)),
  (805, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', 102, 'DV-M002', '扇叶压装机', '博世', 'PRS-150', DATE_SUB(NOW(3), INTERVAL 20 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 20 DAY)),
  (806, 305, 'PLAN-MAINTEN-02', '辅助设备季保养', 'MAINTEN', 118, 'DV-M018', 'AGV小车#1', '海康', 'AGV-500', DATE_SUB(NOW(3), INTERVAL 25 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 25 DAY)),
  (807, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', 104, 'DV-M004', '自动包装线', '永创', 'PKG-LINE-A', DATE_SUB(NOW(3), INTERVAL 30 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 30 DAY)),
  (808, 302, 'PLAN-MAINTEN-01', '设备月度保养计划', 'MAINTEN', 108, 'DV-M008', '传送带', '东元', 'CV-12M', DATE_SUB(NOW(3), INTERVAL 35 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 35 DAY)),
  (809, 305, 'PLAN-MAINTEN-02', '辅助设备季保养', 'MAINTEN', 105, 'DV-M005', '空压机', '阿特拉斯', 'GA-37', DATE_SUB(NOW(3), INTERVAL 40 DAY), 'repair', '设备维修员', 'FINISHED', DATE_SUB(NOW(3), INTERVAL 40 DAY));

-- 维修单扩充
INSERT INTO dv_repair (
  repair_id, repair_code, repair_name,
  machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id,
  require_date, status, create_time
)
VALUES
  (1003, 'REP-20260708', '立柱旋紧机伺服报警', 112, 'DV-M012', '落地扇立柱旋紧机', '博世', 'TIGHT-120', 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), 'PREPARE', NOW(3)),
  (1004, 'REP-20260709', '冷却水机压缩机故障', 117, 'DV-M017', '冷却循环水机', '格力', 'CW-20HP', 5, NOW(3), 'CONFIRMED', NOW(3)),
  (1005, 'REP-20260702', '贴标机标签偏移', 115, 'DV-M015', '贴标机', '永创', 'LBL-200', 4, DATE_SUB(NOW(3), INTERVAL 8 DAY), 'FINISHED', DATE_SUB(NOW(3), INTERVAL 8 DAY)),
  (1006, 'REP-20260628', '功率测试台读数异常', 114, 'DV-M014', '功率测试台', 'ATE', 'PWR-2KW', 3, DATE_SUB(NOW(3), INTERVAL 12 DAY), 'FINISHED', DATE_SUB(NOW(3), INTERVAL 12 DAY)),
  (1007, 'REP-20260620', 'AGV导航偏差', 118, 'DV-M018', 'AGV小车#1', '海康', 'AGV-500', 5, DATE_SUB(NOW(3), INTERVAL 20 DAY), 'FINISHED', DATE_SUB(NOW(3), INTERVAL 20 DAY)),
  (1008, 'REP-20260615', '热缩膜温度不稳', 116, 'DV-M016', '热缩膜机', '东元', 'SHRINK-450', 4, DATE_SUB(NOW(3), INTERVAL 25 DAY), 'FINISHED', DATE_SUB(NOW(3), INTERVAL 25 DAY));

INSERT INTO dv_repair_line (
  line_id, repair_id, subject_id, subject_code, subject_name, subject_type, subject_content,
  malfunction, malfunction_url, repair_des, create_time
)
VALUES
  (1103, 1003, 207, 'SUB-M03', '皮带张紧', 'MAINTEN', '检查皮带张紧力',
   '伺服驱动器 E-240 报警', '/images/dv/demo/servo-alarm.png', NULL, NOW(3)),
  (1104, 1004, 215, 'SUB-M07', '水冷系统清洗', 'MAINTEN', '冷却水路清洗',
   '压缩机过载保护跳闸', '/images/dv/demo/compressor-fault.png', '待更换压缩机启动电容', NOW(3)),
  (1105, 1005, 212, 'SUB-C08', '贴标位置检查', 'CHECK', '标签位置与内容',
   '贴标位置偏移 3mm', '/images/dv/demo/label-offset.png', '重新校准传感器零点', NOW(3)),
  (1106, 1006, 211, 'SUB-C07', '功率偏差检查', 'CHECK', '额定功率偏差',
   '功率读数波动大', '/images/dv/demo/electrical-test.png', '更换采样模块', NOW(3)),
  (1107, 1007, 216, 'SUB-M08', 'AGV电池维护', 'MAINTEN', '电池充放电校准',
   'SLAM定位偏差', '/images/dv/demo/sensor-check.png', '更新地图与重新标定', NOW(3)),
  (1108, 1008, 205, 'SUB-M01', '润滑保养', 'MAINTEN', '导轨及传动部位润滑',
   '加热管接触不良', '/images/dv/demo/cooling-system.png', '紧固接线端子', NOW(3)),
  (1109, 1004, 214, 'SUB-M06', '真空泵保养', 'MAINTEN', '真空泵油更换',
   '冷媒压力偏低', '/images/dv/demo/cooling-system.png', '补充冷媒并检漏', NOW(3)),
  (1110, 1003, 201, 'SUB-C01', '电机扭矩检查', 'CHECK', '检查电机装配扭矩',
   '联轴器同轴度超差', '/images/dv/demo/belt-tension.png', '重新对中校正', NOW(3)),
  (1111, 1001, 208, 'SUB-M04', '紧固件检查', 'MAINTEN', '检查关键紧固螺栓',
   '供料轨道卡料，传感器误报', '/images/dv/demo/sensor-check.png', NULL, NOW(3)),
  (1112, 1001, 206, 'SUB-M02', '滤芯更换', 'MAINTEN', '更换空气滤芯',
   '轨道末端积料', '/images/dv/demo/lubrication.png', '清理轨道并调整节拍', NOW(3)),
  (1113, 1003, 204, 'SUB-C04', '噪音检测', 'CHECK', '运行噪音检测',
   '减速箱异响', '/images/dv/demo/belt-tension.png', '更换轴承', NOW(3)),
  (1114, 1004, 217, 'SUB-C09', '气压表读数', 'CHECK', '主气路压力',
   '冷凝器翅片脏堵', '/images/dv/demo/sensor-check.png', '清洗冷凝器', NOW(3));

-- 待办点检记录明细（602/603/604）
INSERT INTO dv_check_record_line (
  line_id, record_id, subject_id, subject_code, subject_name, subject_type,
  subject_content, subject_standard, check_status, attr2, create_time
)
VALUES
  (704, 602, 202, 'SUB-C02', '扇叶平衡检查', 'CHECK', '检查扇叶动平衡', 'G2.5级', 'Y', '/images/dv/demo/belt-tension.png', NOW(3)),
  (705, 602, 203, 'SUB-C03', '电气绝缘检查', 'CHECK', '检查绝缘电阻', '>=5 MOhm', 'Y', '/images/dv/demo/sensor-check.png', NOW(3)),
  (706, 602, 204, 'SUB-C04', '噪音检测', 'CHECK', '运行噪音检测', '<=45 dB', 'Y', NULL, NOW(3)),
  (707, 603, 201, 'SUB-C01', '电机扭矩检查', 'CHECK', '检查电机装配扭矩', '15-18 Nm', 'Y', '/images/dv/demo/electrical-test.png', NOW(3)),
  (708, 603, 202, 'SUB-C02', '扇叶平衡检查', 'CHECK', '检查扇叶动平衡', 'G2.5级', 'Y', NULL, NOW(3)),
  (709, 603, 204, 'SUB-C04', '噪音检测', 'CHECK', '运行噪音检测', '<=45 dB', 'Y', '/images/dv/demo/sensor-check.png', NOW(3)),
  (710, 604, 210, 'SUB-C06', '安规接地检查', 'CHECK', '接地电阻测试', '<=0.1 Ohm', 'Y', '/images/dv/demo/electrical-test.png', NOW(3)),
  (711, 604, 211, 'SUB-C07', '功率偏差检查', 'CHECK', '额定功率偏差', '+/-5%', 'Y', '/images/dv/demo/sensor-check.png', NOW(3)),
  (712, 604, 203, 'SUB-C03', '电气绝缘检查', 'CHECK', '检查绝缘电阻', '>=5 MOhm', 'Y', NULL, NOW(3));

-- 待办保养记录明细（802/803）
INSERT INTO dv_mainten_record_line (
  line_id, record_id, subject_id, subject_code, subject_name, subject_type,
  subject_content, subject_standard, mainten_status, attr2, create_time
)
VALUES
  (903, 802, 205, 'SUB-M01', '润滑保养', 'MAINTEN', '导轨及传动部位润滑', '每月一次', 'Y', '/images/dv/demo/lubrication.png', NOW(3)),
  (904, 802, 207, 'SUB-M03', '皮带张紧', 'MAINTEN', '检查并调整皮带张紧度', '松紧适中', 'Y', '/images/dv/demo/belt-tension.png', NOW(3)),
  (905, 802, 206, 'SUB-M02', '滤芯更换', 'MAINTEN', '更换空气滤芯', '每季度一次', 'Y', NULL, NOW(3)),
  (906, 803, 215, 'SUB-M07', '水冷系统清洗', 'MAINTEN', '冷却水路清洗', '每半年', 'Y', '/images/dv/demo/cooling-system.png', NOW(3)),
  (907, 803, 214, 'SUB-M06', '真空泵保养', 'MAINTEN', '真空泵油更换', '每季度', 'Y', '/images/dv/demo/lubrication.png', NOW(3));

-- 基础维修单 PREPARE 行补全项目与图片
UPDATE dv_repair_line SET
  subject_id = 206, subject_code = 'SUB-M02', subject_name = '滤芯更换', subject_type = 'MAINTEN',
  subject_content = '更换空气滤芯', malfunction_url = '/images/dv/demo/sensor-check.png'
WHERE line_id = 1101;
