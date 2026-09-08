<template>
  <div class="bin-detail-panel">
    <template v-if="bin">
      <div class="bin-detail-head">
        <div class="bin-detail-head-main">
          <span class="bin-detail-led" :class="'bin-slot-led--' + (bin.status || 'empty')"></span>
          <div>
            <h4 class="bin-detail-title">{{ bin.areaCode }}</h4>
            <p class="bin-detail-sub">{{ bin.areaName }}</p>
          </div>
        </div>
        <span class="bin-detail-status" :style="{ color: statusColor, borderColor: statusColor }">
          {{ statusLabel }}
        </span>
      </div>

      <div class="bin-detail-preview">
        <div class="bin-detail-preview-gauge">
          <div
            class="bin-detail-preview-fill"
            :class="'bin-slot-fill--' + (bin.status || 'empty')"
            :style="{ height: gaugeHeight + '%' }"
          ></div>
        </div>
        <div class="bin-detail-preview-meta">
          <span>占用 {{ bin.occupancyPct != null ? bin.occupancyPct + '%' : '—' }}</span>
          <span>在库 {{ formatQty(bin.quantityOnhand) }}</span>
        </div>
      </div>

      <div class="bin-detail-stats">
        <div class="bin-detail-stat-cell">
          <b>{{ formatQty(bin.quantityOnhand) }}</b><span>在库</span>
        </div>
        <div class="bin-detail-stat-cell">
          <b>{{ formatQty(bin.quantityReserved) }}</b><span>锁定</span>
        </div>
        <div class="bin-detail-stat-cell">
          <b>{{ bin.skuCount || 0 }}</b><span>SKU</span>
        </div>
        <div class="bin-detail-stat-cell">
          <b>{{ bin.occupancyPct != null ? bin.occupancyPct + '%' : '—' }}</b><span>占用率</span>
        </div>
      </div>

      <div class="bin-detail-section">
        <div class="bin-detail-section-title">库存明细</div>
        <div v-if="stocks.length" class="bin-detail-table-wrap">
          <table class="bin-detail-table">
            <thead>
              <tr>
                <th>物料编码</th>
                <th>名称</th>
                <th>批次</th>
                <th>数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in stocks" :key="i">
                <td :title="s.itemCode">{{ s.itemCode }}</td>
                <td :title="s.itemName">{{ s.itemName }}</td>
                <td :title="s.batchCode">{{ s.batchCode }}</td>
                <td>{{ formatQty(s.quantityOnhand) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="bin-detail-empty">该库位暂无库存</div>
      </div>

      <div class="bin-detail-section">
        <div class="bin-detail-section-title">库区占用趋势</div>
        <div class="bin-zone-bars">
          <div v-for="item in zoneBars" :key="item.areaId" class="bin-zone-bar-item">
            <div class="bin-zone-bar-head">
              <span :title="item.areaCode">{{ shortCode(item.areaCode) }}</span>
              <span>{{ item.occupancyPct }}%</span>
            </div>
            <div class="bin-zone-bar-track">
              <div
                class="bin-zone-bar-fill"
                :class="{ active: String(item.areaId) === String(bin.areaId) }"
                :style="{ width: item.occupancyPct + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <div class="bin-detail-actions">
        <button type="button" class="bin-detail-btn bin-detail-btn--primary" @click="$emit('locate', bin)">在表格中定位</button>
        <button type="button" class="bin-detail-btn" @click="$emit('clear')">取消选中</button>
      </div>
    </template>
    <div v-else class="bin-detail-placeholder">
      <div class="bin-detail-placeholder-led"></div>
      <p>点击左侧光槽库位</p>
      <p class="bin-detail-placeholder-sub">查看库存明细与占用情况</p>
    </div>
  </div>
</template>

<script>
import { formatQty, getStatusMeta, shortBinCode } from "./binMapUtils"

export default {
  name: "BinDetailPanel",
  props: {
    bin: { type: Object, default: null },
    zoneBins: { type: Array, default: () => [] },
  },
  emits: ["locate", "clear"],
  computed: {
    stocks() {
      return this.bin?.stocks || []
    },
    statusLabel() {
      return getStatusMeta(this.bin?.status).label
    },
    statusColor() {
      return getStatusMeta(this.bin?.status).color
    },
    gaugeHeight() {
      const pct = Number(this.bin?.occupancyPct) || 0
      if (this.bin?.status === "empty") return 0
      return Math.max(8, Math.min(100, pct))
    },
    zoneBars() {
      return [...(this.zoneBins || [])]
        .sort((a, b) => (b.occupancyPct || 0) - (a.occupancyPct || 0))
        .slice(0, 8)
    },
  },
  methods: {
    formatQty,
    shortCode: shortBinCode,
  },
}
</script>
