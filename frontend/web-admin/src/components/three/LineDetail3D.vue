<template>
  <div ref="root" class="ld3d-root">
    <div ref="canvasMount" class="ld3d-canvas"></div>

    <!-- Switch arrows -->
    <button class="ld3d-arrow ld3d-arrow--left" @click="emitSwitch(-1)" :disabled="animating">
      <ChevronLeft :size="28" :stroke-width="1.5" />
    </button>
    <button class="ld3d-arrow ld3d-arrow--right" @click="emitSwitch(1)" :disabled="animating">
      <ChevronRight :size="28" :stroke-width="1.5" />
    </button>

    <Teleport to="body">
      <div v-if="tooltip.visible" class="ld3d-tooltip" :class="{ 'ld3d-tooltip--dev': tooltip.kind === 'device' }" :style="{ transform: 'translate(' + tooltip.x + 'px,' + tooltip.y + 'px)' }">
        <template v-if="tooltip.kind === 'station'">
          <div class="tt-name">{{ tooltip.name }}</div>
          <div class="tt-row">
            <span class="tt-status">{{ tooltip.runLabel }}</span>
            <span v-if="tooltip.operator" class="tt-sep">|</span>
            <span v-if="tooltip.operator" class="tt-op">{{ tooltip.operator }}</span>
          </div>
        </template>
        <template v-else>
          <div class="tt-name">{{ tooltip.name }}</div>
          <span class="tt-hint">设备</span>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, reactive, ref, watch } from "vue"
import * as THREE from "three"
import { useThreeScene } from "@/composables/useThreeScene"
import { ChevronLeft, ChevronRight } from "lucide-vue-next"

const props = defineProps({
  line:    { type: Object, default: null },
  stations:  { type: Array,  default: () => [] },
  workOrders:{ type: Array,  default: () => [] },
  dispatches:{ type: Array,  default: () => [] },
  andons:    { type: Array,  default: () => [] },
})
const emit = defineEmits(["navigate", "switch-line"])

const root = ref(null)
const canvasMount = ref(null)
const animating = ref(false)
const tooltip = reactive({ visible: false, kind: "station", name: "", runLabel: "", operator: "", x: 0, y: 0 })

const ST_SPACING = 2.2; const ROW_GAP = 1.8; const SEGMENTS = 3
const STATUS_COLORS = { RUNNING: 0x10b981, WARNING: 0xf59e0b, FAULT: 0xef4444, CHANGEOVER: 0x3b82f6, IDLE: 0x64748b }
const RUN_LABELS = { RUNNING: "运行", WARNING: "预警", FAULT: "故障", CHANGEOVER: "换型", IDLE: "待机" }

let scene, camera, renderer, controls
let stationGroups = [], deviceGroups = []
let allGroup = null    // parent group for the whole scene (pedestal + stations + devices) for switch animation
let switchAnimId = null

const three = useThreeScene({
  background: 0xe8ecf0, fov: 42,
  cameraPos: [0, 8, 4], cameraLookAt: [0, 0, 0],
  orbit: { enableRotate: false, enablePan: true, enableZoom: true, minDistance: 3, maxDistance: 22, maxPolarAngle: Math.PI / 6, minPolarAngle: 0, minAzimuthAngle: 0, maxAzimuthAngle: 30, target: new THREE.Vector3(0, 0, 0) },
})

function sLayout(i, N) {
  const perSeg = Math.ceil(N / SEGMENTS); const seg = Math.floor(i / perSeg); const posInSeg = i - seg * perSeg
  const countInSeg = Math.min(perSeg, N - seg * perSeg); const totalW = (countInSeg - 1) * ST_SPACING; const baseX = -totalW / 2
  const reversed = (seg === 1); const idx = reversed ? (countInSeg - 1 - posInSeg) : posInSeg
  return { x: baseX + idx * ST_SPACING, z: (seg - 1) * ROW_GAP, seg, countInSeg }
}

function sDimensions(N) { const perSeg = Math.ceil(N / SEGMENTS); return { maxW: (Math.min(perSeg, N) - 1) * ST_SPACING, totalZ: 2 * ROW_GAP } }

