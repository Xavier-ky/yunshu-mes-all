<template>
  <div ref="root" class="lo3d-root">
    <div ref="canvasMount" class="lo3d-canvas"></div>

    <div v-if="loading" class="lo3d-loading">
      <div class="lo3d-loading-box">
        <div class="lo3d-loading-title">工厂 3D 总览</div>
        <div class="lo3d-loading-bar"><div class="lo3d-loading-fill" :style="{ width: loadPct + '%' }"></div></div>
        <div class="lo3d-loading-text">{{ loadStatusText }}</div>
      </div>
    </div>

    <div v-if="loadError && !loading" class="lo3d-error">
      <p>{{ loadError }}</p>
      <button type="button" class="lo3d-error-btn" @click="retryLoad">重新加载</button>
    </div>

    <button v-if="!loading && !loadError && lineMarkers.length > 1" class="lo3d-arrow lo3d-arrow--left" @click="prevLine" :disabled="transitioning">
      <ChevronLeft :size="28" :stroke-width="1.5" />
    </button>
    <button v-if="!loading && !loadError && lineMarkers.length > 1" class="lo3d-arrow lo3d-arrow--right" @click="nextLine" :disabled="transitioning">
      <ChevronRight :size="28" :stroke-width="1.5" />
    </button>

    <div v-if="!loadError && currentMarker" class="lo3d-bar">
      <div class="lo3d-bar-left">
        <span v-if="lineMarkers.length > 1" class="lo3d-counter">{{ focusedIdx + 1 }} / {{ lineMarkers.length }}</span>
        <span class="lo3d-bar-name">{{ currentMarker.lineName || "\u2014" }}</span>
        <span class="lo3d-bar-status" :class="'st-' + (currentMarker.lineStatus || 'IDLE')">
          {{ statusLabel(currentMarker.lineStatus) }}
        </span>
      </div>
      <div class="lo3d-bar-right">
        <span class="lo3d-bar-meta">OEE <strong>{{ Number(currentMarker.oee || 0).toFixed(1) }}%</strong></span>
        <span class="lo3d-bar-meta">进度 <strong>{{ Math.round(currentMarker.progressPct || 0) }}%</strong></span>
        <span v-if="currentMarker.currentWorkOrderNo" class="lo3d-bar-meta">{{ currentMarker.currentWorkOrderNo }}</span>
        <span v-if="currentMarker.openAndonCount" class="lo3d-bar-andon">{{ currentMarker.openAndonCount }} 安灯</span>
        <button class="lo3d-bar-btn" @click="enterDetailTap">进入产线 →</button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="tooltip.visible" class="lo3d-tooltip" :style="{ transform: 'translate(' + tooltip.x + 'px,' + tooltip.y + 'px)' }">
        <div class="lo3d-tt-name">{{ tooltip.name }}</div>
        <div class="lo3d-tt-row">
          <span class="lo3d-tt-status" :style="{ color: tooltip.statusColor }">{{ tooltip.statusLabel }}</span>
          <span class="lo3d-tt-sep">|</span>
          <span>OEE <strong>{{ tooltip.oee }}%</strong></span>
        </div>
        <div v-if="tooltip.workOrderNo" class="lo3d-tt-row lo3d-tt-meta">{{ tooltip.workOrderNo }}</div>
        <div v-if="tooltip.andonCount" class="lo3d-tt-row lo3d-tt-meta lo3d-tt-andon">{{ tooltip.andonCount }} 安灯</div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="ctxMenu.visible" class="lo3d-ctx" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }" @click.stop>
        <div class="lo3d-ctx-title">在此位置创建产线</div>
        <label class="lo3d-ctx-label">
          产线编码
          <input v-model="ctxMenu.lineCode" class="lo3d-ctx-input" placeholder="L-XXX" @keyup.enter="submitCreateLine" />
        </label>
        <label class="lo3d-ctx-label">
          产线名称
          <input v-model="ctxMenu.lineName" class="lo3d-ctx-input" placeholder="请输入产线名称" @keyup.enter="submitCreateLine" />
        </label>
        <div class="lo3d-ctx-actions">
          <button class="lo3d-ctx-btn lo3d-ctx-btn--cancel" @click="ctxMenu.visible = false">取消</button>
          <button class="lo3d-ctx-btn lo3d-ctx-btn--ok" @click="submitCreateLine" :disabled="!ctxMenu.lineCode.trim() || !ctxMenu.lineName.trim()">创建</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch, nextTick } from "vue"
import * as THREE from "three"
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js"
import { useThreeScene } from "@/composables/useThreeScene"
import { ChevronLeft, ChevronRight } from "lucide-vue-next"

const FACTORY_GLB = "/models/factory.glb"

const props = defineProps({
  lines: { type: Array, default: () => [] },
  initialLineId: { type: Number, default: null },
})
const emit = defineEmits(["select-line", "create-line"])

const root = ref(null)
const canvasMount = ref(null)
const loading = ref(true)
const loadError = ref("")
const loadPct = ref(0)
const loadStatusText = ref("准备加载…")
const transitioning = ref(false)
const focusedIdx = ref(0)

