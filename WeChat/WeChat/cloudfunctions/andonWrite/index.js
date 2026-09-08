const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

async function findTargetUser(roleCode) {
  const { data } = await db.collection('users')
    .where({ roleCode })
    .limit(1)
    .get()
  if (data.length === 0) return null
  return {
    userId: data[0].userId,
    userName: data[0].realName,
    roleCode: data[0].roleCode,
    openid: data[0].openid || ''
  }
}

exports.main = async (event) => {
  const { OPENID } = cloud.getWXContext()
  const { action } = event

  try {
    switch (action) {

      case 'create': {
        const { eventType, eventTypeName, reasonCode, workOrderNo, lineCode, lineName,
                deviceCode, deviceName, description, createdBy } = event

        const ROLE_MAP = {
          MATERIAL:  'WAREHOUSE_KEEPER',
          EQUIPMENT: 'EQUIPMENT_MAINTAINER',
          PROCESS:   'PRODUCTION_SUPERVISOR',
          QUALITY:   'QUALITY_INSPECTOR'
        }
        const roleNameMap = {
          MATERIAL: '仓库物料员', EQUIPMENT: '设备维修员', PROCESS: '生产主管', QUALITY: '质检员'
        }
        const targetRole = ROLE_MAP[eventType] || ROLE_MAP['EQUIPMENT']
        const targetUser = await findTargetUser(targetRole)
        if (!targetUser) {
          const hint = targetRole === 'EQUIPMENT_MAINTAINER' ? 'maintainer01' : targetRole === 'QUALITY_INSPECTOR' ? 'inspector01' : targetRole === 'WAREHOUSE_KEEPER' ? 'keeper01' : targetRole === 'PRODUCTION_SUPERVISOR' ? 'prodsup01' : ''
          return { code: -1, message: `未找到${roleNameMap[eventType] || ''}用户，请先用 ${hint} 账号登录一次后再发起安灯` }
        }
        const targetPerson = targetUser.userName
        const targetMemberId = targetUser.userId

        const eventId = 'ANDON-' + Date.now()
        const groupId = 'GRP-' + Date.now()
        const creator = createdBy || '系统'

        await db.collection('andon_events').add({ data: {
          eventId, eventNo: eventId, eventType, eventTypeName: eventTypeName || '',
          reasonCode, workOrderNo: workOrderNo || '', lineCode: lineCode || '', lineName: lineName || '',
          deviceCode: deviceCode || '', deviceName: deviceName || '',
          description: description || '', status: 'INITIATED', statusLabel: '已发起',
          priority: 'HIGH', createdBy: creator, targetPerson, targetRoleName: roleNameMap[eventType] || '',
          groupId, createTime: new Date().toISOString(), responseTime: null, resolveTime: null, handleRecords: []
        }})

        const taskId = 'TASK-' + Date.now()
        await db.collection('tasks').add({ data: {
          taskId, taskNo: taskId, taskType: 'ANDON', taskTypeName: '安灯处理',
          productName: description || eventTypeName || '安灯事件',
          processName: eventType === 'EQUIPMENT' ? '设备维修' : eventType === 'MATERIAL' ? '物料补给' : eventType === 'PROCESS' ? '工艺指导' : '品质确认',
          workstationName: lineName || '', planQty: 0, completedQty: 0, qty: 0,
          status: 'PENDING', statusLabel: '待处理', priority: 'HIGH', priorityLabel: '高',
          assignedTo: targetPerson, workOrderNo: eventId, frozen: false, version: 0,
          description: description || '', createTime: new Date().toISOString(), updateTime: new Date().toISOString()
        }})

        if (eventType === 'EQUIPMENT' && deviceCode) {
          await db.collection('devices').where({ deviceCode }).update({ data: {
            status: 'FAULT', statusLabel: '故障', faultDescription: description || ''
          }})
        }

        await db.collection('chat_groups').add({ data: {
          groupId, name: `${eventTypeName || '安灯'} · ${lineName || deviceName || ''}`,
          bizType: 'ANDON', bizRefId: eventId, bizRefNo: eventId,
          bizContext: { title: eventTypeName || '', detail: description || '', statusLabel: '已发起', status: 'INITIATED' },
          creatorId: '1', creatorName: creator,
          members: [
            { userId: '1', userName: creator, roleCode: 'LINE_OPERATOR', roleName: '产线操作工' },
            ...(targetMemberId ? [{ userId: targetMemberId, userName: targetPerson, roleCode: targetRole, roleName: roleNameMap[eventType] || '' }] : [])
          ],
          status: 'ACTIVE', createTime: new Date().toISOString()
        }})

        return { code: 0, data: { eventId, groupId }, message: `已通知${roleNameMap[eventType] || ''}（${targetPerson}）` }
      }

      case 'resolve': {
        const { eventId, handler, action: handleAction } = event
        const { data: evts } = await db.collection('andon_events').where({ eventId }).get()
        if (evts.length === 0) return { code: -1, message: '事件不存在' }
        const evt = evts[0]

        const newRecord = { handler: handler || evt.targetPerson || '', handlerName: handler || evt.targetPerson || '', action: handleAction || '处理完成', time: new Date().toISOString() }
        await db.collection('andon_events').where({ eventId }).update({ data: {
          status: 'RESOLVED', statusLabel: '已解决', resolveTime: new Date().toISOString(),
          handleRecords: cloud.database().command.push([newRecord])
        }})

        if (evt.eventType === 'EQUIPMENT' && evt.deviceCode) {
          await db.collection('devices').where({ deviceCode: evt.deviceCode }).update({ data: {
            status: 'RUNNING', statusLabel: '运行', faultDescription: ''
          }})
        }

        await db.collection('tasks').where({ workOrderNo: eventId, taskType: 'ANDON', status: db.command.neq('COMPLETED') }).update({ data: {
          status: 'COMPLETED', statusLabel: '已完成'
        }})

        if (evt.groupId) {
          await db.collection('chat_groups').where({ groupId: evt.groupId }).update({ data: {
            status: 'DISBANDED', disbandTime: new Date().toISOString(), disbandReason: '安灯已解决，群聊自动解散'
          }})
        }

        return { code: 0, message: '处理成功，群聊已自动解散' }
      }

      default:
        return { code: -1, message: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[andonWrite]', action, err)
    return { code: -1, message: err.message || '操作失败' }
  }
}