function setupLighting() {
  for (let i = scene.children.length - 1; i >= 0; i--) { if (scene.children[i].isLight) scene.remove(scene.children[i]) }
  scene.add(new THREE.AmbientLight(0xaaaaaa, 0.9))
  const key = new THREE.DirectionalLight(0xffffff, 1.4); key.position.set(4, 10, 5); scene.add(key)
  const fill = new THREE.DirectionalLight(0xc8d6e5, 0.55); fill.position.set(-3, 5, -3); scene.add(fill)
  scene.fog = new THREE.Fog(0xe8ecf0, 14, 28)
}

function meshAt(geo, mat, x, y, z) { const m = new THREE.Mesh(geo, mat); m.position.set(x, y, z); return m }

function buildStation(st, sIdx, N) {
  const { x, z } = sLayout(sIdx, N); const group = new THREE.Group(); group.position.set(x, 0.14, z)
  group.userData = { kind: "station", stationId: st.stationId, name: st.stationName, status: st.runStatus, operator: st.operatorName || "" }
  const color = STATUS_COLORS[st.runStatus] || STATUS_COLORS.IDLE
  group.add(meshAt(new THREE.BoxGeometry(1.6, 0.55, 1.0), new THREE.MeshStandardMaterial({ color, roughness: 0.35, metalness: 0.5, emissive: color, emissiveIntensity: 0.15 }), 0, 0.28, 0))
  group.add(meshAt(new THREE.BoxGeometry(1.6, 0.03, 0.12), new THREE.MeshStandardMaterial({ color: 0x374151, roughness: 0.3, metalness: 0.7 }), 0, 0.57, 0.44))
  group.add(meshAt(new THREE.BoxGeometry(0.4, 0.55, 1.01), new THREE.MeshStandardMaterial({ color, roughness: 0.25, metalness: 0.5, emissive: color, emissiveIntensity: 0.3 }), -0.6, 0.28, 0))
  group.add(meshAt(new THREE.BoxGeometry(ST_SPACING * 0.55, 0.05, 0.28), new THREE.MeshStandardMaterial({ color: 0xbcc3ca, roughness: 0.7, metalness: 0.3 }), 0, 0.03, 0))

  if (props.andons.some((a) => a.stationName === st.stationName)) {
    group.add(meshAt(new THREE.SphereGeometry(0.09, 16, 16), new THREE.MeshStandardMaterial({ color: 0xef4444, emissive: 0xef4444, emissiveIntensity: 0.85, roughness: 0.2 }), 0.65, 0.65, -0.1))
  }

  const outline = new THREE.Mesh(new THREE.BoxGeometry(1.7, 0.7, 1.1), new THREE.MeshBasicMaterial({ color: 0x3b82f6, transparent: true, opacity: 0, depthTest: false, depthWrite: false }))
  outline.position.y = 0.32; outline.renderOrder = 999; group.add(outline)

  if (st.deviceName) {
    const isFault = st.deviceStatus === "FAULT"; const dg = new THREE.Group(); dg.position.set(x, 0.14, z - 0.85)
    dg.userData = { kind: "device", name: st.deviceName, stationId: st.stationId }
    dg.add(meshAt(new THREE.BoxGeometry(0.9, 0.65, 0.5), new THREE.MeshStandardMaterial({ color: 0x94a3b8, roughness: 0.35, metalness: 0.75 }), 0, 0.33, 0))
    dg.add(meshAt(new THREE.BoxGeometry(0.5, 0.3, 0.02), new THREE.MeshStandardMaterial({ color: 0x1e293b, emissive: 0x38bdf8, emissiveIntensity: 0.35, roughness: 0.15 }), 0, 0.48, 0.26))
    dg.add(meshAt(new THREE.CylinderGeometry(0.025, 0.025, 0.22, 8), new THREE.MeshStandardMaterial({ color: 0x64748b, roughness: 0.3, metalness: 0.8 }), 0, 0.76, 0))
    const antColor = isFault ? 0xef4444 : 0x10b981
    dg.add(meshAt(new THREE.SphereGeometry(0.06, 12, 12), new THREE.MeshStandardMaterial({ color: antColor, emissive: antColor, emissiveIntensity: 0.9, roughness: 0.15 }), 0, 0.88, 0))
    allGroup.add(dg); deviceGroups.push(dg)
  }

  allGroup.add(group); return group
}

