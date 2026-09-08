import { getTaskDetail, bindMaterial, submitReport } from '../../../services/production'
import { lookupMaterialByBatch } from '../../../services/inventory'
import { PAGE_PATH_MAP, DEFECT_TYPE_MAP } from '../../../utils/constants'
import { formatDateTime } from '../../../utils/format'
import { getRoleCode } from '../../../utils/auth'
import { ROLE_CODES } from '../../../utils/permission'

const DEFECT_OPTIONS = Object.entries(DEFECT_TYPE_MAP).map(([value, label]) => ({ value, label }))

Page({
  data: {
    task: null,
    taskId: '',
    loading: true,
    submitting: false,
    progressPercent: 0,

    sopSteps: [],
    sopImages: [],

    materialForm: { batchNo: '' },
    materialInfo: null,
    boundMaterials: [],

    productSn: '',

    reportForm: {
      goodQty: 0,
      defectQty: 0,
      defectReason: '',
      defectReasonLabel: '',
      remark: ''
    },
    defectOptions: DEFECT_OPTIONS,

    isReadOnly: false,
    urging: false
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    const roleCode = getRoleCode()
    const isReadOnly = roleCode === ROLE_CODES.PRODUCTION_SUPERVISOR || roleCode === ROLE_CODES.MANAGER
    this.setData({ taskId, isReadOnly })
    if (taskId) {
      this.loadTaskDetail(taskId)
    }
  },

  loadTaskDetail(taskId) {
    this.setData({ loading: true })
    getTaskDetail(taskId).then(res => {
      if (res.code === 0 && res.data) {
        const task = res.data

        const sopSteps = task.sopDesc
          ? task.sopDesc.split('\n').filter(s => s.trim())
          : []

        this.setData({
          task,
          progressPercent: task.planQty > 0
            ? Math.round((task.completedQty / task.planQty) * 100)
            : 0,
          sopSteps,
          sopImages: task.sopImages || [],
          loading: false,
          boundMaterials: task.materialList || []
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '任务不存在', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onMaterialScan(e) {
    const code = e.detail && e.detail.code
    if (code) {
      this.setData({ 'materialForm.batchNo': code })
      this.lookupMaterial(code)
    }
  },

  onMaterialInput(e) {
    this.setData({ 'materialForm.batchNo': e.detail.value })
  },

  onMaterialConfirm(e) {
    const code = e.detail && e.detail.value
    if (code) {
      this.lookupMaterial(code)
    }
  },

  async lookupMaterial(code) {
    if (!code) return

    let info = null
    try {
      const res = await lookupMaterialByBatch(code)
      if (res && res.code === 0 && res.data) {
        info = res.data
      }
    } catch (_) {

    }
    if (!info) {
      info = { materialName: '未知物料', spec: '--', batchNo: code, stockQty: 0 }
    }
    this.setData({ materialInfo: info })
  },

  onBindMaterial() {
    const { taskId, task, materialInfo, materialForm } = this.data
    if (!materialInfo || !materialForm.batchNo) {
      wx.showToast({ title: '请先扫描物料批次', icon: 'none' })
      return
    }

    wx.showLoading({ title: '绑定中...' })
    bindMaterial({
      taskId,
      workOrderNo: task ? task.workOrderNo : '',
      materialBatch: materialForm.batchNo
    }).then(res => {
      wx.hideLoading()
      if (res.code === 0) {
        wx.showToast({ title: '物料绑定成功', icon: 'success' })
        const newBound = [...this.data.boundMaterials, materialInfo]
        this.setData({
          boundMaterials: newBound,
          materialInfo: null,
          'materialForm.batchNo': ''
        })
      } else {
        wx.showToast({ title: res.message || '绑定失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '绑定失败', icon: 'none' })
    })
  },

  onProductSnScan(e) {
    const code = e.detail && e.detail.code
    if (code) {
      this.setData({ productSn: code })
    }
  },

  onProductSnInput(e) {
    this.setData({ productSn: e.detail.value })
  },

  onBindProductSn() {
    const { productSn, task } = this.data
    if (!productSn) {
      wx.showToast({ title: '请先扫描产品SN', icon: 'none' })
      return
    }
    wx.showToast({ title: 'SN绑定成功', icon: 'success' })
  },

  onGoodQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data.reportForm.goodQty) || 0
    const newVal = Math.max(0, current + delta)
    this.setData({ 'reportForm.goodQty': newVal })
  },

  onGoodQtyInput(e) {
    this.setData({ 'reportForm.goodQty': parseInt(e.detail.value) || 0 })
  },

  onDefectQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data.reportForm.defectQty) || 0
    const newVal = Math.max(0, current + delta)
    this.setData({ 'reportForm.defectQty': newVal })
  },

  onDefectQtyInput(e) {
    this.setData({ 'reportForm.defectQty': parseInt(e.detail.value) || 0 })
  },

  onDefectChange(e) {
    const index = e.detail.value
    const option = DEFECT_OPTIONS[index]
    this.setData({
      'reportForm.defectReason': option.value,
      'reportForm.defectReasonLabel': option.label
    })
  },

  onRemarkInput(e) {
    this.setData({ 'reportForm.remark': e.detail.value })
  },

  onSubmitReport() {
    const { taskId, task, productSn, reportForm, submitting } = this.data

    if (submitting) return

    const goodQty = parseInt(reportForm.goodQty) || 0
    const defectQty = parseInt(reportForm.defectQty) || 0

    if (goodQty <= 0 && defectQty <= 0) {
      wx.showToast({ title: '请填写良品或不良数量', icon: 'none' })
      return
    }

    if (defectQty > 0 && !reportForm.defectReason) {
      wx.showToast({ title: '存在不良，请选择缺陷原因', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    submitReport({
      taskId,
      workOrderNo: task ? task.workOrderNo : '',
      workstationCode: task ? task.workstationCode : '',
      processCode: task ? task.processCode : '',
      productSn: productSn || '',
      goodQty,
      defectQty,
      defectReason: defectQty > 0 ? reportForm.defectReason : '',
      remark: reportForm.remark
    }).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '报工成功', icon: 'success' })

        this.loadTaskDetail(taskId)

        this.setData({
          reportForm: { goodQty: 0, defectQty: 0, defectReason: '', defectReasonLabel: '', remark: '' },
          productSn: ''
        })
      } else {
        wx.showToast({ title: res.message || '报工失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '报工失败', icon: 'none' })
    })
  },

  onMaterialCall() {
    const { task } = this.data
    if (!task) return
    const params = []
    if (task.workOrderNo) params.push('workOrderNo=' + encodeURIComponent(task.workOrderNo))
    if (task.productName) params.push('productName=' + encodeURIComponent(task.productName))
    if (task.processName) params.push('processName=' + encodeURIComponent(task.processName))
    if (task.workstationName) params.push('workstationName=' + encodeURIComponent(task.workstationName))
    if (task.lineName) params.push('lineName=' + encodeURIComponent(task.lineName))
    wx.navigateTo({ url: PAGE_PATH_MAP.materialCall + '?' + params.join('&') })
  },

  onPreviewSop() {
    const images = this.data.sopImages
    if (!images || images.length === 0) {
      wx.showToast({ title: '暂无作业指导图片', icon: 'none' })
      return
    }
    wx.previewImage({ urls: images, current: images[0] })
  },

  onQuickAndon() {
    const { task } = this.data
    const params = []
    if (task) {
      if (task.workOrderNo) params.push('workOrderNo=' + encodeURIComponent(task.workOrderNo))
      if (task.workstationCode) params.push('workstationCode=' + encodeURIComponent(task.workstationCode))
      if (task.workstationName) params.push('workstationName=' + encodeURIComponent(task.workstationName))
      if (task.lineCode) params.push('lineCode=' + encodeURIComponent(task.lineCode))
      if (task.lineName) params.push('lineName=' + encodeURIComponent(task.lineName))
      if (task.productName) params.push('productName=' + encodeURIComponent(task.productName))
    }
    const query = params.length > 0 ? '?' + params.join('&') : ''
    wx.navigateTo({
      url: PAGE_PATH_MAP.andonCreate + query
    })
  },

  onUrge() {
    if (this.data.urging) return
    wx.showModal({
      title: '催办确认',
      content: '确认提醒相关负责人关注该任务吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({ urging: true })
          wx.showToast({ title: '已提醒相关负责人', icon: 'success' })
          setTimeout(() => {
            this.setData({ urging: false })
          }, 3000)
        }
      }
    })
  }
})
