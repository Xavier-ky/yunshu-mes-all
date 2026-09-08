-- Process cover images (attr2) and step SOP rows for fan production line demo
-- Run with utf8mb4 client for Chinese content_text

UPDATE pro_process SET attr2 = '/process/cover-motor.jpg'
WHERE process_code = 'STEP-MOTOR';

UPDATE pro_process SET attr2 = '/process/cover-blade.jpg'
WHERE process_code = 'STEP-BLADE';

UPDATE pro_process SET attr2 = '/process/cover-aging.jpg'
WHERE process_code = 'STEP-AGING';

UPDATE pro_process SET attr2 = '/process/cover-pack.jpg'
WHERE process_code = 'STEP-PACK';

DELETE FROM pro_process_content
WHERE process_id IN (SELECT process_id FROM pro_process WHERE process_code IN ('STEP-MOTOR','STEP-BLADE','STEP-AGING','STEP-PACK'));

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 1, CONVERT('安装电机到主机壳体，确认定位销与螺丝孔对齐' USING utf8mb4), '电批', 'M4螺丝', '/process/step-motor-01.jpg', '电机装配步骤1', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-MOTOR';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 2, CONVERT('锁附固定螺丝并校验扭矩值在标准范围内' USING utf8mb4), '扭矩扳手', 'M4螺丝', '/process/step-motor-01.jpg', '电机装配步骤2', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-MOTOR';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 3, CONVERT('轻载通电测试电机转向与异响' USING utf8mb4), '测试仪', '—', '/process/step-motor-01.jpg', '电机装配步骤3', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-MOTOR';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 1, CONVERT('扇叶按标记方向压装到电机轴并锁紧' USING utf8mb4), '压装治具', '锁紧螺母', '/process/step-blade-01.jpg', '扇叶安装步骤1', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-BLADE';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 2, CONVERT('动平衡检测并记录偏差值' USING utf8mb4), '动平衡仪', '记录表', '/process/step-blade-01.jpg', '扇叶安装步骤2', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-BLADE';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 1, CONVERT('老化架通电运行，记录电流与噪音' USING utf8mb4), '老化架', '记录表', '/process/step-aging-01.jpg', '老化测试步骤1', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-AGING';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 2, CONVERT('连续运行30分钟后复测并贴合格标签' USING utf8mb4), '老化架', '合格标签', '/process/step-aging-01.jpg', '老化测试步骤2', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-AGING';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 1, CONVERT('放入缓冲泡沫后封箱并贴标签' USING utf8mb4), '封箱机', '纸箱', '/process/step-pack-01.jpg', '包装步骤1', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-PACK';

INSERT INTO pro_process_content (process_id, order_num, content_text, device, material, doc_url, remark, create_by, update_by, create_time)
SELECT p.process_id, 2, CONVERT('扫码核对型号并打印外箱标签' USING utf8mb4), '条码打印机', '外箱标签', '/process/step-pack-01.jpg', '包装步骤2', '', '', NOW(3)
FROM pro_process p WHERE p.process_code = 'STEP-PACK';
