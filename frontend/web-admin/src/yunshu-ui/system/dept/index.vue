<template>
  <div :class="embedMode ? 'inbound-doc-panel access-doc-panel' : 'app-container'">
    <template v-if="embedMode">
      <section v-show="showSearch" class="inbound-filter-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
          <el-form-item prop="deptName">
            <el-input v-model="queryParams.deptName" placeholder="部门名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <div class="inbound-toolbar">
        <div class="inbound-toolbar-left">
          <span class="inbound-page-title">部门管理</span>
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd()" v-hasPermi="['system:dept:add']">新增</el-button>
          <el-button type="info" plain icon="el-icon-sort" size="default" @click="toggleExpandAll">展开/折叠</el-button>
        </div>
        <div class="inbound-toolbar-meta">
          <span>共 {{ deptCount }} 条</span>
          <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
        </div>
      </div>

      <div class="inbound-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <el-table
          v-if="refreshTable"
          class="yunshu-data-table inbound-table"
          stripe
          border
          height="100%"
          v-loading="loading"
          :data="displayDeptList"
          row-key="deptId"
          :default-expand-all="isExpandAll"
          :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
          :row-class-name="rowClassName"
        >
          <el-table-column prop="deptName" label="部门名称" min-width="200" show-overflow-tooltip>
            <template #default="scope">
              <button type="button" class="doc-link-btn" @click.stop.prevent="handleUpdate(scope.row)">{{ scope.row.deptName }}</button>
            </template>
          </el-table-column>
          <el-table-column prop="deptCode" label="部门编码" min-width="140" show-overflow-tooltip />
          <el-table-column prop="orderNum" label="排序" min-width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" min-width="160">
            <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="200" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <div class="yunshu-row-actions">
                <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dept:edit']">修改</el-button>
                <el-button size="small" link icon="el-icon-plus" @click="handleAdd(scope.row)" v-hasPermi="['system:dept:add']">新增</el-button>
                <el-button v-if="scope.row.parentId != 0" size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dept:remove']">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <template v-else>
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="queryParams.deptName" placeholder="请输入部门名称" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="部门状态" clearable>
            <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:dept:add']">新增</el-button></el-col>
        <el-col :span="1.5"><el-button type="info" plain icon="el-icon-sort" size="mini" @click="toggleExpandAll">展开/折叠</el-button></el-col>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </el-row>
      <el-table class="yunshu-data-table" stripe border v-if="refreshTable" v-loading="loading" :data="deptList" row-key="deptId" :default-expand-all="isExpandAll" :tree-props="{children: 'children', hasChildren: 'hasChildren'}">
        <el-table-column prop="deptName" label="部门名称" width="260"></el-table-column>
        <el-table-column prop="deptCode" label="部门编码" width="260"></el-table-column>
        <el-table-column prop="orderNum" label="排序" width="200"></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="scope"><dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/></template></el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="200"><template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dept:edit']">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-plus" @click="handleAdd(scope.row)" v-hasPermi="['system:dept:add']">新增</el-button>
            <el-button v-if="scope.row.parentId != 0" size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dept:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog :title="title" v-model="open" class="access-form-dialog access-form-dialog--wide" width="720px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-form-item label="部门编码" prop="deptCode">
              <el-input v-model="form.deptCode" placeholder="请输入部门编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="自动生成" label-width="96px">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange(autoGenFlag)" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.parentId !== 0">
          <el-col :span="24">
            <el-form-item label="上级部门" prop="parentId">
              <treeselect v-model="form.parentId" :options="deptOptions" :normalizer="normalizer" placeholder="选择上级部门" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" class="access-form-dialog__number" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
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
  </div>
</template>

<script>
import { listDept, getDept, delDept, addDept, updateDept, listDeptExcludeChild } from "@/yunshu-ui/api/system/dept";
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";

