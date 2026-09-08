<template>
  <el-dialog title="缺陷记录"
    v-if="showFlag"
    v-model="showFlag"
    :modal= false
    width="80%"
    center
  >
    <el-row :gutter="10" v-if="optType !='view'" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="default"
          :disabled="single"
          @click="handleUpdate"
        >常见缺陷</el-button>
      </el-col>      
      <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="defectrecordList">
      <el-table-column label="缺陷描述" width="400px" align="center" prop="defectName" >
            <template #default="scope">
              <el-input type="textarea" v-model="scope.row.defectName"></el-input>
            </template>
      </el-table-column>
      <el-table-column label="缺陷等级" align="center" prop="defectLevel">
        <template #default="scope">
            <el-select v-model="scope.row.defectLevel">
                <el-option
                    v-for="dict in dict.type.mes_defect_level"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                />                
            </el-select>          
        </template>
      </el-table-column>
      <el-table-column label="缺陷数量" align="center" prop="defectQuantity" >
        <template #default="scope">
            <el-input-number :min="1" v-model="scope.row.defectQuantity" />
        </template>
      </el-table-column>
      <el-table-column label="操作" v-if="optType !='view'" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            v-if="optType !='view'"
            @click="handleDelete(scope.$index, scope.row)"

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
    <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view'">返回</el-button>
        <el-button type="primary"  v-else @click="confirm">确 定</el-button>
        <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import { listDefectrecord, getDefectrecord, delDefectrecord, addDefectrecord, updateDefectrecord } from "@/yunshu-ui/api/mes/qc/defectrecord";

export default {
  name: "Defectrecord",
  dicts: ['mes_defect_level'],
  props: {
    qcId: null,
    qcType: null,
    lineId:null,
    optType:null
  },
  watch :{
      qcId(){
        this.queryParams.qcId = this.qcId;        
      },
      qcType(){
        this.queryParams.qcType = this.qcType;
      },
      lineId(){
        this.queryParams.lineId = this.lineId;
      }      
  },
  data() {
    return {
      showFlag: false,
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
      // 检验单缺陷记录表格数据
      defectrecordList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        qcType: this.qcType,
        qcId: this.qcId,
        lineId: this.lineId,
        defectName: null,
        defectLevel: null,
        defectQuantity: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询检验单缺陷记录列表 */
    getList() {
      this.loading = true;
      listDefectrecord(this.queryParams).then(response => {
        this.defectrecordList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },

    /** 新增按钮操作 */
    handleAdd() {
      debugger;
      var row = {
            recordId: null,
            qcId: this.qcId,
            qcType: this.qcType,
            lineId: this.lineId,
            defectName: null,
            defectLevel: null,
            defectQuantity: null,
            flag:'add',
            remark: null
        }
        this.defectrecordList.unshift(row);
    },
    /** 选择常见缺陷 */
    handleUpdate(row) {

    },
    /** 提交按钮 */
    confirm() {
      if(this.defectrecordList.length !=0){
        updateDefectrecord(this.defectrecordList).then(res => {
          this.$modal.msgSuccess(res.msg);
          if (res.code === 200) {
            this.showFlag = false;
            this.getList();
            this.$parent.getList();
          }
        });
      }
    },
    /** 删除按钮操作 */
    handleDelete(index,row) {
        const recordIds = row.recordId || this.ids;
        let that = this;
        this.$modal.confirm('是否确认删除缺陷记录？').then(function() {
            that.defectrecordList.forEach(function(item,i,arr){
                debugger;
                if(i == index){
                  if(that.defectrecordList[index].flag=='add'){
                    //如果是刚新增的则直接删除
                    that.defectrecordList.splice(index,1);
                  }else{
                    //如果是从数据库查询的则从数据库删除
                    delDefectrecord(recordIds).then(res=>{
                      that.getList();
                    });
                  }                                      
                }
            })
        });
        this.$parent.getList();
    }

  }
};
</script>
