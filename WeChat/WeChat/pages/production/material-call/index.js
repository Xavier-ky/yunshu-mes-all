import { createMaterialCall, upgradeToAndon } from '../../../services/material-call'
import { PAGE_PATH_MAP } from '../../../utils/constants'

Page({
  data: {

    callId: '',
    workOrderNo: '',
    productName: '',
    processName: '',
    workstationName: '',
    lineName: '',

    materialName: '',
    materialCode: '',
    needQty: '',
    remainQty: '--',
    urgency: 'NORMAL',
    urgencyLabel: '一般',
    urgencyOptions: [
      { value: 'NORMAL', label: '一般' },
      { value: 'URGENT', label: '紧急' }
    ],
    urgencyIndex: 0,
    remark: '',

    submitting: false,
    submitted: false,
    resultCallId: '',
    canUpgrade: false
  },

  onLoad(options) {
    const fields = ['workOrderNo', 'productName', 'processName', 'workstationName', 'lineName']
    const data = {}
    fields.forEach(f => {
      if (options[f]) data[f] = decodeURIComponent(options[f])
    })
    if (options.callId) data.callId = decodeURIComponent(options.callId)
    if (options.canUpgrade === 'true') data.canUpgrade = true
    this.setData(data)
  },

  onWorkOrderInput(e) { this.setData({ workOrderNo: e.detail.value }) },
  onProductNameInput(e) { this.setData({ productName: e.detail.value }) },
  onProcessNameInput(e) { this.setData({ processName: e.detail.value }) },
  onWorkstationNameInput(e) { this.setData({ workstationName: e.detail.value }) },
  onMaterialInput(e) { this.setData({ materialName: e.detail.value }) },
  onMaterialCodeInput(e) { this.setData({ materialCode: e.detail.value }) },
  onNeedQtyInput(e) { this.setData({ needQty: e.detail.value }) },
  onRemainQtyInput(e) { this.setData({ remainQty: e.detail.value }) },
  onRemarkInput(e) { this.setData({ remark: e.detail.value }) },

  onUrgencyChange(e) {
    const index = e.detail.value
    const opt = this.data.urgencyOptions[index]
    this.setData({ urgencyIndex: index, urgency: opt.value, urgencyLabel: opt.label })
  },

  onSubmit() {
    const { materialName, needQty, urgency, workOrderNo, productName, processName, workstationName, lineName, remark, submitting } = this.data
    if (submitting) return
    if (!workOrderNo.trim()) { wx.showToast({ title: '请输入工单号', icon: 'none' }); return }
    if (!productName.trim()) { wx.showToast({ title: '请输入产品名称', icon: 'none' }); return }
    if (!materialName.trim()) { wx.showToast({ title: '请输入缺料物料', icon: 'none' }); return }
    if (!needQty || parseInt(needQty) <= 0) { wx.showToast({ title: '请填写缺料数量', icon: 'none' }); return }

    this.setData({ submitting: true })
    createMaterialCall({
      workOrderNo, productName, processName, workstationName, lineName,
      materialName: materialName.trim(),
      needQty: parseInt(needQty),
      urgency, urgencyLabel: this.data.urgencyLabel,
      remark: remark.trim(),
      operatorName: '当前操作工'
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        this.setData({ submitted: true, resultCallId: res.data.callId })
        wx.showToast({ title: '缺料呼叫已提交', icon: 'success' })
      } else {
        wx.showToast({ title: res.message || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '网络错误', icon: 'none' })
    })
  },

  onUpgradeToAndon() {
    const callId = this.data.resultCallId || this.data.callId
    if (!callId) return
    wx.showModal({
      title: '升级为安灯',
      content: '将缺料呼叫升级为安灯事件，通知将扩展至主管和仓库主管。确定升级？',
      success: (res) => {
        if (res.confirm) {
          upgradeToAndon(callId).then(res => {
            if (res.code === 0) {
              wx.showToast({ title: '已升级为安灯', icon: 'success' })
              const params = []
              const d = this.data
              if (d.workOrderNo) params.push('workOrderNo=' + encodeURIComponent(d.workOrderNo))
              if (d.lineName) params.push('lineName=' + encodeURIComponent(d.lineName))
              if (d.workstationName) params.push('workstationName=' + encodeURIComponent(d.workstationName))
              setTimeout(() => {
                wx.redirectTo({ url: PAGE_PATH_MAP.andonCreate + '?' + params.join('&') })
              }, 1000)
            } else {
              wx.showToast({ title: '升级失败', icon: 'none' })
            }
          })
        }
      }
    })
  },

  onBack() { wx.navigateBack({ delta: 1 }) }
})
