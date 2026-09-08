<template>
  <div ref="mountRef" class="geo-root" :style="{ opacity: opacity }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
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
let fanGroup;
let bladeGroup;
let particles;
let rafId;
let onResize;

function cloverShape(scale = 1, segments = 120) {
  const shape = new THREE.Shape();
  const points = [];
  for (let index = 0; index <= segments; index += 1) {
    const angle = (index / segments) * Math.PI * 2;
    const radius = Math.abs(Math.cos(angle * 2)) * scale;
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
  element.appendChild(renderer.domElement);

  scene = new THREE.Scene();
  camera = new THREE.PerspectiveCamera(42, width / height, 0.1, 100);
  camera.position.set(0, 0, 6);

  fanGroup = new THREE.Group();
  fanGroup.scale.setScalar(props.scale);
  scene.add(fanGroup);

  bladeGroup = new THREE.Group();
  fanGroup.add(bladeGroup);

  const clover = cloverShape(2.4, 160);
  const extrudeSettings = {
    steps: 1,
    depth: 0.06,
    bevelEnabled: true,
    bevelThickness: 0.03,
    bevelSize: 0.03,
    bevelSegments: 1,
  };
  const geometry = new THREE.ExtrudeGeometry(clover, extrudeSettings);
  geometry.center();

  const wireMaterial = new THREE.MeshBasicMaterial({
    color: 0x1a1a1a,
    wireframe: true,
    transparent: true,
    opacity: 0.13,
  });
  const wireClover = new THREE.Mesh(geometry, wireMaterial);
  bladeGroup.add(wireClover);

  const edgeGeometry = new THREE.EdgesGeometry(geometry);
  const edgeMaterial = new THREE.LineBasicMaterial({
    color: 0x1a1a1a,
    transparent: true,
    opacity: 0.35,
  });
  const edgeLine = new THREE.LineSegments(edgeGeometry, edgeMaterial);
  bladeGroup.add(edgeLine);

  const axisGeometry = new THREE.CylinderGeometry(0.06, 0.06, 1.6, 16);
  const axisMaterial = new THREE.MeshBasicMaterial({
    color: 0x1a1a1a,
    transparent: true,
    opacity: 0.3,
  });
  const axis = new THREE.Mesh(axisGeometry, axisMaterial);
  bladeGroup.add(axis);

  const hubGeometry = new THREE.SphereGeometry(0.18, 24, 24);
  const hubMaterial = new THREE.MeshBasicMaterial({
    color: 0x1a1a1a,
    transparent: true,
    opacity: 0.45,
  });
  const hub = new THREE.Mesh(hubGeometry, hubMaterial);
  bladeGroup.add(hub);

  const particleCount = 40;
  const particleGeometry = new THREE.BufferGeometry();
  const positions = new Float32Array(particleCount * 3);
  const particleData = [];
  for (let index = 0; index < particleCount; index += 1) {
    const angle = Math.random() * Math.PI * 2;
    const distance = 1.2 + Math.random() * 4;
    positions[index * 3] = Math.cos(angle) * distance;
    positions[index * 3 + 1] = Math.sin(angle) * distance;
    positions[index * 3 + 2] = (Math.random() - 0.5) * 0.8;
    particleData.push({
      baseAngle: angle,
      baseDist: distance,
      speed: 0.3 + Math.random() * 0.7,
      zBase: positions[index * 3 + 2],
    });
  }
  particleGeometry.setAttribute(
    "position",
    new THREE.BufferAttribute(positions, 3),
  );
  const particleMaterial = new THREE.PointsMaterial({
    color: 0x1a1a1a,
    size: 0.03,
    transparent: true,
    opacity: 0.22,
  });
  particles = new THREE.Points(particleGeometry, particleMaterial);
  fanGroup.add(particles);

  let last = 0;
  function animate(timestamp) {
    const delta = Math.min((timestamp - last) / 1000, 0.1);
    last = timestamp;

    bladeGroup.rotation.z += delta * 2.6 * props.speed;

    const positionArray = particles.geometry.attributes.position.array;
    for (let index = 0; index < particleCount; index += 1) {
      const particle = particleData[index];
      particle.baseDist += delta * particle.speed * props.speed;
      if (particle.baseDist > 5.2) {
        particle.baseDist = 1.2;
      }
      const angle = particle.baseAngle + bladeGroup.rotation.z * 0.18;
      positionArray[index * 3] = Math.cos(angle) * particle.baseDist;
      positionArray[index * 3 + 1] = Math.sin(angle) * particle.baseDist;
      positionArray[index * 3 + 2] =
        particle.zBase + Math.sin(particle.baseDist * 1.2) * 0.15;
    }
    particles.geometry.attributes.position.needsUpdate = true;

    renderer.render(scene, camera);
    rafId = requestAnimationFrame(animate);
  }
  rafId = requestAnimationFrame(animate);

  onResize = () => {
    if (!mountRef.value || !renderer || !camera) {
      return;
    }
    const resizedWidth = mountRef.value.clientWidth;
    const resizedHeight = mountRef.value.clientHeight;
    camera.aspect = resizedWidth / resizedHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(resizedWidth, resizedHeight);
  };
  window.addEventListener("resize", onResize);
}

function cleanup() {
  cancelAnimationFrame(rafId);
  if (onResize) {
    window.removeEventListener("resize", onResize);
  }
  renderer?.dispose();
  renderer = null;
  scene = null;
  camera = null;
  fanGroup = null;
  bladeGroup = null;
  particles = null;
  if (mountRef.value) {
    mountRef.value.innerHTML = "";
  }
}

onMounted(init);
onBeforeUnmount(cleanup);

watch(
  () => props.scale,
  (value) => {
    if (fanGroup) {
      fanGroup.scale.setScalar(value);
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
