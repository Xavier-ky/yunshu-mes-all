

const STORAGE_KEY = 'ai_prefill_data'

export function setPrefillData(page, data) {

  if (data && typeof data === 'string') {
    try {
      console.log('[AI预填] data 为字符串，尝试解析:', data.substring(0, 200))
      data = JSON.parse(data)
    } catch (e) {
      console.warn('[AI预填] data 字符串 JSON.parse 失败，丢弃:', e.message)
      return
    }
  }

  console.log('[AI预填] 最终 data 类型:', typeof data, '是否为对象:', data && typeof data === 'object')

  if (!page || !data || typeof data !== 'object') {
    console.warn('[AI预填] 参数无效，跳过存储。page:', page, 'data类型:', typeof data)
    return
  }

  const payload = {
    page,
    data,
    timestamp: Date.now()
  }
  try {
    wx.setStorageSync(STORAGE_KEY, payload)
    console.log('[AI预填] 已存储预填数据:', JSON.stringify(payload))
  } catch (e) {
    console.warn('[AI预填] 存储失败:', e)
  }
}

export function consumePrefillData(currentPage) {
  try {
    const raw = wx.getStorageSync(STORAGE_KEY)
    if (!raw) return null

    const payload = raw

    if (payload.page !== currentPage) {
      console.log('[AI预填] 页面不匹配，忽略。期望:', payload.page, '实际:', currentPage)
      return null
    }

    const elapsed = Date.now() - payload.timestamp
    if (elapsed > 5 * 60 * 1000) {
      console.log('[AI预填] 数据已过期，清除')
      wx.removeStorageSync(STORAGE_KEY)
      return null
    }

    wx.removeStorageSync(STORAGE_KEY)
    console.log('[AI预填] 已消费预填数据:', JSON.stringify(payload.data))
    return payload.data
  } catch (e) {
    console.warn('[AI预填] 读取失败:', e)
    return null
  }
}

export function clearPrefillData() {
  try {
    wx.removeStorageSync(STORAGE_KEY)
  } catch (e) {

  }
}
