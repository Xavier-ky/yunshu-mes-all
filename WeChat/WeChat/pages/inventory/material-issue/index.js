import { getIssueTasks, getReturnTasks, submitMaterialReturn as submitReturnToService } from '../../../services/inventory'
import { formatDateTime } from '../../../utils/format'
import { PAGE_PATH_MAP } from '../../../utils/constants'

Page({
  data: {
    activeTab: 'issue',
    issueList: [],
    returnList: [],
    loading: false,
    loaded: false,

    showReturnForm: false,
    returnForm: {
      materialCode: '',
      materialName: '',
      spec: '',
      qty: '',
      batchNo: '',
      reason: ''
    },
    submittingReturn: false
  },

  onShow() {
    this.loadData()
  },

  loadData() {
    this.setData({ loading: true })
    const { activeTab } = this.data

    if (activeTab === 'issue') {
      getIssueTasks().then(res => {
        if (res.code === 0 && res.data) {
          this.setData({
            issueList: (res.data.list || []).map(item => ({
              ...item,
              createTimeStr: formatDateTime(item.createTime)
            }))
          })
        }
      }).finally(() => {
        this.setData({ loading: false, loaded: true })
      })
    } else {
      getReturnTasks().then(res => {
        if (res.code === 0 && res.data) {
          this.setData({
            returnList: (res.data.list || []).map(item => ({
              ...item,
              createTimeStr: formatDateTime(item.createTime)
            }))
          })
        }
      }).finally(() => {
        this.setData({ loading: false, loaded: true })
      })
    }
  },

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab !== this.data.activeTab) {
      this.setData({ activeTab: tab, loaded: false })
      this.loadData()
    }
  },

  onCardTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.inventoryTaskDetail}?taskId=${taskId}` })
    }
  },

  onReturnTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.returnDetail}?taskId=${taskId}` })
    }
  },

  onNewReturn() {
    this.setData({ showReturnForm: true })
  },

  onCancelReturn() {
    this.setData({
      showReturnForm: false,
      returnForm: { materialCode: '', materialName: '', spec: '', qty: '', batchNo: '', reason: '' }
    })
  },

  onReturnField(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`returnForm.${field}`]: e.detail.value })
  },

  onSubmitReturn() {
    const f = this.data.returnForm
    if (!f.materialCode.trim()) return wx.showToast({ title: '请输入物料编码', icon: 'none' })
    if (!f.materialName.trim()) return wx.showToast({ title: '请输入物料名称', icon: 'none' })
    if (!f.qty || parseInt(f.qty) <= 0) return wx.showToast({ title: '请输入有效数量', icon: 'none' })
    if (!f.reason.trim()) return wx.showToast({ title: '请填写退料原因', icon: 'none' })

    this.setData({ submittingReturn: true })

    const newReturn = {
      taskId: 'MAT-RETURN-' + Date.now(),
      taskNo: 'MAT-RETURN-' + Date.now(),
      workOrderNo: '',
      materialCode: f.materialCode.trim(),
      materialName: f.materialName.trim(),
      spec: f.spec.trim(),
      batchNo: f.batchNo.trim(),
      warehouseName: '原材料仓',
      locationCode: '--',
      qty: parseInt(f.qty),
      unit: '个',
      reason: f.reason.trim()
    }

    submitReturnToService(newReturn)
      .then(() => {
        wx.showToast({ title: '退料已提交', icon: 'success' })
        this.setData({ submittingReturn: false, showReturnForm: false })
        this.loadData()
      })
      .catch(err => {
        wx.showToast({ title: err.message || '退料失败', icon: 'none' })
        this.setData({ submittingReturn: false })
      })
  }
})
