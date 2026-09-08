import { getTaskDetail, submitInspection } from '../../../services/quality'
import { QUALITY_DEFECT_TYPE_MAP, INSPECTION_ACTION_MAP } from '../../../utils/constants'

const DEFECT_TYPE_OPTIONS = Object.entries(QUALITY_DEFECT_TYPE_MAP).map(([value, label]) => ({ value, label }))
const ACTION_OPTIONS = Object.entries(INSPECTION_ACTION_MAP).map(([value, label]) => ({ value, label }))
const ANDON_ACTIONS = [
  { value: 'REWORK', label: '返工处理' },
  { value: 'SCRAP', label: '报废处理' },
  { value: 'MATERIAL', label: '物料异常上报' },
  { value: 'EQUIPMENT', label: '设备异常上报' }
]

Page({
  data: {
    taskId: '', taskDetail: null, loading: true, submitting: false,
    productSn: '',

    checklist: [],
    allChecked: false,

    sampleQty: '',
    goodQty: 0,

    defects: [],
    defectTypeOptions: DEFECT_TYPE_OPTIONS,

    needAndon: false,
    andonDesc: '',
    andonActionOptions: ANDON_ACTIONS,
    andonActionIndex: 0,

    action: '',
    actionOptions: ACTION_OPTIONS,

    remark: '',
    inspectionStandard: ''
  },

  onLoad(options) {
    const taskId = options.taskId || ''
    if (taskId) { this.setData({ taskId }); this.loadTaskDetail(taskId) }
    else { this.setData({ loading: false }) }
  },

  loadTaskDetail(taskId) {
    this.setData({ loading: true })
    getTaskDetail(taskId).then(res => {
      if (res.code === 0 && res.data) {
        const taskDetail = res.data
        const items = taskDetail.inspectionItems || []
        const checklist = items.map(item => ({
          itemName: item.itemName,
          standard: item.standard,
          checked: false
        }))
        this.setData({
          taskDetail,
          productSn: taskDetail.productSn || '',
          checklist,
          inspectionStandard: checklist.length > 0 ? '' : (taskDetail.inspectionStandard || ''),
          loading: false
        })
      } else {
        this.setData({ loading: false, taskDetail: null })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  calcDefect() {
    const sample = parseInt(this.data.sampleQty) || 0
    const good = parseInt(this.data.goodQty) || 0
    const defect = Math.max(0, sample - good)
    const prev = this.data.defectAuto
    this.setData({ defectAuto: defect })

    if (defect === 0 && prev > 0) {
      this.setData({ defects: [], needAndon: false, andonDesc: '', action: '' })
    }

    if (defect < 3 && prev >= 3) {
      this.setData({ needAndon: false })
    }
  },

  onCheckItem(e) {
    const index = e.currentTarget.dataset.index
    const checklist = this.data.checklist.slice()
    checklist[index] = { ...checklist[index], checked: !checklist[index].checked }
    const allChecked = checklist.every(i => i.checked)
    this.setData({ checklist, allChecked })
  },

  onProductSnScan() {
    wx.scanCode({ success: (res) => { this.setData({ productSn: res.result }) } })
  },
  onProductSnInput(e) { this.setData({ productSn: e.detail.value }) },

  onSampleQtyInput(e) {
    this.setData({ sampleQty: e.detail.value }, () => this.calcDefect())
  },
  onGoodQtyChange(e) {
    const delta = parseInt(e.currentTarget.dataset.delta)
    const current = parseInt(this.data.goodQty) || 0
    this.setData({ goodQty: Math.max(0, current + delta) }, () => this.calcDefect())
  },
  onGoodQtyInput(e) {
    this.setData({ goodQty: parseInt(e.detail.value) || 0 }, () => this.calcDefect())
  },

  onAddDefect() {
    const defects = this.data.defects.slice()
    defects.push({ typeIndex: -1, defectCode: '', defectName: '', qty: '', desc: '', images: [] })
    this.setData({ defects })
  },
  onRemoveDefect(e) {
    const idx = parseInt(e.currentTarget.dataset.index)
    this.setData({ defects: this.data.defects.filter((_, i) => i !== idx) })
  },
  onDefectTypeChange(e) {
    const idx = parseInt(e.currentTarget.dataset.index)
    const ti = parseInt(e.detail.value)
    const opt = DEFECT_TYPE_OPTIONS[ti]
    const defects = this.data.defects.slice()
    defects[idx] = { ...defects[idx], typeIndex: ti, defectCode: opt.value, defectName: opt.label }
    this.setData({ defects })
  },
  onDefectQtyChange(e) {
    const idx = parseInt(e.currentTarget.dataset.index)
    const defects = this.data.defects.slice()
    defects[idx] = { ...defects[idx], qty: e.detail.value }
    this.setData({ defects })
  },
  onDefectDescChange(e) {
    const idx = parseInt(e.currentTarget.dataset.index)
    const defects = this.data.defects.slice()
    defects[idx] = { ...defects[idx], desc: e.detail.value }
    this.setData({ defects })
  },

  onToggleAndon() {
    const toggled = !this.data.needAndon
    this.setData({ needAndon: toggled })
    if (toggled && !this.data.andonDesc) {
      const task = this.data.taskDetail
      const desc = `产品 ${task ? task.productName : ''}（SN: ${this.data.productSn}）抽检发现 ${this.data.defectAuto} 件不良`
      this.setData({ andonDesc: desc })
    }
  },
  onAndonDescInput(e) { this.setData({ andonDesc: e.detail.value }) },
  onAndonActionChange(e) {
    this.setData({ andonActionIndex: parseInt(e.detail.value) })
  },

  onActionChange(e) {
    this.setData({ action: e.currentTarget.dataset.value })
  },

  onRemarkInput(e) { this.setData({ remark: e.detail.value }) },

  onSubmit() {
    const {
      taskId, taskDetail, productSn, checklist, sampleQty, goodQty, defectAuto,
      defects, needAndon, andonDesc, andonActionOptions, andonActionIndex,
      action, remark, submitting
    } = this.data

    if (submitting) return

    if (checklist.length > 0 && !checklist.every(i => i.checked)) {
      wx.showToast({ title: '请确认所有检验标准项', icon: 'none' }); return
    }
    if (!productSn.trim()) {
      wx.showToast({ title: '请扫描或输入产品SN', icon: 'none' }); return
    }
    const sample = parseInt(sampleQty) || 0
    if (sample <= 0) {
      wx.showToast({ title: '请输入抽检数量', icon: 'none' }); return
    }
    const good = parseInt(goodQty) || 0
    if (good + defectAuto !== sample) {
      wx.showToast({ title: '合格+不良应等于抽检总数', icon: 'none' }); return
    }
    if (defectAuto > 0) {
      const validDefects = defects.filter(d => d.typeIndex >= 0 && parseInt(d.qty) > 0)
      if (validDefects.length === 0) {
        wx.showToast({ title: '不良时请登记至少一条缺陷', icon: 'none' }); return
      }
      if (!action) {
        wx.showToast({ title: '请选择处置动作', icon: 'none' }); return
      }
    }

    this.setData({ submitting: true })

    const defectRecords = defects
      .filter(d => d.typeIndex >= 0 && parseInt(d.qty) > 0)
      .map(d => ({
        defectCode: d.defectCode, defectName: d.defectName,
        defectQty: parseInt(d.qty) || 0, description: d.desc || '', images: d.images || []
      }))

    const recordData = {
      inspectionTaskId: taskDetail.inspectionTaskId || taskDetail.taskId || taskId,
      workOrderNo: taskDetail.workOrderNo,
      productSn: productSn.trim(),
      inspectionType: taskDetail.inspectionType,
      result: defectAuto > 0 ? 'UNQUALIFIED' : 'QUALIFIED',
      sampleQty: sample,
      goodQty: good,
      defectQty: defectAuto,
      defects: defectRecords,
      action: defectAuto > 0 ? action : 'RELEASE',
      remark: remark.trim(),
      needAndon,
      andonDesc: needAndon ? andonDesc.trim() : '',
      andonAction: needAndon ? andonActionOptions[andonActionIndex].label : ''
    }

    submitInspection(recordData).then(res => {
      this.setData({ submitting: false })
      if (res.code === 0) {
        wx.showToast({ title: '质检提交成功', icon: 'success' })
        setTimeout(() => wx.navigateBack({ delta: 1 }), 1500)
      } else {
        wx.showToast({ title: res.message || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ submitting: false })
      wx.showToast({ title: '提交失败', icon: 'none' })
    })
  }
})
