<template>
    <el-button
            size="mini"
            type="text"
            icon="el-icon-printer"
            @click="handleHiPrint()"
          >{{labelText}}</el-button>
</template>
<script>
import { printBarcodeLabel } from "@/yunshu-ui/api/print/print";
import {hiprint} from 'vue-plugin-hiprint'
import provider from "@/yunshu-ui/print/template/ProviderData";
import {PRINT_DICT} from "@/yunshu-ui/utils/print";
import {getMdItem} from "../../api/mes/md/mdItem";
import {getByTemplateType, getTemplate} from "@/yunshu-ui/api/print/template";
import {getBatchconfig} from "@/yunshu-ui/api/mes/md/batchconfig";
import {getItemrecptline} from "../../api/mes/wm/itemrecptline";
import {getDefaultPrinter} from "../../api/print/printerconfig";
import { ElMessage as Message } from "element-plus";

let hiprintTemplate
export default {
    name: 'printLabel',
    data(){
        return {

        }
    },
  mounted() {
    this.init()
  },
    props:{
        businessId: null,
        businessCode: null,
        businessType: null,
        labelText: {
            type: String,
            default: '打印'
        },
        printerCode: null
    },
    methods:{
        handlePrint(){

            var theCode;
            if(this.printerCode ==null){
                //是否有配置默认的打印机
                const key = 'defaultPrinter';
                let printerJson = localStorage.getItem(key);
                if(printerJson !=null){
                    const printer = JSON.parse(printerJson);
                    theCode = printer.printerCode;
                }else{
                    this.$modal.msgError("请在打印管理-打印机配置功能中指定默认打印机！");
                }
            }else{
                theCode = this.printerCode;
            }

            const param = { businessId: this.businessId, businessCode: this.businessCode, businessType: this.businessType, printerCode: theCode };
            printBarcodeLabel(param).then(response => {
              this.$modal.msgSuccess("打印成功");
            });
        },
      // 初始化打印插件
      init() {
        hiprint.init({
          providers: [provider]
        });
        hiprintTemplate = new hiprint.PrintTemplate()
      },
      // 标签打印
      async handleHiPrint() {
        // 根据 businessType 类型查询具体打印数据
        let printData
        switch (this.businessType) {
          case 'ITEM':
            // 物料/产品
            await getMdItem(this.businessId).then(res => {
              printData = res.data
            })
            break;
          case 'ITEM_BATCH':
            // 物料-批次
            await getItemrecptline(this.businessId).then(res => {
              printData = res.data
            })
            break;
          default:
            break;
        }
        // 根据 templateType 查询打印模板
        getByTemplateType(this.businessType).then(res => {
          const data = res.data
          if (hiprintTemplate) {
            try {
              // 将打印模板封装
              hiprintTemplate.update(JSON.parse(data.templateJson));
              if (window.hiwebSocket.opened) {
                // 根据配置好的客户端查询相应的打印机
                const client = JSON.parse(localStorage.getItem("defaultClient"))
                getDefaultPrinter(client.clientId).then(res => {
                  if (res.code == 200) {
                    hiprintTemplate.print2(printData, {printer: res.data.printerName, title: 'hiprint测试打印'});
                    this.$message.success("打印成功")
                  }
                })
                return
              }
              this.$message.error('客户端未连接,无法直接打印')
            } catch (e) {
              this.$message.error(`更新失败: ${e}`)
            }
          }
        })
      },
    }
}


</script>
