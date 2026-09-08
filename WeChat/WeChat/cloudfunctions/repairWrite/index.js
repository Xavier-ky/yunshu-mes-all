const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

exports.main = async (event) => {
  const { action } = event

  try {
    switch (action) {

      case 'submit': {
        const { deviceCode, deviceName, faultType, faultDescription, reportedBy } = event

        await db.collection('devices').where({ deviceCode }).update({ data: {
          status: 'REPAIRING', statusLabel: '维修中',
          faultCode: faultType || '', faultDescription: faultDescription || ''
        }})

        const taskId = 'TASK-' + Date.now()
        await db.collection('tasks').add({ data: {
          taskId, taskNo: taskId, taskType: 'REPAIR', taskTypeName: '维修任务',
          productName: deviceName || '', productModel: deviceCode,
          processName: '设备维修', workstationName: '',
          planQty: 0, completedQty: 0, qty: 0,
          status: 'PENDING', statusLabel: '待处理', priority: 'HIGH', priorityLabel: '高',
          deadline: '', assignedTo: '赵六', frozen: false, version: 0,
          description: faultDescription || '', createTime: new Date().toISOString(), updateTime: new Date().toISOString()
        }})

        return { code: 0, data: { taskId, deviceCode }, message: '报修成功' }
      }

      case 'complete': {
        const { deviceCode, repairResult, repairMethod, repairDuration } = event

        await db.collection('devices').where({ deviceCode }).update({ data: {
          status: 'RUNNING', statusLabel: '运行', faultDescription: ''
        }})

        const { data: tasks } = await db.collection('tasks')
          .where({ taskType: 'REPAIR', productModel: deviceCode, status: db.command.neq('COMPLETED') }).get()
        for (const t of tasks) {
          await db.collection('tasks').where({ taskId: t.taskId }).update({ data: {
            status: 'COMPLETED', statusLabel: '已完成',
            repairMethod: repairMethod || '', repairDuration: repairDuration || 0,
            updateTime: new Date().toISOString()
          }})
        }

        return { code: 0, message: '维修完成' }
      }

      default:
        return { code: -1, message: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[repairWrite]', action, err)
    return { code: -1, message: err.message || '操作失败' }
  }
}
