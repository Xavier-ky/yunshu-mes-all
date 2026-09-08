

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

      case 'create': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }
        if (!['PRODUCTION_SUPERVISOR', 'MANAGER'].includes(user.roleCode)) {
          return { code: -1, msg: '无排产权限' }
        }

        const { lineName, productName, planQty, operatorName, operatorId,
                startDate, endDate, routingSteps, toolingCode, standardHours, description, priority } = event

        const scheduleNo = 'SCH-' + Date.now()
        const steps = (routingSteps || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)

        const result = await db.runTransaction(async (tx) => {
          const taskIds = []

          if (steps.length === 0) {
            const taskId = 'TASK-' + Date.now()
            taskIds.push(taskId)
            await tx.collection('tasks').add({ data: {
              taskId, taskNo: taskId, taskType: 'PRODUCTION', taskTypeName: '生产任务',
              workOrderNo: scheduleNo, productName, productModel: '',
              processName: '生产加工', workstationName: lineName,
              planQty: Number(planQty), completedQty: 0, qty: Number(planQty),
              status: 'PENDING', statusLabel: '待处理',
              priority: priority || 'MEDIUM', priorityLabel: priority === 'HIGH' ? '高' : priority === 'LOW' ? '低' : '中',
              deadline: `${endDate || ''} 18:00:00`, assignedTo: operatorName,
              toolingCode: toolingCode || '', routingSteps: [], stepIndex: 0, currentStepIndex: 0,
              standardHoursPerUnit: Number(standardHours) || 0,
              frozen: false, version: 0, description: description || '',
              createdBy: user.realName, createdByOpenid: OPENID,
              createTime: new Date(), updateTime: new Date()
            }})
          } else {
            for (let i = 0; i < steps.length; i++) {
              const taskId = 'TASK-' + Date.now() + '-' + i
              taskIds.push(taskId)
              await tx.collection('tasks').add({ data: {
                taskId, taskNo: taskId, taskType: 'PRODUCTION', taskTypeName: '生产任务',
                workOrderNo: scheduleNo, productName, productModel: '',
                processName: steps[i], workstationName: lineName,
                planQty: Number(planQty), completedQty: 0, qty: Number(planQty),
                status: i === 0 ? 'PENDING' : 'PENDING',
                statusLabel: i === 0 ? '待处理' : '等待上道完工',
                priority: priority || 'MEDIUM', priorityLabel: priority === 'HIGH' ? '高' : priority === 'LOW' ? '低' : '中',
                deadline: `${endDate || ''} 18:00:00`, assignedTo: operatorName,
                toolingCode: toolingCode || '',
                routingSteps: steps, stepIndex: i, currentStepIndex: i,
                standardHoursPerUnit: Number(standardHours) || 0,
                frozen: false, version: 0, description: description || '',
                createdBy: user.realName, createdByOpenid: OPENID,
                createTime: new Date(), updateTime: new Date()
              }})
            }
          }

          await tx.collection('schedules').add({ data: {
            id: scheduleNo, scheduleNo, taskIds,
            lineName, productName, planQty: Number(planQty), completedQty: 0,
            operatorName, operatorId: operatorId || '',
            startDate: startDate || '', endDate: endDate || '',
            routingSteps: routingSteps || '', toolingCode: toolingCode || '',
            standardHours: Number(standardHours) || 0, description: description || '',
            priority: priority || 'MEDIUM', priorityLabel: priority === 'HIGH' ? '高' : priority === 'LOW' ? '低' : '中',
            status: 'PENDING', statusLabel: '待执行',
            createdBy: user.realName, createdByOpenid: OPENID,
            createdAt: new Date().toISOString(), createTime: new Date(), updateTime: new Date()
          }})

          return { scheduleNo, taskIds }
        })

        return { code: 0, data: result, msg: '排产布置成功' }
      }

      case 'delete': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }
        if (!['PRODUCTION_SUPERVISOR', 'MANAGER'].includes(user.roleCode)) {
          return { code: -1, msg: '无删除排产权限' }
        }

        const { scheduleNo } = event
        if (!scheduleNo) return { code: -1, msg: '缺少排产编号' }

        await db.runTransaction(async (tx) => {
          const { data: schedules } = await tx.collection('schedules').where({ scheduleNo }).get()
          if (schedules.length === 0) throw new Error('排产不存在')
          const sched = schedules[0]
          if (sched.taskIds && sched.taskIds.length > 0) {
            for (const taskId of sched.taskIds) {
              await tx.collection('tasks').where({ taskId }).remove()
            }
          }
          await tx.collection('schedules').where({ scheduleNo }).remove()
        })

        return { code: 0, data: { scheduleNo }, msg: '排产已删除' }
      }

      case 'list': {
        const { status, lineName, keyword } = event || {}
        const where = {}
        if (status) where.status = status
        if (lineName) where.lineName = lineName

        let query = db.collection('schedules').where(where).orderBy('createTime', 'desc').limit(50)
        const { data: list } = await query.get()

        let filtered = list
        if (keyword) {
          const kw = keyword.toLowerCase()
          filtered = list.filter(s =>
            (s.productName || '').toLowerCase().includes(kw) ||
            (s.operatorName || '').toLowerCase().includes(kw) ||
            (s.scheduleNo || '').toLowerCase().includes(kw)
          )
        }

        return { code: 0, data: { list: filtered, total: filtered.length } }
      }

      case 'detail': {
        const { scheduleNo } = event
        const { data } = await db.collection('schedules').where({ scheduleNo }).limit(1).get()
        if (data.length === 0) return { code: -1, msg: '排产不存在' }
        return { code: 0, data: data[0] }
      }

      case 'getLines': {
        const LINES = ['风扇总装一线', '风扇总装二线', '电机组装线', '网罩组装线', '喷涂线', '包装线']
        return { code: 0, data: { lines: LINES } }
      }

      case 'getOperators': {
        const { lineName } = event || {}
        const { data: users } = await db.collection('users')
          .where({ roleCode: 'LINE_OPERATOR' }).get()

        if (users.length === 0) {
          const OPERATORS = [
            { id: 'OP001', name: '张三', lineName: '风扇总装一线' },
            { id: 'OP002', name: '李四', lineName: '风扇总装一线' },
            { id: 'OP003', name: '王五', lineName: '原材料仓' },
            { id: 'OP004', name: '赵六', lineName: '风扇总装一线' },
            { id: 'OP005', name: '张建国', lineName: '风扇总装一线' },
            { id: 'OP006', name: '李明辉', lineName: '风扇总装二线' }
          ]
          let list = OPERATORS
          if (lineName) list = list.filter(o => o.lineName === lineName)
          return { code: 0, data: { list } }
        }
        const list = users.map(u => ({ id: u.userId, name: u.realName, lineName: u.lineName || '' }))
        return { code: 0, data: { list } }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-schedule]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
