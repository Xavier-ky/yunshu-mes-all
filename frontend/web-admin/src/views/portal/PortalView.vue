<template>
  <main class="intro" ref="rootRef">
    <div class="intro-screen" :class="{ off: phase === 'ready' }"></div>

    <Teleport to="body">
      <div
        class="clover-float"
        :style="{ ...cloverStyle, zIndex: 50 }"
      >
        <AbstractGeometry :speed="cloverSpeed" :scale="cloverScale" />
      </div>
    </Teleport>

    <div class="nav-dots" aria-label="页面导航">
      <button
        v-for="(dot, i) in dots"
        :key="dot.id"
        class="nav-dot"
        :class="{ active: activeSection === i }"
        :aria-label="dot.label"
        @click="scrollTo(dot.id)"
      >
        <span class="dot-pip"></span>
        <span class="dot-label">{{ dot.label }}</span>
      </button>
    </div>

    <header class="intro-bar" :class="{ on: scrolled }">
      <RouterLink class="bar-logo" to="/">云枢智造<span>MES</span></RouterLink>
      <nav class="bar-nav">
        <a href="#cap" @click.prevent="scrollTo('cap')">能力</a>
        <a href="#flow" @click.prevent="scrollTo('flow')">流程</a>
      </nav>
      <RouterLink class="bar-cta" to="/login">进入控制台</RouterLink>
    </header>

    <section id="hero" class="hero" ref="heroRef">
      <div class="hero-text" :class="{ 'hero-text-compact': heroPhase !== 'title' }">
        <h1 v-if="heroPhase === 'title'" class="hero-h1">
          <span
            v-for="(char, index) in splitChars('为每一台风扇')"
            :key="`a${index}`"
            class="hc"
            :style="{ transitionDelay: `${0.04 + index * 0.04}s` }"
            :class="{ in: heroVisible }"
            >{{ char }}</span
          >
          <br />
          <span
            v-for="(char, index) in splitChars('建立数字档案')"
            :key="`b${index}`"
            class="hc accent"
            :style="{ transitionDelay: `${0.04 + (index + 8) * 0.04}s` }"
            :class="{ in: heroVisible }"
            >{{ char }}</span
          >
        </h1>

        <div v-else class="hero-typewriter-stage">
          <div class="hero-typewriter" v-show="heroPhase !== 'button' && heroPhase !== 'brand'">
            <span
              v-for="(char, index) in splitChars(typewriterText)"
              :key="index"
              class="tw-char"
              :class="{ 'accent': index >= typewriterLine1.length && heroPhase !== 'typing' }"
            >
              <template v-if="char === '\n'"><br /></template>
              <template v-else>{{ char }}</template>
            </span>
            <span class="tw-cursor">|</span>
          </div>

          <RouterLink
            v-if="heroPhase === 'button'"
            class="hero-enter-wrap"
            to="/login"
          >
            <span class="hero-enter-sub">云枢智造 MES</span>
            <span class="hero-enter-text">
              进入控制台
            </span>
            <span class="hero-enter-hint">
              <span class="enter-line"></span>
              <span class="enter-arrow">&rarr;</span>
            </span>
          </RouterLink>

          <div v-if="heroPhase === 'brand'" class="hero-brand-final">
            <p class="brand-final-tag">YUNSHU MANUFACTURING EXECUTION SYSTEM</p>
            <h2 class="brand-final-title">
              <span class="brand-char" v-for="(char, i) in splitChars('云枢智造')" :key="i" :style="{ animationDelay: `${0.1 + i * 0.08}s` }">{{ char }}</span>
            </h2>
          </div>
        </div>
      </div>
      <div class="hero-visual"></div>
    </section>

    <section id="cap" class="sec sec-cap" ref="capRef">
      <div class="sec-label" :class="{ in: inView.cap }">
        <span>01</span> 核心能力
      </div>
      <h2 class="sec-h2" :class="{ in: inView.cap }">
        <span
          v-for="(char, index) in splitChars('六大业务模块')"
          :key="index"
          class="hc"
          :style="{ transitionDelay: `${index * 0.04}s` }"
          :class="{ in: inView.cap }"
          >{{ char }}</span
        >
      </h2>
      <div class="cap-grid" :class="{ in: inView.cap }">
        <div
          v-for="(item, index) in capabilities"
          :key="item.title"
          class="cap-card"
          :style="{ transitionDelay: `${0.1 + index * 0.08}s` }"
        >
          <div class="cap-img">
            <img :src="item.img" :alt="item.title" loading="lazy" />
          </div>
          <div class="cap-body">
            <span class="cap-num">{{
              String(index + 1).padStart(2, "0")
            }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section id="flow" class="sec" ref="flowRef">
      <div class="sec-label" :class="{ in: inView.flow }">
        <span>02</span> 业务流程
      </div>
      <h2 class="sec-h2" :class="{ in: inView.flow }">
        <span
          v-for="(char, index) in splitChars('制造业务闭环')"
          :key="index"
          class="hc"
          :style="{ transitionDelay: `${index * 0.04}s` }"
          :class="{ in: inView.flow }"
          >{{ char }}</span
        >
      </h2>
      <div class="flow-row" :class="{ in: inView.flow }">
        <div
          v-for="(item, index) in flow"
          :key="item.title"
          class="flow-item"
          :style="{ transitionDelay: `${0.1 + index * 0.1}s` }"
        >
          <div class="flow-img">
            <img :src="item.img" :alt="item.title" loading="lazy" />
          </div>
          <span class="flow-phase">Phase {{ index + 1 }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <section id="cta" class="cta" ref="ctaRef">
      <RouterLink class="btn-main" to="/login">进入控制台</RouterLink>
    </section>

    <footer class="intro-foot">
      <span>云枢智造 MES</span>
      <span class="foot-dot"></span>
      <span>Vue 3 · Spring Boot · Flyway</span>
    </footer>
  </main>
</template>

<script setup>
import {
  reactive,
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  defineAsyncComponent,
} from "vue";
import { RouterLink } from "vue-router";

const AbstractGeometry = defineAsyncComponent(
  () => import("@/components/AbstractGeometry.vue"),
);

const rootRef = ref(null);
const heroRef = ref(null);
const capRef = ref(null);
const flowRef = ref(null);
const ctaRef = ref(null);

const activeSection = ref(0);
const heroVisible = ref(false);
const scrolled = ref(false);
const phase = ref("loading");
const inView = reactive({ cap: false, flow: false });

const heroPhase = ref("title");
const typewriterText = ref("");
const typewriterLine1 = "为每一台风扇";
const typewriterLine2 = "建立数字档案";
const FULL_TITLE = typewriterLine1 + "\n" + typewriterLine2;

let typewriterTimer = null;

const cloverPositions = [
  {
    left: "calc(50vw + 560px)",
    top: "48vh",
    scale: 1.15,
    speed: 0.4,
    opacity: 1,
  },
  { left: "80px", top: "50vh", scale: 1.05, speed: 0.35, opacity: 1 },
  {
    left: "calc(100vw - 120px)",
    top: "140px",
    scale: 0.38,
    speed: 0.2,
    opacity: 0.45,
  },
  { left: "50vw", top: "-120px", scale: 0.3, speed: 0.15, opacity: 0 },
];

const cloverStyle = computed(() => {
  if (phase.value === "loading") {
    return {
      left: "50vw",
      top: "50vh",
      transform: "translate(-50%, -50%)",
      opacity: 1,
    };
  }
  const position = cloverPositions[activeSection.value];
  return {
    left: position.left,
    top: position.top,
    transform: "translate(-50%, -50%)",
    opacity: position.opacity,
  };
});

const cloverSpeed = computed(() => cloverPositions[activeSection.value].speed);
const cloverScale = computed(() => cloverPositions[activeSection.value].scale);

const dots = [
  { id: "hero", label: "首页" },
  { id: "cap", label: "能力" },
  { id: "flow", label: "流程" },
  { id: "cta", label: "开始" },
];

const sectionRefs = computed(() => [
  heroRef.value,
  capRef.value,
  flowRef.value,
  ctaRef.value,
]);

const capabilities = [
  {
    title: "计划与排程",
    desc: "客户订单接入、生产工单创建、齐套分析与派工任务下达，打通需求到产线。",
    img: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&q=80",
  },
  {
    title: "生产执行",
    desc: "工位扫码接单、电子 SOP 指引、物料批次绑定与实时报工，全生命周期追踪。",
    img: "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=600&q=80",
  },
  {
    title: "质量管控",
    desc: "首末件检验、过程巡检、成品抽检、不良记录与返修闭环，确保出厂品质。",
    img: "https://images.unsplash.com/photo-1581092335871-4c7b80efbdd4?w=600&q=80",
  },
  {
    title: "设备与安灯",
    desc: "设备台账、点检保养、故障报修与 OEE 分析，安灯事件全链路闭环。",
    img: "https://images.unsplash.com/photo-1537462715879-360eeb61a0ad?w=600&q=80",
  },
  {
    title: "全程追溯",
    desc: "以产品 SN 串联物料批次、工序记录与质检数据，构建完整数字档案。",
    img: "https://images.unsplash.com/photo-1563986768609-322da13575f2?w=600&q=80",
  },
  {
    title: "智能 Agent",
    desc: "大模型驱动的知识问答、风险预警与决策辅助，实时智能协同。",
    img: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=600&q=80",
  },
];

const flow = [
  {
    title: "计划建模",
    desc: "订单、BOM、工艺路线与工单任务形成生产主数据。",
    img: "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=500&q=80",
  },
  {
    title: "现场执行",
    desc: "扫码、SOP、物料绑定、报工驱动实时产线数据沉淀。",
    img: "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?w=500&q=80",
  },
  {
    title: "质量闭环",
    desc: "检验、不良、返修、放行在同一链路中完整记录。",
    img: "https://images.unsplash.com/photo-1581092335871-4c7b80efbdd4?w=500&q=80",
  },
  {
    title: "智能协同",
    desc: "Agent 分析风险、检索知识库、辅助班组高效决策。",
    img: "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=500&q=80",
  },
];

let observers = [];
let scrollTicking = false;

function splitChars(text) {
  return text.split("");
}

function setupObserver(element, key, threshold = 0.1) {
  if (!element) {
    return;
  }
  const observer = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting) {
        inView[key] = true;
        observer.unobserve(entry.target);
      }
    },
    { threshold },
  );
  observer.observe(element);
  observers.push(observer);
}

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
}

