import { getInventoryTaskDetail, submitMaterialIssue, submitMaterialReturn, submitInbound, getFifoBatch, lookupMaterialByBatch } from '../../../services/inventory'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

Page({
  data: {
    taskId: '',
    task: null,
    pageMode: 'ISSUE',
    loading: true,
    submitting: false,

    scanBatchNo: '',
    scannedMaterial: null,
    stockNotEnough: false,
    batchNotExist: false,
    issueQty: 0,
    maxIssueQty: 0,
    remainQty: 0,
    remark: '',
    fifoWarning: '',

    inboundQty: 0,

    showReturnForm: false,
    returnQty: 1,
    returnReason: '',
    returnPhotos: [],
    returnSubmitting: false
  },

  onLoad(options) {
    guardPageAccess(PAGE_PATH_MAP.inventoryTaskDetail)
    const taskId = options.taskId || ''
    if (taskId) {
      this.setData({ taskId })
      this.loadTaskDetail(taskId)
    } else {
      this.setData({ loading: false })
    }
  },

  loadTaskDetail(taskId) {
    this.setData({ loading: true })
    getInventoryTaskDetail(taskId).then(res => {
      if (res.code === 0 && res.data) {
        const task = res.data
        const taskType = task.taskType || 'ISSUE'

        let pageMode = 'ISSUE'
        if (taskType === 'INBOUND' || taskType === 'INBOUND') pageMode = 'INBOUND'
        else if (taskType === 'RETURN') pageMode = 'RETURN'
        else if (taskType === 'OUTBOUND') pageMode = 'OUTBOUND'

        const remainQty = task.remainQty || task.qty - (task.issuedQty || 0)
        const registeredQty = task.registeredQty || 0

        this.setData({
          task, pageMode, remainQty,
          maxIssueQty: Math.min(remainQty, task.stockQty || remainQty),
          inboundQty: task.qty - registeredQty,
          loading: false
        })
      } else {
        this.setData({ loading: false, task: null })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onScan() {
    wx.scanCode({ success: (res) => {
      this.setData({ scanBatchNo: res.result })
      this.lookupMaterial(res.result)
    }})
  },
  onScanInput(e) { this.setData({ scanBatchNo: e.detail.value }) },
  onScanConfirm(e) {
    const value = e.detail && e.detail.value
    if (value) this.lookupMaterial(value)
  },

  async lookupMaterial(batchNo) {
    if (!batchNo) return
    let info = null
    try {
      const res = await lookupMaterialByBatch(batchNo)
      if (res && res.code === 0 && res.data) info = res.data
    } catch (_) {}
    if (!info) {
      this.setData({ scannedMaterial: null, batchNotExist: true, stockNotEnough: false, issueQty: 0 })
      wx.showToast({ title: '未找到该批次物料', icon: 'none' })
      return
    }
    const stockQty = info.stockQty || info.registeredQty || info.qty || 0
    const maxIssue = Math.min(this.data.remainQty, stockQty)
    let fifoWarning = ''
    try {
      const materialCode = (this.data.task || {}).materialCode
      if (materialCode) {
        const fifoRes = await getFifoBatch(materialCode)
        const fifo = (fifoRes && fifoRes.data) || {}
        if (fifo.locked && fifo.batch && fifo.batch.batchNo !== batchNo) {
          fifoWarning = `FIFO 提醒：当前最早批次为 ${fifo.batch.batchNo}，建议优先消耗`
        }
      }
    } catch (_) {}
    this.setData({
      scannedMaterial: info, batchNotExist: false,
      stockNotEnough: stockQty < this.data.remainQty,
      maxIssueQty: maxIssue, issueQty: maxIssue > 0 ? 1 : 0, fifoWarning
    })
  },

  onQtyChange(e) {
    const field = e.currentTarget.dataset.field || 'issueQty'
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data[field]) || 0
    const max = this.data.maxIssueQty
    const newVal = Math.max(0, Math.min(max, current + delta))
    this.setData({ [field]: newVal })
  },
  onQtyInput(e) {
    const field = e.currentTarget.dataset.field || 'issueQty'
    const val = Math.min(parseInt(e.detail.value) || 0, this.data.maxIssueQty || 9999)
    this.setData({ [field]: val })
  },

  onInboundQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data.inboundQty) || 0
    const maxQty = this.data.task ? (this.data.task.qty - (this.data.task.registeredQty || 0)) : 9999
    this.setData({ inboundQty: Math.max(0, Math.min(maxQty, current + delta)) })
  },
  onInboundQtyInput(e) {
    const maxQty = this.data.task ? (this.data.task.qty - (this.data.task.registeredQty || 0)) : 9999
    this.setData({ inboundQty: Math.min(parseInt(e.detail.value) || 0, maxQty) })
  },

  onRemarkInput(e) { this.setData({ remark: e.detail.value }) },

  onSubmitIssue() {
    const { task, scanBatchNo, scannedMaterial, issueQty, remark, submitting } = this.data
    if (submitting) return
    if (!scannedMaterial) return wx.showToast({ title: '请先扫描物料批次', icon: 'none' })
    const qty = parseInt(issueQty) || 0
    if (qty <= 0) return wx.showToast({ title: '请输入有效的发料数量', icon: 'none' })
    if (qty > this.data.maxIssueQty) return wx.showToast({ title: `发料数量不能超过 ${this.data.maxIssueQty}`, icon: 'none' })
    this.setData({ submitting: true })
    submitMaterialIssue({
      taskId: task.taskId, workOrderNo: task.workOrderNo,
      materialCode: task.materialCode, materialName: task.materialName,
      batchNo: scanBatchNo.trim(), issueQty: qty,
      warehouseName: task.warehouseName, locationCode: task.locationCode, remark: remark.trim()
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '领料成功', icon: 'success', duration: 1500 })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.msg || res.message || '发料失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  },

  onSubmitInbound() {
    const { task, inboundQty, remark, submitting } = this.data
    if (submitting) return
    const qty = parseInt(inboundQty) || 0
    if (qty <= 0) return wx.showToast({ title: '请输入有效的入库数量', icon: 'none' })
    this.setData({ submitting: true })
    submitInbound({
      taskId: task.taskId, registeredQty: qty,
      remark: remark.trim()
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '入库登记成功', icon: 'success', duration: 1500 })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.msg || res.message || '入库失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  },

  onToggleReturnForm() { this.setData({ showReturnForm: !this.data.showReturnForm }) },
  onReturnQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    this.setData({ returnQty: Math.max(1, (parseInt(this.data.returnQty) || 0) + delta) })
  },
  onReturnQtyInput(e) { this.setData({ returnQty: Math.max(1, parseInt(e.detail.value) || 1) }) },
  onReturnReasonInput(e) { this.setData({ returnReason: e.detail.value }) },
  onChoosePhoto() {
    const remain = 4 - (this.data.returnPhotos || []).length
    if (remain <= 0) return wx.showToast({ title: '最多上传 4 张照片', icon: 'none' })
    wx.chooseImage({ count: remain, sizeType: ['compressed'], sourceType: ['camera', 'album'],
      success: (res) => this.setData({ returnPhotos: [...this.data.returnPhotos, ...res.tempFilePaths] })
    })
  },
  onDeletePhoto(e) {
    const idx = e.currentTarget.dataset.index
    const photos = [...this.data.returnPhotos]; photos.splice(idx, 1)
    this.setData({ returnPhotos: photos })
  },
  onSubmitReturn() {
    const { task, returnQty, returnReason, returnPhotos, returnSubmitting } = this.data
    if (returnSubmitting) return
    const qty = parseInt(returnQty) || 0
    if (qty <= 0) return wx.showToast({ title: '请输入有效的退料数量', icon: 'none' })
    if (!returnReason.trim()) return wx.showToast({ title: '请输入退料原因', icon: 'none' })
    this.setData({ returnSubmitting: true })
    submitMaterialReturn({
      taskId: task.taskId, taskNo: task.taskNo,
      materialCode: task.materialCode, materialName: task.materialName,
      qty, reason: returnReason.trim(), photos: returnPhotos
    }).then(res => {
      this.setData({ returnSubmitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '退料成功', icon: 'success', duration: 1500 })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.msg || res.message || '退料失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ returnSubmitting: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  }
})
