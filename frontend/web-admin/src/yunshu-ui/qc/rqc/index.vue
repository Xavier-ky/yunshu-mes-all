<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="检验单编号" prop="rqcCode">
          <el-input
            v-model="queryParams.rqcCode"
            placeholder="请输入检验单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="来源单据编号" prop="sourceDocCode">
          <el-input
            v-model="queryParams.sourceDocCode"
            placeholder="请输入来源单据编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物料" prop="itemName">
          <el-input
            v-model="queryParams.itemName"
            placeholder="请输入物料名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测结论" prop="checkResult">
          <el-select v-model="queryParams.checkResult" placeholder="请选择检测结论" clearable>
            <el-option
              v-for="dict in dict.type.mes_qc_result"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="workorders-toolbar">
      <div class="toolbar-left">
        <el-button
          type="primary"
          size="small"
          class="primary-action"
          icon="el-icon-plus"
          @click="handleAdd"
          v-hasPermi="['qc:rqc:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['qc:rqc:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['qc:rqc:remove']"
        >删除</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ total }}</b> 笔检验单</span>
      </div>
      <div class="toolbar-right">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="workorders-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table workorders-table"
        stripe
        border
        :data="rqcList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="检验单编号" min-width="148" align="center" header-align="center" prop="rqcCode" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:qc:rqc:query']"
            >{{ scope.row.rqcCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="检验单名称" min-width="136" align="center" header-align="center" prop="rqcName" show-overflow-tooltip />
        <el-table-column label="来源单据类型" min-width="120" align="center" header-align="center" prop="sourceDocType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_source_doc_type" :value="scope.row.sourceDocType"/>
          </template>
        </el-table-column>
        <el-table-column label="来源单据编号" min-width="128" align="center" header-align="center" prop="sourceDocCode" show-overflow-tooltip />
        <el-table-column label="物资编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="物资名称" min-width="136" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
        <el-table-column label="规格型号" min-width="108" align="center" header-align="center" prop="specification" show-overflow-tooltip />
        <el-table-column label="单位" min-width="72" align="center" header-align="center" prop="unitName" show-overflow-tooltip />
        <el-table-column label="批次号" min-width="100" align="center" header-align="center" prop="batchCode" show-overflow-tooltip />
        <el-table-column label="检测结论" min-width="100" align="center" header-align="center" prop="checkResult" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult"/>
          </template>
        </el-table-column>
        <el-table-column label="检测日期" min-width="116" align="center" header-align="center" prop="inspectDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.inspectDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测人员" min-width="100" align="center" header-align="center" prop="nickName" show-overflow-tooltip />
        <el-table-column label="单据状态" min-width="100" align="center" header-align="center" prop="status" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_rt_issue_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" header-align="center" fixed="right" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['qc:rqc:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['qc:rqc:remove']"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改退料检验单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="rqcCode">
              <el-input v-model="form.rqcCode" placeholder="请输入检验单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'">
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检验单名称" prop="rqcName">
              <el-input v-model="form.rqcName" placeholder="请输入检验单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验类型" prop="rqcType">
              <el-select v-model="form.rqcType" placeholder="请选择检验类型">
                <el-option
                  v-for="dict in dict.type.mes_rqc_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源单据类型" prop="sourceDocType">
              <el-select v-model="form.sourceDocType" disabled>
                <el-option
                  v-for="dict in dict.type.mes_source_doc_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源单据编号" prop="sourceDocCode">
              <el-input v-model="form.sourceDocCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="物资编码" prop="itemCode">
              <el-input v-if="form.rqcId ==null " v-model="form.itemCode" readonly placeholder="请选择物资" >
                <el-button slot="append" @click="handleSelectProduct" icon="el-icon-search"></el-button>
              </el-input>
              <!--如果已经保存过，则产品不允许再修改，需要修改就删除重做-->
              <el-input v-else v-model="form.itemCode">
              </el-input>
            </el-form-item>
            <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="物资名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请选择物资" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请选择物资" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" placeholder="请选择物资" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" placeholder="请输入批次号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测结论" prop="checkResult">
              <el-select v-model="form.checkResult" placeholder="请选择检测结论">
                <el-option
                  v-for="dict in dict.type.mes_qc_result"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测日期" prop="inspectDate">
              <el-date-picker clearable
                v-model="form.inspectDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择检测日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="检测数量" prop="quantityCheck">
              <el-input-number :min="0" :max="99999999" v-model="form.quantityCheck" placeholder="检测数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格品数量" prop="quantityQualified">
              <el-input-number :min="0" :max="99999999" v-model="form.quantityQualified" placeholder="请输入合格品数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不合格数量" prop="quantityUnqualified">
              <el-input-number :min="0" :max="99999999" v-model="form.quantityUnqualified" placeholder="请输入不合格数" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="审核人" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请选择审核人" >
                <el-button slot="append" @click="handleUser2Select" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="user2Select" @onSelected="onUser2Selected"></UserSingleSelect>
          </el-col>
          <el-col :span="8"></el-col>
          <el-col :span="8"></el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card" v-if="form.rqcId != null">
        <el-tab-pane label="检测项">
          <RqcLine ref=line :rqcId="form.rqcId" :optType="optType"></RqcLine>
        </el-tab-pane>
        <el-tab-pane label="检测结果">
          <QCResutl ref="qcResult" :qcId="form.rqcId" :qcType="'RQC'" :optType="optType"></QCResutl>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="warning" @click="submitForm" v-if="form.status =='PREPARE' && form.rqcId !=null && optType !='view' ">提 交</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRqc, getRqc, delRqc, addRqc, updateRqc } from "@/yunshu-ui/api/mes/qc/rqc";
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue"
import QCResutl from "../qcresult/index.vue";
import RqcLine from "./line.vue"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
export default {
  name: "Rqc",
  components: {ItemSelect,UserSingleSelect,RqcLine,QCResutl},
  dicts: ['mes_rqc_type','mes_qc_result','mes_source_doc_type','mes_rt_issue_status'],
  data() {
    return {
      autoGenFlag:false,
      optType: undefined,
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
      // 退料检验单表格数据
      rqcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        rqcCode: null,        rqcName: null,        templateId: null,  rqcType: null,      sourceDocId: null,        sourceDocType: null,        sourceDocCode: null,        sourceLineId: null,    itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        batchId: null,        batchCode: null,        quantityCheck: null,        quantityUnqualified: null,        quantityQualified: null,        checkResult: null,        inspectDate: null,        userId: null,        userName: null,        nickName: null,        status: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        rqcCode: [
          { required: true, message: "检验单编号不能为空", trigger: "blur" }
        ],        rqcType: [
          { required: true, message: "请选择退料检测类型", trigger: "blur" }
        ],        checkResult: [
          { required: true, message: "请选择检测结果", trigger: "blur" }
        ],        inspectDate: [
          { required: true, message: "检测日期不能为空", trigger: "blur" }
        ],        nickName: [
          { required: true, message: "请选择检测人员", trigger: "blur" }
        ],        quantityCheck: [
          { required: true, message: "请填写检测数量", trigger: "blur" }
        ],      quantityQualified: [
          { required: true, message: "请填写合格品数量", trigger: "blur" }
        ],    quantityUnqualified: [
          { required: true, message: "请填写不良品数量", trigger: "blur" }
        ],                
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询退料检验单列表 */
    getList() {
      this.loading = true;
      listRqc(this.queryParams).then(response => {
        this.rqcList = response.rows;
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
        rqcId: null,        rqcCode: null,        rqcName: null,        templateId: null,        sourceDocId: null,        sourceDocType: null,        sourceDocCode: null,        sourceLineId: null,     
        itemId: null,        itemCode: null,        itemName: null,        specification: null,        unitOfMeasure: null,        unitName: null,        batchId: null,        batchCode: null,       
         quantityCheck: null,        quantityUnqualified: 0,        quantityQualified: 0,        checkResult: null,        inspectDate: new Date(),        userId: null,        userName: null,        nickName: null,        status: "PREPARE",        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
      this.resetForm("form");
      this.autoGenFlag = false;
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
      this.ids = selection.map(item => item.rqcId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 查看操作 */
    handleView(row) {
      this.reset();
      const rqcId = row.rqcId || this.ids
      getRqc(rqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.optType = "view";
        this.title = "查看退料检验单";
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.optType = "add";
      this.title = "添加退料检验单";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const rqcId = row.rqcId || this.ids
      getRqc(rqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.optType = "edit";
        this.title = "修改退料检验单";
      });
    },
    /** 保存按钮 */
    saveForm() {
      // 判断接收总数与合格不合格数之间的校验
      if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined && this.form.quantityQualified != 0
        && this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined && this.form.quantityUnqualified != 0) {
        if ((this.form.quantityQualified + this.form.quantityUnqualified) != this.form.quantityCheck) {
          this.$message.warning("合格数与不合格数之和必须等于检测总数！");
          return
        }
      }
      if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined) {
        if (this.form.quantityQualified > this.form.quantityCheck) {
          this.$message.warning("合格数不能大于检测总数！");
          return
        }
      }
      if (this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined) {
        if (this.form.quantityUnqualified > this.form.quantityCheck) {
          this.$message.warning("不合格数不能大于检测总数！");
          return
        }
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.rqcId != null) {
            updateRqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRqc(this.form).then(response => {
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
      const rqcIds = row.rqcId || this.ids;
      this.$modal.confirm('是否确认删除退料检验单编号为"' + rqcIds + '"的数据项？').then(function() {
        return delRqc(rqcIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    },
    handleSelectProduct(){
      this.$refs.itemSelect.showFlag = true;
    },
    //物料选择弹出框
    onItemSelected(obj){        
        if(obj != undefined && obj != null){
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName
        }
    },
    //点击人员选择按钮
    handleUser2Select(){
        this.$refs.user2Select.showFlag = true;
    },
    //人员选择返回
    onUser2Selected(row){
        this.form.userId = row.userId;
        this.form.userName = row.userName;
        this.form.nickName = row.nickName;                
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_RQC_CODE').then(response =>{
          this.form.rqcCode = response;
        });
      }else{
        this.form.rqcCode = null;
      }
    }
  }
};
</script>
