

const SYSTEM_PROMPT = `你是云枢智造 MES 系统的智能助手（ReAct Agent），具备三种核心能力：

1. **智能页面跳转**：当用户说"打开生产任务""查看设备状态""去扫码页面"等，调用 open_page 工具跳转。
2. **智能表单预填**：当用户描述异常需要填报（如"电机坏了，帮我填报修""产线A缺料，发起安灯"），调用 fill_form 工具提取关键信息并跳转到表单页面，预填数据但**不自动提交**，由人工确认。
3. **实时数据查询**：当用户询问产量、合格率、库存、设备状态、异常数量、排产进度等数据类问题，**必须调用 query_data 工具获取真实数据库数据**，禁止凭空编造数字。

重要规则：
- 仅当用户明确表达意图时才调用对应工具
- 数据查询类问题必须调用 query_data，拿到真实数据后再用口语化方式回复
- fill_form 只提取用户明确提到的信息，未提及的字段不填
- 回复保持简洁专业，适合车间现场人员阅读

当前支持的表单预填：
- /pages/equipment/repair/index（设备报修）：deviceCode, deviceName, faultType, faultDescription
- /pages/andon/create/index（发起安灯）：deviceCode, workOrderNo, lineCode, description, eventType, reasonCode
- /pages/quality/defect/index（缺陷登记）：defectType, defectQty, description

当前支持的数据查询：
- 产量/达成率（production_today）
- 合格率（quality_rate）
- 库存情况（inventory_summary）
- 设备状态（device_status）
- 异常数量（abnormal_count）
- 排产进度（schedule_progress）`

export async function sendMessage(messages, newMessage) {

  if (newMessage && typeof wx.msgSecCheck === 'function') {
    try {
      const checkResult = await wx.msgSecCheck({ content: newMessage })
      if (checkResult.errCode === 87014) {
        wx.showToast({ title: '内容含有敏感信息', icon: 'none' })
        return { reply: null, action: null }
      }
    } catch (e) {
      console.warn('[AI服务] msgSecCheck 调用失败:', e)
    }
  }

  console.log('[AI服务] 调用云函数 deepseek-proxy，消息条数:', messages.length)

  let res
  try {
    res = await wx.cloud.callFunction({
      name: 'deepseek-proxy',
      data: {
        messages: messages,
        systemPrompt: SYSTEM_PROMPT
      }
    })
  } catch (callErr) {
    console.error('[AI服务] 云函数调用失败:', callErr)
    wx.showToast({ title: '云函数调用失败，请检查是否已部署', icon: 'none' })
    return {
      reply: '❌ 云函数调用失败。请确认：\n1. 云函数 deepseek-proxy 是否已部署\n2. app.js 中的 env ID 是否正确\n3. 微信开发者工具是否已开启"不校验合法域名"\n\n错误信息：' + (callErr.message || '未知'),
      action: null
    }
  }

  console.log('[AI服务] 云函数返回值:', JSON.stringify(res))

  const result = res && res.result ? res.result : res

  if (!result || result.code !== 0) {
    const errorMsg = (result && result.error) || '云函数返回未知错误'
    console.error('[AI服务] 云函数返回错误:', errorMsg)
    wx.showToast({ title: 'AI 服务错误: ' + errorMsg, icon: 'none' })
    return {
      reply: '❌ AI 服务返回错误：' + errorMsg + '\n\n请检查云函数环境变量 DEEPSEEK_API_KEY 是否已配置。',
      action: null
    }
  }

  const replyContent = result.data?.reply
  let action = result.data?.action || null

  if (action && action.type === 'fillForm' && action.data && typeof action.data === 'string') {
    try {
      console.log('[AI服务] action.data 为字符串，解析为对象:', action.data.substring(0, 200))
      action = { ...action, data: JSON.parse(action.data) }
    } catch (e) {
      console.warn('[AI服务] action.data 字符串 JSON.parse 失败:', e.message)
      action = { ...action, data: {} }
    }
  }

  if (!replyContent && !action) {
    console.error('[AI服务] AI 返回内容为空，完整响应:', JSON.stringify(result.data))
    return {
      reply: '❌ AI 返回内容为空，请检查 DeepSeek API 账户状态和额度。',
      action: null
    }
  }

  const finalReply = replyContent || '好的，正在为您处理。'

  if (!action && replyContent) {
    const parsed = parseLegacyAction(replyContent)
    if (parsed.action) {
      console.log('[AI服务] 从旧版格式解析到 action:', JSON.stringify(parsed.action))
      return { reply: parsed.reply, action: parsed.action }
    }
  }

  return { reply: finalReply, action }
}

function parseLegacyAction(content) {
  if (!content) return { reply: '', action: null }

  const actionRegex = /```action\s*([\s\S]*?)\s*```/
  const match = content.match(actionRegex)

  let action = null
  let reply = content

  if (match) {
    try {
      const parsed = JSON.parse(match[1].trim())
      if (parsed.action === 'navigateTo' && parsed.url) {
        action = { type: 'navigateTo', page: parsed.url }
      }
    } catch (e) {
      console.warn('[AI服务] 解析旧版 action 失败:', e)
    }
    reply = content.replace(actionRegex, '').trim()
  }

  return { reply, action }
}

export function voiceToText(audioBase64, audioLength) {
  return new Promise((resolve, reject) => {
    wx.cloud.callFunction({
      name: 'voiceRecognize',
      data: { audioBase64, audioLength }
    }).then((res) => {
      console.log('[AI服务] voiceRecognize 返回:', JSON.stringify(res).substring(0, 300))
      const result = (res && res.result) ? res.result : res
      if (result && result.code === 0) {
        resolve(result.data || '')
      } else {
        const msg = (result && result.message) || '语音识别失败'
        reject(new Error(msg))
      }
    }).catch((err) => {
      console.error('[AI服务] 云函数调用失败:', err)
      let msg = '语音识别失败'
      if (err.errMsg && err.errMsg.includes('not found')) {
        msg = '云函数 voiceRecognize 未部署，请先上传云函数'
      } else if (err.errMsg && err.errMsg.includes('not init')) {
        msg = '云环境未初始化'
      } else if (err.errMsg) {
        msg = err.errMsg
      }
      reject(new Error(msg))
    })
  })
}