function startTypewriterSequence() {
  typewriterText.value = FULL_TITLE;
  heroPhase.value = "reading";

  typewriterTimer = setTimeout(() => {
    heroPhase.value = "backspacing";
    let text = FULL_TITLE;
    const backspaceTimer = setInterval(() => {
      text = text.slice(0, -1);
      typewriterText.value = text;
      if (text.length === 0) {
        clearInterval(backspaceTimer);
        typewriterText.value = "";
        heroPhase.value = "brand";
      }
    }, 35);
  }, 1200);
}

function clearTypewriterTimers() {
  if (typewriterTimer) {
    clearTimeout(typewriterTimer);
    typewriterTimer = null;
  }
}

function updateActive() {
  if (scrollTicking) {
    return;
  }
  scrollTicking = true;
  requestAnimationFrame(() => {
    const sections = sectionRefs.value;
    const viewportHeight = window.innerHeight;
    scrolled.value = window.scrollY > 40;
    let best = 0;
    let bestIndex = 0;
    for (let index = 0; index < sections.length; index += 1) {
      const element = sections[index];
      if (!element) {
        continue;
      }
      const rect = element.getBoundingClientRect();
      const visible =
        Math.min(rect.bottom, viewportHeight) - Math.max(rect.top, 0);
      if (visible > best) {
        best = visible;
        bestIndex = index;
      }
    }
    activeSection.value = bestIndex;
    scrollTicking = false;
  });
}

