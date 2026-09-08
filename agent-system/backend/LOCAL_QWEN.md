# 小精灵本地大脑

小精灵的推理、会话存储和启动脚本都放在本目录，模型文件仍位于相邻的 `../mymodel/`。

```text
backend/
├── app/
│   ├── companion/chat.py        # 小精灵对话编排：上下文、落库、模型调用
│   ├── llm/local_qwen.py        # 原始 Qwen3-0.6B 的懒加载 GPU 推理运行器
│   ├── storage/mysql_store.py   # agent_session / agent_message 持久化
│   └── main.py                  # POST /api/companion/chat
├── .env                         # 本机模型与 MySQL 配置（不会提交 Git）
└── start-local-qwen.ps1         # 启动 FastAPI 服务
```

## 启动

在 `agent-system/backend` 执行：

```powershell
.\start-local-qwen.ps1
```

启动整个 MES 项目时，直接在项目根目录运行 `start.cmd`（或 `./start.ps1`）。先启动 Docker Desktop；总启动脚本会检查/拉起 Qdrant `:6333`，再依次打开 MES 后端 `:8080`、本地 Qwen 小精灵 `:8090` 与前端 `:5173`，不需要再单独启动 AI 或向量库。

服务监听 `127.0.0.1:8090`。前端 Vite 将 `/yunshu-agent/*` 代理到它的 `/api/*`，因此右下角小精灵无需额外配置。

首次聊天会加载 `../mymodel/models/Qwen3-0.6B` 到 CUDA；模型常驻在该 FastAPI 进程中。访问 `GET /api/health` 可查看 `local_qwen.loaded` 状态。

## 数据与边界

每次请求均从 MES JWT 获取身份，而不信任前端传入的用户信息。服务会校验该用户存在于 `sys_user`，创建或复用只属于该用户的 `agent_session`，然后按顺序写入 `agent_message`：`USER` 问题、`AGENT` 回答。模型故障时，用户问题仍已保留；接口返回 503，不以固定文案冒充模型回答。

本实现只做问答，不允许模型直接写 MES 业务数据，也不允许模型执行任意 SQL。
