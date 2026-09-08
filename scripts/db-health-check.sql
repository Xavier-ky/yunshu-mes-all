-- fan_mes 数据库健康检查（只读）
-- 用法: mysql -uroot -p fan_mes < scripts/db-health-check.sql

SELECT '=== 1. Compat 主线表行数 ===' AS section;
SELECT 'wm_issue_header (compat领料)' AS item, COUNT(*) AS cnt FROM wm_issue_header
UNION ALL SELECT 'wm_product_recpt (compat入库)', COUNT(*) FROM wm_product_recpt
UNION ALL SELECT 'pro_feedback', COUNT(*) FROM pro_feedback
UNION ALL SELECT 'qc_ipqc', COUNT(*) FROM qc_ipqc
UNION ALL SELECT 'pro_card', COUNT(*) FROM pro_card
UNION ALL SELECT 'wm_transaction', COUNT(*) FROM wm_transaction;

SELECT '=== 2. 工单 lifecycle 分布 ===' AS section;
SELECT IFNULL(lifecycle_status, '(NULL-旧种子)') AS lifecycle_status, COUNT(*) AS cnt
FROM work_order WHERE is_deleted = 0
GROUP BY lifecycle_status ORDER BY cnt DESC;

SELECT '=== 3. 黄金/演示工单链路 ===' AS section;
SELECT wo.work_order_no,
       wo.lifecycle_status,
       (SELECT COUNT(*) FROM production_task t WHERE t.work_order_id = wo.work_order_id) AS tasks,
       (SELECT COUNT(*) FROM dispatch_task d WHERE d.work_order_id = wo.work_order_id) AS dispatches,
       (SELECT COUNT(*) FROM wm_issue_header i WHERE i.workorder_id = wo.work_order_id) AS issues,
       (SELECT COUNT(*) FROM pro_feedback f WHERE f.workorder_id = wo.work_order_id) AS feedbacks,
       (SELECT COUNT(*) FROM qc_ipqc q WHERE q.workorder_id = wo.work_order_id) AS ipqc,
       (SELECT COUNT(*) FROM wm_product_recpt r WHERE r.workorder_id = wo.work_order_id) AS recpts
FROM work_order wo
WHERE wo.is_deleted = 0
  AND (wo.work_order_no LIKE 'WO-GP%' OR wo.lifecycle_status IS NOT NULL)
ORDER BY wo.work_order_id DESC;

SELECT '=== 4. 孤儿 compat 单据 (无工单) ===' AS section;
SELECT 'wm_issue_header' AS tbl, issue_code AS doc_code, status
FROM wm_issue_header WHERE workorder_id IS NULL
UNION ALL
SELECT 'wm_product_recpt', recpt_code, status
FROM wm_product_recpt WHERE workorder_id IS NULL;

SELECT '=== 5. 同一工单多张有效入库/领料 (可能重复测试) ===' AS section;
SELECT wo.work_order_no, 'issue' AS doc_type, COUNT(*) AS finished_cnt
FROM wm_issue_header i
JOIN work_order wo ON wo.work_order_id = i.workorder_id
WHERE i.status = 'FINISHED'
GROUP BY wo.work_order_no HAVING COUNT(*) > 1
UNION ALL
SELECT wo.work_order_no, 'recpt', COUNT(*)
FROM wm_product_recpt r
JOIN work_order wo ON wo.work_order_id = r.workorder_id
WHERE r.status = 'FINISHED'
GROUP BY wo.work_order_no HAVING COUNT(*) > 1;

SELECT '=== 6. completed_qty 未回写 (lifecycle=COMPLETED 但完工数为0) ===' AS section;
SELECT work_order_no, lifecycle_status, plan_qty, completed_qty
FROM work_order
WHERE is_deleted = 0 AND lifecycle_status = 'COMPLETED' AND IFNULL(completed_qty, 0) = 0;
