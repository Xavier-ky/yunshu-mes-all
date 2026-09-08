<template>
  <div class="ld-root">
    <div class="ld-top">
      <div class="ld-hero">
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div class="ld-bar">
        <slot name="toolbar" />
      </div>
    </div>

    <div class="ld-stats" v-if="$slots.stats">
      <slot name="stats" />
    </div>

    <div class="ld-main">
      <aside class="ld-left">
        <slot name="list" />
      </aside>
      <section class="ld-right">
        <slot name="detail" />
      </section>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: "" },
  description: { type: String, default: "" },
});
</script>

<style scoped>
.ld-root {
  display: grid;
  gap: 16px;
  min-height: 0;
}

.ld-top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.ld-hero h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #111;
}

.ld-hero p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.ld-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ld-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.ld-main {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 14px;
  min-height: 520px;
  align-items: stretch;
}

.ld-left,
.ld-right {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.5), 0 8px 24px rgba(180, 190, 205, 0.12);
  min-height: 0;
  overflow: hidden;
}

.ld-left {
  display: flex;
  flex-direction: column;
}

.ld-right {
  display: flex;
  flex-direction: column;
}

@media (max-width: 960px) {
  .ld-main {
    grid-template-columns: 1fr;
  }
}
</style>
