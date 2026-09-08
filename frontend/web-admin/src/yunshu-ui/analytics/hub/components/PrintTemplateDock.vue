<template>
  <aside class="print-template-dock">
    <div class="print-template-dock__head">
      <strong>模板库</strong>
      <span>共 {{ templateList.length }} 条</span>
    </div>
    <div class="print-template-dock__search">
      <el-input
        v-model="keyword"
        size="small"
        clearable
        placeholder="搜索编号 / 名称"
        prefix-icon="el-icon-search"
        @keyup.enter="applyFilter"
        @clear="applyFilter"
      />
    </div>
    <div v-loading="loading" class="print-template-dock__list">
      <button
        type="button"
        class="print-template-dock__item print-template-dock__item--create"
        @click="handleAdd"
        v-hasPermi="['template:template:add']"
      >
        <span class="print-template-dock__thumb print-template-dock__thumb--create">
          <el-icon :size="24"><Plus /></el-icon>
        </span>
        <span class="print-template-dock__meta">
          <span class="print-template-dock__name">新建模板</span>
          <span class="print-template-dock__code">创建标签打印模板</span>
        </span>
      </button>
      <button
        v-for="item in filteredList"
        :key="item.templateId"
        type="button"
        class="print-template-dock__item"
        :class="{ 'is-active': String(selectedTemplateId) === String(item.templateId) }"
        @click="handleSelect(item)"
      >
        <span class="print-template-dock__thumb">
          <img v-if="item.templatePic" :src="item.templatePic" alt="" />
          <el-icon v-else :size="24"><Printer /></el-icon>
        </span>
        <span class="print-template-dock__meta">
          <span class="print-template-dock__name">{{ item.templateName }}</span>
          <span class="print-template-dock__code">{{ item.templateCode }}</span>
          <span class="print-template-dock__paper">{{ item.paperType }} · {{ item.templateWidth }}×{{ item.templateHeight }}mm</span>
        </span>
        <el-icon
          class="print-template-dock__delete"
          @click.stop="handleDelete(item)"
          v-hasPermi="['template:template:remove']"
        >
          <Delete />
        </el-icon>
      </button>
      <div v-if="!loading && !filteredList.length" class="print-template-dock__empty">
        {{ keyword ? "无匹配模板" : "暂无模板，点击「新建模板」开始" }}
      </div>
    </div>

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="960px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="access-form-dialog__form">
        <el-row>
          <el-col :span="8">
            <el-form-item label="模板编号" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="80">
              <el-switch
                v-model="autoGenFlag"
                active-color="#13ce66"
                active-text="自动生成"
                @change="handleAutoGenChange(autoGenFlag)"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" placeholder="请选择模板类型" style="width: 100%">
                <el-option v-for="dict in dict.type.print_template_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纸张类型" prop="paperType">
              <el-select v-model="form.paperType" placeholder="请选择纸张类型" style="width: 100%" @change="changePaperType">
                <el-option v-for="dict in dict.type.print_paper_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.paperType === 'other'">
          <el-col :span="12">
            <el-form-item label="纸张宽度" prop="templateWidth">
              <el-input-number v-model="form.templateWidth" placeholder="宽(mm)" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纸张高度" prop="templateHeight">
              <el-input-number v-model="form.templateHeight" placeholder="高(mm)" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否默认" prop="isDefault">
              <el-radio-group v-model="form.isDefault">
                <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag">
                <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </aside>
</template>

<script>
import { listTemplate, delTemplate, addTemplate } from "@/yunshu-ui/api/print/template";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";
import { starterPanel } from "@/yunshu-ui/print/template/panel-starter";
import { Delete, Plus, Printer } from "@element-plus/icons-vue";

