<template>
  <div class="andon-category-insight-row">
    <button
      v-for="cat in cards"
      :key="cat.key"
      type="button"
      class="andon-category-insight-card"
      :class="[cat.tone, { 'is-active': activeKey === cat.key, 'is-zero': cat.count === 0 }]"
      @click="$emit('filter', activeKey === cat.key ? '' : cat.key)"
    >
      <div class="andon-category-insight-card__top">
        <span class="andon-category-insight-card__label">{{ cat.label }}</span>
        <span class="andon-category-insight-card__count">{{ cat.count }}</span>
        <span class="andon-category-insight-card__pct">{{ cat.pct }}%</span>
      </div>
      <div class="andon-category-insight-card__bar">
        <div class="andon-category-insight-card__bar-fill" :style="{ width: `${cat.pct}%` }" />
      </div>
    </button>
  </div>
</template>

<script>
import { CATEGORIES, countByCategory } from "../../../utils/category";

export default {
  name: "AndonCategoryInsightRow",
  props: {
    alerts: { type: Array, default: () => [] },
    activeKey: { type: String, default: "" },
  },
  emits: ["filter"],
  computed: {
    cards() {
      const counts = countByCategory(this.alerts);
      const total = (this.alerts || []).length || 1;
      return CATEGORIES.map((c) => {
        const count = counts[c.key] ?? 0;
        return {
          ...c,
          count,
          pct: total > 0 ? Math.round((count / total) * 100) : 0,
        };
      });
    },
  },
};
</script>
