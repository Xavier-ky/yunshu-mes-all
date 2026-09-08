

import { getSchedules, doCreateSchedule, doDeleteSchedule } from '../../../services/scheduling'
import { getOperators } from '../../../services/scheduling'

const LINE_OPTIONS = ['风扇总装一线', '风扇总装二线', '电机组装线', '网罩组装线', '喷涂线', '包装线']

Page({
  data: {

    form: {
      lineName: '',
      productName: '',
      planQty: '',
      operatorName: '',
      operatorId: '',
      endDate: '',
      routingSteps: '',
      toolingCode: '',
      standardHours: '',
      description: ''
    },
    lineOptions: LINE_OPTIONS,
    lineIndex: -1,
    operatorNames: [],
    operatorList: [],
    operatorIndex: -1,
    today: '',
    submitting: false,

    recentList: []
  },

  onLoad() {
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
    this.setData({ today })
    this.loadOperators()
  },

  onShow() {
    this.loadRecent()
  },

  loadOperators(lineName) {
    getOperators(lineName).then(res => {
      if (res.code === 0 && res.data) {
        const list = res.data.list || []
        const names = list.map(o => `${o.name}（${o.lineName}）`)
        this.setData({ operatorNames: names, operatorList: list })
      }
    })
  },

  loadRecent() {
    getSchedules().then(res => {
      if (res.code === 0 && res.data) {
        const list = (res.data.list || []).slice(0, 10)
        this.setData({ recentList: list })
      }
    }).catch(() => {  })
  },

  onLinePick(e) {
    const idx = parseInt(e.detail.value)
    const lineName = LINE_OPTIONS[idx]
    this.setData({
      lineIndex: idx,
      'form.lineName': lineName,
      'form.operatorName': '',
      'form.operatorId': '',
      operatorIndex: -1
    })
    this.loadOperators(lineName)
  },

  onOperatorPick(e) {
    const idx = parseInt(e.detail.value)
    const op = this.data.operatorList[idx]
    if (op) {
      this.setData({
        operatorIndex: idx,
        'form.operatorName': op.name,
        'form.operatorId': op.id
      })
    }
  },

  onDatePick(e) {
    this.setData({ 'form.endDate': e.detail.value })
  },

  onField(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onSubmit() {
    const { form } = this.data

    if (!form.lineName) return wx.showToast({ title: '请选择产线', icon: 'none' })
    if (!form.operatorName) return wx.showToast({ title: '请选择执行人', icon: 'none' })
    if (!form.productName.trim()) return wx.showToast({ title: '请输入产品名称', icon: 'none' })
    if (!form.planQty || parseInt(form.planQty) <= 0) return wx.showToast({ title: '请输入有效数量', icon: 'none' })
    if (!form.endDate) return wx.showToast({ title: '请选择完成日期', icon: 'none' })

    this.setData({ submitting: true })

    const payload = {
      lineName: form.lineName,
      productName: form.productName.trim(),
      planQty: parseInt(form.planQty),
      operatorName: form.operatorName,
      operatorId: form.operatorId,
      startDate: this.data.today,
      endDate: form.endDate,
      priority: 'MEDIUM',
      routingSteps: form.routingSteps.trim(),
      toolingCode: form.toolingCode.trim(),
      standardHours: parseInt(form.standardHours) || 0,
      description: form.description.trim()
    }

    doCreateSchedule(payload).then(res => {
      if (res.code === 0) {
        const opName = form.operatorName
        wx.showToast({ title: `已向 ${opName} 布置任务`, icon: 'success', duration: 2000 })

        this.setData({
          'form.productName': '',
          'form.planQty': '',
          'form.routingSteps': '',
          'form.toolingCode': '',
          'form.standardHours': '',
          'form.description': '',
          submitting: false
        })

        this.loadRecent()
      } else {
        this.setData({ submitting: false })
        wx.showToast({ title: res.msg || '布置失败', icon: 'none' })
      }
    }).catch((err) => {
      this.setData({ submitting: false })
      wx.showToast({ title: (err && err.message) || '布置失败', icon: 'none' })
    })
  },

  onDeleteItem(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.recentList.find(s => s.id === id)
    if (!item) return
    if (item.status !== 'PENDING') {
      return wx.showToast({ title: '仅可删除待执行任务', icon: 'none' })
    }

    wx.showModal({
      title: '撤销布置',
      content: `确定撤销「${item.productName}」的布置吗？操作工的任务将同步移除。`,
      confirmText: '撤销',
      confirmColor: '#FF4757',
      success: (res) => {
        if (res.confirm) {
          doDeleteSchedule(id).then(r => {
            if (r.code === 0) {
              wx.showToast({ title: '已撤销', icon: 'success' })
              this.loadRecent()
            }
          })
        }
      }
    })
  }
})