onMounted(() => {
  window.addEventListener("scroll", updateActive, { passive: true });

  setTimeout(() => {
    phase.value = "ready";
  }, 1800);
  setTimeout(() => {
    heroVisible.value = true;
    typewriterTimer = setTimeout(() => {
      startTypewriterSequence();
    }, 1800);
  }, 2600);

  setupObserver(capRef.value, "cap", 0.08);
  setupObserver(flowRef.value, "flow", 0.08);
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", updateActive);
  observers.forEach((observer) => observer.disconnect());
  clearTypewriterTimers();
});
</script>

<style scoped>
.intro {
  background: #fff;
  color: #1a1a1a;
  font-family: "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
  overflow-x: hidden;
}

.intro::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url("https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=1600&q=20")
    center / cover no-repeat;
  opacity: 0.06;
}

.intro-screen {
  position: fixed;
  inset: 0;
  z-index: 12;
  pointer-events: none;
  background: radial-gradient(
    ellipse at center,
    rgba(10, 10, 10, 0.7) 0%,
    #0a0a0a 60%
  );
}

.intro-screen.off {
  opacity: 0;
  transition: opacity 1.2s cubic-bezier(0.33, 0, 0.1, 1);
}

.clover-float {
  position: fixed;
  width: 1200px;
  height: 1200px;
  pointer-events: none;
  transition:
    left 1.2s cubic-bezier(0.33, 0, 0.1, 1),
    top 1.2s cubic-bezier(0.33, 0, 0.1, 1),
    transform 1.2s cubic-bezier(0.33, 0, 0.1, 1),
    opacity 0.7s ease;
  will-change: left, top, transform;
}

