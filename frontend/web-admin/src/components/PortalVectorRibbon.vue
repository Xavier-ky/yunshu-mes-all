<template>
  <div class="vector-ribbon-layer" aria-hidden="true">
    <canvas ref="canvasRef" class="vector-ribbon-canvas"></canvas>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from "vue";

const canvasRef = ref(null);
const TAU = Math.PI * 2;

const CONFIG = {
  maxDpr: 1.2,

  bodyDesktop: 420,
  bodyTablet: 300,
  bodyMobile: 180,

  flowDesktop: 220,
  flowTablet: 160,
  flowMobile: 90,

  filamentDesktop: 360,
  filamentTablet: 250,
  filamentMobile: 150,

  edgeDesktop: 260,
  edgeTablet: 180,
  edgeMobile: 110,

  sparkDesktop: 90,
  sparkTablet: 60,
  sparkMobile: 36,

  startDesktop: 45,
  startTablet: 32,
  startMobile: 20,
};

let ctx = null;
let rafId = 0;
let lastTime = 0;

let glowSprite = null;
let coreSprite = null;
let dustSprite = null;
let sparkSprite = null;

let pathPoints = [];

let bodyParticles = [];
let flowParticles = [];
let filamentParticles = [];
let edgeParticles = [];
let sparks = [];
let startScatterParticles = [];

const state = {
  width: 0,
  height: 0,
  dpr: 1,
  time: 0,
  running: true,
};

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function lerp(a, b, t) {
  return a + (b - a) * t;
}

function pickByWidth(width, desktop, tablet, mobile) {
  if (width < 768) return mobile;
  if (width < 1280) return tablet;
  return desktop;
}

function createSprite(size, stops) {
  const c = document.createElement("canvas");
  c.width = size;
  c.height = size;
  const cctx = c.getContext("2d");
  const r = size / 2;

  const g = cctx.createRadialGradient(r, r, 0, r, r, r);
  stops.forEach(([stop, color]) => g.addColorStop(stop, color));

  cctx.fillStyle = g;
  cctx.beginPath();
  cctx.arc(r, r, r, 0, TAU);
  cctx.fill();

  return c;
}

function createSprites() {
  glowSprite = createSprite(144, [
    [0, "rgba(165,250,255,0.18)"],
    [0.14, "rgba(94,238,255,0.12)"],
    [0.34, "rgba(58,222,255,0.07)"],
    [0.62, "rgba(48,214,255,0.028)"],
    [1, "rgba(48,214,255,0)"],
  ]);

  coreSprite = createSprite(72, [
    [0, "rgba(252,255,255,0.98)"],
    [0.06, "rgba(232,255,255,0.97)"],
    [0.16, "rgba(170,250,255,0.92)"],
    [0.28, "rgba(96,238,255,0.72)"],
    [0.48, "rgba(56,226,255,0.26)"],
    [0.72, "rgba(56,226,255,0.06)"],
    [1, "rgba(56,226,255,0)"],
  ]);

  dustSprite = createSprite(64, [
    [0, "rgba(248,255,255,0.98)"],
    [0.08, "rgba(214,254,255,0.94)"],
    [0.18, "rgba(136,246,255,0.82)"],
    [0.34, "rgba(92,234,255,0.44)"],
    [0.58, "rgba(92,234,255,0.12)"],
    [1, "rgba(92,234,255,0)"],
  ]);

  sparkSprite = createSprite(84, [
    [0, "rgba(255,255,255,1)"],
    [0.06, "rgba(236,255,255,0.98)"],
    [0.16, "rgba(174,250,255,0.9)"],
    [0.28, "rgba(112,238,255,0.56)"],
    [0.5, "rgba(112,238,255,0.14)"],
    [1, "rgba(112,238,255,0)"],
  ]);
}

function buildPathPoints() {
  const w = state.width;
  const h = state.height;

  const raw = [
    { x: -0.1 * w, y: 0.948 * h },
    { x: 0.08 * w, y: 1.0 * h },
    { x: 0.34 * w, y: 0.915 * h },
    { x: 0.7 * w, y: 0.668 * h },
    { x: 0.92 * w, y: 0.266 * h },
    { x: 0.5 * w, y: 0.15 * h },
  ];

  pathPoints = [raw[0], ...raw, raw[raw.length - 1]];
}

