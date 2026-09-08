<template>
  <div class="app-container">
    <el-row v-if="optType!= 'view'" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['qc:qcresult:add']"
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
          v-hasPermi="['qc:qcresult:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['qc:qcresult:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="qcresultList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="样品编号" align="center" prop="resultCode" />
      <el-table-column label="对应的物资SN" align="center" prop="snCode" />
      <el-table-column label="操作" v-if="optType != 'view'" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['qc:qcresult:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['qc:qcresult:remove']"
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

    <!-- 添加或修改检测结果记录对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="样品编号" prop="resultCode">
              <el-input v-model="form.resultCode" placeholder="请输入记录编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view'" >               
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SN" prop="snCode">
              <el-input v-model="form.snCode" placeholder="请输入对应的物资SN" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider>检测值</el-divider>
        <div v-for="(item, index) in form.items" :key="index">
          <el-row>
            <el-col :span="12">
              <el-form-item :label="'检测项' + (index + 1)" :prop="'items.' + index + '.indexName'">
                <el-input v-model="item.indexName" readonly="readonly"  />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item v-if="item.qcResultType =='TEXT'" label="检测值" :prop="'items.' + index + '.qcValText'">
                <el-input type="textarea" v-model="item.qcValText" placeholder="请输入检测值" />
              </el-form-item>
              <el-form-item v-else-if="item.qcResultType =='FLOAT'" label="检测值" :prop="'items.' + index + '.qcValFloat'">
                <el-input v-model="item.qcValFloat" placeholder="请输入检测值" />
              </el-form-item>
              <el-form-item v-else-if="item.qcResultType =='INTEGER'" label="检测值" :prop="'items.' + index + '.qcValInteger'">
                <el-input v-model="item.qcValInteger" placeholder="请输入检测值" />
              </el-form-item>
              <el-form-item v-else-if="item.qcResultType =='DICT'" label="检测值" :prop="'items.' + index + '.qcValDict'">
                <DictDataSelect  :dictName="item.qcResultSpc" v-model="item.qcValDict" :initialValue="item.qcValDict"></DictDataSelect>
              </el-form-item>
              <el-form-item v-else :label="'文件 ' + (index + 1)" :prop="'items.' + index + '.qcValFile'">
                <ImageUpload v-if="item.qcResultType =='FILE' && item.qcResultSpc == 'IMG'" v-model="item.qcValFile" :limit="1" :fileSize="10" :fileType="['jpg','png','jpeg']" @onUploaded="(file) => handleUploaded(file,index)" @onRemoved="(file) => handleRemoved(file,index)" ></ImageUpload>
                <FileUpload v-else="item.qcResultType == 'FILE' && item.qcResultSpc == 'FILE'"  v-model="item.qcValFile" :limit="1" :fileSize="10" :fileType="['text','doc','docx','excel','mp4']" @onUploaded="(file) => handleUploaded(file,index)" @onRemoved="(file) => handleRemoved(file,index)"></FileUpload>
              </el-form-item>
            </el-col>
          </el-row>
          <el-divider></el-divider>
        </div>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listQcresult, getQcresult, delQcresult, addQcresult, updateQcresult } from "@/yunshu-ui/api/mes/qc/qcresult";
import { listQcresultdetail, listDetails ,getQcresultdetail, delQcresultdetail, addQcresultdetail, updateQcresultdetail } from "@/yunshu-ui/api/mes/qc/qcresultdetail";
import DictDataSelect from "@/yunshu-ui/components/DictSelect/dictOptionSelect.vue"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  autoGenFlag : false,
  name: "Qcresult",
  props:{
    qcId: null,
    qcType: null,
    qcDetailType: null,
    optType: null,
  },
  components: { DictDataSelect },
  data() {
    return {
      optType2: this.optType,
      autoGenFlag : false,
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
      // 检测结果记录表格数据
      qcresultList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        resultCode: null,        sourceDocId: this.qcId,        sourceDocCode: null,        sourceDocName: null,        sourceDocType: this.qcType,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        snCode: null,      },
      
       queryParams2: {
          qcId:this.qcId,
          qcType:this.qcType,
          resultId: null
       },
        // 表单参数
      form: {

      },
      // 表单校验
      rules: {
        resultCode: [
          { required: true, message: "记录编号不能为空", trigger: "blur" }
        ],        itemId: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询检测结果记录列表 */
    getList() {
      this.loading = true;
      listQcresult(this.queryParams).then(response => {
        this.qcresultList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },

    /** 查询检测结果明细记录列表 
     * 此处无论是否有传递resultId参数，都会返回所有检查项的结果记录明细列表
    */
    getDetailList() {
      this.loading = true;
      listDetails(this.queryParams2).then(response => {
        debugger;
        this.form.items = response.data;
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
        resultId: null,        resultCode: null,        sourceDocId: this.qcId,        sourceDocCode: null,        sourceDocName: null,        sourceDocType: this.qcType,        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        snCode: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null,      
        items: [],
      };
      this.autoGenFlag = false;
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
      this.ids = selection.map(item => item.resultId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加检测结果记录";
      this.optType2 = "add";
      this.getDetailList();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const resultId = row.resultId || this.ids
      getQcresult(resultId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改检测结果记录";
        this.optType2 = "edit";
        this.queryParams2.resultId = this.form.resultId
        this.getDetailList();
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          debugger;
          if (this.form.resultId != null) {
            updateQcresult(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addQcresult(this.form).then(response => {
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
      const resultIds = row.resultId || this.ids;
      this.$modal.confirm('是否确认删除检测结果记录编号为"' + resultIds + '"的数据项？').then(function() {
        return delQcresult(resultIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    },
    /** 上传完成 */
    handleUploaded(url,index){
      this.form.items[index].qcValFile = url;
    },
    /** 移除上传文件 */
    handleRemoved(url,index){
      this.form.items[index].qcValFile = null;
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_RESULT_CODE').then(response =>{
          this.form.resultCode = response;
        });
      }else{
        this.form.resultCode = null;
      }
    },
  }
};
</script>
