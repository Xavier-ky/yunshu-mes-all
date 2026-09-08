import modal from '@/yunshu-ui/plugins/modal'

/** Jasper 未对接：降级为浏览器打印，并 reject 避免按 PDF blob 打开 */
export function getReport() {
  return degradePrint()
}

export function getReport2() {
  return degradePrint()
}

export function previewPrint() {
  return degradePrint()
}

function degradePrint() {
  modal.msgWarning('PDF 报表引擎未对接，已改为浏览器打印当前页面')
  setTimeout(() => window.print(), 300)
  return Promise.reject(new Error('PRINT_FALLBACK'))
}
