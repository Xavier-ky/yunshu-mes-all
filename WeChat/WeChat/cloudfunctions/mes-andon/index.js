

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

const ANDON_ROLE_MAP = {
  MATERIAL:  { role: 'WAREHOUSE_KEEPER',      roleName: '仓库物料员' },
  EQUIPMENT: { role: 'EQUIPMENT_MAINTAINER',  roleName: '设备维修员' },
  PROCESS:   { role: 'PRODUCTION_SUPERVISOR', roleName: '生产主管' },
  QUALITY:   { role: 'QUALITY_INSPECTOR',     roleName: '质检员' }
}

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

exports.main = async (event, context) => {
  const { OPENID } = cloud.getWXContext()
  if (!OPENID) return { code: -1, msg: '未获取到用户身份' }

  const { action } = event
  try {
    switch (action) {

      case 'create': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const {
          eventType, eventTypeName, reasonCode, reasonName,
          workOrderNo, lineCode, lineName, workstationCode, workstationName,
          deviceCode, deviceName, description
        } = event

        const targetRole = ANDON_ROLE_MAP[eventType] || ANDON_ROLE_MAP['EQUIPMENT']

        const targetUser = await findTargetUser(targetRole.role)
        if (!targetUser) {
          return { code: -1, msg: `未找到${targetRole.roleName}用户，请先用 ${targetRole.role === 'EQUIPMENT_MAINTAINER' ? 'maintainer01' : targetRole.role === 'QUALITY_INSPECTOR' ? 'inspector01' : targetRole.role === 'WAREHOUSE_KEEPER' ? 'keeper01' : targetRole.role === 'PRODUCTION_SUPERVISOR' ? 'prodsup01' : ''} 账号登录一次后再发起安灯` }
        }
        const targetPerson = targetUser.userName
        const targetMemberId = targetUser.userId
        const targetOpenid = targetUser.openid

        const eventId = 'ANDON-' + Date.now()
        const groupId = 'GRP-' + Date.now()

        const result = await db.runTransaction(async (tx) => {

          await tx.collection('andon_events').add({ data: {
            eventId, eventNo: eventId, groupId,
            eventType, eventTypeName: eventTypeName || '',
            reasonCode: reasonCode || '', reasonName: reasonName || '',
            workOrderNo: workOrderNo || '',
            lineCode: lineCode || '', lineName: lineName || '',
            workstationCode: workstationCode || '', workstationName: workstationName || '',
            deviceCode: deviceCode || '', deviceName: deviceName || '',
            description: description || '',
            status: 'INITIATED', statusLabel: '已发起', priority: 'HIGH',
            createdBy: user.realName, createdByName: user.realName, createdByOpenid: OPENID,
            targetRole: targetRole.role, targetRoleName: targetRole.roleName, targetPerson: targetPerson,
            createTime: new Date(), responseTime: null, resolveTime: null,
            handleRecords: []
          }})

          const processMap = {
            EQUIPMENT: '设备维修', MATERIAL: '物料补给', PROCESS: '工艺指导', QUALITY: '品质确认'
          }
          const taskId = 'TASK-' + Date.now()
          await tx.collection('tasks').add({ data: {
            taskId, taskNo: taskId, taskType: 'ANDON', taskTypeName: '安灯处理',
            productName: description || eventTypeName || '安灯事件',
            productModel: deviceCode || '',
            processName: processMap[eventType] || '异常处理',
            workstationName: lineName || workstationName || '',
            planQty: 0, completedQty: 0, qty: 0,
            status: 'PENDING', statusLabel: '待处理', priority: 'HIGH', priorityLabel: '高',
            assignedTo: targetPerson, workOrderNo: eventId,
            description: description || '', frozen: false, version: 0,
            createdBy: user.realName,
            createTime: new Date(), updateTime: new Date()
          }})

          if (eventType === 'EQUIPMENT' && deviceCode) {
            await tx.collection('devices').where({ deviceCode }).update({
              data: {
                status: 'FAULT', statusLabel: '故障',
                faultDescription: description || '',
                updateTime: new Date()
              }
            })
          }

          await tx.collection('chat_groups').add({ data: {
            groupId,
            name: `${eventTypeName || '安灯'} · ${lineName || deviceName || workstationName || '未知'}`,
            bizType: 'ANDON', bizRefId: eventId, bizRefNo: eventId,
            bizContext: {
              title: eventTypeName || '安灯事件',
              detail: description || '',
              statusLabel: '已发起', status: 'INITIATED'
            },
            creatorId: user.userId, creatorName: user.realName,
            members: [
              { userId: user.userId, userName: user.realName, roleCode: user.roleCode, openid: OPENID },
              ...(targetMemberId && targetMemberId !== user.userId
                ? [{ userId: targetMemberId, userName: targetPerson, roleCode: targetRole.role, openid: targetOpenid }]
                : [])
            ],
            status: 'ACTIVE',
            createTime: new Date()
          }})

          return { eventId, groupId, taskId, targetPerson, targetRoleName: targetRole.roleName }
        })

        return {
          code: 0,
          data: result,
          msg: `安灯已发起，已通知${targetRole.roleName}（${targetPerson}）`
        }
      }

      case 'resolve': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { eventId, handler, handleAction } = event

        const { data: evts } = await db.collection('andon_events').where({ eventId }).get()
        if (evts.length === 0) return { code: -1, msg: '安灯事件不存在' }
        const evt = evts[0]

        if (evt.status === 'RESOLVED') return { code: -1, msg: '该安灯已解决' }

        const handlerName = handler || user.realName
        const handlerAction = handleAction || '处理完成'

        await db.runTransaction(async (tx) => {

          const newRecord = {
            handler: handlerName, handlerName,
            action: handlerAction,
            handlerOpenid: OPENID,
            time: new Date()
          }
          await tx.collection('andon_events').where({ eventId }).update({
            data: {
              status: 'RESOLVED', statusLabel: '已解决',
              resolveTime: new Date(),
              resolvedBy: handlerName, resolvedByOpenid: OPENID,
              handleRecords: _.push([newRecord])
            }
          })

          if (evt.eventType === 'EQUIPMENT' && evt.deviceCode) {
            await tx.collection('devices').where({ deviceCode: evt.deviceCode }).update({
              data: {
                status: 'RUNNING', statusLabel: '运行',
                faultDescription: '', faultCode: '',
                updateTime: new Date()
              }
            })
          }

          await tx.collection('tasks').where({
            workOrderNo: eventId, taskType: 'ANDON', status: _.neq('COMPLETED')
          }).update({
            data: {
              status: 'COMPLETED', statusLabel: '已完成',
              completedBy: handlerName,
              updateTime: new Date()
            }
          })

          if (evt.groupId) {
            await tx.collection('chat_groups').where({ groupId: evt.groupId }).update({
              data: {
                status: 'DISBANDED',
                disbandTime: new Date(),
                disbandReason: '安灯已解决，群聊自动解散'
              }
            })
          }
        })

        return { code: 0, msg: '安灯已解决，群聊已自动解散' }
      }

      case 'accept': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { eventId } = event
        const { data: evts } = await db.collection('andon_events').where({ eventId }).get()
        if (evts.length === 0) return { code: -1, msg: '安灯事件不存在' }
        const evt = evts[0]

        if (evt.status !== 'INITIATED') return { code: -1, msg: '该安灯已被其他人接收' }

        await db.collection('andon_events').where({ eventId }).update({
          data: {
            status: 'PROCESSING', statusLabel: '处理中',
            responseTime: new Date(),
            acceptedBy: user.realName, acceptedByOpenid: OPENID,
            handleRecords: _.push([{
              handler: user.realName, handlerName: user.realName,
              action: '已接收任务，开始处理',
              handlerOpenid: OPENID,
              time: new Date()
            }])
          }
        })

        return { code: 0, msg: '已接收安灯任务' }
      }

      case 'list': {
        const { status, eventType } = event || {}
        const where = {}
        if (status) where.status = status
        if (eventType) where.eventType = eventType

        const { data: list } = await db.collection('andon_events')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'detail': {
        const { eventId } = event
        const { data } = await db.collection('andon_events').where({ eventId }).limit(1).get()
        if (data.length === 0) return { code: -1, msg: '安灯事件不存在' }
        return { code: 0, data: data[0] }
      }

      case 'sendMessage': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { groupId, content, msgType } = event
        if (!groupId) return { code: -1, msg: '缺少群ID' }
        if (!content || !content.trim()) return { code: -1, msg: '消息不能为空' }

        const { data: groups } = await db.collection('chat_groups')
          .where({ groupId, status: 'ACTIVE' }).get()
        if (groups.length === 0) return { code: -1, msg: '群聊不存在或已解散' }

        const group = groups[0]
        const isMember = group.members.some(m => m.openid === OPENID || m.userId === user.userId)
        if (!isMember) return { code: -1, msg: '您不在该群聊中，无法发送消息' }

        const msgId = 'CM-' + Date.now()
        const message = {
          msgId, groupId,
          type: msgType || 'TEXT',
          senderId: user.userId, senderName: user.realName, senderOpenid: OPENID,
          content: content.trim(),
          time: new Date()
        }

        await db.collection('chat_messages').add({ data: message })

        return { code: 0, data: { msgId, message }, msg: '已发送' }
      }

      case 'listMessages': {
        const { groupId } = event
        if (!groupId) return { code: -1, msg: '缺少群ID' }

        const { data: list } = await db.collection('chat_messages')
          .where({ groupId })
          .orderBy('time', 'asc')
          .limit(200)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'listGroups': {
        const { status, bizType } = event || {}
        const where = {}
        if (status) where.status = status
        if (bizType) where.bizType = bizType

        const { data: list } = await db.collection('chat_groups')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(50)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'groupDetail': {
        const { groupId } = event
        const { data: groups } = await db.collection('chat_groups').where({ groupId }).limit(1).get()
        if (groups.length === 0) return { code: -1, msg: '群聊不存在' }

        const { data: messages } = await db.collection('chat_messages')
          .where({ groupId })
          .orderBy('time', 'asc')
          .limit(200)
          .get()

        return { code: 0, data: { group: groups[0], messages } }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-andon]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
