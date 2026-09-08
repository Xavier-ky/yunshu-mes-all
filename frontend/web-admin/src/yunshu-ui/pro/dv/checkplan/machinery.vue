<template>
  <div class="app-container">    
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          v-if="optType !='view'"
          @click="handleAdd"
          v-hasPermi="['mes:dv:checkplan:add']"
        >新增</el-button>
      </el-col>     
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          v-if="optType !='view'"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:dv:checkplan:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>
    <MachinerySelect ref="machinerySelect" @onSelected="onMachineryAdd"></MachinerySelect>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="checkmachineryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备编码" align="center" prop="machineryCode" />
      <el-table-column label="设备名称" align="center" prop="machineryName" />
      <el-table-column label="品牌" align="center" prop="machineryBrand" />
      <el-table-column label="规格型号" align="center" prop="machinerySpec" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            v-if="optType !='view'"
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
import { listCheckmachinery,  delCheckmachinery, addCheckmachinery } from "@/yunshu-ui/api/mes/dv/checkmachinery";
import MachinerySelect from "@/yunshu-ui/components/machinerySelect/index.vue";
export default {
  name: "Checkmachinery",
  components:{MachinerySelect},
  props:{
    planId: null,
    optType: null
  },
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
      // 点检设备表格数据
      checkmachineryList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planId: this.planId,
        machineryId: null,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
      },
      // 表单参数
      form: {},
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询点检设备列表 */
    getList() {
      this.loading = true;
      listCheckmachinery(this.queryParams).then(response => {
        this.checkmachineryList = response.rows;
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
      this.$refs.machinerySelect.showFlag = true;
    },
      //设备资源选择回调
    onMachineryAdd(rows){      
      if(rows !=null && rows.length >0){
         rows.forEach(row => {
            row.planId = this.planId;
            addCheckmachinery(row).then(response =>{
              this.getList();              
            });
         });
      }

    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除点检设备编号为"' + recordIds + '"的数据项？').then(function() {
        return delCheckmachinery(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    }
  }
};
</script>
