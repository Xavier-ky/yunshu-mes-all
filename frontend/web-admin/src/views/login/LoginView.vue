<template>
  <main class="login-root">
    <div class="login-bg" aria-hidden="true">
      <video
        ref="bgVideoRef"
        class="login-bg-video"
        autoplay
        muted
        loop
        playsinline
        :src="loginBgVideo"
      />
      <div class="login-bg-veil"></div>
    </div>

    <section class="login-brand-panel" aria-label="云枢智造">
      <div class="login-brand-inner">
        <img class="login-brand-logo" :src="cloudLogo" alt="" />
        <div class="login-brand-copy">
          <h1 class="login-brand-title">云枢智造</h1>
          <p class="login-brand-tagline">制造运营平台</p>
        </div>
      </div>
    </section>

    <section class="login-form-panel">
      <button class="login-client-entry" type="button" @click="enterClientPortal">登录客户端</button>
      <div class="login-glass-shell">
        <div class="login-card">
          <div class="login-fan">
            <LoginAbstractGeometry :scale="0.54" :speed="0.3" :opacity="0.75" />
            <div class="login-fan-base"></div>
          </div>

          <h2 class="login-card-title">登录</h2>
          <p class="login-card-desc">欢迎回到云枢智造制造运营平台</p>

          <div class="login-tabs">
            <div class="login-tabs-track" :class="{ 'show-reg': showRegister }">
            <form class="login-form" @submit.prevent="enterSystem">
              <div class="login-field">
                <input
                  v-model="form.username"
                  autocomplete="username"
                  placeholder="账号"
                  type="text"
                />
              </div>
              <div class="login-field">
                <input
                  v-model="form.password"
                  autocomplete="current-password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="密码"
                />
                <button class="login-pw-toggle" type="button" @click="showPassword = !showPassword">
                  {{ showPassword ? "隐藏" : "显示" }}
                </button>
              </div>

              <p v-if="loginError" class="login-msg login-msg-err">{{ loginError }}</p>

              <div ref="roleDropdownRef" class="login-dev-role login-role-dropdown">
                <button
                  type="button"
                  class="login-role-trigger"
                  :class="{ open: roleOpen, placeholder: !selectedRole }"
                  @click.stop="roleOpen = !roleOpen"
                >
                  <span class="login-role-trigger-text" :title="selectedRoleLabel">{{ selectedRoleLabel }}</span>
                  <svg class="login-role-chevron" viewBox="0 0 20 20" aria-hidden="true">
                    <path d="M5.5 7.5L10 12l4.5-4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </button>
                <Transition name="login-role-menu">
                  <ul v-if="roleOpen" class="login-role-menu">
                    <li
                      v-for="role in roleOptions"
                      :key="role.value || 'empty'"
                      class="login-role-option"
                      :class="{ active: selectedRole === role.value, placeholder: !role.value }"
                      @click="selectRole(role.value)"
                    >
                      {{ role.label }}
                    </li>
                  </ul>
                </Transition>
              </div>

              <button class="login-btn" type="submit">登录</button>
              <button class="login-face-btn" type="button" @click="openFaceLogin">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8.2 3.5H6.5a3 3 0 0 0-3 3v1.7M15.8 3.5h1.7a3 3 0 0 1 3 3v1.7M20.5 15.8v1.7a3 3 0 0 1-3 3h-1.7M3.5 15.8v1.7a3 3 0 0 0 3 3h1.7M8.4 11.1c.4-1.5 1.6-2.4 3.6-2.4s3.2.9 3.6 2.4M9.3 15.1c.7.6 1.6.9 2.7.9s2-.3 2.7-.9" /></svg>
                人脸验证登录
              </button>

              <div class="login-extras">
                <button class="login-link" type="button" @click="showForgotMessage">忘记密码</button>
                <span class="login-sep"></span>
                <button class="login-link" type="button" @click="fillDemoAccount">演示账号</button>
                <span class="login-sep"></span>
                <button class="login-link" type="button" @click="toggleRegister">
                  {{ showRegister ? "去登录" : "注册" }}
                </button>
              </div>
            </form>

            <form class="login-form" @submit.prevent="registerSystem">
              <div class="login-field">
                <input v-model="registerForm.username" autocomplete="username" placeholder="账号" type="text" />
              </div>
              <div class="login-field">
                <input v-model="registerForm.password" autocomplete="new-password" type="password" placeholder="密码" />
              </div>
              <div class="login-field">
                <input v-model="registerForm.confirm" autocomplete="new-password" type="password" placeholder="确认密码" />
              </div>

              <p v-if="registerMessage" class="login-msg login-msg-info">{{ registerMessage }}</p>

              <button class="login-btn" type="submit">注册</button>
            </form>
            </div>
          </div>

          <div class="login-foot">
            <a href="/" @click.prevent="goHome">返回首页</a>
            <span class="login-foot-dot"></span>
            <span>v2.0</span>
          </div>
        </div>
      </div>
    </section>
    <Teleport to="body">
      <div v-if="faceLoginOpen" class="face-login-overlay" @click.self="closeFaceLogin">
        <section class="face-login-dialog" role="dialog" aria-modal="true" aria-labelledby="face-login-title">
          <button class="face-login-close" type="button" aria-label="关闭人脸验证" @click="closeFaceLogin">×</button>
          <div class="face-login-orb" aria-hidden="true"><span></span><span></span></div>
          <h2 id="face-login-title">人脸验证登录</h2>
          <p>请正视摄像头，完成后将以测试人员身份进入系统。</p>
          <div class="face-camera-frame" :class="{ 'is-checking': faceVerifying }">
            <video ref="faceVideoRef" autoplay playsinline muted></video>
            <div class="face-camera-guide" aria-hidden="true"></div>
            <span v-if="faceVerifying" class="face-scanner"></span>
          </div>
          <canvas ref="faceCanvasRef" class="face-canvas" aria-hidden="true"></canvas>
          <p class="face-login-status" :class="{ error: faceLoginError }">{{ faceLoginError || faceLoginStatus }}</p>
          <button class="face-confirm-btn" type="button" :disabled="faceVerifying || !faceCameraReady" @click="verifyFaceLogin">
            {{ faceVerifying ? "正在核验…" : "开始人脸核验" }}
          </button>
          <span class="face-login-note">仅用于本机测试人员账号核验，画面不会在浏览器端保存。</span>
        </section>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { reactive, ref, computed, nextTick, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { useAppStore } from "@/stores/app";
import { resolveDefaultHomePath } from "@/constants/home";
import { setClientSession } from "@/utils/client-session";
import LoginAbstractGeometry from "./LoginAbstractGeometry.vue";
import loginBgVideo from "../../../../images/202607151604.mp4";
import cloudLogo from "../../../../images/云.svg";

const router = useRouter();
const app = useAppStore();
const bgVideoRef = ref(null);
const roleDropdownRef = ref(null);
const roleOpen = ref(false);

const roleOptions = [
  { value: "", label: "— 开发角色选择 —" },
  { value: "admin", label: "系统管理员 (admin)" },
  { value: "supervisor", label: "生产主管 (supervisor)" },
  { value: "warehouse", label: "仓库物料员 (warehouse)" },
  { value: "quality", label: "质检员 (quality)" },
  { value: "repair", label: "设备维修员 (repair)" },
  { value: "worker", label: "产线操作工 (worker)" },
  { value: "tester", label: "测试人员 (tester)" },
];

const selectedRoleLabel = computed(
  () => roleOptions.find((role) => role.value === selectedRole.value)?.label || "— 开发角色选择 —",
);

function handleClickOutside(event) {
  if (roleDropdownRef.value && !roleDropdownRef.value.contains(event.target)) {
    roleOpen.value = false;
  }
}

function selectRole(value) {
  selectedRole.value = value;
  fillRoleAccount(value);
  roleOpen.value = false;
}

function enterClientPortal() {
  setClientSession();
  router.push("/client/mall");
}

const form = reactive({ username: "", password: "" });
const registerForm = reactive({ username: "", password: "", confirm: "" });
const showPassword = ref(false);
const showRegister = ref(false);
const loginError = ref("");
const registerMessage = ref("");
const faceLoginOpen = ref(false);
const faceVideoRef = ref(null);
const faceCanvasRef = ref(null);
const faceLoginStatus = ref("准备摄像头中…");
const faceLoginError = ref("");
const faceVerifying = ref(false);
const faceCameraReady = ref(false);
let faceStream;

onMounted(() => {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches && bgVideoRef.value) {
    bgVideoRef.value.pause();
  }

  document.addEventListener("click", handleClickOutside);

  // Dev quick-login via URL param: ?role=warehouse
  const params = new URLSearchParams(window.location.search);
  const roleParam = params.get("role");
  if (roleParam && roleAccounts[roleParam]) {
    fillRoleAccount(roleParam);
  }
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside);
  stopFaceCamera();
});

