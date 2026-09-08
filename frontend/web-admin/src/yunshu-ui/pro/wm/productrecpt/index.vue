<template>
  <div class="app-container inbound-doc-panel">
    <WorkflowContextBar :work-order-code="workflowWoCode" />
    <div class="recpt-intro">
      <strong>成品入库（步骤 9）</strong>
      <span class="recpt-intro__hint">
        点 <em>从工单生成</em> 选择已质检通过工单并自动带入产品与数量 → 或 <em>新增</em> 选工单、保存后添加物料行 → 提交 → 提交执行 → 执行入库。
      </span>
    </div>
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="recptCode">
          <el-input v-model="queryParams.recptCode" placeholder="入库单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="workorderCode">
          <el-input v-model="queryParams.workorderCode" placeholder="生产工单" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="warehouseName">
          <el-input v-model="queryParams.warehouseName" placeholder="仓库" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">产品入库</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:productrecpt:add']">新增</el-button>
        <el-button type="primary" plain icon="el-icon-document-add" size="default" :loading="creatingFromWo" @click="handleCreateFromWorkorder" v-hasPermi="['mes:wm:productrecpt:add']">从工单生成</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:productrecpt:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:productrecpt:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:wm:productrecpt:export']">导出</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table"
        stripe
        border
        height="100%"
        :data="productrecptList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="入库单编号" align="center" header-align="center" min-width="130" prop="recptCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.recptCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="入库单名称" align="center" header-align="center" min-width="120" prop="recptName" show-overflow-tooltip />
        <el-table-column label="生产工单" align="center" header-align="center" min-width="120" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="产品编码" align="center" header-align="center" min-width="100" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="产品名称" align="center" header-align="center" min-width="110" prop="itemName" show-overflow-tooltip />
        <el-table-column label="入库日期" align="center" header-align="center" prop="recptDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status =='PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='APPROVING'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >执行上架</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='APPROVING'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='APPROVED'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >执行入库</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:productrecpt:remove']"
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
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改产品入库录对话框 -->
    <el-dialog :title="title" v-model="open" width="980px" append-to-body class="recpt-form-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="recpt-edit-form">
        <div class="recpt-form-section">
          <div class="recpt-form-section__title">单据信息</div>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="入库单编号" prop="recptCode">
                <el-input v-model="form.recptCode" placeholder="请输入入库单编号" :disabled="autoGenFlag && optType === 'add'" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label-width="16">
                <el-switch
                  v-if="optType != 'view' && form.status =='PREPARE'"
                  v-model="autoGenFlag"
                  active-text="自动生成编号"
                  @change="handleAutoGenChange(autoGenFlag)"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="入库单名称" prop="recptName">
                <el-input v-model="form.recptName" placeholder="选择工单后可自动生成" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="入库日期" prop="recptDate">
                <el-date-picker clearable
                  v-model="form.recptDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择入库日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        <div class="recpt-form-section recpt-form-section--last">
          <div class="recpt-form-section__title">生产关联</div>
          <el-row :gutter="16">
            <el-col :span="10">
              <el-form-item label="生产工单" prop="workorderCode">
                <el-input v-model="form.workorderCode" placeholder="请选择生产工单" readonly>
                  <template #append><el-button icon="el-icon-search" @click="handleWorkorderSelect"></el-button></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="14">
              <el-form-item label="生产工单名称" prop="workorderName">
                <el-input v-model="form.workorderName" readonly placeholder="选择工单后带出" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入内容" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template v-if="form.recptId != null">
        <el-divider content-position="center">物料信息</el-divider>
        <div v-if="form.status == 'PREPARE'" class="recpt-lines-panel">
          <Productrecptline
            ref="line"
            :recptId="form.recptId"
            :itemId="form.itemId"
            :itemCode="form.itemCode"
            :itemName="form.itemName"
            :unitName="form.unitName"
            :unitOfMeasure="form.unitOfMeasure"
            :plannedQty="form.plannedQty"
            :workorderCode="form.workorderCode"
            :workorderName="form.workorderName"
            :optType="optType"
          ></Productrecptline>
        </div>
        <div v-else class="recpt-lines-panel">
          <ProductrecptDetail ref="detail" :recptId="form.recptId" :optType="optType"></ProductrecptDetail>
        </div>
      </template>
        <template #footer><div class="dialog-footer">
          <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.recptId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='APPROVING' && form.recptId !=null && optType !='view' ">提 交</el-button>
          <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div></template>
    </el-dialog>

    <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected" />
  </div>
