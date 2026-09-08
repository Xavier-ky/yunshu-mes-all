<template>
  <div class="app-container">
    
    <el-table v-loading="loading" :data="oqclineList" >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检测项名称" width="120px" align="center" prop="indexName" />
      <el-table-column label="检测项类型" width="100px" align="center" prop="indexType">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_index_type" :value="scope.row.indexType"/>
        </template>
      </el-table-column>
      <el-table-column label="检测工具" align="center" prop="qcTool" />
      <el-table-column label="检测要求" width="150px" align="center" prop="checkMethod" :show-overflow-tooltip="true"/>
      <el-table-column label="标准值" align="center" prop="standerVal" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" />
      <el-table-column label="误差上限" align="center" prop="thresholdMax" />
      <el-table-column label="误差下限" align="center" prop="thresholdMin" />
      <el-table-column label="致命缺陷数量" align="center" prop="crQuantity" />
      <el-table-column label="严重缺陷数量" align="center" prop="majQuantity" />
      <el-table-column label="轻微缺陷数量" align="center" prop="minQuantity" />      
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleDefect(scope.row)"
            v-hasPermi="['mes:qc:oqc:edit']"
            v-if="optType !='view'"
          >缺陷记录</el-button>        
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
    <Defectrecord ref="defectDialog" :qcId="defect_qcid" :lineId="defect_qclineid" :qcType="defect_qctype" :optType="optType"></Defectrecord>
  </div>
</template>

<script>
import { listOqcline, getOqcline, delOqcline, addOqcline, updateOqcline } from "@/yunshu-ui/api/mes/qc/oqcline";
import Defectrecord from "../defectrecord/index.vue"
export default {
  name: "Oqcline",
  components: {Defectrecord},
  dicts: ['mes_index_type'],
  props: {
    oqcId: null,
    optType: undefined
  },
  data() {
    return {
      defect_qcid: null,
      defect_qclineid: null,
      defect_qctype: 'OQC',
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
      // 出货检验单行表格数据
      oqclineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        oqcId: this.oqcId,
        indexId: null,
        indexCode: null,
        indexName: null,
        indexType: null,
        qcTool: null,
        checkMethod: null,
        standerVal: null,
        unitOfMeasure: null,
        thresholdMax: null,
        thresholdMin: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询出货检验单行列表 */
    getList() {
      this.loading = true;
      listOqcline(this.queryParams).then(response => {
        debugger;
        this.oqclineList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    //缺陷记录
    handleDefect(row){
      this.defect_qcid = row.oqcId;      
      this.defect_qclineid = row.lineId;
      this.$nextTick(() => {
        this.$refs.defectDialog.showFlag = true;
        this.$refs.defectDialog.getList();
      })
    }
  }
};
</script>
