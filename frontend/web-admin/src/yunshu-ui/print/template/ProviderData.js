import {hiprint} from 'vue-plugin-hiprint'

function nowDateStr() {
  if (typeof window !== 'undefined' && typeof window.formatDate === 'function') {
    return window.formatDate(new Date(), 'yyyy-MM-dd HH:mm:ss');
  }
  return new Date().toISOString().slice(0, 19).replace('T', ' ');
}

// 自定义设计元素1
export const aProvider = function (ops) {
  var addElementTypes = function (context) {
    context.removePrintElementTypes("defaultModule");
    context.addPrintElementTypes(
      "defaultModule",
      [
        new hiprint.PrintElementTypeGroup("平台", [
          // 固定文本
          {
            tid: 'defaultModule.text', title: '固定文本', data: '固定文本', type: 'text',
            options: {
              type: "text",
              testData: '',
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 图片
          {tid: 'defaultModule.image', title: '', data: '', type: 'image'},
          // 长文本
          {
            tid: 'defaultModule.longText', title: '长文本', data: '长文本', type: 'longText',
            options: {
              type: "text",
              testData: '',
              height: 12,
              fontSize: 6.75,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 日期时间
          {
            tid: 'defaultModule.date', title: '日期时间', data: nowDateStr(), type: 'text',
            options: {
              type: "date",
              field: 'date',
              testData: nowDateStr(),
              formatter: "yyyy-MM-dd HH:mm:ss",
              height: 16,
              fontSize: 6.75,
              fontWeight: "700",
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 横线
          {
            tid: 'defaultModule.hline',
            title: '横线',
            type: 'hline',
            options: {
              width: 90,
              height: 9
            }
          },
          // 竖线
          {
            tid: 'defaultModule.vline',
            title: '竖线',
            type: 'vline',
            options: {
              width: 9,
              height: 90
            }
          },
          // 矩形
          {
            tid: 'defaultModule.rect',
            title: '矩形',
            type: 'rect',
            options: {
              width: 66,
              height: 42
            }
          },
          // 椭圆
          {
            tid: 'defaultModule.oval',
            title: '椭圆',
            type: 'oval',
            options: {
              width: 60,
              height: 60
            }
          },
          // 条形码
          {
            tid: 'defaultModule.barcode', title: '条形码', data: '', type: 'text',
            options: {
              type: "barcode",
              field: 'barcode',
              testData: 'test',
              height: 32,
              fontSize: 12,
              lineHeight: 18,
              textType: "barcode"
            }
          },
          // 二维码
          {
            tid: 'defaultModule.qrcode', title: '二维码', data: '', type: 'text',
            options: {
              type: "qrcode",
              field: 'barcodeContent',
              testData: 'test',
              height: 40,
              width: 40,
              fontSize: 12,
              lineHeight: 18,
              hideTitle: true,
              textType: "qrcode"
            }
          },
          // 物料编码
          {
            tid: 'defaultModule.itemCode', title: "物料编码", data: "", type: 'text',
            options: {
              testData: "IF2022082437",
              height: 12,
              field: "itemCode",
              hideTitle: true,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 物料名称
          {
            tid: 'defaultModule.itemName', title: "物料名称", data: "", type: 'text',
            options: {
              testData: "色粉【黑色】",
              field: "itemName",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 工单编码
          {
            tid: 'defaultModule.workorderCode', title: "工单编码", data: "", type: 'text',
            options: {
              testData: "MO202503180014",
              field: "workorderCode",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 工单名称
          {
            tid: 'defaultModule.workorderName', title: "工单名称", data: "", type: 'text',
            options: {
              testData: "螺丝刀刀头【10000】PCS",
              field: "workorderName",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 供应商编码
          {
            tid: 'defaultModule.vendorCode', title: "供应商编码", data: "", type: 'text',
            options: {
              testData: "V00009",
              field: "vendorCode",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 供应商名称
          {
            tid: 'defaultModule.vendorName', title: "供应商名称", data: "", type: 'text',
            options: {
              testData: "江苏南通塑有为塑料制品有限公司",
              field: "vendorName",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 客户编码
          {
            tid: 'defaultModule.clientCode', title: "客户编码", data: "", type: 'text',
            options: {
              testData: "C00184",
              field: "clientCode",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
          // 客户名称
          {
            tid: 'defaultModule.clientName', title: "客户名称", data: "", type: 'text',
            options: {
              testData: "比亚迪股份有限公司",
              field: "clientName",
              hideTitle: true,
              height: 12,
              fontSize: 6.75,
              width: 61.5,
              textAlign: "left",
              textContentVerticalAlign: "middle"
            }
          },
        ]),
      ]
    );
  };
  return {
    addElementTypes: addElementTypes
  };
};

export default aProvider()
