<template>
  <div class="print-studio" :class="{ 'print-studio--embed': embedMode }">
    <PrintStudioHeader :template-name="form.templateName" :dirty="dirty" :embed-mode="embedMode" />
    <el-card class="print-studio-body">
    <div class="print-studio-toolbar">
      <div class="print-studio-toolbar__inner">
        <el-button-group>
          <el-button
            v-for="(value, type) in paperTypes"
            :key="type"
            :type="curPaperType === type ? 'primary' : 'info'"
            @click="setPaper(type, value)"
          >
            {{ type }}
          </el-button>

          <el-popover
            :visible="paperPopVisible"
            title="设置纸张宽高(mm)"
            trigger="click"
            @update:visible="paperPopVisible = $event"
          >
              <div>
                <el-input-number v-model="paperWidth" style=" width: 120px; text-align: center"
                      controls-position="right"   placeholder="宽(mm)"/>
                <el-input style=" width: 50px; border-left: 0; pointer-events: none;text-align: center; backgroundColor: #fff"
                         placeholder="~" disabled
                />
                <el-input-number v-model="paperHeight" style="width: 120px; text-align: center; border-left: 0"
                      controls-position="right"   placeholder="高(mm)"/>
              </div>
              <el-button type="primary" style="width: 290px; margin-top: 10px;" @click="otherPaper">确定</el-button>
            <template #reference>
              <el-button :type="curPaperType === 'other' ? 'primary' : ''">自定义纸张</el-button>
            </template>
          </el-popover>
        </el-button-group>
          <el-input
            :min="scaleMin"
            :max="scaleMax"
            :step="0.1"
            disabled
            style="width: 100px; margin-left: 10px"
            :model-value="`${(scaleValue * 100).toFixed(0)}%`"
          >
            <template #prepend>
              <el-button icon="el-icon-zoom-out" @click="changeScale(false)"></el-button>
            </template>
            <template #append>
              <el-button icon="el-icon-zoom-in" @click="changeScale(true)"></el-button>
            </template>
          </el-input>
          <el-button-group style="margin-left: 10px;">
            <el-button type="primary" icon="el-icon-printer" @click="print()">打印</el-button>
            <el-button type="primary" icon="el-icon-refresh-left" @click="rotatePaper()">旋转</el-button>
            <el-button type="primary" icon="el-icon-view" @click="preView">预览</el-button>
            <el-button type="success" icon="el-icon-check" @click="handleSave">保存</el-button>
            <el-popconfirm
              title="是否确认清空?"
              confirm-button-text="确定清空"
              @confirm="clearPaper">
              <template #reference>
                <el-button type="danger" icon="el-icon-close">清空</el-button>
              </template>
            </el-popconfirm>
        </el-button-group>
        <el-radio-group style="margin-left: 10px;">
          <el-radio-button @click="setElsAlign('left')" title="左对齐">
            <svg-icon icon-class="alignLeft" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('vertical')" title="居中">
            <svg-icon icon-class="alignCenter" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('right')" title="右对齐">
            <svg-icon icon-class="alignRight" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('top')" title="顶部对齐">
            <svg-icon icon-class="alignTop" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('horizontal')" title="垂直居中">
            <svg-icon icon-class="horizontal" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('bottom')" title="底部对齐">
            <svg-icon icon-class="alignBottom" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('distributeHor')" title="横向分散">
            <svg-icon icon-class="resizeHorizontal" />
          </el-radio-button>
          <el-radio-button @click="setElsAlign('distributeVer')" title="纵向分散">
            <svg-icon icon-class="resizeVertical" />
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <el-row :gutter="8" class="print-studio-workspace">
      <el-col :span="4">
        <el-card class="ep-drag-container">
          <el-row>
            <el-col :span="24" class="rect-printElement-types hiprintEpContainer">
              <el-row class="drag_item_title">基础组件</el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.text" style>
                      <svg-icon icon-class="text" class="draggable-item-svg"/>
                      <p class="glyphicon-class">固定文本</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.image" style>
                      <svg-icon icon-class="picture" class="draggable-item-svg"/>
                      <p class="glyphicon-class">图片</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.longText">
                      <svg-icon icon-class="textarea" class="draggable-item-svg"/>
                      <p class="glyphicon-class">长文</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.date" style>
                      <svg-icon icon-class="date" class="draggable-item-svg"/>
                      <p class="glyphicon-class">日期时间</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row class="drag_item_title">辅助</el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.hline" style>
                      <svg-icon icon-class="hline" class="draggable-item-svg"/>
                      <p class="glyphicon-class">横线</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.vline" style>
                      <svg-icon icon-class="vline" class="draggable-item-svg"/>
                      <p class="glyphicon-class">竖线</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.rect">
                      <svg-icon icon-class="rect" class="draggable-item-svg"/>
                      <p class="glyphicon-class">矩形</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.oval">
                      <svg-icon icon-class="circle" class="draggable-item-svg"/>
                      <p class="glyphicon-class">圆形</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row class="drag_item_title">表单</el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.barcode">
                      <svg-icon icon-class="barcode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">条形码</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.qrcode">
                      <svg-icon icon-class="qrcode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">二维码</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.itemCode">
                      <svg-icon icon-class="itemcode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">物料编码</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.itemName">
                      <svg-icon icon-class="itemname" class="draggable-item-svg"/>
                      <p class="glyphicon-class">物料名称</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.workorderCode">
                      <svg-icon icon-class="workordercode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">工单编号</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.workorderName">
                      <svg-icon icon-class="workordername" class="draggable-item-svg"/>
                      <p class="glyphicon-class">工单名称</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.vendorCode">
                      <svg-icon icon-class="vendorcode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">供应商编号</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.vendorName">
                      <svg-icon icon-class="vendorname" class="draggable-item-svg"/>
                      <p class="glyphicon-class">供应商名称</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
              <el-row style="height: 100px;">
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.clientCode">
                      <svg-icon icon-class="clientcode" class="draggable-item-svg"/>
                      <p class="glyphicon-class">客户编号</p>
                    </a>
                  </div>
                </el-col>
                <el-col :span="12" class="drag_item_box">
                  <div>
                    <a class="ep-draggable-item" tid="defaultModule.clientName">
                      <svg-icon icon-class="clientname" class="draggable-item-svg"/>
                      <p class="glyphicon-class">客户名称</p>
                    </a>
                  </div>
                </el-col>
              </el-row>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="card-design">
          <div :id="canvasDomId" class="hiprint-printTemplate"></div>
        </el-card>
      </el-col>
      <el-col :span="4" class="params_setting_container">
        <el-card class="print-params-card">
          <template #header>
            <span class="print-params-card__title">元素属性</span>
          </template>
          <div :id="settingDomId" class="print-params-panel"></div>
          <p v-if="embedMode" class="print-params-empty">默认显示纸张设置；选中元素后可编辑元素属性</p>
        </el-card>
      </el-col>
    </el-row>
    <templatePreview id="templateView" ref="preView"></templatePreview>
    </el-card>

    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="模板编号" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4"></el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" placeholder="请选择模板类型">
                <el-option
                  v-for="item in printDictOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否默认" prop="isDefault">
              <el-input v-model="form.isDefault" placeholder="请输入是否默认" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="启用状态" prop="enableFlag">
              <el-input v-model="form.enableFlag" placeholder="请输入启用状态" />
            </el-form-item>
          </el-col>
          <el-col :span="4"></el-col>
          <el-col :span="12"></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="saveTemplate">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { defaultElementTypeProvider, hiprint } from "vue-plugin-hiprint";
