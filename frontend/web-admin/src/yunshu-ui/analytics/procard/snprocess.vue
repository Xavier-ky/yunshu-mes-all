<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="SNID" prop="snId">
        <el-input
          v-model="queryParams.snId"
          placeholder="请输入SNID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="SN编号" prop="snCode">
        <el-input
          v-model="queryParams.snCode"
          placeholder="请输入SN编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="序号" prop="seqNum">
        <el-input
          v-model="queryParams.seqNum"
          placeholder="请输入序号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工序ID" prop="processId">
        <el-input
          v-model="queryParams.processId"
          placeholder="请输入工序ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工序编号" prop="processCode">
        <el-input
          v-model="queryParams.processCode"
          placeholder="请输入工序编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input
          v-model="queryParams.processName"
          placeholder="请输入工序名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="进入工序时间" prop="inputTime">
        <el-date-picker clearable
          v-model="queryParams.inputTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择进入工序时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="出工序时间" prop="outputTime">
        <el-date-picker clearable
          v-model="queryParams.outputTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择出工序时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="投入数量" prop="quantityInput">
        <el-input
          v-model="queryParams.quantityInput"
          placeholder="请输入投入数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产出数量" prop="quantityOutput">
        <el-input
          v-model="queryParams.quantityOutput"
          placeholder="请输入产出数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="不合格品数量" prop="quantityUnquanlify">
        <el-input
          v-model="queryParams.quantityUnquanlify"
          placeholder="请输入不合格品数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工作站ID" prop="workstationId">
        <el-input
          v-model="queryParams.workstationId"
          placeholder="请输入工作站ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工作站编号" prop="workstationCode">
        <el-input
          v-model="queryParams.workstationCode"
          placeholder="请输入工作站编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="工作站名称" prop="workstationName">
        <el-input
          v-model="queryParams.workstationName"
          placeholder="请输入工作站名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名" prop="userName">
        <el-input
          v-model="queryParams.userName"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="名称" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="过程检验单ID" prop="ipqcId">
        <el-input
          v-model="queryParams.ipqcId"
          placeholder="请输入过程检验单ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['pro:prosnprocess:add']"
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
          v-hasPermi="['pro:prosnprocess:edit']"
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
          v-hasPermi="['pro:prosnprocess:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['pro:prosnprocess:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="prosnprocessList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流水ID" align="center" prop="recordId" />
      <el-table-column label="SNID" align="center" prop="snId" />
      <el-table-column label="SN编号" align="center" prop="snCode" />
      <el-table-column label="序号" align="center" prop="seqNum" />
      <el-table-column label="工序ID" align="center" prop="processId" />
      <el-table-column label="工序编号" align="center" prop="processCode" />
      <el-table-column label="工序名称" align="center" prop="processName" />
      <el-table-column label="进入工序时间" align="center" prop="inputTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.inputTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="出工序时间" align="center" prop="outputTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.outputTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投入数量" align="center" prop="quantityInput" />
      <el-table-column label="产出数量" align="center" prop="quantityOutput" />
      <el-table-column label="不合格品数量" align="center" prop="quantityUnquanlify" />
      <el-table-column label="工作站ID" align="center" prop="workstationId" />
      <el-table-column label="工作站编号" align="center" prop="workstationCode" />
      <el-table-column label="工作站名称" align="center" prop="workstationName" />
      <el-table-column label="用户ID" align="center" prop="userId" />
      <el-table-column label="用户名" align="center" prop="userName" />
      <el-table-column label="名称" align="center" prop="nickName" />
      <el-table-column label="过程检验单ID" align="center" prop="ipqcId" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['pro:prosnprocess:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['pro:prosnprocess:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改SN流转-工序信息对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="SNID" prop="snId">
          <el-input v-model="form.snId" placeholder="请输入SNID" />
        </el-form-item>
        <el-form-item label="SN编号" prop="snCode">
          <el-input v-model="form.snCode" placeholder="请输入SN编号" />
        </el-form-item>
        <el-form-item label="序号" prop="seqNum">
          <el-input v-model="form.seqNum" placeholder="请输入序号" />
        </el-form-item>
        <el-form-item label="工序ID" prop="processId">
          <el-input v-model="form.processId" placeholder="请输入工序ID" />
        </el-form-item>
        <el-form-item label="工序编号" prop="processCode">
          <el-input v-model="form.processCode" placeholder="请输入工序编号" />
        </el-form-item>
        <el-form-item label="工序名称" prop="processName">
          <el-input v-model="form.processName" placeholder="请输入工序名称" />
        </el-form-item>
        <el-form-item label="进入工序时间" prop="inputTime">
          <el-date-picker clearable
            v-model="form.inputTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择进入工序时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="出工序时间" prop="outputTime">
          <el-date-picker clearable
            v-model="form.outputTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择出工序时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="投入数量" prop="quantityInput">
          <el-input v-model="form.quantityInput" placeholder="请输入投入数量" />
        </el-form-item>
        <el-form-item label="产出数量" prop="quantityOutput">
          <el-input v-model="form.quantityOutput" placeholder="请输入产出数量" />
        </el-form-item>
        <el-form-item label="不合格品数量" prop="quantityUnquanlify">
          <el-input v-model="form.quantityUnquanlify" placeholder="请输入不合格品数量" />
        </el-form-item>
        <el-form-item label="工作站ID" prop="workstationId">
          <el-input v-model="form.workstationId" placeholder="请输入工作站ID" />
        </el-form-item>
        <el-form-item label="工作站编号" prop="workstationCode">
          <el-input v-model="form.workstationCode" placeholder="请输入工作站编号" />
        </el-form-item>
        <el-form-item label="工作站名称" prop="workstationName">
          <el-input v-model="form.workstationName" placeholder="请输入工作站名称" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="名称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="过程检验单ID" prop="ipqcId">
          <el-input v-model="form.ipqcId" placeholder="请输入过程检验单ID" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProsnprocess, getProsnprocess, delProsnprocess, addProsnprocess, updateProsnprocess } from "@/yunshu-ui/api/mes/pro/prosnprocess";

