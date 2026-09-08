import request from '@/yunshu-ui/api/request'
import { getWorkorder } from '@/yunshu-ui/api/mes/pro/workorder'
import { listProductrecptline, addProductrecptline } from '@/yunshu-ui/api/mes/wm/productrecptline'
import { genCode } from '@/yunshu-ui/api/system/autocode/rule'

function pad2(n) {
  return String(n).padStart(2, '0')
}

function formatDate(dt) {
  const d = dt instanceof Date ? dt : new Date(dt)
  if (Number.isNaN(d.getTime())) {
    return null
  }
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function shouldUseClientFallback(err) {
  const msg = String(err?.message || '')
  return msg.includes('static resource')
    || msg.includes('404')
    || msg.includes('Not Found')
    || msg.includes('500')
    || msg.includes('接口请求失败')
}

async function createProductRecptFromWorkOrderClient(workorderId) {
  const woRes = await getWorkorder(workorderId)
  const wo = woRes?.data
  if (!wo?.workorderCode) {
    throw new Error('生产工单不存在')
  }

  const listRes = await listProductrecpt({
    workorderCode: wo.workorderCode,
    pageNum: 1,
    pageSize: 20,
  })
  const existing = (listRes?.rows || []).find(
    (row) => row.status !== 'FINISHED' && row.status !== 'CANCELED',
  )

  let recptId = existing?.recptId
  if (recptId) {
    const lineRes = await listProductrecptline({ recptId, pageNum: 1, pageSize: 1 })
    if ((lineRes?.total || 0) > 0) {
      return { data: recptId }
    }
  }

  if (!recptId) {
    let recptCode = `PR${Date.now() % 100000}`
    try {
      const codeRes = await genCode('PRODUCTRECPT_CODE')
      const code = codeRes?.data ?? codeRes
      if (typeof code === 'string' && code.trim()) {
        recptCode = code.trim()
      }
    } catch {
      /* use local fallback code */
    }

    const header = {
      recptCode,
      recptName: `成品入库-${wo.workorderCode}`,
      workorderId: wo.workorderId ?? workorderId,
      workorderCode: wo.workorderCode,
      workorderName: wo.workorderName,
      itemId: wo.productId,
      itemCode: wo.productCode,
      itemName: wo.productName,
      unitOfMeasure: wo.unitOfMeasure || 'PCS',
      unitName: wo.unitName || '件',
      recptDate: formatDate(new Date()),
      status: 'PREPARE',
    }

    const createRes = await addProductrecpt(header)
    recptId = createRes?.data ?? createRes
    if (!recptId) {
      throw new Error('创建入库单头失败')
    }
  }

  const qty = Number(wo.quantity) > 0 ? Number(wo.quantity) : (Number(wo.quantityProduced) || 1)
  await addProductrecptline({
    recptId,
    itemId: wo.productId,
    itemCode: wo.productCode,
    itemName: wo.productName,
    unitOfMeasure: wo.unitOfMeasure || 'PCS',
    unitName: wo.unitName || '件',
    quantityRecived: qty,
    batchCode: `PB-${String(wo.workorderCode).replace('WO', '')}`,
    workorderId: wo.workorderId ?? workorderId,
    workorderCode: wo.workorderCode,
    workorderName: wo.workorderName,
  })

  return { data: recptId }
}

// 查询产品入库录列表
export function listProductrecpt(query) {
  return request({
    url: '/mes/wm/productrecpt/list',
    method: 'get',
    params: query
  })
}

// 查询产品入库录详细
export function getProductrecpt(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'get'
  })
}

/**
 * 检查入库数量与上架数量是否一致
 * @param  recptId
 * @returns
 */
export function checkQuantity(recptId) {
  return request({
    url: '/mes/wm/productrecpt/checkQuantity/' + recptId,
    method: 'get'
  })
}

// 新增产品入库录
export function addProductrecpt(data) {
  return request({
    url: '/mes/wm/productrecpt',
    method: 'post',
    data: data
  })
}

// 从生产工单一键生成成品入库单（含产品行）；后端未部署时自动走前端组合接口
export async function createProductRecptFromWorkOrder(workorderId) {
  try {
    return await request({
      url: '/mes/wm/productrecpt/fromWorkOrder/' + workorderId,
      method: 'post'
    })
  } catch (err) {
    if (!shouldUseClientFallback(err)) {
      throw err
    }
    return createProductRecptFromWorkOrderClient(workorderId)
  }
}

// 修改产品入库录
export function updateProductrecpt(data) {
  return request({
    url: '/mes/wm/productrecpt',
    method: 'put',
    data: data
  })
}

// 删除产品入库录
export function delProductrecpt(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'delete'
  })
}

//执行入库
export function execute(recptId) {
  return request({
    url: '/mes/wm/productrecpt/' + recptId,
    method: 'put'
  })
}
