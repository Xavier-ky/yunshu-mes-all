import { bindMaterial, getTaskDetail } from '../../../services/production'

Page({
  data: {
    taskId: '',
    task: null,
    materialBatch: '',
    materialInfo: null,
    binding: false,
    bound: false
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    const code = options.code || ''
    this.setData({ taskId })
    if (taskId) {
      this.loadTaskDetail(taskId)
    }
    if (code) {
      this.setData({ materialBatch: code })
      this.lookupMaterial(code)
    }
  },

  loadTaskDetail(taskId) {
    getTaskDetail(taskId).then(res => {
      if (res.code === 0 && res.data) {
        this.setData({ task: res.data })
      }
    }).catch(() => {
      wx.showToast({ title: '加载任务信息失败', icon: 'none' })
    })
  },

  onScanMaterial(e) {
    const { code } = e.detail
    this.setData({
      materialBatch: code,
      bound: false
    })
    this.lookupMaterial(code)
  },

  onMaterialInput(e) {
    const value = e.detail.value
    this.setData({ materialBatch: value })
  },

  onMaterialIdentify(e) {
    const { code } = e.detail
    this.setData({
      materialBatch: code,
      bound: false
    })
    this.lookupMaterial(code)
  },

  lookupMaterial(code) {
    if (!code) return
    this.setData({
      materialInfo: {
        materialCode: '',
        materialName: '',
        spec: '',
        batchNo: code,
        scanned: true
      }
    })
  },

  onBind() {
    const { taskId, task, materialBatch } = this.data

    if (!materialBatch) {
      wx.showToast({ title: '请扫描物料批次码', icon: 'none' })
      return
    }

    this.setData({ binding: true })

    bindMaterial({
      taskId,
      workOrderNo: task ? task.workOrderNo : '',
      materialBatch
    }).then(res => {
      this.setData({ binding: false })
      if (res.code === 0) {
        wx.showToast({ title: '物料绑定成功', icon: 'success' })
        this.setData({ bound: true })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.message || '绑定失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ binding: false })
      wx.showToast({ title: '绑定失败', icon: 'none' })
    })
  }
})