.intro-bar {
  position: fixed;
  inset: 0 0 auto;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  min-height: 64px;
  transition:
    background 0.3s,
    box-shadow 0.3s;
}

.intro-bar.on {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.06);
}

.bar-logo {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #1a1a1a;
}

.bar-logo span {
  margin-left: 4px;
  font-weight: 400;
  color: #999;
}

.bar-nav {
  display: flex;
  gap: 4px;
}

.bar-nav a {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  transition:
    color 0.15s,
    background 0.15s;
}

.bar-nav a:hover {
  color: #1a1a1a;
  background: rgba(0, 0, 0, 0.04);
}

.bar-cta {
  padding: 8px 20px;
  border-radius: 6px;
  background: #1a1a1a;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  transition: background 0.2s;
}

.bar-cta:hover {
  background: #333;
}

.nav-dots {
  position: fixed;
  right: 28px;
  top: 50%;
  z-index: 195;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 18px;
}

.nav-dot {
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 4px 0;
  opacity: 0.35;
  transition: opacity 0.3s;
}

.nav-dot:hover,
.nav-dot.active {
  opacity: 1;
}

.dot-pip {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a1a1a;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.nav-dot.active .dot-pip {
  transform: scale(1.5);
}

.nav-dot:not(.active) .dot-pip {
  background: rgba(0, 0, 0, 0.25);
}

.nav-dot:hover .dot-pip {
  transform: scale(1.3);
}

.dot-label {
  font-size: 11px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.04em;
  white-space: nowrap;
  opacity: 0;
  transform: translateX(4px);
  transition:
    opacity 0.2s,
    transform 0.2s;
}

.nav-dot:hover .dot-label,
.nav-dot.active .dot-label {
  opacity: 1;
  transform: translateX(0);
}

.hero {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100vh;
  padding: 0 48px;
}

.hero-text {
  position: relative;
  z-index: 60;
  flex: 0 0 42%;
  padding: 100px 0 60px;
}

.hero-text-compact {
  flex: 0 0 46%;
}

.hero-tag {
  margin: 0 0 16px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: rgba(0, 0, 0, 0.45);
  text-transform: uppercase;
}

.hero-h1 {
  margin: 0 0 24px;
  font-size: clamp(52px, 6.5vw, 88px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.015em;
  color: #1a1a1a;
  white-space: nowrap;
}

.hero-typewriter-stage {
  margin: 0 0 24px;
  min-height: 200px;
}

.hero-typewriter {
  font-size: clamp(52px, 6.5vw, 88px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.015em;
  color: #1a1a1a;
  white-space: nowrap;
}

.tw-char {
  display: inline;
}

.tw-char.accent {
  color: transparent;
  -webkit-text-stroke: 1.5px #1a1a1a;
}

.tw-cursor {
  display: inline;
  animation: twBlink 0.7s step-end infinite;
  color: #1a1a1a;
}

@keyframes twBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.hero-enter-wrap {
  display: inline-flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 80px;
  color: #1a1a1a;
  text-decoration: none;
  animation: btnReveal 0.6s ease-out;
  cursor: pointer;
}

@keyframes btnReveal {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-enter-sub {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.16em;
  color: rgba(0, 0, 0, 0.4);
  white-space: nowrap;
  animation: btnReveal 0.6s 0.1s ease-out both;
}

.hero-enter-text {
  display: inline-block;
  padding: 24px 44px;
  font-size: clamp(52px, 6.5vw, 88px);
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.2;
  color: #fff;
  background: #1a1a1a;
  border-radius: 8px;
  white-space: nowrap;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-enter-wrap:hover .hero-enter-text {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.18);
}

.hero-enter-hint {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 24px;
}

.enter-line {
  width: 0;
  height: 2px;
  background: #1a1a1a;
  transition: width 0.4s ease;
}

.hero-enter-wrap:hover .enter-line {
  width: 52px;
}

.enter-arrow {
  font-size: 22px;
  line-height: 1;
  opacity: 0;
  transform: translateX(-12px);
  transition: opacity 0.5s ease, transform 0.5s ease;
  animation: arrowIn 0.6s 0.4s ease-out forwards;
}

.hero-enter-wrap:hover .enter-arrow {
  transform: translateX(6px);
}

@keyframes arrowIn {
  to { opacity: 1; transform: translateX(0); }
}

.hero-brand-final {
  margin: 80px 0 0;
  animation: btnReveal 0.8s ease-out;
}

.brand-final-tag {
  margin: 0 0 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.14em;
  color: rgba(0, 0, 0, 0.35);
  text-transform: uppercase;
}

.brand-final-title {
  margin: 0;
  font-size: clamp(52px, 6.5vw, 88px);
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.2;
  color: #1a1a1a;
  white-space: nowrap;
}

.brand-char {
  display: inline-block;
  opacity: 0;
  transform: translateY(20px);
  animation: brandCharIn 0.5s ease-out forwards;
}

.brand-char.accent {
  color: transparent;
  -webkit-text-stroke: 1.5px #1a1a1a;
}

.brand-gap {
  display: inline-block;
  width: 0.4em;
}

@keyframes brandCharIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-visual {
  flex: 0 0 54%;
  min-height: 100vh;
}

.hc {
  display: inline-block;
  opacity: 0;
  transform: translateY(40px);
  transition:
    opacity 0.5s ease,
    transform 0.5s ease;
}

.hc.in {
  opacity: 1;
  transform: translateY(0);
}

.hc.accent {
  color: transparent;
  -webkit-text-stroke: 1.5px #1a1a1a;
}

.btn-main {
  display: inline-flex;
  align-items: center;
  min-height: 48px;
  padding: 0 28px;
  border-radius: 6px;
  background: #1a1a1a;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  transition: background 0.2s;
}

.btn-main:hover {
  background: #333;
}

.btn-sub {
  display: inline-flex;
  align-items: center;
  min-height: 48px;
  padding: 0 28px;
  border-radius: 6px;
  border: 1.5px solid rgba(0, 0, 0, 0.15);
  color: #1a1a1a;
  font-weight: 600;
  font-size: 15px;
  transition:
    border-color 0.2s,
    background 0.2s;
}

.btn-sub:hover {
  border-color: rgba(0, 0, 0, 0.35);
  background: rgba(0, 0, 0, 0.03);
}

.sec {
  position: relative;
  z-index: 60;
  padding: 120px 48px;
  max-width: 1200px;
  margin: 0 auto;
}

.sec-cap {
  padding-left: calc(48px + 260px);
}

.sec-label {
  margin-bottom: 24px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.35);
  opacity: 0;
  transform: translateY(16px);
  transition:
    opacity 0.6s,
    transform 0.6s;
}

.sec-label.in {
  opacity: 1;
  transform: translateY(0);
}

.sec-label span {
  display: inline-block;
  margin-right: 8px;
  padding: 2px 8px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.5);
  font-size: 11px;
  font-weight: 700;
}

.sec-h2 {
  margin: 0 0 56px;
  font-size: clamp(36px, 6vw, 64px);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #1a1a1a;
}

.cap-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.cap-grid.in .cap-card {
  opacity: 1;
  transform: translateY(0);
}

.cap-card {
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  opacity: 0;
  transform: translateY(28px);
  transition:
    opacity 0.6s ease,
    transform 0.6s ease,
    box-shadow 0.2s;
}

.cap-card:hover {
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.cap-img {
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: #f5f5f5;
}

.cap-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.cap-card:hover .cap-img img {
  transform: scale(1.04);
}

.cap-body {
  padding: 24px 22px 28px;
}

.cap-num {
  display: block;
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.2);
  font-variant-numeric: tabular-nums;
}

.cap-body h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.cap-body p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.5);
}