export default {
  name: "Prosnprocess",
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
      // SN流转-工序信息表格数据
      prosnprocessList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        snId: null,        snCode: null,        seqNum: null,        processId: null,        processCode: null,        processName: null,        inputTime: null,        outputTime: null,        quantityInput: null,        quantityOutput: null,        quantityUnquanlify: null,        workstationId: null,        workstationCode: null,        workstationName: null,        userId: null,        userName: null,        nickName: null,        ipqcId: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        snId: [
          { required: true, message: "SNID不能为空", trigger: "blur" }
        ],        workstationId: [
          { required: true, message: "工作站ID不能为空", trigger: "blur" }
        ],        userId: [
          { required: true, message: "用户ID不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询SN流转-工序信息列表 */
    getList() {
      this.loading = true;
      listProsnprocess(this.queryParams).then(response => {
        this.prosnprocessList = response.rows;
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
        recordId: null,        snId: null,        snCode: null,        seqNum: null,        processId: null,        processCode: null,        processName: null,        inputTime: null,        outputTime: null,        quantityInput: null,        quantityOutput: null,        quantityUnquanlify: null,        workstationId: null,        workstationCode: null,        workstationName: null,        userId: null,        userName: null,        nickName: null,        ipqcId: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加SN流转-工序信息";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getProsnprocess(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改SN流转-工序信息";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateProsnprocess(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProsnprocess(this.form).then(response => {
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
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除SN流转-工序信息编号为"' + recordIds + '"的数据项？').then(function() {
        return delProsnprocess(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('pro/prosnprocess/export', {
        ...this.queryParams
      }, `prosnprocess_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
