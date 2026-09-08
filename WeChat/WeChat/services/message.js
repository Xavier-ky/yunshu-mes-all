

import { getEvents } from './andon'
import { getDevices } from './equipment'
import { getTasks } from './production'
import { getMaterialCalls } from './inventory'

const STORAGE_KEY_READ = '_mes_read_ids'

const ROLE_CODES = {
  LINE_OPERATOR: 'LINE_OPERATOR',
  QUALITY_INSPECTOR: 'QUALITY_INSPECTOR',
  WAREHOUSE_KEEPER: 'WAREHOUSE_KEEPER',
  EQUIPMENT_MAINTAINER: 'EQUIPMENT_MAINTAINER',
  PRODUCTION_SUPERVISOR: 'PRODUCTION_SUPERVISOR',
  MANAGER: 'MANAGER'
}

const ROLE_ANDON_TYPES = {
  [ROLE_CODES.QUALITY_INSPECTOR]:    ['QUALITY'],
  [ROLE_CODES.WAREHOUSE_KEEPER]:     ['MATERIAL'],
  [ROLE_CODES.EQUIPMENT_MAINTAINER]: ['EQUIPMENT'],
}

const SUPERVISOR_ROLES = [
  ROLE_CODES.PRODUCTION_SUPERVISOR, ROLE_CODES.MANAGER
]

function getReadMap() {
  try {
    const raw = wx.getStorageSync(STORAGE_KEY_READ)
    return (raw && typeof raw === 'object') ? raw : {}
  } catch (_) {
    console.warn('[消息] 读取已读状态失败，重置')
    return {}
  }
}

function saveReadMap(map) {
  try {
    wx.setStorageSync(STORAGE_KEY_READ, map)
  } catch (e) {
    console.warn('[消息] 保存已读状态失败:', e)
  }
}

function cleanReadMap(map, activeIds) {
  const activeSet = new Set(activeIds)
  let changed = false
  for (const key of Object.keys(map)) {
    if (!activeSet.has(key)) { delete map[key]; changed = true }
  }
  return changed
}

export async function getMessages(params = {}) {
  const { type, roleCode, operatorName } = params
  const isSupervisor = SUPERVISOR_ROLES.includes(roleCode)
  const andonTypes = ROLE_ANDON_TYPES[roleCode]
  const readMap = getReadMap()
  const messages = []
  const activeIds = []

  try {
    const andonRes = await getEvents()
    const andonList = (andonRes && andonRes.data && andonRes.data.list) || []
    andonList.forEach(e => {

      if (andonTypes) {
        if (!andonTypes.includes(e.eventType)) return
      } else if (roleCode === ROLE_CODES.LINE_OPERATOR) {
        if (operatorName && e.createdByName !== operatorName) return
      }

      const mid = 'A-' + e.eventId
      activeIds.push(mid)
      const isNew = e.status === 'INITIATED'
      messages.push({
        msgId: mid,
        type: 'ANDON', typeName: isNew ? '安灯·新' : '安灯',
        typeColor: isNew ? 'red' : 'orange',
        title: `${e.eventTypeName || '安灯'} · ${e.lineName || e.workstationName || ''}`,
        content: e.description || '',
        refType: 'ANDON', refId: e.eventId, refNo: e.eventNo || e.eventId,
        time: e.createTime,
        isRead: !!readMap[mid]
      })
    })
  } catch (_) {  }

  if (isSupervisor) {

    messages.sort((a, b) => new Date(b.time) - new Date(a.time))
    let list = messages
    if (type) list = list.filter(m => m.type === type)
    return { code: 0, data: { list, total: list.length } }
  }

  if (roleCode === ROLE_CODES.EQUIPMENT_MAINTAINER) {
    try {
      const deviceRes = await getDevices()
      const devices = (deviceRes && deviceRes.data && deviceRes.data.list) || []
      devices
        .filter(d => d.status === 'FAULT' || d.status === 'REPAIRING')
        .forEach(d => {
          const mid = 'E-' + d.deviceCode
          activeIds.push(mid)
          messages.push({
            msgId: mid,
            type: 'EQUIPMENT', typeName: '设备故障',
            typeColor: 'orange',
            title: `${d.deviceName} · ${d.statusLabel}`,
            content: d.faultDescription || '设备异常，需维修处理',
            refType: 'EQUIPMENT', refId: d.deviceCode, refNo: d.deviceCode,
            time: d.updateTime || d.createTime,
            isRead: !!readMap[mid]
          })
        })
    } catch (_) {  }
  }

  if (roleCode === ROLE_CODES.QUALITY_INSPECTOR) {
    try {
      const qualityRes = await getTasks({ taskType: 'QUALITY' })
      const qualityList = (qualityRes && qualityRes.data && qualityRes.data.list) || []
      qualityList
        .filter(t => t.status === 'PENDING')
        .forEach(t => {
          const mid = 'Q-' + t.taskId
          activeIds.push(mid)
          messages.push({
            msgId: mid,
            type: 'QUALITY', typeName: '质检待办',
            typeColor: 'red',
            title: `${t.productName} · ${t.processName || '质检'}`,
            content: `工单 ${t.workOrderNo || ''} 待检验，计划 ${t.planQty} 件`,
            refType: 'QUALITY', refId: t.taskId, refNo: t.taskNo || t.taskId,
            time: t.createTime,
            isRead: !!readMap[mid]
          })
        })
    } catch (_) {  }
  }

  if (roleCode === ROLE_CODES.WAREHOUSE_KEEPER) {
    try {
      const callRes = await getMaterialCalls()
      const callList = (callRes && callRes.data && callRes.data.list) || []
      callList
        .filter(c => c.status === 'PENDING')
        .forEach(c => {
          const mid = 'M-' + c.callId
          activeIds.push(mid)
          messages.push({
            msgId: mid,
            type: 'MATERIAL', typeName: '缺料呼叫',
            typeColor: 'blue',
            title: `${c.materialName} 库存不足`,
            content: `${c.lineName || ''} 需求 ${c.needQty}，剩余 ${c.remainQty}${c.remark ? ' · ' + c.remark : ''}`,
            refType: 'MATERIAL', refId: c.callId, refNo: c.callNo || c.callId,
            time: c.createTime,
            isRead: !!readMap[mid]
          })
        })
    } catch (_) {  }
  }

  if (cleanReadMap(readMap, activeIds)) {
    saveReadMap(readMap)
  }

  messages.sort((a, b) => new Date(b.time) - new Date(a.time))

  let list = messages
  if (type) {
    list = list.filter(m => m.type === type)
  }

  return { code: 0, data: { list, total: list.length } }
}

export async function getUnreadCount(roleCode, operatorName) {
  try {
    const res = await getMessages({ roleCode, operatorName })
    const unread = (res.data && res.data.list || []).filter(m => !m.isRead).length
    return { code: 0, data: { count: unread } }
  } catch (_) {
    return { code: 0, data: { count: 0 } }
  }
}

export function markAsRead(msgId) {
  if (!msgId) return Promise.resolve({ code: 0, msg: 'ok' })
  try {
    const map = getReadMap()
    map[msgId] = true
    saveReadMap(map)
  } catch (_) {  }
  return Promise.resolve({ code: 0, msg: '已标记' })
}
