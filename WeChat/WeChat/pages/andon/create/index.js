

import { createEvent } from '../../../services/andon'
import { ANDON_EVENT_TYPE_MAP, ANDON_REASON_MAP, ANDON_TARGET } from '../../../utils/constants'
import { consumePrefillData } from '../../../utils/ai-prefill'
import { getCurrentUser } from '../../../utils/auth'

const EVENT_TYPE_OPTIONS = Object.entries(ANDON_EVENT_TYPE_MAP).map(([value, label]) => ({
  value, label,
  target: ANDON_TARGET[value] || {}
}))

const REASON_BY_TYPE = {
  MATERIAL:  ['MATERIAL_SHORTAGE', 'MATERIAL_DEFECT'],
  EQUIPMENT: ['MACHINE_FAULT', 'MACHINE_STOP'],
  PROCESS:   ['PROCESS_CONFUSION', 'PROCESS_TOOLING'],
  QUALITY:   ['QUALITY_DEFECT', 'QUALITY_BATCH']
}

Page({
  data: {
    eventTypeIndex: -1,
    eventType: '',
    eventTypeOptions: EVENT_TYPE_OPTIONS,
    reasonCodeIndex: -1,
    reasonCode: '',
    reasonOptions: [],
    targetInfo: null,
    workOrderNo: '',
    lineCode: '', lineName: '',
    workstationCode: '', workstationName: '',
    deviceCode: '', deviceName: '',
    productName: '',
    description: '',
    images: [],
    submitting: false
  },

  onLoad(options) {
    const prefillFields = ['workOrderNo', 'lineCode', 'lineName', 'workstationCode', 'workstationName', 'deviceCode', 'productName']
    const dataToSet = {}
    prefillFields.forEach(field => {
      if (options[field]) dataToSet[field] = decodeURIComponent(options[field])
    })
    if (Object.keys(dataToSet).length > 0) this.setData(dataToSet)
    this.applyAiPrefill()
  },

  onShow() { this.applyAiPrefill() },

  applyAiPrefill() {
    const prefill = consumePrefillData('/pages/andon/create/index')
    if (!prefill) return
    const updates = {}
    if (prefill.description) updates.description = prefill.description
    if (prefill.deviceCode) updates.deviceCode = prefill.deviceCode
    if (prefill.workOrderNo) updates.workOrderNo = prefill.workOrderNo
    if (prefill.lineCode) updates.lineCode = prefill.lineCode
    if (prefill.eventType) {
      const idx = EVENT_TYPE_OPTIONS.findIndex(item => item.value === prefill.eventType)
      if (idx >= 0) {
        updates.eventTypeIndex = idx; updates.eventType = prefill.eventType
        this.updateTargetAndReasons(prefill.eventType, updates)
      }
    }
    if (Object.keys(updates).length > 0) {
      this.setData(updates)
      wx.showToast({ title: '已自动填入AI识别信息，请核对', icon: 'none', duration: 2000 })
    }
  },

  updateTargetAndReasons(eventType, updates) {
    const codes = REASON_BY_TYPE[eventType] || ['OTHER']
    const reasonOptions = codes.map(c => ({ value: c, label: ANDON_REASON_MAP[c] || c }))
    const target = ANDON_TARGET[eventType]
    updates.reasonOptions = reasonOptions
    updates.reasonCodeIndex = -1
    updates.reasonCode = ''
    updates.targetInfo = target ? {
      role: target.roleName, person: target.defaultPerson
    } : null
  },

  onTypeTap(e) {
    const eventType = e.currentTarget.dataset.type
    const index = EVENT_TYPE_OPTIONS.findIndex(o => o.value === eventType)
    const updates = { eventTypeIndex: index, eventType }
    this.updateTargetAndReasons(eventType, updates)
    this.setData(updates)
  },

  onEventTypeChange(e) {
    const index = parseInt(e.detail.value)
    const eventType = EVENT_TYPE_OPTIONS[index].value
    const updates = { eventTypeIndex: index, eventType }
    this.updateTargetAndReasons(eventType, updates)
    this.setData(updates)
  },

  onReasonCodeChange(e) {
    const index = parseInt(e.detail.value)
    const opts = this.data.reasonOptions
    this.setData({ reasonCodeIndex: index, reasonCode: opts[index] ? opts[index].value : '' })
  },

  onWorkOrderInput(e) { this.setData({ workOrderNo: e.detail.value }) },
  onLineCodeInput(e) { this.setData({ lineCode: e.detail.value }) },
  onWorkstationCodeInput(e) { this.setData({ workstationCode: e.detail.value }) },
  onDeviceCodeInput(e) { this.setData({ deviceCode: e.detail.value }) },
  onDescriptionInput(e) { this.setData({ description: e.detail.value }) },

  onChooseImages() {
    wx.chooseImage({
      count: 9, sizeType: ['compressed'], sourceType: ['camera', 'album'],
      success: (res) => this.setData({ images: this.data.images.concat(res.tempFilePaths) })
    })
  },
  onRemoveImage(e) {
    const index = e.currentTarget.dataset.index
    this.setData({ images: this.data.images.filter((_, i) => i !== index) })
  },

  onSubmit() {
    const { eventType, reasonCode, workOrderNo, lineCode, workstationCode, deviceCode, description, images, submitting } = this.data
    if (submitting) return
    if (!eventType) return wx.showToast({ title: '请选择安灯类型', icon: 'none' })
    if (!reasonCode) return wx.showToast({ title: '请选择安灯原因', icon: 'none' })
    if (!description.trim()) return wx.showToast({ title: '请输入问题描述', icon: 'none' })

    this.setData({ submitting: true })

    const target = ANDON_TARGET[eventType]
    const eventTypeName = ANDON_EVENT_TYPE_MAP[eventType]

    createEvent({
      eventType, eventTypeName,
      reasonCode, reasonName: ANDON_REASON_MAP[reasonCode] || '',
      workOrderNo: workOrderNo.trim(), lineCode: lineCode.trim(),
      workstationCode: workstationCode.trim(), deviceCode: deviceCode.trim(),
      description: description.trim(), images,
      createdBy: (getCurrentUser()?.realName) || '张三',
      creatorId: (getCurrentUser()?.userId) || '1',
      targetRole: target.role, targetRoleName: target.roleName,
      targetPerson: target.defaultPerson
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {

        wx.showToast({ title: `已通知${target.roleName}（${target.defaultPerson}），群聊已创建`, icon: 'success', duration: 2000 })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.message || '发起失败', icon: 'none' })
      }
    }).catch((err) => {
      this.setData({ submitting: false })
      wx.showToast({ title: (err && err.message) || '发起失败', icon: 'none', duration: 3000 })
    })
  }
})
