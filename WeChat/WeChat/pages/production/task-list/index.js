import { getTasks } from '../../../services/production'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

Page({
  data: {
    activeStatus: '',
    statusList: [
      { label: '全部', value: '' },
      { label: '待处理', value: 'PENDING' },
      { label: '进行中', value: 'PROCESSING' },
      { label: '已完成', value: 'COMPLETED' },
      { label: '异常', value: 'ABNORMAL' }
    ],
    taskList: [],
    loading: false
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.productionTaskList)
  },

  onShow() {
    this.loadTasks()
  },

  loadTasks() {
    this.setData({ loading: true })
    const params = { taskType: 'PRODUCTION' }
    if (this.data.activeStatus) {
      params.status = this.data.activeStatus
    }
    getTasks(params).then(res => {
      if (res.code === 0) {
        this.setData({
          taskList: res.data.list || [],
          loading: false
        })
      } else {
        this.setData({ loading: false })
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onStatusChange(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ activeStatus: value })
    this.loadTasks()
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    wx.navigateTo({
      url: PAGE_PATH_MAP.productionTaskDetail + '?taskId=' + taskId
    })
  }
})
