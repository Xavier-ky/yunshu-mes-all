import { getTasks } from '../../../services/quality'
import { PAGE_PATH_MAP, INSPECTION_TYPE_MAP } from '../../../utils/constants'
import { guardPageAccess } from '../../../utils/permission'

const STATUS_LIST = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '进行中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '异常', value: 'ABNORMAL' }
]

const TYPE_LIST = [
  { label: '全部', value: '' },
  { label: '首检', value: 'FIRST_INSPECTION' },
  { label: '巡检', value: 'PATROL_INSPECTION' },
  { label: '完工检验', value: 'FINAL_INSPECTION' },
  { label: '入库检验', value: 'WAREHOUSE_INSPECTION' }
]

Page({
  data: {
    activeType: '',
    activeStatus: '',
    taskList: [],
    loading: false,
    typeList: TYPE_LIST,
    statusList: STATUS_LIST
  },

  onLoad() {
    guardPageAccess(PAGE_PATH_MAP.qualityTaskList)
  },

  onShow() {
    this.loadTasks()
  },

  loadTasks() {
    this.setData({ loading: true })
    const params = {}
    if (this.data.activeType) params.inspectionType = this.data.activeType
    if (this.data.activeStatus) params.status = this.data.activeStatus
    getTasks(params).then(res => {
      if (res.code === 0) {
        this.setData({ taskList: res.data.list || [], loading: false })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  onTypeChange(e) {
    this.setData({ activeType: e.currentTarget.dataset.value })
    this.loadTasks()
  },

  onStatusChange(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.value })
    this.loadTasks()
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) {
      wx.navigateTo({
        url: `${PAGE_PATH_MAP.qualityInspect}?taskId=${taskId}`
      })
    }
  }
})