export default {
  name: "PrintTemplateDock",
  components: { Delete, Plus, Printer },
  dicts: ["print_template_type", "sys_yes_no", "print_paper_type"],
  props: {
    selectedTemplateId: { type: [String, Number], default: null },
  },
  emits: ["select", "created", "deleted", "loaded", "loading-change"],
  data() {
    return {
      loading: false,
      keyword: "",
      templateList: [],
      autoGenFlag: false,
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 200,
        templateCode: null,
        templateName: null,
      },
      form: {},
      rules: {
        templateCode: [{ required: true, message: "模板编号不能为空", trigger: "blur" }],
        templateName: [{ required: true, message: "模板名称不能为空", trigger: "blur" }],
        templateType: [{ required: true, message: "模板类型不能为空", trigger: "change" }],
        paperType: [{ required: true, message: "请选择纸张类型", trigger: "change" }],
        remark: [{ max: 250, message: "长度必须小于250个字符", trigger: "blur" }],
      },
      paperTypes: {
        A3: { width: 420, height: 296.6 },
        A4: { width: 210, height: 296.6 },
        A5: { width: 210, height: 147.6 },
        B3: { width: 500, height: 352.6 },
        B4: { width: 250, height: 352.6 },
        B5: { width: 250, height: 175.6 },
      },
    };
  },
  computed: {
    filteredList() {
      const q = (this.keyword || "").trim().toLowerCase();
      if (!q) return this.templateList;
      return this.templateList.filter(
        (item) =>
          String(item.templateCode || "").toLowerCase().includes(q) ||
          String(item.templateName || "").toLowerCase().includes(q),
      );
    },
  },
  mounted() {
    this.getList();
  },
  methods: {
    applyFilter() {
      /* client-side filter only */
    },
    refresh() {
      return this.getList();
    },
    getList() {
      this.loading = true;
      this.$emit("loading-change", true);
      return listTemplate(this.queryParams)
        .then((response) => {
          this.templateList = response.rows || [];
          this.$emit("loaded", this.templateList);
          this.autoSelectFirst();
        })
        .finally(() => {
          this.loading = false;
          this.$emit("loading-change", false);
        });
    },
    autoSelectFirst() {
      if (this.selectedTemplateId != null && this.selectedTemplateId !== "") return;
      const first = this.templateList[0];
      if (!first) return;
      this.$emit("select", {
        templateId: first.templateId,
        paperType: first.paperType || "A4",
        templateName: first.templateName,
      });
    },
    changePaperType(val) {
      if (val === "other") {
        this.form.templateWidth = undefined;
        this.form.templateHeight = undefined;
      } else if (this.paperTypes[val]) {
        this.form.templateWidth = this.paperTypes[val].width;
        this.form.templateHeight = this.paperTypes[val].height;
      }
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        templateId: null,
        templateCode: null,
        templateName: null,
        templateType: null,
        templateJson: JSON.stringify(starterPanel),
        paperType: null,
        templateWidth: undefined,
        templateHeight: undefined,
        isDefault: "Y",
        enableFlag: "Y",
        remark: null,
        templatePic: null,
      };
      this.autoGenFlag = false;
      this.resetForm("form");
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加打印模板";
    },
    handleSelect(item) {
      if (!item) return;
      this.$emit("select", {
        templateId: item.templateId,
        paperType: item.paperType || "A4",
        templateName: item.templateName,
      });
    },
    handleDelete(item) {
      const templateIds = item.templateId;
      this.$modal
        .confirm(`是否确认删除模板「${item.templateName}」？`)
        .then(() => delTemplate(templateIds))
        .then(() => {
          this.$modal.msgSuccess("删除成功");
          const deletedId = String(item.templateId);
          this.getList().then(() => {
            this.$emit("deleted", deletedId);
          });
        })
        .catch(() => {});
    },
    submitForm() {
      if (!(this.form.templateWidth >= 1)) {
        this.$message.warning("请输入纸张宽度");
        return;
      }
      if (!(this.form.templateHeight >= 1)) {
        this.$message.warning("请输入纸张高度");
        return;
      }
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        addTemplate(this.form).then((response) => {
          this.$modal.msgSuccess("新增成功");
          this.open = false;
          const created = {
            ...this.form,
            templateId: response.data?.templateId ?? response.data,
          };
          this.getList().then(() => {
            this.$emit("created", created);
            this.handleSelect(created);
          });
        });
      });
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) {
        genCode("PRINT_TEMPLATE_CODE").then((response) => {
          this.form.templateCode = response;
        });
      } else {
        this.form.templateCode = null;
      }
    },
  },
};
</script>
