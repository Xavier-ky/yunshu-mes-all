<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="检验单编号" prop="oqcCode">
          <el-input
            v-model="queryParams.oqcCode"
            placeholder="请输入出货检验单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input
            v-model="queryParams.clientName"
            placeholder="请输入客户名称"
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
          v-hasPermi="['mes:qc:oqc:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:oqc:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:oqc:remove']"
        >删除</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:qc:oqc:export']"
        >导出</el-button>
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
        :data="oqcList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="出货检验单编号" min-width="148" align="center" header-align="center" prop="oqcCode" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:qc:oqc:query']"
            >{{ scope.row.oqcCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="验单名称" min-width="120" align="center" header-align="center" prop="oqcName" show-overflow-tooltip />
        <el-table-column label="客户名称" min-width="120" align="center" header-align="center" prop="clientName" show-overflow-tooltip />
        <el-table-column label="批次号" min-width="100" align="center" header-align="center" prop="batchCode" show-overflow-tooltip />
        <el-table-column label="产品编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="产品名称" min-width="136" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
        <el-table-column label="规格型号" min-width="108" align="center" header-align="center" prop="specification" show-overflow-tooltip />
        <el-table-column label="单位" min-width="72" align="center" header-align="center" prop="unitOfMeasure" show-overflow-tooltip />
        <el-table-column label="发货数量" min-width="96" align="center" header-align="center" prop="quantityOut" show-overflow-tooltip />
        <el-table-column label="检测数量" min-width="96" align="center" header-align="center" prop="quantityCheck" show-overflow-tooltip />
        <el-table-column label="不合格数" min-width="96" align="center" header-align="center" prop="quantityUnqualified" show-overflow-tooltip />
        <el-table-column label="检测结论" min-width="100" align="center" header-align="center" prop="checkResult" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult"/>
          </template>
        </el-table-column>
        <el-table-column label="出货日期" min-width="116" align="center" header-align="center" prop="outDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.outDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测日期" min-width="116" align="center" header-align="center" prop="inspectDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.inspectDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测人员" min-width="100" align="center" header-align="center" prop="inspector" show-overflow-tooltip />
        <el-table-column label="单据状态" min-width="100" align="center" header-align="center" prop="status" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
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
                v-if="scope.row.status =='PREPARE'"
                v-hasPermi="['mes:qc:oqc:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-if="scope.row.status =='PREPARE'"
                v-hasPermi="['mes:qc:oqc:remove']"
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

    <!-- 添加或修改出货检验单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="oqcCode">
              <el-input v-model="form.oqcCode" placeholder="请输入出货检验单编号" />
            </el-form-item>
          </el-col>

          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'" >
              </el-switch>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="检验单名称" prop="oqcName">
              <el-input v-model="form.oqcName" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="产品编码" prop="itemCode">
              <el-input v-if="form.oqcId ==null " readonly v-model="form.itemCode" placeholder="请选择产品" >
                <el-button slot="append" @click="handleSelectProduct" icon="el-icon-search"></el-button>
              </el-input>
              <!--如果已经保存过，则产品不允许再修改，需要修改就删除重做-->
              <el-input v-else v-model="form.itemCode"/>
              <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="产品名称" prop="itemName">
              <el-input readonly="readonly" v-model="form.itemName"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="单位" prop="unitOfMeasure">
              <el-input readonly="readonly" v-model="form.unitOfMeasure"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input readonly="readonly" v-model="form.specification" type="textarea" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编号" prop="clientCode">
              <el-input v-model="form.clientCode" readonly placeholder="请输选择客户" >
                <el-button slot="append" @click="handleSelectClient" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <ClientSelect ref="clientSelect" @onSelected="onClientSelected" > </ClientSelect>
          </el-col>

          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input readonly="readonly" v-model="form.clientName"  />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" placeholder="请输入批次号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="发货数量" prop="quantityOut">
              <el-input-number :min="1" :max="99999999" v-model="form.quantityOut" placeholder="请输入发货数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测数量" prop="quantityCheck">
              <el-input-number :min="1" :max="99999999" v-model="form.quantityCheck" placeholder="请输入本次检测数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不合格数量" prop="quantityUnqualified">
              <el-input-number :min="0" :max="99999999" readonly="readonly" v-model="form.quantityUnqualified" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="出货日期" prop="outDate">
              <el-date-picker clearable
                v-model="form.outDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出货日期">
              </el-date-picker>
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
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-collapse accordion>
          <el-collapse-item title="结果统计">
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷数量" prop="crQuantity">
                  <el-input readonly="readonly" v-model="form.crQuantity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷数量" prop="majQuantity">
                  <el-input readonly="readonly" v-model="form.majQuantity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷数量" prop="minQuantity">
                  <el-input readonly="readonly" v-model="form.minQuantity" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷率" prop="crQuantity">
                  <el-input readonly="readonly" v-model="form.crRate" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷率" prop="majRate">
                  <el-input readonly="readonly" v-model="form.majRate" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷率" prop="minRate">
                  <el-input readonly="readonly" v-model="form.minRate" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
      </el-form>

      <el-tabs type="border-card" v-if="form.oqcId != null">
          <el-tab-pane label="检测项">
            <Oqcline ref=line :oqcId="form.oqcId" :optType="optType"></Oqcline>
          </el-tab-pane>
          <el-tab-pane label="检测结果">
            <QCResutl ref="qcResult" :qcId="form.oqcId" :qcType="'OQC'" :optType="optType"></QCResutl>
          </el-tab-pane>
      </el-tabs>

      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && optType !='view'  && form.oqcId !=null">完成</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listOqc, getOqc, delOqc, addOqc, updateOqc } from "@/yunshu-ui/api/mes/qc/oqc";
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import ClientSelect from "@/yunshu-ui/components/clientSelect/single.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
import Oqcline from "./line.vue";
import QCResutl from "../qcresult/index.vue";
export default {
  name: "Oqc",
  dicts: ['mes_order_status','mes_qc_result'],
  components: {
    ItemSelect,Oqcline,ClientSelect,QCResutl
  },
  data() {
    return {
      //自动生成编码
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
      // 出货检验单表格数据
      oqcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        oqcCode: null,
        oqcName: null,
        templateId: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        batchCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityMinCheck: null,
        quantityMaxUnqualified: null,
        quantityOut: null,
        quantityCheck: null,
        quantityUnqualified: null,
        quantityQualified: null,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        outDate: null,
        inspectDate: null,
        inspector: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        oqcCode: [
          { required: true, message: "出货检验单编号不能为空", trigger: "blur" }
        ],
        clientCode: [
          { required: true, message: "客户不能为空", trigger: "blur" }
        ],
        itemCode: [
          { required: true, message: "产品不能为空", trigger: "blur" }
        ],
        quantityOut: [
          { required: true, message: "发货数量不能为空", trigger: "blur" }
        ],
        quantityCheck: [
          { required: true, message: "本次检测数量不能为空", trigger: "blur" }
        ],
        outDate: [
          { required: true, message: "请选择出货日期", trigger: "blur" }
        ],
        inspectDate: [
          { required: true, message: "请选择检测日期", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询出货检验单列表 */
    getList() {
      this.loading = true;
      listOqc(this.queryParams).then(response => {
        this.oqcList = response.rows;
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
        oqcId: null,
        oqcCode: null,
        oqcName: null,
        templateId: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        batchCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityMinCheck: null,
        quantityMaxUnqualified: null,
        quantityOut: null,
        quantityCheck: null,
        quantityUnqualified: null,
        quantityQualified: null,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        outDate: null,
        inspectDate: null,
        inspector: null,
        status: "PREPARE",
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
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
      this.ids = selection.map(item => item.oqcId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    //查看明细
    handleView(row){
      this.reset();
      const oqcId = row.oqcId || this.ids;
      getOqc(oqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看出货检验单信息";
        this.optType = "view";
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加出货检验单";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const oqcId = row.oqcId || this.ids
      getOqc(oqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改出货检验单";
        this.optType = "edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.oqcId != null) {
            updateOqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addOqc(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //点击完成
    handleFinish(){
      let that = this;
      if(this.form.checkResult == null){
        this.$modal.msgError("请选择检测结果！");
        return;
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          this.$modal.confirm('是否完成出货检验单编制？【完成后将不能更改】').then(function(){
            that.form.status = 'CONFIRMED';
            that.submitForm();
            that.open = false;
          });
        }
        });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const oqcIds = row.oqcId || this.ids;
      this.$modal.confirm('是否确认删除出货检验单编号为"' + oqcIds + '"的数据项？').then(function() {
        return delOqc(oqcIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleSelectProduct(){
      this.$refs.itemSelect.handleOpen(this.form.itemId)
    },
    //物料选择弹出框
    onItemSelected(obj){
        if(obj != undefined && obj != null){
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
        }
    },
    handleSelectClient(){
      this.$refs.clientSelect.handleOpen(this.form.clientId)
    },
    //客户选择弹出框
    onClientSelected(obj){
        if(obj != undefined && obj != null){
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
        }
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_OQC_CODE').then(response =>{
          this.form.oqcCode = response;
        });
      }else{
        this.form.oqcCode = null;
      }
    },
  }
};
</script>
