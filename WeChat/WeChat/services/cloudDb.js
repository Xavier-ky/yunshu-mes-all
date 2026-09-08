

const db = wx.cloud.database()

export async function queryTasks(params = {}) {
  console.warn('[cloudDb] queryTasks 已废弃，请使用云函数 mes-production')
  try {
    const where = {}
    if (params.taskType) where.taskType = params.taskType
    if (params.status) where.status = params.status
    const r = await db.collection('tasks').where(where).orderBy('createTime', 'desc').limit(50).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1, error: e.message } }
}

export async function querySchedules(params = {}) {
  console.warn('[cloudDb] querySchedules 已废弃')
  try {
    const where = {}
    if (params.status) where.status = params.status
    const r = await db.collection('schedules').where(where).orderBy('createTime', 'desc').limit(20).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function queryAndonEvents(params = {}) {
  console.warn('[cloudDb] queryAndonEvents 已废弃')
  try {
    const where = {}
    if (params.status) where.status = params.status
    const r = await db.collection('andon_events').where(where).orderBy('createTime', 'desc').limit(50).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function queryDevices(params = {}) {
  console.warn('[cloudDb] queryDevices 已废弃')
  try {
    const where = {}
    if (params.status) where.status = params.status
    const r = await db.collection('devices').where(where).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function queryMaterialCalls(params = {}) {
  console.warn('[cloudDb] queryMaterialCalls 已废弃')
  try {
    const where = {}
    if (params.status) where.status = params.status
    const r = await db.collection('material_calls').where(where).orderBy('createTime', 'desc').limit(50).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function queryInventory(params = {}) {
  console.warn('[cloudDb] queryInventory 已废弃')
  try {
    const where = {}
    if (params.taskType) where.taskType = params.taskType
    if (params.status) where.status = params.status
    const r = await db.collection('inventory').where(where).orderBy('createTime', 'desc').limit(50).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function queryChatGroups(params = {}) {
  console.warn('[cloudDb] queryChatGroups 已废弃')
  try {
    const where = {}
    if (params.status) where.status = params.status
    const r = await db.collection('chat_groups').where(where).orderBy('createTime', 'desc').limit(50).get()
    return { code: 0, data: { list: r.data || [], total: (r.data || []).length } }
  } catch (e) { return { code: -1 } }
}

export async function addTaskToCloud(task) {
  console.warn('[cloudDb] addTaskToCloud 已废弃，请使用云函数 mes-production')
  return { code: -1, error: '已废弃' }
}

export async function updateTaskInCloud(taskId, updates) {
  console.warn('[cloudDb] updateTaskInCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function removeTaskFromCloud(taskId) {
  console.warn('[cloudDb] removeTaskFromCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function addAndonToCloud(evt) {
  console.warn('[cloudDb] addAndonToCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function updateAndonInCloud(eventId, updates) {
  console.warn('[cloudDb] updateAndonInCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function addScheduleToCloud(schedule) {
  console.warn('[cloudDb] addScheduleToCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function removeScheduleFromCloud(scheduleNo) {
  console.warn('[cloudDb] removeScheduleFromCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function updateDeviceInCloud(deviceCode, updates) {
  console.warn('[cloudDb] updateDeviceInCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function addMaterialCallToCloud(call) {
  console.warn('[cloudDb] addMaterialCallToCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function updateMaterialCallInCloud(callId, updates) {
  console.warn('[cloudDb] updateMaterialCallInCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function addInventoryToCloud(item) {
  console.warn('[cloudDb] addInventoryToCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function addChatGroupToCloud(group) {
  console.warn('[cloudDb] addChatGroupToCloud 已废弃')
  return { code: -1, error: '已废弃' }
}

export async function updateChatGroupInCloud(groupId, updates) {
  console.warn('[cloudDb] updateChatGroupInCloud 已废弃')
  return { code: -1, error: '已废弃' }
}
