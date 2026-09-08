

const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

const USERS = {
  '001': { userId: 'U001', password: '123456', realName: '张三', roleCode: 'LINE_OPERATOR',         roleName: '产线操作工',   lineName: '风扇总装一线' },
  '002': { userId: 'U002', password: '123456', realName: '李四', roleCode: 'QUALITY_INSPECTOR',    roleName: '质检员',       lineName: '风扇总装一线' },
  '003': { userId: 'U003', password: '123456', realName: '王五', roleCode: 'WAREHOUSE_KEEPER',     roleName: '仓库物料员',   lineName: '原材料仓' },
  '004': { userId: 'U004', password: '123456', realName: '赵六', roleCode: 'EQUIPMENT_MAINTAINER', roleName: '设备维修员',   lineName: '风扇总装一线' },
  '005': { userId: 'U007', password: '123456', realName: '王主管', roleCode: 'PRODUCTION_SUPERVISOR',roleName: '生产主管',     lineName: '全部产线' },
  '006': { userId: 'U008', password: '123456', realName: '陈总', roleCode: 'MANAGER',                roleName: '管理层',       lineName: '全部产线' }
}

exports.main = async (event) => {
  const { OPENID } = cloud.getWXContext()
  if (!OPENID) return { code: -1, msg: '无法获取用户身份，请在微信环境中打开' }

  const { username, password } = event

  if (!username || !password) {
    return { code: -1, msg: '请输入账号和密码' }
  }

  const user = USERS[username.trim()]
  if (!user) {
    return { code: -1, msg: '账号不存在' }
  }
  if (user.password !== password) {
    return { code: -1, msg: '密码错误' }
  }

  try {
    const { data: existing } = await db.collection('users').where({ openid: OPENID }).limit(1).get()
    const record = {
      openid: OPENID,
      userId: user.userId,
      realName: user.realName,
      roleCode: user.roleCode,
      roleName: user.roleName,
      lineName: user.lineName,
      lastLoginAt: new Date()
    }

    if (existing.length > 0) {
      await db.collection('users').doc(existing[0]._id).update({ data: record })
    } else {
      await db.collection('users').add({ data: { ...record, createdAt: new Date() } })
    }
    console.log(`[mes-auth] 用户 ${user.userId} (${user.realName}) 登录成功, OPENID=${OPENID}`)
  } catch (err) {
    console.warn('[mes-auth] 更新 users 表失败:', err.message)

  }

  return {
    code: 0,
    msg: '登录成功',
    data: {
      token: `token-${user.userId}-${Date.now()}`,
      userId: user.userId,
      realName: user.realName,
      roleCode: user.roleCode,
      roleName: user.roleName,
      departmentName: '',
      lineName: user.lineName,
      shiftName: '白班'
    }
  }
}
