

const axios = require('axios')
const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })
const db = cloud.database()
const _ = db.command

const DEEPSEEK_API_URL = 'https://api.deepseek.com/v1/chat/completions'
const MODEL = 'deepseek-chat'
const TIMEOUT_MS = 25000
const MAX_TOKENS = 2048
const TEMPERATURE = 0.7
const MAX_TOOL_LOOPS = 5

const REACT_SYSTEM_PROMPT = `你是一个遵循 ReAct 模式的智能助手，专门服务于"云枢智造 MES"现场人员。

你的响应流程必须严格遵循以下四步循环（内部执行，对外只输出最终结果）：

1. **Thought（思考）**：分析用户的输入，判断其真实意图（是闲聊咨询、数据查询，还是需要操作页面或填报异常）。
2. **Action（行动）**：
   - 如果用户明确要求"打开/去往/查看/跳转/进入"某个业务页面，**必须**调用 \`open_page\` 工具。
   - 如果用户明确描述了设备故障、质量异常、报修需求（如"报修""填报""登记""发起安灯""坏了""填上""帮我填"），**必须**调用 \`fill_form\` 工具，将提取到的关键信息填入 \`data\`。
   - 如果用户只是普通提问或闲聊，**不调用任何工具**，直接进入第 4 步。
3. **Observation（观察）**：（此步骤由系统自动完成，你无需处理）。
4. **Final Answer（最终答案）**：生成一段简洁、口语化、适合车间现场人员阅读的最终回复。

## ⚠️ 铁律（不可违反）

**涉及页面跳转或表单填写的操作，必须通过调用工具完成。**
- 绝对禁止用纯文字回复"已帮您填写""已为您打开""已跳转"等声称已完成操作的文本，除非你真的调用了对应的工具。
- 如果你没有调用工具，就不要说"已经做了"——你只能说你做了什么（文字回复），并引导用户自行操作。
- 识别到跳转/填报意图时，不要犹豫，必须立即调用对应工具。`

