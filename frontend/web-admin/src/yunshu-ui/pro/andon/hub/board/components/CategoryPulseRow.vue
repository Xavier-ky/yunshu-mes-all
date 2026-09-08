<template>
  <div class="andon-category-pulse">
    <div
      v-for="cat in categories"
      :key="cat.key"
      class="andon-category-card"
      :class="[cat.tone, { 'is-active': activeKey === cat.key, 'is-zero': counts[cat.key] === 0 }]"
      @click="$emit('filter', activeKey === cat.key ? '' : cat.key)"
    >
      <span class="andon-category-card__label">{{ cat.label }}</span>
      <strong class="andon-category-card__count">{{ counts[cat.key] ?? 0 }}</strong>
    </div>
  </div>
</template>

<script>
import { CATEGORIES, countByCategory } from "../../../utils/category";

export default {
  name: "CategoryPulseRow",
  props: {
    alerts: { type: Array, default: () => [] },
    activeKey: { type: String, default: "" },
  },
  emits: ["filter"],
  computed: {
    categories() {
      return CATEGORIES;
    },
    counts() {
      return countByCategory(this.alerts);
    },
  },
};
</script>
