const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const _ = db.command

exports.main = async (event) => {
  const { action } = event

  try {
    switch (action) {

      case 'scheduleCreate': {
        const { lineName, productName, planQty, operatorName, endDate, routingSteps, toolingCode, standardHours, description } = event
        const scheduleNo = 'SCH-' + Date.now()
        const steps = (routingSteps || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
        const taskIds = []

        if (steps.length === 0) {
          const taskId = 'TASK-' + Date.now()
          taskIds.push(taskId)
          await db.collection('tasks').add({ data: {
            taskId, taskNo: taskId, taskType: 'PRODUCTION', taskTypeName: '生产任务',
            workOrderNo: scheduleNo, productName, productModel: '', processName: '生产加工',
            workstationName: lineName, planQty: Number(planQty), completedQty: 0, qty: Number(planQty),
            status: 'PENDING', statusLabel: '待处理', priority: 'MEDIUM', priorityLabel: '中',
            deadline: `${endDate} 18:00:00`, assignedTo: operatorName, toolingCode: toolingCode || '',
            routingSteps: [], stepIndex: 0, currentStepIndex: 0, standardHoursPerUnit: Number(standardHours) || 0,
            frozen: false, version: 0, description: description || '',
            createTime: new Date().toISOString(), updateTime: new Date().toISOString()
          }})
        } else {
          for (let i = 0; i < steps.length; i++) {
            const taskId = 'TASK-' + Date.now() + '-' + i
            taskIds.push(taskId)
            await db.collection('tasks').add({ data: {
              taskId, taskNo: taskId, taskType: 'PRODUCTION', taskTypeName: '生产任务',
              workOrderNo: scheduleNo, productName, productModel: '', processName: steps[i],
              workstationName: lineName, planQty: Number(planQty), completedQty: 0, qty: Number(planQty),
              status: i === 0 ? 'PENDING' : 'PENDING', statusLabel: i === 0 ? '待处理' : '等待上道完工',
              priority: 'MEDIUM', priorityLabel: '中',
              deadline: `${endDate} 18:00:00`, assignedTo: operatorName, toolingCode: toolingCode || '',
              routingSteps: steps, stepIndex: i, currentStepIndex: i,
              standardHoursPerUnit: Number(standardHours) || 0,
              frozen: false, version: 0, description: description || '',
              createTime: new Date().toISOString(), updateTime: new Date().toISOString()
            }})
          }
        }

        await db.collection('schedules').add({ data: {
          scheduleNo, lineName, productName, planQty: Number(planQty), completedQty: 0,
          operatorName, endDate, routingSteps: routingSteps || '', toolingCode: toolingCode || '',
          standardHours: Number(standardHours) || 0, description: description || '',
          status: 'PENDING', statusLabel: '待执行', taskIds,
          createTime: new Date().toISOString()
        }})

        return { code: 0, data: { scheduleNo, taskIds }, message: '排产布置成功' }
      }

      case 'taskReport': {
        const { taskId, goodQty, toolingCode } = event
        const addQty = Number(goodQty) || 0
        if (addQty <= 0) return { code: -1, message: '报工数量无效' }

        const { data: task } = await db.collection('tasks').where({ taskId }).get()
        if (!task || task.length === 0) return { code: -1, message: '任务不存在' }
        const t = task[0]

        if (t.frozen) return { code: -1, message: `工单已冻结：${t.freezeReason || '不合格品隔离'}` }
        if (t.toolingCode && toolingCode && t.toolingCode !== toolingCode) {
          return { code: -1, message: `工装不匹配，当前工序要求：${t.toolingCode}` }
        }

        const newQty = (t.completedQty || 0) + addQty
        if (newQty > t.planQty) return { code: -1, message: `超出计划余量 ${t.planQty - (t.completedQty || 0)}` }

        let status = t.status, statusLabel = t.statusLabel
        if (newQty >= t.planQty) { status = 'COMPLETED'; statusLabel = '已完成' }
        else if (newQty > 0 && t.status === 'PENDING') { status = 'PROCESSING'; statusLabel = '进行中' }

        await db.collection('tasks').where({ taskId }).update({ data: {
          completedQty: newQty, status, statusLabel,
          version: _.inc(1), updateTime: new Date().toISOString()
        }})

        if (t.workOrderNo) {
          const schedTasks = await db.collection('tasks').where({ workOrderNo: t.workOrderNo }).get()
          const totalDone = schedTasks.data.reduce((s, st) => s + (st.completedQty || 0), 0)
          await db.collection('schedules').where({ scheduleNo: t.workOrderNo }).update({ data: {
            completedQty: totalDone, status: status === 'COMPLETED' ? 'COMPLETED' : 'IN_PROGRESS',
            statusLabel: status === 'COMPLETED' ? '已完成' : '进行中'
          }})
        }

        return { code: 0, data: { taskId, completedQty: newQty }, message: '报工成功' }
      }

      case 'taskFreeze': {
        const { taskId, reason, approver } = event
        if (approver) {

          await db.collection('tasks').where({ taskId }).update({ data: {
            frozen: false, freezeReason: '', unfrozenBy: approver,
            status: 'PENDING', statusLabel: '待处理', updateTime: new Date().toISOString()
          }})
          return { code: 0, message: '已解冻' }
        }

        const { data: task } = await db.collection('tasks').where({ taskId }).get()
        if (task.length === 0) return { code: -1, message: '任务不存在' }
        const t = task[0]

        await db.collection('tasks').where({ taskId }).update({ data: {
          frozen: true, freezeReason: reason, freezeTime: new Date().toISOString(),
          _prevStatus: t.status, status: 'HOLD', statusLabel: '已冻结',
          updateTime: new Date().toISOString()
        }})

        if (t.workOrderNo) {
          await db.collection('tasks').where({ workOrderNo: t.workOrderNo, taskId: _.neq(taskId) }).update({ data: {
            frozen: true, freezeReason: `级联冻结：工单 ${t.workOrderNo}`, updateTime: new Date().toISOString()
          }})
        }
        return { code: 0, message: '已冻结' }
      }

      default:
        return { code: -1, message: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[taskWrite]', action, err)
    return { code: -1, message: err.message || '操作失败' }
  }
}
