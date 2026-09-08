<template>
  <el-dialog title="项目选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal= false
    width="80%"
    center
  >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目编码" prop="subjectCode">
        <el-input
          v-model="queryParams.subjectCode"
          placeholder="请输入项目编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="项目名称" prop="subjectName">
        <el-input
          v-model="queryParams.subjectName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="dvsubjectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="项目编码" align="center" prop="subjectCode" />
      <el-table-column label="项目类型" align="center" prop="subjectType">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_dvsubject_type" :value="scope.row.subjectType"/>
        </template>
      </el-table-column>
      <el-table-column label="项目内容" align="center" prop="subjectContent" :show-overflow-tooltip="true"/>
      <el-table-column label="标准" align="center" prop="subjectStandard" :show-overflow-tooltip="true"/>
      <el-table-column label="是否启用" align="center" prop="enableFlag">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
    <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import { listDvsubject} from "@/yunshu-ui/api/mes/dv/dvsubject";
export default {
  name: "DvsubjectSelect",
  dicts: ['sys_yes_no', 'mes_dvsubject_type'],
  props:{
      subjectType: null,
  },
  data() {
    return {
      showFlag:false,
      // 遮罩层
      loading: true,
      // 选中数组s
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 设备点检保养项目表格数据
      dvsubjectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        subjectCode: null,
        subjectName: null,
        subjectType: this.subjectType,
        subjectContent: null,
        subjectStandard: null,
        enableFlag: null,
      },
      // 表单参数
      form: {}
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询设备点检保养项目列表 */
    getList() {
      this.loading = true;
      listDvsubject(this.queryParams).then(response => {
        this.dvsubjectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {}
      this.queryParams.pageNum = 1;
      this.queryParams.pageSize = 10;
      this.queryParams.subjectType = this.subjectType
      this.getList();
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.queryParams.pageSize = 10;
      this.getList();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.subjectId)
      this.selectedRows = selection;
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    //确定选中
    confirmSelect(){
        if(this.ids ==[] || this.ids.length==0){
            this.$notify({
                title:'提示',
                type:'warning',
                message: '请至少选择一条数据!'
            });
            return;
        }
        this.$emit('onSelected',this.selectedRows);
        this.showFlag = false;
    }
  }
};
</script>
