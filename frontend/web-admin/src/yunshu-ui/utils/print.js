import { ElMessage as Message } from "element-plus";
import {getDefaultPrinter} from "@/yunshu-ui/api/print/printerconfig";

export { PRINT_DICT } from "./print-dict";

/**
 * 直接打印（使用Hiprint客户端打印）
 * @param printTemplate 打印模板
 * @param  printData 打印数据
 * @param  hiprintTemplate hiprint模板
 */
export function print(printTemplate, printData, hiprintTemplate, hiprintThis) {
  if (hiprintTemplate) {
    try {
      // 将打印模板封装
      hiprintTemplate.update(JSON.parse(printTemplate));
      // 根据配置好的客户端查询相应的打印机
      const client = JSON.parse(localStorage.getItem("defaultClient"))
      const clients = Object.entries(hiprintThis.hiwebSocket.clients)
      let clientName = ""
      clients.forEach(item => {
        if (client.clientIp == item[1].ip) {
          clientName = item[0]
        }
      })
      if (clientName) {
        getDefaultPrinter(client.clientId).then(res => {
          if (res.code == 200) {
            hiprintTemplate.print2(printData, {client: clientName, printer: res.data.printerName, title: 'hiprint测试打印'});
            Message.success("打印成功")
          }
        }).catch(err => {
        })
      } else {
        Message.error('客户端未连接,无法直接打印')
      }
    } catch (e) {
      Message.error(`更新失败: ${e}`)
    }
  }
}