function highlightGroup(g) { g.scale.set(1.05, 1.0, 1.05); const o = g.children.find((c) => { try { return c.userData?.kind === "outline" } catch { return false }}); if (o) o.material.opacity = 0.2 }
function unhighlightGroup(g) { if (!g) return; g.scale.set(1.0, 1.0, 1.0); const o = g.children.find((c) => { try { return c.userData?.kind === "outline" } catch { return false }}); if (o) o.material.opacity = 0 }
function findGroup(hitList, kind) { for (const hit of hitList) { let g = hit.object; while (g) { if (g.userData?.kind === kind) return g; if (kind === "station" && g.userData?.stationId) return g; g = g.parent } } return null }

function showTooltip(ev, g) { tooltip.kind = g.userData.kind; tooltip.name = g.userData.name || ""; if (g.userData.kind === "station") { tooltip.runLabel = RUN_LABELS[g.userData.status] || g.userData.status; tooltip.operator = g.userData.operator || "" } tooltip.x = ev.clientX + 16; tooltip.y = ev.clientY - 12; tooltip.visible = true }
function moveTooltip(ev) { if (!tooltip.visible) return; tooltip.x = ev.clientX + 16; tooltip.y = ev.clientY - 12 }
function hideTooltip() { tooltip.visible = false }

let hoveredStation = null, hoveredDevice = null

function setupInteraction(raycaster, mouse) {
  renderer.domElement.addEventListener("click", (ev) => {
    if (animating.value) return
    const rect = renderer.domElement.getBoundingClientRect(); mouse.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1; mouse.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1; raycaster.setFromCamera(mouse, camera)
    const all = [...stationGroups.flatMap((g) => g.children), ...deviceGroups.flatMap((g) => g.children)]; const hits = raycaster.intersectObjects(all, true)
    if (findGroup(hits, "device")) { emit("navigate", "/app/equipment/devices"); return }
    if (findGroup(hits, "station")) { emit("navigate", "/app/my-work"); return }
  })
  renderer.domElement.addEventListener("mousemove", (ev) => {
    const rect = renderer.domElement.getBoundingClientRect(); mouse.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1; mouse.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1; raycaster.setFromCamera(mouse, camera)
    const all = [...stationGroups.flatMap((g) => g.children), ...deviceGroups.flatMap((g) => g.children)]; const hits = raycaster.intersectObjects(all, true)
    const hitSt = findGroup(hits, "station"), hitDev = findGroup(hits, "device")
    if (hitSt && hitSt !== hoveredStation) { if (hoveredStation) unhighlightGroup(hoveredStation); hoveredStation = hitSt; highlightGroup(hitSt); showTooltip(ev, hitSt) }
    else if (!hitSt && hoveredStation) { unhighlightGroup(hoveredStation); hoveredStation = null; if (!hitDev) hideTooltip() }
    if (hitDev && hitDev !== hoveredDevice) { if (hoveredDevice) unhighlightGroup(hoveredDevice); hoveredDevice = hitDev; highlightGroup(hitDev); showTooltip(ev, hitDev) }
    else if (!hitDev && hoveredDevice) { unhighlightGroup(hoveredDevice); hoveredDevice = null; if (!hitSt) hideTooltip() }
    moveTooltip(ev); renderer.domElement.style.cursor = (hitSt || hitDev) ? "pointer" : ""
  })
  renderer.domElement.addEventListener("mouseleave", () => { if (hoveredStation) { unhighlightGroup(hoveredStation); hoveredStation = null } if (hoveredDevice) { unhighlightGroup(hoveredDevice); hoveredDevice = null } hideTooltip(); renderer.domElement.style.cursor = "" })
}

// ---- Switch animation ----

function easeInOutCubic(t) { return t < 0.5 ? 4*t*t*t : 1 - Math.pow(-2*t+2,3)/2 }
function easeOutBack(t) { const c1=1.70158; return 1+(c1+1)*Math.pow(t-1,3)+c1*Math.pow(t-1,2) }

function cancelSwitchAnim() { if (switchAnimId) { cancelAnimationFrame(switchAnimId); switchAnimId = null } }

async function emitSwitch(dir) {
  if (animating.value) return
  animating.value = true; cancelSwitchAnim()

  // Sink current group down
  const oldGroup = allGroup
  const SW_DUR = 420
  await new Promise((resolve) => {
    const start = performance.now()
    function tick(now) { const t = Math.min(1, (now - start) / SW_DUR); const ease = easeInOutCubic(t)
      oldGroup.position.y = -5 * ease; oldGroup.scale.setScalar(1 - 0.15 * ease)
      if (t >= 1) { oldGroup.position.y = -5; resolve() } else { switchAnimId = requestAnimationFrame(tick) } }
    switchAnimId = requestAnimationFrame(tick)
  })
  scene.remove(oldGroup)

  emit("switch-line", dir)
}

