<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="检验单编号" prop="iqcCode">
          <el-input
            v-model="queryParams.iqcCode"
            placeholder="请输入来料检验单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商名称" prop="vendorName">
          <el-input
            v-model="queryParams.vendorName"
            placeholder="请输入供应商名称"
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
        <el-form-item label="检测日期" prop="inspectDate">
          <el-date-picker
            clearable
            v-model="queryParams.inspectDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择检测日期"
          />
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
          v-hasPermi="['mes:qc:iqc:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:iqc:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:iqc:remove']"
        >删除</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:qc:iqc:export']"
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
        :data="iqcList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="来料检验单编号" min-width="148" align="center" header-align="center" prop="iqcCode" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:qc:iqc:query']"
            >{{ scope.row.iqcCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="来料检验单名称" min-width="136" align="center" header-align="center" prop="iqcName" show-overflow-tooltip />
        <el-table-column label="供应商简称" min-width="120" align="center" header-align="center" prop="vendorNick" show-overflow-tooltip />
        <el-table-column label="供应商批次号" min-width="120" align="center" header-align="center" prop="vendorBatch" show-overflow-tooltip />
        <el-table-column label="产品物料编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="产品物料名称" min-width="136" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
        <el-table-column label="接收数量" min-width="96" align="center" header-align="center" prop="quantityRecived" show-overflow-tooltip />
        <el-table-column label="检测数量" min-width="96" align="center" header-align="center" prop="quantityCheck" show-overflow-tooltip />
        <el-table-column label="不合格数" min-width="96" align="center" header-align="center" prop="quantityUnqualified" show-overflow-tooltip />
        <el-table-column label="检测结论" min-width="100" align="center" header-align="center" prop="checkResult" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult"/>
          </template>
        </el-table-column>
        <el-table-column label="来料日期" min-width="116" align="center" header-align="center" prop="reciveDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.reciveDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测日期" min-width="116" align="center" header-align="center" prop="inspectDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.inspectDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测人员" min-width="100" align="center" header-align="center" prop="inspectorName" show-overflow-tooltip />
        <el-table-column label="单据状态" min-width="100" align="center" header-align="center" prop="status" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" align="center" header-align="center" fixed="right" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:qc:iqc:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:qc:iqc:remove']"
              >删除</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-document"
                @click="viewReport"
              >查看报表</el-button>
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

    <!-- 添加或修改来料检验单对话框 -->
    <el-dialog :title="title" v-model="open" width="1080px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="iqcCode">
              <el-input v-model="form.iqcCode" placeholder="请输入来料检验单编号" />
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
          <el-col :span="9">
            <el-form-item label="检验单名称" prop="iqcName">
              <el-input v-model="form.iqcName" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="3" align="middle">
              <el-image fit="contain" v-if="form.checkResult == 'ACCEPT'" :src="acceptImg" />
              <el-image fit="contain" v-else-if="form.checkResult == 'REJECT'" :src="rejectImg" />
              <el-image fit="contain" v-else :src="prepareImg" />
          </el-col>
        </el-row>
        <el-divider content-position="center">物料与供应商</el-divider>
        <el-row>
          <el-col :span="8">
            <el-form-item label="产品物料编码" prop="itemCode">
              <el-input v-if="form.iqcId ==null " v-model="form.itemCode" readonly placeholder="请选择物料" >
                <el-button slot="append" @click="handleSelectProduct" icon="el-icon-search"></el-button>
              </el-input>
              <!--如果已经保存过，则产品不允许再修改，需要修改就删除重做-->
              <el-input v-else v-model="form.itemCode">
              </el-input>
            </el-form-item>
            <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品物料名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="供应商编码" prop="vendorCode">
              <el-input v-model="form.vendorCode" readonly placeholder="请选择供应商" >
                <el-button slot="append" @click="handleSelectVendor" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商简称" prop="vendorNick">
              <el-input v-model="form.vendorNick" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商批次号" prop="vendorBatch">
              <el-input v-model="form.vendorBatch" placeholder="请输入供应商批次号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">检测情况</el-divider>
        <el-row>
          <el-col :span="8">
            <el-form-item label="本次接收数量" prop="quantityRecived">
              <el-input-number :min="1" :max="99999999" v-model="form.quantityRecived" placeholder="请输入本次接收数量" />
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
            <el-form-item label="来料日期" prop="reciveDate">
              <el-date-picker clearable
                v-model="form.reciveDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择来料日期">
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
          <el-collapse-item title="缺陷情况">
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷率" prop="crRate">
                  <el-input v-model="form.crRate" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷率" prop="majRate">
                  <el-input v-model="form.majRate" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷率" prop="minRate">
                  <el-input v-model="form.minRate" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷数量" prop="crQuantity">
                  <el-input v-model="form.crQuantity" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷数量" prop="majQuantity">
                  <el-input v-model="form.majQuantity" readonly="readonly" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷数量" prop="minQuantity">
                  <el-input v-model="form.minQuantity" readonly="readonly" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
        <el-tabs type="border-card" v-if="form.iqcId != null">
        <el-tab-pane label="检测项">
          <IqcLine ref=line :iqcId="form.iqcId" :optType="optType"></IqcLine>
        </el-tab-pane>
        <el-tab-pane label="检测结果">
          <QCResutl ref="qcResult" :qcId="form.iqcId" :qcType="'IQC'" :optType="optType"></QCResutl>
        </el-tab-pane>
      </el-tabs>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && optType !='view'  && form.iqcId !=null">完成</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listIqc, getIqc, delIqc, addIqc, updateIqc } from "@/yunshu-ui/api/mes/qc/iqc";
import acceptImg from '@/yunshu-ui/assets/images/accept.png';
import rejectImg from '@/yunshu-ui/assets/images/reject.png';
import prepareImg from '@/yunshu-ui/assets/images/prepare.png';

import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import IqcLine from "./iqcline.vue";
import QCResutl from "../qcresult/index.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import {getReport,getReport2} from "@/yunshu-ui/api/mes/report/report"
export default {
  name: "Iqc",
  dicts: ['mes_qc_result','mes_order_status'],
  components: {ItemSelect,VendorSelect,IqcLine,QCResutl},
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
      acceptImg,
      rejectImg,
      prepareImg,
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
      // 来料检验单表格数据
      iqcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        iqcCode: null,
        iqcName: null,
        templateId: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        vendorBatch: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityMinCheck: null,
        quantityMaxUnqualified: null,
        quantityRecived: null,
        quantityCheck: null,
        quantityQualified: null,
        quantityUnqualified: null,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        reciveDate: null,
        inspectDate: new Date(),
        inspector: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        iqcCode: [
          { required: true, message: "来料检验单编号不能为空", trigger: "blur" }
        ],
        iqcName: [
          { required: true, message: "来料检验单名称不能为空", trigger: "blur" }
        ],

        vendorCode: [
          { required: true, message: "供应商不能为空", trigger: "blur" }
        ],
        itemCode: [
          { required: true, message: "物料不能为空", trigger: "blur" }
        ],
        quantityRecived: [
          { required: true, message: "本次接收数量不能为空", trigger: "blur" }
        ],
        reciveDate:[
          { required: true, message: "清选择来料日期", trigger: "blur" }
        ],
        inspectDate:[
          { required: true, message: "清选择检验日期", trigger: "blur" }
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
    /** 查询来料检验单列表 */
    getList() {
      this.loading = true;
      listIqc(this.queryParams).then(response => {
        this.iqcList = response.rows;
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
        iqcId: null,
        iqcCode: null,
        iqcName: null,
        templateId: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        vendorBatch: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityMinCheck: null,
        quantityMaxUnqualified: null,
        quantityRecived: null,
        quantityCheck: null,
        quantityUnqualified: null,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        reciveDate: null,
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
      this.ids = selection.map(item => item.iqcId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加来料检验单";
      this.optType = 'add';
    },
    //查看明细
    handleView(row){
      this.reset();
      const iqcId = row.iqcId || this.ids;
      getIqc(iqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看来料检验单信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const iqcId = row.iqcId || this.ids
      getIqc(iqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改来料检验单";
        this.optType = 'edit';
      });
    },
    /** 提交按钮 */
    submitForm() {
      // 判断接收总数与合格不合格数之间的校验
      if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined && this.form.quantityQualified != 0
        && this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined && this.form.quantityUnqualified != 0) {
        if ((this.form.quantityQualified + this.form.quantityUnqualified) != this.form.quantityRecived) {
          this.$message.warning("合格数与不合格数之和必须等于接收总数！");
          return
        }
      }
      if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined) {
        if (this.form.quantityQualified > this.form.quantityRecived) {
          this.$message.warning("合格数不能大于接收总数！");
          return
        }
      }
      if (this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined) {
        if (this.form.quantityUnqualified > this.form.quantityRecived) {
          this.$message.warning("不合格数不能大于接收总数！");
          return
        }
      }
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.iqcId != null) {
            updateIqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              //this.open = false;
              this.getList();
            });
          } else {
            addIqc(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              //this.open = false;
              this.form.iqcId=response.data;
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
          this.$modal.confirm('是否完成来料检验单编制？【完成后将不能更改】').then(function(){
            that.form.status = 'CONFIRMED';
            that.submitForm();
            that.open = false;
          });
        }
        });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const iqcIds = row.iqcId || this.ids;
      this.$modal.confirm('是否确认删除来料检验单编号为"' + iqcIds + '"的数据项？').then(function() {
        return delIqc(iqcIds);
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
        debugger;
        if(obj != undefined && obj != null){
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName
        }
    },
    handleSelectVendor(){
      this.$refs.vendorSelect.showFlag = true;
    },
    //供应商选择弹出框
    onVendorSelected(obj){
        debugger;
        if(obj != undefined && obj != null){
          this.form.vendorId = obj.vendorId;
          this.form.vendorCode = obj.vendorCode;
          this.form.vendorName = obj.vendorName;
          this.form.vendorNick = obj.vendorNick;
        }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_IQC_CODE').then(response =>{
          this.form.iqcCode = response;
        });
      }else{
        this.form.iqcCode = null;
      }
    },
    viewReport(){
      var reportName = "Test";
      getReport2(reportName).then(res=>{
        debugger;
        let blob = new Blob([res],{type:'application/pdf'});
        let href = URL.createObjectURL(blob);
        console.log(href);
        window.open(`/pdf/web/viewer.html?file=${encodeURIComponent(href)}`);
      });
    }
  }
};
</script>
