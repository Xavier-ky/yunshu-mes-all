-- 打印中心演示：客户端 + 默认打印机
SET NAMES utf8mb4;

INSERT INTO print_client (
  client_code, client_name, client_ip, client_port, client_token, status,
  workshop_id, workshop_code, workshop_name, workstation_id, workstation_code, workstation_name,
  create_by, create_time
)
SELECT 'PC-LOCAL', '本机打印客户端', '127.0.0.1', 17521, 'yunshu-print-token', 'OFFLINE',
       w.workshop_id, w.workshop_code, w.workshop_name, ws.station_id, ws.station_code, ws.station_name,
       'seed', NOW()
FROM workstation ws
JOIN production_line pl ON ws.line_id = pl.line_id
JOIN workshop w ON pl.workshop_id = w.workshop_id
WHERE NOT EXISTS (SELECT 1 FROM print_client WHERE client_code = 'PC-LOCAL')
ORDER BY ws.station_id
LIMIT 1;

INSERT INTO print_printer_config (
  client_id, printer_code, printer_type, printer_name, brand, printer_model,
  connection_type, enable_flag, status, default_flag, remark, create_by, create_time
)
SELECT c.client_id, 'PRN-DEFAULT', 'LABEL', '默认标签打印机', 'Generic', 'DP-230',
       'CLIENT', 'Y', 'OFFLINE', 'Y', '演示默认打印机', 'seed', NOW()
FROM print_client c
WHERE c.client_code = 'PC-LOCAL'
  AND NOT EXISTS (
    SELECT 1 FROM print_printer_config p WHERE p.client_id = c.client_id AND p.printer_code = 'PRN-DEFAULT'
  );
