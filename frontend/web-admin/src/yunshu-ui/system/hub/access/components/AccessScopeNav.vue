<template>
  <div class="access-scope-nav stock-aside-tree--fill">
    <div class="access-scope-nav__head">
      <span class="access-scope-nav__title">{{ title }}</span>
      <span v-if="depthHint" class="access-scope-nav__depth-hint">{{ depthHint }}</span>
      <div class="access-scope-nav__actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="access-scope-nav__search">
      <slot name="search">
        <el-input
          :model-value="searchText"
          :placeholder="searchPlaceholder"
          clearable
          size="default"
          prefix-icon="el-icon-search"
          @update:model-value="$emit('update:searchText', $event)"
        />
      </slot>
    </div>

    <div class="access-scope-nav__crumb" :title="crumbText">
      {{ crumbText }}
    </div>

    <div class="access-scope-nav__insight" :class="{ 'access-scope-nav__insight--stack': insightLayout === 'stack' }">
      <div
        v-for="(item, idx) in insightItems"
        :key="item.key"
        class="access-scope-nav__insight-item"
        :class="{ 'is-last': idx === insightItems.length - 1 }"
      >
        <span class="access-scope-nav__insight-label">{{ item.label }}</span>
        <strong class="access-scope-nav__insight-value" :title="item.title ?? String(item.value ?? '')">{{ item.value }}</strong>
      </div>
    </div>

    <div class="access-scope-nav__frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <slot />
    </div>

    <div v-if="showStatusFilter || $slots.footer" class="access-scope-nav__footer">
      <slot name="footer">
        <div v-if="showStatusFilter" class="access-scope-nav__status">
          <div class="access-scope-nav__status-head">
            <span>状态筛选</span>
          </div>
          <div class="status-filter-segment" role="group" aria-label="状态筛选">
            <button type="button" :class="{ active: status == null || status === '' }" @click="$emit('update:status', null)">全部</button>
            <button type="button" :class="{ active: status === '0' }" @click="$emit('update:status', '0')">正常</button>
            <button type="button" :class="{ active: status === '1' }" @click="$emit('update:status', '1')">停用</button>
          </div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script>
export default {
  name: "AccessScopeNav",
  props: {
    title: { type: String, required: true },
    depthHint: { type: String, default: "" },
    crumbText: { type: String, default: "" },
    insightItems: { type: Array, default: () => [] },
    insightLayout: { type: String, default: "inline" },
    searchText: { type: String, default: "" },
    searchPlaceholder: { type: String, default: "搜索" },
    showStatusFilter: { type: Boolean, default: false },
    status: { type: String, default: null },
  },
  emits: ["update:searchText", "update:status"],
};
</script>
