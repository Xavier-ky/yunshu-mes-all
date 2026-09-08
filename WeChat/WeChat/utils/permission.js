


import { getRoleCode } from './auth'
import { ROLE_MAP, PAGE_PATH_MAP } from './constants'


export const ROLE_CODES = {
  LINE_OPERATOR: 'LINE_OPERATOR',
  QUALITY_INSPECTOR: 'QUALITY_INSPECTOR',
  WAREHOUSE_KEEPER: 'WAREHOUSE_KEEPER',
  EQUIPMENT_MAINTAINER: 'EQUIPMENT_MAINTAINER',
  PRODUCTION_SUPERVISOR: 'PRODUCTION_SUPERVISOR',
  MANAGER: 'MANAGER'
}


const ENTRY_MATRIX = {
  [ROLE_CODES.LINE_OPERATOR]: [
    'productionReport', 'scan', 'andonCreate', 'myTasks',
    'materialCalls', 'equipmentRepair'
  ],
  [ROLE_CODES.QUALITY_INSPECTOR]: [
    'qualityTask', 'andonCreate',
    'defectRegister', 'scan', 'andonHandle'
  ],
  [ROLE_CODES.WAREHOUSE_KEEPER]: [
    'materialIssue', 'materialQuery',
    'inventoryTaskList', 'scan', 'materialCallList', 'andonHandle'
  ],
  [ROLE_CODES.EQUIPMENT_MAINTAINER]: [
    'equipmentStatus', 'repairTask', 'andonHandle',
    'scan', 'equipmentRepair'
  ],
  [ROLE_CODES.PRODUCTION_SUPERVISOR]: [
    'productionBoard', 'taskOverview', 'abnormalMonitor',
    'andonAbnormal', 'scan', 'scheduling',
    'andonCreate', 'qualityTask', 'equipmentStatus'
  ],
  [ROLE_CODES.MANAGER]: [
    'outputTrend',
    'productionBoard', 'scan', 'equipmentStatus', 'qualityTask'
  ]
}


