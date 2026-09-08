<template>
  <div class="app-container inbound-doc-panel feedback-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        class="inbound-filter-form"
        label-width="0"
        @submit.prevent
      >
        <el-form-item prop="feedbackType">
          <el-select v-model="queryParams.feedbackType" placeholder="报工类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_feedback_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item prop="workstationName">
          <el-input
            v-model="queryParams.workstationName"
            placeholder="工作站名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="workorderCode">
          <el-input
            v-model="queryParams.workorderCode"
            placeholder="生产工单编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="itemCode">
          <el-input
            v-model="queryParams.itemCode"
            placeholder="产品物料编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="itemName">
          <el-input
            v-model="queryParams.itemName"
            placeholder="产品物料名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="报工人"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="recordUser">
          <el-input
            v-model="queryParams.recordUser"
            placeholder="记录人"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="状态" clearable>
            <el-option
              v-for="dict in dict.type.mes_feedback_status"
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
        <span class="inbound-page-title">生产报工</span>
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['mes:pro:feedback:add']"
        >新增</el-button>
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="default"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:pro:feedback:edit']"
        >修改</el-button>
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:pro:feedback:remove']"
        >删除</el-button>
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="default"
          @click="handleExport"
          v-hasPermi="['mes:pro:feedback:export']"
        >导出</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table feedback-doc-table"
        stripe
        border
        height="100%"
        :data="feedbackList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="报工单号" align="center" header-align="center" min-width="118" prop="feedbackCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click="handleView(scope.row)">
              {{ scope.row.feedbackCode }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="报工类型" align="center" header-align="center" width="88" prop="feedbackType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_feedback_type" :value="scope.row.feedbackType" />
          </template>
        </el-table-column>
        <el-table-column label="工作站" align="center" header-align="center" min-width="92" prop="workstationName" show-overflow-tooltip />
        <el-table-column label="工序" align="center" header-align="center" min-width="92" prop="processName" show-overflow-tooltip />
        <el-table-column label="生产工单" align="center" header-align="center" min-width="108" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="物料名称" align="center" header-align="center" min-width="120" prop="itemName" show-overflow-tooltip />
        <el-table-column label="报工数量" align="center" header-align="center" width="80" prop="quantityFeedback" />
        <el-table-column label="报工人" align="center" header-align="center" min-width="80" prop="nickName" show-overflow-tooltip />
        <el-table-column label="报工时间" align="center" header-align="center" width="112" prop="feedbackTime">
          <template #default="scope">
            <span>{{ parseTime(scope.row.feedbackTime, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="88" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_feedback_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status === 'PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:pro:feedback:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-s-promotion"
                v-if="scope.row.status === 'PREPARE' && scope.row.recordId"
                @click="handleSubmitRow(scope.row)"
                v-hasPermi="['mes:pro:feedback:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="canApproveRow(scope.row)"
                @click="handleApprove(scope.row)"
                v-hasPermi="['mes:pro:feedback:approve']"
              >审核</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="canApproveRow(scope.row)"
                @click="handleExecuteRow(scope.row)"
                v-hasPermi="['mes:pro:feedback:approve']"
              >执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="canApproveRow(scope.row)"
                @click="handleRejectRow(scope.row)"
                v-hasPermi="['mes:pro:feedback:approve']"
              >驳回</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status === 'PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:pro:feedback:remove']"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      layout="prev, pager, next"
      :pager-count="5"
      :auto-scroll="false"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改生产报工记录对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" :disabled="optType === 'view'">
        <el-row>
          <el-col :span="8">
            <el-form-item label="报工类型" prop="feedbackType">
              <el-select v-model="form.feedbackType" placeholder="请选择报工类型">
                <el-option
                  v-for="dict in dict.type.mes_feedback_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="报工单号" prop="feedbackCode">
              <el-input v-model="form.feedbackCode" readonly="readonly" placeholder="系统自动生成"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="请选择生产工单" >
                <template #append><el-button icon="el-icon-search" @click="handleWorkorderSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工作站" prop="workstationName">
              <el-input v-model="form.workstationName" placeholder="请选择工作站" >
                <template #append><el-button icon="el-icon-search" @click="handleWorkstationSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelect" @onSelected="onWorkstationSelected"></WorkstationSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产任务" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="请选择生产任务" >
                <template #append><el-button icon="el-icon-search" @click="handleTaskSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <ProtaskSelect ref="taskSelect" :workorderId="form.workorderId" :workstationId="form.workstationId" @onSelected="onTaskSelected"> </ProtaskSelect>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="产品编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品名称" prop="itemName">
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
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" type="textarea" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.isCheck == 'N'">
          <el-col :span="8">
            <el-form-item label="报工数量" prop="quantityFeedback">
              <el-input readonly="readonly" v-model="form.quantityFeedback" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格品数量" prop="quantityQualified">
              <el-input-number :min="0" @change="handleQuantityChanged" v-model="form.quantityQualified" placeholder="请输入合格品数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不良品数量" prop="quantityUnquanlified">
              <el-input-number :min="0" @change="handleQuantityChanged" v-model="form.quantityUnquanlified" placeholder="请输入不良品数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-else>
          <el-col :span="8">
            <el-form-item label="报工数量" prop="quantityFeedback">
              <el-input-number :min="0" :max="99999" v-model="form.quantityFeedback" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.isCheck == 'N' && form.quantityUnquanlified >0">
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
            <el-form-item label="报工人" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请选择报工人" >
                <template #append><el-button @click="handleUserSelect" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected"></UserSingleSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报工时间" prop="feedbackTime">
              <el-date-picker clearable
                v-model="form.feedbackTime"
                type="datetime"                
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择日期时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="审核人" prop="recordNick">
              <el-input v-model="form.recordNick" placeholder="请选择审核人" >
                <template #append><el-button @click="handleUser2Select" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="user2Select" @onSelected="onUser2Selected"></UserSingleSelect>
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
      <el-tabs type="border-card"  v-model="activeName"  v-if="form.recordId != null && form.status !='PREPARE' && form.status !='APPROVING' ">
        <el-tab-pane label="BOM物资消耗" name="consume">
          <ItemConsume :feedbackId="form.recordId"></ItemConsume>
        </el-tab-pane>
        <el-tab-pane label="产品产出" name="produce">
          <ProductProduce :feedbackId="form.recordId"></ProductProduce>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="primary" @click="handleSubmit" v-if="form.status =='PREPARE' && optType !='view' && form.recordId !=null ">提交审批</el-button>
        <el-button type="success" @click="approve" v-if="form.status =='APPROVING'  && form.recordId !=null && form.recordUser == user.userName">审批通过</el-button>
        <el-button type="danger"  @click="handleReject" v-if="form.status =='APPROVING'  && form.recordId !=null && form.recordUser == user.userName">审批不通过</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listFeedback, getFeedback, delFeedback, addFeedback, updateFeedback, execute } from "@/yunshu-ui/api/mes/pro/feedback";
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue"
import ProtaskSelect from "@/yunshu-ui/components/TaskSelect/taskSelectSingle.vue"
import ProductProduce from './produce.vue';
import ItemConsume from './consume.vue'
import {getUserProfile} from "@/yunshu-ui/api/system/user";

export default {
  name: "Feedback",
  components: {WorkorderSelect,WorkstationSelect,UserSingleSelect,ProtaskSelect,ItemConsume,ProductProduce},
  dicts: ['mes_feedback_status', 'mes_feedback_type'],
  data() {
    return {
      user: {}, // 当前登录用户
      activeName: "consume", // 选项卡名称
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
      // 生产报工记录表格数据
      feedbackList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        feedbackType: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        unitOfMeasure: null,
        specification: null,
        quantity: null,
        quantityFeedback: null,
        quantityQualified: null,
        quantityUnquanlified: null,
        userName: null,
        nickName: null,
        feedbackChannel: null,
        feedbackTime: null,
        recordUser: null,
        recordNick: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        feedbackType: [
          { required: true, message: "报工类型不能为空", trigger: "change" }
        ],
        taskCode: [
          { required: true, message: "请选择生产任务", trigger: "blur" }
        ],       
        quantityFeedback: [
          { required: true, message: "请填写报工数量", trigger: "blur" }
        ],
        nickName: [
          { required: true, message: "请选择报工人", trigger: "blur"}
        ],
        recordNick: [
          { required: true, message: "请选择审核人", trigger: "blur"}
        ],
        feedbackTime: [
          { required: true, message: "请选择报工时间", trigger: "blur"}
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    getUserProfile().then(response => {      
      this.user = response.data;
    });
  },
  methods: {
    resolveRecordId(row) {
      if (row?.recordId != null) return row.recordId;
      if (Array.isArray(this.ids)) return this.ids[0];
      return this.ids;
    },
    unwrapFeedback(response) {
      return response?.data ?? response;
    },
    canApproveRow(row) {
      return row?.status === "APPROVING"
        && row?.recordUser
        && this.user?.userName
        && row.recordUser === this.user.userName;
    },
    openFeedbackDetail(recordId, title, optType) {
      if (!recordId) {
        this.$modal.msgWarning("未找到报工记录");
        return;
      }
      getFeedback(recordId)
        .then((response) => {
          this.form = this.unwrapFeedback(response);
          this.open = true;
          this.title = title;
          this.optType = optType;
        })
        .catch((cause) => {
          this.$modal.msgError(cause?.message || "报工详情加载失败");
        });
    },
    /** 查询生产报工记录列表 */
    getList() {
      this.loading = true;
      listFeedback(this.queryParams).then(response => {
        this.feedbackList = response.rows;
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
        recordId: null,
        feedbackCode: null,
        feedbackType: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        routeId: null,
        routeCode: null,
        processId: null,
        processCode: null,
        processName: null,
        workorderId: null,
        workorderCode: null,
        workorderName: null,
        taskId: null,
        taskCode: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityFeedback: 0,
        quantityQualified: 0,
        quantityUnquanlified: 0,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        userName: null,
        nickName: null,
        feedbackChannel: null,
        feedbackTime: this.formatDate(new Date(),"yyyy-MM-dd HH:mm:ss"),
        recordUser: null,
        recordNick: null,
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
      this.form.userId = this.user.userId;
      this.form.userName = this.user.userName;
      this.form.nickName = this.user.nickName;
      this.resetForm("form");
    },
    handleQuantityChanged(){
      this.form.quantityFeedback = this.form.quantityQualified + this.form.quantityUnquanlified;
    },
    handleScrapChanged(){
      this.form.quantityUnquanlified = this.form.quantityLaborScrap + this.form.quantityMaterialScrap + this.form.quantityOtherScrap;
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加生产报工记录";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      const recordId = this.resolveRecordId(row);
      this.openFeedbackDetail(recordId, "修改生产报工记录", "edit");
    },

    handleApprove(row) {
      const recordId = this.resolveRecordId(row);
      this.openFeedbackDetail(recordId, "生产报工审核", "edit");
    },

    handleView(row) {
      const recordId = this.resolveRecordId(row);
      this.openFeedbackDetail(recordId, "查看生产报工单信息", "view");
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateFeedback(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open=false;
              this.getList();

            });
          } else {
            addFeedback(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open=false;
              this.getList();
            });
          }
        }
      });
    },
    handleSubmit(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = "APPROVING";
          if (this.form.recordId != null) {
            updateFeedback(this.form).then(response => {
              this.$modal.msgSuccess("提交成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //审批通过执行
    approve(){
      const recordIds = this.form.recordId;
      this.$modal.confirm('确认执行报工？').then(function() {
        return execute(recordIds)//执行报工
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("执行成功");
        this.open = false;
      }).catch(() => {});
    },
    //审批不通过，拒绝
    handleReject(){
      this.form.status = "PREPARE";
      if (this.form.recordId != null) {
        updateFeedback(this.form).then(response => {
          this.$modal.msgSuccess("已驳回");
          this.open = false;
          this.getList();
        });
      }
    },
    handleSubmitRow(row) {
      const recordId = this.resolveRecordId(row);
      this.$modal.confirm(`确认提交报工单「${row?.feedbackCode || recordId}」审批？`).then(() => {
        return getFeedback(recordId).then((response) => {
          const payload = { ...this.unwrapFeedback(response), status: "APPROVING" };
          return updateFeedback(payload);
        });
      }).then(() => {
        this.$modal.msgSuccess("提交成功");
        this.getList();
      }).catch(() => {});
    },
    handleExecuteRow(row) {
      const recordId = this.resolveRecordId(row);
      this.$modal.confirm(`确认执行报工单「${row?.feedbackCode || recordId}」？`).then(() => execute(recordId))
        .then(() => {
          this.$modal.msgSuccess("执行成功");
          this.getList();
        }).catch(() => {});
    },
    handleRejectRow(row) {
      const recordId = this.resolveRecordId(row);
      this.$modal.confirm(`确认驳回报工单「${row?.feedbackCode || recordId}」？`).then(() => {
        return getFeedback(recordId).then((response) => {
          const payload = { ...this.unwrapFeedback(response), status: "PREPARE" };
          return updateFeedback(payload);
        });
      }).then(() => {
        this.$modal.msgSuccess("已驳回");
        this.getList();
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除生产报工记录编号为"' + recordIds + '"的数据项？').then(function() {
        return delFeedback(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('pro/feedback/export', {
        ...this.queryParams
      }, `feedback_${new Date().getTime()}.xlsx`)
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
        this.form.unitName = row.unitName;
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
    handleTaskSelect(){
      this.$refs.taskSelect.handleOpen(this.form.taskId)
    },
    onTaskSelected(row){
      if(row != undefined && row != null){
        this.form.taskId = row.taskId;
        this.form.taskCode = row.taskCode;
        this.form.taskName = row.taskName;
        this.form.workstationId = row.workstationId;
        this.form.workstationCode = row.workstationCode;
        this.form.workstationName = row.workstationName;
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
        this.form.workorderName = row.workorderName;
        this.form.itemId = row.itemId;
        this.form.itemCode = row.itemCode;
        this.form.itemName = row.itemName;
        this.form.specification = row.specification;
        this.form.unitOfMeasure = row.unitOfMeasure;
        this.form.unitName = row.unitName;
        this.form.processId = row.processId;
        this.form.processCode = row.processCode;
        this.form.processName = row.processName;
        this.form.routeId = row.routeId;
        this.form.routeCode = row.routeCode;
        this.form.isCheck = row.isCheck;        

        this.form.quantityFeedback = 0;
        this.form.quantityQualified =0;
        this.form.quantityUnquanlified = 0;
      }
    },
    //点击人员选择按钮
    handleUserSelect(){
        this.$refs.userSelect.showFlag = true;
    },
    //人员选择返回
    onUserSelected(row){
        this.form.nickName = row.nickName;
        this.form.userName = row.userName;
    },
    //点击人员选择按钮
    handleUser2Select(){
        this.$refs.user2Select.showFlag = true;
    },
    //人员选择返回
    onUser2Selected(row){
        this.form.recordUser = row.userName;
        this.form.recordNick = row.nickName;
    },
  }
};
</script>
