

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

      case 'fifoQuery': {
        const { materialCode } = event
        if (!materialCode) return { code: -1, msg: '缺少物料编码' }

        const { data: batches } = await db.collection('inventory')
          .where({ materialCode, status: 'COMPLETED', registeredQty: _.gt(0) })
          .orderBy('createTime', 'asc')
          .get()

        if (batches.length === 0) {
          return { code: 0, data: { locked: false, batch: null, allBatches: [], msg: '该物料无可用库存' } }
        }

        return {
          code: 0,
          data: {
            locked: true,
            batch: batches[0],
            allBatches: batches,
            msg: `最早批次 ${batches[0].batchNo}`
          }
        }
      }

      case 'batchLookup': {
        const { batchNo } = event
        if (!batchNo) return { code: -1, msg: '缺少批次号' }

        const { data: items } = await db.collection('inventory')
          .where({ batchNo })
          .orderBy('createTime', 'desc')
          .limit(1)
          .get()

        if (items.length === 0) {
          return { code: -1, msg: `未找到批次 ${batchNo}` }
        }

        const item = items[0]
        return {
          code: 0,
          data: {
            materialCode: item.materialCode,
            materialName: item.materialName,
            spec: item.spec || '',
            batchNo: item.batchNo,
            stockQty: item.registeredQty || item.qty || 0,
            warehouseName: item.warehouseName || '',
            locationCode: item.locationCode || ''
          }
        }
      }

      case 'materialIssue': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { taskId, batchNo, issueQty, warehouseName, locationCode, workOrderNo, materialCode, materialName } = event
        const qty = Number(issueQty) || 0
        if (qty <= 0) return { code: -1, msg: '发料数量无效' }
        if (!batchNo) return { code: -1, msg: '请扫描物料批次' }
        if (!taskId) return { code: -1, msg: '缺少任务ID' }

        const result = await db.runTransaction(async (tx) => {

          const { data: invItems } = await tx.collection('inventory')
            .where({ batchNo, status: 'COMPLETED' }).get()
          if (invItems.length === 0) return { error: `批次 ${batchNo} 不存在` }
          const inv = invItems[0]
          const available = inv.registeredQty || inv.qty || 0
          if (available < qty) return { error: `库存不足，当前可用 ${available}，需求 ${qty}` }

          const newQty = available - qty
          await tx.collection('inventory').where({ batchNo }).update({
            data: {
              registeredQty: newQty,
              status: newQty <= 0 ? 'DEPLETED' : 'COMPLETED',
              updateTime: new Date()
            }
          })

          const { data: tasks } = await tx.collection('tasks').where({ taskId }).get()
          if (tasks.length === 0) return { error: '领料任务不存在' }
          const t = tasks[0]
          const currentIssued = t.issuedQty || 0
          const newIssued = currentIssued + qty
          const maxQty = t.qty || 0

          await tx.collection('tasks').where({ taskId }).update({
            data: {
              issuedQty: newIssued,
              status: newIssued >= maxQty ? 'COMPLETED' : 'PROCESSING',
              statusLabel: newIssued >= maxQty ? '已完成' : '进行中',
              issuedBy: user.realName, issuedByOpenid: OPENID,
              updateTime: new Date()
            }
          })

          const inboundId = 'MAT-INB-' + Date.now()
          await tx.collection('inventory').add({
            data: {
              taskId: inboundId, taskNo: inboundId, taskType: 'INBOUND',
              materialCode: materialCode || t.materialCode || '',
              materialName: materialName || t.materialName || '',
              batchNo,
              warehouseName: warehouseName || t.warehouseName || '',
              locationCode: locationCode || t.locationCode || '',
              qty, registeredQty: 0,
              unit: t.unit || '',
              status: 'PENDING', statusLabel: '待入库',
              createdBy: user.realName, createdByOpenid: OPENID,
              createTime: new Date(), updateTime: new Date()
            }
          })

          return { issueId: inboundId, actualQty: qty }
        })

        if (result.error) return { code: -1, msg: result.error }
        return { code: 0, data: result, msg: `发料成功，数量：${qty}，已生成入库登记` }
      }

      case 'materialReturn': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { taskId, materialCode, materialName, qty, reason, batchNo, spec, warehouseName, locationCode } = event
        const returnQty = Number(qty) || 0
        if (returnQty <= 0) return { code: -1, msg: '退料数量无效' }
        if (!reason || !reason.trim()) return { code: -1, msg: '请填写退料原因' }

        const returnId = 'MAT-RETURN-' + Date.now()
        await db.collection('inventory').add({
          data: {
            taskId: returnId, taskNo: returnId, taskType: 'RETURN',
            refTaskId: taskId || '',
            materialCode: materialCode || '', materialName: materialName || '',
            spec: spec || '', batchNo: batchNo || '',
            warehouseName: warehouseName || '', locationCode: locationCode || '',
            qty: returnQty, unit: '个',
            reason: reason.trim(),
            status: 'PENDING', statusLabel: '待处理',
            createdBy: user.realName, createdByOpenid: OPENID,
            createTime: new Date(), updateTime: new Date()
          }
        })

        return { code: 0, data: { returnId, qty: returnQty }, msg: '退料成功' }
      }

      case 'listTasks': {
        const { taskType, status } = event || {}
        const where = {}
        if (taskType) where.taskType = taskType
        if (status) where.status = status

        const { data: list } = await db.collection('inventory')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'taskDetail': {
        const { taskId } = event

        const { data: results } = await db.collection('inventory')
          .where({ taskId })
          .limit(1)
          .get()
        if (results.length > 0) return { code: 0, data: results[0] }

        const { data: tasks } = await db.collection('tasks')
          .where({ taskId, taskType: 'MATERIAL' })
          .limit(1)
          .get()
        if (tasks.length === 0) return { code: -1, msg: '库存任务不存在' }
        return { code: 0, data: tasks[0] }
      }

      case 'submitInbound': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { taskId, registeredQty } = event
        const qty = Number(registeredQty) || 0
        if (qty <= 0) return { code: -1, msg: '入库数量无效' }

        await db.collection('inventory').where({ taskId, taskType: 'INBOUND' }).update({
          data: {
            registeredQty: qty,
            status: 'COMPLETED', statusLabel: '已入库',
            registeredBy: user.realName,
            registeredAt: new Date(),
            updateTime: new Date()
          }
        })

        return { code: 0, msg: '入库成功' }
      }

      case 'listMaterialCalls': {
        const { status } = event || {}
        const where = {}
        if (status) where.status = status

        const { data: list } = await db.collection('material_calls')
          .where(where)
          .orderBy('createTime', 'desc')
          .limit(100)
          .get()

        return { code: 0, data: { list, total: list.length } }
      }

      case 'createMaterialCall': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const {
          workOrderNo, productName, processName, workstationName, lineName,
          materialName, materialCode, needQty, remainQty, urgency, remark
        } = event

        const callId = 'MC-' + Date.now()
        await db.collection('material_calls').add({
          data: {
            callId, callNo: callId,
            workOrderNo: workOrderNo || '',
            productName: productName || '',
            processName: processName || '',
            workstationName: workstationName || '',
            lineName: lineName || '',
            materialName: materialName || '', materialCode: materialCode || '',
            needQty: Number(needQty) || 0, remainQty: Number(remainQty) || 0,
            urgency: urgency || 'NORMAL',
            urgencyLabel: urgency === 'URGENT' ? '紧急' : '一般',
            remark: remark || '',
            status: 'PENDING', statusLabel: '待处理',
            operatorName: user.realName, operatorOpenid: OPENID,
            createTime: new Date(),
            actualQty: null, batchNo: null, estimatedArrival: null, completeTime: null,
            upgradedToAndon: false, andonId: null
          }
        })

        return { code: 0, data: { callId }, msg: '缺料呼叫已提交' }
      }

      case 'processMaterialCall': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { callId, actualQty, batchNo, estimatedArrival } = event

        const { data: calls } = await db.collection('material_calls').where({ callId }).get()
        if (calls.length === 0) return { code: -1, msg: '呼叫单不存在' }

        await db.collection('material_calls').where({ callId }).update({
          data: {
            status: 'PROCESSING', statusLabel: '处理中',
            actualQty: Number(actualQty) || 0,
            batchNo: batchNo || '',
            estimatedArrival: estimatedArrival || '',
            processedBy: user.realName,
            updateTime: new Date()
          }
        })

        return { code: 0, msg: '已确认处理' }
      }

      case 'completeMaterialCall': {
        const user = await resolveUserRole(OPENID)
        if (!user) return { code: -1, msg: '用户未注册' }

        const { callId } = event

        const { data: calls } = await db.collection('material_calls').where({ callId }).get()
        if (calls.length === 0) return { code: -1, msg: '呼叫单不存在' }

        await db.collection('material_calls').where({ callId }).update({
          data: {
            status: 'COMPLETED', statusLabel: '已完成',
            completeTime: new Date(),
            completedBy: user.realName
          }
        })

        return { code: 0, msg: '配送已确认完成' }
      }

      default:
        return { code: -1, msg: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[mes-inventory]', action, err)
    return { code: -1, msg: err.message || '操作失败' }
  }
}
