# 云枢智造 MES 对称加密预留规范

当前阶段只建立目录、配置项和前后端占位实现，后续统一补齐真实加密协议。

## 推荐约定

- 算法：AES-GCM
- 密钥长度：256 bit
- Nonce：每次请求随机生成，不复用
- 优先加密范围：登录密码、高风险业务请求、Agent 工具审批信息
- 输出格式：`algorithm + nonce + cipherText + authTag + timestamp`
- 后端异常码：`CRYPTO_ERROR`

## 代码位置

- 后端：`backend/mes-server/src/main/java/com/yunshu/mes/security/crypto`
- Web 管理端：`frontend/web-admin/src/security/crypto`
- 小程序预留：`frontend/wechat-miniprogram/utils/crypto`
