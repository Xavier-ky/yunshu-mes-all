
export const BASE_URL = 'http://localhost:8080'
export const APP_NAME = '云枢智造 MES'

export const STATUS_COLOR_MAP = {
  NORMAL: 'green',
  COMPLETED: 'green',
  QUALIFIED: 'green',
  PENDING: 'blue',
  PROCESSING: 'blue',
  WARNING: 'orange',
  WAITING_RESPONSE: 'orange',
  ABNORMAL: 'red',
  UNQUALIFIED: 'red',
  FAULT: 'red',
  TIMEOUT: 'red',
  CLOSED: 'gray',
  CANCELLED: 'gray'
}

export const ROLE_MAP = {
  LINE_OPERATOR: '产线操作工',
  QUALITY_INSPECTOR: '质检员',
  WAREHOUSE_KEEPER: '仓库物料员',
  EQUIPMENT_MAINTAINER: '设备维修员',
  PRODUCTION_SUPERVISOR: '生产主管',
  MANAGER: '管理层'
}

export const TASK_TYPE_MAP = {
  PRODUCTION: '生产任务',
  QUALITY: '质检任务',
  ANDON: '安灯处理',
  REPAIR: '维修任务',
  MATERIAL: '物料任务'
}

export const TASK_STATUS_MAP = {
  PENDING: '待处理',
  PROCESSING: '进行中',
  COMPLETED: '已完成',
  ABNORMAL: '异常'
}

export const INSPECTION_TYPE_MAP = {
  FIRST_INSPECTION: '首检',
  PATROL_INSPECTION: '巡检',
  FINAL_INSPECTION: '完工检验',
  WAREHOUSE_INSPECTION: '入库检验'
}

export const EQUIPMENT_STATUS_MAP = {
  RUNNING: '运行',
  STOPPED: '停机',
  FAULT: '故障',
  REPAIRING: '维修中',
  MAINTAINING: '保养中'
}

export const ANDON_STATUS_MAP = {
  INITIATED: '已发起',
  RESPONDED: '已响应',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
}

export const ANDON_EVENT_TYPE_MAP = {
  MATERIAL: '缺料安灯',
  EQUIPMENT: '设备故障安灯',
  PROCESS: '工艺求助安灯',
  QUALITY: '品质异常安灯'
}

export const ANDON_TARGET = {
  MATERIAL:  { role: 'WAREHOUSE_KEEPER',     roleName: '仓库物料员', defaultPerson: '王五' },
  EQUIPMENT: { role: 'EQUIPMENT_MAINTAINER', roleName: '设备维修员', defaultPerson: '赵六' },
  PROCESS:   { role: 'PRODUCTION_SUPERVISOR',roleName: '生产主管',   defaultPerson: '王主管' },
  QUALITY:   { role: 'QUALITY_INSPECTOR',    roleName: '质检员',     defaultPerson: '李四' }
}

export const ANDON_REASON_MAP = {
  MATERIAL_SHORTAGE: '物料短缺',
  MATERIAL_DEFECT: '物料缺陷',
  MACHINE_FAULT: '设备故障',
  MACHINE_STOP: '设备停机',
  PROCESS_CONFUSION: '工艺不明确',
  PROCESS_TOOLING: '工装异常',
  QUALITY_DEFECT: '品质缺陷',
  QUALITY_BATCH: '批次异常',
  OTHER: '其他原因'
}

export const SCAN_PREFIX_MAP = {
  SN: { type: 'product_sn', name: '产品SN', page: '/pages/traceability/product/index' },
  MAT: { type: 'material_batch', name: '物料批次', page: '/pages/production/material-bind/index' },
  WO: { type: 'work_order', name: '工单', page: '/pages/production/task-detail/index' },
  EQ: { type: 'equipment', name: '设备', page: '/pages/equipment/detail/index' }
}

export const DEFECT_TYPE_MAP = {
  MOTOR_NOISE: '电机异响',
  BLADE_DAMAGE: '扇叶损坏',
  COVER_LOOSE: '网罩松动',
  SWITCH_FAULT: '开关故障',
  BASE_UNSTABLE: '底座不稳',
  SPEED_ABNORMAL: '转速异常',
  OTHER: '其他缺陷'
}