const tooltip = reactive({ visible: false, name: "", statusLabel: "", statusColor: "", oee: 0, workOrderNo: "", andonCount: 0, x: 0, y: 0 })
const ctxMenu = reactive({ visible: false, x: 0, y: 0, lineCode: "", lineName: "", worldX: 0, worldY: 0, worldZ: 0 })

const STATUS_COLORS = { RUNNING: 0x10b981, WARNING: 0xf59e0b, FAULT: 0xef4444, CHANGEOVER: 0x3b82f6, IDLE: 0x64748b }
/** 产线在工厂平面上的归一化锚点：u=长轴位置，v=0.5 为车间中心带 */
const DEFAULT_LINE_UV = {
  "LINE-FAN-01": { u: 0.22, v: 0.36 },
  "LINE-FAN-02": { u: 0.54, v: 0.52 },
  "LINE-FAN-03": { u: 0.78, v: 0.36 },
  "LINE-FAN-04": { u: 0.38, v: 0.64 },
  "L-001": { u: 0.22, v: 0.36 },
  "L-002": { u: 0.54, v: 0.52 },
  "L-003": { u: 0.78, v: 0.36 },
}
/** 初始相机：倍率越小模型在屏幕上越大（<1 贴近，>1 拉远） */
const CAMERA_FILL_MULT = 0.52
const CAMERA_FOV = 42
const VIEW_TARGET_Y_RATIO = -0.06
const VIEW_CAMERA_Y_RATIO = 0.74
const VIEW_CAMERA_Z_RATIO = 0.46
const MARKER_SCALE_RATIO = 0.0022
const MARKER_SCALE_MIN = 5
const MARKER_SCALE_MAX = 26
const MARKER_MIN_SEP_RATIO = 0.055
const INTERACTIVE_KINDS = new Set(["marker", "beacon", "ripple"])

let scene, camera, renderer, controls
let modelGroup = null
let markerGroup = null
let groundPlane = null
let flyAnimId = null
let hoveredMarker = null
let beaconSpheres = []
let rippleSets = []
let factoryBounds = null
let factoryPickTargets = []
let surfaceRaycaster = null
let factoryFootprint = 100
let modelOrigin = { x: 0, y: 0, z: 0 }
let markerScale = 1
let viewTargetY = 0
let initCtx = null
const lineMarkers = ref([])

const currentMarker = computed(() => lineMarkers.value[focusedIdx.value] || null)

function statusLabel(s) {
  return { RUNNING: "运行", WARNING: "预警", FAULT: "故障", CHANGEOVER: "换型", IDLE: "待机" }[s] || s
}

function worldToScene(worldX, worldY, worldZ) {
  return {
    x: worldX - modelOrigin.x,
    y: (worldY ?? 0) - modelOrigin.y,
    z: worldZ - modelOrigin.z,
  }
}

function sceneToWorld(sceneX, sceneY, sceneZ) {
  return {
    x: Math.round((sceneX + modelOrigin.x) * 100) / 100,
    y: 0,
    z: Math.round((sceneZ + modelOrigin.z) * 100) / 100,
  }
}

function snapMarkerToSurface(sceneX, sceneZ, fallbackY = 0) {
  if (!factoryBounds || !factoryPickTargets.length) {
    return { x: sceneX, y: fallbackY, z: sceneZ }
  }
  if (!surfaceRaycaster) surfaceRaycaster = new THREE.Raycaster()
  const size = factoryBounds.getSize(new THREE.Vector3())
  const maxY = factoryBounds.max.y + Math.max(size.y * 0.2, 20)
  const origin = new THREE.Vector3(sceneX, maxY, sceneZ)
  const dir = new THREE.Vector3(0, -1, 0)
  surfaceRaycaster.set(origin, dir)
  const hits = surfaceRaycaster.intersectObjects(factoryPickTargets, true)
  if (hits.length) {
    const pt = hits[0].point
    return { x: pt.x, y: pt.y, z: pt.z }
  }
  return { x: sceneX, y: fallbackY, z: sceneZ }
}

function spreadMarkerPositions(positions) {
  if (positions.length < 2) return positions
  const minSep = factoryFootprint * MARKER_MIN_SEP_RATIO
  for (let iter = 0; iter < 10; iter++) {
    let moved = false
    for (let i = 0; i < positions.length; i++) {
      for (let j = i + 1; j < positions.length; j++) {
        const a = positions[i]
        const b = positions[j]
        const dx = b.x - a.x
        const dz = b.z - a.z
        const dist = Math.hypot(dx, dz)
        if (dist >= minSep) continue
        const push = (minSep - dist) * 0.55
        const angle = dist < 0.001 ? (i + j + 1) * 1.35 : Math.atan2(dz, dx)
        const ox = Math.cos(angle) * push * 0.5
        const oz = Math.sin(angle) * push * 0.5
        a.x -= ox
        a.z -= oz
        b.x += ox
        b.z += oz
        const sa = snapMarkerToSurface(a.x, a.z, a.y)
        const sb = snapMarkerToSurface(b.x, b.z, b.y)
        Object.assign(a, sa)
        Object.assign(b, sb)
        moved = true
      }
    }
    if (!moved) break
  }
  return positions
}

