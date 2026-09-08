<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-if="optType !='view'"
          v-hasPermi="['mes:dv:checkplan:add']"
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
          v-if="optType !='view'"
          v-hasPermi="['mes:dv:checkplan:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>
    <DvsubjectSelect ref="subjectSelect" subjectType="CHECK" @onSelected="onSubjectSelected"></DvsubjectSelect>

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="checksubjectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />      
      <el-table-column label="项目名称" align="center" prop="subjectName" width="150px"/>
      <el-table-column label="项目内容" align="center" width="350px" prop="subjectContent" :show-overflow-tooltip="true"/>
      <el-table-column label="标准" align="center" width="300px" prop="subjectStandard" :show-overflow-tooltip="true"/>
      <el-table-column label="操作" align="center" v-if="optType !='view'" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:dv:checkplan:remove']"
          >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listChecksubject, getChecksubject, delChecksubject, addChecksubject, updateChecksubject } from "@/yunshu-ui/api/mes/dv/checksubject";
import DvsubjectSelect from "@/yunshu-ui/components/dvsubjectSelect/multi.vue"
export default {
  name: "Checksubject",
  props:{
      planId: null,
      optType: null
  },
  components:{DvsubjectSelect},
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
        selectedRows: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 点检项目表格数据
      checksubjectList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planId: this.planId,
        subjectId: null,
        subjectCode: null,
        subjectName: null,
        subjectType: null,
        subjectContent: null,
        subjectStandard: null,
      },
      // 表单参数
      form: {}
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询点检项目列表 */
    getList() {
      this.loading = true;
      listChecksubject(this.queryParams).then(response => {
        this.checksubjectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
        this.$refs.subjectSelect.showFlag = true;
    },
    onSubjectSelected(rows){
        if(rows != null && rows.length >0){
            rows.forEach(row => {
                row.planId= this.planId;
                addChecksubject(row).then(response => {
                    this.getList();
                });
            });
        }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除点检项目编号为"' + recordIds + '"的数据项？').then(function() {
        return delChecksubject(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },

  }
};
</script>
