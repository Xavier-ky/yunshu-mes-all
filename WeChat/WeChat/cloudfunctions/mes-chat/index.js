

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

      case 'createGroup': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { name, bizType, bizRefId, bizRefNo, memberIds, bizContext } = event

        if (bizType && bizType !== 'ANDON') {
          return { code: -1, msg: '仅安灯事件可自动创建群聊，其他业务请通过安灯发起' }
        }

        const groupId = 'GRP-' + Date.now()

        const members = [
          { userId: user.userId, userName: user.realName, roleCode: user.roleCode, openid: OPENID }
        ]

        if (memberIds && Array.isArray(memberIds)) {
          for (const uid of memberIds) {
            if (uid === user.userId) continue

            const { data: foundUsers } = await db.collection('users').where({ userId: uid }).limit(1).get()
            if (foundUsers.length > 0) {
              const fu = foundUsers[0]
              members.push({ userId: fu.userId, userName: fu.realName, roleCode: fu.roleCode, openid: fu.openid })
            } else {
              members.push({ userId: uid, userName: uid, roleCode: '', openid: '' })
            }
          }
        }

        await db.collection('chat_groups').add({
          data: {
            groupId,
            name: name || '群聊',
            bizType: bizType || 'GENERAL',
            bizRefId: bizRefId || '', bizRefNo: bizRefNo || groupId,
            bizContext: bizContext || {},
            creatorId: user.userId, creatorName: user.realName,
            members,
            status: 'ACTIVE',
            createTime: new Date()
          }
        })

        return { code: 0, data: { groupId, members }, msg: '群聊已创建' }
      }

      case 'addMember': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { groupId, targetUserId } = event
        if (!groupId || !targetUserId) return { code: -1, msg: '缺少参数' }

        const { data: groups } = await db.collection('chat_groups').where({ groupId, status: 'ACTIVE' }).get()
        if (groups.length === 0) return { code: -1, msg: '群聊不存在或已解散' }

        const group = groups[0]
        const alreadyIn = group.members.some(m => m.userId === targetUserId)
        if (alreadyIn) return { code: -1, msg: '该成员已在群中' }

        const { data: foundUsers } = await db.collection('users').where({ userId: targetUserId }).limit(1).get()
        const newMember = foundUsers.length > 0
          ? { userId: foundUsers[0].userId, userName: foundUsers[0].realName, roleCode: foundUsers[0].roleCode, openid: foundUsers[0].openid }
          : { userId: targetUserId, userName: targetUserId, roleCode: '', openid: '' }

        await db.collection('chat_groups').where({ groupId }).update({
          data: { members: _.push([newMember]), updateTime: new Date() }
        })

        return { code: 0, msg: '成员已添加' }
      }

      case 'removeMember': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { groupId, targetUserId } = event

        const { data: groups } = await db.collection('chat_groups').where({ groupId, status: 'ACTIVE' }).get()
        if (groups.length === 0) return { code: -1, msg: '群聊不存在或已解散' }

        await db.collection('chat_groups').where({ groupId }).update({
          data: {
            members: _.pull({
              userId: targetUserId
            }),
            updateTime: new Date()
          }
        })

        return { code: 0, msg: '成员已移除' }
      }

      case 'disbandGroup': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { groupId, reason } = event

        const { data: groups } = await db.collection('chat_groups').where({ groupId, status: 'ACTIVE' }).get()
        if (groups.length === 0) return { code: -1, msg: '群聊不存在或已解散' }

        if (groups[0].creatorId !== user.userId) {
          return { code: -1, msg: '仅群创建者可解散群聊' }
        }

        await db.collection('chat_groups').where({ groupId }).update({
          data: {
            status: 'DISBANDED',
            disbandTime: new Date(),
            disbandReason: reason || '群主解散',
            disbandedBy: user.realName
          }
        })

        return { code: 0, msg: '群聊已解散' }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-chat]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