function resolveAllMarkerPositions(lines) {
  const raw = lines.map((line, idx) => ({
    line,
    ...resolveMarkerScenePos(line, idx, lines.length),
  }))
  return spreadMarkerPositions(raw)
}

function resolveMarkerScenePos(line, slotIndex, lineCount) {
  if (factoryBounds) {
    const size = factoryBounds.getSize(new THREE.Vector3())
    const min = factoryBounds.min
    const center = factoryBounds.getCenter(new THREE.Vector3())
    const alongX = size.x >= size.z
    const groundY = min.y + Math.max(size.y * 0.008, 0.3)
    const uv = DEFAULT_LINE_UV[line.lineCode]
    const u = uv ? uv.u : (slotIndex + 1) / (lineCount + 1)
    const v = uv ? uv.v : 0.5
    const lateral = (v - 0.5) * 0.22
    let sceneX
    let sceneZ
    if (alongX) {
      sceneX = min.x + size.x * u
      sceneZ = center.z + size.z * lateral
    } else {
      sceneX = center.x + size.x * lateral
      sceneZ = min.z + size.z * u
    }
    return snapMarkerToSurface(sceneX, sceneZ, groundY)
  }
  let x = line.modelPosX
  let y = line.modelPosY
  let z = line.modelPosZ
  if (x != null && z != null) {
    return worldToScene(x, y, z)
  }
  const id = line.lineId ?? slotIndex
  const slot = Math.abs(id) % 5
  return worldToScene(300 + slot * 115, 10, -1000 - slot * 1500)
}

function pickMarkerIndex(hits) {
  for (const h of hits) {
    let obj = h.object
    while (obj) {
      const kind = obj.userData?.kind
      const idx = obj.userData?.lineIndex
      if (idx != null && INTERACTIVE_KINDS.has(kind)) return idx
      obj = obj.parent
    }
  }
  return null
}

const three = useThreeScene({
  background: 0x1a1a2e,
  fov: CAMERA_FOV,
  cameraPos: [0, 400, 800],
  cameraLookAt: [0, 0, 0],
  orbit: {
    enablePan: true,
    minDistance: 20,
    maxDistance: 50000,
    maxPolarAngle: Math.PI / 2.15,
    target: new THREE.Vector3(0, 0, 0),
  },
  far: 100000,
})

function setupLighting() {
  for (let i = scene.children.length - 1; i >= 0; i--) {
    if (scene.children[i].isLight) scene.remove(scene.children[i])
  }
  scene.add(new THREE.AmbientLight(0x404060, 0.65))
  const hemi = new THREE.HemisphereLight(0x87ceeb, 0x362d2d, 0.85)
  scene.add(hemi)
  const dir = new THREE.DirectionalLight(0xffeedd, 2.2)
  dir.position.set(500, 800, 600)
  scene.add(dir)
  const fill = new THREE.DirectionalLight(0x4488ff, 0.55)
  fill.position.set(-300, 200, -400)
  scene.add(fill)
}

function frameCameraToBounds(box) {
  if (!box || box.isEmpty()) return
  const size = box.getSize(new THREE.Vector3())
  const center = box.getCenter(new THREE.Vector3())
  factoryFootprint = Math.max(size.x, size.z, 1)
  const height = Math.max(size.y, 1)

  markerScale = THREE.MathUtils.clamp(
    factoryFootprint * MARKER_SCALE_RATIO,
    MARKER_SCALE_MIN,
    MARKER_SCALE_MAX,
  )
  viewTargetY = center.y + height * VIEW_TARGET_Y_RATIO

  camera.fov = CAMERA_FOV
  camera.updateProjectionMatrix()

  const aspect = camera.aspect || 1.6
  const vFov = (camera.fov * Math.PI) / 180
  const hFov = 2 * Math.atan(Math.tan(vFov / 2) * aspect)
  const distY = (height / 2) / Math.tan(vFov / 2)
  const distX = (factoryFootprint / 2) / Math.tan(hFov / 2)
  const fitDist = Math.max(distX, distY, factoryFootprint * 0.35) * CAMERA_FILL_MULT

  camera.position.set(
    center.x + fitDist * 0.48,
    center.y + fitDist * VIEW_CAMERA_Y_RATIO,
    center.z + fitDist * VIEW_CAMERA_Z_RATIO,
  )
  controls.target.set(center.x, viewTargetY, center.z)
  controls.minDistance = factoryFootprint * 0.08
  controls.maxDistance = factoryFootprint * 6
  controls.update()

  const fogNear = fitDist * 3.5
  const fogFar = fitDist * 18
  scene.fog = new THREE.Fog(0x1a1a2e, fogNear, fogFar)
}

