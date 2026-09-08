<template>
  <div class="app-container" :class="{ 'dv-wb-line-panel': workbenchMode, 'dv-wb-line-panel--workbench': workbenchMode }">

    <el-row :gutter="10" v-if="!workbenchMode" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['mes:dv:repair:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:dv:repair:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <div class="dv-wb-line-table-wrap">
    <el-table
      v-loading="loading"
      class="yunshu-data-table"
      :class="{ 'dv-wb-line-table': workbenchMode }"
      stripe
      border
      :data="repairlineList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="!workbenchMode" type="selection" width="55" align="center" />
      <el-table-column label="项目名称" align="center" min-width="100" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.subjectName || scope.row.subjectCode || "—" }}
        </template>
      </el-table-column>
      <el-table-column label="故障描述" align="center" prop="malfunction" min-width="140" show-overflow-tooltip />
      <el-table-column label="现场图片" align="center" width="130">
        <template #default="scope">
          <DvLineThumb :src="lineImageUrl(scope.row)" :alt="scope.row.subjectName || scope.row.malfunction" />
        </template>
      </el-table-column>
      <el-table-column label="维修情况" align="center" prop="repairDes" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" v-if="!workbenchMode" class-name="col-actions small-padding fixed-width" width="120">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:dv:repair:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:dv:repair:remove']"
          >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <pagination
      v-if="!workbenchMode"
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改设备维修单行对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="subjectCode">
          <el-select v-model="form.subjectName" @change="changeSubject" filterable placeholder="请输入项目名称" style="width: 100%">
            <el-option
              v-for="item in dvsubjectList"
              :key="item.subjectId"
              :label="item.subjectName"
              :value="item.subjectCode">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="malfunction">
          <el-input v-model="form.malfunction" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="故障描述资源" prop="malfunctionUrl">
          <el-input type="textarea" v-model="form.malfunctionUrl" placeholder="请输入故障描述资源" />
        </el-form-item>
        <el-form-item label="维修情况" prop="repairDes">
          <el-input  v-model="form.repairDes" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRepairline, getRepairline, delRepairline, addRepairline, updateRepairline } from "@/yunshu-ui/api/mes/dv/repairline";
import { listDvsubject } from "@/yunshu-ui/api/mes/dv/dvsubject";
import DvLineThumb from "@/yunshu-ui/pro/dv/workbench/components/DvLineThumb.vue";

export default {
  name: "Repairline",
  components: { DvLineThumb },
  props: {
    repairId: null,
    optType: undefined,
    workbenchMode: { type: Boolean, default: false },
  },
  emits: ["line-total"],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 项目选择
      dvsubjectList: [],
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
      // 设备维修单行表格数据
      repairlineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        repairId: null,
        subjectId: null,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
        malfunction: null,
        malfunctionUrl: null,
        repairDes: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        subjectCode: [
          { required: true, message: "项目名称不能为空", trigger: "blur" }
        ],
        malfunction: [
          { required: true, message: "故障描述不能为空", trigger: "blur" }
        ],
      },
      listSeq: 0,
    };
  },
  created() {
    if (this.workbenchMode) this.queryParams.pageSize = 100;
    this.syncRepairId(true);
    if (!this.workbenchMode) this.loadDvSubjects();
  },
  watch: {
    repairId() {
      this.syncRepairId(true);
    },
    workbenchMode(v) {
      this.queryParams.pageSize = v ? 100 : 10;
      this.getList();
    },
  },
  methods: {
    syncRepairId(reload = false) {
      this.queryParams.repairId = this.repairId;
      if (!this.repairId) {
        ++this.listSeq;
        this.repairlineList = [];
        this.total = 0;
        this.loading = false;
        this.$emit("line-total", 0);
        return;
      }
      if (reload) {
        this.repairlineList = [];
        this.getList();
      }
    },
    loadDvSubjects() {
      listDvsubject({ pageNum: 1, pageSize: 99999 }).then((res) => {
        this.dvsubjectList = res.rows || [];
      }).catch(() => {
        this.dvsubjectList = [];
      });
    },
    lineImageUrl(row) {
      return row?.malfunctionUrl || "";
    },
    // 选择项目
    changeSubject(val) {
      let data = (this.dvsubjectList.filter(item => item.subjectCode == val))[0]
      this.form.subjectId = data.subjectId
      this.form.subjectName = data.subjectName
      this.form.subjectCode = data.subjectCode
      this.form.subjectContent = data.subjectContent
    },
    /** 查询设备维修单行列表 */
    getList() {
      const seq = ++this.listSeq;
      this.loading = true;
      listRepairline(this.queryParams).then((response) => {
        if (seq !== this.listSeq) return;
        this.repairlineList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
        this.$emit("line-total", this.total);
      }).catch(() => {
        if (seq !== this.listSeq) return;
        this.repairlineList = [];
        this.total = 0;
        this.loading = false;
        this.$emit("line-total", 0);
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
        lineId: null,
        repairId: this.repairId,
        subjectId: null,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
        malfunction: null,
        malfunctionUrl: null,
        repairDes: null,
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
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
      this.ids = selection.map(item => item.lineId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.loadDvSubjects();
      this.reset();
      this.open = true;
      this.title = "添加设备维修单行";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const lineId = row.lineId || this.ids
      getRepairline(lineId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备维修单行";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {
            updateRepairline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRepairline(this.form).then(response => {
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
      const lineIds = row.lineId || this.ids;
      this.$modal.confirm('是否确认删除当前数据项？').then(function() {
        return delRepairline(lineIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('dv/repairline/export', {
        ...this.queryParams
      }, `repairline_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
