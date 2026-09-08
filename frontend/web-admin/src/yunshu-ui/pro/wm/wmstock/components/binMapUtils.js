/** 库位态势图工具：坐标、状态、配色 */

export const DEFAULT_WAREHOUSE_CODE = "WH-RAW"
export const DEFAULT_ZONE_CODE = "RAW-ZONE-A"

export const BIN_STATUS = {
  empty: { label: "空闲", color: "#e8edf5", front: "#d1dae6", side: "#b8c4d4" },
  occupied: { label: "在库", color: "#2563eb", front: "#1d4ed8", side: "#1e40af" },
  low: { label: "低占用", color: "#93c5fd", front: "#60a5fa", side: "#3b82f6" },
  frozen: { label: "冻结", color: "#f59e0b", front: "#d97706", side: "#b45309" },
  expiring: { label: "临期", color: "#ef4444", front: "#dc2626", side: "#b91c1c" },
  disabled: { label: "停用", color: "#94a3b8", front: "#64748b", side: "#475569" },
}

export function parseBinGrid(areaCode) {
  if (!areaCode) return { x: 0, y: 0 }
  const m = String(areaCode).match(/([A-Z]+)-?([A-Z])(\d+)/i)
  if (!m) return { x: 0, y: 0 }
  const row = m[2] ? m[2].toUpperCase().charCodeAt(0) - 65 : 0
  const col = parseInt(m[3], 10) - 1
  return { x: Math.max(0, col), y: Math.max(0, row) }
}

export function resolvePosition(bin) {
  const px = bin.positionX ?? bin.position_x
  const py = bin.positionY ?? bin.position_y
  if (px != null && py != null) {
    return { x: Number(px), y: Number(py) }
  }
  return parseBinGrid(bin.areaCode)
}

export function getStatusMeta(status) {
  return BIN_STATUS[status] || BIN_STATUS.empty
}

export function isAbnormalStatus(status) {
  return ["frozen", "expiring", "disabled"].includes(status)
}

export function formatQty(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return "0"
  if (Number.isInteger(n)) return String(n)
  return n.toFixed(1)
}

export function shortBinCode(code) {
  if (!code) return "—"
  const parts = String(code).split("-")
  return parts.length >= 2 ? parts.slice(-2).join("-") : code
}

/** 构建带通道的网格行（在第2行后插入通道） */
export function buildGridRows(bins, aisleAfterRow = 1) {
  if (!bins.length) return { rows: [], cols: 0 }
  const positioned = bins.map((bin) => {
    const pos = resolvePosition(bin)
    return { ...bin, gridX: pos.x, gridY: pos.y }
  })
  const maxCol = Math.max(...positioned.map((b) => b.gridX), 0)
  const maxRow = Math.max(...positioned.map((b) => b.gridY), 0)
  const cols = maxCol + 1

  const rowMap = new Map()
  for (const bin of positioned) {
    const key = bin.gridY
    if (!rowMap.has(key)) rowMap.set(key, [])
    rowMap.get(key).push(bin)
  }

  const rows = []
  for (let r = 0; r <= maxRow; r++) {
    const cells = Array.from({ length: cols }, () => null)
    const rowBins = rowMap.get(r) || []
    for (const bin of rowBins) {
      if (bin.gridX < cols) cells[bin.gridX] = bin
    }
    rows.push({ type: "row", rowIndex: r, cells, label: String.fromCharCode(65 + r) })
    if (r === aisleAfterRow && r < maxRow) {
      rows.push({ type: "aisle", label: "主通道" })
    }
  }
  return { rows, cols }
}

export function buildTooltip(bin) {
  const meta = getStatusMeta(bin.status)
  const sku = bin.skuCount || 0
  const qty = formatQty(bin.quantityOnhand)
  const pct = bin.occupancyPct != null ? `${bin.occupancyPct}%` : "—"
  const items = (bin.stocks || []).slice(0, 2).map((s) => s.itemName || s.itemCode).join("、")
  const suffix = sku > 2 ? ` 等${sku}种` : ""
  return `${bin.areaCode} · ${meta.label} · ${qty} · 占用${pct}${items ? ` · ${items}${suffix}` : ""}`
}

export function unwrapBinMap(response) {
  const data = response?.data ?? response
  if (!data || typeof data !== "object") return null
  return data
}

export function resolveDefaultWarehouse(warehouses) {
  if (!warehouses?.length) return null
  return (
    warehouses.find((w) => w.warehouseCode === DEFAULT_WAREHOUSE_CODE) ||
    warehouses.find((w) => (w.warehouseName || "").includes("原料")) ||
    warehouses[0]
  )
}

const WAREHOUSE_TAB_ORDER = [DEFAULT_WAREHOUSE_CODE, "WH-FIN", "WH-SCRAP"]

/** 库位态势顶栏：原材料仓优先，其余按演示顺序 */
export function sortWarehousesForBinMap(warehouses) {
  if (!warehouses?.length) return []
  return [...warehouses].sort((a, b) => {
    const ia = WAREHOUSE_TAB_ORDER.indexOf(a.warehouseCode || "")
    const ib = WAREHOUSE_TAB_ORDER.indexOf(b.warehouseCode || "")
    const pa = ia >= 0 ? ia : 999
    const pb = ib >= 0 ? ib : 999
    if (pa !== pb) return pa - pb
    return String(a.warehouseName || "").localeCompare(String(b.warehouseName || ""), "zh")
  })
}

export function resolveDefaultZone(zones) {
  if (!zones?.length) return null
  return (
    zones.find((z) => z.locationCode === DEFAULT_ZONE_CODE) ||
    zones.find((z) => (z.locationName || "").includes("原料A")) ||
    zones[0]
  )
}

export function gridSizeLabel(bins) {
  if (!bins?.length) return ""
  const positioned = bins.map((b) => resolvePosition(b))
  const cols = Math.max(...positioned.map((p) => p.x), 0) + 1
  const rows = Math.max(...positioned.map((p) => p.y), 0) + 1
  return `${cols}×${rows}`
}