function loadFactoryGlb() {
  return new Promise((resolve, reject) => {
    const loader = new GLTFLoader()
    loader.load(
      FACTORY_GLB,
      (gltf) => resolve(gltf),
      (xhr) => {
        if (xhr.total) {
          loadPct.value = Math.round((xhr.loaded / xhr.total) * 100)
          const mb = (xhr.loaded / (1024 * 1024)).toFixed(1)
          const totalMb = (xhr.total / (1024 * 1024)).toFixed(0)
          loadStatusText.value = `加载工厂模型 ${mb} / ${totalMb} MB (${loadPct.value}%)`
        } else {
          loadStatusText.value = "加载工厂模型…"
        }
      },
      (err) => reject(err),
    )
  })
}

async function loadModels() {
  loading.value = true
  loadError.value = ""
  loadPct.value = 0
  loadStatusText.value = "加载工厂模型…"

  if (modelGroup) {
    scene.remove(modelGroup)
    modelGroup.traverse((c) => {
      if (c.geometry) c.geometry.dispose()
      if (c.material) {
        const mats = Array.isArray(c.material) ? c.material : [c.material]
        mats.forEach((m) => {
          if (m.map) m.map.dispose()
          m.dispose()
        })
      }
    })
    modelGroup = null
  }
  factoryPickTargets = []

  modelGroup = new THREE.Group()
  scene.add(modelGroup)

  try {
    const gltf = await loadFactoryGlb()
    const factory = gltf.scene
    factory.traverse((child) => {
      if (child.isMesh) {
        child.castShadow = true
        child.receiveShadow = true
      }
    })

    const box = new THREE.Box3().setFromObject(factory)
    const center = box.getCenter(new THREE.Vector3())
    modelOrigin = { x: center.x, y: box.min.y, z: center.z }
    factory.position.set(-center.x, -box.min.y, -center.z)
    modelGroup.add(factory)

    factoryBounds = new THREE.Box3().setFromObject(modelGroup)
    factoryPickTargets = []
    factory.traverse((child) => {
      if (child.isMesh) factoryPickTargets.push(child)
    })
    frameCameraToBounds(factoryBounds)
    updateGroundPlane()
    loadPct.value = 100
    loadStatusText.value = "加载完成"
    loading.value = false
  } catch (e) {
    console.error("Failed to load factory.glb:", e)
    loadError.value = "3D 工厂模型加载失败，请检查网络后刷新页面重试"
    loading.value = false
  }
}

function updateGroundPlane() {
  if (!groundPlane || !factoryBounds) return
  const size = factoryBounds.getSize(new THREE.Vector3())
  const center = factoryBounds.getCenter(new THREE.Vector3())
  const span = Math.max(size.x, size.z, 100) * 2
  groundPlane.geometry.dispose()
  groundPlane.geometry = new THREE.PlaneGeometry(span, span)
  groundPlane.position.set(center.x, factoryBounds.min.y, center.z)
}

function addGroundPlane() {
  const geo = new THREE.PlaneGeometry(20000, 20000)
  const mat = new THREE.MeshBasicMaterial({ visible: false, side: THREE.DoubleSide })
  groundPlane = new THREE.Mesh(geo, mat)
  groundPlane.rotation.x = -Math.PI / 2
  groundPlane.position.y = 0
  groundPlane.name = "groundPlane"
  scene.add(groundPlane)
}

