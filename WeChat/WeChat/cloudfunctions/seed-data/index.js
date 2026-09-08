

const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const _ = db.command

const now = new Date()
const fmt = (d) => {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
const daysAgo = (n) => { const d = new Date(now); d.setDate(d.getDate()-n); return d }
const hoursAgo = (n) => { const d = new Date(now); d.setHours(d.getHours()-n); return d }

const ID = {

  sch1: 'SCH-20260717001', sch2: 'SCH-20260717002', sch3: 'SCH-20260716001',

  pt1: 'PROD-001', pt2: 'PROD-002', pt3: 'PROD-003', pt4: 'PROD-004',
  pt5: 'PROD-005', pt6: 'PROD-006', pt7: 'PROD-007',

  qt1: 'QUAL-001', rt1: 'REPAIR-001', at1: 'ANDON-001', mt1: 'MATL-001',

  d1: 'EQ-AS-001', d2: 'EQ-AS-002', d3: 'EQ-AS-003', d4: 'EQ-AS-004', d5: 'EQ-AS-005',

  a1: 'ANDON-20260717001', a2: 'ANDON-20260717002',

  g1: 'GRP-20260717001', g2: 'GRP-20260717002',

  c1: 'MC-20260717001', c2: 'MC-20260717002',

  inv1:'INV-001', inv2:'INV-002', inv3:'INV-003', inv4:'INV-004', inv5:'INV-005',
  inv6:'INV-006', inv7:'INV-007', inv8:'INV-008', inv9:'INV-009', inv10:'INV-010',
}

exports.main = async () => {
  try {

    const names = ['users','tasks','schedules','andon_events','devices','inventory','material_calls','chat_groups','chat_messages','quality_records','tpm_alerts','dashboard_snapshots']
    for (const n of names) {
      try {
        const { data } = await db.collection(n).limit(100).get()
        for (const d of data) await db.collection(n).doc(d._id).remove()
        console.log(`✓ 清空 ${n} (${data.length})`)
      } catch (_) {}
    }

    const users = [
      { openid:'', userId:'U001', realName:'张三',   roleCode:'LINE_OPERATOR',         roleName:'产线操作工', lineName:'风扇总装一线', lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
      { openid:'', userId:'U002', realName:'李四',   roleCode:'QUALITY_INSPECTOR',    roleName:'质检员',     lineName:'风扇总装一线', lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
      { openid:'', userId:'U003', realName:'王五',   roleCode:'WAREHOUSE_KEEPER',     roleName:'仓库物料员', lineName:'原材料仓',     lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
      { openid:'', userId:'U004', realName:'赵六',   roleCode:'EQUIPMENT_MAINTAINER', roleName:'设备维修员', lineName:'风扇总装一线', lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
      { openid:'', userId:'U005', realName:'王主管', roleCode:'PRODUCTION_SUPERVISOR', roleName:'生产主管',   lineName:'全部产线',     lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
      { openid:'', userId:'U008', realName:'陈总',   roleCode:'MANAGER',               roleName:'管理层',     lineName:'全部产线',     lastLoginAt:daysAgo(30), createdAt:daysAgo(30) },
    ]
    for (const u of users) await db.collection('users').add({ data: u })
    console.log(`✓ users (${users.length})`)

    const devices = [
      { deviceId:ID.d1, deviceCode:ID.d1, deviceName:'自动锁螺丝机', deviceModel:'ASM-2000', lineCode:'LINE-01', lineName:'风扇总装一线', workstationCode:'WS-01', workstationName:'总装工位', status:'RUNNING', statusLabel:'运行', lastMaintainTime:fmt(daysAgo(15)), inspectRecords:[{inspectId:'INS-001',inspectTime:daysAgo(2),inspectBy:'赵六',result:'NORMAL',resultLabel:'正常'}], repairRecords:[], faultRecords:[] },
      { deviceId:ID.d2, deviceCode:ID.d2, deviceName:'电机测试台',   deviceModel:'MT-500',  lineCode:'LINE-01', lineName:'风扇总装一线', workstationCode:'WS-02', workstationName:'测试工位', status:'OVERDUE_MAINT', statusLabel:'超期未点检', lockReason:'距上次点检已过10天', lastMaintainTime:fmt(daysAgo(30)), inspectRecords:[{inspectId:'INS-002',inspectTime:daysAgo(10),inspectBy:'赵六',result:'ABNORMAL',resultLabel:'异常'}], repairRecords:[], faultRecords:[] },
      { deviceId:ID.d3, deviceCode:ID.d3, deviceName:'自动打包机',   deviceModel:'PK-100',  lineCode:'LINE-01', lineName:'风扇总装一线', workstationCode:'WS-03', workstationName:'包装工位', status:'RUNNING', statusLabel:'运行', lastMaintainTime:fmt(daysAgo(5)), inspectRecords:[{inspectId:'INS-003',inspectTime:daysAgo(1),inspectBy:'赵六',result:'NORMAL',resultLabel:'正常'}], repairRecords:[], faultRecords:[] },
      { deviceId:ID.d4, deviceCode:ID.d4, deviceName:'平衡检测仪',   deviceModel:'BL-300',  lineCode:'LINE-02', lineName:'风扇总装二线', workstationCode:'WS-04', workstationName:'电机装配工位', status:'REPAIRING', statusLabel:'维修中', faultCode:'SENSOR_FAULT', faultDescription:'检测结果偏差超出允许范围', lastMaintainTime:fmt(daysAgo(60)), inspectRecords:[], repairRecords:[{repairId:'REP-001',faultCode:'SENSOR_FAULT',faultDescription:'检测结果偏差',repairResult:'',repairTime:fmt(hoursAgo(2)),repairBy:'赵六'}], faultRecords:[{faultId:'FLT-001',faultCode:'SENSOR_FAULT',faultDescription:'检测结果偏差超出允许范围',faultTime:fmt(hoursAgo(3)),reportedBy:'张三',status:'REPAIRING'}] },
      { deviceId:ID.d5, deviceCode:ID.d5, deviceName:'噪音测试箱',   deviceModel:'NT-200',  lineCode:'LINE-02', lineName:'风扇总装二线', workstationCode:'WS-05', workstationName:'测试工位B', status:'FAULT', statusLabel:'故障', faultDescription:'测试箱门密封失效，噪音读数异常', lastMaintainTime:fmt(daysAgo(45)), inspectRecords:[], repairRecords:[], faultRecords:[{faultId:'FLT-002',faultCode:'OTHER',faultDescription:'测试箱门密封失效',faultTime:fmt(hoursAgo(5)),reportedBy:'张三',status:'INITIATED'}] },
    ]
    for (const d of devices) await db.collection('devices').add({ data: d })
    console.log(`✓ devices (${devices.length})`)

    const schedules = [
      { id:ID.sch1, scheduleNo:ID.sch1, lineName:'风扇总装一线', productName:'落地扇 FS40-A', planQty:100, completedQty:45, operatorName:'张三', operatorId:'U001', routingSteps:'电机装配,扇叶安装,通电测试,包装', toolingCode:'TL-FS40', standardHours:0.5, description:'A批次订单，优先保障交付', priority:'HIGH', priorityLabel:'高', status:'IN_PROGRESS', statusLabel:'进行中', taskIds:[ID.pt1,ID.pt2,ID.pt3,ID.pt4], createdBy:'王主管', createdByOpenid:'', createTime:daysAgo(2), updateTime:hoursAgo(1) },
      { id:ID.sch2, scheduleNo:ID.sch2, lineName:'风扇总装一线', productName:'台扇 FS30-B', planQty:50, completedQty:0, operatorName:'张三', operatorId:'U001', routingSteps:'电机装配,通电测试,包装', toolingCode:'TL-FS30', standardHours:0.4, description:'常规排产', priority:'MEDIUM', priorityLabel:'中', status:'PENDING', statusLabel:'待执行', taskIds:[ID.pt5,ID.pt6,ID.pt7], createdBy:'王主管', createdByOpenid:'', createTime:daysAgo(1), updateTime:daysAgo(1) },
      { id:ID.sch3, scheduleNo:ID.sch3, lineName:'风扇总装二线', productName:'落地扇 FS50-B', planQty:200, completedQty:200, operatorName:'张三', operatorId:'U001', routingSteps:'电机装配,包装', toolingCode:'TL-FS50', standardHours:0.3, description:'已完成订单', priority:'HIGH', priorityLabel:'高', status:'COMPLETED', statusLabel:'已完成', taskIds:[], createdBy:'王主管', createdByOpenid:'', createTime:daysAgo(5), updateTime:daysAgo(1) },
    ]

    const tasks = [

      { taskId:ID.pt1, taskNo:ID.pt1, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'电机装配', workstationName:'风扇总装一线', planQty:100, completedQty:100, qty:100, status:'COMPLETED', statusLabel:'已完成', priority:'HIGH', priorityLabel:'高', assignedTo:'张三', routingSteps:['电机装配','扇叶安装','通电测试','包装'], stepIndex:0, currentStepIndex:0, toolingCode:'TL-FS40', standardHoursPerUnit:0.5, frozen:false, version:3, createdBy:'王主管', lastReportBy:'张三', lastReportAt:hoursAgo(8), createTime:daysAgo(2), updateTime:hoursAgo(8) },
      { taskId:ID.pt2, taskNo:ID.pt2, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'扇叶安装', workstationName:'风扇总装一线', planQty:100, completedQty:45, qty:100, status:'PROCESSING', statusLabel:'进行中', priority:'HIGH', priorityLabel:'高', assignedTo:'张三', routingSteps:['电机装配','扇叶安装','通电测试','包装'], stepIndex:1, currentStepIndex:1, toolingCode:'TL-FS40', standardHoursPerUnit:0.5, frozen:false, version:5, createdBy:'王主管', lastReportBy:'张三', lastReportAt:hoursAgo(1), createTime:daysAgo(2), updateTime:hoursAgo(1) },
      { taskId:ID.pt3, taskNo:ID.pt3, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'通电测试', workstationName:'风扇总装一线', planQty:100, completedQty:0, qty:100, status:'PENDING', statusLabel:'等待上道完工', priority:'HIGH', priorityLabel:'高', assignedTo:'张三', routingSteps:['电机装配','扇叶安装','通电测试','包装'], stepIndex:2, currentStepIndex:2, toolingCode:'TL-FS40', standardHoursPerUnit:0.5, frozen:false, version:0, createdBy:'王主管', deviceCode:ID.d2, createTime:daysAgo(2), updateTime:daysAgo(2) },
      { taskId:ID.pt4, taskNo:ID.pt4, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'包装',   workstationName:'风扇总装一线', planQty:100, completedQty:0, qty:100, status:'PENDING', statusLabel:'等待上道完工', priority:'HIGH', priorityLabel:'高', assignedTo:'张三', routingSteps:['电机装配','扇叶安装','通电测试','包装'], stepIndex:3, currentStepIndex:3, toolingCode:'TL-FS40', standardHoursPerUnit:0.5, frozen:false, version:0, createdBy:'王主管', createTime:daysAgo(2), updateTime:daysAgo(2) },

      { taskId:ID.pt5, taskNo:ID.pt5, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch2, productName:'台扇 FS30-B', processName:'电机装配', workstationName:'风扇总装一线', planQty:50, completedQty:0, qty:50, status:'PENDING', statusLabel:'待处理', priority:'MEDIUM', priorityLabel:'中', assignedTo:'张三', routingSteps:['电机装配','通电测试','包装'], stepIndex:0, currentStepIndex:0, toolingCode:'TL-FS30', standardHoursPerUnit:0.4, frozen:false, version:0, createdBy:'王主管', createTime:daysAgo(1), updateTime:daysAgo(1) },
      { taskId:ID.pt6, taskNo:ID.pt6, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch2, productName:'台扇 FS30-B', processName:'通电测试', workstationName:'风扇总装一线', planQty:50, completedQty:0, qty:50, status:'PENDING', statusLabel:'等待上道完工', priority:'MEDIUM', priorityLabel:'中', assignedTo:'张三', routingSteps:['电机装配','通电测试','包装'], stepIndex:1, currentStepIndex:1, toolingCode:'TL-FS30', standardHoursPerUnit:0.4, frozen:false, version:0, createdBy:'王主管', createTime:daysAgo(1), updateTime:daysAgo(1) },
      { taskId:ID.pt7, taskNo:ID.pt7, taskType:'PRODUCTION', taskTypeName:'生产任务', workOrderNo:ID.sch2, productName:'台扇 FS30-B', processName:'包装',   workstationName:'风扇总装一线', planQty:50, completedQty:0, qty:50, status:'PENDING', statusLabel:'等待上道完工', priority:'MEDIUM', priorityLabel:'中', assignedTo:'张三', routingSteps:['电机装配','通电测试','包装'], stepIndex:2, currentStepIndex:2, toolingCode:'TL-FS30', standardHoursPerUnit:0.4, frozen:false, version:0, createdBy:'王主管', createTime:daysAgo(1), updateTime:daysAgo(1) },

      { taskId:ID.qt1, taskNo:ID.qt1, taskType:'QUALITY', taskTypeName:'质检任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'首检', workstationName:'风扇总装一线', planQty:10, completedQty:3, qty:10, status:'PROCESSING', statusLabel:'进行中', priority:'HIGH', priorityLabel:'高', assignedTo:'李四', frozen:false, version:1, createdBy:'王主管', createTime:daysAgo(2), updateTime:hoursAgo(3) },
      { taskId:'QUAL-002', taskNo:'QUAL-002', taskType:'QUALITY', taskTypeName:'质检任务', workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'巡检', workstationName:'风扇总装一线', planQty:10, completedQty:0, qty:10, status:'HOLD', statusLabel:'已冻结', priority:'HIGH', priorityLabel:'高', assignedTo:'李四', frozen:true, freezeReason:'报废：电机异响，疑似轴承损坏', freezeTime:hoursAgo(3), frozenBy:'李四', version:1, createdBy:'王主管', createTime:daysAgo(1), updateTime:hoursAgo(3) },

      { taskId:ID.rt1, taskNo:ID.rt1, taskType:'REPAIR', taskTypeName:'维修任务', productName:'平衡检测仪', productModel:ID.d4, processName:'设备维修', workstationName:'风扇总装二线', planQty:0, completedQty:0, qty:0, status:'PROCESSING', statusLabel:'进行中', priority:'HIGH', priorityLabel:'高', assignedTo:'赵六', description:'检测结果偏差超出允许范围，传感器故障', reportedBy:'张三', frozen:false, version:0, createTime:hoursAgo(2), updateTime:hoursAgo(2) },

      { taskId:ID.at1, taskNo:ID.at1, taskType:'ANDON', taskTypeName:'安灯处理', workOrderNo:ID.a1, productName:'噪音测试箱 NT-200', productModel:ID.d5, processName:'设备维修', workstationName:'风扇总装二线', planQty:0, completedQty:0, qty:0, status:'PENDING', statusLabel:'待处理', priority:'HIGH', priorityLabel:'高', assignedTo:'赵六', description:'测试箱门密封失效，噪音读数异常', createdBy:'张三', frozen:false, version:0, createTime:hoursAgo(5), updateTime:hoursAgo(5) },

      { taskId:ID.mt1, taskNo:ID.mt1, taskType:'MATERIAL', taskTypeName:'物料任务', workOrderNo:ID.sch1, productName:'风扇电机', processName:'领料', workstationName:'原材料仓', planQty:200, completedQty:0, qty:200, issuedQty:100, status:'PROCESSING', statusLabel:'进行中', priority:'MEDIUM', priorityLabel:'中', assignedTo:'王五', materialCode:'MAT-MOTOR-001', materialName:'风扇电机', frozen:false, version:2, createdBy:'王主管', createTime:daysAgo(2), updateTime:hoursAgo(4) },
    ]

    for (const s of schedules) await db.collection('schedules').add({ data: s })
    for (const t of tasks) await db.collection('tasks').add({ data: t })
    console.log(`✓ schedules (${schedules.length}) + tasks (${tasks.length})`)

    const andonEvents = [
      { eventId:ID.a1, eventNo:ID.a1, groupId:ID.g1, eventType:'EQUIPMENT', eventTypeName:'设备故障安灯', reasonCode:'OTHER', reasonName:'密封故障', lineName:'风扇总装二线', workstationName:'测试工位B', deviceCode:ID.d5, deviceName:'噪音测试箱', description:'测试箱门密封失效，噪音读数异常', status:'INITIATED', statusLabel:'已发起', priority:'HIGH', createdBy:'张三', createdByName:'张三', targetRole:'EQUIPMENT_MAINTAINER', targetRoleName:'设备维修员', targetPerson:'赵六', createTime:hoursAgo(5), handleRecords:[] },
      { eventId:ID.a2, eventNo:ID.a2, groupId:ID.g2, eventType:'MATERIAL', eventTypeName:'物料异常安灯', reasonCode:'MATERIAL_DEFECT', reasonName:'物料缺陷', lineName:'风扇总装一线', workstationName:'总装工位', description:'发现一批电机存在异响，疑似轴承问题', status:'RESPONDED', statusLabel:'已响应', priority:'MEDIUM', createdBy:'张三', createdByName:'张三', targetRole:'WAREHOUSE_KEEPER', targetRoleName:'仓库物料员', targetPerson:'王五', createTime:hoursAgo(8), responseTime:hoursAgo(6), handleRecords:[{handler:'王五',handlerName:'王五',action:'已确认物料问题，正在协调更换批次',time:hoursAgo(6)}] },
    ]
    for (const a of andonEvents) await db.collection('andon_events').add({ data: a })
    console.log(`✓ andon_events (${andonEvents.length})`)

    const chatGroups = [
      { groupId:ID.g1, name:'设备故障 · 风扇总装二线', bizType:'ANDON', bizRefId:ID.a1, bizRefNo:ID.a1, bizContext:{title:'设备故障安灯',detail:'噪音测试箱密封失效',statusLabel:'已发起',status:'INITIATED'}, creatorId:'U001', creatorName:'张三', members:[{userId:'U001',userName:'张三',roleCode:'LINE_OPERATOR'},{userId:'U004',userName:'赵六',roleCode:'EQUIPMENT_MAINTAINER'}], status:'ACTIVE', createTime:hoursAgo(5) },
      { groupId:ID.g2, name:'物料异常 · 风扇总装一线', bizType:'ANDON', bizRefId:ID.a2, bizRefNo:ID.a2, bizContext:{title:'物料异常安灯',detail:'电机异响疑似轴承问题',statusLabel:'已响应',status:'RESPONDED'}, creatorId:'U001', creatorName:'张三', members:[{userId:'U001',userName:'张三',roleCode:'LINE_OPERATOR'},{userId:'U003',userName:'王五',roleCode:'WAREHOUSE_KEEPER'}], status:'ACTIVE', createTime:hoursAgo(8) },
    ]
    const chatMsgs = [
      { msgId:'CM-001', groupId:ID.g1, type:'SYSTEM', senderId:'system', senderName:'系统', content:'安灯已发起：设备故障\n发起人：张三\n处理人：赵六（设备维修员）\n描述：测试箱门密封失效，噪音读数异常', time:hoursAgo(5) },
      { msgId:'CM-002', groupId:ID.g1, type:'TEXT', senderId:'U001', senderName:'张三', content:'噪音测试箱的密封条坏了，测试数据全跑偏', time:hoursAgo(4.5) },
      { msgId:'CM-003', groupId:ID.g1, type:'TEXT', senderId:'U004', senderName:'赵六', content:'收到，我先远程看一下日志', time:hoursAgo(4) },
      { msgId:'CM-004', groupId:ID.g1, type:'TEXT', senderId:'U004', senderName:'赵六', content:'确认是密封问题，需要更换密封条，预计2小时', time:hoursAgo(3.5) },
      { msgId:'CM-005', groupId:ID.g2, type:'SYSTEM', senderId:'system', senderName:'系统', content:'安灯已发起：物料异常\n发起人：张三\n处理人：王五（仓库物料员）\n描述：发现一批电机存在异响', time:hoursAgo(8) },
      { msgId:'CM-006', groupId:ID.g2, type:'TEXT', senderId:'U003', senderName:'王五', content:'已确认物料问题，正在协调更换批次', time:hoursAgo(6) },
    ]
    for (const g of chatGroups) await db.collection('chat_groups').add({ data: g })
    for (const m of chatMsgs) await db.collection('chat_messages').add({ data: m })
    console.log(`✓ chat_groups (${chatGroups.length}) + chat_messages (${chatMsgs.length})`)

    const inventory = [

      { taskId:ID.inv1,  taskNo:ID.inv1,  taskType:'INBOUND', materialCode:'MAT-MOTOR-001', materialName:'风扇电机', spec:'FS40-A', batchNo:'BATCH-20260706001', warehouseName:'原材料仓', locationCode:'A-01-01', qty:500, registeredQty:400, unit:'台', status:'COMPLETED', statusLabel:'已完成', createdBy:'王五', createTime:daysAgo(7), updateTime:hoursAgo(4) },
      { taskId:ID.inv2,  taskNo:ID.inv2,  taskType:'INBOUND', materialCode:'MAT-BLADE-001', materialName:'风扇扇叶', spec:'40cm',   batchNo:'BATCH-20260706002', warehouseName:'原材料仓', locationCode:'A-02-01', qty:300, registeredQty:250, unit:'套', status:'COMPLETED', statusLabel:'已完成', createdBy:'王五', createTime:daysAgo(6), updateTime:daysAgo(6) },
      { taskId:ID.inv3,  taskNo:ID.inv3,  taskType:'INBOUND', materialCode:'MAT-COVER-001', materialName:'风扇网罩', spec:'40cm',   batchNo:'BATCH-20260706003', warehouseName:'原材料仓', locationCode:'A-03-01', qty:200, registeredQty:200, unit:'个', status:'COMPLETED', statusLabel:'已完成', createdBy:'王五', createTime:daysAgo(5), updateTime:daysAgo(5) },
      { taskId:ID.inv10, taskNo:ID.inv10, taskType:'INBOUND', materialCode:'MAT-BLADE-001', materialName:'风扇扇叶', spec:'40cm',   batchNo:'BATCH-20260706004', warehouseName:'原材料仓', locationCode:'A-02-02', qty:200, registeredQty:80,  unit:'套', status:'COMPLETED', statusLabel:'已完成', createdBy:'王五', createTime:daysAgo(4), updateTime:daysAgo(3) },

      { taskId:ID.inv4,  taskNo:ID.inv4,  taskType:'INBOUND', materialCode:'MAT-MOTOR-001', materialName:'风扇电机', spec:'FS40-A', batchNo:'BATCH-20260706005', warehouseName:'原材料仓', locationCode:'A-01-02', qty:200, registeredQty:0,   unit:'台', status:'PENDING',   statusLabel:'待入库', createdBy:'王五', createTime:hoursAgo(4), updateTime:hoursAgo(4) },

      { taskId:ID.inv5,  taskNo:ID.inv5,  taskType:'ISSUE',   materialCode:'MAT-MOTOR-001', materialName:'风扇电机', spec:'FS40-A', batchNo:'BATCH-20260706001', warehouseName:'原材料仓', locationCode:'A-01-01', qty:100, unit:'台', status:'COMPLETED',  statusLabel:'已完成', assignedTo:'王五', issuedBy:'王五', issuedQty:100, createTime:daysAgo(2), updateTime:daysAgo(2) },
      { taskId:ID.inv7,  taskNo:ID.inv7,  taskType:'ISSUE',   materialCode:'MAT-BLADE-001', materialName:'风扇扇叶', spec:'40cm',   batchNo:'BATCH-20260706002', warehouseName:'原材料仓', locationCode:'A-02-01', qty:150, unit:'套', status:'PROCESSING', statusLabel:'进行中', assignedTo:'王五', issuedBy:'王五', issuedQty:60,  createTime:daysAgo(1), updateTime:hoursAgo(8) },
      { taskId:ID.inv8,  taskNo:ID.inv8,  taskType:'ISSUE',   materialCode:'MAT-COVER-001', materialName:'风扇网罩', spec:'40cm',   batchNo:'BATCH-20260706003', warehouseName:'原材料仓', locationCode:'A-03-01', qty:80,  unit:'个', status:'PENDING',    statusLabel:'待处理', assignedTo:'王五', issuedBy:'王五', issuedQty:0,   createTime:hoursAgo(2), updateTime:hoursAgo(2) },

      { taskId:ID.inv6,  taskNo:ID.inv6,  taskType:'RETURN',  materialCode:'MAT-BLADE-001', materialName:'风扇扇叶', spec:'40cm',   batchNo:'BATCH-20260706002', warehouseName:'原材料仓', locationCode:'A-02-02', qty:5,  unit:'套', reason:'扇叶毛刺，质量不合格', status:'PENDING',    statusLabel:'待处理', assignedTo:'王五', createdBy:'张三', createTime:hoursAgo(12), updateTime:hoursAgo(12) },
      { taskId:ID.inv9,  taskNo:ID.inv9,  taskType:'RETURN',  materialCode:'MAT-MOTOR-001', materialName:'风扇电机', spec:'FS40-A', batchNo:'BATCH-20260706001', warehouseName:'原材料仓', locationCode:'A-01-03', qty:3,  unit:'台', reason:'电机异响，疑似轴承故障', status:'COMPLETED',  statusLabel:'已完成', assignedTo:'王五', createdBy:'张三', createTime:daysAgo(3), updateTime:daysAgo(2) },
    ]
    for (const inv of inventory) await db.collection('inventory').add({ data: inv })
    console.log(`✓ inventory (${inventory.length})`)

    const materialCalls = [
      { callId:ID.c1, callNo:ID.c1, workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'电机装配', workstationName:'总装工位', lineName:'风扇总装一线', materialName:'风扇电机', materialCode:'MAT-MOTOR-001', needQty:50, remainQty:12, urgency:'URGENT', urgencyLabel:'紧急', remark:'电机库存仅剩12台，无法满足本班次需求', status:'PENDING', statusLabel:'待处理', operatorName:'张三', createTime:hoursAgo(6) },
      { callId:ID.c2, callNo:ID.c2, workOrderNo:ID.sch1, productName:'落地扇 FS40-A', processName:'扇叶安装', workstationName:'总装工位', lineName:'风扇总装一线', materialName:'风扇网罩', materialCode:'MAT-COVER-001', needQty:30, remainQty:5, urgency:'NORMAL', urgencyLabel:'一般', remark:'网罩库存偏低，请提前备货', status:'PROCESSING', statusLabel:'处理中', operatorName:'张三', createTime:hoursAgo(10), actualQty:30, batchNo:'BATCH-20260706003', estimatedArrival:`${fmt(now)}` },
    ]
    for (const mc of materialCalls) await db.collection('material_calls').add({ data: mc })
    console.log(`✓ material_calls (${materialCalls.length})`)

    const qualityRecords = [
      { recordId:'QC-001', recordType:'INSPECTION', taskId:ID.qt1, productSn:'SN-FS40A-001', productName:'落地扇 FS40-A', workOrderNo:ID.sch1, result:'QUALIFIED', sampleQty:5, goodQty:5, defectQty:0, action:'RELEASE', actionLabel:'放行', description:'外观检查、电机运转测试均正常', createdBy:'李四', createTime:hoursAgo(24), updateTime:hoursAgo(24) },
      { recordId:'QC-002', recordType:'INSPECTION', taskId:ID.qt1, productSn:'SN-FS40A-002', productName:'落地扇 FS40-A', workOrderNo:ID.sch1, result:'QUALIFIED', sampleQty:5, goodQty:5, defectQty:0, action:'RELEASE', actionLabel:'放行', description:'各项指标符合标准', createdBy:'李四', createTime:hoursAgo(20), updateTime:hoursAgo(20) },
      { recordId:'QC-003', recordType:'INSPECTION', taskId:ID.qt1, productSn:'SN-FS40A-003', productName:'落地扇 FS40-A', workOrderNo:ID.sch1, result:'QUALIFIED', sampleQty:5, goodQty:5, defectQty:0, action:'RELEASE', actionLabel:'放行', description:'检验合格', createdBy:'李四', createTime:hoursAgo(16), updateTime:hoursAgo(16) },
      { recordId:'QC-004', recordType:'DEFECT',    taskId:'QUAL-002', productSn:'SN-FS40A-004', productName:'落地扇 FS40-A', workOrderNo:ID.sch1, result:'UNQUALIFIED', sampleQty:5, goodQty:2, defectQty:3, action:'SCRAP', actionLabel:'报废', description:'电机运转时有明显异响，疑似轴承损坏', defectType:'MOTOR_NOISE', defectTypeName:'电机异响', defectQty:3, createdBy:'李四', createTime:hoursAgo(3), updateTime:hoursAgo(3) },
      { recordId:'QC-005', recordType:'DEFECT',    taskId:'QUAL-002', productSn:'SN-FS40A-005', productName:'落地扇 FS40-A', workOrderNo:ID.sch1, result:'UNQUALIFIED', sampleQty:5, goodQty:4, defectQty:1, action:'REWORK', actionLabel:'返工', description:'网罩有轻微松动', defectType:'COVER_LOOSE', defectTypeName:'网罩松动', defectQty:1, createdBy:'李四', createTime:hoursAgo(2), updateTime:hoursAgo(2) },
    ]
    for (const qr of qualityRecords) await db.collection('quality_records').add({ data: qr })
    console.log(`✓ quality_records (${qualityRecords.length})`)

    const { data: prodTasks } = await db.collection('tasks').where({ taskType:'PRODUCTION' }).get()
    const todayOutput = prodTasks.reduce((s,t)=>s+(t.completedQty||0),0)
    const totalPlan = prodTasks.reduce((s,t)=>s+(t.planQty||0),0)
    const qualifiedRate = totalPlan>0 ? (todayOutput/totalPlan*100).toFixed(1) : '0'
    const { total: andonCnt } = await db.collection('andon_events').where({ status:_.in(['INITIATED','RESPONDED','PROCESSING']) }).count()
    const { total: faultCnt } = await db.collection('devices').where({ status:_.in(['FAULT','REPAIRING']) }).count()
    const frozenCnt = prodTasks.filter(t=>t.frozen).length

    const lineMap = {}
    prodTasks.forEach(t=>{ const n=t.workstationName||'未知'; if(!lineMap[n]) lineMap[n]={lineName:n,planQty:0,actualQty:0}; lineMap[n].planQty+=t.planQty||0; lineMap[n].actualQty+=t.completedQty||0 })

    await db.collection('dashboard_snapshots').add({ data:{ type:'supervisor', data:{ todayOutput, todayQualifiedRate:qualifiedRate+'%', todayAbnormalCount:andonCnt+faultCnt+frozenCnt, equipmentFaultCount:faultCnt, lineProgress:Object.values(lineMap) }, generatedAt:new Date() }})
    await db.collection('dashboard_snapshots').add({ data:{ type:'management', data:{ todayOutput, todayQualifiedRate:qualifiedRate+'%', activeLines:Object.keys(lineMap).length, totalTasks:prodTasks.length, pendingTasks:prodTasks.filter(t=>t.status==='PENDING').length, completedTasks:prodTasks.filter(t=>t.status==='COMPLETED').length, andonOpen:andonCnt, equipmentIssues:faultCnt }, generatedAt:new Date() }})

    const overdueDevs = devices.filter(d=>d.status==='OVERDUE_MAINT').map(d=>({deviceCode:d.deviceCode,deviceName:d.deviceName,lineName:d.lineName,daysSince:10}))
    if (overdueDevs.length>0) await db.collection('tpm_alerts').add({ data:{ type:'HOURLY_SCAN', devices:overdueDevs, lockedCount:overdueDevs.length, generatedAt:new Date() }})

    console.log('✓ dashboard_snapshots + tpm_alerts')

    return { code:0, msg:'数据库初始化完成', data:{ users:users.length, devices:devices.length, schedules:schedules.length, tasks:tasks.length, andonEvents:andonEvents.length, chatGroups:chatGroups.length, chatMessages:chatMsgs.length, inventory:inventory.length, materialCalls:materialCalls.length, qualityRecords:qualityRecords.length, dashboard:'已生成', tpm:overdueDevs.length>0?`${overdueDevs.length}条告警`:'无超期设备' }}
  } catch (err) {
    console.error('[seed-data]', err)
    return { code:-1, msg:err.message||'初始化失败' }
  }
}
