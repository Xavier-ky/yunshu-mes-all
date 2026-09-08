SELECT dt.dispatch_id, dt.dispatch_no, wo.work_order_no, co.order_no, dt.planned_qty, dt.status, u.username,
       dt.planned_start_time, dt.actual_start_time, dt.created_at
FROM dispatch_task dt
LEFT JOIN work_order wo ON wo.work_order_id = dt.work_order_id
LEFT JOIN customer_order co ON co.order_id = wo.order_id
LEFT JOIN sys_user u ON u.user_id = dt.operator_id
WHERE u.username IN ('worker','worker01') OR dt.operator_id = 6
ORDER BY dt.dispatch_id DESC
LIMIT 15;
