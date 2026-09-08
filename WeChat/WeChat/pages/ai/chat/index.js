

import { sendMessage, voiceToText } from '../../../services/ai'
import { getCurrentUser } from '../../../utils/auth'
import { setPrefillData } from '../../../utils/ai-prefill'

const recorderManager = wx.getRecorderManager()
const fileSystemManager = wx.getFileSystemManager()

const TAB_BAR_PAGES = new Set([
  '/pages/home/index',
  '/pages/message/index',
  '/pages/ai/chat/index',
  '/pages/mine/index'
])

let inputTimer = null
const INPUT_DEBOUNCE = 80

Page({
  data: {
    messages: [],
    inputText: '',
    isLoading: false,
    showKeyboard: true,
    isRecording: false,
    lastMsgId: '',
    scrollToView: 'msg-bottom',
    isIphoneX: false,
    headerHeightPx: 64,
    debugLog: [],

    showScrollFab: false,

    errorMsg: '',

    lastRequestText: '',

    inputFocus: false,
    quickTips: [
      '今天产量多少？',
      '当前合格率怎么样',
      '查看设备状态',
      '电机有异响，帮我报修',
      '打开生产任务',
      '最近有什么异常',
      '帮我发起安灯',
      '仓库库存情况'
    ]
  },

  getStorageKey() {
    const user = getCurrentUser()
    const uid = (user && user.userId) ? user.userId : (user && user.realName) || 'anonymous'
    return `ai_chat_history_${uid}`
  },

  onLoad(options) {
    console.log('[AI] onLoad')
    try {
      this.calcHeaderHeight()
      this.checkSafeArea()
      this.loadHistory()
      this.initRecorder()
      this.diagnose()
    } catch (e) {
      console.error('[AI] onLoad 异常:', e)
    }

    if (options && options.voiceText) {
      const text = decodeURIComponent(options.voiceText)
      console.log('[AI] 语音识别带入文字:', text)
      setTimeout(() => {
        this.doAddMessage('user', text)
        this.requestAI(text)
      }, 500)
    }
  },

  onReady() {

    setTimeout(() => {
      this.setData({ inputFocus: true })
    }, 400)
  },

  onShow() {
    console.log('[AI] onShow')
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3 })
    }

    try {
      const key = this.getStorageKey()
      const raw = wx.getStorageSync(key)
      const stored = Array.isArray(raw) ? raw : []
      const memory = Array.isArray(this.data.messages) ? this.data.messages : []
      if (stored.length >= memory.length) {
        if (stored.length > 0 && JSON.stringify(stored) !== JSON.stringify(memory)) {
          this.setData({ messages: stored }, () => {
            if (stored.length > 0) this.scrollToBottom()
          })
          console.log('[AI] onShow 从存储恢复, 条数:', stored.length)
        }
      } else if (memory.length > stored.length) {
        this.saveHistory()
        console.log('[AI] onShow 内存数据更新, 补保存, 条数:', memory.length)
      }
    } catch (e) {
      console.warn('[AI] onShow 同步异常:', e)
    }
  },

  onHide() {
    console.log('[AI] onHide, 保存历史')
    this.saveHistory()
  },

  onUnload() {
    console.log('[AI] onUnload, 最终保存')
    this.saveHistory()
  },

  scrollToBottom() {
    this.setData({ scrollToView: '' }, () => {
      this.setData({ scrollToView: 'msg-bottom' })
    })
  },

  diagnose() {
    console.log('[AI] ===== 自检 =====')
    console.log('[AI] messages 类型:', Array.isArray(this.data.messages) ? '数组' : typeof this.data.messages)
    console.log('[AI] sendMessage 可用:', typeof sendMessage === 'function')
    console.log('[AI] voiceToText 可用:', typeof voiceToText === 'function')
    console.log('[AI] ===== 自检结束 =====')
  },

  calcHeaderHeight() {
    try {
      const sysInfo = wx.getSystemInfoSync()
      const statusBarHeight = sysInfo.statusBarHeight || 20
      const navBarHeight = 44
      const tabBarHeight = 55
      const headerHeightPx = statusBarHeight + navBarHeight + tabBarHeight
      this.setData({ headerHeightPx })
      console.log('[AI] headerHeight:', headerHeightPx)
    } catch (e) {
      console.warn('[AI] calcHeaderHeight 失败:', e)
    }
  },

  checkSafeArea() {
    try {
      const sysInfo = wx.getSystemInfoSync()
      const isIphoneX = sysInfo.safeArea?.top >= 44 && sysInfo.platform === 'ios'
      this.setData({ isIphoneX })
    } catch (e) {
      console.warn('[AI] checkSafeArea 失败:', e)
    }
  },

  loadHistory() {
    try {
      try { wx.removeStorageSync('ai_chat_history') } catch (_) {}
      const key = this.getStorageKey()
      const raw = wx.getStorageSync(key)
      const history = Array.isArray(raw) ? raw : []
      this.setData({ messages: history }, () => {
        if (history.length > 0) this.scrollToBottom()
      })
      console.log('[AI] 加载历史, key:', key, '条数:', history.length)
    } catch (e) {
      console.warn('[AI] 加载历史失败:', e)
    }
  },

  saveHistory() {
    try {
      const msgs = this.data.messages
      if (!Array.isArray(msgs) || msgs.length === 0) return
      const saved = msgs.slice(-50)
      const key = this.getStorageKey()
      wx.setStorageSync(key, saved)
      console.log('[AI] 保存历史, 条数:', saved.length)
    } catch (e) {
      console.warn('[AI] 保存历史失败:', e)
    }
  },

  onClearHistory() {
    wx.showModal({
      title: '清空对话',
      content: '确定要清空当前所有对话记录吗？此操作不可撤销。',
      confirmText: '清空',
      confirmColor: '#FF4757',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            messages: [],
            errorMsg: '',
            showScrollFab: false
          }, () => {
            const key = this.getStorageKey()
            try { wx.removeStorageSync(key) } catch (_) {}
            wx.showToast({ title: '对话已清空', icon: 'success', duration: 1500 })
            console.log('[AI] 对话已清空')
          })
        }
      }
    })
  },

  onInput(e) {
    if (inputTimer) clearTimeout(inputTimer)
    inputTimer = setTimeout(() => {
      this.setData({ inputText: e.detail.value })
    }, INPUT_DEBOUNCE)
  },

  onSend(e) {
    const text = (e && e.detail && e.detail.value)
      ? e.detail.value.trim()
      : this.data.inputText.trim()
    if (!text || this.data.isLoading) {
      console.log('[AI] 发送拦截: text=%s, isLoading=%s', text, this.data.isLoading)
      return
    }
    console.log('[AI] 发送消息:', text)

    this.setData({ errorMsg: '', lastRequestText: '' })

    this.doAddMessage('user', text)
    this.setData({ inputText: '', showScrollFab: false })
    this.requestAI(text)
  },

  onTipTap(e) {
    const text = e.currentTarget.dataset.text
    if (!text || this.data.isLoading) return
    console.log('[AI] 快捷提示:', text)

    this.setData({ errorMsg: '', lastRequestText: '' })
    this.doAddMessage('user', text)
    this.requestAI(text)
  },

  doAddMessage(role, content) {
    const safeContent = content == null ? '' : String(content)
    const now = new Date()
    const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    const msg = { id: Date.now() + Math.random(), role, content: safeContent, time }

    const current = Array.isArray(this.data.messages) ? this.data.messages : []
    const newList = [...current, msg]

    console.log('[AI] doAddMessage:', role, '总条数:', newList.length)

    this.setData({
      messages: newList,
      lastMsgId: `msg-${msg.id}`
    }, () => {
      this.saveHistory()
      this.scrollToBottom()
    })
    return msg
  },

  async requestAI(text) {
    this.setData({ isLoading: true, lastRequestText: text })
    console.log('[AI] requestAI 开始, text:', text)

    try {
      const current = Array.isArray(this.data.messages) ? this.data.messages : []
      const history = current.map(m => ({ role: m.role, content: m.content }))
      console.log('[AI] 历史消息条数:', history.length)

      const result = await sendMessage(history, text)
      console.log('[AI] 收到回复:', result)

      if (result && result.reply) {
        this.doAddMessage('assistant', result.reply)
      } else {
        console.warn('[AI] 回复为空')
        this.doAddMessage('assistant', '抱歉，暂时无法获取回复，请稍后再试。')
      }

      if (result && result.action) {
        this.executeAction(result.action)
      }
    } catch (err) {
      console.error('[AI] requestAI 异常:', err)
      this.setData({
        errorMsg: '网络异常，请检查连接后重试'
      })
    } finally {
      this.setData({ isLoading: false })
      console.log('[AI] requestAI 结束')
    }
  },

  onRetry() {
    const text = this.data.lastRequestText
    if (!text) {
      this.setData({ errorMsg: '' })
      return
    }
    console.log('[AI] 重试请求:', text)
    this.setData({ errorMsg: '' })
    this.requestAI(text)
  },

  executeAction(action) {
    const { type, page, data } = action
    console.log('[AI] 执行动作:', type, page)

    if (type === 'fillForm' && data) {
      setPrefillData(page, data)
      console.log('[AI] 已存储预填数据，准备跳转到:', page)
    }

    setTimeout(() => {
      this.navigateToPage(page)
    }, 1200)
  },

  navigateToPage(page) {
    if (!page) return
    if (TAB_BAR_PAGES.has(page)) {
      console.log('[AI] switchTab:', page)
      wx.switchTab({ url: page })
    } else {
      console.log('[AI] navigateTo:', page)
      wx.navigateTo({ url: page })
    }
  },

  onCopyMsg(e) {
    const content = e.currentTarget.dataset.content
    if (!content) return
    wx.setClipboardData({
      data: content,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success', duration: 1200 })
      },
      fail: () => {
        wx.showToast({ title: '复制失败', icon: 'none', duration: 1200 })
      }
    })
  },

  onScroll(e) {
    if (!e || !e.detail) return
    const { scrollTop, scrollHeight } = e.detail

    if (!this._scrollViewHeight) {
      const query = wx.createSelectorQuery()
      query.select('.chat-list').boundingClientRect((rect) => {
        if (rect) this._scrollViewHeight = rect.height
      }).exec()
    }
    const viewHeight = this._scrollViewHeight || 500
    const distanceToBottom = scrollHeight - scrollTop - viewHeight

    const showScrollFab = distanceToBottom > 200
    if (showScrollFab !== this.data.showScrollFab) {
      this.setData({ showScrollFab })
    }
  },

  initRecorder() {
    if (this._recorderInited) return
    this._recorderInited = true

    recorderManager.onStop((res) => {
      console.log('[AI] 录音结束:', res.duration, 'ms, 大小:', res.fileSize)
      this.setData({ isRecording: false })
      wx.hideLoading()

      if (res.duration < 500) {
        wx.showToast({ title: '说话时间太短', icon: 'none' })
        return
      }

      fileSystemManager.readFile({
        filePath: res.tempFilePath,
        encoding: 'base64',
        success: (readRes) => {
          console.log('[AI] Base64 读取成功, 长度:', readRes.data.length)
          voiceToText(readRes.data, res.fileSize).then((text) => {
            if (text && text.trim()) {
              console.log('[AI] 语音识别结果:', text)
              this.setData({ errorMsg: '', lastRequestText: '' })
              this.doAddMessage('user', text)
              this.requestAI(text)
            } else {
              wx.showToast({ title: '未识别到语音内容', icon: 'none' })
            }
          }).catch((err) => {
            console.error('[AI] voiceToText 失败:', err)

            if (err && err.message) {
              wx.showToast({ title: err.message, icon: 'none', duration: 2500 })
            }
          })
        },
        fail: (err) => {
          console.error('[AI] 读取录音文件失败:', err)
          wx.showToast({ title: '读取录音文件失败', icon: 'none' })
        }
      })
    })

    recorderManager.onError((err) => {
      console.error('[AI] 录音器错误:', JSON.stringify(err))
      this.setData({ isRecording: false })
      wx.hideLoading()
      if (err.errMsg && err.errMsg.includes('auth')) {
        wx.showToast({ title: '未获得录音权限，请在设置中开启', icon: 'none' })
      } else {
        wx.showToast({ title: '录音失败: ' + (err.errMsg || '未知错误'), icon: 'none', duration: 2000 })
      }
    })
  },

  onSwitchVoice() {
    this.setData({ showKeyboard: false })
  },

  onSwitchKeyboard() {
    this.setData({ showKeyboard: true, inputFocus: true })
  },

  onVoiceStart() {
    if (this.data.isRecording || this.data.isLoading) return

    const doStart = () => {
      this.setData({ isRecording: true })
      recorderManager.start({
        duration: 60000,
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 48000,
        format: 'mp3',
        frameSize: 50
      })
    }

    wx.getSetting({
      success: (res) => {
        if (res.authSetting['scope.record'] === false) {
          wx.showModal({
            title: '需要录音权限',
            content: '请在设置中允许麦克风权限',
            confirmText: '去设置',
            success: (m) => { if (m.confirm) wx.openSetting() }
          })
          return
        }

        wx.authorize({ scope: 'scope.record' }).then(doStart).catch(doStart)
      },
      fail: doStart
    })
  },

  onVoiceEnd() {
    if (!this.data.isRecording) return
    wx.showLoading({ title: '识别中...' })
    recorderManager.stop()

    const t = setInterval(() => {
      if (!this.data.isRecording) { wx.hideLoading(); clearInterval(t) }
    }, 200)
  }
})
