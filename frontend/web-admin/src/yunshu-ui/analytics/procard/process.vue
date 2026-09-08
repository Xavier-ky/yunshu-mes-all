<template>
  <div class="app-container">

    <el-table v-loading="loading" :data="procardprocessList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" prop="seqNum" />
      <el-table-column label="工序名称" align="center" prop="processName" />
      <el-table-column label="进入工序时间" align="center" prop="inputTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.inputTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="出工序时间" align="center" prop="outputTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.outputTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投入数量" align="center" prop="quantityInput" />
      <el-table-column label="产出数量" align="center" prop="quantityOutput" />
      <el-table-column label="不良品数量" width="120px" align="center" prop="quantityUnquanlify" />
      <el-table-column label="工作站" align="center" prop="workstationName" />
      <el-table-column label="名称" align="center" prop="nickName" />
      <el-table-column label="备注" align="center" prop="remark" />
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listProcardprocess } from "@/yunshu-ui/api/mes/pro/procardprocess";

export default {
  name: "Procardprocess",
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
      // 工序流转卡-工序信息表格数据
      procardprocessList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        cardId: this.cardId,        cardCode: null,        seqNum: null,        processId: null,        processCode: null,        processName: null,        inputTime: null,        outputTime: null,        quantityInput: null,        quantityOutput: null,        quantityUnquanlify: null,        workstationId: null,        workstationCode: null,        workstationName: null,        userId: null,        userName: null,        nickName: null,        ipqcId: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,      },
    };
  },
  props :{
      cardId: undefined,
      optType: undefined
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询工序流转卡-工序信息列表 */
    getList() {
      this.loading = true;
      listProcardprocess(this.queryParams).then(response => {
        this.procardprocessList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 表单重置
    reset() {
      this.form = {
        recordId: null,        cardId: null,        cardCode: null,        seqNum: null,        processId: null,        processCode: null,        processName: null,        inputTime: null,        outputTime: null,        quantityInput: null,        quantityOutput: null,        quantityUnquanlify: null,        workstationId: null,        workstationCode: null,        workstationName: null,        userId: null,        userName: null,        nickName: null,        ipqcId: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
  }
};
</script>
