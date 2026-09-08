<template>
  <el-drawer
    v-model="visible"
    :with-header="false"
    size="78%"
    append-to-body
    destroy-on-close
    class="wm-bin-map-drawer"
    @closed="onClosed"
  >
    <div class="wm-bin-map-root">
      <header class="wm-bin-map-header">
        <div class="wm-bin-map-header-left">
          <span class="wm-bin-map-icon" aria-hidden="true">库</span>
          <div class="wm-bin-map-title-wrap">
            <span class="wm-bin-frame-corner wm-bin-frame-corner--tl" aria-hidden="true"></span>
            <span class="wm-bin-frame-corner wm-bin-frame-corner--tr" aria-hidden="true"></span>
            <span class="wm-bin-frame-corner wm-bin-frame-corner--bl" aria-hidden="true"></span>
            <span class="wm-bin-frame-corner wm-bin-frame-corner--br" aria-hidden="true"></span>
            <h3 class="wm-bin-map-title">库位态势图</h3>
            <p class="wm-bin-map-sub">Warehouse Bin Situation Map · 工业 HUD</p>
          </div>
        </div>
        <div class="wm-bin-map-warehouse-tabs">
          <button
            v-for="wh in sortedWarehouses"
            :key="wh.warehouseId"
            type="button"
            class="wm-bin-map-wh-tab"
            :class="{ active: String(activeWarehouseId) === String(wh.warehouseId) }"
            @click="switchWarehouse(wh.warehouseId)"
          >
            {{ wh.warehouseName }}
          </button>
        </div>
        <button type="button" class="wm-bin-map-close" @click="visible = false" aria-label="关闭">关闭</button>
      </header>

      <div class="wm-bin-map-zone-pills">
        <button
          v-for="z in zones"
          :key="z.locationId"
          type="button"
          class="wm-bin-map-zone-pill"
          :class="{ active: String(activeZoneId) === String(z.locationId) }"
          @click="switchZone(z.locationId)"
        >
          {{ z.locationName }}
        </button>
      </div>

      <BinMapKpiBar
        :summary="summary"
        :alert-only="alertOnly"
        :zone-name="activeZoneName"
        :grid-label="gridLabel"
        @toggle-alert="alertOnly = !alertOnly"
        @reset-filter="resetFilter"
      />

      <div class="wm-bin-map-main">
        <div class="wm-bin-map-grid-wrap">
          <span class="wm-bin-frame-corner wm-bin-frame-corner--tl" aria-hidden="true"></span>
          <span class="wm-bin-frame-corner wm-bin-frame-corner--tr" aria-hidden="true"></span>
          <span class="wm-bin-frame-corner wm-bin-frame-corner--bl" aria-hidden="true"></span>
          <span class="wm-bin-frame-corner wm-bin-frame-corner--br" aria-hidden="true"></span>
          <BinGridCanvas
            :bins="displayBins"
            :selected-id="selectedBin?.areaId"
            :alert-only="alertOnly"
            :loading="loading"
            @select="selectBin"
          />
        </div>
        <aside class="wm-bin-map-detail-wrap">
          <BinDetailPanel
            :bin="selectedBin"
            :zone-bins="displayBins"
            @locate="onLocate"
            @clear="selectedBin = null"
          />
        </aside>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { getBinMap } from "@/yunshu-ui/api/mes/wm/wmstock"
import { listWarehouse } from "@/yunshu-ui/api/mes/wm/warehouse"
import {
  gridSizeLabel,
  resolveDefaultWarehouse,
  resolveDefaultZone,
  sortWarehousesForBinMap,
  unwrapBinMap,
} from "./binMapUtils"
import BinMapKpiBar from "./BinMapKpiBar.vue"
import BinGridCanvas from "./BinGridCanvas.vue"
import BinDetailPanel from "./BinDetailPanel.vue"

export default {
  name: "WmBinMapDrawer",
  components: { BinMapKpiBar, BinGridCanvas, BinDetailPanel },
  emits: ["locate"],
  data() {
    return {
      visible: false,
      loading: false,
      alertOnly: false,
      warehouses: [],
      zones: [],
      bins: [],
      summary: {},
      warehouse: null,
      activeWarehouseId: null,
      activeZoneId: null,
      selectedBin: null,
    }
  },
  computed: {
    sortedWarehouses() {
      return sortWarehousesForBinMap(this.warehouses)
    },
    displayBins() {
      if (!this.activeZoneId) return this.bins
      return this.bins.filter((b) => String(b.locationId) === String(this.activeZoneId))
    },
    activeZoneName() {
      const z = this.zones.find((x) => String(x.locationId) === String(this.activeZoneId))
      return z?.locationName || ""
    },
    gridLabel() {
      return gridSizeLabel(this.displayBins)
    },
  },
  methods: {
    async open() {
      this.visible = true
      this.alertOnly = false
      this.selectedBin = null
      await this.loadWarehouses()
      const wh = resolveDefaultWarehouse(this.warehouses)
      if (wh?.warehouseId) {
        await this.switchWarehouse(wh.warehouseId)
      }
    },
    async loadWarehouses() {
      try {
        const res = await listWarehouse({ pageNum: 1, pageSize: 50 })
        this.warehouses = res.rows || []
      } catch {
        this.warehouses = []
      }
    },
    async switchWarehouse(warehouseId) {
      this.activeWarehouseId = warehouseId
      this.activeZoneId = null
      this.selectedBin = null
      await this.fetchBinMap()
      const zone = resolveDefaultZone(this.zones)
      if (zone?.locationId) {
        this.activeZoneId = zone.locationId
      }
    },
    switchZone(zoneId) {
      this.activeZoneId = zoneId
      this.selectedBin = null
    },
    async fetchBinMap() {
      if (!this.activeWarehouseId) return
      this.loading = true
      try {
        const res = await getBinMap({ warehouseId: this.activeWarehouseId })
        const data = unwrapBinMap(res)
        if (!data) return
        this.warehouse = data.warehouse || null
        this.zones = data.zones || []
        this.bins = data.bins || []
        this.summary = data.summary || {}
        if (!this.activeZoneId && this.zones.length) {
          const zone = resolveDefaultZone(this.zones)
          this.activeZoneId = zone?.locationId || this.zones[0].locationId
        }
      } catch (e) {
        this.$message?.error?.(e?.message || "库位态势加载失败")
        this.zones = []
        this.bins = []
        this.summary = {}
      } finally {
        this.loading = false
      }
    },
    selectBin(bin) {
      this.selectedBin = bin
    },
    resetFilter() {
      this.alertOnly = false
      this.selectedBin = null
    },
    onLocate(bin) {
      this.visible = false
      this.$emit("locate", {
        warehouseId: this.activeWarehouseId,
        areaCode: bin.areaCode,
        locationId: bin.locationId,
      })
    },
    onClosed() {
      this.resetFilter()
      this.bins = []
      this.zones = []
    },
  },
}
</script>
