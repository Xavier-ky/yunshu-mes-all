# 合并前检查清单

## 基础检查

- [ ] 分支名符合 `feature/<owner>/<module>-<desc>` 或 `fix/<owner>/<module>-<desc>`。
- [ ] 改动集中在自己负责的模块目录。
- [ ] 没有修改与任务无关的文件。
- [ ] 没有提交 `node_modules`、`dist`、`target`、日志或本地密钥。

## 后端检查

- [ ] controller/service/repository/entity/dto/vo/converter/enums 分层清晰。
- [ ] controller 不直接调用其他模块 repository。
- [ ] 返回结构使用统一 `ApiResponse`。
- [ ] 新增接口路径在本模块 namespace 下。
- [ ] 新增异常使用全局异常结构。

## 前端检查

- [ ] 页面在 `views/<domain>` 内。
- [ ] 接口调用通过 `src/api`。
- [ ] 没有在页面里硬编码后端 base URL。
- [ ] 表格、查询、弹窗骨架符合后台管理系统风格。
- [ ] 页面在浏览器中无控制台错误。

## 数据库检查

- [ ] 没有修改已经合并的历史 migration。
- [ ] 新 migration 版本号没有冲突。
- [ ] 表名、字段名符合数据库文档约定。
- [ ] 新表能归属到明确业务模块。

## 验证命令

```bash
./ops/scripts/verify-local.sh
```

通过后再发起合并。
