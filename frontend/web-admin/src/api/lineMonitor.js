import { request } from "@/api/request";

// ---- Mock fallback data ----
// When the backend is unavailable, these demo datasets drive the 3D views.

const MOCK_SUMMARY = {
  data: {
    refreshedAt: "10:32:15",
    currentShiftName: "白班",
    metrics: [
      { code: "running_lines", label: "运行产线", value: "2", unit: "条", tone: "NORMAL" },
      { code: "idle_lines", label: "待机产线", value: "0", unit: "条", tone: "NORMAL" },
      { code: "fault_lines", label: "故障产线", value: "0", unit: "条", tone: "NORMAL" },
      { code: "active_wo", label: "在制工单", value: "3", unit: "单", tone: "NORMAL" },
      { code: "today_output", label: "今日产出", value: "595", unit: "台", tone: "NORMAL" },
      { code: "avg_oee", label: "平均OEE", value: "76.6", unit: "%", tone: "NORMAL" },
      { code: "open_andons", label: "待处理安灯", value: "3", unit: "件", tone: "WARNING" },
      { code: "fault_devices", label: "故障设备", value: "0", unit: "台", tone: "NORMAL" },
    ],
    lines: [
      // 3D 总览标记由 LineOverview3D DEFAULT_LINE_UV + 表面射线贴合定位
      {
        lineId: 1, lineCode: "L-001", lineName: "台扇组装一线", workshopName: "电风扇总装车间",
        lineStatus: "RUNNING", currentWorkOrderNo: "WO-20260701", productName: "40cm台扇A型",
        planQty: 500, completedQty: 320, progressPct: 64, oee: 82.5, oeeEstimated: false,
        stationCount: 4,
        stationRatio: { running: 2, warning: 0, fault: 0, changeover: 1, idle: 1 },
        openAndonCount: 1,
        modelPosX: 300, modelPosY: 10, modelPosZ: -1000,
      },
      {
        lineId: 2, lineCode: "L-002", lineName: "落地扇组装一线", workshopName: "电风扇总装车间",
        lineStatus: "RUNNING", currentWorkOrderNo: "WO-20260702", productName: "40cm落地扇A型",
        planQty: 300, completedQty: 180, progressPct: 60, oee: 76.3, oeeEstimated: true,
        stationCount: 6,
        stationRatio: { running: 1, warning: 1, fault: 0, changeover: 0, idle: 2 },
        openAndonCount: 0,
        modelPosX: 529, modelPosY: 10, modelPosZ: -2500,
      },
      {
        lineId: 3, lineCode: "L-003", lineName: "风扇组装线A-01", workshopName: "电风扇总装车间",
        lineStatus: "WARNING", currentWorkOrderNo: "WO-20260703", productName: "40cm落地电风扇A型",
        planQty: 200, completedQty: 95, progressPct: 47.5, oee: 71.0, oeeEstimated: true,
        stationCount: 8,
        stationRatio: { running: 1, warning: 1, fault: 0, changeover: 1, idle: 1 },
        openAndonCount: 2,
        modelPosX: 300, modelPosY: 10, modelPosZ: -4000,
      },
    ],
    alerts: [
      {
        andonId: 1, andonNo: "AD-001", lineId: 1, lineName: "台扇组装一线",
        message: "缺料安灯：M4螺丝库存不足", status: "OPEN", occurTime: "07-09 09:15",
        typeName: "缺料安灯", typeCode: "MATERIAL_SHORTAGE", priority: "HIGH",
        exceptionDesc: "M4螺丝库存不足", durationMinutes: 25,
      },
      {
        andonId: 2, andonNo: "AD-002", lineId: 3, lineName: "风扇组装线A-01",
        message: "质量安灯：设备噪音异常", status: "OPEN", occurTime: "07-09 10:42",
        typeName: "质量异常", typeCode: "QUALITY_ABNORMAL", priority: "HIGH",
        exceptionDesc: "设备噪音异常", durationMinutes: 8,
      },
      {
        andonId: 3, andonNo: "AD-003", lineId: 3, lineName: "风扇组装线A-01",
        message: "工艺安灯：扇叶动平衡", status: "OPEN", occurTime: "07-09 11:05",
        typeName: "工艺求助", typeCode: "PROCESS_HELP", priority: "NORMAL",
        exceptionDesc: "扇叶动平衡", durationMinutes: 5,
      },
    ],
    alertSummary: {
      total: 3,
      highPriorityCount: 2,
      lineCount: 2,
      maxDurationMinutes: 25,
      typeSlices: [
        { typeCode: "MATERIAL_SHORTAGE", typeName: "缺料安灯", count: 1 },
        { typeCode: "QUALITY_ABNORMAL", typeName: "质量异常", count: 1 },
        { typeCode: "PROCESS_HELP", typeName: "工艺求助", count: 1 },
      ],
    },
    equipmentStatus: [
      { status: "WORKING", count: 13 },
      { status: "STOP", count: 3 },
      { status: "REPAIR", count: 2 },
      { status: "NORMAL", count: 1 },
    ],
    equipmentSummary: {
      total: 19,
      runningCount: 14,
      runningRate: 73.7,
      faultCount: 5,
      avgOee: 64.0,
    },
  },
};

