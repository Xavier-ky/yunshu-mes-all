# 后端模块协作说明

每个业务域固定使用以下内部结构：

```text
controller/
service/
repository/
entity/
dto/
vo/
converter/
enums/
```

当前已创建的业务域：

- `system`：用户、角色、权限、日志
- `factory`：工厂、车间、产线、工位
- `masterdata`：产品、物料、BOM
- `process`：工艺、工序、路线、SOP
- `barcode`：条码规则、条码生成、包装绑定
- `planning`：订单、工单、齐套、派工
- `inventory`：仓库、库存、领料、退料、出入库
- `production`：生产执行、报工、SN、物料绑定
- `quality`：质检、缺陷、返工、放行
- `andon`：安灯呼叫、响应、处理
- `equipment`：设备、点检、维修、OEE
- `traceability`：追溯
- `reporting`：报表
- `integration`：外部系统接口
- `agent`：智能 Agent、知识库、任务、工具调用

跨模块调用应通过 service 接口，不允许 controller 直接调用其他模块 repository。

## 团队协作

- 每个成员优先只修改自己负责的业务域包。
- 新增接口时保持 controller/service/repository/entity/dto/vo/converter/enums 分层。
- 如果需要修改 `common/config/security`，先由项目组长确认。
- 详细分工见 `../../docs/collaboration/team_split.md`。
