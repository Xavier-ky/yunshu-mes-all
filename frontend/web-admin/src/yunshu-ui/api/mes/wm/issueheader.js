import request from '@/yunshu-ui/api/request'
import { getWorkorder } from '@/yunshu-ui/api/mes/pro/workorder'
import { listWorkorderbom } from '@/yunshu-ui/api/mes/pro/workorderbom'
import { addIssueline, listIssueline } from '@/yunshu-ui/api/mes/wm/issueline'
import { genCode } from '@/yunshu-ui/api/system/autocode/rule'

function pad2(n) {
  return String(n).padStart(2, '0')
}

function formatDateTime(dt) {
  const d = dt instanceof Date ? dt : new Date(dt)
  if (Number.isNaN(d.getTime())) {
    return null
  }
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

function shouldUseClientFallback(err) {
  const msg = String(err?.message || '')
  return msg.includes('static resource')
    || msg.includes('404')
    || msg.includes('Not Found')
    || msg.includes('500')
    || msg.includes('接口请求失败')
}

async function createIssueFromWorkOrderClient(workorderId) {
  const woRes = await getWorkorder(workorderId)
  const wo = woRes?.data
  if (!wo?.workorderCode) {
    throw new Error('生产工单不存在')
  }

  const listRes = await listIssueheader({
    workorderCode: wo.workorderCode,
    pageNum: 1,
    pageSize: 20,
  })
  const existing = (listRes?.rows || []).find(
    (row) => row.status !== 'FINISHED' && row.status !== 'CANCELED',
  )

  let issueId = existing?.issueId
  if (issueId) {
    const lineRes = await listIssueline({ issueId, pageNum: 1, pageSize: 1 })
    if ((lineRes?.total || 0) > 0) {
      return { data: issueId }
    }
  }

  if (!issueId) {
    let issueCode = `IS${Date.now() % 100000}`
    try {
      const codeRes = await genCode('ISSUE_CODE')
      const code = codeRes?.data ?? codeRes
      if (typeof code === 'string' && code.trim()) {
        issueCode = code.trim()
      }
    } catch {
      /* use local fallback code */
    }

    const now = formatDateTime(new Date())
    const requiredTime = formatDateTime(wo.requestDate) || formatDateTime(new Date(Date.now() + 86400000))

    const header = {
      issueCode,
      issueName: `生产领料-${wo.workorderCode}`,
      workorderId: wo.workorderId ?? workorderId,
      workorderCode: wo.workorderCode,
      workorderName: wo.workorderName,
      clientId: wo.clientId,
      clientCode: wo.clientCode,
      clientName: wo.clientName,
      requiredTime,
      issueDate: now,
      status: 'PREPARE',
    }

    const createRes = await addIssueheader(header)
    issueId = createRes?.data ?? createRes
    if (!issueId) {
      throw new Error('创建领料单头失败')
    }
  }

  const bomRes = await listWorkorderbom({ workorderId })
  const bomRows = bomRes?.rows || []
  for (const bom of bomRows) {
    await addIssueline({
      issueId,
      itemId: bom.itemId,
      itemCode: bom.itemCode,
      itemName: bom.itemName,
      specification: bom.itemSpc || bom.specification,
      unitOfMeasure: bom.unitOfMeasure,
      unitName: bom.unitName,
      quantityIssued: bom.quantity,
    })
  }

  return { data: issueId }
}

// 查询生产领料单头列表
export function listIssueheader(query) {
  return request({
    url: '/mes/wm/issueheader/list',
    method: 'get',
    params: query
  })
}

// 查询生产领料单头详细
export function getIssueheader(issueId) {
  return request({
    url: '/mes/wm/issueheader/' + issueId,
    method: 'get'
  })
}

/**
 * 检查领料数量与拣货数量是否一致
 * @param  issueId 
 * @returns 
 */
export function checkQuantity(issueId) {
  return request({
    url: '/mes/wm/issueheader/checkQuantity/' + issueId,
    method: 'get'
  })
}


// 新增生产领料单头
export function addIssueheader(data) {
  return request({
    url: '/mes/wm/issueheader',
    method: 'post',
    data: data
  })
}

// 从生产工单一键生成领料单（含 BOM 行）；后端未部署时自动走前端组合接口
export async function createIssueFromWorkOrder(workorderId) {
  try {
    return await request({
      url: '/mes/wm/issueheader/fromWorkOrder/' + workorderId,
      method: 'post'
    })
  } catch (err) {
    if (!shouldUseClientFallback(err)) {
      throw err
    }
    return createIssueFromWorkOrderClient(workorderId)
  }
}

// 修改生产领料单头
export function updateIssueheader(data) {
  return request({
    url: '/mes/wm/issueheader',
    method: 'put',
    data: data
  })
}

// 删除生产领料单头
export function delIssueheader(issueId) {
  return request({
    url: '/mes/wm/issueheader/' + issueId,
    method: 'delete'
  })
}

//执行出库
export function execute(issueId) {
  return request({
    url: '/mes/wm/issueheader/' + issueId,
    method: 'put'
  })
}
