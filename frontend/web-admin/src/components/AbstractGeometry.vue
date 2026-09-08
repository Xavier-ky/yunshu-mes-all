<template>
  <div ref="mountRef" class="geo-root" :style="{ opacity: opacity }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import * as THREE from "three";

const props = defineProps({
  speed: { type: Number, default: 0.5 },
  scale: { type: Number, default: 1 },
  opacity: { type: Number, default: 1 },
});

const mountRef = ref(null);
let renderer;
let scene;
let camera;
let group;
let blade;
let rafId;

function cloverShape(size = 1, segs = 240) {
  const shape = new THREE.Shape();
  const points = [];
  for (let index = 0; index <= segs; index += 1) {
    const angle = (index / segs) * Math.PI * 2;
    const radius = Math.abs(Math.cos(angle * 2)) * size;
    points.push(
      new THREE.Vector2(Math.cos(angle) * radius, Math.sin(angle) * radius),
    );
  }
  shape.moveTo(points[0].x, points[0].y);
  for (let index = 1; index < points.length; index += 1) {
    shape.lineTo(points[index].x, points[index].y);
  }
  return shape;
}

function buildEnvMap(webGlRenderer) {
  const pmrem = new THREE.PMREMGenerator(webGlRenderer);
  const envScene = new THREE.Scene();

  const bgGeo = new THREE.SphereGeometry(20, 64, 32);
  const bgMat = new THREE.MeshBasicMaterial({
    color: 0xcccccc,
    side: THREE.BackSide,
  });
  envScene.add(new THREE.Mesh(bgGeo, bgMat));

  const topLight = new THREE.Mesh(
    new THREE.PlaneGeometry(8, 8),
    new THREE.MeshBasicMaterial({ color: 0xdddddd, side: THREE.DoubleSide }),
  );
  topLight.position.set(0, 6, 2);
  topLight.rotation.x = -0.5;
  envScene.add(topLight);

  const sideLight = new THREE.Mesh(
    new THREE.PlaneGeometry(2, 6),
    new THREE.MeshBasicMaterial({ color: 0xaabbcc, side: THREE.DoubleSide }),
  );
  sideLight.position.set(5, 1, 0);
  sideLight.rotation.y = -0.8;
  envScene.add(sideLight);

  const darkPanel = new THREE.Mesh(
    new THREE.PlaneGeometry(4, 6),
    new THREE.MeshBasicMaterial({ color: 0x222233, side: THREE.DoubleSide }),
  );
  darkPanel.position.set(-4, 0, -2);
  darkPanel.rotation.y = 1.2;
  envScene.add(darkPanel);

  const renderTarget = pmrem.fromScene(envScene, 0.04);
  pmrem.dispose();
  envScene.clear();
  return renderTarget.texture;
}