const selectedRole = ref("");

const roleAccounts = {
  admin: { username: "admin", password: "admin123" },
  supervisor: { username: "supervisor", password: "123456" },
  warehouse: { username: "warehouse", password: "123456" },
  quality: { username: "quality", password: "123456" },
  repair: { username: "repair", password: "123456" },
  worker: { username: "worker", password: "123456" },
  tester: { username: "tester", password: "123456" },
};

function fillRoleAccount(key) {
  const roleKey = typeof key === "string" ? key : selectedRole.value;
  if (!roleKey) return;
  const acct = roleAccounts[roleKey];
  if (acct) {
    form.username = acct.username;
    form.password = acct.password;
    loginError.value = "";
  }
}

function fillDemoAccount() {
  form.username = "admin";
  form.password = "admin123";
  loginError.value = "";
}

function toggleRegister() {
  showRegister.value = !showRegister.value;
  loginError.value = "";
  registerMessage.value = "";
}

function showForgotMessage() {
  loginError.value = "忘记密码功能暂未开放，请联系管理员处理。";
}

async function registerSystem() {
  if (!registerForm.username || !registerForm.password || !registerForm.confirm) {
    registerMessage.value = "请完整填写注册信息。";
    return;
  }
  if (registerForm.password !== registerForm.confirm) {
    registerMessage.value = "两次输入的密码不一致。";
    return;
  }
  registerMessage.value = "注册功能暂未开放，当前请联系管理员创建账号。";
}

