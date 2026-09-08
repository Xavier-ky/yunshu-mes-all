import { login } from '../../services/auth'
import { setToken, setUserInfo } from '../../utils/storage'
import { PAGE_PATH_MAP } from '../../utils/constants'

const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onLoad() {

    if (app.isLoggedIn()) {
      setTimeout(() => {
        wx.reLaunch({ url: PAGE_PATH_MAP.home })
      }, 100)
    }
  },

  onUsernameInput(e) {
    this.setData({ username: e.detail.value })
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  onLogin() {
    const { username, password } = this.data
    if (!username.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    this.setData({ loading: true })

    login(username, password)
      .then(res => {
        this.setData({ loading: false })
        if (res.code === 0) {
          const { token, ...userInfo } = res.data
          setToken(token)
          setUserInfo(userInfo)
          app.setUserInfo(userInfo)
          app.setToken(token)
          wx.showToast({ title: '登录成功', icon: 'success' })
          setTimeout(() => {
            wx.switchTab({ url: PAGE_PATH_MAP.home })
          }, 1000)
        } else {
          wx.showToast({ title: res.message || '登录失败', icon: 'none' })
        }
      })
      .catch((err) => {
        this.setData({ loading: false })
        console.error('登录失败:', err)
        const msg = (err && err.message) || '登录失败'

        if (msg !== '网络异常，请重试') {
          wx.showToast({ title: msg, icon: 'none' })
        }
      })
  }
})