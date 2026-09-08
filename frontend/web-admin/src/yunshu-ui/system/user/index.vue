<template>
  <div :class="embedMode ? 'inbound-doc-panel access-doc-panel' : 'app-container'">
    <template v-if="embedMode">
      <section v-show="showSearch" class="inbound-filter-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
          <el-form-item prop="userName">
            <el-input v-model="queryParams.userName" placeholder="用户名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" placeholder="手机号码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-date-picker
              v-model="dateRange"
              value-format="yyyy-MM-dd"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <div class="inbound-toolbar">
        <div class="inbound-toolbar-left">
          <span class="inbound-page-title">用户管理</span>
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button>
          <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:user:edit']">修改</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:user:remove']">删除</el-button>
          <el-button type="info" plain icon="el-icon-upload2" size="default" @click="handleImport" v-hasPermi="['system:user:import']">导入</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button>
        </div>
        <div class="inbound-toolbar-meta">
          <span>共 {{ total }} 条</span>
          <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" :columns="columns"></right-toolbar>
        </div>
      </div>

      <div class="inbound-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <el-table class="yunshu-data-table inbound-table" stripe border height="100%" v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" align="center" />
          <el-table-column label="序号" type="index" width="60" align="center" :index="serialIndex" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" min-width="90" />
          <el-table-column label="用户名称" align="center" key="userName" prop="userName" v-if="columns[1].visible" min-width="120" show-overflow-tooltip>
            <template #default="scope">
              <button type="button" class="doc-link-btn" @click.stop.prevent="handleUpdate(scope.row)">{{ scope.row.userName }}</button>
            </template>
          </el-table-column>
          <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns[2].visible" min-width="120" show-overflow-tooltip />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible" min-width="120" show-overflow-tooltip />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible" min-width="120" />
          <el-table-column label="状态" align="center" key="status" v-if="columns[5].visible" width="80">
            <template #default="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[6].visible" min-width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="200" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <template v-if="scope.row.userId !== 1">
                <div class="yunshu-row-actions">
                  <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']">修改</el-button>
                  <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']">删除</el-button>
                  <el-dropdown size="small" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:user:resetPwd', 'system:user:edit']">
                    <el-button size="small" link>更多</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="handleResetPwd" icon="el-icon-key" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                        <el-dropdown-item command="handleAuthRole" icon="el-icon-circle-check" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination
        v-show="total>0"
        :total="total"
        :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
        :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
        @pagination="getList"
      />
    </template>

    <template v-else>
      <el-row :gutter="20">
        <el-col :span="4" :xs="24">
          <div class="head-container">
            <el-input v-model="deptName" placeholder="请输入部门名称" clearable size="small" prefix-icon="el-icon-search" style="margin-bottom: 20px" />
          </div>
          <div class="head-container">
            <el-tree :data="deptOptions" :props="defaultProps" :expand-on-click-node="false" :filter-node-method="filterNode" ref="tree" default-expand-all @node-click="handleNodeClick" />
          </div>
        </el-col>
        <el-col :span="20" :xs="24">
          <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="用户名称" prop="userName">
              <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
                <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="创建时间">
              <el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:user:add']">新增</el-button></el-col>
            <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:user:edit']">修改</el-button></el-col>
            <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:user:remove']">删除</el-button></el-col>
            <el-col :span="1.5"><el-button type="info" plain icon="el-icon-upload2" size="mini" @click="handleImport" v-hasPermi="['system:user:import']">导入</el-button></el-col>
            <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:user:export']">导出</el-button></el-col>
            <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" :columns="columns"></right-toolbar>
          </el-row>

          <el-table class="yunshu-data-table" stripe border v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column label="序号" type="index" width="60" align="center" :index="serialIndex" />
            <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" />
            <el-table-column label="用户名称" align="center" key="userName" prop="userName" v-if="columns[1].visible" :show-overflow-tooltip="true" />
            <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns[2].visible" :show-overflow-tooltip="true" />
            <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible" :show-overflow-tooltip="true" />
            <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible" width="120" />
            <el-table-column label="状态" align="center" key="status" v-if="columns[5].visible">
              <template #default="scope">
                <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[6].visible" width="160">
              <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
              <template #default="scope">
                <template v-if="scope.row.userId !== 1">
                  <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']">修改</el-button>
                  <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']">删除</el-button>
                  <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:user:resetPwd', 'system:user:edit']">
                    <span class="el-dropdown-link"><i class="el-icon-d-arrow-right el-icon--right"></i>更多</span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="handleResetPwd" icon="el-icon-key" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                        <el-dropdown-item command="handleAuthRole" icon="el-icon-circle-check" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <pagination v-show="total>0" :total="total" :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event" :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event" @pagination="getList" />
        </el-col>
      </el-row>
    </template>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog
      :title="title"
      v-model="open"
      :class="isAddMode ? 'access-form-dialog access-user-form-dialog' : 'access-form-dialog access-form-dialog--wide'"
      :width="isAddMode ? '560px' : '760px'"
      append-to-body
      align-center
      destroy-on-close
    >
      <!-- 新增：精简表单（对齐 sys_user 必填 + 默认密码） -->
      <template v-if="isAddMode">
        <div class="access-user-quick-form">
          <p class="access-user-quick-form__lead">只需填写账号、姓名、部门与角色；其余信息可创建后在「修改」中补充。</p>
          <el-form ref="form" :model="form" :rules="addRules" label-width="96px" class="access-form-dialog__form access-user-quick-form__body">
            <div class="access-user-quick-form__row access-user-quick-form__row--2">
              <el-form-item label="登录账号" prop="userName">
                <el-input
                  v-model="form.userName"
                  placeholder="如 worker02、zhangsan"
                  maxlength="30"
                  clearable
                  @blur="syncNickNameFromUserName"
                />
              </el-form-item>
              <el-form-item label="姓名" prop="nickName">
                <el-input v-model="form.nickName" placeholder="真实姓名或显示名" maxlength="30" clearable />
              </el-form-item>
            </div>
            <el-form-item label="归属部门" prop="deptId" class="access-user-quick-form__dept">
              <treeselect
                v-model="form.deptId"
                :options="deptOptions"
                :show-count="true"
                :default-expand-level="2"
                placeholder="请选择部门"
                class="access-user-quick-form__treeselect"
              />
            </el-form-item>
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="form.roleId" placeholder="请选择角色" class="access-user-quick-form__select">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.roleId"
                  :label="item.roleName"
                  :value="item.roleId"
                  :disabled="item.status == 1"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="access-user-quick-form__meta">
            <span>初始密码</span>
            <code>{{ initPassword || "123456" }}</code>
            <span class="access-user-quick-form__meta-note">创建后可在「更多 → 重置密码」修改</span>
          </div>
        </div>
      </template>

      <!-- 修改：完整表单 -->
      <el-form v-else ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <treeselect
                v-model="form.deptId"
                :options="deptOptions"
                :show-count="true"
                :default-expand-level="2"
                placeholder="请选择归属部门"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择性别">
                <el-option v-for="dict in dict.type.sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-select v-model="form.postIds" multiple collapse-tags collapse-tags-tooltip placeholder="请选择岗位">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" :disabled="item.status == 1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="form.roleId" clearable placeholder="请选择角色">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" :disabled="item.status == 1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" />
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

    <el-dialog
      :title="upload.title"
      v-model="upload.open"
      class="access-form-dialog access-user-import-dialog"
      width="560px"
      append-to-body
      align-center
      destroy-on-close
    >
      <div class="access-user-import-dialog__body">
        <div class="access-user-import-dialog__row">
          <span class="access-user-import-dialog__step">1</span>
          <p class="access-user-import-dialog__line">
            <strong>下载模板</strong>
            <span class="access-user-import-dialog__muted">含「用户数据」「参照数据」Sheet</span>
          </p>
          <el-button
            type="primary"
            plain
            icon="el-icon-download"
            class="access-user-import-dialog__action"
            @click="importTemplate"
          >
            下载模板
          </el-button>
        </div>

        <div class="access-user-import-dialog__panel">
          <div class="access-user-import-dialog__row access-user-import-dialog__row--flat">
            <span class="access-user-import-dialog__step">2</span>
            <p class="access-user-import-dialog__line">
              <strong>上传文件</strong>
              <span class="access-user-import-dialog__muted">必填：登录账号、姓名、部门、角色 · 仅 xls / xlsx</span>
            </p>
          </div>
          <el-upload
            ref="upload"
            class="access-user-import-dialog__upload"
            :limit="1"
            accept=".xlsx, .xls"
            :headers="upload.headers"
            :action="upload.url + '?updateSupport=' + upload.updateSupport"
            :disabled="upload.isUploading"
            :on-progress="handleFileUploadProgress"
            :on-success="handleFileSuccess"
            :on-error="handleFileError"
            :auto-upload="false"
            drag
          >
            <div class="access-user-import-dialog__drop">
              <i class="el-icon-upload"></i>
              <span>拖拽文件到此处，或<em>点击选择</em></span>
            </div>
          </el-upload>
        </div>

        <div class="access-user-import-dialog__opts">
          <el-checkbox v-model="upload.updateSupport" :true-value="1" :false-value="0">
            更新已存在用户
          </el-checkbox>
          <span class="access-user-import-dialog__muted">新建留空用默认密码 · 更新不改密码</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="upload.isUploading" @click="submitFileForm">开始导入</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listUser, getUser, delUser, addUser, updateUser, resetUserPwd, changeUserStatus } from "@/yunshu-ui/api/system/user";
