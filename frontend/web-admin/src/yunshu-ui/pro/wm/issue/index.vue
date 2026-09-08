<template>
  <div class="app-container outbound-doc-panel">
    <WorkflowContextBar :work-order-code="workflowWoCode" />
    <div class="issue-intro">
      <strong>生产领料（步骤 6）</strong>
      <span class="issue-intro__hint">
        点 <em>从工单生成</em> 选择已排产工单并自动带入 BOM → 或 <em>新增</em> 手工建单 → 提交 → 拣货 → 执行领出。
      </span>
    </div>
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="issueCode">
          <el-input v-model="queryParams.issueCode" placeholder="领料单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="warehouseName">
          <el-input v-model="queryParams.warehouseName" placeholder="仓库" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="单据状态" clearable>
            <el-option
              v-for="dict in dict.type.mes_issue_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">生产领料</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:issueheader:add']">新增</el-button>
        <el-button type="primary" plain icon="el-icon-document-add" size="default" :loading="creatingFromWo" @click="handleCreateFromWorkorder" v-hasPermi="['mes:wm:issueheader:add']">从工单生成</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:issueheader:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:issueheader:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:wm:issueheader:export']">导出</el-button>
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
        :data="issueheaderList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="领料单编号" align="center" header-align="center" min-width="130" prop="issueCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.issueCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="领料单名称" align="center" header-align="center" min-width="120" prop="issueName" show-overflow-tooltip />
        <el-table-column label="生产工单" align="center" header-align="center" min-width="120" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="工作站" align="center" header-align="center" min-width="100" prop="workstationName" show-overflow-tooltip />
        <el-table-column label="需求时间" align="center" header-align="center" prop="requiredTime" width="150">
          <template #default="scope">
            <span>{{ parseTime(scope.row.requiredTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_issue_status" :value="scope.row.status"/>
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
                v-hasPermi="['mes:wm:issueheader:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status =='PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='APPROVING'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >执行拣货</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='APPROVING'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='APPROVED'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >执行领出</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:issueheader:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:issueheader:remove']"
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

    <!-- 添加或修改生产领料单头对话框 -->
    <el-dialog
      :title="title"
      v-model="open"
      width="min(920px, 96vw)"
      append-to-body
      class="issue-form-dialog"
      destroy-on-close
      @close="reset"
    >
      <div v-if="optType === 'add'" class="issue-dialog-intro">
        <span class="issue-dialog-intro__icon" aria-hidden="true">📦</span>
        <div>
          <strong>新建生产领料单</strong>
          <small>选择生产工单后保存，系统会展开物料行；也可直接使用「从工单生成」一键带入 BOM。</small>
        </div>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="issue-edit-form">
        <div class="issue-form-section">
          <div class="issue-form-section__title">单据信息</div>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="领料单编号" prop="issueCode">
                <el-input v-model="form.issueCode" placeholder="请输入领料单编号" :disabled="autoGenFlag && optType === 'add'" />
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
              <el-form-item label="领料单名称" prop="issueName">
                <el-input v-model="form.issueName" placeholder="选择工单后可自动生成" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="需求时间" prop="requiredTime">
                <el-date-picker
                  clearable
                  v-model="form.requiredTime"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="请选择需求日期时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        <div class="issue-form-section">
          <div class="issue-form-section__title">生产关联</div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="生产工单" prop="workorderCode">
                <el-input v-model="form.workorderCode" placeholder="点击右侧搜索选择工单" readonly>
                  <template #append><el-button icon="el-icon-search" @click="handleWorkorderSelect"></el-button></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="工作站" prop="workstationCode">
                <el-input v-model="form.workstationCode" placeholder="可选，点击搜索选择" readonly>
                  <template #append><el-button icon="el-icon-search" @click="handleWorkstationSelect"></el-button></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="工作站名称" prop="workstationName">
                <el-input v-model="form.workstationName" readonly placeholder="选择工作站后带出" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="客户编号">
                <el-input v-model="form.clientCode" readonly placeholder="选择工单后带出" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="客户名称">
                <el-input v-model="form.clientName" readonly placeholder="选择工单后带出" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
        <div class="issue-form-section issue-form-section--last">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
          </el-form-item>
        </div>
      </el-form>
      <template v-if="form.issueId != null">
        <el-divider content-position="center">物料信息</el-divider>
        <div v-if="form.status =='PREPARE'" class="issue-lines-panel">
          <Issueline ref="line" :issueId="form.issueId" :warehouseId="form.warehouseId" :locationId="form.locationId" :areaId="form.areaId" :optType="optType" />
        </div>
        <div v-else class="issue-lines-panel">
          <IssueDetail ref="detail" :issueId="form.issueId" :optType="optType" />
        </div>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.issueId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='APPROVING' && form.issueId !=null && optType !='view' ">提 交</el-button>
          <el-button type="danger" @click="cancel" v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' ">取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected" />
    <WorkstationSelect ref="wsSelect" @onSelected="onWorkstationSelected" />
  </div>
</template>

<script>
import { listIssueheader, getIssueheader, checkQuantity, delIssueheader, addIssueheader, updateIssueheader, execute, createIssueFromWorkOrder } from "@/yunshu-ui/api/mes/wm/issueheader";
import { listWorkorder } from "@/yunshu-ui/api/mes/pro/workorder";
import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import Issueline from "./line.vue";
import IssueDetail from "./detail.vue";
import WorkflowContextBar from "@/yunshu-ui/components/WorkflowContextBar.vue";
export default {
  name: "Issueheader",
  dicts: ['mes_issue_status'],
  components: {Issueline,IssueDetail,WorkstationSelect,WorkorderSelect,WorkflowContextBar},
  data() {
    return {
      autoGenFlag:false,
      creatingFromWo: false,
      woPickMode: null,
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
      // 生产领料单头表格数据
      issueheaderList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        issueCode: null,
        issueName: null,
        workstationId: null,
        workstationCode: null,
        workorderId: null,
        workorderCode: null,
        taskId: null,
        taskCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        issueDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        issueCode: [
          { required: true, message: "领料单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        issueName: [
          { required: true, message: "领料单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        requiredTime: [
          { required: true, message: "请指定需求日期时间", trigger: "blur" }
        ],
        workorderCode: [
        { required: true, message: "请指定生产工单", trigger: "blur" }
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
    this.getWarehouseList();
  },
  methods: {
    /** 查询生产领料单头列表 */
    getList() {
      this.loading = true;
      listIssueheader(this.queryParams).then(response => {
        this.issueheaderList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getWarehouseList(){
      getTreeList().then( response =>{
        if(response.data){
          this.warehouseOptions = response.data.filter((el) =>{
              return el.warehouseCode.indexOf('VIR') == -1;
          });;
        }
        this.warehouseOptions.map(w =>{
          w.children.map(l =>{
                  let lstr =JSON.stringify(l.children).replace(/locationId/g,'lId').replace(/areaId/g, 'pId').replace(/areaName/g,'pName');
                  l.children = JSON.parse(lstr);
          });

          let wstr = JSON.stringify(w.children).replace(/warehouseId/g,'wId').replace(/locationId/g, 'pId').replace(/locationName/g,'pName');
          w.children =  JSON.parse(wstr);

        });
        let ostr=JSON.stringify(this.warehouseOptions).replace(/warehouseId/g,'pId').replace(/warehouseName/g, 'pName');
        this.warehouseOptions = JSON.parse(ostr);
      });
    },
    // 关闭按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        issueId: null,
        issueCode: null,
        issueName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        clientId:null,
        clientCode:null,
        clientName:null,
        taskId: null,
        taskCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        requiredTime: null,
        issueDate: null,
        status: 'PREPARE',
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
      this.woPickMode = null;
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
      this.ids = selection.map(item => item.issueId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加生产领料单";
      this.optType = "add";
      this.autoGenFlag = true;
      this.form.requiredTime = this.formatDateTime(new Date(Date.now() + 86400000));
      this.form.issueDate = this.formatDateTime(new Date());
      this.applyAutoIssueCode();
      this.prefillWorkorderFromRoute();
    },
    prefillWorkorderFromRoute() {
      const woCode = this.$route?.query?.wo;
      if (!woCode) {
        return;
      }
      listWorkorder({ workorderCode: String(woCode), pageNum: 1, pageSize: 1, status: "CONFIRMED" }).then((response) => {
        const row = response?.rows?.[0];
        if (row) {
          this.onWorkorderSelected(row);
          if (!this.form.issueName) {
            this.form.issueName = `生产领料-${row.workorderCode}`;
          }
        }
      }).catch(() => {});
    },
    formatDateTime(dt) {
      const d = dt instanceof Date ? dt : new Date(dt);
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    },
    applyAutoIssueCode() {
      genCode("ISSUE_CODE").then((response) => {
        const code = response?.data ?? response;
        if (typeof code === "string" && code.trim()) {
          this.form.issueCode = code.trim();
        }
      }).catch(() => {
        this.form.issueCode = `IS${Date.now() % 100000}`;
      });
    },
    openIssueDialog(issueId, title, optType) {
      return getIssueheader(issueId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = title;
        this.optType = optType;
        this.getList();
      });
    },
    /** 从工单一键生成领料单 */
    handleCreateFromWorkorder() {
      const woCode = this.$route?.query?.wo;
      if (woCode) {
        this.doCreateIssueFromWoCode(String(woCode));
        return;
      }
      this.woPickMode = "createIssue";
      this.$nextTick(() => {
        this.$refs.woSelect?.handleOpen(null, {
          title: "选择生产工单",
          hint: "请选择已齐套/已排产的工单，系统将自动生成领料单并带入 BOM 物料",
        });
      });
    },
    doCreateIssueFromWoCode(woCode) {
      this.creatingFromWo = true;
      listWorkorder({ workorderCode: woCode, pageNum: 1, pageSize: 1, status: "CONFIRMED" })
        .then((response) => {
          const wo = response?.rows?.[0];
          if (!wo?.workorderId) {
            throw new Error(`未找到可领料的工单：${woCode}`);
          }
          return this.doCreateIssueFromWorkOrderId(wo.workorderId);
        })
        .catch((err) => {
          this.$modal.msgError(err?.message || "从工单生成领料单失败");
        })
        .finally(() => {
          this.creatingFromWo = false;
        });
    },
    doCreateIssueFromWorkOrderId(workorderId) {
      if (!workorderId) {
        this.$modal.msgError("工单 ID 无效");
        return Promise.reject(new Error("工单 ID 无效"));
      }
      this.creatingFromWo = true;
      return createIssueFromWorkOrder(workorderId)
        .then((res) => {
          const issueId = res?.data ?? res;
          if (!issueId || typeof issueId === "object") {
            throw new Error("生成领料单失败：未返回有效 ID");
          }
          return this.openIssueDialog(issueId, "编辑生产领料单", "edit");
        })
        .then(() => {
          this.$modal.msgSuccess("已根据工单 BOM 生成领料单，请核对物料行后提交");
        })
        .catch((err) => {
          this.$modal.msgError(err?.message || "从工单生成领料单失败");
          throw err;
        })
        .finally(() => {
          this.creatingFromWo = false;
        });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const issueId = row.issueId || this.ids
      getIssueheader(issueId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改生产领料单";
        this.optType = "edit";
      });
    },

    /**
     * 拣货按钮操作
     */
     handleStocking(row){
      this.reset();
      const issueId = row.issueId || this.ids
      getIssueheader(issueId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "领料单拣货";
        this.optType = "edit";
      });
    },

     // 查询明细按钮操作
    handleView(row){
      this.reset();
      const issueIds = row.issueId
      getIssueheader(issueIds).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.open = true;
        this.title = "查看领料单信息";
        this.optType = "view";
      });
    },
    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return;
        }
        if (this.form.issueId != null) {
          updateIssueheader(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.getList();
          }).catch((err) => {
            this.$modal.msgError(err?.message || "保存失败");
          });
          return;
        }
        addIssueheader(this.form).then((response) => {
          const issueId = response?.data ?? response;
          if (!issueId) {
            this.$modal.msgError("新增失败：未返回领料单 ID");
            return;
          }
          this.$modal.msgSuccess("领料单已创建，请继续添加物料行");
          return getIssueheader(issueId).then((res) => {
            this.form = res.data;
            this.optType = "edit";
            this.title = "编辑生产领料单";
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
          updateIssueheader(this.form).then(response => {
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
      checkQuantity(this.form.issueId).then( response =>{
        if(response.data){
            that.form.status = 'APPROVED';
            updateIssueheader(that.form).then(response => {
              that.$modal.msgSuccess("提交成功");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'APPROVING';
            });    
          }else{
            this.$modal.confirm('领料数量与拣货数量不一致，确认提交?').then(function() {
              that.form.status = 'APPROVED';
              updateIssueheader(that.form).then(response => {
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
      this.$modal.confirm('确认撤销领料单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
          updateIssueheader(that.form).then(response => {
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

    //执行出库
    handleExecute(row){
      const issueIds = row.issueId || this.ids;
      this.$modal.confirm('确认执行出库？').then(() => {
        return execute(issueIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("出库成功");
      }).catch((err) => {
        if (err !== 'cancel' && err?.message !== 'cancel') {
          this.$modal.msgError(err?.message || "执行领出失败");
        }
      });
    },
    /** 列表：草稿提交到待拣货 */
    handleSubmit(row) {
      const issueId = row.issueId;
      this.$modal.confirm('确认提交该领料单？').then(() => {
        return getIssueheader(issueId).then(response => {
          const data = { ...response.data, status: 'APPROVING' };
          return updateIssueheader(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待拣货提交到待执行 */
    handleSubmitExecute(row) {
      const issueId = row.issueId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return checkQuantity(issueId).then(response => {
          const doSubmit = () => getIssueheader(issueId).then(res => {
            const data = { ...res.data, status: 'APPROVED' };
            return updateIssueheader(data);
          });
          if (response.data) {
            return doSubmit();
          }
          return this.$modal.confirm('领料数量与拣货数量不一致，确认提交?').then(() => doSubmit());
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const issueId = row.issueId;
      this.$modal.confirm('确认撤销领料单？').then(() => {
        return getIssueheader(issueId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateIssueheader(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const issueIds = row.issueId || this.ids;
      this.$modal.confirm('是否确认删除生产领料单头编号为"' + issueIds + '"的数据项？').then(function() {
        return delIssueheader(issueIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/issueheader/export', {
        ...this.queryParams
      }, `issueheader_${new Date().getTime()}.xlsx`)
    },
    //选择默认的仓库、库区、库位
    handleWarehouseChanged(obj){
      if(obj !=null){
        this.form.warehouseId = obj[0];
        this.form.locationId = obj[1];
        this.form.areaId = obj[2];
      }
    },
    //选择工作站
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
    //选择生产工单
    handleWorkorderSelect(){
      this.woPickMode = "form";
      this.$refs.woSelect.handleOpen(this.form.workorderId, {
        title: "选择生产工单",
        workorderCode: this.form.workorderCode || undefined,
      });
    },
    onWorkorderSelected(row){
      if (this.woPickMode === "createIssue") {
        this.woPickMode = null;
        if (row?.workorderId) {
          this.doCreateIssueFromWorkOrderId(row.workorderId);
        }
        return;
      }
      this.woPickMode = null;
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.clientId = row.clientId;
        this.form.clientCode = row.clientCode;
        this.form.clientName = row.clientName;
        if (!this.form.issueName) {
          this.form.issueName = `生产领料-${row.workorderCode}`;
        }
        if (!this.form.requiredTime && row.requestDate) {
          this.form.requiredTime = this.formatDateTime(row.requestDate);
        }
      }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        this.applyAutoIssueCode();
      }else{
        this.form.issueCode = null;
      }
    }
  }
};
</script>

<style scoped>
.issue-intro {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 6px;
  font-size: 13px;
}
.issue-intro__hint {
  color: #606266;
  flex: 1;
  min-width: 200px;
}
.issue-intro__hint em {
  font-style: normal;
  color: #409eff;
  font-weight: 500;
}
.issue-form-dialog :deep(.el-dialog__body) {
  max-height: min(78vh, 820px);
  overflow-x: hidden;
  overflow-y: auto;
  padding-top: 12px;
}
.issue-dialog-intro {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.1), rgba(64, 158, 255, 0.02));
  border: 1px solid rgba(64, 158, 255, 0.2);
}
.issue-dialog-intro strong {
  display: block;
  color: #1a1a1a;
  font-size: 14px;
  margin-bottom: 4px;
}
.issue-dialog-intro small {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
}
.issue-dialog-intro__icon {
  font-size: 22px;
  line-height: 1;
}
.issue-form-section {
  margin-bottom: 14px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}
.issue-form-section--last {
  border-bottom: none;
  margin-bottom: 0;
}
.issue-form-section__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.issue-edit-form :deep(.el-input),
.issue-edit-form :deep(.el-select),
.issue-edit-form :deep(.el-date-editor) {
  width: 100%;
}
.issue-lines-panel {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 8px 10px 4px;
  background: #fafbfc;
}
.issue-lines-panel :deep(.issue-line-root) {
  padding: 0;
  min-height: 0;
  height: auto;
}
</style>
