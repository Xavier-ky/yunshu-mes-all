

import { ROLE_MAP, ROLE_ENTRIES } from '../utils/constants'

const listeners = {}

export function on(event, fn) {
  if (!listeners[event]) listeners[event] = []
  listeners[event].push(fn)
}

export function off(event, fn) {
  if (!listeners[event]) return
  listeners[event] = listeners[event].filter(f => f !== fn)
}

export function emit(event, payload) {
  if (!listeners[event]) return
  listeners[event].forEach(fn => {
    try { fn(payload) } catch (e) { console.error(`[Store] 事件 ${event} 回调异常:`, e) }
  })
}

const state = {

  userInfo: null,

  roleMap: ROLE_MAP,
  roleEntries: ROLE_ENTRIES
}

let _initialized = false
export function initStore() {
  if (_initialized) return
  _initialized = true

  try {
    const raw = wx.getStorageSync('_mes_user')
    if (raw) {
      state.userInfo = JSON.parse(raw)
      console.log('[Store] 已恢复登录用户:', state.userInfo?.realName)
    }
  } catch (_) {  }
}

export function forcePersist() {
  try {
    if (state.userInfo) {
      wx.setStorageSync('_mes_user', JSON.stringify(state.userInfo))
    }
  } catch (_) {  }
}

export function getUserInfo() {
  return state.userInfo
}

export function setUserInfo(info) {
  state.userInfo = { ...info, loginTime: new Date().toISOString() }
  try {
    wx.setStorageSync('_mes_user', JSON.stringify(state.userInfo))
  } catch (_) {  }
  emit('user:changed', state.userInfo)
}

export function getState() {
  if (!_initialized) initStore()
  return state
}

export function snapshot(key) {
  if (!key) return { ...state }
  const val = state[key]
  if (Array.isArray(val)) return [...val]
  if (val && typeof val === 'object') return { ...val }
  return val
}

export function validateReportCheck() {
  console.warn('[Store] validateReportCheck 已废弃，请通过 services/production.js 调用云函数 mes-production')
  return { ok: false, error: '此函数已废弃，请更新代码使用云函数' }
}

export function freezeWorkOrder() {
  console.warn('[Store] freezeWorkOrder 已废弃，请通过 services/quality.js 调用云函数 mes-quality')
  return { ok: false, error: '此函数已废弃' }
}

export function unfreezeWorkOrder() {
  console.warn('[Store] unfreezeWorkOrder 已废弃')
  return { ok: false, error: '此函数已废弃' }
}

export function getFifoBatch() {
  console.warn('[Store] getFifoBatch 已废弃，请通过 services/inventory.js 调用云函数 mes-inventory')
  return { locked: false, error: '此函数已废弃' }
}

export function runTpmCheck() {
  console.warn('[Store] runTpmCheck 已废弃，TPM 已改为云函数定时触发器，前端请调用 services/equipment.js 的 getTpmAlerts()')
  return []
}

export function createAndon() {
  console.warn('[Store] createAndon 已废弃，请通过 services/andon.js 调用云函数 mes-andon')
}

export function resolveAndon() {
  console.warn('[Store] resolveAndon 已废弃')
}

export function submitRepair() {
  console.warn('[Store] submitRepair 已废弃')
}

export function completeRepair() {
  console.warn('[Store] completeRepair 已废弃')
}

export function createSchedule() {
  console.warn('[Store] createSchedule 已废弃')
}

export function deleteSchedule() {
  console.warn('[Store] deleteSchedule 已废弃')
}

export function updateTask() {
  console.warn('[Store] updateTask 已废弃')
}

export function addTask() {
  console.warn('[Store] addTask 已废弃')
}

export function removeTask() { console.warn('[Store] removeTask 已废弃') }

export function sendMessage() { console.warn('[Store] sendMessage 已废弃') }

export function optimisticUpdate() { console.warn('[Store] optimisticUpdate 已废弃') }

export function authorizeFifoSkip() { console.warn('[Store] authorizeFifoSkip 已废弃') }

export function checkDeviceAvailable() { console.warn('[Store] checkDeviceAvailable 已废弃') }

export function completeInspection() { console.warn('[Store] completeInspection 已废弃') }

export function getLiveSupervisorStats() { console.warn('[Store] getLiveSupervisorStats 已废弃') }

export function createMaterialCall() { console.warn('[Store] createMaterialCall 已废弃') }

export function processMaterialCall() { console.warn('[Store] processMaterialCall 已废弃') }

export function completeMaterialCall() { console.warn('[Store] completeMaterialCall 已废弃') }
