import { submitRecord, freezeWorkOrder } from '../../../services/quality'

const ACTION_MAP = { RELEASE: '放行', REWORK: '返工', SCRAP: '报废' }

Page({
  data: {
    taskId: '',
    productSn: '',
    productName: '',
    workOrderNo: '',
    defectTypeIndex: -1,
    defectTypeList: [],
    defectQty: '',
    description: '',
    defectImages: [],
    action: '',
    submitting: false
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    const productSn = options.productSn || ''
    const productName = options.productName || ''
    const workOrderNo = options.workOrderNo || ''

    const defectTypeList = Object.keys(DEFECT_TYPE_MAP).map(key => ({
      value: key,
      label: DEFECT_TYPE_MAP[key]
    }))

    this.setData({
      taskId,
      productSn,
      productName,
      workOrderNo,
      defectTypeList
    })

    this.applyAiPrefill()
  },

  onShow() {
    this.applyAiPrefill()
  },

  applyAiPrefill() {
    const prefill = consumePrefillData('/pages/quality/defect/index')
    if (!prefill) return

    console.log('[缺陷登记] 读取到原始预填数据，类型:', typeof prefill, '内容:', JSON.stringify(prefill))

    const updates = {}

    if (prefill.description) updates.description = prefill.description
    if (prefill.defectQty) updates.defectQty = prefill.defectQty

    if (prefill.defectType) {
      const idx = this.data.defectTypeList.findIndex(
        item => item.value === prefill.defectType
      )
      if (idx >= 0) {
        updates.defectTypeIndex = idx
      }
    }

    console.log('[缺陷登记] 最终设置到表单的字段:', Object.keys(updates))

    if (Object.keys(updates).length > 0) {
      this.setData(updates)
      wx.showToast({ title: '已自动填入AI识别信息，请核对', icon: 'none', duration: 2000 })
    }
  },

  onProductSnInput(e) { this.setData({ productSn: e.detail.value }) },
  onProductNameInput(e) { this.setData({ productName: e.detail.value }) },
  onWorkOrderNoInput(e) { this.setData({ workOrderNo: e.detail.value }) },

  onDefectTypeChange(e) {
    this.setData({ defectTypeIndex: parseInt(e.detail.value) })
  },

  onDefectQtyInput(e) {
    this.setData({ defectQty: e.detail.value })
  },

  onDescriptionInput(e) {
    this.setData({ description: e.detail.value })
  },

  onChooseImage() {
    wx.chooseImage({
      count: 9 - this.data.defectImages.length,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const newImages = this.data.defectImages.concat(res.tempFilePaths)
        this.setData({ defectImages: newImages })
      }
    })
  },

  onPreviewImage(e) {
    const index = e.currentTarget.dataset.index
    wx.previewImage({
      current: this.data.defectImages[index],
      urls: this.data.defectImages
    })
  },

  onActionTap(e) {
    this.setData({ action: e.currentTarget.dataset.action })
  },

  onRemoveImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.defectImages.filter((_, i) => i !== index)
    this.setData({ defectImages: images })
  },

  onSubmit() {
    const { defectTypeIndex, defectTypeList, defectQty, description, productSn, taskId, workOrderNo, productName, defectImages, action } = this.data

    if (defectTypeIndex < 0) {
      wx.showToast({ title: '请选择缺陷类型', icon: 'none' }); return
    }
    if (!defectQty || parseInt(defectQty) <= 0) {
      wx.showToast({ title: '请输入有效的缺陷数量', icon: 'none' }); return
    }
    if (!description.trim()) {
      wx.showToast({ title: '请输入缺陷描述', icon: 'none' }); return
    }
    if (!action) {
      wx.showToast({ title: '请选择处置方式', icon: 'none' }); return
    }

    this.setData({ submitting: true })

    const recordData = {
      recordType: 'DEFECT',
      taskId: taskId || '',
      productSn, productName, workOrderNo,
      defectType: defectTypeList[defectTypeIndex].value,
      defectTypeName: defectTypeList[defectTypeIndex].label,
      defectQty: parseInt(defectQty),
      description: description.trim(),
      action, actionLabel: ACTION_MAP[action],
      images: defectImages
    }

    submitRecord(recordData).then(() => {

      if (action === 'SCRAP' || action === 'REWORK') {
        freezeWorkOrder(taskId, `${ACTION_MAP[action]}：${description.trim()}`)
          .then(() => {
            wx.showModal({
              title: '批次已冻结',
              content: `工单 ${workOrderNo || taskId} 已被系统锁定，后续工序报工已禁止。需生产主管审批后方可解锁。`,
              showCancel: false,
              confirmText: '知道了',
              success: () => { wx.navigateBack({ delta: 1 }) }
            })
          })
          .catch(err => {
            wx.showToast({ title: err.message || '冻结失败', icon: 'none' })
          })
      } else {
        wx.showToast({ title: '缺陷登记成功', icon: 'success' })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      }
      this.setData({ submitting: false })
    }).catch(err => {
      this.setData({ submitting: false })
      wx.showToast({ title: err.message || '提交失败', icon: 'none' })
    })
  },

  onCancel() {
    wx.navigateBack({ delta: 1 })
  }
})