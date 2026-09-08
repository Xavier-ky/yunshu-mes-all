-- 大屏演示数据日期刷新：保证「今日产出 / OEE / 排产任务」在 CURDATE() 下有可读数值
-- 可安全重复执行（Flyway repeatable）

UPDATE pro_feedback
SET feedback_time = NOW()
WHERE feedback_code IN ('FB-STORY-003', 'FB-STORY-010')
  AND DATE(feedback_time) <> CURDATE();

UPDATE production_report
SET report_time = CONCAT(CURDATE(), ' ', TIME(report_time))
WHERE report_no LIKE 'PR-20260701-H%'
  AND DATE(report_time) <> CURDATE();

UPDATE production_task
SET task_date = CURDATE(),
    start_time = COALESCE(start_time, NOW())
WHERE task_no IN ('PT-20260701', 'PT-20260702', 'PT-20260703')
  AND status IN ('RUNNING', 'DISPATCHED', 'CREATED');

UPDATE andon_event
SET occur_time = DATE_SUB(NOW(), INTERVAL 25 MINUTE)
WHERE andon_no = 'AD-20260701' AND status = 'OPEN';

UPDATE andon_event
SET occur_time = DATE_SUB(NOW(), INTERVAL 8 MINUTE)
WHERE andon_no = 'AD-20260702' AND status = 'OPEN';

INSERT INTO andon_event (andon_no, andon_type_id, reason_id, work_order_id, line_id, station_id, report_user_id, exception_desc, status, occur_time)
SELECT 'AD-20260703',
       (SELECT andon_type_id FROM andon_type WHERE type_code = 'PROCESS_HELP'),
       NULL,
       (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260703'),
       (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-03'),
       (SELECT station_id FROM workstation WHERE station_code = 'ST-07'),
       (SELECT user_id FROM sys_user WHERE username = 'worker01'),
       '扇叶动平衡', 'OPEN', DATE_SUB(NOW(), INTERVAL 15 MINUTE)
WHERE NOT EXISTS (SELECT 1 FROM andon_event WHERE andon_no = 'AD-20260703');

UPDATE andon_event
SET occur_time = DATE_SUB(NOW(), INTERVAL 15 MINUTE), status = 'OPEN'
WHERE andon_no = 'AD-20260703';
