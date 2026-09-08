import { getTaskDetail } from '../../../services/production'
import { getDeviceDetail } from '../../../services/equipment'
import { createEvent } from '../../../services/andon'
import { ANDON_EVENT_TYPE_MAP } from '../../../utils/constants'
import { formatDateTime } from '../../../utils/format'

Page({
  data: {
    taskId: '',
    task: null,
    device: null,
    loading: true,
    upgraded: false
  },

  onLoad(options) {
    const taskId = options.taskId || options.id || ''
    if (taskId) {
      this.setData({ taskId })
      this.loadAll(taskId)
    } else {
      this.setData({ loading: false })
    }
  },

  async loadAll(taskId) {
    this.setData({ loading: true })
    try {

      let task = null
      const taskRes = await getTaskDetail(taskId)
      if (taskRes && taskRes.code === 0 && taskRes.data) {
        task = taskRes.data
      }

      let device = null
      if (task && task.productModel) {
        try {
          const devRes = await getDeviceDetail(task.productModel)
          if (devRes && devRes.code === 0 && devRes.data) {
            device = devRes.data
          }
        } catch (_) {  }
      }

      this.setData({ task, device, loading: false })
    } catch (_) {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  onUpgrade() {
    const { task, device } = this.data
    if (!task) return

    wx.showModal({
      title: '升级为安灯',
      content: '将此维修任务升级为安灯事件，系统将自动通知对应负责人并创建沟通群聊。确认？',
      success: (res) => {
        if (res.confirm) {
          const deviceName = task.productName || device?.deviceName || ''
          const deviceCode = task.productModel || device?.deviceCode || ''
          const description = `设备报修升级：${deviceName}(${deviceCode}) - ${task.description || '设备故障'}`

          createEvent({
            eventType: 'EQUIPMENT',
            eventTypeName: ANDON_EVENT_TYPE_MAP['EQUIPMENT'],
            reasonCode: 'MACHINE_FAULT',
            deviceCode,
            lineName: device?.lineName || task.workstationName || '',
            description
          }).then(() => {
            this.setData({ upgraded: true })
            wx.showToast({ title: '已升级为安灯事件', icon: 'success', duration: 2000 })
          }).catch(err => {
            wx.showToast({ title: err.message || '升级失败', icon: 'none' })
          })
        }
      }
    })
  },

  onBack() { wx.navigateBack({ delta: 1 }) }
})
