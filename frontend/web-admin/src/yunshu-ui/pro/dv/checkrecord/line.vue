<template>
  <div class="app-container" :class="{ 'dv-wb-line-panel': workbenchMode, 'dv-wb-line-panel--workbench': workbenchMode }">
    <el-row :gutter="10" v-if="!workbenchMode && optType !== 'view'" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['dv:checkrecordline:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['dv:checkrecordline:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['dv:checkrecordline:remove']"
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
      :data="checkrecordlineList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column v-if="!workbenchMode" type="selection" width="55" align="center" />
      <el-table-column label="项目名称" align="center" prop="subjectName" min-width="100" show-overflow-tooltip />
      <el-table-column label="检查内容" align="center" prop="subjectContent" min-width="140" :show-overflow-tooltip="true"/>
      <el-table-column label="标准" align="center" prop="subjectStandard" min-width="90" show-overflow-tooltip />
      <el-table-column label="点检结果" align="center" prop="checkStatus" width="88">
        <template #default="scope">
          <dict-tag :options="dict.type.dv_cm_result_status" :value="scope.row.checkStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="异常描述" align="center" prop="checkResult" min-width="100" :show-overflow-tooltip="true" />
      <el-table-column label="现场图片" align="center" width="130">
        <template #default="scope">
          <DvLineThumb :src="lineImageUrl(scope.row)" :alt="scope.row.subjectName" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" v-if="!workbenchMode && optType !== 'view'" class-name="col-actions small-padding fixed-width" width="120">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['dv:checkrecordline:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['dv:checkrecordline:remove']"
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

    <!-- 添加或修改设备点检记录行对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目名称" prop="subjectName">
              <el-input v-model="form.subjectName" placeholder="请选择检查项目" >
                <el-button slot="append" @click="handleSelectSubject" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <DvSubjectSelect ref="subjectSelect" subjectType="CHECK" @onSelected="onSubjectSelected"></DvSubjectSelect>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="标准" prop="subjectStandard">
              <el-input v-model="form.subjectStandard" placeholder="请输入标准" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="项目内容">
              <el-input type="textarea" v-model="form.subjectContent" :min-height="192"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="点检结果">
          <el-radio-group v-model="form.checkStatus">
            <el-radio label="Y">正常</el-radio>
            <el-radio label="N">异常</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.checkStatus === 'N'" label="异常描述" prop="checkResult">
          <el-input v-model="form.checkResult" type="textarea" placeholder="请输入内容" />
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
import { listCheckrecordline, getCheckrecordline, delCheckrecordline, addCheckrecordline, updateCheckrecordline } from "@/yunshu-ui/api/mes/dv/checkrecordline";
import DvSubjectSelect from "@/yunshu-ui/components/dvsubjectSelect/single.vue";
import DvLineThumb from "@/yunshu-ui/pro/dv/workbench/components/DvLineThumb.vue";
export default {
  name: "Checkrecordline",
  props:{
    optType: null,
    recordId: null,
    workbenchMode: { type: Boolean, default: false },
  },
  emits: ["line-total"],
  dicts: ['dv_cm_result_status'],
  components:{ DvSubjectSelect, DvLineThumb },
  data() {
    return {
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
      // 设备点检记录行表格数据
      checkrecordlineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recordId: null,
        subjectId: null,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
        checkStatus: null,
        checkResult: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recordId: [
          { required: true, message: "计划ID不能为空", trigger: "blur" }
        ],        subjectId: [
          { required: true, message: "项目ID不能为空", trigger: "blur" }
        ],        subjectCode: [
          { required: true, message: "项目编码不能为空", trigger: "blur" }
        ],        subjectContent: [
          { required: true, message: "项目内容不能为空", trigger: "blur" }
        ],        checkStatus: [
          { required: true, message: "点检结果不能为空", trigger: "blur" }
        ],      },
      listSeq: 0,
    };
  },
  created() {
    if (this.workbenchMode) this.queryParams.pageSize = 100;
    this.syncRecordId(true);
  },
  watch: {
    recordId() {
      this.syncRecordId(true);
    },
    workbenchMode(v) {
      this.queryParams.pageSize = v ? 100 : 10;
      this.getList();
    },
  },
  methods: {
    syncRecordId(reload = false) {
      this.queryParams.recordId = this.recordId;
      if (!this.recordId) {
        ++this.listSeq;
        this.checkrecordlineList = [];
        this.total = 0;
        this.loading = false;
        this.$emit("line-total", 0);
        return;
      }
      if (reload) {
        this.checkrecordlineList = [];
        this.getList();
      }
    },
    lineImageUrl(row) {
      return row?.attr2 || "";
    },
    /** 查询设备点检记录行列表 */
    getList() {
      const seq = ++this.listSeq;
      this.loading = true;
      listCheckrecordline(this.queryParams).then((response) => {
        if (seq !== this.listSeq) return;
        this.checkrecordlineList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
        this.$emit("line-total", this.total);
      }).catch(() => {
        if (seq !== this.listSeq) return;
        this.checkrecordlineList = [];
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
        lineId: null,        recordId: this.recordId,        subjectId: null,        subjectCode: null,        subjectName: null,        subjectType: null,        subjectContent: null,        subjectStandard: null,        checkStatus: "Y",        checkResult: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.reset();
      this.open = true;
      this.title = "添加设备点检记录行";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const lineId = row.lineId || this.ids
      getCheckrecordline(lineId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备点检记录行";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {            
            updateCheckrecordline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addCheckrecordline(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除设备点检记录行编号为"' + lineIds + '"的数据项？').then(function() {
        return delCheckrecordline(lineIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleSelectSubject(){
      this.$refs.subjectSelect.showFlag = true;
    },
    onSubjectSelected(obj){
      if(obj){
        this.form.subjectId = obj.subjectId;
        this.form.subjectCode = obj.subjectCode;
        this.form.subjectName = obj.subjectName;
        this.form.subjectType = obj.subjectType;
        this.form.subjectContent = obj.subjectContent;
        this.form.subjectStandard = obj.subjectStandard;
      }
    }
  }
};
</script>