const TOOLS = [
  {
    type: 'function',
    function: {
      name: 'query_data',
      description: '【数据查询】查询系统实时数据。当用户询问"产量""合格率""库存""设备状态""异常数量""排产进度"等数据类问题时，必须调用此工具获取真实数据，禁止凭空编造数字。',
      parameters: {
        type: 'object',
        properties: {
          query: {
            type: 'string',
            description: '查询类型',
            enum: ['production_today', 'quality_rate', 'inventory_summary', 'device_status', 'abnormal_count', 'schedule_progress']
          }
        },
        required: ['query']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'open_page',
      description: '【纯跳转】打开/跳转到指定业务页面。当用户要求"打开XX""查看XX""去XX""跳转到XX""进入XX"时，必须调用此工具，禁止用纯文本"已为您打开"代替。',
      parameters: {
        type: 'object',
        properties: {
          page: {
            type: 'string',
            description: '目标页面路径，必须从支持的列表中选择',
            enum: [
              '/pages/production/task-list/index',
              '/pages/quality/task-list/index',
              '/pages/inventory/task-list/index',
              '/pages/equipment/status/index',
              '/pages/equipment/detail/index',
              '/pages/equipment/repair/index',
              '/pages/andon/create/index',
              '/pages/andon/detail/index',
              '/pages/scan/index',
              '/pages/traceability/product/index',
              '/pages/supervisor/dashboard/index',
              '/pages/supervisor/abnormal/index',
              '/pages/trend/index',
              '/pages/message/index',
              '/pages/mine/index',
              '/pages/home/index'
            ]
          }
        },
        required: ['page']
      }
    }
  },
  {
    type: 'function',
    function: {
      name: 'fill_form',
      description: '【表单预填】跳转到表单页面并自动填入识别到的信息。当用户表达了"报修""填报""登记""发起安灯""缺陷""坏了要修""填上""帮我填""填表"等意图时，必须调用此工具，禁止用纯文本"已帮您填写"代替。提取用户描述中的关键信息填入 data 字段，表单不会自动提交，需用户人工确认。',
      parameters: {
        type: 'object',
        properties: {
          page: {
            type: 'string',
            description: '目标表单页面路径',
            enum: [
              '/pages/equipment/repair/index',
              '/pages/andon/create/index',
              '/pages/quality/defect/index'
            ]
          },
          data: {
            type: 'object',
            description: '预填的字段数据。根据目标表单的不同，可填字段不同。只填用户明确提到的信息，未提及的字段不填，宁可少填也不少填。',
            properties: {

              deviceCode: { type: 'string', description: '设备编号' },
              deviceName: { type: 'string', description: '设备名称' },
              lineCode: { type: 'string', description: '产线编码' },
              workstationCode: { type: 'string', description: '工位编码' },
              faultType: {
                type: 'string',
                description: '故障类型',
                enum: ['SCREW_JAM', 'MOTOR_OVERHEAT', 'BELT_BROKEN', 'SENSOR_FAULT', 'CONTROL_FAULT', 'OTHER']
              },
              faultDescription: { type: 'string', description: '故障描述' },

              eventType: {
                type: 'string',
                description: '安灯事件类型',
                enum: ['QUALITY', 'EQUIPMENT', 'MATERIAL', 'SAFETY', 'OTHER']
              },
              reasonCode: {
                type: 'string',
                description: '安灯原因代码',
                enum: ['DEFECT_RATE_HIGH', 'MACHINE_FAULT', 'MATERIAL_SHORTAGE', 'MATERIAL_DEFECT', 'PROCESS_ERROR', 'SAFETY_ISSUE', 'OTHER']
              },
              workOrderNo: { type: 'string', description: '工单号' },
              description: { type: 'string', description: '问题描述（安灯/缺陷共用）' },

              defectType: {
                type: 'string',
                description: '缺陷类型',
                enum: ['MOTOR_NOISE', 'BLADE_DAMAGE', 'COVER_LOOSE', 'SWITCH_FAULT', 'BASE_UNSTABLE', 'SPEED_ABNORMAL', 'SCRATCH', 'DIMENSION', 'SURFACE_RUST', 'LABEL_ERROR', 'PACKAGE_DAMAGE', 'OTHER']
              },
              defectQty: { type: 'string', description: '缺陷数量' }
            }
          }
        },
        required: ['page', 'data']
      }
    }
  }
]

const PAGE_NAMES = {
  '/pages/production/task-list/index': '生产任务列表',
  '/pages/quality/task-list/index': '质检任务列表',
  '/pages/inventory/task-list/index': '库存任务列表',
  '/pages/equipment/status/index': '设备状态',
  '/pages/equipment/detail/index': '设备详情',
  '/pages/equipment/repair/index': '设备报修',
  '/pages/andon/create/index': '发起安灯',
  '/pages/andon/detail/index': '安灯详情',
  '/pages/scan/index': '扫码作业',
  '/pages/traceability/product/index': '产品追溯',
  '/pages/supervisor/dashboard/index': '生产看板',
  '/pages/supervisor/abnormal/index': '异常监控',
  '/pages/trend/index': '产量趋势',
  '/pages/message/index': '消息列表',
  '/pages/mine/index': '个人中心',
  '/pages/home/index': '首页',
  '/pages/quality/defect/index': '缺陷登记'
}

async function executeToolCall(toolCall) {
  const { name, arguments: argsStr } = toolCall.function
  let args

  try {
    args = JSON.parse(argsStr)
  } catch (e) {
    console.error('[DeepSeek] 工具参数 JSON.parse 失败:', e.message, '原始:', argsStr)
    return { success: false, error: '参数解析失败: ' + e.message }
  }

  console.log('[DeepSeek] 执行工具:', name, '参数:', JSON.stringify(args))

  if (args.data && typeof args.data === 'string') {
    try {
      args.data = JSON.parse(args.data)
      console.log('[DeepSeek] data 由字符串转为对象:', JSON.stringify(args.data))
    } catch (e) {
      console.warn('[DeepSeek] data 字符串 JSON.parse 失败，使用空对象:', e.message)
      args.data = {}
    }
  }

  switch (name) {
    case 'open_page': {
      const pageName = PAGE_NAMES[args.page] || args.page
      console.log('[DeepSeek] open_page → action type=navigateTo page=', args.page)
      return {
        success: true,
        action: { type: 'navigateTo', page: args.page },
        message: `已确认跳转目标：${pageName}`
      }
    }

    case 'fill_form': {
      const pageName = PAGE_NAMES[args.page] || args.page
      const data = args.data || {}
      const fieldNames = Object.keys(data)
      const dataSummary = fieldNames.length > 0
        ? `已提取 ${fieldNames.length} 个字段：${fieldNames.join('、')}`
        : '未提取到可预填的字段'
      console.log('[DeepSeek] fill_form → action type=fillForm page=', args.page, 'data类型=', typeof data, '字段=', fieldNames)
      return {
        success: true,
        action: { type: 'fillForm', page: args.page, data },
        message: `${pageName}页面。${dataSummary}。请提醒用户核对后手动提交。`
      }
    }

    case 'query_data': {
      try {
        const result = await queryDatabase(args.query)
        return { success: true, message: JSON.stringify(result) }
      } catch (err) {
        return { success: false, error: '数据查询失败: ' + err.message }
      }
    }

    default:
      console.warn('[DeepSeek] 未知工具调用:', name)
      return { success: false, error: `未知工具: ${name}` }
  }
}

async function queryDatabase(query) {
  switch (query) {
    case 'production_today': {
      const { data: tasks } = await db.collection('tasks').where({ taskType: 'PRODUCTION' }).get()
      const totalPlan = tasks.reduce((s, t) => s + (t.planQty || 0), 0)
      const totalDone = tasks.reduce((s, t) => s + (t.completedQty || 0), 0)
      return {
        计划总量: totalPlan, 已完成: totalDone,
        达成率: totalPlan > 0 ? Math.round(totalDone / totalPlan * 100) + '%' : '0%',
        待处理: tasks.filter(t => t.status === 'PENDING').length,
        进行中: tasks.filter(t => t.status === 'PROCESSING').length,
        已完成工序: tasks.filter(t => t.status === 'COMPLETED').length
      }
    }
    case 'quality_rate': {
      const { data: records } = await db.collection('quality_records').limit(100).get()
      const total = records.length
      const pass = records.filter(r => r.result === 'QUALIFIED' || r.action === 'RELEASE').length
      return { 检验批次: total, 合格: pass, 不合格: total - pass, 合格率: total > 0 ? Math.round(pass / total * 100) + '%' : '暂无数据' }
    }
    case 'inventory_summary': {
      const { data: items } = await db.collection('inventory').where({ status: 'COMPLETED' }).limit(50).get()
      const map = {}; items.forEach(i => { const k = i.materialName || '未知'; map[k] = (map[k] || 0) + (i.registeredQty || i.qty || 0) })
      return { 物料种类: Object.keys(map).length, 库存: Object.entries(map).slice(0, 5).map(([n, q]) => ({ 物料: n, 数量: q })) }
    }
    case 'device_status': {
      const { data: devs } = await db.collection('devices').get()
      return {
        总设备: devs.length, 运行中: devs.filter(d => d.status === 'RUNNING').length,
        故障维修: devs.filter(d => d.status === 'FAULT' || d.status === 'REPAIRING').length,
        超期未检: devs.filter(d => d.status === 'OVERDUE_MAINT').length
      }
    }
    case 'abnormal_count': {
      const { total: a } = await db.collection('andon_events').where({ status: _.in(['INITIATED', 'RESPONDED', 'PROCESSING']) }).count()
      const { total: f } = await db.collection('devices').where({ status: _.in(['FAULT', 'REPAIRING']) }).count()
      const { total: z } = await db.collection('tasks').where({ frozen: true }).count()
      return { 活跃安灯: a, 故障设备: f, 冻结工单: z, 异常总计: a + f + z }
    }
    case 'schedule_progress': {
      const { data: schs } = await db.collection('schedules').orderBy('createTime', 'desc').limit(5).get()
      return schs.map(s => ({ 产品: s.productName, 产线: s.lineName, 完成: s.completedQty + '/' + s.planQty, 状态: s.statusLabel }))
    }
    default: return { error: '未知查询' }
  }
}

exports.main = async (event, context) => {
  const { messages } = event

  const apiKey = process.env.DEEPSEEK_API_KEY
  if (!apiKey) {
    console.error('[DeepSeek] API Key 未配置')
    return { code: -1, error: 'DeepSeek API Key 未配置' }
  }

  if (!messages || !Array.isArray(messages) || messages.length === 0) {
    return { code: -1, error: '消息不能为空' }
  }

  const requestMessages = [
    { role: 'system', content: REACT_SYSTEM_PROMPT },
    ...messages
  ]

  console.log('[DeepSeek] 初始消息条数:', requestMessages.length)

  console.log('[DeepSeek] tool_choice = auto（由模型自主决策）')

  let loopCount = 0

  try {
    while (loopCount < MAX_TOOL_LOOPS) {
      loopCount++
      console.log(`[DeepSeek] === 第 ${loopCount}/${MAX_TOOL_LOOPS} 轮调用 ===`)

      const response = await axios({
        method: 'POST',
        url: DEEPSEEK_API_URL,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`
        },
        data: {
          model: MODEL,
          messages: requestMessages,
          temperature: TEMPERATURE,
          max_tokens: MAX_TOKENS,
          stream: false,
          tools: TOOLS,
          tool_choice: 'auto'
        },
        timeout: TIMEOUT_MS
      })

      const assistantMessage = response.data?.choices?.[0]?.message
      if (!assistantMessage) {
        console.error('[DeepSeek] 响应无 message 内容')
        return { code: -1, error: 'AI 响应格式异常' }
      }

      const textContent = assistantMessage.content || ''
      console.log('[DeepSeek] 模型回复 content:', textContent.substring(0, 150) || '(无文本)')

      const toolCalls = assistantMessage.tool_calls
      if (!toolCalls || toolCalls.length === 0) {

        console.log('[DeepSeek] 模型决策：不调用工具 → 返回纯文本回复，action: null')
        return {
          code: 0,
          data: { reply: textContent, action: null }
        }
      }

      console.log(`[DeepSeek] ★ 检测到 ${toolCalls.length} 个 tool_calls:`)
      toolCalls.forEach((tc, i) => {
        console.log(`[DeepSeek]   [${i}] ${tc.function.name}(${tc.function.arguments})`)
      })

      requestMessages.push(assistantMessage)

      for (const tc of toolCalls) {
        const toolResult = await executeToolCall(tc)
        console.log('[DeepSeek] 工具执行结果 message:', toolResult.message)

        if (toolResult.action) {
          const pageName = PAGE_NAMES[toolResult.action.page] || toolResult.action.page
          let reply = assistantMessage.content || ''
          if (!reply || reply.trim().length === 0) {
            if (toolResult.action.type === 'fillForm') {
              reply = `好的，已为您打开${pageName}页面，相关信息已自动填入，请核对后手动提交。`
            } else {
              reply = `好的，正在为您跳转到${pageName}页面。`
            }
          }
          console.log('[DeepSeek] ★ 最终 action:', JSON.stringify(toolResult.action))
          return { code: 0, data: { reply, action: toolResult.action } }
        }

        requestMessages.push({
          role: 'tool',
          tool_call_id: tc.id,
          content: toolResult.success ? toolResult.message : JSON.stringify(toolResult)
        })
      }
    }

    console.log('[DeepSeek] 达到最大循环次数，无有效 action，返回兜底回复')
    return {
      code: 0,
      data: {
        reply: '抱歉，操作未能完成，请重新描述您的需求。',
        action: null
      }
    }
  } catch (err) {

    let errorMsg = ''

    if (err.response) {
      const status = err.response.status
      const apiError = err.response.data?.error?.message || ''
      errorMsg = `HTTP ${status}: ${apiError}`
      console.error('[DeepSeek] API 错误:', status, apiError)
    } else if (err.code === 'ECONNABORTED') {
      errorMsg = `请求超时（${TIMEOUT_MS / 1000}s）`
      console.error('[DeepSeek] 请求超时')
    } else if (err.code === 'ENOTFOUND' || err.code === 'ECONNREFUSED') {
      errorMsg = '无法连接到 DeepSeek API 服务器'
      console.error('[DeepSeek] 网络不可达:', err.code)
    } else {
      errorMsg = err.message || '未知错误'
      console.error('[DeepSeek] 请求异常:', err.message)
    }

    return { code: -1, error: errorMsg }
  }
}