.flow-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.flow-row.in .flow-item {
  opacity: 1;
  transform: translateY(0);
}

.flow-item {
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  opacity: 0;
  transform: translateY(28px);
  transition:
    opacity 0.6s ease,
    transform 0.6s ease,
    box-shadow 0.2s;
}

.flow-item:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.flow-img {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: #f5f5f5;
}

.flow-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.flow-item:hover .flow-img img {
  transform: scale(1.04);
}

.flow-phase {
  display: block;
  padding: 18px 18px 0;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: rgba(0, 0, 0, 0.3);
  text-transform: uppercase;
}

.flow-item h3 {
  margin: 8px 18px;
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
}

.flow-item p {
  margin: 0 18px 20px;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.5);
}

.cta {
  position: relative;
  z-index: 10;
  padding: 100px 48px;
  text-align: center;
}

.cta h2 {
  margin: 0 0 14px;
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
}

.cta p {
  margin: 0 0 28px;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.5);
}

.cta .btn-main {
  font-size: 15px;
  padding: 0 32px;
}

.intro-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 48px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 13px;
  color: rgba(0, 0, 0, 0.35);
}

.foot-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
}

@media (max-width: 860px) {
  .intro-screen,
  .clover-float {
    display: none;
  }

  .nav-dots {
    display: none;
  }

  .intro-bar {
    padding: 0 20px;
  }

  .bar-nav {
    display: none;
  }

  .hero {
    flex-direction: column;
    min-height: auto;
    padding: 0 20px;
    max-width: 100%;
  }

  .hero-text {
    flex: none;
    padding: 100px 0 30px;
    max-width: 100%;
  }

  .hero-visual {
    flex: none;
    height: 50vh;
  }

  .hero-h1 {
    font-size: clamp(36px, 10vw, 56px);
    white-space: normal;
  }

  .hero-typewriter {
    font-size: clamp(36px, 10vw, 56px);
  }

  .hero-enter-text,
  .brand-final-title {
    font-size: clamp(36px, 10vw, 56px);
  }

  .hero-enter-wrap {
    padding: 16px 24px;
  }

  .sec {
    padding: 80px 20px;
  }

  .sec-cap {
    padding-left: 20px;
  }

  .cap-grid {
    grid-template-columns: 1fr;
  }

  .flow-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 540px) {
  .flow-row {
    grid-template-columns: 1fr;
  }

  .hero-enter-text,
  .brand-final-title {
    font-size: 32px;
  }

  .btn-main,
  .btn-sub {
    justify-content: center;
  }

  .cta {
    padding: 60px 20px;
  }
}
</style>
