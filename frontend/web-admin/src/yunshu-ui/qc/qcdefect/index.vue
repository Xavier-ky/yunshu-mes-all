<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="缺陷描述" prop="defectName">
          <el-input
            v-model="queryParams.defectName"
            placeholder="请输入缺陷描述"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测项类型" prop="indexType">
          <el-select v-model="queryParams.indexType" placeholder="请选择检测项类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_index_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="缺陷等级" prop="defectLevel">
          <el-select v-model="queryParams.defectLevel" placeholder="请选择缺陷等级" clearable>
            <el-option
              v-for="dict in dict.type.mes_defect_level"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="workorders-toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          size="small"
          class="primary-action"
          icon="el-icon-plus"
          @click="handleAdd"
          v-hasPermi="['mes:qc:qcdefect:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:qcdefect:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:qcdefect:remove']"
        >删除</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:qc:qcdefect:export']"
        >导出</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ total }}</b> 项缺陷</span>
      </div>
      <div class="toolbar-right">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="workorders-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table workorders-table"
        stripe
        border
        :data="qcdefectList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="缺陷描述" min-width="200" align="center" header-align="center" prop="defectName" show-overflow-tooltip />
        <el-table-column label="检测项类型" min-width="120" align="center" header-align="center" prop="indexType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_index_type" :value="scope.row.indexType"/>
          </template>
        </el-table-column>
        <el-table-column label="缺陷等级" min-width="120" align="center" header-align="center" prop="defectLevel" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_defect_level" :value="scope.row.defectLevel"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" header-align="center" fixed="right" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:qc:qcdefect:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:qc:qcdefect:remove']"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改常见缺陷对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="缺陷描述" prop="defectName">
              <el-input v-model="form.defectName" type="textarea" placeholder="请输入缺陷描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="检测项类型" prop="indexType">
              <el-select v-model="form.indexType" placeholder="请选择检测项类型">
                <el-option
                  v-for="dict in dict.type.mes_index_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="缺陷等级" prop="defectLevel">
              <el-select v-model="form.defectLevel" placeholder="请选择缺陷等级" clearable>
                <el-option
                  v-for="dict in dict.type.mes_defect_level"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view'">返回</el-button>
        <el-button type="primary" @click="submitForm" v-else>确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listQcdefect, getQcdefect, delQcdefect, addQcdefect, updateQcdefect } from "@/yunshu-ui/api/mes/qc/qcdefect";

export default {
  name: "Qcdefect",
  dicts: ['mes_index_type','mes_defect_level'],
  data() {
    return {
      optType: null,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 常见缺陷表格数据
      qcdefectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        defectCode: null,
        defectName: null,
        indexType: null,
        defectLevel: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        defectName: [
          { required: true, message: "缺陷描述不能为空", trigger: "blur" },
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        indexType: [
          { required: true, message: "检测项类型不能为空", trigger: "change" }
        ],
        defectLevel: [
          { required: true, message: "缺陷等级不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询常见缺陷列表 */
    getList() {
      this.loading = true;
      listQcdefect(this.queryParams).then(response => {
        this.qcdefectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        defectId: null,
        defectCode: null,
        defectName: null,
        indexType: null,
        defectLevel: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加常见缺陷";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const defectId = row.defectId || this.ids
      getQcdefect(defectId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改常见缺陷";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.defectId != null) {
            updateQcdefect(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addQcdefect(this.form).then(response => {
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
      const defectIds = row.defectId || this.ids;
      this.$modal.confirm('是否确认删除常见缺陷编号为"' + defectIds + '"的数据项？').then(function() {
        return delQcdefect(defectIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    }
  }
};
</script>
