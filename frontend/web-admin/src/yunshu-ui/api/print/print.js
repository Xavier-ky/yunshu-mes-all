import request from '@/yunshu-ui/api/request'


// 调用赋码打印接口
/**
 * 参数格式：
 * 
 * @param {*} data 
 * @returns 
 */
export function printBarcodeLabel(data) {
    return request({
      url: '/print/barcodePrint/printing',
      method: 'post',
      data: data
    })
  }