const MOCK_DETAILS = {
  1: {
    data: {
      refreshedAt: "10:32:15",
      currentShiftName: "白班",
      line: { lineId: 1, lineCode: "L-001", lineName: "台扇组装一线", workshopName: "电风扇总装车间", ratedCapacity: "800", capacityUnit: "台/班", status: "ENABLED" },
      stations: [
        { stationId: 1, stationCode: "ST-001", stationName: "底座装配工位", runStatus: "RUNNING", dispatchNo: "DT-001", operatorName: "张三", plannedQty: 125, completedQty: 80, deviceName: "螺丝机01", deviceStatus: "NORMAL" },
        { stationId: 2, stationCode: "ST-002", stationName: "电机安装工位", runStatus: "RUNNING", dispatchNo: "DT-002", operatorName: "李四", plannedQty: 125, completedQty: 72, deviceName: null, deviceStatus: null },
        { stationId: 3, stationCode: "ST-003", stationName: "扇叶装配工位", runStatus: "CHANGEOVER", dispatchNo: "DT-003", operatorName: "王五", plannedQty: 125, completedQty: 0, deviceName: null, deviceStatus: null },
        { stationId: 4, stationCode: "ST-004", stationName: "整机测试工位", runStatus: "WARNING", dispatchNo: "DT-004", operatorName: "赵六", plannedQty: 125, completedQty: 45, deviceName: "老化测试台01", deviceStatus: "NORMAL" },
      ],
      workOrders: [
        { workOrderId: 1, workOrderNo: "WO-20260701", productName: "40cm台扇A型", planQty: 500, completedQty: 320, status: "RUNNING" },
      ],
      dispatches: [
        { dispatchId: 1, dispatchNo: "DT-001", stationName: "底座装配工位", stepName: "电机装配", operatorName: "张三", plannedQty: 125, completedQty: 80, status: "RUNNING" },
        { dispatchId: 2, dispatchNo: "DT-002", stationName: "电机安装工位", stepName: "扇叶安装", operatorName: "李四", plannedQty: 125, completedQty: 72, status: "RUNNING" },
        { dispatchId: 3, dispatchNo: "DT-003", stationName: "扇叶装配工位", stepName: "老化测试", operatorName: "王五", plannedQty: 125, completedQty: 0, status: "DISPATCHED" },
        { dispatchId: 4, dispatchNo: "DT-004", stationName: "整机测试工位", stepName: "包装入箱", operatorName: "赵六", plannedQty: 125, completedQty: 45, status: "RUNNING" },
      ],
      andons: [
        { andonId: 1, andonNo: "AD-001", typeName: "缺料安灯", stationName: "底座装配工位", exceptionDesc: "M4螺丝库存不足", status: "OPEN", occurTime: "07-09 09:15" },
      ],
      hourlyOutput: [
        { hour: 8, qty: 12 }, { hour: 9, qty: 28 }, { hour: 10, qty: 45 },
        { hour: 11, qty: 52 }, { hour: 12, qty: 38 }, { hour: 13, qty: 41 },
        { hour: 14, qty: 55 }, { hour: 15, qty: 49 }, { hour: 16, qty: 0 }, { hour: 17, qty: 0 },
      ],
      oee: { availability: 88.5, performance: 79.2, quality: 97.1, oee: 68.0, estimated: false },
      events: [
        { eventType: "TASK", message: "START · PT-20260701", eventTime: "2026-07-09 08:05:00" },
        { eventType: "ANDON", message: "AD-001 · M4螺丝库存不足", eventTime: "2026-07-09 09:15:22" },
        { eventType: "TASK", message: "REPORT · PT-20260701", eventTime: "2026-07-09 10:30:00" },
      ],
    },
  },
  2: {
    data: {
      refreshedAt: "10:32:15",
      currentShiftName: "白班",
      line: { lineId: 2, lineCode: "L-002", lineName: "落地扇组装一线", workshopName: "电风扇总装车间", ratedCapacity: "600", capacityUnit: "台/班", status: "ENABLED" },
      stations: [
        { stationId: 5, stationCode: "ST-101", stationName: "底座装配工位", runStatus: "RUNNING", dispatchNo: "DT-101", operatorName: "孙七", plannedQty: 100, completedQty: 65, deviceName: "螺丝机02", deviceStatus: "NORMAL" },
        { stationId: 6, stationCode: "ST-102", stationName: "电机安装工位", runStatus: "RUNNING", dispatchNo: "DT-102", operatorName: "周八", plannedQty: 100, completedQty: 50, deviceName: null, deviceStatus: null },
        { stationId: 7, stationCode: "ST-103", stationName: "扇叶装配工位", runStatus: "WARNING", dispatchNo: "DT-103", operatorName: "吴九", plannedQty: 100, completedQty: 30, deviceName: "平衡机01", deviceStatus: "FAULT" },
        { stationId: 8, stationCode: "ST-104", stationName: "整机测试工位", runStatus: "IDLE", dispatchNo: null, operatorName: null, plannedQty: 0, completedQty: 0, deviceName: null, deviceStatus: null },
      ],
      workOrders: [
        { workOrderId: 2, workOrderNo: "WO-20260702", productName: "40cm落地扇A型", planQty: 300, completedQty: 180, status: "RUNNING" },
      ],
      dispatches: [
        { dispatchId: 5, dispatchNo: "DT-101", stationName: "底座装配工位", stepName: "底座装配", operatorName: "孙七", plannedQty: 100, completedQty: 65, status: "RUNNING" },
        { dispatchId: 6, dispatchNo: "DT-102", stationName: "电机安装工位", stepName: "电机安装", operatorName: "周八", plannedQty: 100, completedQty: 50, status: "RUNNING" },
        { dispatchId: 7, dispatchNo: "DT-103", stationName: "扇叶装配工位", stepName: "扇叶装配", operatorName: "吴九", plannedQty: 100, completedQty: 30, status: "RUNNING" },
      ],
      andons: [],
      hourlyOutput: [
        { hour: 8, qty: 8 }, { hour: 9, qty: 22 }, { hour: 10, qty: 35 },
        { hour: 11, qty: 42 }, { hour: 12, qty: 28 }, { hour: 13, qty: 20 },
        { hour: 14, qty: 25 }, { hour: 15, qty: 0 }, { hour: 16, qty: 0 }, { hour: 17, qty: 0 },
      ],
      oee: { availability: 82.0, performance: 71.5, quality: 95.0, oee: 55.7, estimated: true },
      events: [
        { eventType: "TASK", message: "START · PT-20260702", eventTime: "2026-07-09 08:10:00" },
        { eventType: "TASK", message: "REPORT · PT-20260702", eventTime: "2026-07-09 09:45:00" },
      ],
    },
  },
  3: {
    data: {
      refreshedAt: "10:32:15",
      currentShiftName: "白班",
      line: { lineId: 3, lineCode: "L-003", lineName: "风扇组装线A-01", workshopName: "电风扇总装车间", ratedCapacity: "500", capacityUnit: "台/班", status: "ENABLED" },
      stations: [
        { stationId: 9, stationCode: "ST-201", stationName: "底座预装工位", runStatus: "RUNNING", dispatchNo: "DT-201", operatorName: "郑十", plannedQty: 80, completedQty: 42, deviceName: "螺丝机03", deviceStatus: "NORMAL" },
        { stationId: 10, stationCode: "ST-202", stationName: "电机组装工位", runStatus: "RUNNING", dispatchNo: "DT-202", operatorName: "冯一", plannedQty: 80, completedQty: 38, deviceName: null, deviceStatus: null },
        { stationId: 11, stationCode: "ST-203", stationName: "扇叶安装工位", runStatus: "WARNING", dispatchNo: "DT-203", operatorName: "陈二", plannedQty: 80, completedQty: 28, deviceName: "平衡机02", deviceStatus: "FAULT" },
        { stationId: 12, stationCode: "ST-204", stationName: "防护网装配工位", runStatus: "RUNNING", dispatchNo: "DT-204", operatorName: "褚三", plannedQty: 80, completedQty: 55, deviceName: null, deviceStatus: null },
        { stationId: 13, stationCode: "ST-205", stationName: "电气测试工位", runStatus: "CHANGEOVER", dispatchNo: "DT-205", operatorName: "卫四", plannedQty: 80, completedQty: 0, deviceName: "测试台02", deviceStatus: "NORMAL" },
        { stationId: 14, stationCode: "ST-206", stationName: "噪音检测工位", runStatus: "IDLE", dispatchNo: null, operatorName: null, plannedQty: 0, completedQty: 0, deviceName: "噪音计01", deviceStatus: "NORMAL" },
        { stationId: 15, stationCode: "ST-207", stationName: "外观检查工位", runStatus: "IDLE", dispatchNo: null, operatorName: null, plannedQty: 0, completedQty: 0, deviceName: null, deviceStatus: null },
        { stationId: 16, stationCode: "ST-208", stationName: "包装工位", runStatus: "IDLE", dispatchNo: null, operatorName: null, plannedQty: 0, completedQty: 0, deviceName: "打包机01", deviceStatus: "NORMAL" },
      ],
      workOrders: [
        { workOrderId: 3, workOrderNo: "WO-20260703", productName: "40cm落地电风扇A型", planQty: 200, completedQty: 95, status: "RUNNING" },
      ],
      dispatches: [
        { dispatchId: 9, dispatchNo: "DT-201", stationName: "底座预装工位", stepName: "底座预装", operatorName: "郑十", plannedQty: 80, completedQty: 42, status: "RUNNING" },
        { dispatchId: 10, dispatchNo: "DT-202", stationName: "电机组装工位", stepName: "电机组装", operatorName: "冯一", plannedQty: 80, completedQty: 38, status: "RUNNING" },
        { dispatchId: 11, dispatchNo: "DT-203", stationName: "扇叶安装工位", stepName: "扇叶安装", operatorName: "陈二", plannedQty: 80, completedQty: 28, status: "RUNNING" },
        { dispatchId: 12, dispatchNo: "DT-204", stationName: "防护网装配工位", stepName: "防护网装配", operatorName: "褚三", plannedQty: 80, completedQty: 55, status: "RUNNING" },
      ],
      andons: [
        { andonId: 2, andonNo: "AD-002", typeName: "设备安灯", stationName: "扇叶安装工位", exceptionDesc: "平衡机异常噪音", status: "OPEN", occurTime: "07-09 10:42" },
        { andonId: 3, andonNo: "AD-003", typeName: "工艺安灯", stationName: "扇叶安装工位", exceptionDesc: "扇叶动平衡超标", status: "OPEN", occurTime: "07-09 11:05" },
      ],
      hourlyOutput: [
        { hour: 8, qty: 5 }, { hour: 9, qty: 15 }, { hour: 10, qty: 22 },
        { hour: 11, qty: 28 }, { hour: 12, qty: 12 }, { hour: 13, qty: 8 },
        { hour: 14, qty: 5 }, { hour: 15, qty: 0 }, { hour: 16, qty: 0 }, { hour: 17, qty: 0 },
      ],
      oee: { availability: 72.0, performance: 58.3, quality: 94.2, oee: 39.5, estimated: true },
      events: [
        { eventType: "TASK", message: "START · PT-20260703", eventTime: "2026-07-09 08:15:00" },
        { eventType: "ANDON", message: "AD-002 · 平衡机异常噪音", eventTime: "2026-07-09 10:42:08" },
        { eventType: "ANDON", message: "AD-003 · 扇叶动平衡超标", eventTime: "2026-07-09 11:05:33" },
      ],
    },
  },
};

const MOCK_DETAIL_DEFAULT = {
  data: {
    refreshedAt: "10:32:15",
    currentShiftName: "白班",
    line: { lineId: 0, lineCode: "L-???", lineName: "未知产线", workshopName: "-", ratedCapacity: "0", capacityUnit: "台", status: "DISABLED" },
    stations: [],
    workOrders: [],
    dispatches: [],
    andons: [],
    hourlyOutput: [],
    oee: { availability: 0, performance: 0, quality: 0, oee: 0, estimated: true },
    events: [],
  },
};

export const fetchLineMonitorSummary = () =>
  request.get("/factory/lines/monitor/summary");

export const fetchLineMonitorDetail = (lineId) =>
  request.get(`/factory/lines/${lineId}/monitor`);