function goHome() {
  router.push("/");
}

async function enterSystem() {
  loginError.value = "";
  try {
    await app.login({ username: form.username, password: form.password });
    const redirect = router.currentRoute.value.query.redirect;
    const role = (app.authUser?.roleCodes || [])[0] || "";
    router.push(typeof redirect === "string" ? redirect : resolveDefaultHomePath(role));
  } catch (error) {
    loginError.value = error?.message || "登录失败";
  }
}

function stopFaceCamera() {
  if (faceStream) {
    faceStream.getTracks().forEach((track) => track.stop());
    faceStream = undefined;
  }
  faceCameraReady.value = false;
}

async function openFaceLogin() {
  faceLoginOpen.value = true;
  faceLoginError.value = "";
  faceLoginStatus.value = "正在请求摄像头权限…";
  faceCameraReady.value = false;
  await nextTick();
  try {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error("当前浏览器不支持摄像头访问，请使用最新版 Chrome 或 Edge。");
    }
    faceStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 480 } },
      audio: false,
    });
    faceVideoRef.value.srcObject = faceStream;
    await faceVideoRef.value.play();
    faceCameraReady.value = true;
    faceLoginStatus.value = "摄像头已就绪，请保持正脸和光线清晰。";
  } catch (error) {
    faceLoginError.value = error?.message || "无法打开摄像头，请检查浏览器权限。";
  }
}

function closeFaceLogin() {
  if (faceVerifying.value) return;
  stopFaceCamera();
  faceLoginOpen.value = false;
  faceLoginError.value = "";
}

