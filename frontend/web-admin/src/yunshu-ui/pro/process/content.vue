<template>
  <div
    class="process-content-panel"
    :class="{
      'process-content-panel--gallery': galleryMode,
      'process-content-panel--workbench-table': workbenchTable,
    }"
  >
    <el-row v-if="optType !== 'view'" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:pro:process:add']">新增步骤</el-button>
      </el-col>
    </el-row>

    <div v-if="galleryMode && displayList.length" class="process-step-gallery">
      <div v-for="(item, index) in displayList" :key="item.contentId || index" class="process-step-card">
        <button
          v-if="stepImage(item)"
          type="button"
          class="process-step-card__img-btn"
          @click="openPreview(item)"
        >
          <img :src="stepImage(item)" alt="" class="process-step-card__img" />
        </button>
        <div v-else class="process-step-card__img process-step-card__img--empty">暂无步骤图</div>
        <div class="process-step-card__body">
          <b>步骤 {{ item.orderNum }}</b>
          <p>{{ item.contentText }}</p>
          <small v-if="item.device">设备：{{ item.device }}</small>
          <div v-if="optType !== 'view'" class="process-step-card__actions">
            <el-button size="small" link @click="handleUpdate(item)" v-hasPermi="['mes:pro:process:edit']">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(item)" v-hasPermi="['mes:pro:process:remove']">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="workbenchTable || !galleryMode" class="process-step-table-wrap">
      <div class="inbound-table-frame process-step-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <el-table
          v-loading="loading"
          class="yunshu-data-table inbound-table process-step-table"
          stripe
          border
          :data="displayList"
          empty-text="暂无操作步骤"
          @selection-change="handleSelectionChange"
        >
        <el-table-column v-if="optType !== 'view'" type="selection" width="42" align="center" />
        <el-table-column label="步骤" width="64" align="center" header-align="center">
          <template #default="{ row }">
            <span class="process-step-order">{{ row.orderNum }}</span>
          </template>
        </el-table-column>
        <el-table-column label="步骤示意图与说明" min-width="360" align="left" header-align="center" class-name="col-step-media">
          <template #default="{ row }">
            <div class="process-step-row">
              <button
                type="button"
                class="process-step-row__thumb"
                :title="row.contentText"
                @click="openPreview(row)"
              >
                <img v-if="stepImage(row)" :src="stepImage(row)" alt="" />
                <span v-else class="process-step-row__thumb-empty">无图</span>
                <span class="process-step-row__zoom">放大</span>
              </button>
              <div class="process-step-row__text">
                <p class="process-step-row__desc">{{ row.contentText || "—" }}</p>
                <div class="process-step-row__meta">
                  <span v-if="row.device">设备 {{ row.device }}</span>
                  <span v-if="row.material">材料 {{ row.material }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="辅助设备" min-width="110" align="center" header-align="center" prop="device" show-overflow-tooltip />
        <el-table-column label="辅助材料" min-width="110" align="center" header-align="center" prop="material" show-overflow-tooltip />
        <el-table-column label="备注" min-width="120" align="center" header-align="center" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" width="120" v-if="optType !== 'view'" align="center" header-align="center" class-name="col-actions small-padding fixed-width" fixed="right">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:process:edit']">修改</el-button>
              <el-button size="small" link @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:process:remove']">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </div>

    <pagination
      v-show="total > 0 && !galleryMode && !workbenchTable"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      layout="prev, pager, next"
      :pager-count="5"
      :auto-scroll="false"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <el-dialog
      v-model="previewVisible"
      title="步骤示意图"
      width="760px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      class="process-step-preview-dialog"
      @closed="onPreviewClosed"
    >
      <div class="process-step-preview-dialog__body">
        <img v-if="previewUrl" :src="previewUrl" alt="" class="process-step-preview-dialog__img" />
        <div v-if="previewCaption" class="process-step-preview-dialog__caption">
          <b>步骤 {{ previewOrderNum }}</b>
          <p>{{ previewCaption }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="closePreview">关闭</el-button>
        <el-button v-if="hasNextPreview" type="primary" plain @click="showNextPreview">下一张</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="title" v-model="open" width="720px" append-to-body destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="顺序编号" prop="orderNum">
              <el-input-number :min="1" :max="999999" v-model="form.orderNum" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="步骤示意图">
          <ImageUpload
            :limit="1"
            :value="form.docUrl"
            :file-size="5"
            @onUploaded="handleImgUploaded"
            @onRemoved="handleImgRemoved"
          />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="辅助设备" prop="device">
              <el-input v-model="form.device" placeholder="请输入辅助设备" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="辅助材料" prop="material">
              <el-input v-model="form.material" placeholder="请输入辅助材料" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="步骤说明" prop="contentText">
          <el-input v-model="form.contentText" type="textarea" placeholder="请输入步骤说明" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listProcesscontent, getProcesscontent, delProcesscontent, addProcesscontent, updateProcesscontent } from "@/yunshu-ui/api/mes/pro/processcontent";
import ImageUpload from "@/yunshu-ui/components/ImageUpload/index.vue";
import { resolveStepImage, getDemoProcessSteps } from "./processImageMap.js";

function shouldUseDemoSteps(rows, processCode, optType) {
  if (optType !== "view" || !processCode) return false;
  if (!rows.length) return true;
  return rows.every((row) => !row.contentText || !/[\u4e00-\u9fa5]/.test(String(row.contentText)));
}

export default {
  name: "Processcontent",
  components: { ImageUpload },
  props: {
    processId: undefined,
    processCode: { type: String, default: "" },
    optType: undefined,
    galleryMode: { type: Boolean, default: false },
    workbenchTable: { type: Boolean, default: false },
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      processcontentList: [],
      title: "",
      open: false,
      previewVisible: false,
      previewUrl: "",
      previewCaption: "",
      previewOrderNum: null,
      previewIndex: -1,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processId: null,
      },
      form: {},
      rules: {
        processId: [{ required: true, message: "工序不能为空", trigger: "blur" }],
      },
    };
  },
  computed: {
    displayList() {
      if (shouldUseDemoSteps(this.processcontentList, this.processCode, this.optType)) {
        return getDemoProcessSteps(this.processCode);
      }
      return this.processcontentList;
    },
    previewItems() {
      return this.displayList
        .map((row) => ({
          row,
          url: this.stepImage(row),
        }))
        .filter((item) => item.url);
    },
    hasNextPreview() {
      return this.previewIndex >= 0 && this.previewIndex < this.previewItems.length - 1;
    },
  },
  watch: {
    processId: {
      immediate: true,
      handler(val) {
        this.queryParams.processId = val;
        if (this.galleryMode || this.workbenchTable) this.queryParams.pageSize = 50;
        if (val) this.getList();
        else {
          this.processcontentList = [];
          this.total = 0;
          this.loading = false;
        }
      },
    },
  },
  methods: {
    stepImage(row) {
      return resolveStepImage(row, this.processCode);
    },
    openPreview(row) {
      const url = this.stepImage(row);
      if (!url) return;
      const index = this.previewItems.findIndex(
        (item) => item.row.contentId === row.contentId && item.row.orderNum === row.orderNum,
      );
      this.previewIndex = index >= 0 ? index : 0;
      this.previewUrl = url;
      this.previewCaption = row.contentText || "";
      this.previewOrderNum = row.orderNum;
      this.previewVisible = true;
    },
    closePreview() {
      this.previewVisible = false;
    },
    onPreviewClosed() {
      this.previewUrl = "";
      this.previewCaption = "";
      this.previewOrderNum = null;
      this.previewIndex = -1;
    },
    showNextPreview() {
      if (!this.hasNextPreview) return;
      const next = this.previewItems[this.previewIndex + 1];
      this.previewIndex += 1;
      this.previewUrl = next.url;
      this.previewCaption = next.row.contentText || "";
      this.previewOrderNum = next.row.orderNum;
    },
    getList() {
      if (!this.queryParams.processId) return;
      this.loading = true;
      listProcesscontent(this.queryParams).then((response) => {
        this.processcontentList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      }).catch(() => {
        this.processcontentList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        contentId: null,
        processId: this.processId,
        orderNum: 1,
        contentText: null,
        device: null,
        material: null,
        docUrl: null,
        remark: null,
      };
      this.resetForm("form");
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.contentId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加操作步骤";
    },
    handleUpdate(row) {
      if (row._demo) return;
      this.reset();
      const contentId = row.contentId || this.ids;
      getProcesscontent(contentId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = "修改操作步骤";
      });
    },
    handleImgUploaded(file) {
      this.form.docUrl = file.url;
    },
    handleImgRemoved() {
      this.form.docUrl = null;
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const req = this.form.contentId != null ? updateProcesscontent(this.form) : addProcesscontent(this.form);
        req.then(() => {
          this.$modal.msgSuccess(this.form.contentId != null ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      if (row._demo) return;
      const contentIds = row.contentId || this.ids;
      this.$modal.confirm("是否确认删除操作步骤？").then(() => delProcesscontent(contentIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
  },
};
</script>