export const ALL_ENTRIES = [
  {
    key: 'productionReport', label: '生产报工', icon: '⚙', color: 'blue',
    roles: [ROLE_CODES.LINE_OPERATOR], page: PAGE_PATH_MAP.productionTaskList
  },
  {
    key: 'scan', label: '扫码作业', icon: '🔍', iconImage: '/images/扫码_scan-code.svg', color: 'orange',
    roles: [ROLE_CODES.LINE_OPERATOR], page: PAGE_PATH_MAP.scan, isTab: false
  },
  {
    key: 'andonCreate', label: '发起安灯', icon: '⚠', color: 'red',
    roles: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.QUALITY_INSPECTOR], page: PAGE_PATH_MAP.andonCreate
  },
  {
    key: 'myTasks', label: '我的任务', icon: '📋', iconImage: '/images/任务.svg', color: 'blue',
    roles: [ROLE_CODES.LINE_OPERATOR], page: PAGE_PATH_MAP.tasks, isTab: false
  },
  {
    key: 'sopView', label: '数据看板', icon: '📊', color: 'blue',
    roles: [ROLE_CODES.LINE_OPERATOR], page: PAGE_PATH_MAP.productionTaskList
  },
  {
    key: 'qualityTask', label: '质检任务', icon: '✓', iconImage: '/images/质检.svg', color: 'green',
    roles: [ROLE_CODES.QUALITY_INSPECTOR], page: PAGE_PATH_MAP.qualityTaskList
  },
  {
    key: 'qualityInspect', label: '质检录入', icon: '✓', iconImage: '/images/质检.svg', color: 'green',
    roles: [ROLE_CODES.QUALITY_INSPECTOR], page: PAGE_PATH_MAP.qualityInspect
  },
  {
    key: 'defectRegister', label: '缺陷登记', icon: '✗', iconImage: '/images/异常_abnormal.svg', color: 'red',
    roles: [ROLE_CODES.QUALITY_INSPECTOR], page: PAGE_PATH_MAP.qualityDefect
  },
  {
    key: 'materialIssue', label: '领料', icon: '📦', iconImage: '/images/领料.svg', color: 'purple',
    roles: [ROLE_CODES.WAREHOUSE_KEEPER], page: PAGE_PATH_MAP.materialIssue
  },
  {
    key: 'materialQuery', label: '物料查询', icon: '🔍', iconImage: '/images/查询.svg', color: 'orange',
    roles: [ROLE_CODES.WAREHOUSE_KEEPER], page: PAGE_PATH_MAP.traceabilityProduct
  },
  {
    key: 'inventoryTaskList', label: '库存管理', icon: '📦', iconImage: '/images/库存管理.svg', color: 'purple',
    roles: [ROLE_CODES.WAREHOUSE_KEEPER], page: PAGE_PATH_MAP.inventoryTaskList
  },
  {
    key: 'equipmentStatus', label: '设备状态', icon: '🔧', iconImage: '/images/设备状态.svg', color: 'purple',
    roles: [ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.MANAGER], page: PAGE_PATH_MAP.equipmentStatus
  },
  {
    key: 'repairTask', label: '维修任务', icon: '🔧', iconImage: '/images/维修任务.svg', color: 'purple',
    roles: [ROLE_CODES.EQUIPMENT_MAINTAINER], page: PAGE_PATH_MAP.repairTaskList, isTab: false
  },
  {
    key: 'equipmentRepair', label: '设备报修', icon: '🔧', iconImage: '/images/设备报修.svg', color: 'purple',
    roles: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.EQUIPMENT_MAINTAINER], page: PAGE_PATH_MAP.equipmentRepair
  },
  {
    key: 'andonHandle', label: '安灯处理', icon: '⚠', color: 'red',
    roles: [ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.WAREHOUSE_KEEPER, ROLE_CODES.QUALITY_INSPECTOR, ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.andonList || '/pages/andon/handle/index'
  },
  {
    key: 'productionBoard', label: '生产看板', icon: '📊', iconImage: '/images/生产看板.svg', color: 'blue',
    roles: [ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.supervisorDashboard
  },
  {
    key: 'taskOverview', label: '任务总览', icon: '📋', iconImage: '/images/任务.svg', color: 'blue',
    roles: [ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.tasks, isTab: false
  },
  {
    key: 'abnormalMonitor', label: '异常监控', icon: '⚠', iconImage: '/images/异常_abnormal.svg', color: 'red',
    roles: [ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.supervisorAbnormal
  },
  {
    key: 'andonAbnormal', label: '安灯处理', icon: '⚠', color: 'red',
    roles: [ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.andonList || '/pages/andon/handle/index'
  },
  {
    key: 'scheduling', label: '排产管理', icon: '📝', iconImage: '/images/试卷、作业、任务、布置.svg', color: 'blue',
    roles: [ROLE_CODES.PRODUCTION_SUPERVISOR], page: PAGE_PATH_MAP.scheduling
  },
  {
    key: 'outputTrend', label: '生产数据', icon: '📈', iconImage: '/images/生产数据管理.svg', color: 'blue',
    roles: [ROLE_CODES.MANAGER], page: PAGE_PATH_MAP.trend
  },
  {
    key: 'materialCalls', label: '缺料呼叫', icon: '📦', iconImage: '/images/缺料.svg', color: 'orange',
    roles: [ROLE_CODES.LINE_OPERATOR], page: PAGE_PATH_MAP.materialCall
  },
  {
    key: 'materialCallList', label: '缺料呼叫', icon: '📦', iconImage: '/images/缺料.svg', color: 'orange',
    roles: [ROLE_CODES.WAREHOUSE_KEEPER], page: PAGE_PATH_MAP.materialCallList
  },
]


export const ROLE_TASK_TYPES = {
  [ROLE_CODES.LINE_OPERATOR]: ['PRODUCTION'],
  [ROLE_CODES.QUALITY_INSPECTOR]: ['QUALITY'],
  [ROLE_CODES.WAREHOUSE_KEEPER]: ['MATERIAL'],
  [ROLE_CODES.EQUIPMENT_MAINTAINER]: ['REPAIR', 'ANDON'],
  [ROLE_CODES.PRODUCTION_SUPERVISOR]: [],
  [ROLE_CODES.MANAGER]: []
}


const PAGE_ACCESS_RULES = {
  [PAGE_PATH_MAP.productionTaskList]: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER],
  [PAGE_PATH_MAP.productionTaskDetail]: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER],
  [PAGE_PATH_MAP.productionOperation]: [ROLE_CODES.LINE_OPERATOR],
  [PAGE_PATH_MAP.productionReport]: [ROLE_CODES.LINE_OPERATOR],
  [PAGE_PATH_MAP.productionMaterialBind]: [ROLE_CODES.LINE_OPERATOR],
  [PAGE_PATH_MAP.qualityTaskList]: [ROLE_CODES.QUALITY_INSPECTOR, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER],
  [PAGE_PATH_MAP.qualityInspect]: [ROLE_CODES.QUALITY_INSPECTOR],
  [PAGE_PATH_MAP.qualityDefect]: [ROLE_CODES.QUALITY_INSPECTOR],
  [PAGE_PATH_MAP.equipmentStatus]: [ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER],
  [PAGE_PATH_MAP.equipmentDetail]: [ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER],
  [PAGE_PATH_MAP.repairTaskList]: [ROLE_CODES.EQUIPMENT_MAINTAINER],
  [PAGE_PATH_MAP.equipmentRepair]: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.EQUIPMENT_MAINTAINER],
  [PAGE_PATH_MAP.materialIssue]: [ROLE_CODES.WAREHOUSE_KEEPER],
  [PAGE_PATH_MAP.andonCreate]: [ROLE_CODES.LINE_OPERATOR, ROLE_CODES.QUALITY_INSPECTOR],
  [PAGE_PATH_MAP.andonHandle]: [ROLE_CODES.EQUIPMENT_MAINTAINER, ROLE_CODES.WAREHOUSE_KEEPER, ROLE_CODES.QUALITY_INSPECTOR, ROLE_CODES.PRODUCTION_SUPERVISOR],
  [PAGE_PATH_MAP.andonDetail]: null,
  [PAGE_PATH_MAP.aiAssistant]: null,
  [PAGE_PATH_MAP.scheduling]: [ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER]
}





export function getCurrentRoleCode() {
  return getRoleCode()
}



export function hasEntryPermission(entryKey) {
  const entry = ALL_ENTRIES.find(e => e.key === entryKey)
  if (!entry) return false
  const roleCode = getCurrentRoleCode()
  return entry.roles.includes(roleCode)
}



export function getVisibleEntries() {
  const roleCode = getCurrentRoleCode()
  const allowedKeys = ENTRY_MATRIX[roleCode] || []
  return ALL_ENTRIES.filter(e => allowedKeys.includes(e.key))
}



export function getAllowedTaskTypes() {
  const roleCode = getCurrentRoleCode()
  return ROLE_TASK_TYPES[roleCode] || []
}



export function canAccessPage(pagePath) {
  const roleCode = getCurrentRoleCode()
  const allowedRoles = PAGE_ACCESS_RULES[pagePath]
  if (allowedRoles === null) return true
  if (!allowedRoles) return true
  return allowedRoles.includes(roleCode)
}



export function guardPageAccess(pagePath, options) {
  if (!canAccessPage(pagePath)) {
    wx.showToast({ title: '无权访问该页面', icon: 'none' })
    setTimeout(() => {
      wx.switchTab({ url: PAGE_PATH_MAP.home })
    }, 1500)
    return false
  }
  return true
}
