<template>
    <el-dialog title="设备点检保养项目选择"
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
  
      <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="dvsubjectList"  @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="55" align="center" >
            <template #default="scope">
                <el-radio v-model="selectedId" :label="scope.row.subjectId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
        </el-table-column>
        <el-table-column label="项目编码" align="center" prop="subjectCode" />
        <el-table-column label="项目名称" align="center" prop="subjectName" />
        <el-table-column label="项目内容" align="center" prop="subjectContent" :show-overflow-tooltip="true"/>
        <el-table-column label="标准" align="center" prop="subjectStandard" :show-overflow-tooltip="true"/>
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
        // 选中数组
        selectedId: undefined,
        selectedRow: undefined,
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

        handleCurrent(row){
            if(row){
                this.selectedRow = row;
            }
        },
        //行双击选中
        handleRowDbClick(row){
            if(row){
                this.selectedRow = row;
                this.$emit('onSelected',this.selectedRow);
                this.showFlag = false;
            }
        },
        // 单选选中数据
        handleRowChange(row) {
            if(row){
                this.selectedRow = row;
            }
        },

      //确定选中
      confirmSelect(){
          if(this.selectedId == null || this.selectedId == 0){
              this.$notify({
                  title:'提示',
                  type:'warning',
                  message: '请选择一条数据!'
              });
              return;
          }
          this.$emit('onSelected',this.selectedRow);
          this.showFlag = false;
      }
    }
  };
  </script>
  