</template>

<script>
import { listProductrecpt, getProductrecpt, delProductrecpt, addProductrecpt, updateProductrecpt, checkQuantity, execute, createProductRecptFromWorkOrder } from "@/yunshu-ui/api/mes/wm/productrecpt";
import { listWorkorder } from "@/yunshu-ui/api/mes/pro/workorder";
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
import Productrecptline from "./line.vue"
import ProductrecptDetail from "./detail.vue"
import WorkflowContextBar from "@/yunshu-ui/components/WorkflowContextBar.vue";
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Productrecpt",
  dicts: ['mes_order_status'],
  components:{WorkorderSelect,Productrecptline,ProductrecptDetail,WorkflowContextBar},
  data() {
    return {
      creatingFromWo: false,
      woPickMode: null,
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
      warehouseInfo:[],
      warehouseOptions:[],
      warehouseProps:{
        multiple: false,
        value: 'pId',
        label: 'pName',
      },
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
      // 产品入库录表格数据
      productrecptList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        recptCode: null,
        recptName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recptCode: [
          { required: true, message: "入库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        recptName: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        recptDate: [
          { required: true, message: "请选择入库日期", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    workflowWoCode() {
      return this.$route?.query?.wo || this.queryParams.workorderCode || "";
    },
  },
  created() {
    const wo = this.$route?.query?.wo;
    if (wo) {
      this.queryParams.workorderCode = String(wo);
    }
    this.getList();
  },
  methods: {
    /** 查询产品入库录列表 */
    getList() {
      this.loading = true;
      listProductrecpt(this.queryParams).then(response => {
        this.productrecptList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        recptId: null,
        recptCode: null,
        recptName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        unitOfMeasure: null,
        clientCode: null,
        clientName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: new Date(),
        plannedQty: null,
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
      this.ids = selection.map(item => item.recptId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加产品入库单";
      this.optType = 'add';
      this.autoGenFlag = true;
      this.form.recptDate = this.formatDate(new Date());
      this.applyAutoRecptCode();
      this.prefillWorkorderFromRoute();
    },
    prefillWorkorderFromRoute() {
      const woCode = this.$route?.query?.wo;
      if (!woCode) {
        return;
      }
      listWorkorder({ workorderCode: String(woCode), pageNum: 1, pageSize: 1 }).then((response) => {
        const row = response?.rows?.[0];
        if (row) {
          this.onWorkorderSelected(row);
          if (!this.form.recptName) {
            this.form.recptName = `成品入库-${row.workorderCode}`;
          }
        }
      }).catch(() => {});
    },
    formatDate(dt) {
      const d = dt instanceof Date ? dt : new Date(dt);
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    },
    applyAutoRecptCode() {
      genCode("PRODUCTRECPT_CODE").then((response) => {
        const code = response?.data ?? response;
        if (typeof code === "string" && code.trim()) {
          this.form.recptCode = code.trim();
        }
      }).catch(() => {
        this.form.recptCode = `PR${Date.now() % 100000}`;
      });
    },
    openRecptDialog(recptId, title, optType) {
      return getProductrecpt(recptId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = title;
        this.optType = optType;
        this.getList();
      });
    },
    /** 从工单一键生成入库单 */
    handleCreateFromWorkorder() {
      const woCode = this.$route?.query?.wo;
      if (woCode) {
        this.doCreateRecptFromWoCode(String(woCode));
        return;
      }
      this.woPickMode = "createRecpt";
      this.$nextTick(() => {
        const picker = this.$refs.woSelect;
        if (!picker?.handleOpen) {
          this.$modal.msgError("工单选择器未就绪，请刷新页面后重试");
          this.woPickMode = null;
          return;
        }
        picker.handleOpen(null, {
          title: "选择生产工单",
          hint: "请选择已质检通过(QC_PASSED)的工单，系统将自动生成入库单并带入产品与计划数量",
        });
      });
    },
    doCreateRecptFromWoCode(woCode) {
      this.creatingFromWo = true;
      listWorkorder({ workorderCode: woCode, pageNum: 1, pageSize: 1 })
        .then((response) => {
          const wo = response?.rows?.[0];
          if (!wo?.workorderId) {
            throw new Error(`未找到可入库的工单：${woCode}`);
          }
          return this.doCreateRecptFromWorkOrderId(wo.workorderId);
        })
        .catch((err) => {
          this.$modal.msgError(err?.message || "从工单生成入库单失败");
        })
        .finally(() => {
          this.creatingFromWo = false;
        });
    },
    doCreateRecptFromWorkOrderId(workorderId) {
      if (!workorderId) {
        this.$modal.msgError("工单 ID 无效");
        return Promise.reject(new Error("工单 ID 无效"));
      }
      this.creatingFromWo = true;
      return createProductRecptFromWorkOrder(workorderId)
        .then((res) => {
          const recptId = res?.data ?? res;
          if (!recptId || typeof recptId === "object") {
            throw new Error("生成入库单失败：未返回有效 ID");
          }
          return this.openRecptDialog(recptId, "编辑产品入库单", "edit");
        })
        .then(() => {
          this.$modal.msgSuccess("已根据工单生成入库单，请核对物料行后提交");
        })
        .catch((err) => {
          this.$modal.msgError(err?.message || "从工单生成入库单失败");
          throw err;
        })
        .finally(() => {
          this.creatingFromWo = false;
        });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recptId = row.recptId || this.ids
      getProductrecpt(recptId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品入库单";
        this.optType = 'edit'
      });
    },
    /** 查看单据 */
    handleView(row) {
      this.reset();
      const recptId = row.recptId || this.ids
      getProductrecpt(recptId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看产品入库单";
        this.optType = 'view'
      });
    },
    /**
     * 上架按钮操作
     */
    handleStocking(row){
      const recptIds = row.recptId
      getProductrecpt(recptIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "入库单上架";
        this.optType = "edit";
      });
    },

    //执行入库
    handleExecute(row){
      const recptIds = row.recptId || this.ids;
      this.$modal.confirm('确认执行入库？').then(() => {
        return execute(recptIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("入库成功");
      }).catch((err) => {
        if (err !== 'cancel' && err?.message) {
          this.$modal.msgError(err.message);
        }
      });
    },
    /** 列表：草稿提交到待上架 */
    handleSubmit(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认提交该入库单？').then(() => {
        return getProductrecpt(recptId).then(response => {
          const data = { ...response.data, status: 'APPROVING' };
          return updateProductrecpt(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待上架提交到待执行 */
    handleSubmitExecute(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return checkQuantity(recptId).then(response => {
          const doSubmit = () => getProductrecpt(recptId).then(res => {
            const data = { ...res.data, status: 'APPROVED' };
            return updateProductrecpt(data);
          });
          if (response.data) {
            return doSubmit();
          }
          return this.$modal.confirm('入库数量与上架数量不一致，确认提交?').then(() => doSubmit());
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认撤销入库单？').then(() => {
        return getProductrecpt(recptId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateProductrecpt(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return;
        }
        const recptDate = this.form.recptDate;
        if (recptDate instanceof Date) {
          this.form.recptDate = this.formatDate(recptDate);
        }
        if (this.form.recptId != null) {
          updateProductrecpt(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.getList();
          }).catch((err) => {
            this.$modal.msgError(err?.message || "保存失败");
          });
          return;
        }
        addProductrecpt(this.form).then((response) => {
          const recptId = response?.data ?? response;
          if (!recptId) {
            this.$modal.msgError("新增失败：未返回入库单 ID");
            return;
          }
          this.$modal.msgSuccess("入库单已创建，请继续添加入库物料行");
          return getProductrecpt(recptId).then((res) => {
            this.form = res.data;
            this.optType = "edit";
            this.title = "编辑产品入库单";
            this.getList();
          });
        }).catch((err) => {
          this.$modal.msgError(err?.message || "新增失败");
        });
      });
    },

    //提交按钮(提交到待上架状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'APPROVING';
          updateProductrecpt(this.form).then(response => {
            this.$modal.msgSuccess("提交成功");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'PREPARE';
         });;          
        }
      });

    },

    submitToExecute(){
      let that = this;
      checkQuantity(this.form.recptId).then( response =>{
        if(response.data){
            that.form.status = 'APPROVED';
            updateProductrecpt(that.form).then(response => {
              that.$modal.msgSuccess("提交成功");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'APPROVING';
            });    
          }else{
            this.$modal.confirm('入库数量与上架数量不一致，确认提交?').then(function() {
              that.form.status = 'APPROVED';
              updateProductrecpt(that.form).then(response => {
                that.$modal.msgSuccess("提交成功");
                that.open = false;
                that.getList();
              } 
              ).catch(() => {
                that.form.status = 'APPROVING';
              });    
            }).catch(() => {

            });
          }
        }
      );   
    },


    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销入库单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
        updateProductrecpt(that.form).then(response => {
            that.$modal.msgSuccess("撤销成功");
            that.open = false;
            that.getList();
          } 
          ).catch(() => {
            that.form.status = oldStatus;
          });
          return true;
      }).catch(() => {});
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const recptIds = row.recptId || this.ids;
      this.$modal.confirm('是否确认删除产品入库单编号为"' + row.recptCode + '"的数据项？').then(function() {
        return delProductrecpt(recptIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/productrecpt/export', {
        ...this.queryParams
      }, `productrecpt_${new Date().getTime()}.xlsx`)
    },
    //选择生产工单
    handleWorkorderSelect(){
      this.woPickMode = "form";
      this.$refs.woSelect.handleOpen(this.form.workorderId, {
        title: "选择生产工单",
        hint: "请选择已质检通过的工单，保存后可添加产品入库行",
        workorderCode: this.form.workorderCode || undefined,
      });
    },
    onWorkorderSelected(row){
      if (this.woPickMode === "createRecpt") {
        this.woPickMode = null;
        if (row?.workorderId) {
          this.doCreateRecptFromWorkOrderId(row.workorderId);
        }
        return;
      }
      this.woPickMode = null;
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.itemId = row.productId;
        this.form.itemCode = row.productCode;
        this.form.itemName = row.productName;
        this.form.unitOfMeasure = row.unitOfMeasure || "PCS";
        this.form.unitName = row.unitName || "件";
        this.form.plannedQty = row.quantity;
        this.form.clientCode = row.clientCode;
        this.form.clientName = row.clientName;
        if (!this.form.recptName) {
          this.form.recptName = `成品入库-${row.workorderCode}`;
        }
      }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        this.applyAutoRecptCode();
      }else{
        this.form.recptCode = null;
      }
    }
  }
};
</script>

<style scoped>
.recpt-intro {
  margin: 0 0 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}
.recpt-intro__hint {
  margin-left: 8px;
  color: #606266;
}
.recpt-intro__hint em {
  font-style: normal;
  color: #409eff;
  font-weight: 600;
}
.recpt-form-dialog :deep(.el-dialog__body) {
  max-height: min(78vh, 820px);
  overflow-x: hidden;
  overflow-y: auto;
  padding-top: 12px;
}
.recpt-form-section {
  margin-bottom: 14px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}
.recpt-form-section--last {
  border-bottom: none;
  margin-bottom: 0;
}
.recpt-form-section__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.recpt-edit-form :deep(.el-input),
.recpt-edit-form :deep(.el-select),
.recpt-edit-form :deep(.el-date-editor) {
  width: 100%;
}
.recpt-edit-form :deep(.el-switch) {
  height: 32px;
  white-space: nowrap;
}
.recpt-edit-form :deep(.el-switch__label) {
  white-space: nowrap;
  color: #606266;
}
.recpt-lines-panel {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 10px 4px;
  background: #fafbfc;
}
.recpt-lines-panel :deep(.recpt-line-root) {
  padding: 0;
  min-height: 0;
  height: auto;
}
</style>
