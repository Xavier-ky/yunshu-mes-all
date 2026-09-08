

const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const _ = db.command

async function resolveUserRole(openid) {
  if (!openid) return null
  const { data } = await db.collection('users').where({ openid }).limit(1).get()
  if (data.length === 0) return null
  return { userId: data[0].userId, realName: data[0].realName, roleCode: data[0].roleCode }
}

exports.main = async (event, context) => {
  const { OPENID } = cloud.getWXContext()
  if (!OPENID) return { code: -1, msg: '未获取到用户身份' }

  const { action } = event
  try {
    switch (action) {

      case 'submitReport': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { taskId, goodQty, toolingCode } = event
        const addQty = Number(goodQty) || 0
        if (addQty <= 0) return { code: -1, msg: '报工数量无效' }

        const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
        if (tasks.length === 0) return { code: -1, msg: '任务不存在' }
        const t = tasks[0]

        if (t.frozen) {
          return { code: -1, msg: `工单已冻结：${t.freezeReason || '不合格品隔离'}，请联系主管审批解锁` }
        }

        if (t.routingSteps && Array.isArray(t.routingSteps) && t.currentStepIndex > 0) {
          const { data: prevTasks } = await db.collection('tasks')
            .where({ workOrderNo: t.workOrderNo, stepIndex: t.currentStepIndex - 1 })
            .get()
          const allDone = prevTasks.length > 0 && prevTasks.every(pt => pt.status === 'COMPLETED')
          if (!allDone) {
            const stepName = t.routingSteps[t.currentStepIndex - 1] || '上道工序'
            return { code: -1, msg: `上道工序「${stepName}」尚未完工，请等待前序完成` }
          }
        }

        if (t.toolingCode && toolingCode && t.toolingCode !== toolingCode) {
          return { code: -1, msg: `工装不匹配，当前工序要求：${t.toolingCode}` }
        }

        if (t.deviceCode) {
          const { data: devices } = await db.collection('devices').where({ deviceCode: t.deviceCode }).get()
          if (devices.length > 0) {
            const d = devices[0]
            if (d.status === 'LOCKED' || d.status === 'OVERDUE_MAINT') {
              return { code: -1, msg: `设备 ${d.deviceCode} 已锁定：${d.lockReason || '超期未点检'}，请联系维修员处理` }
            }
            if (d.status === 'FAULT' || d.status === 'REPAIRING') {
              return { code: -1, msg: `设备 ${d.deviceCode} 故障/维修中，无法报工` }
            }
          }
        }

        const newQty = (t.completedQty || 0) + addQty
        if (newQty > t.planQty) {
          return { code: -1, msg: `超出计划余量 ${t.planQty - (t.completedQty || 0)}` }
        }

        let status = t.status, statusLabel = t.statusLabel
        if (newQty >= t.planQty) {
          status = 'COMPLETED'; statusLabel = '已完成'
        } else if (newQty > 0 && t.status === 'PENDING') {
          status = 'PROCESSING'; statusLabel = '进行中'
        }

        await db.collection('tasks').where({ taskId }).update({
          data: {
            completedQty: newQty, status, statusLabel,
            version: _.inc(1),
            lastReportBy: user.realName, lastReportByOpenid: OPENID,
            lastReportAt: new Date(),
            updateTime: new Date()
          }
        })

        if (t.workOrderNo) {
          const { data: schedTasks } = await db.collection('tasks')
            .where({ workOrderNo: t.workOrderNo, taskType: 'PRODUCTION' })
            .get()
          const totalDone = schedTasks.reduce((s, st) => s + (st.completedQty || 0), 0)
          const totalPlan = schedTasks.reduce((s, st) => s + (st.planQty || 0), 0)
          let schedStatus = 'PENDING', schedLabel = '待执行'
          if (totalDone >= totalPlan) { schedStatus = 'COMPLETED'; schedLabel = '已完成' }
          else if (totalDone > 0) { schedStatus = 'IN_PROGRESS'; schedLabel = '进行中' }
          await db.collection('schedules').where({ scheduleNo: t.workOrderNo }).update({
            data: { completedQty: totalDone, status: schedStatus, statusLabel: schedLabel, updateTime: new Date() }
          })
        }

        if (status === 'COMPLETED' && t.routingSteps && Array.isArray(t.routingSteps) && t.currentStepIndex < t.routingSteps.length - 1) {
          const nextStepIndex = t.currentStepIndex + 1
          const { data: nextTasks } = await db.collection('tasks')
            .where({ workOrderNo: t.workOrderNo, stepIndex: nextStepIndex })
            .get()
          if (nextTasks.length > 0) {
            const nextStepName = t.routingSteps[nextStepIndex] || ('工序' + (nextStepIndex + 1))
            for (const nt of nextTasks) {
              if (nt.statusLabel === '等待上道完工') {
                await db.collection('tasks').where({ taskId: nt.taskId }).update({
                  data: {
                    statusLabel: '待处理',
                    updateTime: new Date()
                  }
                })
              }
            }
            console.log(`[submitReport] 工序 ${t.currentStepIndex + 1} 完工 → 激活工序 ${nextStepIndex + 1}「${nextStepName}」`)
          }
        }

        return { code: 0, data: { taskId, completedQty: newQty }, msg: '报工成功' }
      }

      case 'listTasks': {
        const { status, taskType, assignedTo, frozen } = event || {}
        const where = {}
        if (status) where.status = status
        if (taskType) where.taskType = taskType
        if (assignedTo) where.assignedTo = assignedTo
        if (frozen !== undefined) where.frozen = frozen

        const { data: list } = await db.collection('tasks')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'taskDetail': {
        const { taskId } = event
        const { data } = await db.collection('tasks').where({ taskId }).limit(1).get()
        if (data.length === 0) return { code: -1, msg: '任务不存在' }
        return { code: 0, data: data[0] }
      }

      case 'bindMaterial': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { taskId, materialCode, materialName, batchNo, qty } = event
        if (!taskId) return { code: -1, msg: '缺少任务ID' }

        await db.collection('tasks').where({ taskId }).update({
          data: {
            materialCode: materialCode || '',
            materialName: materialName || '',
            materialBatchNo: batchNo || '',
            materialQty: Number(qty) || 0,
            materialBoundBy: user.realName,
            materialBoundAt: new Date(),
            updateTime: new Date()
          }
        })

        return { code: 0, data: { taskId }, msg: '物料绑定成功' }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-production]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
