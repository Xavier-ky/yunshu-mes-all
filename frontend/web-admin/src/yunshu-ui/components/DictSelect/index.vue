<template>
  <el-dialog title="供应商选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal= false
    width="80%"
    center
    >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input
            v-model="queryParams.dictName"
            placeholder="请输入字典名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input
            v-model="queryParams.dictType"
            placeholder="请输入字典类型"
            clearable
            style="width: 240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="字典状态"
            clearable
            style="width: 240px"
          >
            <el-option
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            style="width: 240px"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      
      <el-table v-loading="loading" :data="typeList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
        <el-table-column  width="55" align="center" >
            <template v-slot="scope">
                <el-radio v-model="selectedDictId" :label="scope.row.dictId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
            </template>
        </el-table-column>
        <el-table-column label="字典编号" align="center" prop="dictId" />
        <el-table-column label="字典名称" align="center" prop="dictName" :show-overflow-tooltip="true" />
        <el-table-column label="字典类型" align="center" :show-overflow-tooltip="true">
          <template #default="scope">
              <span>{{ scope.row.dictType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="confirmSelect">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
      </div></template>
    </el-dialog>
  </template>
  
  <script>
  import { listType, getType, delType, addType, updateType, refreshCache } from "@/yunshu-ui/api/system/dict/type";
  
  export default {
    name: "Dict",
    dicts: ['sys_normal_disable'],
    data() {
      return {
        showFlag:false,
        selectedDictId: undefined,
        selectedRow: undefined,
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
        // 字典表格数据
        typeList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 日期范围
        dateRange: [],
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          dictName: undefined,
          dictType: undefined,
          status: undefined
        },
      };
    },
    created() {
      this.getList();
    },
    methods: {
      /** 查询字典类型列表 */
      getList() {
        this.loading = true;
        listType(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
            this.typeList = response.rows;
            this.total = response.total;
            this.loading = false;
          }
        );
      },
      /** 搜索按钮操作 */
      handleQuery() {
        this.queryParams.pageNum = 1;
        this.getList();
      },
      /** 重置按钮操作 */
      resetQuery() {
        this.dateRange = [];
        this.resetForm("queryForm");
        this.handleQuery();
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
            if(this.selectedDictId ==null || this.selectedDictId==0){
                this.$notify({
                    title:'提示',
                    type:'warning',
                    message: '请至少选择一条数据!'
                });
                return;
            }
            this.$emit('onSelected',this.selectedRow);
            this.showFlag = false;
        }
    }
  };
  </script>