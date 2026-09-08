<template>
  <div class="app-container inbound-doc-panel route-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        class="inbound-filter-form"
        label-width="0"
        @submit.prevent
      >
        <el-form-item prop="routeCode">
          <el-input v-model="queryParams.routeCode" placeholder="工艺路线编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="routeName">
          <el-input v-model="queryParams.routeName" placeholder="工艺路线名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="enableFlag">
          <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable>
            <el-option
              v-for="dict in dict.type.sys_yes_no"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">工艺流程</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:pro:proroute:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:pro:proroute:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:pro:proroute:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:pro:proroute:export']">导出</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table route-doc-table"
        stripe
        border
        height="100%"
        :data="prorouteList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="工艺路线编号" align="center" header-align="center" min-width="130" prop="routeCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click="handleView(scope.row)">{{ scope.row.routeCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="工艺路线名称" align="center" header-align="center" min-width="128" prop="routeName" show-overflow-tooltip />
        <el-table-column label="工序数" align="center" header-align="center" width="72">
          <template #default="scope">
            <span class="route-step-count">{{ stepCount(scope.row.routeId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="流程概览" align="center" header-align="center" min-width="280" class-name="col-route-preview">
          <template #default="scope">
            <RouteFlowCanvas
              mini
              :max-nodes="3"
              :steps="routeStepsMap[scope.row.routeId] || []"
              :link-types="dict.type.mes_link_type"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="80" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag === 'Y' ? 'Y' : 'N'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.enableFlag === 'N'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:pro:proroute:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                :icon="scope.row.enableFlag === 'Y' ? 'el-icon-circle-close' : 'el-icon-circle-check'"
                @click="toggleEnable(scope.row)"
                v-hasPermi="['mes:pro:proroute:edit']"
              >{{ scope.row.enableFlag === 'Y' ? '停用' : '启用' }}</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.enableFlag === 'N'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:pro:proroute:remove']"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total > 0"
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

    <el-dialog :title="title" v-model="open" width="1200px" append-to-body destroy-on-close class="route-detail-dialog">
      <div v-if="dialogSteps.length" class="route-detail-dialog__flow">
        <RouteFlowCanvas :steps="dialogSteps" :link-types="dict.type.mes_link_type" />
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" :disabled="optType === 'view'">
        <el-row>
          <el-col :span="8">
            <el-form-item label="编号" prop="routeCode">
              <el-input v-model="form.routeCode" placeholder="请输入工艺路线编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="80">
              <el-switch
                v-model="autoGenFlag"
                active-color="#13ce66"
                active-text="自动生成"
                @change="handleAutoGenChange(autoGenFlag)"
                v-if="optType !== 'view'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="名称" prop="routeName">
              <el-input v-model="form.routeName" placeholder="请输入工艺路线名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="说明" prop="routeDesc">
              <el-input v-model="form.routeDesc" type="textarea" placeholder="请输入内容" />
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
      <el-tabs type="border-card" v-if="form.routeId != null" class="route-detail-dialog__tabs">
        <el-tab-pane label="组成工序">
          <Routeprocess v-if="form.routeId != null" :optType="optType" :routeId="form.routeId" @steps-changed="loadDialogSteps" />
        </el-tab-pane>
        <el-tab-pane label="关联产品">
          <Routeproduct v-if="form.routeId != null" :optType="optType" :routeId="form.routeId" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">保 存</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listProroute, getProroute, delProroute, addProroute, updateProroute, summarizeProroute } from "@/yunshu-ui/api/mes/pro/proroute";
import { listRouteprocess } from "@/yunshu-ui/api/mes/pro/routeprocess";
import Routeprocess from "./routeprocess.vue";
import Routeproduct from "./product.vue";
import RouteFlowCanvas from "./RouteFlowCanvas.vue";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";

export default {
  name: "Proroute",
  dicts: ["sys_yes_no", "mes_link_type"],
  components: { Routeprocess, Routeproduct, RouteFlowCanvas },
  data() {
    return {
      autoGenFlag: false,
      optType: undefined,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      prorouteList: [],
      routeStepsMap: {},
      dialogSteps: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        routeCode: null,
        routeName: null,
        enableFlag: null,
      },
      form: {},
      rules: {
        routeCode: [
          { required: true, message: "工艺路线编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" },
        ],
        routeName: [
          { required: true, message: "工艺路线名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" },
        ],
        enableFlag: [{ required: true, message: "是否启用不能为空", trigger: "blur" }],
        remark: [{ max: 250, message: "长度必须小于250个字符", trigger: "blur" }],
        routeDesc: [{ max: 250, message: "字段过长", trigger: "blur" }],
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listProroute(this.queryParams).then((response) => {
        this.prorouteList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
        this.loadRouteSummaries(this.prorouteList);
      });
    },
    loadRouteSummaries(routes) {
      const ids = (routes || []).map((r) => r.routeId).filter(Boolean);
      if (!ids.length) {
        this.routeStepsMap = {};
        return;
      }
      summarizeProroute(ids).then((response) => {
        this.routeStepsMap = response.data || {};
      }).catch(() => {
        this.routeStepsMap = {};
      });
    },
    stepCount(routeId) {
      const steps = this.routeStepsMap[routeId] || this.routeStepsMap[String(routeId)] || [];
      return steps.length;
    },
    toggleEnable(row) {
      const next = row.enableFlag === "Y" ? "N" : "Y";
      const text = next === "Y" ? "启用" : "停用";
      this.$modal.confirm(`确认要"${text}""${row.routeName}"工艺吗？`).then(() => updateProroute({ ...row, enableFlag: next })).then(() => {
        row.enableFlag = next;
        this.$modal.msgSuccess(`${text}成功`);
      }).catch(() => {});
    },
    loadDialogSteps() {
      if (!this.form.routeId) {
        this.dialogSteps = [];
        return;
      }
      listRouteprocess({ routeId: this.form.routeId, pageNum: 1, pageSize: 200 }).then((response) => {
        this.dialogSteps = response.rows || [];
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        routeId: null,
        routeCode: null,
        routeName: null,
        routeDesc: null,
        enableFlag: "N",
        remark: null,
      };
      this.dialogSteps = [];
      this.autoGenFlag = false;
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.routeId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加工艺路线";
      this.optType = "add";
    },
    handleView(row) {
      this.reset();
      const routeId = row.routeId || this.ids;
      getProroute(routeId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = "查看工艺线路信息";
        this.optType = "view";
        this.loadDialogSteps();
      });
    },
    handleUpdate(row) {
      this.reset();
      const routeId = row.routeId || this.ids;
      getProroute(routeId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = "修改工艺路线";
        this.optType = "edit";
        this.loadDialogSteps();
      });
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        if (this.form.routeId != null) {
          updateProroute(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.getList();
          });
        } else {
          addProroute(this.form).then((response) => {
            this.form.routeId = response.data.routeId;
            this.$modal.msgSuccess("新增成功");
            this.getList();
            this.loadDialogSteps();
          });
        }
      });
    },
    handleDelete(row) {
      const routeIds = row.routeId || this.ids;
      this.$modal.confirm('是否确认删除工艺路线编号为"' + routeIds + '"的数据项？').then(() => delProroute(routeIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleEnableFlagChange(row) {
      const text = row.enableFlag === "N" ? "停用" : "启用";
      this.$modal.confirm('确认要"' + text + '""' + row.routeName + '"工艺吗？').then(() => updateProroute(row)).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(() => {
        row.enableFlag = row.enableFlag === "N" ? "Y" : "N";
      });
    },
    handleExport() {
      this.download("pro/proroute/export", { ...this.queryParams }, `proroute_${new Date().getTime()}.xlsx`);
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) {
        genCode("ROUTE_CODE").then((response) => {
          this.form.routeCode = response;
        });
      } else {
        this.form.routeCode = null;
      }
    },
  },
};
</script>
