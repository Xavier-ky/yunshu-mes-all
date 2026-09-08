# 分支与合并流程

## 日常开发

```bash
git checkout main
git pull
git checkout -b feature/member-a/masterdata-product
```

开发中只改自己模块目录。需要改公共目录时，先同步给项目组长。

## 提交前

运行：

```bash
./ops/scripts/verify-local.sh
```

然后提交：

```bash
git add .
git commit -m "feat(masterdata): add product list api"
git push origin feature/member-a/masterdata-product
```

## 合并前

1. 自己先把主分支合进功能分支。
2. 解决自己模块内冲突。
3. 再跑一次验证脚本。
4. 提交合并请求给项目组长。

```bash
git checkout feature/member-a/masterdata-product
git pull origin main
./ops/scripts/verify-local.sh
git push
```

## 合并规则

- `main` 只接受经过验证的合并。
- 项目组长负责最终合并。
- 公共目录变更必须写明影响范围。
- 数据库 migration 需要检查版本号是否和别人冲突。

## 避免冲突的习惯

- 不格式化与任务无关的文件。
- 不把 mock 数据散落到公共 store。
- 不在一个提交里混合多个模块。
- 不修改别人负责模块里的页面文案、字段名和接口路径。
- 大范围移动文件前先讨论。
