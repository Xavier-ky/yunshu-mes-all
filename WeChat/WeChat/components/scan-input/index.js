import { SCAN_PREFIX_MAP } from '../../utils/constants'

Component({
  properties: {
    placeholder: { type: String, value: '扫描或输入编码' },
    value: { type: String, value: '' },
    disabled: { type: Boolean, value: false }
  },

  methods: {
    onScan() {
      wx.scanCode({
        onlyFromCamera: false,
        scanType: ['barCode', 'qrCode'],
        success: (res) => {
          const code = res.result
          const scanResult = this.identifyCode(code)
          this.triggerEvent('scan', { code, type: scanResult.type, name: scanResult.name, page: scanResult.page })
        },
        fail: () => {
          wx.showToast({ title: '扫码失败', icon: 'none' })
        }
      })
    },

    identifyCode(code) {
      for (const prefix of Object.keys(SCAN_PREFIX_MAP)) {
        if (code.startsWith(prefix)) {
          return SCAN_PREFIX_MAP[prefix]
        }
      }
      return { type: 'unknown', name: '未知类型', page: '' }
    },

    onInput(e) {
      const value = e.detail.value
      this.triggerEvent('input', { value })
      if (value.length >= 4) {
        const scanResult = this.identifyCode(value)
        this.triggerEvent('identify', { code: value, ...scanResult })
      }
    },

    onConfirm(e) {
      const value = e.detail.value
      if (value) {
        const scanResult = this.identifyCode(value)
        this.triggerEvent('scan', { code: value, type: scanResult.type, name: scanResult.name, page: scanResult.page })
      }
    }
  }
})