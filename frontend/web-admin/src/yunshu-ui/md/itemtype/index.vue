<template>
  <div class="app-container inbound-doc-panel md-doc-panel">
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
        <el-form-item prop="itemTypeName">
          <el-input
            v-model="queryParams.itemTypeName"
            placeholder="分类名称"
            clearable
            @keyup.enter="handleQuery"
          />
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
        <span class="inbound-page-title">物料分类</span>
        <el-button type="info" plain icon="el-icon-sort" size="default" @click="toggleExpandAll">展开/折叠</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-if="refreshTable"
        v-loading="loading"
        class="yunshu-data-table inbound-table md-doc-table"
        stripe
        border
        height="100%"
        :data="itemTypeList"
        row-key="itemTypeId"
        :default-expand-all="isExpandAll"
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      >
      <el-table-column prop="itemTypeName" label="分类" min-width="200" show-overflow-tooltip></el-table-column>
      <el-table-column prop="orderNum" label="排序" width="100" align="center" header-align="center"></el-table-column>
      <el-table-column prop="itemOrProduct" label="物料/产品" width="120" align="center" header-align="center">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_item_product" :value="scope.row.itemOrProduct"/>
        </template>
      </el-table-column>

      <el-table-column prop="enableFlag" label="是否启用" width="100" align="center" header-align="center">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" header-align="center" prop="createTime" min-width="160" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" header-align="center" min-width="200" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:md:itemtype:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['mes:md:itemtype:add']"
          >新增</el-button>
          <el-button
            v-if="scope.row.parentTypeId != 0"
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:md:itemtype:remove']"
          >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <!-- 添加或修改物料分类对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="24" v-if="form.parentTypeId !== 0">
            <el-form-item label="父分类" prop="parentTypeId">
              <treeselect v-model="form.parentTypeId" disabled = "disabled" :options="itemTypeOptions" :normalizer="normalizer" placeholder="请选择上级分类" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="分类名称" prop="itemTypeName">
              <el-input v-model="form.itemTypeName" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="物料/产品" prop="itemOrProduct">
              <el-radio-group v-model="form.itemOrProduct">
                <el-radio
                  v-for="dict in dict.type.mes_item_product"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-radio-group v-model="form.enableFlag">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listItemType, getItemType, delItemType, addItemType, updateItemType, listItemTypeExcludeChild } from "@/yunshu-ui/api/mes/md/itemtype";
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";

export default {
  name: "ItemType",
  dicts: ['sys_yes_no','mes_item_product'],
  components: { Treeselect },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 表格树数据
      itemTypeList: [],
      // 部门树选项
      itemTypeOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        itemTypeName: undefined,
        enableFlag: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        parentTypeId: [
          { required: true, message: "父分类不能为空", trigger: "blur" }
        ],
        itemTypeName: [
          { required: true, message: "分类名称不能为空", trigger: "blur" }
        ],
        orderNum: [
          { required: true, message: "显示排序不能为空", trigger: "blur" }
        ],
        itemOrProduct: [
          {
            required: true,
            message: "请选择是产品分类还是物料分类",
            trigger: ["blur"]
          }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询部门列表 */
    getList() {
      this.loading = true;
      listItemType(this.queryParams).then(response => {
        const rows = response.rows || [];
        this.itemTypeList = this.handleTree(rows, "itemTypeId", "parentTypeId");
        this.loading = false;
      });
    },
    /** 转换部门数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.itemTypeId,
        label: node.itemTypeName,
        children: node.children
      };
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        itemTypeId: undefined,
        parentTypeId: undefined,
        itemTypeCode: undefined,
        itemTypeName: undefined,
        orderNum: undefined,
        itemOrProduct: undefined,
        enableFlag: 'Y'
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      if (row != undefined) {
        this.form.parentTypeId = row.itemTypeId;
      }
      this.open = true;
      this.title = "添加分类";
      listItemType().then(response => {
        this.itemTypeOptions = this.handleTree(response.rows || [], "itemTypeId", "parentTypeId");
      });
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      getItemType(row.itemTypeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改分类";
      });
      listItemTypeExcludeChild(row.itemTypeId).then(response => {
        this.itemTypeOptions = this.handleTree(response.data, "itemTypeId","parentTypeId");
      });
    },
    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.itemTypeId != undefined) {
            updateItemType(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addItemType(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除名称为"' + row.itemTypeName + '"的分类？').then(function() {
        return delItemType(row.itemTypeId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
