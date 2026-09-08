# 团队拉取与首次运行检查

检查日期：2026-09-08。仓库：<https://github.com/Xavier-ky/yunshu-mes-all>，分支 `main`。

## 结论与验证范围

- 仓库为公开仓库，团队成员无需额外读取权限即可克隆。向该仓库直接推送代码仍需协作者写入权限。
- 本次从 GitHub 独立克隆，与上传副本的 Git 文件树完全一致，原始提交包含 3,176 个文件；本检查文档是后续新增文件。
- 按用户确认的范围，源码、项目资源、SQL、模型均纳入；依赖、构建产物、缓存和未完成下载排除。
- 上传副本的 API 密钥已改为占位符，本机原配置未修改。检查时修正了两个被误替换的数值参数：`LOCAL_QWEN_MAX_NEW_TOKENS` 和 `LOCAL_QWENL_MAX_NEW_TOKENS`，均恢复为 `384`。修正后 AI 配置导入通过。
- 前端 `npm ci` 和 `npm run build` 成功；报表组件及 MES 后端编译打包成功。构建使用本机 JDK 17 和可用的 Maven 依赖缓存，未验证全新空 Maven 缓存下载全部依赖。
- Python 语法检查：94 个文件通过。
- 前端现有测试：71 项，66 项通过、5 项失败。失败项涉及固定首页标签、路由标签收集、报表表格覆盖层、报表属性控件样式和报表 iframe 桥接。未在本次上传核验中改动这些业务实现。
- 本轮未在全新 MySQL 实例运行全部迁移，也未进行所有业务流程的端到端验收。因此不能将“完整上传、能够克隆和编译”理解为“零配置即可运行所有业务”。

## 1. 拉取完整项目

先安装 Git 和 Git LFS，再执行：

```powershell
git lfs install
git clone https://github.com/Xavier-ky/yunshu-mes-all.git
cd yunshu-mes-all
git lfs pull
git lfs fsck
```

大文件共约 3.1 GB。`git lfs ls-files` 可以检查模型和视频状态；应取得实际文件，不能只保留指针文本。协作开发请使用 Git 克隆并拉取 LFS 文件。

## 2. 首次环境准备

- Java：JDK 17，配置 `JAVA_HOME`。
- 前端：安装能满足 `frontend/web-admin/package-lock.json` 中依赖要求的 Node.js/npm；本轮测试使用 Node.js 24.14.0 / npm 11.9.0。
- 后端：项目自带 Maven Wrapper；首次下载 Wrapper 和依赖需要联网。
- 数据库：MySQL 8，准备 `fan_mes` 数据库并配置连接。数据库服务中的实时业务数据不属于 Git 文件；仓库提供的是迁移和种子脚本。
- AI / 人脸服务：安装 Python，并在各自环境安装对应 `requirements.txt`。
- 知识库：需要 Docker / Docker Compose 启动 Qdrant，并初始化、导入知识。被排除的本机向量索引需要重建。

根目录 README 的部分说明较旧：当前后端默认 profile 为 `local`，不是无需数据库的 mock 模式；`CONTRIBUTING.md` 当前不存在。以实际配置及本检查说明为准。

## 3. 已验证的前后端构建命令

以下后端命令必须从 `backend/mes-server` 目录执行，使 Wrapper 能找到 `.mvn/wrapper`：

```powershell
cd backend/mes-server
.\mvnw.cmd -B -f ../../third-party/ureport3/ureport3-parent/pom.xml install -DskipTests
.\mvnw.cmd -B -f ../../third-party/ureport3/pom.xml install -DskipTests
.\mvnw.cmd -B package -DskipTests
```

这会先构建仓库内已有的 UReport3 源码，再打包 MES 后端。无需克隆其他参考仓库。不要直接在根目录调用上述 Wrapper：本次验证中会导致 Wrapper 查找目录错误。`scripts/install-ureport3.ps1` 也有相同的工作目录问题，可先使用上面已验证的命令。

前端从仓库根目录执行：

```powershell
cd frontend/web-admin
npm ci
npm run build
npm run dev
```

`npm test` 当前有上述 5 个失败项，尚未达到全部测试通过。

## 4. 数据库与 AI 配置

1. 根据自己的 MySQL 修改后端连接配置或设置 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`。不要直接使用其他成员本机的数据库口令。
2. 当前 `application-local.yml` 开启 Flyway，并从 `db/migration`、`db/seed` 读取脚本。首次初始化需在团队指定的开发数据库验证迁移和种子数据；不要仅执行旧 README 的前三个建表脚本就视为完整初始化。
3. AI 服务的 `agent-system/backend/.env` 是已脱敏副本。可先参考 `.env.example`，设置自己的 `AGENT_DB_PASSWORD` 及需要使用的云模型 API Key。未配置的提供商密钥应留空，不要把 `REPLACE_WITH_YOUR_API_KEY` 当作有效值。
4. AI 的 `JWT_SECRET` 必须与 Spring Boot 后端的 `JWT_SECRET` 一致。
5. Qdrant 启动、初始化和知识导入步骤参见 `agent-system/README.md`。其中本机绝对路径需要替换为自己的目录和 Python 环境。
6. `start.ps1` 优先尝试本机 Anaconda 路径，再回退到 `python`；新成员应确认实际使用的 Python 已安装依赖。首次设置完成前，不应只双击启动脚本就预期所有服务可用。

人脸核验另见 `face-auth/README.md`；微信小程序另需微信开发者工具及团队对应的小程序配置。
