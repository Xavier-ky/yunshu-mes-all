<template>
  <div class="app-container scheduling-command-center">
    <div class="scheduling-intro">
      <strong>排产派工（步骤 4）</strong>
      <span class="scheduling-intro__hint">
        在下方列表找到已齐套工单 → 点 <em>排产</em> → 为每道工序新增任务（或点 <em>一键排产</em>）→ 保存后甘特图可见，系统自动生成派工单。
      </span>
    </div>
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="70px" class="scheduling-filter-form">
      <el-form-item label="工单编码" prop="workorderCode">
        <el-input
          v-model="queryParams.workorderCode"
          placeholder="请输入工单编码"
          clearable
          class="scheduling-filter-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input
          v-model="queryParams.productName"
          placeholder="请输入产品名称"
          clearable
          class="scheduling-filter-input"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="需求日期" prop="requestDate">
        <el-date-picker clearable
          v-model="queryParams.requestDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择需求日期"
          class="scheduling-filter-date">
        </el-date-picker>
      </el-form-item>
      <el-form-item class="scheduling-filter-actions">
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
        <el-button type="primary" icon="el-icon-refresh" v-hasPermi="['mes:pro:protask:list']" circle @click="getList"></el-button>
        <el-button type="primary" icon="el-icon-edit" v-hasPermi="['mes:pro:protask:edit']" circle @click="handleOpenGantt"></el-button>
      </el-form-item>
    </el-form>
    <div class="wrapper scheduling-gantt-shell">
      <div class="container scheduling-gantt-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <GanttChar class="left-container" ref="ganttChar" :tasks="tasks"></GanttChar>
      </div>
    </div>
    <div class="scheduling-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table scheduling-table"
        stripe
        border
        :data="workorderList"
        row-key="workorderId"
        default-expand-all
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      >
      <el-table-column label="工单编码" min-width="148" prop="workorderCode" fixed="left">
        <template #default="scope">
          <el-button
            size="small"
            link
            @click="handleView(scope.row)"
            v-hasPermi="['mes:pro:protask:query']"
          >{{scope.row.workorderCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="工单名称" min-width="136" align="center" prop="workorderName" show-overflow-tooltip />
      <el-table-column label="工单来源" min-width="100" align="center" prop="orderSource" show-overflow-tooltip>
        <template #default="scope">
          <dict-tag :options="dict.type.mes_workorder_sourcetype" :value="scope.row.orderSource"/>
        </template>
      </el-table-column>
      <el-table-column label="订单编号" min-width="128" align="center" prop="sourceCode" show-overflow-tooltip />
      <el-table-column label="产品编号" min-width="116" align="center" prop="productCode" show-overflow-tooltip />
      <el-table-column label="产品名称" min-width="148" align="center" prop="productName" show-overflow-tooltip />
      <el-table-column label="规格型号" min-width="108" align="center" prop="productSpc" show-overflow-tooltip />
      <el-table-column label="单位" min-width="72" align="center" prop="unitOfMeasure" show-overflow-tooltip />
      <el-table-column label="工单数量" min-width="96" align="center" prop="quantity" show-overflow-tooltip />
      <el-table-column label="调整数量" min-width="96" align="center" prop="quantityChanged" show-overflow-tooltip />
      <el-table-column label="已生产数量" min-width="112" align="center" prop="quantityProduced" show-overflow-tooltip />
      <el-table-column label="客户编码" min-width="100" align="center" prop="clientCode" show-overflow-tooltip />
      <el-table-column label="客户名称" min-width="132" align="center" prop="clientName" show-overflow-tooltip />
      <el-table-column label="需求日期" min-width="116" align="center" prop="requestDate" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排产状态" min-width="100" align="center" prop="status" show-overflow-tooltip>
        <template #default="scope">
          <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="流程状态" min-width="100" align="center" prop="lifecycleStatus" show-overflow-tooltip>
        <template #default="scope">
          <el-tag size="small" :type="lifecycleTagType(scope.row.lifecycleStatus)">
            {{ lifecycleLabel(scope.row.lifecycleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" align="center" fixed="right" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            v-if="canSchedule(scope.row)"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:pro:protask:edit']"
          >排产</el-button>
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
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="工单编号" prop="workorderCode">
              <el-input v-model="form.workorderCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单名称" prop="workorderName">
              <el-input v-model="form.workorderName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="来源类型" prop="orderSource">
              <el-radio-group v-model="form.orderSource" disabled>
                <el-radio
                  v-for="dict in dict.type.mes_workorder_sourcetype"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="form.orderSource == 'ORDER'">
            <el-form-item label="订单编号" prop="sourceCode">
              <el-input v-model="form.sourceCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排产状态" prop="status">
              <el-select v-model="form.status" disabled>
                <el-option
                  v-for="dict in dict.type.mes_order_status"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="产品编号" prop="productCode">
              <el-input v-model="form.productCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="规格型号" prop="productSpc">
              <el-input v-model="form.productSpc" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unitOfMeasure">
              <el-input v-model="form.unitOfMeasure" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工单数量" prop="quantity">
              <el-input v-model="form.quantity" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求日期" prop="requestDate">
              <el-input :model-value="formatDateOnly(form.requestDate) || '—'" readonly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.orderSource == 'ORDER'">
          <el-col :span="12">
            <el-form-item label="客户编号" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col></el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-steps :active="activeProcess" v-if="form.workorderId !=null && processOptions.length" align-center simple>
        <el-step
          v-for="(item,index) in processOptions"
          :key="item.processId || index"
          :title="item.processName"
          @click="handleStepClick(index)"
        />
      </el-steps>
      <el-alert
        v-else-if="form.workorderId != null && !processLoading && !processOptions.length"
        type="warning"
        :closable="false"
        show-icon
        title="该产品未配置工艺路线，请先在主数据维护 product_route / process_route。"
        class="scheduling-process-alert"
      />
      <el-card v-if="form.workorderId != null && activeProcessItem">
        <ProTask
          ref="proTask"
          :workorderId="form.workorderId"
          :workorderQty="form.quantity"
          :routeId="activeProcessItem.routeId"
          :processId="activeProcessItem.processId"
          :processCode="activeProcessItem.processCode"
          :processName="activeProcessItem.processName"
          :colorCode="activeProcessItem.colorCode"
          :optType="optType"
          @tasks-changed="onTasksChanged"
        />
      </el-card>
      <template #footer><div class="dialog-footer">
        <el-button
          type="success"
          @click="handleQuickSchedule"
          v-if="canSchedule(form) && processOptions.length && optType === 'edit'"
          :loading="quickScheduling"
        >一键排产（全部工序）</el-button>
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">确 定</el-button>
        <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && optType !='view'  && form.workorderId !=null">完成</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkorder, getWorkorder, delWorkorder, addWorkorder, updateWorkorder } from "@/yunshu-ui/api/mes/pro/workorder";
import {listGanttTaskList, addProtask, listProtask} from "@/yunshu-ui/api/mes/pro/protask";
import { listProductprocess } from "@/yunshu-ui/api/mes/pro/routeprocess";
import ProTask from "./proTask.vue";
import GanttChar from "./ganttx.vue";
import { formatDateOnly } from "@/yunshu-ui/pro/workorder/workorder-form-utils";
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";

const LIFECYCLE_LABELS = {
  RELEASED: "已下达",
  KITTING_OK: "齐套完成",
  SCHEDULED: "已排产",
  MATERIAL_ISSUED: "已领料",
  IN_PROGRESS: "生产中",
  QC_PENDING: "待质检",
  QC_PASSED: "质检通过",
  COMPLETED: "已完工",
};

const GANTT_COLORS = ["#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#9B59B6", "#1ABC9C"];

/** 工序默认工位（演示用） */
const DEFAULT_STATIONS = {
  "STEP-MOTOR": { workstationId: 1, workstationCode: "ST-01", workstationName: "电机装配工位", lineId: 1 },
  "STEP-BLADE": { workstationId: 2, workstationCode: "ST-02", workstationName: "风叶组装工位", lineId: 1 },
  "STEP-AGING": { workstationId: 3, workstationCode: "ST-03", workstationName: "老化测试工位", lineId: 1 },
  "STEP-PACK": { workstationId: 4, workstationCode: "ST-04", workstationName: "包装工位", lineId: 1 },
};

export default {
  name: "Workorder",
  dicts: ['mes_order_status','mes_workorder_sourcetype'],
  components: {
    Treeselect,
    ProTask,
    GanttChar
  },
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
      activeProcess: 0,
      processLoading: false,
      quickScheduling: false,
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 生产工单表格数据
      workorderList: [],
      // 生产工单树选项
      workorderOptions: [],
      //当前生产工单中产品对应的工序列表
      processOptions: [],
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
        workorderType: 'SELF', //这里的排产要排除自产之外的外协和外购
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
        status: 'CONFIRMED',
      },
      tasks:{
        data: [],
        links: []
      },
      // 表单参数
      form: {},
    };
  },
  computed: {
    activeProcessItem() {
      if (!this.form.workorderId || !this.processOptions?.length) {
        return null;
      }
      return this.processOptions[this.activeProcess] || null;
    },
  },
  created() {
    const wo = this.$route?.query?.wo;
    if (wo) {
      this.queryParams.workorderCode = String(wo);
    }
    this.getList();
    this.getGanttTasks();
  },
  methods: {
    formatDateOnly,
    /** 查询生产工单列表 */
    getList() {
      this.loading = true;
      listWorkorder(this.queryParams).then(response => {
        this.workorderList = this.handleTree(response.rows, "workorderId", "parentId");
        this.total = response.total;
        this.loading = false;
        this.tryOpenScheduleFromRoute();
      });
    },
    tryOpenScheduleFromRoute() {
      const wo = this.$route?.query?.wo;
      if (!wo || this._routeScheduleOpened) return;
      const flat = [];
      const walk = (rows) => {
        (rows || []).forEach((r) => {
          flat.push(r);
          if (r.children?.length) walk(r.children);
        });
      };
      walk(this.workorderList);
      const hit = flat.find((r) => r.workorderCode === wo);
      if (hit && this.canSchedule(hit)) {
        this._routeScheduleOpened = true;
        this.$nextTick(() => this.handleUpdate(hit));
      }
    },
    lifecycleLabel(status) {
      return LIFECYCLE_LABELS[status] || status || "—";
    },
    lifecycleTagType(status) {
      if (status === "SCHEDULED" || status === "COMPLETED") return "success";
      if (status === "KITTING_OK") return "warning";
      return "info";
    },
    canSchedule(row) {
      if (!row) return false;
      const ls = row.lifecycleStatus;
      const schedulable = !ls || ls === "RELEASED" || ls === "KITTING_OK" || ls === "SCHEDULED";
      return row.status === "CONFIRMED" && schedulable;
    },
    onTasksChanged() {
      this.getGanttTasks();
      this.getList();
    },
    handleOpenGantt(){
      this.$router.push({ path: '/app/planning/scheduling/ganttedit' })
    },
    getGanttTasks(){
      listGanttTaskList(this.queryParams).then(response => {
        const payload = response?.data || {};
        this.tasks.data = payload.data || [];
        this.tasks.links = payload.links || [];
        this.$nextTick(() => {
          this.$refs.ganttChar?.reload();
        });
      }).catch(() => {
        this.tasks.data = [];
        this.tasks.links = [];
      });
    },

    //获取当前产品对应的生产工序
    getProcess(){
        this.processLoading = true;
        listProductprocess(this.form.productId).then(response => {
            const rows = response.rows || response.data || [];
            this.processOptions = rows.map((item, index) => ({
              ...item,
              colorCode: item.colorCode || GANTT_COLORS[index % GANTT_COLORS.length],
            }));
            this.activeProcess = 0;
            this.$nextTick(() => {
              this.$refs.proTask?.getList?.();
            });
        }).catch(() => {
            this.processOptions = [];
        }).finally(() => {
            this.processLoading = false;
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
        data.children = this.handleTree(response.data, "workorderId", "parentId");
        this.workorderOptions.push(data);
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
        workorderId: null,
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
        status: "PREPARE",
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.activeProcess =0;
      this.autoGenFlag = false;
      this.resetForm("form");
    },
    //甘特图按钮点击
    openGanttChart(){
      this.$refs.ganttChar.showFlag =true;
    },
    //Step点击
    handleStepClick(index){
        this.activeProcess = index;
        this.$nextTick(() => {
          this.$refs.proTask?.getList?.();
        });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
      this.getGanttTasks();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    //从BOM行中直接新增
    handleSubAdd(row){
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
      this.optType="add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      this.getTreeselect();
      const workorderId = row.workorderId || this.ids;
      getWorkorder(workorderId).then(response => {
        this.form = response.data;
        this.form.requestDate = formatDateOnly(response.data.requestDate);
        this.open = true;
        this.title = "查看工单信息";
        this.optType = "view";
        this.getProcess();
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
        this.form.requestDate = formatDateOnly(response.data.requestDate);
        this.open = true;
        this.title = "生产排产";
        this.optType="edit";
        this.getProcess();
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.workorderId != null) {
            updateWorkorder(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              //this.open = false;
              this.$refs["bomlist"].getList();
              this.getList();
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
      this.$refs.itemSelect.showFlag = true;
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
    handleFinish(){
      let that = this;
      this.$modal.confirm('是否完成工单编制？【完成后将不能更改】').then(function(){
        that.form.status = 'CONFIRMED';
        that.submitForm();
      });
    },
    //物料选择弹出框
    onItemSelected(obj){
        if(obj != undefined && obj != null){
          this.form.productId = obj.itemId;
          this.form.productCode = obj.itemCode;
          this.form.productName = obj.itemName;
          this.form.productSpc = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
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
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('WORKORDER_CODE').then(response =>{
          this.form.workorderCode = response;
        });
      }else{
        this.form.workorderCode = null;
      }
    },
    pad2(n) {
      return String(n).padStart(2, "0");
    },
    formatDateTime(dt) {
      return `${dt.getFullYear()}-${this.pad2(dt.getMonth() + 1)}-${this.pad2(dt.getDate())} ${this.pad2(dt.getHours())}:00:00`;
    },
    async handleQuickSchedule() {
      if (!this.form.workorderId || !this.processOptions.length) return;
      const existing = await listProtask({
        pageNum: 1,
        pageSize: 100,
        workorderId: this.form.workorderId,
      }).catch(() => ({ rows: [] }));
      if ((existing.rows || []).length > 0) {
        this.$modal.confirm("该工单已有排产任务，是否继续为缺失工序补建？").then(() => {
          this.runQuickSchedule(existing.rows || []);
        }).catch(() => {});
        return;
      }
      this.runQuickSchedule([]);
    },
    async runQuickSchedule(existingTasks) {
      this.quickScheduling = true;
      const existingProcessIds = new Set(
        existingTasks.map((t) => Number(t.processId)).filter(Boolean)
      );
      const qty = Number(this.form.quantity) || 100;
      const base = new Date();
      base.setHours(8, 0, 0, 0);
      let created = 0;
      try {
        for (let i = 0; i < this.processOptions.length; i += 1) {
          const proc = this.processOptions[i];
          if (existingProcessIds.has(Number(proc.processId))) continue;
          const station = DEFAULT_STATIONS[proc.processCode] || DEFAULT_STATIONS["STEP-MOTOR"];
          const start = new Date(base.getTime() + i * 24 * 3600 * 1000);
          const duration = 1;
          const end = new Date(start.getTime() + duration * 8 * 3600 * 1000);
          await addProtask({
            workorderId: this.form.workorderId,
            routeId: proc.routeId,
            processId: proc.processId,
            processCode: proc.processCode,
            processName: proc.processName,
            workstationId: station.workstationId,
            workstationCode: station.workstationCode,
            workstationName: station.workstationName,
            lineId: station.lineId,
            quantity: qty,
            duration,
            startTime: this.formatDateTime(start),
            endTime: this.formatDateTime(end),
            colorCode: proc.colorCode || GANTT_COLORS[i % GANTT_COLORS.length],
          });
          created += 1;
        }
        if (created > 0) {
          this.$modal.msgSuccess(`已创建 ${created} 条排产任务，甘特图与派工已更新`);
        } else {
          this.$modal.msgWarning("所有工序已有任务，无需重复创建");
        }
        this.onTasksChanged();
        this.$refs.proTask?.getList?.();
      } catch (e) {
        this.$modal.msgError(e?.message || "一键排产失败");
      } finally {
        this.quickScheduling = false;
      }
    },
  }
};
</script>
<style scoped>
.scheduling-intro {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.25);
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08), rgba(64, 158, 255, 0.02));
}
.scheduling-intro strong {
  flex-shrink: 0;
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
}
.scheduling-intro__hint {
  flex: 1;
  min-width: 280px;
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
}
.scheduling-intro em {
  font-style: normal;
  color: #409eff;
  font-weight: 600;
}
.scheduling-process-alert {
  margin: 12px 0;
}
.scheduling-filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  margin-bottom: 8px;
}
.scheduling-filter-form :deep(.el-form-item) {
  margin: 0;
}
.scheduling-filter-input {
  width: 200px;
}
.scheduling-filter-date {
  width: 180px;
}
.scheduling-filter-form :deep(.el-form-item.scheduling-filter-actions) {
  margin-left: 208px;
}
.scheduling-filter-actions :deep(.el-form-item__content) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