function buildMarkers() {
  if (!scene || loading.value || loadError.value) return

  if (markerGroup) {
    scene.remove(markerGroup)
    markerGroup.traverse((c) => {
      if (c.material && c.material.map) c.material.map.dispose()
      if (c.material) c.material.dispose()
      if (c.geometry) c.geometry.dispose()
    })
  }
  beaconSpheres = []
  rippleSets = []
  markerGroup = new THREE.Group()
  const arr = []
  const lines = props.lines || []
  const ms = markerScale || 1

  const resolvedPositions = resolveAllMarkerPositions(lines)

  lines.forEach((line, idx) => {
    const pos = resolvedPositions[idx]
    const markerIndex = arr.length
    const color = STATUS_COLORS[line.lineStatus] || STATUS_COLORS.IDLE
    const { x, y, z } = pos

    const ringGeo = new THREE.TorusGeometry(ms * 2.8, ms * 0.18, 14, 40)
    const ringMat = new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.6, roughness: 0.3, metalness: 0.1 })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2
    ring.position.set(x, y + ms * 0.12, z)
    ring.userData = { kind: "marker", lineIndex: markerIndex }
    markerGroup.add(ring)

    const discGeo = new THREE.CircleGeometry(ms * 2.5, 28)
    const discMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.18, side: THREE.DoubleSide, depthTest: true, depthWrite: false, fog: false })
    const disc = new THREE.Mesh(discGeo, discMat)
    disc.rotation.x = -Math.PI / 2
    disc.position.set(x, y + ms * 0.06, z)
    disc.renderOrder = 1
    disc.userData = { kind: "marker", lineIndex: markerIndex }
    markerGroup.add(disc)

    const pillarH = ms * 8
    const pillarGeo = new THREE.CylinderGeometry(ms * 0.26, ms * 0.62, pillarH, 14)
    const pillarMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.22, depthTest: true, depthWrite: false, fog: false })
    const pillar = new THREE.Mesh(pillarGeo, pillarMat)
    pillar.position.set(x, y + pillarH * 0.5, z)
    pillar.renderOrder = 2
    pillar.userData = { kind: "marker", lineIndex: markerIndex }
    markerGroup.add(pillar)

    const coreGeo = new THREE.CylinderGeometry(ms * 0.13, ms * 0.13, pillarH, 10)
    const coreMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.5, depthTest: true, depthWrite: false, fog: false })
    const core = new THREE.Mesh(coreGeo, coreMat)
    core.position.set(x, y + pillarH * 0.5, z)
    core.renderOrder = 3
    core.userData = { kind: "marker", lineIndex: markerIndex }
    markerGroup.add(core)

    const beaconGeo = new THREE.SphereGeometry(ms * 0.72, 18, 18)
    const beaconMat = new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 1.5, roughness: 0.15, metalness: 0.1 })
    const beacon = new THREE.Mesh(beaconGeo, beaconMat)
    beacon.position.set(x, y + pillarH, z)
    beacon.userData = { kind: "beacon", lineIndex: markerIndex, baseEmissive: 1.5 }
    markerGroup.add(beacon)
    beaconSpheres.push(beacon)

    const rippleSet = []
    for (let r = 0; r < 3; r++) {
      const rGeo = new THREE.TorusGeometry(ms * 3.0, ms * 0.13, 10, 48)
      const rMat = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0, depthTest: true, depthWrite: false, fog: false })
      const ripple = new THREE.Mesh(rGeo, rMat)
      ripple.rotation.x = -Math.PI / 2
      ripple.position.set(x, y + ms * 0.12, z)
      ripple.renderOrder = 1
      ripple.userData = { kind: "ripple", phase: r * 0.8, lineIndex: markerIndex }
      markerGroup.add(ripple)
      rippleSet.push(ripple)
    }
    rippleSets.push(rippleSet)

    const label = makeLabelSprite(line.lineName, color, ms)
    label.position.set(x, y + pillarH + ms * 3.2, z)
    label.userData = { kind: "marker", lineIndex: markerIndex }
    markerGroup.add(label)

    const world = sceneToWorld(x, y, z)
    arr.push({
      lineId: line.lineId,
      lineName: line.lineName,
      lineStatus: line.lineStatus,
      oee: line.oee,
      progressPct: line.progressPct,
      currentWorkOrderNo: line.currentWorkOrderNo,
      openAndonCount: line.openAndonCount,
      worldX: x,
      worldY: y,
      worldZ: z,
      modelPosX: world.x,
      modelPosY: world.y,
      modelPosZ: world.z,
    })
  })
  scene.add(markerGroup)
  markerGroup.renderOrder = 10
  lineMarkers.value = arr
}

