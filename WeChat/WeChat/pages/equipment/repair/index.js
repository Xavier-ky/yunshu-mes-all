import { getDeviceDetail, createRepairOrder, completeRepair } from '../../../services/equipment'
import { FAULT_TYPE_MAP, REPAIR_RESULT_MAP, PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'
import { consumePrefillData } from '../../../utils/ai-prefill'

const RESULT_OPTIONS = Object.entries(REPAIR_RESULT_MAP).map(([value, label]) => ({ value, label }))

const URGENCY_OPTIONS = [
  { value: 'LOW', label: '一般' },
  { value: 'MEDIUM', label: '紧急' },
  { value: 'HIGH', label: '非常紧急' }
]

const MAINTENANCE_TYPE_LIST = [
  { key: 'DAILY_CHECK', label: '日常点检' },
  { key: 'LUBRICATION', label: '定期润滑' },
  { key: 'FILTER_REPLACE', label: '滤网更换' },
  { key: 'CALIBRATION', label: '精度校准' },
  { key: 'WEAR_PARTS', label: '易损件更换' },
  { key: 'OTHER_MAINT', label: '其他保养' }
]

Page({
  data: {

    pageMode: 'create',
    taskId: '',
    deviceCode: '',

    form: {
      deviceCode: '',
      deviceName: '',
      lineCode: '',
      workstationCode: '',
      maintenanceType: 'repair',
      faultTypeIndex: -1,
      faultCode: '',
      maintenanceTypeIndex: -1,
      maintenanceCode: '',
      urgencyIndex: -1,
      urgency: '',
      faultDescription: '',
      images: []
    },
    faultTypeList: [],
    maintenanceTypeList: MAINTENANCE_TYPE_LIST,
    urgencyOptions: URGENCY_OPTIONS,

    processForm: {
      accepted: false,
      faultDescription: '',
      repairMethod: '',
      replacedParts: '',
      repairDuration: 0,
      resultIndex: -1,
      repairResult: '',
      remark: ''
    },
    resultOptions: RESULT_OPTIONS,
    accepting: false,

    deviceInfo: null,
    submitting: false
  },

  onLoad(options) {
    guardPageAccess(PAGE_PATH_MAP.equipmentRepair)
    const taskId = options.taskId || ''
    const deviceCode = options.deviceCode || ''

    if (taskId) {

      this.setData({
        pageMode: 'process',
        taskId,
        deviceCode
      })
      this.loadTaskInfo(taskId, deviceCode)
    } else {

      this.setData({ deviceCode })
      if (deviceCode) {
        this.setData({ 'form.deviceCode': deviceCode })
        this.loadDeviceInfo(deviceCode)
      }
    }
    this.initFaultTypeList()
    this.applyAiPrefill()
  },

  onShow() {
    this.applyAiPrefill()
  },

  applyAiPrefill() {
    if (this.data.pageMode !== 'create') return

    const prefill = consumePrefillData('/pages/equipment/repair/index')
    if (!prefill) {

      return
    }

    console.log('[设备报修] 读取到原始预填数据，类型:', typeof prefill, '内容:', JSON.stringify(prefill))

    const updates = {}

    if (prefill.faultDescription) {
      updates['form.faultDescription'] = prefill.faultDescription
    }
    if (prefill.deviceCode) {
      updates['form.deviceCode'] = prefill.deviceCode
    }
    if (prefill.deviceName) {
      updates['form.deviceName'] = prefill.deviceName
    }
    if (prefill.lineCode) {
      updates['form.lineCode'] = prefill.lineCode
    }
    if (prefill.workstationCode) {
      updates['form.workstationCode'] = prefill.workstationCode
    }

    if (prefill.faultType) {
      const idx = this.data.faultTypeList.findIndex(
        item => item.key === prefill.faultType
      )
      if (idx >= 0) {
        updates['form.faultTypeIndex'] = idx
        updates['form.faultCode'] = prefill.faultType
      }
    }

    console.log('[设备报修] 最终设置到表单的字段:', Object.keys(updates))

    if (Object.keys(updates).length > 0) {
      this.setData(updates)
      wx.showToast({ title: '已自动填入AI识别信息，请核对', icon: 'none', duration: 2000 })
    }
  },

  initFaultTypeList() {
    const faultTypeList = Object.entries(FAULT_TYPE_MAP).map(([key, label]) => ({
      key,
      label
    }))
    this.setData({ faultTypeList })
  },

  async loadDeviceInfo(deviceCode) {
    try {
      const res = await getDeviceDetail(deviceCode)
      if (res.code === 0 && res.data) {
        const device = res.data
        this.setData({
          'form.deviceName': device.deviceName || '',
          'form.lineCode': device.lineCode || '',
          'form.workstationCode': device.workstationCode || '',
          deviceInfo: device
        })
      }
    } catch (err) {  }
  },

  async loadTaskInfo(taskId, deviceCode) {
    if (deviceCode) {
      try {
        const res = await getDeviceDetail(deviceCode)
        if (res.code === 0 && res.data) {
          this.setData({
            deviceInfo: res.data,
            'processForm.faultDescription': res.data.faultDescription || ''
          })
        }
      } catch (err) {  }
    }
  },

  onDeviceCodeScan(e) {
    const code = e.detail.code || e.detail.value
    this.setData({ 'form.deviceCode': code, deviceCode: code })
    if (code && code !== this.data.deviceCode) {
      this.loadDeviceInfo(code)
    }
  },

  onDeviceCodeInput(e) {
    this.setData({ 'form.deviceCode': e.detail.value })
  },

  onTypeToggle(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ 'form.maintenanceType': type })
  },

  onUrgencyChange(e) {
    const index = parseInt(e.currentTarget.dataset.index)
    const option = this.data.urgencyOptions[index]
    this.setData({
      'form.urgencyIndex': index,
      'form.urgency': option ? option.value : ''
    })
  },

  onFieldInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onFaultTypeChange(e) {
    const index = parseInt(e.detail.value)
    if (this.data.form.maintenanceType === 'maintenance') {
      const item = this.data.maintenanceTypeList[index]
      this.setData({
        'form.maintenanceTypeIndex': index,
        'form.maintenanceCode': item ? item.key : ''
      })
    } else {
      const faultType = this.data.faultTypeList[index]
      this.setData({
        'form.faultTypeIndex': index,
        'form.faultCode': faultType ? faultType.key : ''
      })
    }
  },

  onAddImage() {
    wx.chooseImage({
      count: 9 - this.data.form.images.length,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const newImages = this.data.form.images.concat(res.tempFilePaths)
        this.setData({ 'form.images': newImages })
      }
    })
  },

  onPreviewImage(e) {
    const index = e.currentTarget.dataset.index
    wx.previewImage({
      current: this.data.form.images[index],
      urls: this.data.form.images
    })
  },

  onRemoveImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.form.images.filter((_, i) => i !== index)
    this.setData({ 'form.images': images })
  },

  validateCreateForm() {
    const { deviceCode, deviceName, faultDescription, maintenanceType, faultCode, maintenanceCode } = this.data.form
    if (!deviceCode || !deviceCode.trim()) {
      wx.showToast({ title: '请输入设备编号', icon: 'none' }); return false
    }
    if (!deviceName || !deviceName.trim()) {
      wx.showToast({ title: '请输入设备名称', icon: 'none' }); return false
    }
    if (maintenanceType === 'maintenance') {
      if (!maintenanceCode) {
        wx.showToast({ title: '请选择保养类型', icon: 'none' }); return false
      }
    } else {
      if (!faultCode) {
        wx.showToast({ title: '请选择故障类型', icon: 'none' }); return false
      }
    }
    if (!faultDescription || !faultDescription.trim()) {
      wx.showToast({ title: maintenanceType === 'maintenance' ? '请输入保养说明' : '请输入故障描述', icon: 'none' }); return false
    }
    return true
  },

  onCreateSubmit() {
    if (!this.validateCreateForm()) return
    this.setData({ submitting: true })

    const form = this.data.form
    const isMaintenance = form.maintenanceType === 'maintenance'
    const faultType = isMaintenance
      ? this.data.maintenanceTypeList[form.maintenanceTypeIndex]
      : this.data.faultTypeList[form.faultTypeIndex]

    createRepairOrder({
      deviceCode: form.deviceCode.trim(),
      deviceName: form.deviceName.trim(),
      lineCode: form.lineCode.trim(),
      workstationCode: form.workstationCode.trim(),
      maintenanceType: form.maintenanceType,
      faultCode: isMaintenance ? form.maintenanceCode : form.faultCode,
      faultName: faultType ? faultType.label : '',
      urgency: form.urgency || 'MEDIUM',
      faultDescription: form.faultDescription.trim(),
      images: form.images
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '报修成功', icon: 'success', duration: 2000 })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 2000)
      } else {
        wx.showToast({ title: res.message || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '网络异常', icon: 'none' })
    })
  },

  onAcceptTask() {
    const self = this
    wx.showModal({
      title: '接收维修任务',
      content: '确认接收该维修任务？接收后将开始处理。',
      success(res) {
        if (res.confirm) {
          self.setData({
            'processForm.accepted': true,
            accepting: false
          })
          wx.showToast({ title: '已接收任务', icon: 'success' })
        }
      }
    })
  },

  onProcessFieldInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`processForm.${field}`]: e.detail.value })
  },

  onResultChange(e) {
    const index = parseInt(e.currentTarget.dataset.index)
    const option = this.data.resultOptions[index]
    this.setData({
      'processForm.resultIndex': index,
      'processForm.repairResult': option.value
    })
  },

  onDurationInput(e) {
    this.setData({ 'processForm.repairDuration': parseInt(e.detail.value) || 0 })
  },

  onProcessSubmit() {
    const { processForm, deviceInfo, deviceCode, taskId } = this.data

    if (!processForm.repairMethod.trim()) {
      wx.showToast({ title: '请填写维修说明', icon: 'none' }); return
    }
    if (processForm.resultIndex < 0) {
      wx.showToast({ title: '请选择维修结果', icon: 'none' }); return
    }
    if ((parseInt(processForm.repairDuration) || 0) <= 0) {
      wx.showToast({ title: '请填写维修时长', icon: 'none' }); return
    }

    this.setData({ submitting: true })

    completeRepair({
      taskId,
      deviceCode,
      deviceName: deviceInfo?.deviceName || '',
      faultCode: deviceInfo?.faultCode || '',
      faultDescription: processForm.faultDescription || deviceInfo?.faultDescription || '',
      repairResult: REPAIR_RESULT_MAP[processForm.repairResult] || '',
      repairMethod: processForm.repairMethod.trim(),
      replacedParts: processForm.replacedParts.trim(),
      repairDuration: parseInt(processForm.repairDuration) || 0,
      repairStatus: processForm.repairResult,
      remark: processForm.remark.trim()
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '维修完成提交成功', icon: 'success' })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.message || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '网络异常', icon: 'none' })
    })
  },

  onCancel() {
    wx.navigateBack({ delta: 1 })
  },

  onSubmit() {
    if (this.data.pageMode === 'create') {
      this.onCreateSubmit()
    } else {
      this.onProcessSubmit()
    }
  }
})