import "@/yunshu-ui/print/print-bootstrap";
import panel from './panel'
import { starterPanel, resolveTemplateJson } from './panel-starter'
import printData from './print-data'
import templatePreview from './preview.vue'
import {getTemplate,addTemplate, updateTemplate, uploadShotCut } from "@/yunshu-ui/api/print/template";
import fontSize from "./font-size.js";
import scale from "./scale.js";
import html2canvas from "html2canvas";
import provider from "./ProviderData"
import {PRINT_DICT} from "@/yunshu-ui/utils/print";
import {getDefaultPrinter} from "@/yunshu-ui/api/print/printerconfig";
import {print} from "@/yunshu-ui/utils/print"
import webSite from "@/config/website";
import { ElMessage as Message } from "element-plus";
import PrintStudioHeader from "@/yunshu-ui/analytics/hub/components/PrintStudioHeader.vue";
import { layoutPrintParamsPanel } from "./print-params-layout";

export default {
  name: "printDesign",
  components: { templatePreview, PrintStudioHeader },
  props: {
    embedMode: { type: Boolean, default: false },
    templateId: { type: [String, Number], default: null },
    paperType: { type: String, default: "" },
  },
  emits: ["dirty-change"],
  data() {
    return {
      hiprintScopeId: `ps-${Date.now().toString(36)}`,
      dirty: false,
      suppressDirty: false,
      // 遮罩层
      loading: true,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      hiprintThis: null,
      hiprintTemplate: null,
      layoutObserver: null,
      // 表单参数
      form: {
        templateId: null,
        templateCode: null,
        templateType: null,
        templateName: null,
        templateJson: null,
        paperType: null,
        templateWidth: null,
        templateHeight: null,
        templatePic: null,
        remark: null
      },
      // 表单校验
      rules: {
        templateCode: [
          { required: true, message: "模板编号不能为空", trigger: "blur" }
        ],        templateType: [
          { required: true, message: "模板类型不能为空", trigger: "change" }
        ],
      },
      template: null,
      printDictOptions: PRINT_DICT,
      curPaper: {
        type: 'A4',
        width: 210,
        height: 296.6
      },
      paperTypes: {
        'A3': {
          width: 420,
          height: 296.6
        },
        'A4': {
          width: 210,
          height: 296.6
        },
        'A5': {
          width: 210,
          height: 147.6
        },
        'B3': {
          width: 500,
          height: 352.6
        },
        'B4': {
          width: 250,
          height: 352.6
        },
        'B5': {
          width: 250,
          height: 175.6
        }
      },
      // 自定义纸张
      paperPopVisible: false,
      paperWidth: '80',
      paperHeight: '60',
      // 缩放
      scaleValue: 1,
      scaleMax: 5,
      scaleMin: 0.5,
      // 导入导出json
      jsonIn: '',
      jsonOut: '',
      imageFileInput: null,
      pendingImageTarget: null,
      paramsPanelObserver: null,
      paramsPanelLayoutTimer: null,
      paramsPanelLayouting: false,
    }
  },
  computed: {
    canvasDomId() {
      return this.embedMode ? `hiprint-printTemplate-${this.hiprintScopeId}` : "hiprint-printTemplate";
    },
    settingDomId() {
      return this.embedMode ? `PrintElementOptionSetting-${this.hiprintScopeId}` : "PrintElementOptionSetting";
    },
    canvasSelector() {
      return `#${this.canvasDomId}`;
    },
    settingSelector() {
      return `#${this.settingDomId}`;
    },
    curPaperType() {
      let type = 'other'
      let types = this.paperTypes
      for (const key in types) {
        let item = types[key]
        let {width, height} = this.curPaper
        if (item.width === width && item.height === height) {
          type = key
        }
      }
      return type
    }
  },
  mounted() {
    this.ensureImageFileInput();
    this.ensureParamsPanelObserver();
    this.scheduleInit();
  },
  beforeUnmount() {
    this.teardownHiprint();
    this.teardownParamsPanelObserver();
    if (this.imageFileInput) {
      this.imageFileInput.removeEventListener("change", this.onImageFileSelected);
      if (this.imageFileInput.parentNode) {
        this.imageFileInput.parentNode.removeChild(this.imageFileInput);
      }
      this.imageFileInput = null;
    }
  },
  watch: {
    "$route.query.templateId"(id) {
      if (!this.embedMode && id) this.loadTemplateFromRoute();
    },
    templateId(id) {
      if (!this.embedMode || !id) return;
      if (this.hiprintTemplate) {
        this.loadTemplateFromRoute();
      }
    },
    dirty(value) {
      this.$emit("dirty-change", value);
    },
  },
  methods: {
    scheduleInit() {
      const runInit = () => {
        if (this.hiprintTemplate) return;
        const canvasEl = document.getElementById(this.canvasDomId);
        const settingEl = document.getElementById(this.settingDomId);
        if (!canvasEl || !settingEl) {
          requestAnimationFrame(runInit);
          return;
        }
        const rect = canvasEl.getBoundingClientRect();
        if (rect.width < 20 || rect.height < 20) {
          requestAnimationFrame(runInit);
          return;
        }
        try {
          this.init();
        } catch (error) {
          console.error("[print-design] init failed", error);
          Message.error(`设计器初始化失败: ${error?.message || error}`);
        }
      };

      if (this.layoutObserver) {
        this.layoutObserver.disconnect();
        this.layoutObserver = null;
      }

      if (this.embedMode) {
        this.$nextTick(() => {
          const workspace = this.$el?.querySelector(".print-studio-workspace");
          if (workspace && typeof ResizeObserver !== "undefined") {
            this.layoutObserver = new ResizeObserver(() => {
              if (!this.hiprintTemplate) runInit();
            });
            this.layoutObserver.observe(workspace);
          }
          requestAnimationFrame(runInit);
        });
      } else {
        runInit();
      }
    },
    teardownHiprint() {
      if (this.layoutObserver) {
        this.layoutObserver.disconnect();
        this.layoutObserver = null;
      }
      try {
        // eslint-disable-next-line no-undef
        $(this.canvasSelector).empty();
        // eslint-disable-next-line no-undef
        $(this.settingSelector).empty();
      } catch (e) {
        /* ignore */
      }
      this.hiprintTemplate = null;
      this.template = null;
    },
    resolveTemplateId() {
      if (this.embedMode) {
        return this.templateId || this.form.templateId;
      }
      return this.$route.query?.templateId || this.form.templateId;
    },
    markDirty() {
      if (this.suppressDirty) return;
      this.dirty = true;
    },
    loadTemplateFromRoute() {
      const templateId = this.resolveTemplateId();
      if (!templateId) {
        if (this.hiprintTemplate) {
          this.suppressDirty = true;
          try {
            this.hiprintTemplate.update(starterPanel);
          } catch (e) {
            this.$message.error(`更新失败: ${e}`);
          }
          this.suppressDirty = false;
        }
        this.scheduleDefaultPanelSettings();
        return;
      }
      this.suppressDirty = true;
      getTemplate(templateId).then((response) => {
        this.form = response.data;
        if (this.hiprintTemplate) {
          try {
            if (this.form.paperType === "other") {
              this.setPaper(this.form.paperType, {
                width: this.form.templateWidth,
                height: this.form.templateHeight,
              });
            } else {
              this.setPaper(this.form.paperType);
            }
            const { data } = resolveTemplateJson(this.form.templateJson);
            this.hiprintTemplate.update(data);
          } catch (e) {
            this.$message.error(`更新失败: ${e}`);
          }
        }
        this.dirty = false;
      }).finally(() => {
        this.suppressDirty = false;
        this.scheduleDefaultPanelSettings();
      });
    },
    scheduleDefaultPanelSettings() {
      this.$nextTick(() => {
        window.setTimeout(() => {
          this.openDefaultPanelSettings();
        }, 80);
      });
    },
    isSettingPanelEmpty() {
      const settingEl = document.getElementById(this.settingDomId);
      return !settingEl || settingEl.children.length === 0;
    },
    triggerPaperSettingsClick() {
      // eslint-disable-next-line no-undef
      const $canvas = $(this.canvasSelector);
      const $paper = $canvas.find(".hiprint-printPaper").first();
      if ($paper.length) {
        $paper.trigger("click");
        return;
      }
      const $panel = $canvas.find(".hiprint-printPanel").first();
      if ($panel.length) {
        $panel.trigger("click");
      }
    },
    openDefaultPanelSettings(retryCount = 0) {
      if (!this.hiprintTemplate) return;
      try {
        if (typeof this.hiprintTemplate.selectPanel === "function") {
          this.hiprintTemplate.selectPanel(0);
        }
      } catch (e) {
        /* ignore */
      }
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.triggerPaperSettingsClick();
          this.$nextTick(() => {
            if (this.isSettingPanelEmpty() && retryCount < 2) {
              window.setTimeout(() => {
                this.openDefaultPanelSettings(retryCount + 1);
              }, 200);
              return;
            }
            this.scheduleParamsPanelLayout();
          });
        });
      });
    },
    ensureParamsPanelObserver() {
      if (this.paramsPanelObserver) return;
      const panelEl = document.getElementById(this.settingDomId);
      if (!panelEl || typeof MutationObserver === "undefined") return;
      this.paramsPanelObserver = new MutationObserver(() => {
        if (this.paramsPanelLayouting) return;
        this.scheduleParamsPanelLayout();
      });
      this.paramsPanelObserver.observe(panelEl, { childList: true, subtree: true });
    },
    teardownParamsPanelObserver() {
      if (this.paramsPanelLayoutTimer) {
        window.clearTimeout(this.paramsPanelLayoutTimer);
        this.paramsPanelLayoutTimer = null;
      }
      if (this.paramsPanelObserver) {
        this.paramsPanelObserver.disconnect();
        this.paramsPanelObserver = null;
      }
    },
    scheduleParamsPanelLayout() {
      if (this.paramsPanelLayoutTimer) {
        window.clearTimeout(this.paramsPanelLayoutTimer);
      }
      this.paramsPanelLayoutTimer = window.setTimeout(() => {
        this.paramsPanelLayoutTimer = null;
        const panelEl = document.getElementById(this.settingDomId);
        this.paramsPanelLayouting = true;
        try {
          layoutPrintParamsPanel(panelEl);
        } finally {
          this.paramsPanelLayouting = false;
        }
      }, 30);
    },
    ensureImageFileInput() {
      if (this.imageFileInput) return;
      const input = document.createElement("input");
      input.type = "file";
      input.accept = "image/jpeg,image/png,image/gif,image/webp";
      input.style.display = "none";
      input.addEventListener("change", this.onImageFileSelected);
      document.body.appendChild(input);
      this.imageFileInput = input;
    },
    pickLocalImage(target) {
      this.pendingImageTarget = target;
      this.ensureImageFileInput();
      this.imageFileInput.value = "";
      this.imageFileInput.click();
    },
    onImageFileSelected() {
      const target = this.pendingImageTarget;
      const file = this.imageFileInput?.files?.[0];
      this.pendingImageTarget = null;
      if (!file || !target) return;
      const formData = new FormData();
      formData.append("file", file);
      uploadShotCut(formData).then((response) => {
        if (response.code === 200) {
          const raw = response.url || response.fileName;
          if (!raw) {
            Message.error("图片上传失败，未返回文件地址");
            return;
          }
          const absolute = raw.startsWith("http")
            ? raw
            : `${window.location.origin}${raw.startsWith("/") ? "" : "/"}${raw}`;
          target.refresh(absolute, { real: true });
        } else {
          Message.error(response.msg || "图片上传失败，请检查登录状态与文件格式（jpg/png/gif/webp，≤5MB）");
        }
      }).catch((err) => {
        console.error("[print-design] image upload failed", err);
        const status = err?.response?.status;
        if (status === 401) {
          Message.error("图片上传失败：登录已过期，请重新登录");
        } else {
          Message.error("图片上传失败，请检查登录状态与文件格式（jpg/png/gif/webp，≤5MB）");
        }
      });
    },
    init() {
      hiprint.init({
        providers: [provider],
        host: webSite.print_transfer_url,
        token: webSite.print_transfer_token
      });
      this.hiprintThis = hiprint
      // 还原配置
      hiprint.setConfig()
      // 替换配置
      hiprint.setConfig({
        optionItems: [
          fontSize,
          scale,
          function () {
            function t() {
              this.name = "zIndex";
            }

            return t.prototype.css = function (t, e) {
              if (t && t.length) {
                if (e) return t.css('z-index', e);
              }
              return null;
            }, t.prototype.createTarget = function () {
              return this.target = $('<div class="hiprint-option-item">\n        <div class="hiprint-option-item-label">\n        元素层级2\n        </div>\n        <div class="hiprint-option-item-field">\n        <input type="number" class="auto-submit"/>\n        </div>\n    </div>'), this.target;
            }, t.prototype.getValue = function () {
              var t = this.target.find("input").val();
              if (t) return parseInt(t.toString());
            }, t.prototype.setValue = function (t) {
              this.target.find("input").val(t);
            }, t.prototype.destroy = function () {
              this.target.remove();
            }, t;
          }(),
        ],
        movingDistance: 2.5,
        text: {
          tabs: [
            // 隐藏部分
            {
              // name: '测试', // tab名称 可忽略
              options: [] // 必须包含 options
            },// 当修改第二个 tabs 时,必须把他之前的 tabs 都列举出来.
            {
              name: '样式', options: [
                {
                  name: 'scale',
                  after: 'transform', // 自定义参数，插入在 transform 之后
                  hidden: false
                },
              ]
            }
          ],
          supportOptions: [
            {
              name: 'styler',
              hidden: true
            },
            {
              name: 'scale', // 自定义参数，supportOptions 必须得添加
              after: 'transform', // 自定义参数，插入在 transform 之后
              hidden: false
            },
            {
              name: 'formatter',
              hidden: true
            },
          ]
        },
        image: {
          tabs: [
            {
              // 整体替换
              replace: true,
              name: '基本', options: [
                {
                  name: 'field',
                  hidden: false
                },
                {
                  name: 'src',
                  hidden: false
                },
                {
                  name: 'fit',
                  hidden: false
                }
              ]
            },
          ],
        },
      })
      // eslint-disable-next-line no-undef
      hiprint.PrintElementTypeManager.buildByHtml($(this.$el).find('.ep-draggable-item'));
      // eslint-disable-next-line no-undef
      $(this.canvasSelector).empty();
      let that = this;
      this.template = this.hiprintTemplate = new hiprint.PrintTemplate({
        template: panel,
        // 图片选择功能
        onImageChooseClick: (target) => {
          this.pickLocalImage(target);
        },
        // 自定义可选字体
        // 或者使用 this.hiprintTemplate.setFontList([])
        // 或元素中 options.fontList: []
        fontList: [
          {title: '微软雅黑', value: 'Microsoft YaHei'},
          {title: '黑体', value: 'STHeitiSC-Light'},
          {title: '思源黑体', value: 'SourceHanSansCN-Normal'},
          {title: '王羲之书法体', value: '王羲之书法体'},
          {title: '宋体', value: 'SimSun'},
          {title: '华为楷体', value: 'STKaiti'},
          {title: 'cursive', value: 'cursive'},
        ],
        dataMode: 1, // 1:getJson 其他：getJsonTid 默认1
        history: true, // 是否需要 撤销重做功能
        onDataChanged: () => {
          this.markDirty();
        },
        onUpdateError: (e) => {
          console.log(e);
        },
        settingContainer: this.settingSelector,
        paginationContainer: '.hiprint-printPagination'
      });
      this.hiprintTemplate.design(this.canvasSelector, { grid: true });
      this.scaleValue = this.hiprintTemplate.editingPanel?.scale || 1;
      if (this.layoutObserver) {
        this.layoutObserver.disconnect();
        this.layoutObserver = null;
      }
      this.$nextTick(() => {
        if (this.hiprintTemplate?.editingPanel) {
          this.hiprintTemplate.zoom(this.scaleValue);
        }
        this.loadTemplateFromRoute();
      });
    },
    /**
     * 设置纸张大小
     * @param type [A3, A4, A5, B3, B4, B5, other]
     * @param value {width,height} mm
     */
    setPaper(type, value) {
      try {
        this.form.paperType = type
        if (Object.keys(this.paperTypes).includes(type)) {
          this.curPaper = {type: type, width: this.paperTypes[type].width, height: this.paperTypes[type].height}
          this.hiprintTemplate.setPaper(this.paperTypes[type].width, this.paperTypes[type].height)
          this.form.templateWidth = this.paperTypes[type].width
          this.form.templateHeight = this.paperTypes[type].height
        } else {
          this.curPaper = {type: 'other', width: value.width, height: value.height}
          this.hiprintTemplate.setPaper(value.width, value.height)
          this.form.templateWidth = value.width
          this.form.templateHeight = value.height
        }
        const pageArea = this.form.templateHeight * this.form.templateWidth
        let scaleValue = this.scaleValue
        if (pageArea < 30000) {
          // 画布比例放大
          // 根据纸张宽度确定页面缩放比例
          scaleValue = Math.floor(250 / this.form.templateWidth)
        } else {
          scaleValue = 1
        }
        if (this.hiprintTemplate) {
          this.hiprintTemplate.zoom(scaleValue);
          this.scaleValue = scaleValue;
        }
        this.markDirty();
      } catch (error) {
        this.$message.error(`操作失败: ${error}`)
      }
    },
    otherPaper() {
      let value = {}
      value.width = this.paperWidth
      value.height = this.paperHeight
      this.paperPopVisible = false
      this.setPaper('other', value)
    },
    changeScale(big) {
      let scaleValue = this.scaleValue;
      if (big) {
        scaleValue += 0.1;
        if (scaleValue > this.scaleMax) scaleValue = 5;
      } else {
        scaleValue -= 0.1;
        if (scaleValue < this.scaleMin) scaleValue = 0.5;
      }
      if (this.hiprintTemplate) {
        // scaleValue: 放大缩小值, false: 不保存(不传也一样), 如果传 true, 打印时也会放大
        this.hiprintTemplate.zoom(scaleValue);
        this.scaleValue = scaleValue;
      }
    },
    rotatePaper() {
      if (this.hiprintTemplate) {
        this.hiprintTemplate.rotatePaper();
        this.markDirty();
      }
    },
    preView() {
      let {width} = this.curPaper
      this.$refs.preView.show(
        this.hiprintTemplate,
        printData,
        width,
        this.form.templateName || this.form.templateCode || '打印模板',
      )
    },
    handleSave(){
      if(this.form.templateId !=null){

        this.saveTemplate();
      }else{
        this.open = true;
        this.title = "新增打印模板配置";
      }
    },
    //将当前模板的JSON保存到数据库
    saveTemplate(){
      let that = this;
      if (this.hiprintTemplate) {
        // this.form.templateJson = JSON.stringify(this.hiprintTemplate.getJson() || {});
        this.form.templateJson = JSON.stringify(this.hiprintTemplate.getJson())
      }
      //保存缩略图
      html2canvas(document.getElementById(this.canvasDomId)).then(function(canvas){
        canvas.toBlob(blob =>{
          // const href = window.URL.createObjectURL(new Blob([blob]));
          // const link = document.createElement('a');
          // link.href = href;
          // link.download = '测试图片.png';
          // document.body.appendChild(link);
          // link.click();
          // document.body.removeChild(link);
          let fileName = `${new Date().getTime()}.jpg`;
          let file = new File([blob],fileName,{type: 'image/jpg'});
          let formData = new FormData();
          formData.append('file',file);
          uploadShotCut(formData).then(response =>{
            if(response.code === 200){
              that.form.templatePic = response.url || response.fileName;
            }
            if (that.form.templateId != null) {
                  updateTemplate(that.form).then(() => {
                    that.$modal.msgSuccess("修改成功");
                    that.dirty = false;
                    that.open = false;
                  });
              } else {
                  addTemplate(that.form).then((response) => {
                    that.$modal.msgSuccess("新增成功");
                    that.dirty = false;
                    that.open = false;
                    if (response?.data?.templateId) {
                      that.form.templateId = response.data.templateId;
                      that.$router.replace({
                        query: {
                          ...that.$route.query,
                          templateId: String(response.data.templateId),
                        },
                      });
                    }
                  });
            }
          });
        },'img/png');
      });

    },
    //当前模板的打印测试
    printText(){

    },
    onlyPrint2() {
      let that = this;
      if (window.hiwebSocket.opened) {
        let printTpl = this.$print2(undefined, panel, printData, {
          printer: '', title: 'Api单独打印',
          styleHandler: () => {
            // let css = '<link href="http://hiprint.io/Content/hiprint/css/print-lock.css" media="print" rel="stylesheet">';
            let css = '<style>.hiprint-printElement-text{color:red !important;}</style>'
            return css
          }
        })
        let key = 'Api单独直接打印';
        printTpl.on('printSuccess', function () {
          that.$notification.success({
            key: key,
            placement: 'topRight',
            message: key + ' 打印成功',
            description: 'Api单独直接打印回调',
          });
        });
        return;
      }
      this.$message.error('客户端未连接,无法直接打印')
    },
    print() {
          // 根据配置好的客户端查询相应的打印机
          const client = JSON.parse(localStorage.getItem("defaultClient"))
          const clients = Object.entries(this.hiprintThis.hiwebSocket.clients)
          let clientName = ""
          clients.forEach(item => {
            if (client.clientIp == item[1].ip) {
              clientName = item[0]
            }
          })
          if (clientName) {
            getDefaultPrinter(client.clientId).then(res => {
              if (res.code == 200) {
                this.hiprintTemplate.print2({}, {client: clientName, printer: res.data.printerName, title: 'hiprint测试打印'});
                Message.success("打印成功")
              }
            }).catch(err => {
            })
          } else {
            Message.error('客户端未连接,无法直接打印')
          }
    },
    clearPaper() {
      try {
        this.hiprintTemplate.clear();
        this.markDirty();
      } catch (error) {
        this.$message.error(`操作失败: ${error}`);
      }
    },
    ippPrintAttr() {
      // 不知道打印机 ipp 情况， 可通过 '客户端' 获取一下
      const printerList = this.hiprintTemplate.getPrinterList();
      console.log(printerList)
      if (!printerList.length) return;
      let p = printerList[0];
      console.log(p)
      // 系统不同， 参数可能不同
      let url = p.options['printer-uri-supported'];
      // 测试 获取 ipp打印 支持参数
      hiprint.ippPrint({
        url: url,
        // 打印机参数： {version,uri,charset,language}
        opt: {},
        action: 'Get-Printer-Attributes', // 获取打印机支持参数
        // ipp参数
        message: null,
      }, (res) => {
        // 执行的ipp 任务回调 / 错误回调
        console.log(res)
      }, (printer) => {
        // ipp连接成功 回调 打印机信息
        console.log(printer)
      })
    },
    ippPrintTest() {
      // 不知道打印机 ipp 情况， 可通过 '客户端' 获取一下
      const printerList = this.hiprintTemplate.getPrinterList();
      console.log(printerList)
      if (!printerList.length) return;
      let p = printerList[0];
      console.log(p)
      // 系统不同， 参数可能不同
      let url = p.options['printer-uri-supported'];
      // 测试 打印文本
      hiprint.ippPrint({
        url: url,
        // 打印机参数： {version,uri,charset,language}
        opt: {},
        action: 'Print-Job',
        // ipp参数
        message: {
          "operation-attributes-tag": {
            "requesting-user-name": "hiPrint", // 用户名
            "job-name": "ipp Test Job", // 任务名
            "document-format": "text/plain" // 文档类型
          },
          // data 需为 Buffer (客户端简单处理了string 转 Buffer), 支持设置 encoding
          // data 需为 Buffer (客户端简单处理了string 转 Buffer), 支持设置 encoding
          // data 需为 Buffer (客户端简单处理了string 转 Buffer), 支持设置 encoding
          // 其他 Uint8Array/ArrayBuffer   默认仅 使用 Buffer.from(data)
          // 其他 Uint8Array/ArrayBuffer   默认仅 使用 Buffer.from(data)
          // 其他 Uint8Array/ArrayBuffer   默认仅 使用 Buffer.from(data)
          // 其他 Uint8Array/ArrayBuffer   默认仅 使用 Buffer.from(data)
          data: 'test test test test test test test',
          encoding: 'utf-8' // 默认可不传
        }
      }, (res) => {
        // 执行的ipp 任务回调 / 错误回调
        console.log(res)
      }, (printer) => {
        // ipp连接成功 回调 打印机信息
        console.log(printer)
      })
    },
    // 自定义 ipp 请求
    ippRequestTest() {
      const printerList = this.hiprintTemplate.getPrinterList();
      console.log(printerList)
      if (!printerList.length) return;
      let p = printerList[0];
      console.log(p)
      // 系统不同， 参数可能不同
      let url = p.options['printer-uri-supported'];
      // 详见： https://www.npmjs.com/package/ipp
      hiprint.ippRequest({
        url: url,
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        data: {
          "operation": "Get-Printer-Attributes",
          "operation-attributes-tag": {
            // 测试发现 Request下列3个必须要有
            "attributes-charset": "utf-8",
            "attributes-natural-language": "zh-cn",
            "printer-uri": url
          }
        }
      }, (res) => {
        // 执行的ipp 任务回调 / 错误回调
        console.log(res)
      })
    },
    ippRequestPrint() {
      const printerList = this.hiprintTemplate.getPrinterList();
      console.log(printerList)
      if (!printerList.length) return;
      let p = printerList[0];
      console.log(p)
      // 系统不同， 参数可能不同
      let url = p.options['printer-uri-supported'];
      let str = "ippRequestPrint ippRequestPrint ippRequestPrint";
      let array = new Uint8Array(str.length);
      for (var i = 0; i < str.length; i++) {
        array[i] = str.charCodeAt(i);
      }
      let testData = array.buffer;
      // 详见： https://www.npmjs.com/package/ipp
      hiprint.ippRequest({
        url: url,
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        // 传入的数据 ipp.serialize 后 未做任何处理  打印内容 需要 Buffer
        data: {
          "operation": "Print-Job",
          "operation-attributes-tag": {
            // 测试发现 Request下列3个必须要有
            "attributes-charset": "utf-8",
            "attributes-natural-language": "zh-cn",
            "printer-uri": url,
            "requesting-user-name": "hiPrint", // 用户名
            "job-name": "ipp Request Job", // 任务名
            "document-format": "text/plain" // 文档类型
          },
          data: testData
        }
      }, (res) => {
        // 执行的ipp 任务回调 / 错误回调
        console.log(res)
      })
    },
    updateJson() {
      if (this.hiprintTemplate) {
        try {
          this.hiprintTemplate.update(JSON.parse(this.jsonIn))
        } catch (e) {
          this.$message.error(`更新失败: ${e}`)
        }
      }
    },
    exportJson() {
      if (this.hiprintTemplate) {
        this.jsonOut = JSON.stringify(this.hiprintTemplate.getJson() || {})
      }
    },
    setElsAlign(e) {
      this.hiprintTemplate.setElsAlign(e)
    },
    setElsSpace(h) {
      this.hiprintTemplate.setElsSpace(10, h)
    },
    getSelectEls() {
      let els = this.hiprintTemplate.getSelectEls();
      console.log(els)
    },
    updateFontSize() {
      this.hiprintTemplate.updateOption('fontSize', 12);
    },
    updateFontWeight() {
      this.hiprintTemplate.updateOption('fontWeight', 'bolder');
    },
    cancel(){
      this.open = false;
    }
  }
}
</script>

<style scoped>

.drag_item_box {
  height: 100%;
  padding: 6px;
}

.drag_item_box > div {
  height: 100%;
  width: 100%;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
}



.drag_item_box > div > a {
  text-align: center;
  text-decoration-line: none;
}

.drag_item_box > div > a > .draggable-item-svg {
  width: 50px;
  height: 50px;
}


.drag_item_box > div > a > span {
  font-size: 28px;
}

.drag_item_box > div > a > p {
  margin: 0;
}

.drag_item_title {
  font-size: 16px;
  padding: 12px 6px 0 6px;
  font-weight: bold;
}



.card-design {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  overflow-x: auto;
  overflow-y: auto;
}

.ep-drag-container {
  height: 100vh;
  overflow-y: auto;
}

.print-studio--embed .card-design,
.print-studio--embed .ep-drag-container {
  height: 100% !important;
  min-height: 400px;
}

.print-studio--embed .card-design .hiprint-printTemplate {
  width: 100%;
  min-height: 400px;
}

.print-studio--embed .params_setting_container {
  height: 100%;
}

.print-studio-toolbar {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.print-studio-toolbar__inner {
  margin-bottom: 10px;
}

</style>