function makeLabelSprite(text, color, scale = 1) {
  const canvas = document.createElement("canvas")
  canvas.width = 640
  canvas.height = 160
  const ctx = canvas.getContext("2d")
  const hex = "#" + color.toString(16).padStart(6, "0")
  ctx.font = "bold 48px system-ui, 'Microsoft YaHei', sans-serif"
  ctx.textAlign = "center"
  ctx.textBaseline = "middle"
  const textW = Math.min(ctx.measureText(text).width + 56, 600)
  const boxW = textW
  const boxH = 78
  const boxX = (canvas.width - boxW) / 2
  const boxY = (canvas.height - boxH) / 2
  ctx.fillStyle = "rgba(12,16,24,0.82)"
  ctx.strokeStyle = hex
  ctx.lineWidth = 3
  roundRect(ctx, boxX, boxY, boxW, boxH, 10)
  ctx.fill()
  ctx.stroke()
  ctx.fillStyle = hex
  ctx.shadowColor = "rgba(0,0,0,0.55)"
  ctx.shadowBlur = 10
  ctx.fillText(text, canvas.width / 2, canvas.height / 2 + 1)
  ctx.shadowBlur = 0
  ctx.fillStyle = "rgba(255,255,255,0.95)"
  ctx.fillText(text, canvas.width / 2, canvas.height / 2 + 1)
  const tex = new THREE.CanvasTexture(canvas)
  tex.minFilter = THREE.LinearFilter
  const mat = new THREE.SpriteMaterial({
    map: tex,
    transparent: true,
    depthTest: false,
    depthWrite: false,
    fog: false,
    toneMapped: false,
  })
  const sprite = new THREE.Sprite(mat)
  const labelScale = THREE.MathUtils.clamp(scale * 2.5, 16, 88)
  sprite.scale.set(labelScale * 4.4, labelScale * 1.15, 1)
  sprite.renderOrder = 1000
  sprite.userData.isLabel = true
  return sprite
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function easeOutCubic(t) { return 1 - Math.pow(1 - t, 3) }

function cancelFly() { if (flyAnimId) { cancelAnimationFrame(flyAnimId); flyAnimId = null } }

function flyToLine(idx, duration = 800) {
  if (!lineMarkers.value[idx]) return
  cancelFly()
  transitioning.value = true
  const m = lineMarkers.value[idx]
  const sp = camera.position.clone()
  const st = controls.target.clone()
  const dist = factoryFootprint * 0.14
  const ep = new THREE.Vector3(m.worldX + dist * 0.45, m.worldY + dist * 0.74, m.worldZ + dist * 0.42)
  const et = new THREE.Vector3(m.worldX, m.worldY + (markerScale || 1) * 1.5, m.worldZ)
  const start = performance.now()
  function tick(now) {
    const t = Math.min(1, (now - start) / duration)
    const ease = easeOutCubic(t)
    camera.position.lerpVectors(sp, ep, ease)
    controls.target.lerpVectors(st, et, ease)
    if (t >= 1) {
      camera.position.copy(ep)
      controls.target.copy(et)
      transitioning.value = false
    } else {
      flyAnimId = requestAnimationFrame(tick)
    }
  }
  flyAnimId = requestAnimationFrame(tick)
  focusedIdx.value = idx
}

function enterDetailTap() {
  const m = lineMarkers.value[focusedIdx.value]
  if (m) emit("select-line", m.lineId)
}

function prevLine() {
  if (lineMarkers.value.length <= 1 || transitioning.value) return
  const idx = (focusedIdx.value - 1 + lineMarkers.value.length) % lineMarkers.value.length
  flyToLine(idx)
}
function nextLine() {
  if (lineMarkers.value.length <= 1 || transitioning.value) return
  const idx = (focusedIdx.value + 1) % lineMarkers.value.length
  flyToLine(idx)
}

let clickTimer = null
let pendingLineId = null

let interactionBound = false

function setupInteraction(raycaster, mouse) {
  if (interactionBound) return
  interactionBound = true
  renderer.domElement.addEventListener("click", (ev) => {
    if (loading.value || loadError.value || transitioning.value) return
    const rect = renderer.domElement.getBoundingClientRect()
    mouse.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(mouse, camera)
    const hits = raycaster.intersectObjects(markerGroup ? markerGroup.children : [], true)
    const foundIdx = pickMarkerIndex(hits)
    if (foundIdx != null) {
      if (pendingLineId === lineMarkers.value[foundIdx]?.lineId && clickTimer) {
        clearTimeout(clickTimer)
        clickTimer = null
        pendingLineId = null
        enterDetailTap()
      } else {
        pendingLineId = lineMarkers.value[foundIdx]?.lineId
        flyToLine(foundIdx)
        clickTimer = setTimeout(() => { clickTimer = null }, 1200)
      }
    } else {
      pendingLineId = null
      if (clickTimer) { clearTimeout(clickTimer); clickTimer = null }
    }
  })

  renderer.domElement.addEventListener("mousemove", (ev) => {
    if (loading.value || loadError.value || transitioning.value) return
    const rect = renderer.domElement.getBoundingClientRect()
    mouse.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(mouse, camera)
    const hits = raycaster.intersectObjects(markerGroup ? markerGroup.children : [], true)
    const foundIdx = pickMarkerIndex(hits)
    if (foundIdx != null) {
      const li = lineMarkers.value[foundIdx]
      if (li) {
        tooltip.name = li.lineName
        tooltip.statusLabel = statusLabel(li.lineStatus)
        const sc = STATUS_COLORS[li.lineStatus]
        tooltip.statusColor = sc ? "#" + sc.toString(16).padStart(6, "0") : "#94a3b8"
        tooltip.oee = Number(li.oee || 0).toFixed(1)
        tooltip.workOrderNo = li.currentWorkOrderNo || ""
        tooltip.andonCount = li.openAndonCount || 0
        tooltip.visible = true
        tooltip.x = ev.clientX + 16
        tooltip.y = ev.clientY - 12
      }
      if (hoveredMarker !== foundIdx) {
        hoveredMarker = foundIdx
        renderer.domElement.style.cursor = "pointer"
      }
    } else {
      if (tooltip.visible) tooltip.visible = false
      if (hoveredMarker !== null) {
        hoveredMarker = null
        renderer.domElement.style.cursor = ""
      }
    }
  })

  renderer.domElement.addEventListener("mouseleave", () => {
    tooltip.visible = false
    hoveredMarker = null
    renderer.domElement.style.cursor = ""
  })

  renderer.domElement.addEventListener("contextmenu", (ev) => {
    ev.preventDefault()
    if (loading.value || loadError.value || transitioning.value) return
    const rect = renderer.domElement.getBoundingClientRect()
    mouse.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(mouse, camera)
    const groundHits = raycaster.intersectObject(groundPlane)
    if (groundHits.length > 0) {
      const pt = groundHits[0].point
      const world = sceneToWorld(pt.x, pt.y, pt.z)
      ctxMenu.worldX = world.x
      ctxMenu.worldY = world.y
      ctxMenu.worldZ = world.z
      ctxMenu.x = ev.clientX
      ctxMenu.y = ev.clientY
      ctxMenu.lineCode = ""
      ctxMenu.lineName = ""
      ctxMenu.visible = true
    }
  })
}

function submitCreateLine() {
  if (!ctxMenu.lineCode.trim() || !ctxMenu.lineName.trim()) return
  emit("create-line", {
    lineCode: ctxMenu.lineCode.trim(),
    lineName: ctxMenu.lineName.trim(),
    modelPosX: ctxMenu.worldX,
    modelPosY: ctxMenu.worldY,
    modelPosZ: ctxMenu.worldZ,
  })
  ctxMenu.visible = false
}

async function initScene() {
  await nextTick()
  await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))
  if (!canvasMount.value || !root.value) return

  const ctx = three.init(canvasMount.value, root.value)
  initCtx = ctx
  scene = ctx.scene
  camera = ctx.camera
  renderer = ctx.renderer
  controls = ctx.controls
  three.resize()
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.4
  setupLighting()
  addGroundPlane()
  await loadModels()
  if (!loadError.value) {
    buildMarkers()
    await nextTick()
    three.resize()
    if (props.initialLineId) {
      const idx = lineMarkers.value.findIndex((m) => m.lineId === props.initialLineId)
      if (idx >= 0) flyToLine(idx, 0)
    }
    setupInteraction(ctx.raycaster, ctx.mouse)
    three.onUpdate(() => {
      const t = performance.now() * 0.001
      for (const b of beaconSpheres) {
        const pulse = 0.5 + 0.5 * Math.sin(t * 2.5 + (b.userData.lineIndex || 0) * 1.8)
        b.material.emissiveIntensity = (b.userData.baseEmissive || 1.5) * (0.4 + 0.6 * pulse)
        const s = 1 + 0.15 * pulse
        b.scale.setScalar(s)
      }
      for (const set of rippleSets) {
        for (const r of set) {
          const phase = r.userData.phase || 0
          const cycle = ((t + phase) % 2.2) / 2.2
          const rScale = 1 + cycle * 2.5
          r.scale.setScalar(rScale)
          r.material.opacity = cycle < 0.15 ? (cycle / 0.15) * 0.35 : cycle < 0.7 ? 0.35 : ((1 - cycle) / 0.3) * 0.35
        }
      }
    })
  }
}