async function verifyFaceLogin() {
  const video = faceVideoRef.value;
  const canvas = faceCanvasRef.value;
  if (!video || !canvas || !video.videoWidth || !video.videoHeight) {
    faceLoginError.value = "尚未读取到摄像头画面，请稍后重试。";
    return;
  }
  faceVerifying.value = true;
  faceLoginError.value = "";
  faceLoginStatus.value = "正在进行本地人脸核验，请保持页面开启…";
  try {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d").drawImage(video, 0, 0, canvas.width, canvas.height);
    const imageBase64 = canvas.toDataURL("image/jpeg", 0.9);
    await app.faceLogin(imageBase64);
    stopFaceCamera();
    faceLoginOpen.value = false;
    const redirect = router.currentRoute.value.query.redirect;
    const role = (app.authUser?.roleCodes || [])[0] || "";
    router.push(typeof redirect === "string" ? redirect : resolveDefaultHomePath(role));
  } catch (error) {
    faceLoginError.value = error?.message || "人脸核验未通过，请保持正脸后重试。";
    faceLoginStatus.value = "";
  } finally {
    faceVerifying.value = false;
  }
}
</script>

<style scoped>
.login-root {
  position: relative;
  display: flex;
  min-height: 100vh;
  overflow: visible;
  font-family: "SF Pro Text", "PingFang SC", "Segoe UI", "Helvetica Neue", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
}

.login-bg-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-bg-veil {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.58);
}

.login-client-entry {
  position: absolute;
  top: 24px;
  right: 28px;
  z-index: 20;
  border: 1px solid #e4393c;
  background: #fff;
  color: #e4393c;
  font-size: 13px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
}

.login-client-entry:hover {
  background: #e4393c;
  color: #fff;
  box-shadow: 0 4px 12px rgba(228, 57, 60, 0.25);
}

.login-brand-panel {
  position: relative;
  z-index: 1;
  flex: 0 0 50%;
  max-width: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(32px, 6vw, 96px);
  pointer-events: none;
}

.login-brand-inner {
  display: flex;
  align-items: center;
  gap: clamp(20px, 2.8vw, 36px);
  max-width: 520px;
}

.login-brand-logo {
  width: clamp(72px, 8vw, 108px);
  height: auto;
  flex-shrink: 0;
  display: block;
  opacity: 0.9;
  filter: brightness(0.32);
  transform: translateY(-10px);
}

.login-brand-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}

.login-brand-title {
  margin: 0;
  font-family: "钉钉进步体", "DingTalk JinBuTi", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: clamp(40px, 5.2vw, 64px);
  font-weight: 400;
  line-height: 1.1;
  letter-spacing: 0.12em;
  color: #141414;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.65);
}

.login-brand-tagline {
  margin: 10px 0 0;
  font-family: "钉钉进步体", "DingTalk JinBuTi", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: clamp(14px, 1.4vw, 18px);
  font-weight: 400;
  letter-spacing: 0.32em;
  color: rgba(20, 20, 20, 0.55);
}

.login-form-panel {
  position: relative;
  z-index: 1;
  flex: 0 0 50%;
  max-width: 50%;
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(48px, 9vw, 128px) 48px clamp(32px, 5vw, 72px);
  background: transparent;
}

.login-glass-shell {
  width: min(100%, 480px);
  padding: 48px 44px 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(40px) saturate(190%);
  -webkit-backdrop-filter: blur(40px) saturate(190%);
  border: 1px solid rgba(255, 255, 255, 0.52);
  box-shadow:
    0 16px 40px rgba(15, 23, 42, 0.1),
    0 2px 8px rgba(255, 255, 255, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.65),
    inset 0 -1px 0 rgba(255, 255, 255, 0.12);
}

.login-card {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}

