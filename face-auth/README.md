# MES 人脸核验服务

该服务只负责本机 `tester`（测试人员）账号的 1:1 人脸核验：浏览器采集一帧画面，与 `data/sry_face_data/` 内的授权登记照片比对。它不签发 JWT，也不保存浏览器采集的临时画面；JWT 仍由 MES Spring Boot 后端签发。

## 运行方式

```powershell
D:\Anaconda2024.10\envs\pytorch\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8091
```

项目启动时会由 `run-supervisor.ps1` 守护；如果 8091 的工作进程意外退出，守护器会在 3 秒后拉起新进程。项目根目录的 `stop.ps1` 会同时停止守护器及其子进程，不会触发自动重启。

## 识别实现

- 使用 OpenCV SFace 特征模型（DeepFace 也支持的 SFace 识别模型）在 CPU 上生成 128 维人脸特征。
- SFace 权重固定保存在 `data/models/face_recognition_sface_2021dec.onnx`；已准备好后不需要联网。
- 启动时读取并缓存登记照片的特征；每次登录只计算一次摄像头画面的特征，再进行余弦距离比对。
- 当前距离阈值为 `0.46`。距离越小越相似；小于等于该阈值时通过核验。

此前的 DeepFace Facenet512 路径会加载 TensorFlow。此电脑的 Windows 事件日志显示 TensorFlow 进程会受 NVIDIA 图形驱动模块 `nvdxgdmal64.dll` 影响而崩溃，因此运行时改为不加载 TensorFlow 的 OpenCV SFace 路径。已安装的 DeepFace 与 Facenet 权重不会被删除，也不会影响项目中其他 AI 功能。

## 安全边界

- 服务仅监听 `127.0.0.1`，不向局域网公开。
- 前端不能指定要登录的账号；Spring Boot 只会在核验成功后登录绑定的 `tester` 账号。
- 登记照片与运行时数据均被 `.gitignore` 排除。
- 生产部署应保留密码登录等备用方式，并落实授权、审计和个人信息管理要求。
