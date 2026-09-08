

function callCloudFunction(name, data, opts = {}) {
  const { loading = true, loadingText = '处理中...', showError = true } = opts
  if (loading) wx.showLoading({ title: loadingText, mask: true })
  return new Promise((resolve, reject) => {
    wx.cloud.callFunction({
      name,
      data,
      success: res => {
        if (loading) wx.hideLoading()
        const result = (res && res.result) ? res.result : res
        if (result && result.code === 0) {
          resolve({ code: 0, data: result.data, msg: result.msg || 'ok' })
        } else {
          const msg = (result && result.msg) || '云函数调用失败'
          if (showError) wx.showToast({ title: msg, icon: 'none', duration: 2500 })
          reject(new Error(msg))
        }
      },
      fail: err => {
        if (loading) wx.hideLoading()
        const msg = err.errMsg || '网络异常，请重试'
        if (showError) wx.showToast({ title: msg, icon: 'none', duration: 2500 })
        reject(err)
      }
    })
  })
}

export function getGroups(params = {}) {
  return callCloudFunction('mes-andon', { action: 'listGroups', ...params }, { loading: false })
}

export function getChatGroups(userId, roleCode) {
  return getGroups()
}

export function getChatDetail(groupId) {
  return callCloudFunction('mes-andon', { action: 'groupDetail', groupId })
}

export function getMessages(groupId) {
  return callCloudFunction('mes-andon', { action: 'listMessages', groupId }, { loading: false })
}

export function sendMessage(groupId, senderId, senderName, senderRole, content, msgType) {
  return callCloudFunction('mes-andon', { action: 'sendMessage', groupId, content, msgType })
}

export function createChatGroup(data) {
  return callCloudFunction('mes-chat', { action: 'createGroup', ...data })
}

export function disbandChatGroup(groupId, reason) {
  return callCloudFunction('mes-chat', { action: 'disbandGroup', groupId, reason })
}

export function addMember(groupId, targetUserId) {
  return callCloudFunction('mes-chat', { action: 'addMember', groupId, targetUserId })
}

export function triggerAutoCreateGroup(params) {
  const { bizType, bizRefId, bizRefNo, bizContext, creatorId, creatorName, memberIds } = params
  return callCloudFunction('mes-chat', {
    action: 'createGroup',
    name: bizContext?.title || '沟通群',
    bizType, bizRefId, bizRefNo, bizContext,
    memberIds: memberIds || []
  })
}

export function triggerAutoDisbandGroup(bizRefId, reason) {
  return callCloudFunction('mes-chat', { action: 'disbandGroup', groupId: bizRefId, reason })
}