function init() {
  const element = mountRef.value;
  if (!element || renderer) {
    return;
  }
  const width = element.clientWidth;
  const height = element.clientHeight;

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(width, height);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = 1.0;
  element.appendChild(renderer.domElement);

  scene = new THREE.Scene();
  scene.environment = buildEnvMap(renderer);
  scene.background = null;

  camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
  camera.position.set(0, 0, 8);
  camera.lookAt(0, 0, 0);

  const ambient = new THREE.AmbientLight(0xffffff, 1.2);
  scene.add(ambient);

  const key = new THREE.DirectionalLight(0xffffff, 5);
  key.position.set(5, 4, 6);
  scene.add(key);

  const fill = new THREE.DirectionalLight(0xaaccff, 2);
  fill.position.set(-3, 1, 2);
  scene.add(fill);

  const rim = new THREE.DirectionalLight(0xffffff, 3);
  rim.position.set(0, -2, -4);
  scene.add(rim);

  group = new THREE.Group();
  group.scale.setScalar(props.scale);
  scene.add(group);

  blade = new THREE.Group();
  blade.rotation.x = -0.25;
  blade.rotation.y = 0.2;
  group.add(blade);

  const extrudeOpts = {
    steps: 1,
    depth: 0.08,
    bevelEnabled: true,
    bevelThickness: 0.08,
    bevelSize: 0.08,
    bevelSegments: 4,
  };

  const clover = cloverShape(2.4, 240);
  const bladeGeo = new THREE.ExtrudeGeometry(clover, extrudeOpts);
  bladeGeo.center();

  const bladeMat = new THREE.MeshStandardMaterial({
    color: 0x888888,
    metalness: 0.9,
    roughness: 0.22,
  });
  blade.add(new THREE.Mesh(bladeGeo, bladeMat));

  const hubGeo = new THREE.SphereGeometry(0.22, 32, 32);
  const hubMat = new THREE.MeshStandardMaterial({
    color: 0x555555,
    metalness: 0.94,
    roughness: 0.14,
  });
  blade.add(new THREE.Mesh(hubGeo, hubMat));

  const count = 36;
  const dotGeometry = new THREE.BufferGeometry();
  const dotPositions = new Float32Array(count * 3);
  const dotData = [];
  for (let index = 0; index < count; index += 1) {
    const angle = Math.random() * Math.PI * 2;
    const distance = 1.2 + Math.random() * 2.8;
    dotPositions[index * 3] = Math.cos(angle) * distance;
    dotPositions[index * 3 + 1] = Math.sin(angle) * distance;
    dotPositions[index * 3 + 2] = (Math.random() - 0.5) * 0.4;
    dotData.push({
      angle,
      dist: distance,
      z: dotPositions[index * 3 + 2],
      spd: 0.15 + Math.random() * 0.4,
    });
  }
  dotGeometry.setAttribute(
    "position",
    new THREE.BufferAttribute(dotPositions, 3),
  );
  const dotMaterial = new THREE.PointsMaterial({
    color: 0x444444,
    size: 0.04,
    transparent: true,
    opacity: 0.5,
    depthWrite: false,
  });
  const dots = new THREE.Points(dotGeometry, dotMaterial);
  group.add(dots);

  let last = 0;
  function animate(timestamp) {
    const delta = Math.min((timestamp - last) / 1000, 0.1);
    last = timestamp;

    blade.rotation.z += delta * 2.8 * props.speed;

    const positions = dots.geometry.attributes.position.array;
    for (let index = 0; index < count; index += 1) {
      const dot = dotData[index];
      dot.dist += delta * dot.spd * props.speed;
      if (dot.dist > 4) {
        dot.dist = 1.2;
      }
      const angle = dot.angle + blade.rotation.z * 0.08;
      positions[index * 3] = Math.cos(angle) * dot.dist;
      positions[index * 3 + 1] = Math.sin(angle) * dot.dist;
      positions[index * 3 + 2] = dot.z + Math.sin(dot.dist * 1.2) * 0.06;
    }
    dots.geometry.attributes.position.needsUpdate = true;

    renderer.render(scene, camera);
    rafId = requestAnimationFrame(animate);
  }
  rafId = requestAnimationFrame(animate);

  const onResize = () => {
    const resizedWidth = element.clientWidth;
    const resizedHeight = element.clientHeight;
    camera.aspect = resizedWidth / resizedHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(resizedWidth, resizedHeight);
  };
  window.addEventListener("resize", onResize);
  element._cleanup = () => window.removeEventListener("resize", onResize);
}

function cleanup() {
  cancelAnimationFrame(rafId);
  renderer?.dispose();
  renderer = null;
  scene = null;
  camera = null;
  group = null;
  blade = null;
  if (mountRef.value) {
    mountRef.value.innerHTML = "";
    if (mountRef.value._cleanup) {
      mountRef.value._cleanup();
    }
  }
}

onMounted(init);
onBeforeUnmount(cleanup);

watch(
  () => props.scale,
  (value) => {
    if (group) {
      group.scale.setScalar(value);
    }
  },
);
</script>

<style scoped>
.geo-root {
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>
