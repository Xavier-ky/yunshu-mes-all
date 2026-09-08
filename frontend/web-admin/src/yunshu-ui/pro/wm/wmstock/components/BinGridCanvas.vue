<template>
  <div class="bin-rack-canvas">
    <div v-if="loading" class="bin-rack-loading">加载货架态势...</div>
    <div v-else-if="!gridRows.length" class="bin-rack-empty">暂无库位数据</div>
    <div v-else class="bin-rack-stage">
      <div class="bin-rack-col-head" :style="{ gridTemplateColumns: `44px repeat(${cols}, minmax(78px, 1fr))` }">
        <span class="bin-rack-col-corner">层</span>
        <span v-for="c in colLabels" :key="'h'+c" class="bin-rack-col-tick">{{ c }}</span>
      </div>
      <div class="bin-rack-body">
        <template v-for="(row, ri) in gridRows">
          <div v-if="row.type === 'aisle'" :key="'aisle-' + ri" class="bin-rack-aisle">
            <span class="bin-rack-aisle-line bin-rack-aisle-line--left"></span>
            <span class="bin-rack-aisle-label">MAIN AISLE · 主通道</span>
            <span class="bin-rack-aisle-line bin-rack-aisle-line--right"></span>
          </div>
          <div v-else :key="'row-' + ri" class="bin-rack-shelf">
            <span class="bin-rack-row-tag">{{ row.label }}</span>
            <div class="bin-rack-beam">
              <div class="bin-rack-slots" :style="{ gridTemplateColumns: `repeat(${cols}, minmax(78px, 1fr))` }">
                <div
                  v-for="(cell, ci) in row.cells"
                  :key="'slot-'+ri+'-'+ci"
                  class="bin-rack-slot-wrap"
                >
                  <button
                    v-if="cell"
                    type="button"
                    class="bin-slot"
                    :class="slotClass(cell)"
                    :style="slotStyle(cell)"
                    :title="buildTooltip(cell)"
                    @click="$emit('select', cell)"
                    @mouseenter="hoverId = cell.areaId"
                    @mouseleave="hoverId = null"
                  >
                    <span v-if="isSelected(cell)" class="bin-slot-bracket bin-slot-bracket--tl"></span>
                    <span v-if="isSelected(cell)" class="bin-slot-bracket bin-slot-bracket--tr"></span>
                    <span v-if="isSelected(cell)" class="bin-slot-bracket bin-slot-bracket--bl"></span>
                    <span v-if="isSelected(cell)" class="bin-slot-bracket bin-slot-bracket--br"></span>
                    <span class="bin-slot-led" :class="'bin-slot-led--' + (cell.status || 'empty')"></span>
                    <span class="bin-slot-code">{{ shortCode(cell.areaCode) }}</span>
                    <div class="bin-slot-gauge">
                      <div
                        class="bin-slot-fill"
                        :class="'bin-slot-fill--' + (cell.status || 'empty')"
                        :style="{ height: gaugeHeight(cell) + '%' }"
                      ></div>
                    </div>
                    <span class="bin-slot-qty">{{ formatQty(cell.quantityOnhand) }}</span>
                    <span v-if="cell.skuCount > 0" class="bin-slot-sku">{{ cell.skuCount }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
    <div class="bin-rack-legend">
      <span v-for="(meta, key) in legendItems" :key="key" class="bin-rack-legend-item">
        <i class="bin-rack-legend-led" :class="'bin-slot-led--' + key"></i>
        {{ meta.label }}
      </span>
    </div>
  </div>
</template>

<script>
import {
  BIN_STATUS,
  buildGridRows,
  buildTooltip,
  formatQty,
  isAbnormalStatus,
  shortBinCode,
} from "./binMapUtils"

export default {
  name: "BinGridCanvas",
  props: {
    bins: { type: Array, default: () => [] },
    selectedId: { type: [Number, String], default: null },
    alertOnly: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ["select"],
  data() {
    return { hoverId: null, legendItems: BIN_STATUS }
  },
  computed: {
    gridLayout() {
      return buildGridRows(this.bins, 1)
    },
    gridRows() {
      return this.gridLayout.rows || []
    },
    cols() {
      return this.gridLayout.cols || 6
    },
    colLabels() {
      return Array.from({ length: this.cols }, (_, i) => String(i + 1).padStart(2, "0"))
    },
  },
  methods: {
    formatQty,
    shortCode: shortBinCode,
    buildTooltip,
    isSelected(cell) {
      return this.selectedId != null && String(this.selectedId) === String(cell.areaId)
    },
    gaugeHeight(cell) {
      const pct = Number(cell.occupancyPct) || 0
      if (cell.status === "empty") return 0
      return Math.max(8, Math.min(100, pct))
    },
    slotClass(cell) {
      const classes = [`bin-slot--${cell.status || "empty"}`]
      if (this.isSelected(cell)) classes.push("bin-slot--selected")
      if (this.alertOnly && !isAbnormalStatus(cell.status)) classes.push("bin-slot--dimmed")
      else if (this.alertOnly && isAbnormalStatus(cell.status)) classes.push("bin-slot--highlight")
      if (this.hoverId === cell.areaId) classes.push("bin-slot--hover")
      return classes
    },
    slotStyle(cell) {
      const idx = this.bins.indexOf(cell)
      return { "--i": idx >= 0 ? idx : 0 }
    },
  },
}
</script>