import { getToken } from "@/yunshu-ui/utils/auth";
import { treeselect } from "@/yunshu-ui/api/system/dept";
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";

export default {
  name: "User",
  dicts: ['sys_normal_disable', 'sys_user_sex'],
  components: { Treeselect },
  props: {
    embedMode: { type: Boolean, default: false },
    scopeDeptId: { type: [Number, String], default: null },
    scopeStatus: { type: String, default: null },
  },
  emits: ["changed"],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      userList: [],
      title: "",
      deptOptions: undefined,
      open: false,
      deptName: undefined,
      initPassword: undefined,
      dateRange: [],
      postOptions: [],
      roleOptions: [],
      form: {},
      defaultProps: { children: "children", label: "label" },
      upload: {
        open: false,
        title: "",
        isUploading: false,
        updateSupport: 0,
        headers: { Authorization: "Bearer " + getToken() },
        url: (import.meta.env.VITE_APP_BASE_API || "/api") + "/system/user/importData"
      },
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      },
      columns: [
        { key: 0, label: `用户编号`, visible: true },
        { key: 1, label: `用户名称`, visible: true },
        { key: 2, label: `用户昵称`, visible: true },
        { key: 3, label: `部门`, visible: true },
        { key: 4, label: `手机号码`, visible: true },
        { key: 5, label: `状态`, visible: true },
        { key: 6, label: `创建时间`, visible: true }
      ],
      rules: {
        userName: [
          { required: true, message: "用户名称不能为空", trigger: "blur" },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ],
        deptId: [{ required: true, message: "所属部门不能为空", trigger: "blur" }],
        nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
        password: [
          { required: true, message: "用户密码不能为空", trigger: "blur" },
          { min: 5, max: 20, message: '用户密码长度必须介于 5 和 20 之间', trigger: 'blur' }
        ],
        email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
        phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }],
        remark: [{ max: 250, message: '长度必须小于250个字符', trigger: 'blur' }]
      },
      addRules: {
        userName: [
          { required: true, message: "请输入登录账号", trigger: "blur" },
          { min: 2, max: 20, message: "账号长度 2–20 个字符", trigger: "blur" },
          { pattern: /^[a-zA-Z0-9_]+$/, message: "账号仅支持字母、数字、下划线", trigger: "blur" },
        ],
        nickName: [
          { required: true, message: "请输入姓名", trigger: "blur" },
          { max: 30, message: "姓名不超过 30 个字符", trigger: "blur" },
        ],
        deptId: [{ required: true, message: "请选择归属部门", trigger: "change" }],
        roleId: [{ required: true, message: "请选择角色", trigger: "change" }],
      },
    };
  },
  computed: {
    isAddMode() {
      return this.open && this.form.userId == undefined;
    },
  },
  watch: {
    deptName(val) {
      if (!this.embedMode) this.$refs.tree?.filter(val);
    },
    scopeDeptId(val) {
      if (!this.embedMode) return;
      this.queryParams.deptId = val || undefined;
      this.handleQuery();
    },
    scopeStatus(val) {
      if (!this.embedMode) return;
      this.queryParams.status = val ?? undefined;
      this.handleQuery();
    },
  },
  created() {
    if (this.embedMode) {
      this.queryParams.deptId = this.scopeDeptId || undefined;
      this.queryParams.status = this.scopeStatus ?? undefined;
    }
    this.getList();
    this.getTreeselect();
    this.getConfigKey("sys.user.initPassword").then(response => {
      this.initPassword = response.msg;
    }).catch(() => {
      this.initPassword = "123456";
    });
  },
  methods: {
    serialIndex(index) {
      return (this.queryParams.pageNum - 1) * this.queryParams.pageSize + index + 1;
    },
    getList() {
      this.loading = true;
      listUser(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.userList = response.rows;
        this.total = response.total;
      }).catch(() => {
        this.userList = [];
        this.total = 0;
      }).finally(() => {
        this.loading = false;
      });
    },
    getTreeselect() {
      treeselect().then(response => {
        this.deptOptions = response.data;
      });
    },
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    handleNodeClick(data) {
      this.queryParams.deptId = data.id;
      this.handleQuery();
    },
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗？').then(function() {
        return changeUserStatus(row.userId, row.status);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        userId: undefined, deptId: undefined, userName: undefined, nickName: undefined,
        password: undefined, phonenumber: undefined, email: undefined, sex: undefined,
        status: "0", remark: undefined, postIds: [], roleId: undefined
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      if (this.embedMode) {
        this.queryParams.deptId = this.scopeDeptId || undefined;
        this.queryParams.status = this.scopeStatus ?? undefined;
      }
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.userId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleCommand(command, row) {
      if (command === "handleResetPwd") this.handleResetPwd(row);
      else if (command === "handleAuthRole") this.handleAuthRole(row);
    },
    handleAdd() {
      this.reset();
      this.getTreeselect();
      getUser()
        .then((response) => {
          this.roleOptions = response.roles || [];
          this.open = true;
          this.title = "新增用户";
          this.form.password = this.initPassword || "123456";
          this.form.status = "0";
          this.form.sex = "0";
          this.form.postIds = [];
          if (this.embedMode && this.scopeDeptId) this.form.deptId = this.scopeDeptId;
        })
        .catch(() => {
          this.$modal.msgError("加载新增表单失败，请稍后重试");
        });
    },
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      const userId = row.userId || this.ids;
      getUser(userId).then(response => {
        this.form = response.data;
        this.postOptions = response.posts;
        this.roleOptions = response.roles;
        this.form.postIds = response.postIds;
        this.form.roleId = Array.isArray(response.roleIds) && response.roleIds.length
          ? response.roleIds[0]
          : undefined;
        this.open = true;
        this.title = "修改用户";
        this.form.password = "";
      });
    },
    handleResetPwd(row) {
      this.$prompt('请输入"' + row.userName + '"的新密码', "提示", {
        confirmButtonText: "确定", cancelButtonText: "取消", closeOnClickModal: false,
        inputPattern: /^.{5,20}$/, inputErrorMessage: "用户密码长度必须介于 5 和 20 之间"
      }).then(({ value }) => {
        resetUserPwd(row.userId, value).then(() => {
          this.$modal.msgSuccess("修改成功，新密码是：" + value);
        });
      }).catch(() => {});
    },
    handleAuthRole(row) {
      this.$router.push("/app/system/user-auth/role/" + row.userId);
    },
    syncNickNameFromUserName() {
      if (!this.isAddMode) return;
      const name = (this.form.nickName || "").trim();
      const user = (this.form.userName || "").trim();
      if (!name && user) this.form.nickName = user;
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        if (this.isAddMode) {
          if (!this.form.password) this.form.password = this.initPassword || "123456";
          if (!this.form.sex) this.form.sex = "0";
          if (!this.form.status) this.form.status = "0";
        }
        const payload = {
          ...this.form,
          roleIds: this.form.roleId != null && this.form.roleId !== "" ? [this.form.roleId] : [],
        };
        const req = this.form.userId != undefined ? updateUser(payload) : addUser(payload);
        req.then(() => {
          this.$modal.msgSuccess(this.form.userId != undefined ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
        });
      });
    },
    handleDelete(row) {
      const userIds = row.userId || this.ids;
      this.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function() {
        return delUser(userIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() {
      const params = { ...this.queryParams };
      delete params.pageNum;
      delete params.pageSize;
      if (this.dateRange?.length === 2) {
        params.beginTime = this.dateRange[0];
        params.endTime = this.dateRange[1];
      }
      this.download('system/user/export', params, `user_${new Date().getTime()}.xlsx`);
    },
    handleImport() {
      this.upload.title = "用户导入";
      this.upload.open = true;
    },
    importTemplate() {
      this.download('system/user/importTemplate', {}, `user_template_${new Date().getTime()}.xlsx`);
    },
    handleFileUploadProgress() { this.upload.isUploading = true; },
    handleFileSuccess(response) {
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      if (!response || response.code !== 200) {
        this.$modal.msgError(response?.msg || "导入失败");
        return;
      }
      this.upload.open = false;
      this.$alert(
        "<div style='overflow:auto;overflow-x:hidden;max-height:70vh;padding:10px 20px 0;'>" + (response.msg || "导入成功") + "</div>",
        "导入结果",
        { dangerouslyUseHTMLString: true }
      );
      this.getList();
      if (this.embedMode) this.$emit("changed");
    },
    handleFileError() {
      this.upload.isUploading = false;
      this.$modal.msgError("上传失败，请检查网络或登录状态");
    },
    submitFileForm() { this.$refs.upload.submit(); }
  }
};
</script>
