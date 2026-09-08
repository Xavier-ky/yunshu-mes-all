

const THEME_KEY = 'app_theme'
const THEME_DARK = 'dark'
const THEME_LIGHT = 'light'

let currentTheme = THEME_DARK

export function initTheme() {
  const saved = wx.getStorageSync(THEME_KEY)
  if (saved === THEME_LIGHT || saved === THEME_DARK) {
    currentTheme = saved
  }
  applyTheme(currentTheme)
  return currentTheme
}

export function getTheme() {
  return currentTheme
}

export function isDark() {
  return currentTheme === THEME_DARK
}

export function toggleTheme() {
  currentTheme = currentTheme === THEME_DARK ? THEME_LIGHT : THEME_DARK
  wx.setStorageSync(THEME_KEY, currentTheme)
  applyTheme(currentTheme)
  return currentTheme
}

export function setTheme(theme) {
  if (theme === THEME_LIGHT || theme === THEME_DARK) {
    currentTheme = theme
    wx.setStorageSync(THEME_KEY, currentTheme)
    applyTheme(currentTheme)
  }
}

function applyTheme(theme) {
  const isLight = theme === THEME_LIGHT

  wx.setNavigationBarColor({
    frontColor: isLight ? '#000000' : '#ffffff',
    backgroundColor: isLight ? '#f5f5f5' : '#0A1628',
    animation: { duration: 300, timingFunc: 'easeInOut' }
  })

  wx.setBackgroundColor({
    backgroundColor: isLight ? '#f5f5f5' : '#0A1628',
    backgroundColorTop: isLight ? '#f5f5f5' : '#0A1628',
    backgroundColorBottom: isLight ? '#f5f5f5' : '#0A1628'
  })

  try {
    const pages = getCurrentPages()
    pages.forEach(p => {
      if (p.$vm && p.$vm.setData) {
        p.$vm.setData({ theme: currentTheme })
      }
    })
  } catch (e) {}

  try {
    const app = getApp()
    if (app && app.globalData) {
      app.globalData.theme = currentTheme
    }
  } catch (e) {}
}

export function getThemeClass() {
  return 'theme-' + currentTheme
}
