# 云枢智造 MES Web 管理端

Vue 3 + Vue Router 4 + Pinia + Axios + Vite 管理端骨架。

## 启动

```bash
cd frontend/web-admin
npm install
npm run dev
```

访问：

```text
http://127.0.0.1:5173/login
http://127.0.0.1:5173/dashboard/workbench
```

## 目录约定

- `src/layouts`：后台管理外壳
- `src/router`：路由定义
- `src/stores`：Pinia 状态
- `src/api`：Axios request 和接口模块
- `src/components`：公共组件
- `src/security/crypto`：前端加密预留
- `src/views/*`：各业务域页面骨架
