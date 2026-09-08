import { getTaskDetail, submitReport } from '../../../services/production'

Page({
  data: {
    taskId: '',
    task: null,
    loading: false,
    submitting: false,
    formData: {
      productSn: '',
      goodQty: '',
      defectQty: '',
      remark: ''
    }
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    this.setData({ taskId })
    if (taskId) {
      this.loadTaskDetail(taskId)
    }
  },

  loadTaskDetail(taskId) {
    this.setData({ loading: true })
    getTaskDetail(taskId).then(res => {
      if (res.code === 0 && res.data) {
        this.setData({ task: res.data, loading: false })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '任务不存在', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onScanProduct(e) {
    const { code } = e.detail
    this.setData({
      'formData.productSn': code
    })
  },

  onProductInput(e) {
    this.setData({
      'formData.productSn': e.detail.value
    })
  },

  onGoodQtyInput(e) {
    this.setData({
      'formData.goodQty': e.detail.value
    })
  },

  onDefectQtyInput(e) {
    this.setData({
      'formData.defectQty': e.detail.value
    })
  },

  onRemarkInput(e) {
    this.setData({
      'formData.remark': e.detail.value
    })
  },

  onSubmit() {
    const { taskId, task, formData } = this.data

    if (!formData.productSn) {
      wx.showToast({ title: '请扫描产品SN', icon: 'none' })
      return
    }

    if (!formData.goodQty || Number(formData.goodQty) <= 0) {
      wx.showToast({ title: '请输入良品数量', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    submitReport({
      taskId,
      workOrderNo: task ? task.workOrderNo : '',
      workstationCode: task ? task.workstationCode : '',
      processCode: task ? task.processCode : '',
      productSn: formData.productSn,
      goodQty: Number(formData.goodQty),
      defectQty: Number(formData.defectQty) || 0,
      remark: formData.remark
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '报工成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.message || '报工失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '报工失败', icon: 'none' })
    })
  }
})