.login-fan {
  position: relative;
  width: 168px;
  height: 138px;
  margin: 0 auto 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-fan :deep(.geo-root) {
  width: 168px;
  height: 100px;
}

.login-fan-base {
  width: 0;
  height: 0;
  border-left: 13px solid transparent;
  border-right: 13px solid transparent;
  border-bottom: 50px solid #1a1a1a;
  opacity: 0.12;
  margin-top: -48px;
  flex-shrink: 0;
}

.login-card-title {
  margin: 0 0 8px;
  text-align: center;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #111;
}

.login-card-desc {
  margin: 0 0 36px;
  text-align: center;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.6;
  letter-spacing: 0.02em;
  color: #5c5c5c;
}

.login-tabs {
  position: relative;
  min-height: 340px;
}

.login-tabs-track {
  position: relative;
  overflow: visible;
  min-height: 340px;
}

.login-tabs-track .login-form {
  position: absolute;
  inset: 0;
}

.login-tabs-track .login-form:first-child {
  transform: translateX(0);
  opacity: 1;
  transition: transform 0.45s ease, opacity 0.45s ease;
}

.login-tabs-track .login-form:last-child {
  transform: translateX(40px);
  opacity: 0;
  pointer-events: none;
  transition: transform 0.45s ease, opacity 0.45s ease;
}

.login-tabs-track.show-reg .login-form:first-child {
  transform: translateX(-40px);
  opacity: 0;
  pointer-events: none;
}

.login-tabs-track.show-reg .login-form:last-child {
  transform: translateX(0);
  opacity: 1;
  pointer-events: auto;
}

.login-form {
  display: grid;
  gap: 12px;
  align-content: start;
}

.login-field {
  position: relative;
  width: 100%;
}

.login-field input,
.login-role-trigger,
.login-btn {
  display: block;
  width: 100%;
  height: 52px;
  box-sizing: border-box;
  border-radius: 10px;
  font-family: inherit;
}

.login-field input {
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.72);
  color: #111;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0.02em;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.login-field input:focus {
  outline: none;
  border-color: rgba(17, 17, 17, 0.35);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.4);
}

.login-field input::placeholder {
  color: #8f8f8f;
  font-weight: 400;
}

.login-pw-toggle {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.04em;
  cursor: pointer;
}

.login-msg {
  margin: -4px 0 0;
  font-size: 13px;
  line-height: 1.55;
}

.login-msg-err {
  color: #d64545;
}

.login-dev-role {
  width: 100%;
}

.login-role-dropdown {
  position: relative;
}

.login-role-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.72);
  color: #333;
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.login-role-trigger.placeholder {
  color: #8f8f8f;
  font-weight: 400;
}

.login-role-trigger.open,
.login-role-trigger:focus-visible {
  outline: none;
  border-color: rgba(17, 17, 17, 0.35);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.4);
}

.login-role-trigger-text {
  flex: 1;
  min-width: 0;
  text-align: left;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.login-role-chevron {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #666;
  transition: transform 0.22s ease, color 0.15s ease;
}

.login-role-trigger.open .login-role-chevron {
  transform: rotate(180deg);
  color: #333;
}

.login-role-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 30;
  margin: 0;
  padding: 6px;
  list-style: none;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow:
    0 16px 36px rgba(15, 23, 42, 0.12),
    0 4px 12px rgba(15, 23, 42, 0.06);
  max-height: min(320px, 50vh);
  overflow-y: auto;
}

.login-role-option {
  padding: 11px 14px;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0.01em;
  line-height: 1.45;
  color: #333;
  white-space: normal;
  word-break: break-word;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.login-role-option.placeholder {
  color: #8f8f8f;
  font-weight: 400;
}

.login-role-option:hover {
  background: rgba(17, 17, 17, 0.06);
  color: #111;
}

.login-role-option.active {
  background: rgba(17, 17, 17, 0.08);
  color: #111;
  font-weight: 500;
}

.login-role-menu-enter-active,
.login-role-menu-leave-active {
  transition: opacity 0.2s ease, transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
  transform-origin: top center;
}

.login-role-menu-enter-from,
.login-role-menu-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}

.login-msg-info {
  color: #888;
}

.login-btn {
  border: none;
  background: #111;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.login-btn:hover {
  background: #333;
}

.login-btn:active {
  transform: scale(0.98);
}

.login-face-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  margin-top: 10px;
  border: 1px solid rgba(91, 128, 210, 0.36);
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(238, 245, 255, 0.92), rgba(252, 247, 255, 0.96));
  color: #4d67a7;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(98, 128, 196, 0.1);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.login-face-btn svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.login-face-btn:hover {
  border-color: rgba(88, 125, 212, 0.62);
  box-shadow: 0 10px 22px rgba(98, 128, 196, 0.18);
  transform: translateY(-1px);
}

.face-login-overlay {
  position: fixed;
  z-index: 10000;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(18, 29, 53, 0.38);
  backdrop-filter: blur(10px);
}

