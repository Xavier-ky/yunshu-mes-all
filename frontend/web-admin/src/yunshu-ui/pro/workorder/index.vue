<template>
  <div class="app-container workorders-command-center">
    <WorkflowTodoPanel @select="onWorkflowTodoSelect" />
    <WorkflowContextBar ref="workflowContextBar" :work-order-code="workflowWoCode" />
    <section v-show="showSearch" class="workorders-filter-panel">
      <el-form
        :model="queryParams"
        ref="queryForm"
        :inline="true"
        class="workorders-filter-form workorders-filter-form--compact"
        label-width="68px"
        @submit.prevent
      >
        <el-form-item label="工单编码" prop="workorderCode">
          <el-input
            v-model="queryParams.workorderCode"
            placeholder="请输入工单编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="产品名称" prop="productName">
          <el-input
            v-model="queryParams.productName"
            placeholder="请输入产品名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="单据状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option
              v-for="dict in dict.type.mes_order_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源单据" prop="sourceCode">
          <el-input
            v-model="queryParams.sourceCode"
            placeholder="订单编号"
            clearable
            @keyup.enter="handleQuery"
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
          v-hasPermi="['mes:pro:workorder:add']"
        >新增</el-button>
        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleExport"
          v-hasPermi="['mes:pro:workorder:export']"
        >导出</el-button>
        <span class="toolbar-divider"></span>
        <span class="result-count">共 <b>{{ total }}</b> 笔工单</span>
      </div>
      <div class="toolbar-right">
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
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
        :data="workorderList"
        row-key="workorderId"
        default-expand-all
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      >
        <el-table-column label="工单编码" min-width="148" prop="workorderCode" align="center" header-align="center" fixed="left">
          <template #default="scope">
            <el-button
              size="small"
              link
              @click="handleView(scope.row)"
              v-hasPermi="['mes:pro:workorder:query']"
            >{{scope.row.workorderCode}}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="工单名称" min-width="136" align="center" header-align="center" prop="workorderName" show-overflow-tooltip />
        <el-table-column label="工单类型" min-width="100" align="center" header-align="center" prop="workorderType" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_workorder_type" :value="scope.row.workorderType"/>
          </template>
        </el-table-column>
        <el-table-column label="工单来源" min-width="100" align="center" header-align="center" prop="orderSource" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_workorder_sourcetype" :value="scope.row.orderSource"/>
          </template>
        </el-table-column>
        <el-table-column label="订单编号" min-width="128" align="center" header-align="center" prop="sourceCode" show-overflow-tooltip />
        <el-table-column label="产品编号" min-width="116" align="center" header-align="center" prop="productCode" show-overflow-tooltip />
        <el-table-column label="产品名称" min-width="148" align="center" header-align="center" prop="productName" show-overflow-tooltip />
        <el-table-column label="规格型号" min-width="108" align="center" header-align="center" prop="productSpc" show-overflow-tooltip />
        <el-table-column label="单位" min-width="72" align="center" header-align="center" prop="unitName" show-overflow-tooltip />
        <el-table-column label="工单数量" min-width="96" align="center" header-align="center" prop="quantity" show-overflow-tooltip />
        <el-table-column label="已生产数量" min-width="112" align="center" header-align="center" prop="quantityProduced" show-overflow-tooltip />
        <el-table-column label="客户编码" min-width="100" align="center" header-align="center" prop="clientCode" show-overflow-tooltip />
        <el-table-column label="客户名称" min-width="132" align="center" header-align="center" prop="clientName" show-overflow-tooltip />
        <el-table-column label="需求日期" min-width="116" align="center" header-align="center" prop="requestDate" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单据状态" min-width="100" align="center" header-align="center" prop="status" show-overflow-tooltip>
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="流程状态" min-width="100" align="center" header-align="center" prop="lifecycleStatus" show-overflow-tooltip>
          <template #default="scope">
            <el-tag size="small" :type="lifecycleTagType(scope.row.lifecycleStatus)">
              {{ lifecycleLabel(scope.row.lifecycleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="360" align="center" header-align="center" fixed="right" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
            <el-button
              size="small"
              link
              icon="el-icon-edit"
              v-if="scope.row.status =='PREPARE'"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['mes:pro:workorder:edit']"
            >修改</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-plus"
              v-if="scope.row.status =='CONFIRMED' && scope.row.workorderType =='SELF'"
              @click="handleAdd(scope.row)"
              v-hasPermi="['mes:pro:workorder:update']"
            >新增</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-box"
              v-if="scope.row.status =='CONFIRMED' && needsKitting(scope.row)"
              @click="handleKitting(scope.row)"
              v-hasPermi="['mes:pro:workorder:query']"
            >齐套分析</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-circle-check"
              v-if="scope.row.status =='CONFIRMED'"
              @click="handleFinish(scope.row)"
              v-hasPermi="['mes:pro:workorder:update']"
            >完成</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-circle-close"
              v-if="scope.row.status =='CONFIRMED'"
              @click="handleCancel(scope.row)"
              v-hasPermi="['mes:pro:workorder:update']"
            >取消</el-button>
            <el-button
              size="small"
              link
              icon="el-icon-delete"
              v-if="scope.row.status =='PREPARE'"
              @click="handleDelete(scope.row)"
              v-hasPermi="['mes:pro:workorder:remove']"
            >删除</el-button>
            <el-button
                size="small"
                link
                icon="el-icon-printer"
                @click="handlePreview(scope.row)"
            >预览</el-button>
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

    <!-- 添加或修改生产工单对话框 -->
    <el-dialog :title="title" v-model="open" width="920px" @close="cancel" append-to-body class="workorder-create-dialog">
      <div v-if="optType === 'add'" class="wo-dialog-intro">
        <span class="wo-dialog-intro__icon" aria-hidden="true">📋</span>
        <div>
          <strong>从客户订单下达生产工单</strong>
          <small>选择最近订单后自动带出产品、数量与客户信息，并生成规范工单编号</small>
        </div>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="wo-edit-form">
        <div class="wo-form-section">
          <div class="wo-form-section__title">基本信息</div>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="工单编号" prop="workorderCode">
                <el-input
                  v-model="form.workorderCode"
                  placeholder="WO-YYYYMMDD-序号"
                  :disabled="autoGenFlag && optType === 'add'"
                >
                  <template #append>
                    <el-tooltip content="格式：WO-日期-订单尾码，如 WO-20260714-A001" placement="top">
                      <span class="wo-code-hint">?</span>
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label-width="20">
                <el-switch
                  v-if="optType !== 'view' && form.status === 'PREPARE'"
                  v-model="autoGenFlag"
                  active-text="自动生成编号"
                  @change="handleAutoGenChange"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="16">
              <el-form-item label="工单名称" prop="workorderName">
                <el-input v-model="form.workorderName" placeholder="选择订单后可自动生成" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="工单类型" prop="workorderType">
                <el-select v-model="form.workorderType" placeholder="类型" style="width: 100%">
                  <el-option
                    v-for="dict in dict.type.mes_workorder_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="wo-form-section">
          <div class="wo-form-section__title">订单来源</div>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="来源类型" prop="orderSource">
                <el-radio-group v-model="form.orderSource" :disabled="optType === 'view'" @change="onOrderSourceChange">
                  <el-radio
                    v-for="dict in dict.type.mes_workorder_sourcetype"
                    :key="dict.value"
                    :label="dict.value"
                  >{{ dict.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col v-if="form.orderSource === 'ORDER'" :span="16">
              <el-form-item label="来源订单" prop="orderId">
                <el-select
                  v-model="form.orderId"
                  filterable
                  clearable
                  placeholder="选择最近订单（推荐「执行中」）"
                  style="width: 100%"
                  :loading="ordersLoading"
                  @change="onOrderPicked"
                >
                  <el-option
                    v-for="o in recentOrders"
                    :key="o.orderId"
                    :label="orderOptionLabel(o)"
                    :value="o.orderId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row v-if="selectedOrderSummary" :gutter="16">
            <el-col :span="24">
              <div class="wo-order-summary">
                <span>已选 <strong>{{ selectedOrderSummary.orderNo }}</strong></span>
                <span>{{ selectedOrderSummary.customerName }}</span>
                <span>{{ selectedOrderSummary.productName }} × {{ selectedOrderSummary.orderQty }}</span>
                <span>交付 {{ selectedOrderSummary.deliveryDate || "—" }}</span>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="wo-form-section wo-form-section--product">
          <div class="wo-form-section__title">产品与数量</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="产品编号" prop="productCode">
                <el-input v-model="form.productCode" placeholder="选订单自动带出" readonly>
                  <template #append>
                    <el-button @click="handleSelectProduct" icon="el-icon-search" title="手动改产品" />
                  </template>
                </el-input>
                <ItemSelect ref="itemSelect" @onSelected="onItemSelected" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="产品名称" prop="productName">
                <el-input v-model="form.productName" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16" class="wo-product-metrics">
            <el-col :span="6" class="wo-qr-col">
              <el-form-item label="工单二维码" label-position="top" class="wo-qr-form-item">
                <BarcodeImg
                  ref="barcodeImg"
                  :bussinessId="form.workorderId"
                  :bussinessCode="form.workorderCode"
                  barcodeType="WORKORDER"
                />
                <small v-if="form.workorderCode" class="wo-qr-field__code">{{ form.workorderCode }}</small>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="工单数量" prop="quantity" label-position="top" class="wo-metric-form-item">
                <el-input-number v-model="form.quantity" :min="1" :max="99999999" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="需求日期" prop="requestDate" label-position="top" class="wo-metric-form-item wo-field-date">
                <el-date-picker
                  v-model="form.requestDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="单位" prop="unitName" label-position="top" class="wo-metric-form-item">
                <el-input v-model="form.unitName" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-row v-if="form.orderSource === 'ORDER'" :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" placeholder="随订单自动带出" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" placeholder="可选">
                <template #append>
                  <el-button @click="handleSelectClient" icon="el-icon-search" />
                </template>
              </el-input>
              <ClientSelect ref="clientSelect" @onSelected="onClientSelected" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.workorderType == 'OUTSOURCE' || form.workorderType == 'PURCHASE'" :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="vendorCode">
              <el-input v-model="form.vendorCode" placeholder="请选择供应商">
                <template #append><el-button @click="handleSelectVendor" icon="el-icon-search" /></template>
              </el-input>
              <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="vendorName">
              <el-input v-model="form.vendorName" readonly placeholder="请选择供应商" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card" v-if="form.workorderId != null" v-model="activeName" @tab-click="handleClickTab">
        <el-tab-pane label="BOM组成" name="bom">
          <Workorderbom ref="bomlist" :optType="optType" :workorder="form" @handleAddSub="handleSubAdd" ></Workorderbom>
        </el-tab-pane>
        <el-tab-pane label="物料需求" name="item">
          <WorkorderItemList ref="itemlist"  :workorder="form" :itemStatus="itemStatus"></WorkorderItemList>
        </el-tab-pane>
        <el-tab-pane label="齐套分析" name="kitting">
          <WorkOrderKittingTab
            ref="kittingTab"
            :workorder="form"
            :lifecycle-status="form.lifecycleStatus"
            @kitting-done="onKittingDone"
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="handleConfirm" v-if="form.status =='PREPARE' && optType !='view'  && form.workorderId !=null">确 认</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkorder, getWorkorder, delWorkorder, addWorkorder, updateWorkorder ,dofinish ,doCancel} from "@/yunshu-ui/api/mes/pro/workorder";
import Workorderbom from "./bom/bom.vue";
import WorkorderItemList from "./items/item.vue";
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import ClientSelect from "@/yunshu-ui/components/clientSelect/single.vue";
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { fetchOrders } from "@/api/planning";
import { fetchProducts } from "@/api/domain";
import {
  buildWorkOrderCode,
  defaultRequestDate,
  normalizeRecentOrders,
  orderOptionLabel,
} from "./workorder-form-utils";
import Treeselect from "@zanmato/vue3-treeselect";
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue"
import WorkflowContextBar from "@/yunshu-ui/components/WorkflowContextBar.vue";
import WorkflowTodoPanel from "@/yunshu-ui/components/WorkflowTodoPanel.vue";
import WorkOrderKittingTab from "./WorkOrderKittingTab.vue";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";

const LIFECYCLE_LABELS = {
  DRAFT: "草稿",
  RELEASED: "已下达",
  KITTING_OK: "齐套完成",
  SCHEDULED: "已排产",
  MATERIAL_ISSUED: "已领料",
  IN_PROGRESS: "生产中",
  QC_PENDING: "待质检",
  QC_PASSED: "质检通过",
  QC_FAILED: "质检不合格",
  COMPLETED: "已完工",
};

export default {
  name: "Workorder",
  dicts: ['mes_order_status','mes_workorder_sourcetype','mes_workorder_type'],
  components: {
    Treeselect,
    ItemSelect ,
    ClientSelect,
    VendorSelect,
    Workorderbom,
    WorkorderItemList,
    WorkOrderKittingTab,
    BarcodeImg,
    WorkflowContextBar,
    WorkflowTodoPanel,
  },
  data() {
    return {
      itemStatus: false,
      activeName: 'bom',
      workflowWoCode: "",
      openKittingAfterSave: false,
      recentOrders: [],
      productCatalog: [],
      ordersLoading: false,
      //自动生成编码
      autoGenFlag: true,
      optType: undefined,
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 非单个禁用
      single: true,
      // 总条数
      total: 0,
      // 非多个禁用
      multiple: true,
      // 生产工单表格数据
      workorderList: [],
      // 生产工单树选项
      workorderOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workorderCode: null,
        workorderName: null,
        orderSource: null,
        sourceCode: null,
        productId: null,
        productCode: null,
        productName: null,
        productSpc: null,
        unitOfMeasure: null,
        quantity: null,
        quantityProduced: null,
        quantityChanged: null,
        quantityScheduled: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        requestDate: null,
        parentId: null,
        ancestors: null,
        status: null,
      },
      // 表单参数
      form: {},
      formStatus: "parent",
      // 生成工单后的表单
      secondaryForm: {},
      primaryForm: {},
      // 表单校验
      rules: {
        workorderCode: [
          { required: true, message: "工单编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        workorderName: [
          { required: true, message: "工单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        workorderType: [
          { required: true, message: "请选择生产工单类型", trigger: "blur" }
        ],
        orderSource: [
          { required: true, message: "来源类型不能为空", trigger: "blur" }
        ],
        orderId: [
          {
            validator: (_rule, value, callback) => {
              if (this.form.orderSource === "ORDER" && !value) {
                callback(new Error("请选择来源订单"));
              } else {
                callback();
              }
            },
            trigger: "change",
          },
        ],
        productId: [
          { required: true, message: "产品不能为空", trigger: "blur" }
        ],
        productCode: [
          { required: true, message: "产品编号不能为空", trigger: "blur" }
        ],
        productName: [
          { required: true, message: "产品名称不能为空", trigger: "blur" }
        ],
        quantity: [
          { required: true, message: "生产数量不能为空", trigger: "blur" }
        ],
        requestDate: [
          { required: true, message: "需求日期不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    selectedOrderSummary() {
      if (!this.form.orderId) return null;
      const o = this.recentOrders.find((x) => x.orderId === this.form.orderId);
      if (!o) return null;
      return {
        orderNo: o.orderNo,
        customerName: o.customerName,
        productName: o.productName,
        orderQty: Number(o.orderQty || 0).toLocaleString("zh-CN"),
        deliveryDate: o.deliveryDate,
      };
    },
  },
  created() {
    const wo = this.$route?.query?.wo;
    if (wo) {
      this.workflowWoCode = String(wo);
      this.queryParams.workorderCode = String(wo);
    }
    this.loadOrderFormRefs();
    this.getList();
    if (this.$route?.query?.orderId) {
      this.$nextTick(() => this.handleAdd());
    }
  },
  methods: {
    orderOptionLabel,
    loadOrderFormRefs() {
      fetchProducts()
        .then((res) => {
          this.productCatalog = Array.isArray(res?.data) ? res.data : [];
        })
        .catch(() => {
          this.productCatalog = [];
        });
    },
    loadRecentOrders() {
      this.ordersLoading = true;
      return fetchOrders()
        .then((res) => {
          const all = Array.isArray(res?.data) ? res.data : [];
          this.recentOrders = normalizeRecentOrders(all, 15);
        })
        .catch(() => {
          this.recentOrders = [];
        })
        .finally(() => {
          this.ordersLoading = false;
        });
    },
    getSelectedOrderNo() {
      const o = this.recentOrders.find((x) => x.orderId === this.form.orderId);
      return o?.orderNo || this.form.sourceCode || "";
    },
    applyAutoWorkOrderCode() {
      const local = buildWorkOrderCode(this.getSelectedOrderNo());
      genCode("WORKORDER_CODE")
        .then((response) => {
          const code = response?.data ?? response;
          if (typeof code === "string" && code.trim() && !code.includes("error")) {
            this.form.workorderCode = code.trim();
          } else {
            this.form.workorderCode = local;
          }
        })
        .catch(() => {
          this.form.workorderCode = local;
        });
    },
    applyProductFromCatalog(productId) {
      if (!productId) return;
      const prod = this.productCatalog.find((p) => p.productId === productId);
      this.form.productId = productId;
      this.form.productCode = prod?.productCode || this.form.productCode || "";
      this.form.productName = prod?.productName || this.form.productName || "";
      this.form.productSpc = prod?.productModel || this.form.productSpc || "";
    },
    onOrderPicked(orderId) {
      if (!orderId) {
        this.form.sourceCode = null;
        return;
      }
      const order = this.recentOrders.find((o) => o.orderId === orderId);
      if (!order) return;
      this.form.orderId = order.orderId;
      this.form.sourceCode = order.orderNo;
      this.form.clientName = order.customerName || this.form.clientName;
      this.form.quantity = order.orderQty || this.form.quantity || 1;
      if (order.deliveryDate) {
        this.form.requestDate = order.deliveryDate;
      }
      this.applyProductFromCatalog(order.productId);
      if (!this.form.workorderName) {
        this.form.workorderName = `${order.customerName || "客户"}-${order.productName || "产品"}生产工单`;
      }
      if (this.autoGenFlag) {
        this.applyAutoWorkOrderCode();
      }
    },
    onOrderSourceChange(source) {
      if (source !== "ORDER") {
        this.form.orderId = null;
      }
    },
    preselectOrderFromRoute() {
      const q = this.$route?.query || {};
      const orderId = q.orderId ? Number(q.orderId) : null;
      const orderNo = q.orderNo ? String(q.orderNo) : null;
      if (orderId) {
        this.form.orderId = orderId;
        this.onOrderPicked(orderId);
        return;
      }
      if (orderNo) {
        const hit = this.recentOrders.find((o) => o.orderNo === orderNo);
        if (hit) {
          this.form.orderId = hit.orderId;
          this.onOrderPicked(hit.orderId);
        }
      }
    },
    onWorkflowTodoSelect(code) {
      this.workflowWoCode = code;
      this.queryParams.workorderCode = code;
      this.handleQuery();
    },
    handleClickTab(tab) {
      if (tab.name === "item") {
        this.itemStatus = true;
      } else {
        this.itemStatus = false;
      }
      if (tab.name === "kitting") {
        this.$nextTick(() => this.$refs.kittingTab?.analyze?.());
      }
    },
    lifecycleLabel(status) {
      return LIFECYCLE_LABELS[status] || status || "—";
    },
    lifecycleTagType(status) {
      if (status === "KITTING_OK" || status === "SCHEDULED" || status === "COMPLETED") return "success";
      if (status === "RELEASED" || !status) return "info";
      if (status === "QC_FAILED") return "danger";
      return "warning";
    },
    needsKitting(row) {
      const ls = row?.lifecycleStatus;
      return !ls || ls === "RELEASED" || ls === "DRAFT";
    },
    onKittingDone() {
      this.form.lifecycleStatus = "KITTING_OK";
      this.workflowWoCode = this.form.workorderCode || this.workflowWoCode;
      this.getList();
      this.$nextTick(() => {
        this.$refs.workflowContextBar?.refresh?.();
      });
    },
    handleKitting(row) {
      this.reset();
      getWorkorder(row.workorderId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = "齐套分析 — " + (this.form.workorderCode || "");
        this.optType = "view";
        this.activeName = "kitting";
        this.workflowWoCode = this.form.workorderCode || "";
        this.$nextTick(() => {
          this.$refs.barcodeImg?.getBarcode?.();
          this.$refs.kittingTab?.analyze?.();
        });
      });
    },
    /** 查询生产工单列表 */
    getList() {
      this.loading = true;
      listWorkorder(this.queryParams).then(response => {
        this.workorderList = this.handleTree(response.rows, "workorderId", "parentId");
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 转换生产工单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.workorderId,
        label: node.workorderName,
        children: node.children
      };
    },
    /** 查询生产工单下拉树结构 */
    getTreeselect() {
      listWorkorder().then(response => {
        this.workorderOptions = [];
        const data = { workorderId: 0, workorderName: '顶级节点', children: [] };
        data.children = this.handleTree(response.rows, "workorderId", "parentId");
        this.workorderOptions.push(data);
      });
    },
    // 取消按钮
    cancel() {
      if (this.formStatus == 'parent') {
        this.open = false;
        this.reset();
      } else {
        this.reset()
        this.formStatus = 'parent'
        this.getTreeselect();
        const workorderId = this.primaryForm.workorderId;
        getWorkorder(workorderId).then(response => {
          this.form = response.data
          this.open = true;
          this.$nextTick(() => {
            this.$refs.barcodeImg.getBarcode();

          })
          this.title = "查看工单信息";
          this.optType = "view";
        });
      }

    },
    // 表单重置
    reset() {
      this.form = {
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        workorderType: "SELF",
        orderSource: "ORDER",
        orderId: null,
        sourceCode: null,
        productId: null,
        productCode: null,
        productName: null,
        productSpc: null,
        unitOfMeasure: null,
        unitName: null,
        quantity: null,
        quantityProduced: null,
        quantityChanged: null,
        quantityScheduled: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        requestDate: defaultRequestDate(14),
        parentId: null,
        status: "PREPARE",
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
      };
      this.autoGenFlag = true;
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    //从BOM行中直接新增
    handleSubAdd(row){
      this.primaryForm = this.form
      this.formStatus = "child"
      this.open = false;
      this.reset();
      this.getTreeselect();
      if (row != null && row.workorderId) {
        this.form = row;
        this.form.parentId = row.workorderId;
        this.form.workorderId = null;
        this.form.workorderCode = null;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = "添加生产工单";
      this.optType="add";
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset();
      this.getTreeselect();
      if (row != null && row.workorderId) {
        this.form.parentId = row.workorderId;
        this.form.orderSource = row.orderSource;
        this.form.sourceCode = row.sourceCode;
        this.form.clientId = row.clientId;
        this.form.clientCode = row.clientCode;
        this.form.clientName = row.clientName;
      } else {
        this.form.parentId = 0;
      }
      this.open = true;
      this.title = "添加生产工单";
      this.optType = "add";
      this.loadRecentOrders().then(() => {
        this.preselectOrderFromRoute();
        if (this.autoGenFlag) {
          this.applyAutoWorkOrderCode();
        }
      });
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      this.getTreeselect();
      const workorderId = row.workorderId || this.ids;
      getWorkorder(workorderId).then(response => {
        this.form = response.data
        this.open = true;
        this.$nextTick(() => {
          this.$refs.barcodeImg.getBarcode();

        })
        this.title = "查看工单信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      if (row != null) {
        this.form.parentId = row.workorderId;
      }
      getWorkorder(row.workorderId).then(response => {
        this.form = response.data;

        this.form.workorderCode = response.data.workorderCode
        this.form.workorderId = response.data.workorderId
        this.autoGenFlag = false;
        this.loadRecentOrders().then(() => {
          if (this.form.orderSource === "ORDER" && this.form.sourceCode && !this.form.orderId) {
            const hit = this.recentOrders.find((o) => o.orderNo === this.form.sourceCode);
            if (hit) this.form.orderId = hit.orderId;
          }
        });
        this.open = true;
        this.title = "修改生产工单";
        this.optType="edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.workorderId != null) {
            updateWorkorder(this.form).then(response => {
              this.$modal.msgSuccess(this.openKittingAfterSave ? "工单已确认，请完成齐套分析" : "修改成功");
              this.$refs["bomlist"]?.getList?.();
              this.getList();
              if (this.openKittingAfterSave) {
                this.openKittingAfterSave = false;
                this.activeName = "kitting";
                this.$nextTick(() => this.$refs.kittingTab?.analyze?.());
              }
            });
          } else {
            addWorkorder(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              //this.open = false;
              this.form.workorderId = response.data;
              this.getList();
            });
          }
        }
      });
    },
    handlePreview(row){
      const base = (this.website && this.website.reportUrl) || '/ureport'
      window.open(`${base}/preview?_u=mysql:生产工单打印模版.ureport.xml&id=${row.workorderId}&code=${row.workorderCode}`)
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除生产工单编号为"' + row.workorderId + '"的数据项？').then(function() {
        return delWorkorder(row.workorderId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleSelectProduct(){
      this.$refs.itemSelect.handleOpen(this.form.productId)
    },
    handleSelectClient(){
      this.$refs.clientSelect.showFlag = true;
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/pro/workorder/export', {
        ...this.queryParams
      }, `workorder_${new Date().getTime()}.xlsx`)
    },
    handleConfirm(){
      this.$modal.confirm("是确认完成工单编制？【确认后将不能更改】").then(() => {
        this.openKittingAfterSave = true;
        this.form.status = "CONFIRMED";
        this.submitForm();
      }).catch(() => {});
    },
    handleFinish(row){
      const workorderIds = row.workorderId || this.ids;
      this.$modal.confirm('确认完成工单？一旦完成，此工单将无法继续报工').then(function() {
        return dofinish(workorderIds) //完成工单
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("更改成功");
      }).catch(() => {});
    },
    handleCancel(row){
      const workorderIds = row.workorderId || this.ids;
      this.$modal.confirm('确认取消工单？一旦完成，此工单将无法继续报工').then(function() {
        return doCancel(workorderIds) //取消工单
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("更改成功");
      }).catch(() => {});
    },
    //物料选择弹出框
    onItemSelected(obj){
      console.log(obj, '----------------')
      if(obj != undefined && obj != null){
        this.form.productId = obj.itemId;
        this.form.productCode = obj.itemCode;
        this.form.productName = obj.itemName;
        this.form.productSpc = obj.specification;
        this.form.unitOfMeasure = obj.unitOfMeasure;
        this.form.unitName = obj.unitName;
      }
    },
    //客户选择弹出框
    onClientSelected(obj){
      if(obj != undefined && obj != null){
        this.form.clientId = obj.clientId;
        this.form.clientCode = obj.clientCode;
        this.form.clientName = obj.clientName;
      }
    },
    //供应商选择
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
      }
    },
    //自动生成编码
    handleAutoGenChange(flag) {
      if (flag) {
        this.applyAutoWorkOrderCode();
      } else {
        this.form.workorderCode = null;
      }
    },
  }
};
</script>

<style scoped>
.workorder-create-dialog :deep(.el-dialog__body) {
  padding-top: 12px;
}
.wo-dialog-intro {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.1), rgba(64, 158, 255, 0.02));
  border: 1px solid rgba(64, 158, 255, 0.2);
}
.wo-dialog-intro strong {
  display: block;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 4px;
}
.wo-dialog-intro small {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
}
.wo-dialog-intro__icon {
  font-size: 22px;
  line-height: 1;
}
.wo-form-section {
  margin-bottom: 14px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}
.wo-form-section__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.wo-order-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: #f5f7fa;
  font-size: 12px;
  color: #606266;
}
.wo-order-summary strong {
  color: #1a1a1a;
}
.wo-code-hint {
  display: inline-flex;
  width: 22px;
  justify-content: center;
  cursor: help;
  color: #909399;
  font-weight: 600;
}
.wo-edit-form :deep(.el-select),
.wo-edit-form :deep(.el-input-number),
.wo-edit-form :deep(.el-date-editor) {
  width: 100%;
}
.wo-product-metrics {
  align-items: flex-start;
}
.wo-qr-col {
  display: flex;
}
.wo-qr-form-item :deep(.el-form-item__label),
.wo-metric-form-item :deep(.el-form-item__label) {
  padding-bottom: 4px;
  line-height: 1.2;
}
.wo-qr-form-item,
.wo-metric-form-item {
  margin-bottom: 0;
}
.wo-qr-form-item :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.wo-qr-field__code {
  margin-top: 6px;
  max-width: 100%;
  font-size: 11px;
  color: #909399;
  text-align: center;
  word-break: break-all;
}
.wo-field-date :deep(.el-input__wrapper) {
  width: 100%;
}
</style>
