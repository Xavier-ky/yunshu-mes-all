<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="工作站编号" prop="workstationCode">
        <el-input v-model="queryParams.workstationCode" placeholder="请输入工作站编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工作站名称" prop="workstationName">
        <el-input v-model="queryParams.workstationName" placeholder="请输入工作站名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="发起人" prop="nickName">
        <el-input v-model="queryParams.nickName" placeholder="请输入发起人名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="生产工单编号" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="请输入生产工单编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="queryParams.processName" placeholder="请输入工序名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="处置人名称" prop="handlerNickName">
        <el-input v-model="queryParams.handlerNickName" placeholder="请输入处置人名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="处置状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择处置状态" clearable>
          <el-option v-for="dict in statusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="warning" icon="Bell" @click="openLaunch">发起安灯</el-button>
        <el-button type="success" icon="Setting" @click="handleConfig" v-hasPermi="['mes:pro:andonconfig:list']">安灯设置</el-button>
        <el-button type="info" icon="DataAnalysis" @click="openHub">安灯管理中心</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="andonrecordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工作站编号" align="center" prop="workstationCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="工作站名称" align="center" prop="workstationName" min-width="120" show-overflow-tooltip />
      <el-table-column label="工单编号" align="center" prop="workorderCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="工序" align="center" prop="processName" min-width="100" show-overflow-tooltip />
      <el-table-column label="发起人" align="center" prop="nickName" min-width="90" show-overflow-tooltip />
      <el-table-column label="发起时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="呼叫原因" align="center" prop="andonReason" min-width="140" show-overflow-tooltip>
        <template #default="scope">
          <el-button type="primary" link @click="handleView(scope.row)" v-hasPermi="['mes:pro:andonrecord:query']">{{ scope.row.andonReason }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="级别" align="center" prop="andonLevel" width="80" />
      <el-table-column label="处置时间" align="center" prop="handleTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.handleTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="处置人" align="center" prop="handlerNickName" min-width="90" show-overflow-tooltip />
      <el-table-column label="处置状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="col-actions" width="100">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button v-if="scope.row.status === 'ACTIVE'" type="primary" link icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:andonrecord:edit']">处置</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8"><el-form-item label="工作站编号" prop="workstationCode"><el-input v-model="form.workstationCode" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="工作站名称" prop="workstationName"><el-input v-model="form.workstationName" readonly /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="工序名称" prop="processName"><el-input v-model="form.processName" readonly /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="生产工单编号" prop="workorderCode"><el-input v-model="form.workorderCode" readonly /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="发起时间" prop="createTime">
              <el-date-picker clearable v-model="form.createTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择发起时间" style="width:100%" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="发起人" prop="nickName"><el-input v-model="form.nickName" readonly /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><el-form-item label="呼叫原因" prop="andonReason"><el-input v-model="form.andonReason" type="textarea" readonly /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="级别" prop="andonLevel"><el-input v-model="form.andonLevel" placeholder="请输入级别" :readonly="optType === 'view'" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="处置时间" prop="handleTime">
              <el-date-picker clearable v-model="form.handleTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择处置时间" style="width:100%" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="处置人" prop="handlerNickName">
              <el-input v-model="form.handlerNickName" placeholder="请选择处置人" :readonly="optType === 'view'">
                <template #append><el-button @click="handleUserSelect" icon="Search" :disabled="optType === 'view'" /></template>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected" />
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" placeholder="请输入内容" :readonly="optType === 'view'" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" v-if="optType !== 'view' && form.status === 'ACTIVE'" @click="saveForm('ACTIVE')">保 存</el-button>
          <el-button type="success" v-if="optType !== 'view' && form.status === 'ACTIVE'" @click="saveForm('HANDLED')">已处置</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
    <AndonConfig ref="andonConfigRef" />
    <AndonLaunch ref="launchRef" @submitted="onAndonSubmitted" />
    <AndonHub ref="hubRef" />
  </div>
</template>

<script>
import { listAndonrecord, getAndonrecord, updateAndonrecord } from "@/yunshu-ui/api/mes/pro/andonrecord";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";
import AndonConfig from "./config.vue";
import AndonLaunch from "./launch.vue";
import AndonHub from "./hub.vue";
import { parseTime } from "@/yunshu-ui/utils/yunshu-utils";

export default {
  name: "AndonRecord",
  components: { UserSingleSelect, AndonConfig, AndonLaunch, AndonHub },
  dicts: ["mes_andon_status"],
  data() {
    return {
      optType: null,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      andonrecordList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workstationCode: null,
        workstationName: null,
        nickName: null,
        workorderCode: null,
        processName: null,
        handlerNickName: null,
        status: null,
      },
      form: {},
      rules: {
        andonReason: [{ required: true, message: "呼叫原因不能为空", trigger: "blur" }],
      },
    };
  },
  computed: {
    statusOptions() {
      return (this.dict && this.dict.type && this.dict.type.mes_andon_status) || [];
    },
  },
  created() {
    this.getList();
  },
  methods: {
    parseTime,
    getList() {
      this.loading = true;
      listAndonrecord(this.queryParams).then((response) => {
        this.andonrecordList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      }).catch(() => { this.loading = false; });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        recordId: null,
        workstationCode: null,
        workstationName: null,
        processName: null,
        workorderCode: null,
        createTime: null,
        nickName: null,
        andonReason: null,
        andonLevel: null,
        handleTime: parseTime(new Date(), "{y}-{m}-{d} {h}:{i}:{s}"),
        handlerNickName: null,
        handlerUserId: null,
        handlerUserName: null,
        status: "ACTIVE",
        remark: null,
      };
      this.$refs.formRef?.resetFields?.();
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.$refs.queryForm?.resetFields?.();
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.recordId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleView(row) {
      const recordId = row.recordId || this.ids[0];
      getAndonrecord(recordId).then((response) => {
        this.form = response.data || {};
        this.open = true;
        this.optType = "view";
        this.title = "查看安灯呼叫记录";
      });
    },
    handleUpdate(row) {
      const recordId = row.recordId || this.ids[0];
      getAndonrecord(recordId).then((response) => {
        this.form = response.data || {};
        if (!this.form.handleTime && this.form.status === "ACTIVE") {
          this.form.handleTime = parseTime(new Date(), "{y}-{m}-{d} {h}:{i}:{s}");
        }
        this.open = true;
        this.optType = "edit";
        this.title = "修改安灯呼叫记录";
      });
    },
    handleConfig() {
      this.$refs.andonConfigRef.handleOpen();
    },
    openLaunch() {
      this.$refs.launchRef.open();
    },
    openHub(tab) {
      this.$refs.hubRef.open(tab || "analytics");
    },
    onAndonSubmitted() {
      this.getList();
    },
    saveForm(status) {
      this.form.status = status;
      this.$refs.formRef.validate((valid) => {
        if (!valid) return;
        updateAndonrecord(this.form).then(() => {
          this.$modal.msgSuccess("保存成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleUserSelect() {
      if (this.optType === "view") return;
      this.$refs.userSelect.showFlag = true;
    },
    onUserSelected(row) {
      this.form.handlerUserId = row.userId;
      this.form.handlerUserName = row.userName;
      this.form.handlerNickName = row.nickName;
    },
  },
};
</script>
