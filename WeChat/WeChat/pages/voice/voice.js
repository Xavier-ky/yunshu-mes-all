

const recorderManager = wx.getRecorderManager()
const fileSystemManager = wx.getFileSystemManager()

const MAX_RECORD_DURATION = 60000

Page({
  data: {
    isRecording: false,
    loading: false,
    resultText: '',
    errorMsg: '',
    statusHint: '按住下方按钮开始说话',
    showPermGuide: false
  },

  _recorderReady: false,

  onLoad() {
    this.initRecorder()
  },

  initRecorder() {
    if (this._recorderReady) return

    recorderManager.onStart(() => {
      console.log('[Voice] 录音开始')
      this.setData({ isRecording: true, errorMsg: '', resultText: '', statusHint: '正在聆听...' })
    })

    recorderManager.onStop((res) => {
      console.log('[Voice] 录音结束:', res.duration, 'ms, 大小:', res.fileSize)
      this.setData({ isRecording: false })

      if (res.duration < 500) {
        this.setData({ errorMsg: '说话时间太短，请重试', statusHint: '按住下方按钮开始说话' })
        return
      }

      this.readAudioAndRecognize(res.tempFilePath, res.fileSize)
    })

    recorderManager.onError((err) => {
      console.error('[Voice] 录音错误:', err)
      this.setData({ isRecording: false, statusHint: '按住下方按钮开始说话' })
      const errMsg = err.errMsg || ''
      if (errMsg.includes('auth') || errMsg.includes('permission')) {
        this.setData({ errorMsg: '未获得录音权限', showPermGuide: true })
      } else {
        this.setData({ errorMsg: '录音失败，请重试' })
      }
    })

    recorderManager.onFrameRecorded((res) => {

    })

    this._recorderReady = true
  },

  onTouchStart() {
    if (this.data.isRecording || this.data.loading) return
    this.setData({ errorMsg: '', resultText: '', showPermGuide: false })

    wx.getSetting({
      success: (res) => {
        if (res.authSetting['scope.record'] === false) {
          this.setData({ showPermGuide: true, errorMsg: '请在设置中允许麦克风权限' })
          return
        }
        this.doStartRecord()
      },
      fail: () => {
        this.doStartRecord()
      }
    })
  },

  doStartRecord() {
    recorderManager.start({
      duration: MAX_RECORD_DURATION,
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 48000,
      format: 'mp3',
      frameSize: 50
    })
  },

  onTouchEnd() {
    if (!this.data.isRecording) return
    recorderManager.stop()
    this.setData({ statusHint: '识别中...' })
  },

  onTouchCancel() {
    if (!this.data.isRecording) return

    this.setData({ isRecording: false, statusHint: '已取消' })
    recorderManager.stop()
  },

  startRecord() {
    this.setData({ resultText: '', errorMsg: '', statusHint: '按住下方按钮开始说话' })
  },

  readAudioAndRecognize(filePath, fileSize) {
    this.setData({ loading: true, errorMsg: '' })

    if (fileSize > 1024 * 1024) {
      this.setData({
        loading: false,
        errorMsg: '录音文件过大（超过1MB），请缩短录音时间',
        statusHint: '按住下方按钮开始说话'
      })
      return
    }

    fileSystemManager.readFile({
      filePath,
      encoding: 'base64',
      success: (readRes) => {
        const audioBase64 = readRes.data
        console.log('[Voice] Base64 长度:', audioBase64.length)
        this.callVoiceRecognize(audioBase64, fileSize)
      },
      fail: (err) => {
        console.error('[Voice] 读取文件失败:', err)
        this.setData({
          loading: false,
          errorMsg: '读取录音文件失败',
          statusHint: '按住下方按钮开始说话'
        })
      }
    })
  },

  callVoiceRecognize(audioBase64, audioLength) {
    wx.cloud.callFunction({
      name: 'voiceRecognize',
      data: {
        audioBase64,
        audioLength
      }
    }).then((res) => {
      console.log('[Voice] 云函数返回:', JSON.stringify(res))
      const result = (res && res.result) ? res.result : res

      this.setData({ loading: false })

      if (result.code === 0 && result.data) {
        const text = result.data
        this.setData({
          resultText: text,
          statusHint: '识别完成',
          errorMsg: ''
        })

        this.sendToAI(text)
      } else if (result.code === 0 && !result.data) {
        this.setData({
          errorMsg: '未识别到语音内容，请重试',
          statusHint: '按住下方按钮开始说话'
        })
      } else {
        this.setData({
          errorMsg: result.message || '识别失败',
          statusHint: '按住下方按钮开始说话'
        })
      }
    }).catch((err) => {
      console.error('[Voice] 云函数调用失败:', err)
      this.setData({
        loading: false,
        errorMsg: '云函数调用失败，请检查是否已部署 voiceRecognize',
        statusHint: '按住下方按钮开始说话'
      })
    })
  },

  sendToAI(text) {
    if (!text || !text.trim()) return

    try {

      wx.navigateTo({
        url: `/pages/ai/chat/index?voiceText=${encodeURIComponent(text.trim())}`
      })
    } catch (e) {
      console.warn('[Voice] 跳转 AI 页面失败:', e)
    }
  },

  openSetting() {
    wx.openSetting({
      success: (res) => {
        if (res.authSetting['scope.record']) {
          this.setData({ showPermGuide: false, errorMsg: '' })
          wx.showToast({ title: '权限已开启', icon: 'success' })
        }
      }
    })
  }
})