async function retryLoad() {
  interactionBound = false
  await loadModels()
  if (!loadError.value && initCtx) {
    buildMarkers()
    three.resize()
    setupInteraction(initCtx.raycaster, initCtx.mouse)
  }
}

function onDocClick() { if (ctxMenu.visible) ctxMenu.visible = false }
onMounted(() => document.addEventListener("click", onDocClick))
onMounted(() => initScene())

watch(() => props.lines, () => {
  if (scene && !loading.value && !loadError.value) buildMarkers()
}, { deep: true })

function onKey(e) {
  if (e.key === "ArrowLeft") prevLine()
  else if (e.key === "ArrowRight") nextLine()
  else if (e.key === "Enter" && currentMarker.value) enterDetailTap()
}
onMounted(() => window.addEventListener("keydown", onKey))
onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKey)
  cancelFly()
  document.removeEventListener("click", onDocClick)
  three.offUpdate()
  beaconSpheres = []
  rippleSets = []
})
</script>

<style scoped>
.lo3d-root { position: relative; width: 100%; height: 100%; min-height: 0; overflow: hidden; background: #1a1a2e; user-select: none; }
.lo3d-canvas { position: absolute; inset: 0; z-index: 0; }
.lo3d-canvas :deep(canvas) { display: block; }
.lo3d-loading { position: absolute; inset: 0; z-index: 50; display: grid; place-items: center; background: rgba(26,26,46,0.95); }
.lo3d-loading-box { text-align: center; color: #fff; }
.lo3d-loading-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.lo3d-loading-bar { width: 300px; height: 4px; background: rgba(255,255,255,0.1); border-radius: 2px; overflow: hidden; margin: 0 auto; }
.lo3d-loading-fill { height: 100%; background: #4a9eff; transition: width 0.3s; }
.lo3d-loading-text { margin-top: 8px; font-size: 13px; color: #aaa; max-width: 320px; }
.lo3d-error { position: absolute; inset: 0; z-index: 40; display: grid; place-items: center; background: rgba(26,26,46,0.88); color: #fca5a5; text-align: center; padding: 24px; }
.lo3d-error p { margin: 0 0 16px; font-size: 14px; color: rgba(255,255,255,0.75); }
.lo3d-error-btn { padding: 8px 18px; border: 1px solid rgba(59,130,246,0.5); background: rgba(59,130,246,0.15); color: #93c5fd; cursor: pointer; border-radius: 4px; font-size: 13px; }
.lo3d-error-btn:hover { background: rgba(59,130,246,0.28); }
.lo3d-arrow { position: absolute; top: 50%; transform: translateY(-50%); z-index: 20; width: 40px; height: 52px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,0.08); background: rgba(40,40,40,0.8); color: rgba(255,255,255,0.55); cursor: pointer; backdrop-filter: blur(6px); transition: background 0.15s, color 0.15s; }
.lo3d-arrow:hover { background: rgba(60,60,70,0.9); color: rgba(255,255,255,0.85); }
.lo3d-arrow:disabled { opacity: 0.2; cursor: default; }
.lo3d-arrow--left { left: 0; border-radius: 0 4px 4px 0; }
.lo3d-arrow--right { right: 0; border-radius: 4px 0 0 4px; }
.lo3d-bar { position: absolute; bottom: 0; left: 0; right: 0; height: 46px; display: flex; align-items: center; justify-content: space-between; padding: 0 16px; background: rgba(40,40,40,0.85); backdrop-filter: blur(10px); border-top: 1px solid rgba(255,255,255,0.06); z-index: 20; font-size: 13px; }
.lo3d-bar-left, .lo3d-bar-right { display: flex; align-items: center; gap: 10px; }
.lo3d-counter { font-family: "JetBrains Mono", monospace; font-size: 11px; color: rgba(255,255,255,0.35); min-width: 30px; }
.lo3d-bar-name { font-weight: 600; color: rgba(255,255,255,0.9); }
.lo3d-bar-status { font-size: 10px; padding: 2px 8px; border: 1px solid; }
.st-RUNNING { color: #34d399; border-color: rgba(16,185,129,0.3); background: rgba(16,185,129,0.1); }
.st-WARNING { color: #fbbf24; border-color: rgba(245,158,11,0.3); background: rgba(245,158,11,0.1); }
.st-FAULT { color: #f87171; border-color: rgba(239,68,68,0.3); background: rgba(239,68,68,0.1); }
.st-CHANGEOVER { color: #60a5fa; border-color: rgba(59,130,246,0.3); background: rgba(59,130,246,0.1); }
.st-IDLE { color: #94a3b8; border-color: rgba(100,116,139,0.3); }
.lo3d-bar-right { gap: 14px; }
.lo3d-bar-meta { font-size: 12px; color: rgba(255,255,255,0.45); }
.lo3d-bar-meta strong { color: rgba(255,255,255,0.8); margin-left: 4px; }
.lo3d-bar-andon { font-size: 11px; color: #fca5a5; background: rgba(239,68,68,0.12); border: 1px solid rgba(239,68,68,0.25); padding: 3px 8px; }
.lo3d-bar-btn { height: 30px; padding: 0 14px; border: 1px solid rgba(59,130,246,0.4); background: rgba(59,130,246,0.08); color: #93c5fd; font-size: 12px; cursor: pointer; transition: background 0.1s; }
.lo3d-bar-btn:hover { background: rgba(59,130,246,0.2); color: #bfdbfe; }
.lo3d-ctx { position: fixed; z-index: 10000; background: rgba(30,35,42,0.96); border: 1px solid rgba(255,255,255,0.12); border-radius: 6px; padding: 14px; min-width: 260px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); backdrop-filter: blur(8px); }
.lo3d-ctx-title { font-size: 13px; font-weight: 600; color: rgba(255,255,255,0.88); margin-bottom: 10px; }
.lo3d-ctx-label { display: block; font-size: 11px; color: rgba(255,255,255,0.5); margin-bottom: 6px; }
.lo3d-ctx-input { display: block; width: 100%; margin-top: 3px; padding: 6px 8px; font-size: 12px; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.15); border-radius: 3px; color: #fff; outline: none; }
.lo3d-ctx-input:focus { border-color: #4a9eff; }
.lo3d-ctx-actions { display: flex; gap: 8px; margin-top: 10px; justify-content: flex-end; }
.lo3d-ctx-btn { padding: 5px 14px; font-size: 12px; border-radius: 3px; cursor: pointer; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.7); }
.lo3d-ctx-btn--ok { background: #2563eb; border-color: #2563eb; color: #fff; }
.lo3d-ctx-btn--ok:disabled { opacity: 0.4; cursor: default; }
.lo3d-ctx-btn--ok:hover:not(:disabled) { background: #1d4ed8; }
</style>

<style>
.lo3d-tooltip {
  position: fixed; left: 0; top: 0;
  pointer-events: none; z-index: 9999;
  padding: 6px 10px;
  background: rgba(30,35,42,0.94);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
  backdrop-filter: blur(6px);
  font-size: 12px; line-height: 1.5;
  min-width: 100px;
  transition: transform 0.12s ease-out;
  will-change: transform;
}
.lo3d-tt-name { font-weight: 600; color: rgba(255,255,255,0.88); }
.lo3d-tt-row { display: flex; align-items: center; gap: 6px; color: rgba(255,255,255,0.55); font-size: 11px; }
.lo3d-tt-row strong { color: rgba(255,255,255,0.78); }
.lo3d-tt-sep { color: rgba(255,255,255,0.18); }
.lo3d-tt-meta { margin-top: 1px; color: rgba(255,255,255,0.4); }
.lo3d-tt-andon { color: #fca5a5; }
</style>
