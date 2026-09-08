

import { getRoleCode, getCurrentUser } from '../../utils/auth'
import { getAllowedTaskTypes, ROLE_CODES } from '../../utils/permission'
import { PAGE_PATH_MAP } from '../../utils/constants'
import { getTasks as getProductionTasks } from '../../services/production'
import { getEvents } from '../../services/andon'
import { getMaterialCalls } from '../../services/inventory'

const STATUS_LIST = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '进行中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' }
]

Page({
  data: {
    activeTab: 'task',
    activeStatus: '',
    pageTitle: '我的任务',
    taskList: [],
    andonList: [],
    repairList: [],
    materialList: [],
    qualityList: [],
    loading: false,
    statusList: STATUS_LIST,
    isReadOnly: false
  },

  onShow() {
    const roleCode = getRoleCode()
    const isReadOnly = roleCode === ROLE_CODES.PRODUCTION_SUPERVISOR ||
      roleCode === ROLE_CODES.MANAGER

    const userInfo = getCurrentUser()
    const operatorName = (userInfo?.realName) || ''

    const allowedTypes = getAllowedTaskTypes()
    const pageTitle = allowedTypes.length === 0 ? '任务总览' : '我的任务'

    this.setData({ isReadOnly, pageTitle, _operatorName: operatorName })
    this.loadCurrentTab()
  },

  loadCurrentTab() {
    const { activeTab, activeStatus } = this.data
    this.setData({ loading: true })

    switch (activeTab) {
      case 'task': this.loadTasks(activeStatus); break
      case 'andon': this.loadAndon(); break
      case 'repair': this.loadRepair(); break
      case 'material': this.loadMaterial(); break
      case 'quality': this.loadQuality(); break
    }
  },

  async loadTasks(status) {
    try {
      const allowedTypes = getAllowedTaskTypes()
      const res = await getProductionTasks({ status: status || undefined, taskType: allowedTypes.length === 1 ? allowedTypes[0] : undefined })
      const list = (res && res.data && res.data.list) || []
      this.setData({ taskList: list, loading: false })
    } catch (_) {
      this.setData({ taskList: [], loading: false })
    }
  },

  async loadAndon() {
    try {
      const name = this.data._operatorName
      const res = await getEvents()
      const all = (res && res.data && res.data.list) || []
      const mine = name ? all.filter(e => e.createdBy === name || e.createdByName === name) : all
      this.setData({ andonList: mine, loading: false })
    } catch (_) {
      this.setData({ andonList: [], loading: false })
    }
  },

  async loadRepair() {
    try {
      const name = this.data._operatorName
      const rc = getRoleCode()
      const res = await getProductionTasks({ taskType: 'REPAIR' })
      const all = (res && res.data && res.data.list) || []

      let mine
      if (rc === ROLE_CODES.EQUIPMENT_MAINTAINER) {
        mine = name ? all.filter(t => t.assignedTo === name) : all
      } else if (rc === ROLE_CODES.LINE_OPERATOR) {
        mine = name ? all.filter(t => t.reportedBy === name) : all
      } else {
        mine = all
      }
      this.setData({ repairList: mine, loading: false })
    } catch (_) {
      this.setData({ repairList: [], loading: false })
    }
  },

  async loadMaterial() {
    try {
      const name = this.data._operatorName
      const res = await getMaterialCalls()
      const all = (res && res.data && res.data.list) || []
      const mine = name ? all.filter(c => c.operatorName === name) : all
      this.setData({ materialList: mine, loading: false })
    } catch (_) {
      this.setData({ materialList: [], loading: false })
    }
  },

  async loadQuality() {
    try {
      const name = this.data._operatorName
      const res = await getProductionTasks({ taskType: 'QUALITY' })
      const all = (res && res.data && res.data.list) || []
      const mine = name ? all.filter(t => t.assignedTo === name) : all
      this.setData({ qualityList: mine, loading: false })
    } catch (_) {
      this.setData({ qualityList: [], loading: false })
    }
  },

  onTabSwitch(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab !== this.data.activeTab) {
      this.setData({ activeTab: tab, activeStatus: '' })
      this.loadCurrentTab()
    }
  },

  onStatusChange(e) {
    this.setData({ activeStatus: e.currentTarget.dataset.value })
    this.loadTasks(this.data.activeStatus)
  },

  onTaskTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.productionTaskDetail}?taskId=${taskId}` })
    }
  },

  onAndonTap(e) {
    const eventId = e.currentTarget.dataset.eventId
    if (eventId) wx.navigateTo({ url: `${PAGE_PATH_MAP.andonDetail}?eventId=${eventId}` })
  },

  onRepairTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    const deviceCode = e.currentTarget.dataset.deviceCode
    if (!taskId) return
    const rc = getRoleCode()

    if (rc === ROLE_CODES.EQUIPMENT_MAINTAINER) {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.equipmentRepair}?taskId=${taskId}&deviceCode=${deviceCode || ''}` })
    } else {
      wx.navigateTo({ url: `${PAGE_PATH_MAP.repairDetail}?taskId=${taskId}` })
    }
  },

  onMaterialTap(e) {
    const callId = e.currentTarget.dataset.callId
    if (callId) wx.navigateTo({ url: `/pages/tasks/detail/index?type=material&id=${callId}` })
  },

  onQualityTap(e) {
    const taskId = e.currentTarget.dataset.taskId
    if (taskId) wx.navigateTo({ url: `/pages/tasks/detail/index?type=quality&id=${taskId}` })
  },

  onUrge(e) {
    const taskId = e.currentTarget.dataset.taskid
    wx.showModal({
      title: '催办确认',
      content: '确认提醒相关负责人关注该任务吗？',
      success: (res) => {
        if (res.confirm) wx.showToast({ title: '已提醒相关负责人', icon: 'success' })
      }
    })
  }
})
