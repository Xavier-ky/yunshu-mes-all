import { submitReport } from '../../../services/production'

Page({
  data: {
    taskId: '',
    productSn: '',
    materialBatch: '',
    productInfo: null,
    materialInfo: null,
    goodQty: '',
    defectQty: '',
    submitting: false
  },

  onLoad(options) {
    if (options.taskId) {
      this.setData({ taskId: options.taskId })
    }
  },

  onScanProduct(e) {
    const { code } = e.detail
    this.setData({
      productSn: code,
      productInfo: {
        code,
        scanned: true
      }
    })
  },

  onProductInput(e) {
    const value = e.detail.value
    this.setData({ productSn: value })
  },

  onProductIdentify(e) {
    const { code } = e.detail
    this.setData({
      productInfo: {
        code,
        scanned: true
      }
    })
  },

  onScanMaterial(e) {
    const { code } = e.detail
    this.setData({
      materialBatch: code,
      materialInfo: {
        code,
        scanned: true
      }
    })
  },

  onMaterialInput(e) {
    const value = e.detail.value
    this.setData({ materialBatch: value })
  },

  onMaterialIdentify(e) {
    const { code } = e.detail
    this.setData({
      materialInfo: {
        code,
        scanned: true
      }
    })
  },

  onGoodQtyInput(e) {
    this.setData({ goodQty: e.detail.value })
  },

  onDefectQtyInput(e) {
    this.setData({ defectQty: e.detail.value })
  },

  onSubmit() {
    const { productSn, materialBatch, goodQty, defectQty } = this.data

    if (!productSn) {
      wx.showToast({ title: '请扫描产品SN', icon: 'none' })
      return
    }

    if (!goodQty || Number(goodQty) <= 0) {
      wx.showToast({ title: '请输入良品数量', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    submitReport({
      taskId: this.data.taskId,
      productSn,
      materialBatch,
      goodQty: Number(goodQty),
      defectQty: Number(defectQty) || 0
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.message || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  }
})