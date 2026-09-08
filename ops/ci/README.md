# CI 建议

如果后续接入 GitHub Actions、Gitee Go 或其他 CI，建议最小流水线执行：

```bash
./ops/scripts/verify-local.sh
```

CI 环境需要：

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm 10+

当前项目还没有强制接入远程 CI，项目组长本地合并前必须先跑同样的验证脚本。
