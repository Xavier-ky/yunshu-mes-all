

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

  if (event.triggerName === 'tpmCheck') {
    try {
      const { data: allDevices } = await db.collection('devices')
        .where({ status: _.in(['RUNNING', 'STOPPED']) })
        .get()

      const now = Date.now()
      const overdueDevices = []

      for (const d of allDevices) {
        const inspectRecords = d.inspectRecords || []
        const lastInspect = inspectRecords[inspectRecords.length - 1]
        if (lastInspect && lastInspect.inspectTime) {
          const daysSince = (now - new Date(lastInspect.inspectTime).getTime()) / 86400000
          if (daysSince > 7) {
            const daysFloor = Math.floor(daysSince)
            overdueDevices.push({
              deviceCode: d.deviceCode,
              deviceName: d.deviceName,
              lineName: d.lineName || '',
              daysSince: daysFloor,
              lockReason: `距上次点检已过 ${daysFloor} 天（超过 7 天上限）`,
              lockedAt: new Date()
            })

            await db.collection('devices').where({ deviceCode: d.deviceCode }).update({
              data: {
                status: 'OVERDUE_MAINT', statusLabel: '超期未点检',
                lockReason: `距上次点检已过 ${daysFloor} 天`,
                updateTime: new Date()
              }
            })
          }
        }
      }

      if (overdueDevices.length > 0) {
        await db.collection('tpm_alerts').add({
          data: {
            type: 'HOURLY_SCAN',
            devices: overdueDevices,
            lockedCount: overdueDevices.length,
            generatedAt: new Date()
          }
        })
      }

      return { code: 0, data: { lockedCount: overdueDevices.length }, msg: 'TPM扫描完成' }
    } catch (err) {
      console.error('[mes-equipment] TPM定时扫描失败:', err)
      return { code: -1, msg: err.message }
    }
  }

  const { OPENID } = cloud.getWXContext()
  if (!OPENID) return { code: -1, msg: '未获取到用户身份' }

  const { action } = event
  try {
    switch (action) {

      case 'submitRepair': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { deviceCode, deviceName, faultType, faultDescription } = event
        if (!deviceCode) return { code: -1, msg: '缺少设备编号' }

        await db.collection('devices').where({ deviceCode }).update({
          data: {
            status: 'REPAIRING', statusLabel: '维修中',
            faultCode: faultType || '', faultDescription: faultDescription || '',
            updateTime: new Date()
          }
        })

        const taskId = 'TASK-' + Date.now()
        await db.collection('tasks').add({ data: {
          taskId, taskNo: taskId, taskType: 'REPAIR', taskTypeName: '维修任务',
          productName: deviceName || (deviceCode + ' 维修'),
          productModel: deviceCode,
          processName: '设备维修', workstationName: '',
          planQty: 0, completedQty: 0, qty: 0,
          status: 'PENDING', statusLabel: '待处理',
          priority: 'HIGH', priorityLabel: '高',
          deadline: '', assignedTo: '赵六',
          description: faultDescription || '',
          frozen: false, version: 0,
          reportedBy: user.realName, reportedByOpenid: OPENID,
          createTime: new Date(), updateTime: new Date()
        }})

        return { code: 0, data: { taskId, deviceCode }, msg: '报修成功，已通知维修员' }
      }

      case 'completeRepair': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { deviceCode, repairResult } = event
        if (!deviceCode) return { code: -1, msg: '缺少设备编号' }

        await db.collection('devices').where({ deviceCode }).update({
          data: {
            status: 'RUNNING', statusLabel: '运行',
            faultCode: '', faultDescription: '',
            updateTime: new Date()
          }
        })

        await db.collection('tasks').where({
          taskType: 'REPAIR',
          productModel: deviceCode,
          status: _.neq('COMPLETED')
        }).update({
          data: {
            status: 'COMPLETED', statusLabel: '已完成',
            repairResult: repairResult || '维修完成',
            completedBy: user.realName, completedByOpenid: OPENID,
            completedAt: new Date(),
            updateTime: new Date()
          }
        })

        return { code: 0, data: { deviceCode }, msg: '维修完成，设备已恢复运行' }
      }

      case 'completeInspection': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { deviceCode, result } = event
        if (!deviceCode) return { code: -1, msg: '缺少设备编号' }

        const { data: devices } = await db.collection('devices').where({ deviceCode }).get()
        if (devices.length === 0) return { code: -1, msg: '设备不存在' }

        const device = devices[0]
        const inspectId = 'INS-' + Date.now()
        const inspectRecord = {
          inspectId,
          inspectTime: new Date(),
          inspectBy: user.realName,
          inspectByOpenid: OPENID,
          result: result || 'NORMAL',
          resultLabel: result === 'NORMAL' ? '正常' : '异常'
        }

        let updateData = {
          inspectRecords: _.push([inspectRecord]),
          updateTime: new Date()
        }

        if (device.status === 'OVERDUE_MAINT' || device.status === 'LOCKED') {
          updateData.status = 'RUNNING'
          updateData.statusLabel = '运行'
          updateData.lockReason = ''
        }

        await db.collection('devices').where({ deviceCode }).update({ data: updateData })

        return { code: 0, data: { deviceCode, inspectId }, msg: '点检完成' }
      }

      case 'getTpmAlerts': {
        const { data } = await db.collection('tpm_alerts')
          .orderBy('generatedAt', 'desc')
          .limit(1)
          .get()

        if (data.length === 0) return { code: 0, data: { devices: [], lockedCount: 0, generatedAt: null } }
        return { code: 0, data: data[0] }
      }

      case 'list': {
        const { status } = event || {}
        const where = {}
        if (status) where.status = status

        const { data: list } = await db.collection('devices')
          .where(where)
          .orderBy('deviceCode', 'asc')
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'detail': {
        const { deviceCode } = event
        const { data } = await db.collection('devices')
          .where({ deviceCode: _.or(_.eq(deviceCode), _.eq(event.deviceId || '')) })
          .limit(1)
          .get()
        if (data.length === 0) return { code: -1, msg: '设备不存在' }
        return { code: 0, data: data[0] }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-equipment]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
