

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

      case 'freezeWorkOrder': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        if (user.roleCode !== 'QUALITY_INSPECTOR') {
          return { code: -1, msg: '仅质检员可执行冻结操作' }
        }

        const { taskId, reason } = event
        if (!taskId) return { code: -1, msg: '缺少任务ID' }
        if (!reason) return { code: -1, msg: '缺少冻结原因' }

        const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
        if (tasks.length === 0) return { code: -1, msg: '任务不存在' }
        const t = tasks[0]

        if (t.frozen) return { code: -1, msg: '该工单已被冻结' }

        await db.runTransaction(async (tx) => {

          await tx.collection('tasks').where({ taskId }).update({
            data: {
              frozen: true, freezeReason: reason,
              freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
              _prevStatus: t.status,
              status: 'HOLD', statusLabel: '已冻结',
              updateTime: new Date()
            }
          })

          if (t.workOrderNo) {
            await tx.collection('tasks').where({
              workOrderNo: t.workOrderNo,
              taskId: _.neq(taskId),
              status: _.neq('COMPLETED')
            }).update({
              data: {
                frozen: true,
                freezeReason: `级联冻结：工单 ${t.workOrderNo} 存在不合格品`,
                freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
                _prevStatus: _.literal(db.command.aggregate.raw({ $ifNull: ['$_prevStatus', '$status'] })),
                status: 'HOLD', statusLabel: '已冻结',
                updateTime: new Date()
              }
            })
          }
        })

        return { code: 0, msg: '工单已冻结，同工单下所有未完成任务已被锁定' }
      }

      case 'unfreezeWorkOrder': {
        const user = await resolveUserRole(OPENID)

        if (!user || !['PRODUCTION_SUPERVISOR', 'MANAGER'].includes(user.roleCode)) {
          return { code: -1, msg: '无解冻权限，仅生产主管/管理层可执行此操作' }
        }

        const { taskId } = event
        if (!taskId) return { code: -1, msg: '缺少任务ID' }

        const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
        if (tasks.length === 0) return { code: -1, msg: '任务不存在' }
        const t = tasks[0]

        if (!t.frozen) return { code: -1, msg: '该工单未被冻结' }

        await db.runTransaction(async (tx) => {

          await tx.collection('tasks').where({ taskId }).update({
            data: {
              frozen: false, freezeReason: '',
              unfrozenBy: user.realName, unfrozenByOpenid: OPENID,
              unfrozenAt: new Date(),
              status: t._prevStatus || 'PENDING',
              statusLabel: t._prevStatus === 'PROCESSING' ? '进行中' : '待处理',
              updateTime: new Date()
            }
          })

          if (t.workOrderNo) {
            await tx.collection('tasks').where({
              workOrderNo: t.workOrderNo,
              taskId: _.neq(taskId),
              frozen: true
            }).update({
              data: {
                frozen: false, freezeReason: '',
                unfrozenBy: user.realName, unfrozenByOpenid: OPENID,
                unfrozenAt: new Date(),
                updateTime: new Date()
              }
            })
          }
        })

        return { code: 0, msg: '工单已解冻，关联任务已恢复' }
      }

      case 'submitDefectRecord': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const {
          recordType, taskId, productSn, productName, workOrderNo,
          defectType, defectTypeName, defectQty, description, action, actionLabel, images
        } = event

        const recordId = 'QC-' + Date.now()
        const recordData = {
          recordId, recordType: recordType || 'DEFECT',
          taskId: taskId || '', productSn: productSn || '', productName: productName || '',
          workOrderNo: workOrderNo || '',
          defectType: defectType || '', defectTypeName: defectTypeName || '',
          defectQty: Number(defectQty) || 0, description: description || '',
          action: action || '', actionLabel: actionLabel || '',
          images: images || [],
          createdBy: user.realName, createdByOpenid: OPENID,
          createTime: new Date(), updateTime: new Date()
        }

        await db.collection('quality_records').add({ data: recordData })

        let frozen = false
        if (action === 'SCRAP' || action === 'REWORK') {
          if (taskId) {
            try {

              const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
              if (tasks.length > 0 && !tasks[0].frozen) {
                await db.runTransaction(async (tx) => {
                  const t = tasks[0]
                  const freezeReason = `${actionLabel}：${description || ''}`
                  await tx.collection('tasks').where({ taskId }).update({
                    data: {
                      frozen: true, freezeReason,
                      freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
                      _prevStatus: t.status,
                      status: 'HOLD', statusLabel: '已冻结',
                      updateTime: new Date()
                    }
                  })
                  if (t.workOrderNo) {
                    await tx.collection('tasks').where({
                      workOrderNo: t.workOrderNo,
                      taskId: _.neq(taskId),
                      status: _.neq('COMPLETED')
                    }).update({
                      data: {
                        frozen: true,
                        freezeReason: `级联冻结：工单 ${t.workOrderNo}`,
                        freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
                        status: 'HOLD', statusLabel: '已冻结',
                        updateTime: new Date()
                      }
                    })
                  }
                })
                frozen = true
              }
            } catch (freezeErr) {
              console.error('[mes-quality] 自动冻结失败:', freezeErr)
            }
          }
        }

        return { code: 0, data: { recordId, frozen }, msg: frozen ? '缺陷已登记，工单已自动冻结' : '缺陷登记成功' }
      }

      case 'submitInspection': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const {
          inspectionTaskId, workOrderNo, productSn, inspectionType,
          result, sampleQty, goodQty, defectQty, defects, action, remark,
          needAndon, andonDesc, andonAction
        } = event

        if (!inspectionTaskId && !workOrderNo) return { code: -1, msg: '缺少任务信息' }
        if (!productSn) return { code: -1, msg: '缺少产品SN' }

        const recordId = 'QC-' + Date.now()
        const actionLabel = action === 'REWORK' ? '返工' : action === 'SCRAP' ? '报废' : '放行'

        await db.collection('quality_records').add({ data: {
          recordId, recordType: 'INSPECTION',
          taskId: inspectionTaskId || '', workOrderNo: workOrderNo || '',
          productSn: productSn || '', inspectionType: inspectionType || '',
          result: result || 'QUALIFIED', sampleQty: Number(sampleQty) || 0,
          goodQty: Number(goodQty) || 0, defectQty: Number(defectQty) || 0,
          defects: defects || [], action, actionLabel,
          remark: remark || '', needAndon: !!needAndon,
          andonDesc: andonDesc || '', andonAction: andonAction || '',
          createdBy: user.realName, createdByOpenid: OPENID,
          createTime: new Date(), updateTime: new Date()
        }})

        const taskId = inspectionTaskId
        if (taskId) {
          const taskStatus = result === 'QUALIFIED' ? 'COMPLETED' : 'ABNORMAL'
          const taskStatusLabel = result === 'QUALIFIED' ? '已完成' : '异常'
          await db.collection('tasks').where({ taskId }).update({
            data: {
              status: taskStatus, statusLabel: taskStatusLabel,
              completedQty: Number(sampleQty) || 0,
              lastReportBy: user.realName, updateTime: new Date()
            }
          })
        }

        let frozen = false
        if ((action === 'SCRAP' || action === 'REWORK') && taskId) {
          try {
            const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
            if (tasks.length > 0 && !tasks[0].frozen) {
              const t = tasks[0]
              const freezeReason = `${actionLabel}：${remark || '检验不合格'}`
              await db.runTransaction(async (tx) => {
                await tx.collection('tasks').where({ taskId }).update({
                  data: {
                    frozen: true, freezeReason,
                    freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
                    _prevStatus: t.status,
                    status: 'HOLD', statusLabel: '已冻结',
                    updateTime: new Date()
                  }
                })
                if (t.workOrderNo) {
                  await tx.collection('tasks').where({
                    workOrderNo: t.workOrderNo, taskId: _.neq(taskId),
                    status: _.neq('COMPLETED')
                  }).update({
                    data: {
                      frozen: true,
                      freezeReason: `级联冻结：工单 ${t.workOrderNo}`,
                      freezeTime: new Date(), frozenBy: user.realName, frozenByOpenid: OPENID,
                      status: 'HOLD', statusLabel: '已冻结',
                      updateTime: new Date()
                    }
                  })
                }
              })
              frozen = true
            }
          } catch (freezeErr) {
            console.error('[mes-quality] 自动冻结失败:', freezeErr)
          }
        }

        return { code: 0, data: { recordId, frozen }, msg: frozen ? '检验提交成功，工单已冻结' : '质检提交成功' }
      }

      case 'listRecords': {
        const { recordType, taskId } = event || {}
        const where = {}
        if (recordType) where.recordType = recordType
        if (taskId) where.taskId = taskId

        const { data: list } = await db.collection('quality_records')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'recordDetail': {
        const { recordId } = event
        const { data } = await db.collection('quality_records').where({ recordId }).limit(1).get()
        if (data.length === 0) return { code: -1, msg: '记录不存在' }
        return { code: 0, data: data[0] }
      }

      case 'taskDetail': {
        const { taskId } = event
        if (!taskId) return { code: -1, msg: '缺少任务ID' }
        const { data } = await db.collection('tasks').where({ taskId }).limit(1).get()
        if (data.length === 0) return { code: -1, msg: '任务不存在' }
        return { code: 0, data: data[0] }
      }

      case 'listTasks': {
        const { status, frozen } = event || {}
        const where = { taskType: 'QUALITY' }
        if (status) where.status = status
        if (frozen !== undefined) where.frozen = frozen

        const { data: list } = await db.collection('tasks')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-quality]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
