<template>
    <el-dialog
      v-model="visible"
      :show-close="false"
      close-on-click-modal
      :width="(width + 20) + 'mm'">
      <template #header>
        <div class="preview-dialog-header">
          <el-button
            class="preview-dialog-header__export"
            type="primary"
            :loading="spinning"
            @click="toPdf"
          >
            导出 PDF
          </el-button>
          <span class="preview-dialog-header__title">模板预览</span>
          <button
            class="preview-dialog-header__close"
            type="button"
            aria-label="关闭预览"
            @click="hideModal"
          >
            ×
          </button>
        </div>
      </template>
      <div v-show="isMultiPanel" id="template-preview-printPagination" style="margin: 14px 0 0 10px;"></div>
      <el-card class="card-design">
        <div id="preview_content_design"></div>
      </el-card>
    </el-dialog>
  </template>

<script>
import "@/yunshu-ui/print/print-bootstrap";
import { hiprint } from 'vue-plugin-hiprint';
import { ElMessage } from "element-plus";
import { buildPreviewData } from "./preview-data";

let hiprintTemplate;
export default {
  name: "templatePreview",
  data() {
    return {
      visible: false,
      spinning: true,
      waitShowPrinter: false,
      template: null,
      isMultiPanel: true,
      // 模板
      hiprintTemplate: {},
      // 纸张宽 mm
      width: 0,
      // 数据
      name: '名称',
      json: {},
      printData: {},
      // 扩展 css
      extendCss: ''
    }
  },
  methods: {
    hideModal() {
      this.visible = false
      const printLink = document.querySelector("link[media=print]");
      if (printLink) {
        printLink.innerHTML = "";
      }
    },
    show(hiprintTemplateArg, printData, width = '210', name = '打印模板') {
      this.visible = true
      this.spinning = true
      this.width = hiprintTemplateArg.editingPanel ? hiprintTemplateArg.editingPanel.width : width;
      this.hiprintTemplate = hiprintTemplateArg
      this.printData = buildPreviewData(hiprintTemplateArg, printData)
      this.name = name
      hiprintTemplate = hiprintTemplateArg
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          const previewEl = document.getElementById('preview_content_design');
          if (previewEl) {
            const rendered = hiprintTemplateArg.getHtml(this.printData);
            previewEl.replaceChildren(...Array.from(rendered).map((node) => node.cloneNode(true)));
          }
          this.spinning = false
        });
      });
    },
    print() {
      let that = this;
      this.waitShowPrinter = true
      this.hiprintTemplate.print(this.printData, {}, {
        callback: () => {
          this.waitShowPrinter = false
        },
        styleHandler: () => {
          return that.extendCss
        }
      })
    },
    async toPdf() {
      // hiPrint 自带的 toPdf 用 html2canvas 截图后导出为 JPEG（有损压缩），
      // 黑色文字边缘会被压出灰晕、整体发浅。这里改为自绘：html2canvas 高倍
      // 渲染 + PNG 无损输出，文字保持纯黑清晰。
      const paperEl = document.querySelector('#preview_content_design .hiprint-printPaper')
        || document.getElementById('preview_content_design');
      if (!paperEl) {
        ElMessage.error('预览尚未准备完成，请稍后重试');
        return;
      }
      this.spinning = true;
      try {
        const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
          import('html2canvas'),
          import('jspdf'),
        ]);
        const panel = (this.hiprintTemplate?.getJson?.()?.panels || [])[0] || {};
        const mmW = Number(panel.width) || Number(this.width) || 210;
        const mmH = Number(panel.height) || 297;
        const canvas = await html2canvas(paperEl, {
          scale: 4,
          backgroundColor: '#ffffff',
          useCORS: true,
          logging: false,
        });
        const imgData = canvas.toDataURL('image/png');
        const orientation = mmW > mmH ? 'landscape' : 'portrait';
        const pdf = new jsPDF({ orientation, unit: 'mm', format: [mmW, mmH] });
        const pageW = pdf.internal.pageSize.getWidth();
        const pageH = pdf.internal.pageSize.getHeight();
        const imgH = (canvas.height * pageW) / canvas.width;
        let heightLeft = imgH;
        let position = 0;
        pdf.addImage(imgData, 'PNG', 0, position, pageW, imgH, undefined, 'FAST');
        heightLeft -= pageH;
        while (heightLeft > 0) {
          position -= pageH;
          pdf.addPage([mmW, mmH], orientation);
          pdf.addImage(imgData, 'PNG', 0, position, pageW, imgH, undefined, 'FAST');
          heightLeft -= pageH;
        }
        const fileName = (this.name || '打印模板').replace(/\.pdf$/i, '');
        pdf.save(`${fileName}.pdf`);
      } catch (e) {
        console.error('导出 PDF 失败', e);
        ElMessage.error('导出 PDF 失败，请重试');
      } finally {
        this.spinning = false;
      }
    },
    print2() {
      if (hiprint.hiwebSocket.opened) {
        let that = this;
        this.hiprintTemplate.print2(this.printData, {
          printer: '', title: this.name,
          styleHandler: () => {
            return that.extendCss
          }
        })
      } else
        ElMessage.error('请先连接直接打印客户端')
    },
  }
}

</script>
<style scoped>
.card-design {
  overflow: hidden;
  overflow-x: auto;
  overflow-y: auto;
}

:deep(.el-dialog__header) {
  margin-right: 0;
  padding-right: var(--el-dialog-padding-primary, 16px);
}

.preview-dialog-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  width: 100%;
  min-height: 32px;
  gap: 16px;
  box-sizing: border-box;
}

.preview-dialog-header__export {
  justify-self: start;
}

.preview-dialog-header__title {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  line-height: 1;
}

.preview-dialog-header__close {
  justify-self: end;
  display: inline-grid;
  width: 30px;
  height: 30px;
  padding: 0;
  color: #606266;
  font-size: 26px;
  font-weight: 300;
  line-height: 1;
  place-items: center;
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: transparent;
  transition: color 0.16s ease, background-color 0.16s ease;
}

.preview-dialog-header__close:hover {
  color: #303133;
  background: #f2f4f7;
}

.preview-dialog-header__close:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}
</style>
