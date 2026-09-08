# API 协作说明

当前后端 API 以模块 controller 为准，公共响应结构为：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {},
  "traceId": "request-trace-id"
}
```

协作规则：

- 每个业务模块优先在自己的 controller 下新增接口。
- 前端模块通过 `frontend/web-admin/src/api/<module>.js` 调用。
- 公共契约稳定后再同步到 `shared/contracts` 或 `shared/schemas`。
- 修改公共响应结构必须由项目组长统一确认。
