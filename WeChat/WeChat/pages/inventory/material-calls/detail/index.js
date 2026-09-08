import { getMaterialCallDetail, processMaterialCall, completeMaterialCall } from '../../../../services/material-call'
import { PAGE_PATH_MAP } from '../../../../utils/constants'

Page({
  data: {
    call: null,
    callId: '',
    loading: true,

    actualQty: '',
    batchNo: '',
    estimatedArrival: '',
    processing: false
  },

  onLoad(options) {
    const callId = options.callId || ''
    if (callId) {
      this.setData({ callId })
      this.loadDetail(callId)
    }
  },

  loadDetail(callId) {
    this.setData({ loading: true })
    getMaterialCallDetail(callId).then(res => {
      if (res.code === 0 && res.data) {
        const call = res.data
        this.setData({
          call,
          actualQty: call.actualQty || call.needQty || '',
          batchNo: call.batchNo || '',
          estimatedArrival: call.estimatedArrival || '',
          loading: false
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '呼叫单不存在', icon: 'none' })
      }
    }).catch(() => { this.setData({ loading: false }) })
  },

  onActualQtyInput(e) { this.setData({ actualQty: e.detail.value }) },
  onBatchNoInput(e) { this.setData({ batchNo: e.detail.value }) },
  onArrivalInput(e) { this.setData({ estimatedArrival: e.detail.value }) },

  onProcess() {
    const { callId, actualQty, batchNo, estimatedArrival, processing } = this.data
    if (processing) return
    if (!actualQty || parseInt(actualQty) <= 0) {
      wx.showToast({ title: '请填写发料数量', icon: 'none' }); return
    }
    this.setData({ processing: true })
    processMaterialCall(callId, {
      actualQty: parseInt(actualQty),
      batchNo: batchNo.trim(),
      estimatedArrival: estimatedArrival.trim()
    }).then(res => {
      this.setData({ processing: false })
      if (res.code === 0) {
        wx.showToast({ title: '已确认，物料配送中', icon: 'success' })
        this.loadDetail(callId)
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    }).catch(() => { this.setData({ processing: false }) })
  },

  onComplete() {
    const { callId } = this.data
    wx.showModal({
      title: '确认送达',
      content: '确认物料已配送完成？',
      success: (res) => {
        if (res.confirm) {
          completeMaterialCall(callId).then(res => {
            if (res.code === 0) {
              wx.showToast({ title: '配送已完成', icon: 'success' })
              this.loadDetail(callId)
            }
          })
        }
      }
    })
  },

  onViewAndon() {
    const { call } = this.data
    if (call && call.andonId) {
      wx.navigateTo({ url: PAGE_PATH_MAP.andonDetail + '?id=' + call.andonId })
    }
  }
})
