import { getMaterialCalls } from '../../../services/material-call'
import { PAGE_PATH_MAP } from '../../../utils/constants'

Page({
  data: {
    activeStatus: '',
    statusList: [
      { label: '全部', value: '' },
      { label: '待处理', value: 'PENDING' },
      { label: '处理中', value: 'PROCESSING' },
      { label: '已完成', value: 'COMPLETED' },
      { label: '已升级', value: 'UPGRADED' }
    ],
    callList: [],
    loading: false
  },

  onShow() {
    this.loadCalls() },

  loadCalls() {
    this.setData({ loading: true })
    const params = {}
    if (this.data.activeStatus) params.status = this.data.activeStatus
    getMaterialCalls(params).then(res => {
      if (res.code === 0) {
        this.setData({ callList: res.data.list || [], loading: false })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => this.setData({ loading: false }))
  },

  onStatusChange(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.value })
    this.loadCalls()
  },

  onCallTap(e) {
    const callId = e.currentTarget.dataset.callId
    wx.navigateTo({ url: PAGE_PATH_MAP.materialCallDetail + '?callId=' + callId })
  }
})