export default {
  name: "Dept",
  dicts: ['sys_normal_disable'],
  components: { Treeselect },
  props: {
    embedMode: { type: Boolean, default: false },
    scopeDeptId: { type: [Number, String], default: null },
    scopeStatus: { type: String, default: null },
  },
  data() {
    return {
      autoGenFlag: false,
      loading: true,
      showSearch: true,
      deptList: [],
      deptOptions: [],
      title: "",
      open: false,
      isExpandAll: true,
      refreshTable: true,
      queryParams: { deptName: undefined, status: undefined },
      form: {},
      rules: {
        parentId: [{ required: true, message: "上级部门不能为空", trigger: "blur" }],
        deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
        deptCode: [{ required: true, message: "部门编码不能为空", trigger: "blur" }],
        orderNum: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
        email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
        phone: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
      }
    };
  },
  computed: {
    displayDeptList() {
      if (!this.embedMode || !this.scopeDeptId) return this.deptList;
      const node = this.findDeptNode(this.deptList, this.scopeDeptId);
      return node ? [node] : this.deptList;
    },
    deptCount() {
      return this.countTreeNodes(this.displayDeptList);
    },
  },
  watch: {
    scopeDeptId(val) {
      if (!this.embedMode) return;
      if (val) {
        this.isExpandAll = true;
        this.refreshTable = false;
        this.$nextTick(() => { this.refreshTable = true; });
      }
    },
    scopeStatus(val) {
      if (!this.embedMode) return;
      this.queryParams.status = val ?? undefined;
      this.handleQuery();
    },
  },
  created() {
    if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
    this.getList();
  },
  methods: {
    findDeptNode(list, id) {
      for (const item of list || []) {
        if (String(item.deptId) === String(id)) return item;
        const found = this.findDeptNode(item.children, id);
        if (found) return found;
      }
      return null;
    },
    countTreeNodes(list) {
      let n = 0;
      for (const item of list || []) {
        n += 1;
        if (item.children?.length) n += this.countTreeNodes(item.children);
      }
      return n;
    },
    rowClassName({ row }) {
      if (this.embedMode && this.scopeDeptId && String(row.deptId) === String(this.scopeDeptId)) {
        return "access-dept-row--active";
      }
      return "";
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) genCode('DEPT_CODE').then(response => { this.form.deptCode = response; });
      else this.form.deptCode = null;
    },
    getList() {
      this.loading = true;
      listDept(this.queryParams).then(response => {
        this.deptList = this.handleTree(response.data, "deptId");
      }).finally(() => { this.loading = false; });
    },
    normalizer(node) {
      if (node.children && !node.children.length) delete node.children;
      return { id: node.deptId, label: node.deptName, children: node.children };
    },
    cancel() { this.open = false; this.reset(); },
    reset() {
      this.form = { deptCode: undefined, deptId: undefined, parentId: undefined, deptName: undefined, orderNum: undefined, leader: undefined, phone: undefined, email: undefined, status: "0" };
      this.autoGenFlag = false;
      this.resetForm("form");
    },
    handleQuery() { this.getList(); },
    resetQuery() {
      this.resetForm("queryForm");
      if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
      this.handleQuery();
    },
    handleAdd(row) {
      this.reset();
      if (row != undefined) this.form.parentId = row.deptId;
      else if (this.embedMode && this.scopeDeptId) this.form.parentId = this.scopeDeptId;
      this.open = true;
      this.title = "添加部门";
      listDept().then(response => { this.deptOptions = this.handleTree(response.data, "deptId"); });
    },
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => { this.refreshTable = true; });
    },
    expandAll() {
      this.refreshTable = false;
      this.isExpandAll = true;
      this.$nextTick(() => { this.refreshTable = true; });
    },
    collapseAll() {
      this.refreshTable = false;
      this.isExpandAll = false;
      this.$nextTick(() => { this.refreshTable = true; });
    },
    handleUpdate(row) {
      this.reset();
      getDept(row.deptId).then(response => { this.form = response.data; this.open = true; this.title = "修改部门"; });
      listDeptExcludeChild(row.deptId).then(response => { this.deptOptions = this.handleTree(response.data, "deptId"); });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        const req = this.form.deptId != undefined ? updateDept(this.form) : addDept(this.form);
        req.then(() => { this.$modal.msgSuccess(this.form.deptId != undefined ? "修改成功" : "新增成功"); this.open = false; this.getList(); });
      });
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.deptName + '"的数据项？').then(function() { return delDept(row.deptId); }).then(() => {
        this.getList(); this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
:deep(.access-dept-row--active > td.el-table__cell) {
  background: #eef2ff !important;
}
</style>
