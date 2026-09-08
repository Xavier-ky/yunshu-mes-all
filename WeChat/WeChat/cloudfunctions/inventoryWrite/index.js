const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()

exports.main = async (event) => {
  const { action } = event

  try {
    switch (action) {

      case 'issue': {
        const { taskId, batchNo, issueQty, warehouseName, locationCode } = event
        const qty = Number(issueQty) || 0
        if (qty <= 0) return { code: -1, message: '发料数量无效' }

        const { data: tasks } = await db.collection('tasks').where({ taskId }).get()
        if (tasks.length === 0) return { code: -1, message: '任务不存在' }

        const t = tasks[0]
        const newIssued = (t.issuedQty || 0) + qty
        if (newIssued > t.qty) return { code: -1, message: '发料数量超出需求' }

        await db.collection('tasks').where({ taskId }).update({ data: {
          issuedQty: newIssued,
          status: newIssued >= t.qty ? 'COMPLETED' : 'PROCESSING',
          statusLabel: newIssued >= t.qty ? '已完成' : '进行中',
          updateTime: new Date().toISOString()
        }})

        const inboundId = 'MAT-INB-' + Date.now()
        await db.collection('inventory').add({ data: {
          taskId: inboundId, taskNo: inboundId, taskType: 'INBOUND',
          materialCode: t.materialCode, materialName: t.materialName,
          batchNo: batchNo || '', warehouseName: warehouseName || '', locationCode: locationCode || '',
          qty, registeredQty: 0, status: 'PENDING', statusLabel: '待入库',
          createTime: new Date().toISOString()
        }})

        return { code: 0, message: '领料成功，已生成入库登记' }
      }

      case 'return': {
        const { taskId, materialCode, materialName, qty, reason } = event
        const returnQty = Number(qty) || 0
        if (returnQty <= 0) return { code: -1, message: '退料数量无效' }

        const returnId = 'MAT-RETURN-' + Date.now()
        await db.collection('inventory').add({ data: {
          taskId: returnId, taskNo: returnId, taskType: 'RETURN',
          materialCode: materialCode || '', materialName: materialName || '',
          qty: returnQty, reason: reason || '', status: 'PENDING', statusLabel: '待处理',
          createTime: new Date().toISOString()
        }})

        return { code: 0, message: '退料成功' }
      }

      default:
        return { code: -1, message: `未知操作: ${action}` }
    }
  } catch (err) {
    console.error('[inventoryWrite]', action, err)
    return { code: -1, message: err.message || '操作失败' }
  }
}