export const QUALITY_DEFECT_TYPE_MAP = {
  SCRATCH: '外观划痕',
  DIMENSION: '尺寸超差',
  MOTOR_NOISE: '电机异响',
  BLADE_DAMAGE: '扇叶损坏',
  COVER_LOOSE: '网罩松动',
  SWITCH_FAULT: '开关故障',
  SURFACE_RUST: '表面生锈',
  LABEL_ERROR: '标签错误',
  PACKAGE_DAMAGE: '包装破损',
  OTHER: '其他缺陷'
}

export const INSPECTION_ACTION_MAP = {
  RELEASE: '放行',
  REWORK: '返工',
  SCRAP: '报废'
}

export const REPAIR_RESULT_MAP = {
  COMPLETED: '已修复',
  PARTIAL: '部分修复',
  UNREPAIRABLE: '无法修复',
  OUTSOURCED: '需外协'
}

export const INSPECT_RESULT_MAP = {
  NORMAL: '正常',
  ABNORMAL: '异常',
  NEED_MAINTENANCE: '需保养'
}

export const FAULT_TYPE_MAP = {
  SCREW_JAM: '螺丝送料卡滞',
  MOTOR_OVERHEAT: '电机过热',
  BELT_BROKEN: '传动带断裂',
  SENSOR_FAULT: '传感器故障',
  CONTROL_FAULT: '控制器故障',
  OTHER: '其他故障'
}

export const PAGE_PATH_MAP = {
  login: '/pages/login/index',
  home: '/pages/home/index',
  tasks: '/pages/tasks/index',
  scan: '/pages/scan/index',
  message: '/pages/message/index',
  mine: '/pages/mine/index',
  productionTaskList: '/pages/production/task-list/index',
  productionTaskDetail: '/pages/production/task-detail/index',
  productionOperation: '/pages/production/operation/index',
  productionReport: '/pages/production/report/index',
  productionMaterialBind: '/pages/production/material-bind/index',
  qualityTaskList: '/pages/quality/task-list/index',
  qualityInspect: '/pages/quality/inspect/index',
  qualityDefect: '/pages/quality/defect/index',
  qualityDetail: '/pages/quality/detail/index',
  andonCreate: '/pages/andon/create/index',
  andonDetail: '/pages/andon/detail/index',
  andonHandle: '/pages/andon/handle/index',
  andonList: '/pages/andon/handle/index',
  equipmentStatus: '/pages/equipment/status/index',
  equipmentDetail: '/pages/equipment/detail/index',
  equipmentRepair: '/pages/equipment/repair/index',
  materialIssue: '/pages/inventory/material-issue/index',
  materialReturn: '/pages/inventory/material-return/index',
  inventoryTaskList: '/pages/inventory/task-list/index',
  inventoryTaskDetail: '/pages/inventory/task-detail/index',
  traceabilityProduct: '/pages/traceability/product/index',
  supervisorDashboard: '/pages/supervisor/dashboard/index',
  supervisorAbnormal: '/pages/supervisor/abnormal/index',
  trend: '/pages/trend/index',
  aiAssistant: '/pages/ai/chat/index',
  materialCall: '/pages/production/material-call/index',
  materialCallList: '/pages/inventory/material-calls/index',
  materialCallDetail: '/pages/inventory/material-calls/detail/index',
  returnDetail: '/pages/inventory/return-detail/index',
  repairDetail: '/pages/tasks/repair-detail/index',
  repairTaskList: '/pages/repair/task-list/index',
  chatList: '/pages/chat/list/index',
  chatDetail: '/pages/chat/detail/index',
  scheduling: '/pages/production/scheduling/index'
}

export const ROLE_ENTRIES = {
  LINE_OPERATOR: ['productionReport', 'qualityInspect', 'scan', 'andonCreate', 'traceabilityProduct'],
  QUALITY_INSPECTOR: ['qualityInspect', 'qualityDefect', 'traceabilityProduct', 'andonCreate'],
  WAREHOUSE_KEEPER: ['scan', 'materialIssue', 'materialReturn', 'traceabilityProduct'],
  EQUIPMENT_MAINTAINER: ['equipmentRepair', 'equipmentStatus', 'andonCreate'],
  PRODUCTION_SUPERVISOR: ['productionReport', 'qualityInspect', 'scan', 'andonCreate', 'equipmentRepair'],
  MANAGER: ['productionReport', 'qualityInspect', 'traceabilityProduct']
}

export const MESSAGE_TYPE_MAP = {
  ANDON: '安灯通知',
  QUALITY: '质检异常',
  EQUIPMENT: '设备故障',
  TASK: '任务提醒'
}

export const PRIORITY_MAP = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}