.face-login-dialog {
  position: relative;
  width: min(100%, 438px);
  padding: 30px 30px 24px;
  overflow: hidden;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.74);
  border-radius: 24px;
  background: radial-gradient(circle at 15% 5%, rgba(215, 231, 255, 0.96), transparent 34%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(246, 248, 255, 0.95));
  box-shadow: 0 26px 68px rgba(16, 33, 72, 0.28);
}

.face-login-close {
  position: absolute;
  top: 13px;
  right: 15px;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.62);
  color: #7890b5;
  font-size: 25px;
  line-height: 30px;
  cursor: pointer;
}

.face-login-orb {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 45px;
  height: 45px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 25%, #e9fbff, #9ed8fc 42%, #8f8eea 100%);
  box-shadow: 0 8px 18px rgba(101, 137, 221, 0.24);
}

.face-login-orb span {
  width: 4px;
  height: 7px;
  border-radius: 50%;
  background: #456b9a;
}

.face-login-dialog h2 {
  margin: 0;
  color: #36568c;
  font-size: 21px;
  letter-spacing: 0.04em;
}

.face-login-dialog > p {
  margin: 8px auto 18px;
  color: #7888a5;
  font-size: 13px;
  line-height: 1.65;
}

.face-camera-frame {
  position: relative;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  border: 1px solid rgba(106, 146, 219, 0.42);
  border-radius: 18px;
  background: #172238;
  box-shadow: inset 0 0 0 5px rgba(220, 236, 255, 0.08);
}

.face-camera-frame video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.face-camera-guide {
  position: absolute;
  top: 12%;
  left: 50%;
  width: 42%;
  height: 68%;
  transform: translateX(-50%);
  border: 1.5px solid rgba(208, 238, 255, 0.76);
  border-radius: 48% 48% 43% 43%;
  box-shadow: 0 0 0 999px rgba(7, 20, 43, 0.1), 0 0 18px rgba(134, 219, 255, 0.5);
}

.face-scanner {
  position: absolute;
  right: 10%;
  left: 10%;
  height: 2px;
  background: #a5efff;
  box-shadow: 0 0 12px #75dbff;
  animation: face-scan 1.3s ease-in-out infinite alternate;
}

.face-canvas {
  display: none;
}

.face-login-status {
  min-height: 22px;
  margin: 13px 0 11px !important;
  color: #637b9e !important;
}

.face-login-status.error {
  color: #c25e6b !important;
}

.face-confirm-btn {
  width: 100%;
  min-height: 44px;
  border: 0;
  border-radius: 11px;
  background: linear-gradient(105deg, #7093df, #669fd6);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  box-shadow: 0 9px 17px rgba(75, 121, 209, 0.25);
}

.face-confirm-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.face-login-note {
  display: block;
  margin-top: 12px;
  color: #9ba8bd;
  font-size: 11px;
}

@keyframes face-scan {
  from { top: 16%; }
  to { top: 84%; }
}

.login-extras {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-top: 4px;
}

.login-link {
  border: none;
  padding: 6px 10px;
  background: none;
  color: #666;
  font-size: 13px;
  letter-spacing: 0.02em;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.15s;
}

.login-link:hover {
  color: #111;
}

.login-sep {
  width: 1px;
  height: 12px;
  background: #e0e0e0;
}

.login-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.login-foot a {
  color: #888;
  font-size: 14px;
  letter-spacing: 0.02em;
  text-decoration: none;
}

.login-foot a:hover {
  color: #111;
}

.login-foot-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #d0d0d0;
}

.login-foot span {
  color: #888;
  font-size: 14px;
  letter-spacing: 0.04em;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 860px) {
  .login-brand-panel {
    display: none;
  }

  .login-form-panel {
    flex: 1;
    max-width: 100%;
    margin-left: 0;
    justify-content: center;
    padding: 40px 24px;
  }

  .login-glass-shell {
    padding: 36px 24px 24px;
    border-radius: 22px;
  }

  .login-card-title {
    font-size: 26px;
  }

  .login-card-desc {
    font-size: 14px;
  }

  .login-client-entry {
    position: fixed;
    top: 16px;
    right: 16px;
    font-size: 12px;
    padding: 7px 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-bg-video {
    display: none;
  }
}
</style>
