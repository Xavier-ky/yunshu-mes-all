# MES 种子数据清单

> 最后更新：2026-07-11  
> 本地库：`fan_mes`（Flyway 自动执行，无需手工跑 SQL）

---

## 执行方式

1. 启动后端（`start.cmd` → [1] 或 `mvnw spring-boot:run -Dspring-boot.run.profiles=local`）
2. Flyway 会依次执行：
   - `db/migration/V*.sql` — 表结构
   - `db/migration/R__*.sql` — 可重复种子（分析集成、空页补齐）
   - `db/seed/R__*.sql` — 可重复业务演示种子

所有 `R__` 脚本使用 `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE` / `WHERE NOT EXISTS`，**可安全重复执行**。

---

## 业务故事：云枢风扇工厂

单一连贯演示场景 — **电风扇总装车间**，3 个 SKU 贯通全链路：

| SKU | 编码 | 说明 |
|-----|------|------|
| 落地扇 A 型 | `FAN-FS40-A` | 主力 SKU，多数订单/工单 |
| 台扇 B 型 | `FAN-TS30-B` | 第二产线 |
| 壁扇 C 型 | `FAN-WS20-C` | 第三产线 |

**典型链路：** 客户订单 → 工单 `WO-20260701` → 齐套 → 领料 `IS-STORY-001` → 派工/报工 → 过程检验 → 成品入库 `PR-STORY-001` → 出货检验 `OQC-STORY-001` → 销售出库 → 产品 SN 追溯

**关键工单：**

| 工单 | 状态 | 用途 |
|------|------|------|
| WO-20260701 | RUNNING | 工作台、进度、报工、派工 |
| WO-20260705 | COMPLETED | 50 台 SN、成品入库、OQC、销售出库 |
| WO-20260707 | RUNNING | TS30 全链路 |
| WO-20260704 | CREATED | 齐套欠料（M4 螺丝） |

---

## 种子文件一览

| 文件 | 内容 |
|------|------|
| `R__seed_basic_data.sql` | 部门/角色/用户、工厂/产线/工位、FS40 基础 BOM、Agent |
| `R__seed_planning_demo.sql` | 订单 CO-20260701~05、工单、任务、派工 |
| `R__seed_story_01_master.sql` | 3 SKU、5 客户、5 供应商、BOM/工艺 |
| `R__seed_story_02_planning.sql` | 扩展订单/工单、齐套分析、甘特派工 |
| `R__seed_story_03_inventory.sql` | WM 12 类单据、库存批次 |
| `R__seed_story_04_production.sql` | 15 条报工、80 SN、流转卡/追溯 |
| `R__seed_story_05_quality.sql` | IQC/IPQC/OQC/RQC、检测模板 |
| `R__seed_story_06_calendar_system.sql` | 2026 节假日、班组、账号别名、消息 |
| `R__seed_story_07_enrichment.sql` | 领料/发料/质检记录/不良/放行、SN 过站、物料消耗、成品入库 |
| `R__seed_dv_equipment.sql` | 设备台账/点检/保养/维修（基础 8 台） |
| `R__seed_dv_equipment_story.sql` | 设备板块故事化扩充（18 台设备、14 点检记录、8 维修单、OEE 等） |
| `R__seed_sys_menu_dict.sql` | 菜单/字典/公告/操作日志 |
| `R__seed_andon_pro.sql` | 安灯配置与呼叫记录 |

---

## 测试账号

主账号（推荐）与文档别名账号均可登录，密码相同、角色相同：

| 主账号 | 别名（兼容） | 密码 | 角色 | 建议验证页面 |
|--------|--------------|------|------|--------------|
| admin | — | admin123 | 管理人员 | 系统管理、报表 |
| supervisor01 | supervisor | 123456 | 生产主管 | 订单/工单/排产/进度 |
| warehouse01 | warehouse | 123456 | 仓库物料员 | 仓储 12 页 |
| qc01 | quality | 123456 | 质检员 | 待检/IQC/IPQC/OQC/RQC |
| repair01 | repair | 123456 | 设备维修员 | 设备工作台/台账/点检保养/维修 |
| worker01 | worker | 123456 | 产线操作工 | 我的工位/报工 |
| tester | — | 123456 | 测试人员 | 全模块 |

---

## 数据量参考（约）

| 模块 | 表/指标 | 约数量 |
|------|---------|--------|
| 主数据 | md_client / md_vendor | 5 / 5 |
| 计划 | customer_order / work_order | 8 / 12+ |
| 执行 | dispatch_task / pro_feedback | 20+ / 15 |
| 追溯 | product_sn | 80 |
| 仓储 | wm_* 各类单据 | 各 2~4 单 |
| 质量 | qc_iqc/ipqc/oqc/rqc | 6 / 6 / 4 / 2 |
| 日历 | cal_holiday | 33 |

---

## UTF-8 验证

```sql
SET NAMES utf8mb4;
SELECT client_name, HEX(client_name) FROM md_client WHERE client_code = 'C001';
-- 期望 HEX 以 E58D8E 开头（「华」），而非 3F（问号）
```

---

## 手动重跑（可选）

若需在不重启后端的情况下重跑某个 story 脚本：

```bash
mysql -u root -p --default-character-set=utf8mb4 fan_mes < backend/mes-server/src/main/resources/db/seed/R__seed_story_04_production.sql
```

修改 `R__` 文件后，重启后端 Flyway 会因 checksum 变化自动重跑该脚本。

---

## 验证 SQL

```sql
SELECT u.username, u.real_name, r.role_name
FROM sys_user u
JOIN sys_user_role ur ON u.user_id = ur.user_id
JOIN sys_role r ON ur.role_id = r.role_id
WHERE u.username IN ('admin','supervisor01','warehouse01','qc01','worker01','tester')
ORDER BY u.user_id;

SELECT work_order_no, status, plan_qty, completed_qty FROM work_order ORDER BY work_order_no;
SELECT COUNT(*) AS sn_count FROM product_sn;
SELECT oqc_code, client_name, status FROM qc_oqc;
```
