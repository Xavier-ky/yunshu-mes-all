import { guardPageAccess } from '../../utils/permission'
import { PAGE_PATH_MAP } from '../../utils/constants'

Page({
  data: {
    identifyResult: null
  },

  onShow() {
  },

  onScanResult(e) {
    const { code, type, name, page } = e.detail

    if (type === 'unknown') {
      wx.showToast({ title: '无法识别该编码类型', icon: 'none' })
      return
    }

    this.setData({ identifyResult: { code, type, name, page } })

    if (page && guardPageAccess(page)) {
      wx.navigateTo({ url: `${page}?code=${code}` })
    }
  },

  onIdentify(e) {
    const { code, type, name, page } = e.detail
    this.setData({ identifyResult: { code, type, name, page } })
  },

  onGoToPage() {
    const { identifyResult } = this.data
    if (identifyResult && identifyResult.page) {
      if (guardPageAccess(identifyResult.page)) {
        wx.navigateTo({ url: `${identifyResult.page}?code=${identifyResult.code}` })
      }
    }
  }
})
