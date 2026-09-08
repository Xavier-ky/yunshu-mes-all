<template>
  <div class="app-container qc-hub-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form" @submit.prevent>
        <el-form-item label="检验单编号" prop="ipqcCode">
          <el-input
            v-model="queryParams.ipqcCode"
            placeholder="请输入检验单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检验类型" prop="ipqcType">
          <el-select v-model="queryParams.ipqcType" placeholder="请选择检验类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_qc_detail_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="工单编号" prop="workorderCode">
          <el-input
            v-model="queryParams.workorderCode"
            placeholder="请输入工单编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="检测结论" prop="checkResult">
          <el-select v-model="queryParams.checkResult" placeholder="请选择检验结果" clearable>
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
          v-hasPermi="['mes:qc:ipqc:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:qc:ipqc:edit']"
        >修改</el-button>
        <el-button
          size="small"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:qc:ipqc:remove']"
        >删除</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:qc:ipqc:export']"
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
        :data="ipqcList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="检验单编号" min-width="148" align="center" header-align="center" prop="ipqcCode" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:qc:ipqc:query']"
            >{{ scope.row.ipqcCode }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="检验类型" min-width="100" align="center" header-align="center" prop="ipqcType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_pqc_type" :value="scope.row.ipqcType"/>
          </template>
        </el-table-column>
        <el-table-column label="工单编号" min-width="128" align="center" header-align="center" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="产品物料编码" min-width="120" align="center" header-align="center" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="产品物料名称" min-width="136" align="center" header-align="center" prop="itemName" show-overflow-tooltip />
        <el-table-column label="规格型号" min-width="108" align="center" header-align="center" prop="specification" show-overflow-tooltip />
        <el-table-column label="单位" min-width="72" align="center" header-align="center" prop="unitName" show-overflow-tooltip />
        <el-table-column label="检测数量" min-width="96" align="center" header-align="center" prop="quantityCheck" show-overflow-tooltip />
        <el-table-column label="检测结果" min-width="100" align="center" header-align="center" prop="checkResult" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_qc_result" :value="scope.row.checkResult"/>
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
                v-hasPermi="['mes:qc:ipqc:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-if="scope.row.status =='PREPARE'"
                v-hasPermi="['mes:qc:ipqc:remove']"
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

    <!-- 添加或修改过程检验单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="ipqcCode">
              <el-input v-model="form.ipqcCode" placeholder="请输入检验单编号" />
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
            <el-form-item label="检验单名称" prop="ipqcName">
              <el-input v-model="form.ipqcName" placeholder="请输入检验单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验类型" prop="ipqcType">
              <el-select v-model="form.ipqcType" placeholder="请选择检验类型">
                <el-option
                  v-for="dict in dict.type.mes_pqc_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工单编码" prop="workorderCode">
              <el-input v-if="form.ipqcId == null" v-model="form.workorderCode" placeholder="请选择生产工单" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkorderSelect"></el-button>
              </el-input>
              <!--保存过则不允许修改工单，需要修改则删除重做-->
              <el-input v-else v-model="form.workorderCode" >
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工单名称" prop="workorderName">
              <el-input v-model="form.workorderName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="工作站编号" prop="workstationCode">
              <el-input v-model="form.workstationCode" placeholder="请输入工作站编号" >
                <el-button slot="append" icon="el-icon-search" @click="handleWorkstationSelect"></el-button>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelect"  @onSelected="onWorkstationSelected"> </WorkstationSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站名称" prop="workstationName">
              <el-input v-model="form.workstationName" readonly="readonly"  />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测数量" prop="quantityCheck">
              <el-input-number v-model="form.quantityCheck" :max="99999999" placeholder="请输入检测数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-collapse>
          <el-collapse-item>
            <template slot="title">
              更多信息<i class="header-icon el-icon-info"></i>
            </template>
            <el-row>
              <el-col :span="8">
                <el-form-item label="产品编码" prop="itemCode">
                  <el-input v-model="form.itemCode" placeholder="请输入产品编码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="产品名称" prop="itemName">
                  <el-input v-model="form.itemName" placeholder="请输入产品名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="单位" prop="unitName">
                  <el-input v-model="form.unitName" placeholder="请输入单位" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="规格型号" prop="specification">
                  <el-input v-model="form.specification" type="textarea" placeholder="请输入内容" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="工序编码" prop="processCode">
                  <el-input v-model="form.processCode" placeholder="请输入工序编码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="工序名称" prop="processName">
                  <el-input v-model="form.processName" placeholder="请输入工序名称" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="任务编号" prop="taskCode">
                  <el-input v-model="form.taskCode" placeholder="请输入任务编号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="任务名称" prop="taskName">
                  <el-input v-model="form.taskCode" placeholder="请输入任务编号" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="不合格数" prop="quantityUnqualified">
                  <el-input-number v-model="form.quantityUnqualified" :max="99999999" placeholder="请输入不合格数" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合格品数量" prop="quantityQualified">
                  <el-input-number v-model="form.quantityQualified" :max="99999999" placeholder="请输入合格品数量" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.quantityUnqualified >0">
              <el-col :span="8">
                <el-form-item label="工废" prop="quantityLaborScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityLaborScrap" placeholder="请输入工废数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="料废" prop="quantityMaterialScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityMaterialScrap" placeholder="请输入料废数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="其他" prop="quantityOtherScrap">
                  <el-input-number :min="0" @change="handleScrapChanged" v-model="form.quantityOtherScrap" placeholder="请输入其他原因废品数量" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷数量" prop="crQuantity">
                  <el-input-number v-model="form.crQuantity" :max="99999999" placeholder="请输入致命缺陷数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷数量" prop="majQuantity">
                  <el-input-number v-model="form.majQuantity" :max="99999999" placeholder="请输入严重缺陷数量" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷数量" prop="minQuantity">
                  <el-input-number v-model="form.minQuantity" :max="99999999" placeholder="请输入轻微缺陷数量" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>

        <el-row>
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
              <el-select v-model="form.checkResult" placeholder="请选择检验结论">
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
            <el-form-item label="检测人员" prop="inspector">
              <el-input v-model="form.inspector" placeholder="请输入检测人员" />
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
      </el-form>
      <el-tabs type="border-card" v-if="form.ipqcId != null">
          <el-tab-pane label="检测项">
            <Ipqcline ref=line :ipqcId="form.ipqcId" :optType="optType"></Ipqcline>
          </el-tab-pane>
          <el-tab-pane label="检测结果">
            <QCResutl ref="qcResult" :qcId="form.ipqcId" :qcType="'PQC'" :qcDetailType="form.qcDetailType" :optType="optType"></QCResutl>
          </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && optType !='view'  && form.ipqcId !=null">完成</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listIpqc, getIpqc, delIpqc, addIpqc, updateIpqc } from "@/yunshu-ui/api/mes/qc/ipqc";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
import Ipqcline from "./line.vue"
import QCResutl from "../qcresult/index.vue";
export default {
  name: "Ipqc",
  components: {WorkorderSelect,WorkstationSelect,Ipqcline,QCResutl},
  dicts: ['mes_pqc_type','mes_qc_result','mes_order_status'],
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
      // 过程检验单表格数据
      ipqcList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ipqcCode: null,
        ipqcName: null,
        ipqcType: null,
        templateId: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        processId: null,
        processCode: null,
        processName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
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
        inspectDate: null,
        inspector: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        ipqcCode: [
          { required: true, message: "请输入或生产检验单编号", trigger: "blur" }
        ],
        ipqcType: [
          { required: true, message: "请选择检验类型", trigger: "change" }
        ],

        workorderCode: [
          { required: true, message: "请选择生产工单", trigger: "blur" }
        ],

        workstationCode: [
          { required: true, message: "请选择工作站", trigger: "blur" }
        ],

        quantityCheck: [
          { required: true, message: "检测数量不能为空", trigger: "blur" }
        ],

        quantityUnqualified: [
          { required: true, message: "不合格品数量不能为空", trigger: "blur" }
        ],

        quantityQualified: [
          { required: true, message: "合格品数量不能为空", trigger: "blur" }
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
    /** 查询过程检验单列表 */
    getList() {
      this.loading = true;
      listIpqc(this.queryParams).then(response => {
        this.ipqcList = response.rows;
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
        ipqcId: null,
        ipqcCode: null,
        ipqcName: null,
        ipqcType: null,
        templateId: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        taskName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        processId: null,
        processCode: null,
        processName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        quantityCheck: null,
        quantityUnqualified: null,
        quantityQualified: null,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        inspectDate: new Date(),
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
      this.ids = selection.map(item => item.ipqcId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加过程检验单";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const ipqcId = row.ipqcId || this.ids
      getIpqc(ipqcId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改过程检验单";
        this.optType = "edit";
      });
    },
    //查看单据
    handleView(row){
      this.reset();
      const ipqcIds = row.ipqcId
      getIpqc(ipqcIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看检验单信息";
        this.optType = "view";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.ipqcId != null) {
            updateIpqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addIpqc(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.form.ipqcId = response.data; //将返回的ID保存
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
          this.$modal.confirm('是否完成检验单编制？【完成后将不能更改】').then(function(){
            that.form.status = 'FINISHED';
            that.submitForm();
            that.open = false;
          });
        }
        });
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const ipqcIds = row.ipqcId || this.ids;
      this.$modal.confirm('是否确认删除过程检验单编号为"' + ipqcIds + '"的数据项？').then(function() {
        return delIpqc(ipqcIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.$modal.msgWarning('Excel 导出暂未对接，请使用列表数据或后续版本');
    },
     //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.handleOpen(this.form.workorderId)
    },
    onWorkorderSelected(row){
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.itemId = row.productId;
        this.form.itemCode = row.productCode;
        this.form.itemName = row.productName;
        this.form.specification = row.productSpc;
        this.form.unitOfMeasure = row.unitOfMeasure;
        this.form.unitName = row.unitName

      }
    },
    handleWorkstationSelect(){
      this.$refs.wsSelect.handleOpen(this.form.workstationId)
    },
    onWorkstationSelected(row){
      if(row != undefined && row != null){
        this.form.workstationId = row.workstationId;
        this.form.workstationCode = row.workstationCode;
        this.form.workstationName = row.workstationName;
      }
    },
    handleScrapChanged(){
      this.form.quantityUnqualified = this.form.quantityLaborScrap + this.form.quantityMaterialScrap + this.form.quantityOtherScrap;
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_PQC_CODE').then(response =>{
          this.form.ipqcCode = response;
        });
      }else{
        this.form.ipqcCode = null;
      }
    }
  }
};
</script>
