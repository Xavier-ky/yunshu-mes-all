

import { ANDON_EVENT_TYPE_MAP } from '../../../utils/constants'
import { getCurrentUser } from '../../../utils/auth'
import { createEvent } from '../../../services/andon'
import { getMaterialCallDetail } from '../../../services/inventory'
import { getTaskDetail } from '../../../services/production'

const TITLE_MAP = { repair: '报修详情', material: '缺料详情', quality: '质检详情' }

Page({
  data: {
    type: '',
    id: '',
    data: null,
    pageTitle: '详情',
    upgraded: false
  },

  onLoad(options) {
    const type = options.type || ''
    const id = options.id || ''

    if (type === 'repair') {
      wx.redirectTo({ url: `/pages/tasks/repair-detail/index?taskId=${id}` })
      return
    }

    this.setData({ type, id, pageTitle: TITLE_MAP[type] || '详情' })
    this.loadData(type, id)
  },

  async loadData(type, id) {
    let data = null
    try {
      if (type === 'material') {
        const res = await getMaterialCallDetail(id)
        data = (res && res.data) || null
      } else if (type === 'quality') {
        const res = await getTaskDetail(id)
        data = (res && res.data) || null
      }
    } catch (_) {  }
    this.setData({ data: data || null })
  },

  onUpgrade() {
    const { type, data } = this.data
    if (!data) return

    wx.showModal({
      title: '升级为安灯',
      content: '将当前问题升级为安灯事件，系统将自动通知对应负责人并创建沟通群聊。确认？',
      success: (res) => {
        if (res.confirm) {
          const userInfo = getCurrentUser()
          const creatorName = userInfo?.realName || '张三'

          let eventType = 'EQUIPMENT'
          let description = ''
          let deviceCode = ''
          let lineName = ''

          if (type === 'repair') {
            eventType = 'EQUIPMENT'
            description = `设备报修升级：${data.deviceName}(${data.deviceCode}) - ${data.faultDescription || '设备故障'}`
            deviceCode = data.deviceCode
            lineName = data.lineName
          } else if (type === 'material') {
            eventType = 'MATERIAL'
            description = `缺料升级：${data.materialName} 需求${data.needQty}，剩余${data.remainQty} - ${data.remark || ''}`
            lineName = data.lineName
          } else if (type === 'quality') {
            eventType = 'QUALITY'
            description = `质检异常升级：${data.productName}(${data.workOrderNo}) - 当前进度${data.completedQty || 0}/${data.planQty}`
            lineName = data.workstationName
          }

          createEvent({
            eventType,
            eventTypeName: ANDON_EVENT_TYPE_MAP[eventType],
            reasonCode: type === 'repair' ? 'MACHINE_FAULT' : type === 'material' ? 'MATERIAL_SHORTAGE' : 'QUALITY_DEFECT',
            deviceCode,
            lineName,
            description
          }).then(result => {
            this.setData({ upgraded: true })
            wx.showToast({ title: '已升级为安灯事件，群聊已创建', icon: 'success', duration: 2000 })
          }).catch(err => {
            wx.showToast({ title: err.message || '升级失败', icon: 'none' })
          })
        }
      }
    })
  },

  onBack() { wx.navigateBack({ delta: 1 }) }
})