function catmullRomPoint(p0, p1, p2, p3, t) {
  const t2 = t * t;
  const t3 = t2 * t;

  return {
    x:
      0.5 *
      (2 * p1.x +
        (-p0.x + p2.x) * t +
        (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
        (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
    y:
      0.5 *
      (2 * p1.y +
        (-p0.y + p2.y) * t +
        (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
        (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
  };
}

function sampleRibbonPos(t) {
  const segCount = pathPoints.length - 3;
  const tt = clamp(t, 0, 0.999999);
  const scaled = tt * segCount;
  const seg = Math.min(segCount - 1, Math.floor(scaled));
  const u = scaled - seg;

  return catmullRomPoint(
    pathPoints[seg],
    pathPoints[seg + 1],
    pathPoints[seg + 2],
    pathPoints[seg + 3],
    u,
  );
}

function sampleRibbon(t) {
  const p = sampleRibbonPos(t);
  const pPrev = sampleRibbonPos(clamp(t - 0.002, 0, 0.999999));
  const pNext = sampleRibbonPos(clamp(t + 0.002, 0, 0.999999));

  const dx = pNext.x - pPrev.x;
  const dy = pNext.y - pPrev.y;
  const len = Math.hypot(dx, dy) || 1;

  const tx = dx / len;
  const ty = dy / len;
  const nx = -ty;
  const ny = tx;

  return { x: p.x, y: p.y, tx, ty, nx, ny };
}

function ribbonWidth(t) {
  const k = 1 - t;
  const base = 268 * Math.pow(k, 1.9) + 18;
  const startBoost = 52 * Math.exp(-Math.pow((t - 0.08) / 0.1, 2));
  const shoulder = 48 * Math.exp(-Math.pow((t - 0.2) / 0.16, 2));
  const mid = 46 * Math.exp(-Math.pow((t - 0.36) / 0.28, 2));
  return base + startBoost + shoulder + mid;
}

function frontSuppression(t) {
  return 0.08 + 0.92 * clamp((t - 0.06) / 0.3, 0, 1);
}

function drawSprite(sprite, x, y, size, alpha = 1) {
  ctx.globalAlpha = alpha;
  ctx.drawImage(sprite, x - size / 2, y - size / 2, size, size);
}

function drawSharpParticle(x, y, size, alpha = 1) {
  const outer = size * 0.5;
  const inner = size * 0.19;

  ctx.globalAlpha = alpha * 0.95;
  ctx.fillStyle = "rgba(110,242,255,0.92)";
  ctx.beginPath();
  ctx.arc(x, y, outer, 0, TAU);
  ctx.fill();

  ctx.globalAlpha = alpha;
  ctx.fillStyle = "rgba(252,255,255,0.98)";
  ctx.beginPath();
  ctx.arc(x, y, inner, 0, TAU);
  ctx.fill();
}

function resetSystems() {
  const bodyCount = pickByWidth(
    state.width,
    CONFIG.bodyDesktop,
    CONFIG.bodyTablet,
    CONFIG.bodyMobile,
  );
  const flowCount = pickByWidth(
    state.width,
    CONFIG.flowDesktop,
    CONFIG.flowTablet,
    CONFIG.flowMobile,
  );
  const filamentCount = pickByWidth(
    state.width,
    CONFIG.filamentDesktop,
    CONFIG.filamentTablet,
    CONFIG.filamentMobile,
  );
  const edgeCount = pickByWidth(
    state.width,
    CONFIG.edgeDesktop,
    CONFIG.edgeTablet,
    CONFIG.edgeMobile,
  );
  const sparkCount = pickByWidth(
    state.width,
    CONFIG.sparkDesktop,
    CONFIG.sparkTablet,
    CONFIG.sparkMobile,
  );
  const startCount = pickByWidth(
    state.width,
    CONFIG.startDesktop,
    CONFIG.startTablet,
    CONFIG.startMobile,
  );

  bodyParticles = Array.from({ length: bodyCount }, () => {
    const side = Math.random() < 0.5 ? -1 : 1;
    const big = Math.random();
    return {
      baseT: 0.06 + Math.pow(Math.random(), 0.98) * 0.93,
      lane: side * (0.12 + Math.pow(Math.random(), 1.16) * 1.3),
      phase: Math.random() * TAU,
      phase2: Math.random() * TAU,
      tAmp: 0.001 + Math.random() * 0.0028,
      tFreq: 0.04 + Math.random() * 0.07,
      nAmp: 0.003 + Math.random() * 0.015,
      nFreq: 0.15 + Math.random() * 0.46,
      size: big < 0.18 ? 2.8 + Math.random() * 4.2 : 0.95 + Math.random() * 2.9,
      alpha: 0.08 + Math.random() * 0.12,
      glow: Math.random() < 0.18,
      crisp: Math.random() < 0.84,
    };
  });

  flowParticles = Array.from({ length: flowCount }, () => {
    const side = Math.random() < 0.5 ? -1 : 1;
    const large = Math.random();
    return {
      seed: Math.random(),
      speed: 0.004 + Math.random() * 0.005,
      lane: side * (0.22 + Math.pow(Math.random(), 1.06) * 1.45),
      waveAmp: 0.004 + Math.random() * 0.012,
      waveFreq: 0.2 + Math.random() * 0.42,
      phase: Math.random() * TAU,
      size:
        large < 0.18 ? 2.8 + Math.random() * 3.4 : 0.95 + Math.random() * 2.4,
      alpha: 0.085 + Math.random() * 0.11,
      crisp: Math.random() < 0.88,
    };
  });

  filamentParticles = Array.from({ length: filamentCount }, () => {
    const outerSide = Math.random() < 0.72 ? 1 : -1;
    const large = Math.random();
    return {
      seed: Math.random(),
      speed: 0.0021 + Math.random() * 0.0036,
      side: outerSide,
      spread: 1.35 + Math.random() * 2.0,
      back: 12 + Math.random() * 52,
      phase: Math.random() * TAU,
      driftAmp: 2 + Math.random() * 12,
      driftFreq: 0.1 + Math.random() * 0.28,
      size:
        large < 0.1 ? 2.2 + Math.random() * 2.2 : 0.65 + Math.random() * 1.7,
      alpha: 0.06 + Math.random() * 0.09,
      crisp: Math.random() < 0.9,
    };
  });

  edgeParticles = Array.from({ length: edgeCount }, () => {
    const outerSide = Math.random() < 0.8 ? 1 : -1;
    const large = Math.random();
    return {
      seed: Math.random(),
      speed: 0.0016 + Math.random() * 0.0032,
      side: outerSide,
      spread: 1.9 + Math.random() * 3.5,
      back: 18 + Math.random() * 70,
      phase: Math.random() * TAU,
      driftAmp: 4 + Math.random() * 18,
      driftFreq: 0.08 + Math.random() * 0.22,
      size:
        large < 0.08 ? 2.2 + Math.random() * 2.0 : 0.65 + Math.random() * 1.9,
      alpha: 0.045 + Math.random() * 0.075,
      crisp: Math.random() < 0.92,
    };
  });

  sparks = Array.from({ length: sparkCount }, () => ({
    seed: Math.random(),
    speed: 0.0025 + Math.random() * 0.0035,
    lane: (Math.random() - 0.5) * 1.6,
    phase: Math.random() * TAU,
    blink: 0.35 + Math.random() * 0.55,
    size: 1 + Math.random() * 2.8,
    alpha: 0.06 + Math.random() * 0.1,
  }));

  startScatterParticles = Array.from({ length: startCount }, () => {
    const side = Math.random() < 0.5 ? -1 : 1;
    const large = Math.random();
    return {
      baseT: 0.07 + Math.pow(Math.random(), 1.35) * 0.11,
      side,
      normalDist: 48 + Math.random() * 260,
      tangentBack: 36 + Math.random() * 290,
      phase: Math.random() * TAU,
      phase2: Math.random() * TAU,
      freq1: 0.08 + Math.random() * 0.16,
      freq2: 0.12 + Math.random() * 0.2,
      driftX: 0.8 + Math.random() * 7,
      driftY: 0.8 + Math.random() * 8,
      size:
        large < 0.16 ? 2.8 + Math.random() * 3.2 : 0.8 + Math.random() * 2.2,
      alpha: 0.04 + Math.random() * 0.06,
      glow: Math.random() < 0.08,
      crisp: Math.random() < 0.9,
    };
  });
}

function resizeCanvas() {
  const canvas = canvasRef.value;
  if (!canvas) return;

  const rect = canvas.getBoundingClientRect();
  state.dpr = Math.min(window.devicePixelRatio || 1, CONFIG.maxDpr);
  state.width = rect.width;
  state.height = rect.height;

  canvas.width = Math.round(rect.width * state.dpr);
  canvas.height = Math.round(rect.height * state.dpr);

  ctx = canvas.getContext("2d");
  if (!ctx) return;

  ctx.setTransform(state.dpr, 0, 0, state.dpr, 0, 0);

  createSprites();
  buildPathPoints();
  resetSystems();
}

function drawMainRibbonCore() {
  const steps = 148;

  for (let i = 0; i <= steps; i += 1) {
    const t = i / steps;
    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const fade = lerp(1, 0.24, t);
    const front = frontSuppression(t);

    drawSprite(glowSprite, p.x, p.y, w * 2.8, 0.026 * fade * front);
    drawSprite(dustSprite, p.x, p.y, w * 1.95, 0.038 * fade * front);
    drawSprite(coreSprite, p.x, p.y, w * 0.52, 0.048 * fade * front);

    if (t < 0.42) {
      const shoulder = w * 0.34;
      drawSprite(
        dustSprite,
        p.x + p.nx * shoulder,
        p.y + p.ny * shoulder,
        w * 1.08,
        0.016 * fade * front,
      );
      drawSprite(
        dustSprite,
        p.x - p.nx * shoulder,
        p.y - p.ny * shoulder,
        w * 1.08,
        0.016 * fade * front,
      );
    }
  }
}

function drawRibbonStrands() {
  const strandDefs = [
    {
      lane: -0.58,
      thickness: 0.16,
      alpha: 0.028,
      drift: 0.07,
      freq: 0.42,
      phase: 0.6,
    },
    {
      lane: -0.38,
      thickness: 0.18,
      alpha: 0.034,
      drift: 0.064,
      freq: 0.36,
      phase: 2.1,
    },
    {
      lane: -0.16,
      thickness: 0.2,
      alpha: 0.04,
      drift: 0.05,
      freq: 0.32,
      phase: 1.4,
    },
    {
      lane: 0.16,
      thickness: 0.2,
      alpha: 0.04,
      drift: 0.05,
      freq: 0.34,
      phase: 4.2,
    },
    {
      lane: 0.38,
      thickness: 0.18,
      alpha: 0.034,
      drift: 0.064,
      freq: 0.38,
      phase: 5.1,
    },
    {
      lane: 0.58,
      thickness: 0.16,
      alpha: 0.028,
      drift: 0.07,
      freq: 0.44,
      phase: 3.3,
    },
  ];

  const steps = 100;

  for (const strand of strandDefs) {
    for (let i = 0; i <= steps; i += 1) {
      const t = i / steps;
      const p = sampleRibbon(t);
      const w = ribbonWidth(t);
      const front = frontSuppression(t);
      const wave =
        Math.sin(state.time * strand.freq + t * 7.1 + strand.phase) *
        strand.drift;
      const lateral = w * (strand.lane * lerp(1.14, 0.34, t) + wave);
      const x = p.x + p.nx * lateral;
      const y = p.y + p.ny * lateral;
      const fade = lerp(1, 0.24, t);
      const size = w * strand.thickness * lerp(1.18, 0.58, t);

      drawSprite(dustSprite, x, y, size * 2.0, strand.alpha * fade * front);
      drawSprite(
        coreSprite,
        x,
        y,
        size * 0.8,
        strand.alpha * 0.7 * fade * front,
      );
    }
  }
}

function drawStartScatter() {
  for (const item of startScatterParticles) {
    const t = clamp(
      item.baseT +
        Math.sin(state.time * item.freq1 + item.phase) * 0.003 +
        Math.sin(state.time * item.freq2 + item.phase2) * 0.002,
      0.055,
      0.18,
    );

    const p = sampleRibbon(t);
    const front = frontSuppression(t) * 0.5;

    const x =
      p.x +
      p.nx * item.normalDist * item.side -
      p.tx * item.tangentBack +
      Math.sin(state.time * item.freq2 + item.phase) * item.driftX;

    const y =
      p.y +
      p.ny * item.normalDist * item.side -
      p.ty * item.tangentBack +
      Math.cos(state.time * item.freq1 + item.phase2) * item.driftY;

    const size = item.size * 2.1;
    const alpha = item.alpha * lerp(0.8, 0.54, t) * front;

    drawSprite(dustSprite, x, y, size, alpha);
    if (item.crisp) {
      drawSharpParticle(x, y, size * 0.44, alpha * 1.1);
    }
    if (item.glow) {
      drawSprite(glowSprite, x, y, size * 1.65, alpha * 0.14);
    }
  }
}

function drawBodyParticles() {
  for (const item of bodyParticles) {
    const t = clamp(
      item.baseT + Math.sin(state.time * item.tFreq + item.phase) * item.tAmp,
      0,
      0.995,
    );

    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const front = frontSuppression(t);

    const laneScale = lerp(1.14, 0.18, t);
    const flutter = Math.sin(state.time * item.nFreq + item.phase2) * item.nAmp;
    const lateral = w * (item.lane * laneScale + flutter);

    const x = p.x + p.nx * lateral;
    const y = p.y + p.ny * lateral;

    const size = item.size * (1.78 + (1 - t) * 1.35);
    let alpha = item.alpha * lerp(1, 0.3, t) * front;

    if (t < 0.16) alpha *= 0.24;

    drawSprite(dustSprite, x, y, size * 1.65, alpha);
    drawSprite(coreSprite, x, y, size * 0.92, alpha * 0.86);
    drawSharpParticle(x, y, size * 0.42, alpha * 1.15);

    if (item.glow) {
      drawSprite(glowSprite, x, y, size * 2.3, alpha * 0.12);
    }
  }
}

function drawFlowParticles() {
  for (const item of flowParticles) {
    const t = (item.seed + state.time * item.speed) % 1;
    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const front = frontSuppression(t);

    const wave =
      Math.sin(state.time * item.waveFreq + item.phase) * item.waveAmp;
    const lateral = w * (item.lane * lerp(1.18, 0.26, t) + wave);

    const x = p.x + p.nx * lateral;
    const y = p.y + p.ny * lateral;

    const size = item.size * (1.62 + (1 - t) * 1.15);
    let alpha = item.alpha * lerp(1, 0.22, t) * front;

    if (t < 0.16) alpha *= 0.28;

    drawSprite(coreSprite, x, y, size * 1.3, alpha);
    drawSharpParticle(x, y, size * 0.4, alpha * 1.2);
    drawSprite(glowSprite, x, y, size * 2.35, alpha * 0.1);
  }
}

function drawFilamentParticles() {
  for (const item of filamentParticles) {
    const t = (item.seed + state.time * item.speed) % 1;
    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const front = frontSuppression(t);

    const normalOffset =
      w * item.spread +
      Math.sin(state.time * item.driftFreq + item.phase) * item.driftAmp;
    const backOffset = item.back * (0.52 + (1 - t) * 1.04);

    const x = p.x + p.nx * normalOffset * item.side - p.tx * backOffset;
    const y = p.y + p.ny * normalOffset * item.side - p.ty * backOffset;

    const size = item.size * (1.18 + (1 - t) * 0.78);
    const alpha = item.alpha * lerp(1, 0.22, t) * front;

    drawSprite(dustSprite, x, y, size * 1.45, alpha);
    drawSprite(coreSprite, x, y, size * 0.68, alpha * 0.76);
    drawSharpParticle(x, y, size * 0.36, alpha * 1.22);
  }
}

function drawEdgeParticles() {
  for (const item of edgeParticles) {
    const t = (item.seed + state.time * item.speed) % 1;
    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const front = frontSuppression(t);

    const normalOffset =
      w * item.spread +
      22 +
      Math.sin(state.time * item.driftFreq + item.phase) * item.driftAmp;

    const backOffset = item.back * (0.52 + (1 - t) * 1.1);

    const x = p.x + p.nx * normalOffset * item.side - p.tx * backOffset;
    const y = p.y + p.ny * normalOffset * item.side - p.ty * backOffset;

    const size = item.size * (1.42 + (1 - t) * 0.95);
    const alpha = item.alpha * lerp(1, 0.24, t) * front;

    drawSprite(dustSprite, x, y, size * 1.46, alpha);
    drawSharpParticle(x, y, size * 0.34, alpha * 1.24);
  }
}

function drawSparks() {
  for (const item of sparks) {
    const t = (item.seed + state.time * item.speed) % 1;
    const p = sampleRibbon(t);
    const w = ribbonWidth(t);
    const front = frontSuppression(t);

    const lateral = w * item.lane * 0.94;
    const x = p.x + p.nx * lateral;
    const y = p.y + p.ny * lateral;

    const blink = 0.76 + 0.24 * Math.sin(state.time * item.blink + item.phase);
    const size = item.size * (1.65 + blink * 0.82);
    const alpha = item.alpha * blink * lerp(1, 0.24, t) * front;

    drawSprite(sparkSprite, x, y, size, alpha);
    drawSharpParticle(x, y, size * 0.16, alpha * 1.22);
  }
}

function renderFrame(ts) {
  if (!ctx || !state.running) return;

  if (!lastTime) lastTime = ts;
  const dt = Math.min((ts - lastTime) / 1000, 0.04);
  lastTime = ts;

  state.time += dt;

  ctx.clearRect(0, 0, state.width, state.height);
  ctx.globalCompositeOperation = "lighter";

  drawMainRibbonCore();
  drawRibbonStrands();
  drawStartScatter();
  drawEdgeParticles();
  drawFilamentParticles();
  drawBodyParticles();
  drawFlowParticles();
  drawSparks();

  ctx.globalAlpha = 1;
  rafId = requestAnimationFrame(renderFrame);
}

function start() {
  if (!ctx) resizeCanvas();
  state.running = true;
  cancelAnimationFrame(rafId);
  rafId = requestAnimationFrame(renderFrame);
}

function stop() {
  state.running = false;
  cancelAnimationFrame(rafId);
  rafId = 0;
}

function handleVisibilityChange() {
  if (document.hidden) {
    stop();
  } else {
    lastTime = 0;
    start();
  }
}

onMounted(() => {
  resizeCanvas();
  start();

  window.addEventListener("resize", resizeCanvas, { passive: true });
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onBeforeUnmount(() => {
  stop();
  window.removeEventListener("resize", resizeCanvas);
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});
</script>

<style scoped>
.vector-ribbon-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: auto;
  height: 100vh;
  height: 100svh;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;

  mask-image: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 1) 0%,
    rgba(0, 0, 0, 1) 76%,
    rgba(0, 0, 0, 0.82) 86%,
    rgba(0, 0, 0, 0.36) 94%,
    rgba(0, 0, 0, 0) 100%
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 1) 0%,
    rgba(0, 0, 0, 1) 76%,
    rgba(0, 0, 0, 0.82) 86%,
    rgba(0, 0, 0, 0.36) 94%,
    rgba(0, 0, 0, 0) 100%
  );
}

.vector-ribbon-layer::before {
  content: "";
  position: absolute;
  left: -10%;
  bottom: -18%;
  width: 40vw;
  height: 40vw;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(40, 220, 255, 0.07) 0%,
    rgba(40, 220, 255, 0.035) 32%,
    rgba(40, 220, 255, 0.015) 52%,
    rgba(40, 220, 255, 0) 78%
  );
  filter: blur(42px);
  opacity: 0.24;
}

.vector-ribbon-layer::after {
  content: "";
  position: absolute;
  right: -10%;
  top: 0%;
  width: 32vw;
  height: 16vw;
  border-radius: 999px;
  background: radial-gradient(
    ellipse at center,
    rgba(88, 238, 255, 0.065) 0%,
    rgba(88, 238, 255, 0.026) 44%,
    rgba(88, 238, 255, 0) 76%
  );
  filter: blur(34px);
  opacity: 0.16;
}

.vector-ribbon-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  mix-blend-mode: screen;
  opacity: 1;
}

@media (max-width: 1280px) {
  .vector-ribbon-layer::before {
    width: 44vw;
    height: 44vw;
    opacity: 0.2;
  }

  .vector-ribbon-layer::after {
    width: 36vw;
    height: 18vw;
    opacity: 0.13;
  }
}

@media (max-width: 980px) {
  .vector-ribbon-layer {
    height: 100vh;
    height: 100svh;

    mask-image: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 1) 0%,
      rgba(0, 0, 0, 1) 72%,
      rgba(0, 0, 0, 0.78) 84%,
      rgba(0, 0, 0, 0.3) 94%,
      rgba(0, 0, 0, 0) 100%
    );
    -webkit-mask-image: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 1) 0%,
      rgba(0, 0, 0, 1) 72%,
      rgba(0, 0, 0, 0.78) 84%,
      rgba(0, 0, 0, 0.3) 94%,
      rgba(0, 0, 0, 0) 100%
    );
  }

  .vector-ribbon-layer::before {
    width: 50vw;
    height: 50vw;
    opacity: 0.16;
  }

  .vector-ribbon-layer::after {
    width: 40vw;
    height: 20vw;
    opacity: 0.1;
  }

  .vector-ribbon-canvas {
    opacity: 0.98;
  }
}
</style>
