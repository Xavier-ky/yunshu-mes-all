

import { getTasks } from '../../../services/production'
import { getCurrentUser } from '../../../utils/auth'
import { PAGE_PATH_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

const STATUS_LIST = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '维修中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' }
]

Page({
  data: {
    activeStatus: '',
    statusList: STATUS_LIST,

    stats: { pending: 0, processing: 0, completed: 0 },

    taskList: [],
    loading: false,
    _operatorName: ''
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.repairTaskList)
  },

  onShow() {
    const userInfo = getCurrentUser()
    this.setData({ _operatorName: userInfo?.realName || '' })
    this.loadStats()
    this.loadTasks()
  },

  async loadStats() {
    const name = this.data._operatorName
    if (!name) return
    try {
      const [pendingRes, processingRes, completedRes] = await Promise.allSettled([
        getTasks({ taskType: 'REPAIR', status: 'PENDING', assignedTo: name }),
        getTasks({ taskType: 'REPAIR', status: 'PROCESSING', assignedTo: name }),
        getTasks({ taskType: 'REPAIR', status: 'COMPLETED', assignedTo: name })
      ])
      const count = (res) => {
        if (res.status === 'fulfilled' && res.value?.code === 0 && res.value?.data) {
          return (res.value.data.list || []).length
        }
        return 0
      }
      this.setData({
        stats: {
          pending: count(pendingRes),
          processing: count(processingRes),
          completed: count(completedRes)
        }
      })
    } catch (_) {  }
  },

  async loadTasks() {
    this.setData({ loading: true })
    const { activeStatus, _operatorName } = this.data
    const params = { taskType: 'REPAIR' }
    if (activeStatus) params.status = activeStatus
    if (_operatorName) params.assignedTo = _operatorName

    try {
      const res = await getTasks(params)
      const list = (res && res.data && res.data.list) || []
      this.setData({ taskList: list, loading: false })
    } catch (_) {
      this.setData({ taskList: [], loading: false })
    }
  },

  onStatusChange(e) {
    const value = e.currentTarget.dataset.value
    if (value !== this.data.activeStatus) {
      this.setData({ activeStatus: value })
      this.loadTasks()
    }
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    const deviceCode = e.currentTarget.dataset.deviceCode
    if (taskId) {
      wx.navigateTo({
        url: `${PAGE_PATH_MAP.equipmentRepair}?taskId=${taskId}&deviceCode=${deviceCode || ''}`
      })
    }
  },

  onStatTap(e) {
    const status = e.currentTarget.dataset.status
    this.setData({ activeStatus: status })
    this.loadTasks()
  }
})
