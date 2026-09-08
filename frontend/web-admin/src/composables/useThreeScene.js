/**
 * useThreeScene — reusable Three.js init / animate / dispose composable.
 *
 * Usage:
 *   const three = useThreeScene({ background: 0xf2f2f2, fov: 50 })
 *   const { scene, camera, renderer, controls } = three.init(mountEl)
 *   // add meshes, lights, etc. to scene
 *   // three.onUpdate(fn) for per-frame logic (e.g. label projection)
 *   // dispose() is called automatically via onBeforeUnmount
 *
 * Designed to be copied into any Vue + Three.js project.
 * Only dependency: `three` npm package.
 */

import { onBeforeUnmount } from "vue"
import * as THREE from "three"
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js"

function addDefaultLights(scene) {
  scene.add(new THREE.AmbientLight(0xffffff, 0.55))
  const dir = new THREE.DirectionalLight(0xffffff, 0.75)
  dir.position.set(8, 16, 6)
  scene.add(dir)
  const fill = new THREE.DirectionalLight(0x8eb8ff, 0.25)
  fill.position.set(-4, 4, -4)
  scene.add(fill)
}

export function useThreeScene(config = {}) {
  const {
    background = 0xf2f2f2,
    fov = 50,
    near = 0.1,
    far = 1000,
    enableShadows = false,
    cameraPos = [0, 14, 14],
    cameraLookAt = [0, 0, 0],
    orbit = {},
  } = config

  let scene, camera, renderer, controls
  let animationId = null
  let resizeObserver = null
  let containerEl = null
  let sizeEl = null
  const raycaster = new THREE.Raycaster()
  const mouse = new THREE.Vector2()
  const clock = new THREE.Clock()
  const updateFns = new Set()

  function resize() {
    if (!sizeEl || !renderer || !camera) return
    const r = sizeEl.getBoundingClientRect()
    if (r.width === 0 || r.height === 0) return
    camera.aspect = r.width / r.height
    camera.updateProjectionMatrix()
    renderer.setSize(r.width, r.height)
  }

  function init(container, sizeTarget = container) {
    containerEl = container
    sizeEl = sizeTarget
    const rect = sizeEl.getBoundingClientRect()
    const w = rect.width || 800
    const h = rect.height || 500

    scene = new THREE.Scene()
    scene.background = new THREE.Color(background)

    camera = new THREE.PerspectiveCamera(fov, w / Math.max(h, 1), near, far)
    camera.position.set(...cameraPos)
    camera.lookAt(...cameraLookAt)

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
    renderer.setSize(w, h)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = enableShadows
    renderer.domElement.style.display = "block"
    container.appendChild(renderer.domElement)

    controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.dampingFactor = 0.08
    Object.assign(controls, orbit)
    controls.update()

    addDefaultLights(scene)

    resizeObserver = new ResizeObserver(() => {
      resize()
    })
    resizeObserver.observe(sizeEl)

    animate()

    return { scene, camera, renderer, controls, raycaster, mouse, clock }
  }

  function animate() {
    animationId = requestAnimationFrame(animate)
    controls.update()
    const dt = Math.min(clock.getDelta(), 0.1)
    for (const fn of updateFns) {
      fn(dt, { scene, camera, renderer, controls, raycaster, mouse })
    }
    renderer.render(scene, camera)
  }

  function onUpdate(fn) {
    updateFns.add(fn)
  }

  function offUpdate(fn) {
    updateFns.delete(fn)
  }

  function dispose() {
    if (animationId) cancelAnimationFrame(animationId)
    if (resizeObserver) resizeObserver.disconnect()
    if (controls) controls.dispose()
    if (renderer) {
      renderer.dispose()
      if (renderer.domElement.parentNode) {
        renderer.domElement.parentNode.removeChild(renderer.domElement)
      }
    }
    updateFns.clear()
    containerEl = null
    sizeEl = null
  }

  onBeforeUnmount(dispose)

  return { init, dispose, onUpdate, offUpdate, resize }
}