// Watch for new line data → animate new model in
watch(() => props.line?.lineId, () => {
  if (!scene) return
  deviceGroups = []; stationGroups = []
  buildScene()
  // Rise animation
  allGroup.position.y = 4; allGroup.scale.setScalar(0.85)
  const SW_DUR = 420; const start = performance.now()
  function tick(now) { const t = Math.min(1, (now - start) / SW_DUR); const ease = easeOutBack(t)
    allGroup.position.y = 4 * (1 - ease); allGroup.scale.setScalar(0.85 + 0.15 * ease)
    if (t >= 1) { allGroup.position.y = 0; allGroup.scale.setScalar(1); animating.value = false }
    else { switchAnimId = requestAnimationFrame(tick) } }
  cancelSwitchAnim(); switchAnimId = requestAnimationFrame(tick)
})

// ---- Build ----

function buildScene() {
  allGroup = new THREE.Group(); scene.add(allGroup)
  const N = (props.stations || []).length; const dims = sDimensions(N); const pad = 1.6; const w = dims.maxW * 1.15 + pad; const d = dims.totalZ + 2.0
  allGroup.add(meshAt(new THREE.BoxGeometry(w, 0.06, d), new THREE.MeshStandardMaterial({ color: 0xd4d8dd, roughness: 0.5, metalness: 0.3 }), 0, 0.03, 0))
  allGroup.add(meshAt(new THREE.BoxGeometry(w, 0.14, d - 0.06), new THREE.MeshStandardMaterial({ color: 0xc8cdd3, roughness: 0.6, metalness: 0.4 }), 0, -0.07, 0))
  stationGroups = (props.stations || []).map((st, i) => buildStation(st, i, N))
  if (N > 0) { controls.target.set(0, 0, 0); controls.update() }
}

onMounted(() => {
  if (!canvasMount.value) return
  const ctx = three.init(canvasMount.value); scene = ctx.scene; camera = ctx.camera; renderer = ctx.renderer; controls = ctx.controls
  setupLighting(); buildScene(); setupInteraction(ctx.raycaster, ctx.mouse)
})

onBeforeUnmount(() => { cancelSwitchAnim() })
</script>

<style scoped>
.ld3d-root { position: relative; width: 100%; min-height: 380px; height: 45vh; overflow: hidden; background: #e8ecf0; border: 1px solid #d0d5db; }
.ld3d-canvas { width: 100%; height: 100%; }
.ld3d-arrow { position: absolute; top: 50%; transform: translateY(-50%); z-index: 20; width: 40px; height: 52px; display: grid; place-items: center; border: 1px solid rgba(0,0,0,0.1); background: rgba(255,255,255,0.75); color: #555; cursor: pointer; backdrop-filter: blur(6px); transition: background 0.15s, color 0.15s; }
.ld3d-arrow:hover { background: rgba(255,255,255,0.9); color: #1a1a1a; }
.ld3d-arrow:disabled { opacity: 0.2; cursor: default; }
.ld3d-arrow--left { left: 0; border-radius: 0 4px 4px 0; }
.ld3d-arrow--right { right: 0; border-radius: 4px 0 0 4px; }
</style>

<style>
.ld3d-tooltip { position: fixed; left: 0; top: 0; pointer-events: none; z-index: 9999; padding: 6px 10px; background: rgba(30,35,42,0.94); border: 1px solid rgba(255,255,255,0.12); border-radius: 4px; box-shadow: 0 4px 12px rgba(0,0,0,0.25); backdrop-filter: blur(6px); font-size: 12px; line-height: 1.5; min-width: 90px; transition: transform 0.12s ease-out; will-change: transform; }
.ld3d-tooltip--dev { background: rgba(30,41,59,0.94); border-color: rgba(56,189,248,0.2); }
.tt-name   { font-weight: 600; color: rgba(255,255,255,0.88); }
.tt-row    { display: flex; align-items: center; gap: 5px; margin-top: 2px; }
.tt-status { font-size: 10px; color: rgba(255,255,255,0.45); }
.tt-sep    { color: rgba(255,255,255,0.15); font-size: 10px; }
.tt-op     { font-size: 10px; color: #93c5fd; }
.tt-hint   { font-size: 10px; color: rgba(56,189,248,0.6); margin-top: 2px; }
</style>

