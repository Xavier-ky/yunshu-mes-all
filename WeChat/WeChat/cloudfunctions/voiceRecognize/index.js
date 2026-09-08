

const tencentcloud = require('tencentcloud-sdk-nodejs')

const SECRET_ID  = process.env.TENCENT_SECRET_ID
const SECRET_KEY = process.env.TENCENT_SECRET_KEY

if (!SECRET_ID || !SECRET_KEY) {
  console.error('[voiceRecognize] 缺少环境变量 TENCENT_SECRET_ID / TENCENT_SECRET_KEY')
}

let client = null
function getClient() {
  if (client) return client
  if (!SECRET_ID || !SECRET_KEY) {
    throw new Error('云函数未配置腾讯云密钥，请在云函数控制台设置环境变量 TENCENT_SECRET_ID 和 TENCENT_SECRET_KEY')
  }

  const AsrClient = tencentcloud.asr.v20190614.Client

  const clientConfig = {
    credential: {
      secretId:  SECRET_ID,
      secretKey: SECRET_KEY
    },
    region: 'ap-guangzhou',
    profile: {
      httpProfile: {
        endpoint: 'asr.tencentcloudapi.com',
        reqTimeout: 8
      }
    }
  }

  client = new AsrClient(clientConfig)
  return client
}

exports.main = async (event, context) => {
  const { audioBase64, audioLength } = event

  if (!audioBase64) {
    console.warn('[voiceRecognize] 缺少 audioBase64')
    return { code: -1, message: '缺少音频数据' }
  }

  if (audioLength && audioLength > 1024 * 1024) {
    console.warn('[voiceRecognize] 音频过大:', audioLength)
    return { code: -1, message: '音频大小不能超过 1MB' }
  }

  const estimatedSize = (audioBase64.length * 3) / 4
  if (estimatedSize > 3 * 1024 * 1024) {
    console.warn('[voiceRecognize] 音频过大（估算）:', Math.round(estimatedSize / 1024), 'KB')
    return { code: -1, message: '音频大小不能超过 1MB' }
  }

  try {
    const asrClient = getClient()

    const params = {
      EngSerViceType: '16k_zh',
      SourceType: 1,
      VoiceFormat: 'mp3',
      Data: audioBase64,
      DataLen: audioLength || Math.round(estimatedSize)
    }

    console.log('[voiceRecognize] 开始识别, 音频大小约:', Math.round(estimatedSize / 1024), 'KB')

    const response = await asrClient.SentenceRecognition(params)

    console.log('[voiceRecognize] SDK 原始响应:', JSON.stringify(response))

    if (response && response.Result) {
      console.log('[voiceRecognize] 识别成功:', response.Result)
      return {
        code: 0,
        data: response.Result,
        requestId: response.RequestId || ''
      }
    }

    console.warn('[voiceRecognize] 返回为空')
    return {
      code: 0,
      data: '',
      message: '未识别到语音内容'
    }

  } catch (err) {
    console.error('[voiceRecognize] 调用异常:', err)

    let message = '语音识别失败'
    if (err.code) {

      const errorMap = {
        'InvalidParameterValue.ErrorVoicedataTooLong': '音频时长超过 60 秒限制',
        'InvalidParameterValue.ErrorInvalidVoiceFormat': '音频格式不支持',
        'InvalidParameterValue.ErrorInvalidVoiceData': '音频数据无效',
        'AuthFailure.InvalidSecretId': '云函数密钥配置错误',
        'AuthFailure.SignatureFailure': '云函数密钥配置错误',
        'AuthFailure.TokenFailure': '云函数密钥配置错误',
        'LimitExceeded': '调用频率超限',
        'InternalError': '腾讯云服务内部错误'
      }
      message = errorMap[err.code] || `识别失败 (${err.code})`
    } else if (err.message) {
      message = err.message
    }

    return { code: -1, message }
  }
}
