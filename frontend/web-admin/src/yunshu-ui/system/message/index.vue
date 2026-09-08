<template>
  <div class="app-container inbound-doc-panel sys-doc-panel sys-comms-doc-panel">
    <section class="inbound-filter-panel sys-comms-filter-panel" :class="{ 'sys-comms-filter-panel--compact': !showSearch }">
      <div class="sys-comms-filter__bar">
        <div class="sys-comms-filter__bar-main">
          <el-form v-show="showSearch" ref="queryForm" :model="queryParams" size="default" :inline="true" class="inbound-filter-form sys-comms-filter__fields" label-width="0" @submit.prevent>
            <el-form-item prop="messageType">
              <el-select v-model="queryParams.messageType" placeholder="消息类型" clearable>
                <el-option v-for="dict in dict.type.sys_message_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item prop="messageLevel">
              <el-select v-model="queryParams.messageLevel" placeholder="消息级别" clearable>
                <el-option v-for="dict in dict.type.sys_message_level" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item prop="senderNick">
              <el-input v-model="queryParams.senderNick" placeholder="发送人" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="recipientNick">
              <el-input v-model="queryParams.recipientNick" placeholder="接收人" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item prop="status">
              <el-select v-model="queryParams.status" placeholder="状态" clearable>
                <el-option v-for="dict in dict.type.sys_message_status" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item class="sys-comms-filter__query-actions">
              <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="sys-comms-filter__bar-meta">
            <span class="sys-comms-filter__count">共 {{ total }} 条</span>
            <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
          </div>
        </div>
        <div class="sys-comms-filter__bar-crud">
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:message:add']">测试</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:message:remove']">删除</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:message:export']">导出</el-button>
          <el-button type="success" plain icon="el-icon-bell" size="default" @click="handleAllRead" v-hasPermi="['system:message:READ']">全部已读</el-button>
        </div>
      </div>
    </section>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table sys-table inbound-table"
        stripe
        border
        height="100%"
        :data="messageList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" />
        <el-table-column label="消息类型" align="center" prop="messageType" min-width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_message_type" :value="scope.row.messageType" />
          </template>
        </el-table-column>
        <el-table-column label="消息级别" align="center" prop="messageLevel" min-width="100">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_message_level" :value="scope.row.messageLevel" />
          </template>
        </el-table-column>
        <el-table-column label="标题" align="center" prop="messageTitle" min-width="140" show-overflow-tooltip />
        <el-table-column label="内容" align="center" prop="messageContent" min-width="160" show-overflow-tooltip />
        <el-table-column label="发送人" align="center" prop="senderNick" min-width="100" show-overflow-tooltip />
        <el-table-column label="接收人" align="center" prop="recipientNick" min-width="100" show-overflow-tooltip />
        <el-table-column label="处理时间" align="center" prop="processTime" min-width="120">
          <template #default="scope">
            <span>{{ parseTime(scope.row.processTime, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="回调地址" align="center" prop="callBack" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" align="center" prop="status" min-width="90">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_message_status" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" min-width="140" class-name="col-actions">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:message:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:message:remove']">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" class="access-form-dialog" width="960px" append-to-body align-center destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="access-form-dialog__form">
        <el-row>
          <el-col :span="8">
            <el-form-item label="消息类型" prop="messageType">
              <el-select v-model="form.messageType" placeholder="请选择消息类型">
                <el-option v-for="dict in dict.type.sys_message_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="消息级别" prop="messageLevel">
              <el-select v-model="form.messageLevel" placeholder="请选择消息级别">
                <el-option v-for="dict in dict.type.sys_message_level" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="接收人" prop="recipientNick">
              <el-input v-model="form.recipientNick" readonly placeholder="请选择接收人">
                <template #append>
                  <el-button @click="handleUserSelect" icon="el-icon-search" />
                </template>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected" />
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="标题" prop="messageTitle">
              <el-input v-model="form.messageTitle" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="内容">
              <el-input type="textarea" :rows="6" v-model="form.messageContent" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="回调地址" prop="callBack">
              <el-input v-model="form.callBack" placeholder="请输入回调地址" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listMessage, getMessage, delMessage, addMessage, updateMessage, allRead } from "@/yunshu-ui/api/system/message";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";

export default {
  name: "Message",
  components: { UserSingleSelect },
  dicts: ['sys_message_type', 'sys_message_status', 'sys_message_level'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      messageList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        messageType: null,
        messageLevel: null,
        messageTitle: null,
        messageContent: null,
        senderId: null,
        senderName: null,
        senderNick: null,
        recipientId: null,
        recipientName: null,
        recipientNick: null,
        processTime: null,
        callBack: null,
        status: null,
        deletedFlag: null,
      },
      form: {},
      rules: {
        messageType: [
          { required: true, message: "消息类型不能为空", trigger: "change" }
        ],
        messageLevel: [
          { required: true, message: "消息级别不能为空", trigger: "blur" }
        ],
        messageTitle: [
          { required: true, message: "消息标题不能为空", trigger: "blur" }
        ],
        messageContent: [
          { required: true, message: "消息内容不能为空", trigger: "blur" }
        ],
        recipientName: [
          { required: true, message: "接收人不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    handleAllRead() {
      allRead().then(res => {
        if (res.code == 200) {
          this.getList();
          this.$modal.msgSuccess("操作成功");
        }
      });
    },
    getList() {
      this.loading = true;
      listMessage(this.queryParams).then(response => {
        this.messageList = response.rows;
        this.total = response.total;
      }).finally(() => {
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        messageId: null,
        messageType: null,
        messageLevel: null,
        messageTitle: null,
        messageContent: null,
        senderId: null,
        senderName: null,
        senderNick: null,
        recipientId: null,
        recipientName: null,
        recipientNick: null,
        processTime: null,
        callBack: null,
        status: "0",
        deletedFlag: null,
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
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.messageId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加消息";
    },
    handleUpdate(row) {
      this.reset();
      const messageId = row.messageId || this.ids;
      getMessage(messageId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改消息";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.messageId != null) {
            updateMessage(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addMessage(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const messageIds = row.messageId || this.ids;
      this.$modal.confirm('是否确认删除消息编号为"' + messageIds + '"的数据项？').then(function() {
        return delMessage(messageIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      this.download('system/message/export', {
        ...this.queryParams
      }, `message_${new Date().getTime()}.xlsx`);
    },
    handleUserSelect() {
      this.$refs.userSelect.showFlag = true;
    },
    onUserSelected(row) {
      if (row != null) {
        this.form.recipientId = row.userId;
        this.form.recipientName = row.userName;
        this.form.recipientNick = row.nickName;
      }
    }
  }
};
</script>
