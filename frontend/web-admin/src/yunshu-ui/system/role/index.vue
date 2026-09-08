<template>
  <div :class="embedMode ? 'inbound-doc-panel access-doc-panel' : 'app-container'">
    <template v-if="embedMode">
      <section v-show="showSearch" class="inbound-filter-panel">
        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
          <el-form-item prop="roleName">
            <el-input v-model="queryParams.roleName" placeholder="角色名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item prop="roleKey">
            <el-input v-model="queryParams.roleKey" placeholder="权限字符" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-date-picker v-model="dateRange" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <div class="inbound-toolbar">
        <div class="inbound-toolbar-left">
          <span class="inbound-page-title">角色管理</span>
          <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['system:role:add']">新增</el-button>
          <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['system:role:edit']">修改</el-button>
          <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:role:remove']">删除</el-button>
          <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['system:role:export']">导出</el-button>
        </div>
        <div class="inbound-toolbar-meta">
          <span>共 {{ displayTotal }} 条</span>
          <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
        </div>
      </div>

      <div class="inbound-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
        <el-table class="yunshu-data-table inbound-table" stripe border height="100%" v-loading="loading" :data="displayRoleList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" align="center" />
          <el-table-column label="角色编号" prop="roleId" min-width="100" />
          <el-table-column label="角色名称" prop="roleName" min-width="140" show-overflow-tooltip>
            <template #default="scope">
              <button type="button" class="doc-link-btn" @click.stop.prevent="handleUpdate(scope.row)">{{ scope.row.roleName }}</button>
            </template>
          </el-table-column>
          <el-table-column label="权限字符" prop="roleKey" min-width="140" show-overflow-tooltip />
          <el-table-column label="显示顺序" prop="roleSort" min-width="100" />
          <el-table-column label="状态" align="center" width="80">
            <template #default="scope">
              <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" min-width="160">
            <template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" align="center" min-width="200" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <template v-if="scope.row.roleId !== 1">
                <div class="yunshu-row-actions">
                  <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:role:edit']">修改</el-button>
                  <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:role:remove']">删除</el-button>
                  <el-dropdown size="small" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:role:edit']">
                    <el-button size="small" link>更多</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="handleDataScope" icon="el-icon-circle-check">数据权限</el-dropdown-item>
                        <el-dropdown-item command="handleAuthUser" icon="el-icon-user">分配用户</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <pagination v-show="displayTotal>0 && !scopeRoleId" :total="total" :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event" :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event" @pagination="getList" />
    </template>

    <template v-else>
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable style="width: 240px" @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="权限字符" prop="roleKey"><el-input v-model="queryParams.roleKey" placeholder="请输入权限字符" clearable style="width: 240px" @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="角色状态" clearable style="width: 240px">
            <el-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间"><el-date-picker v-model="dateRange" style="width: 240px" value-format="yyyy-MM-dd" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:role:add']">新增</el-button></el-col>
        <el-col :span="1.5"><el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:role:edit']">修改</el-button></el-col>
        <el-col :span="1.5"><el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:role:remove']">删除</el-button></el-col>
        <el-col :span="1.5"><el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['system:role:export']">导出</el-button></el-col>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </el-row>
      <el-table class="yunshu-data-table" stripe border v-loading="loading" :data="roleList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="角色编号" prop="roleId" width="120" />
        <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
        <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="150" />
        <el-table-column label="显示顺序" prop="roleSort" width="100" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope"><el-switch v-model="scope.row.status" active-value="0" inactive-value="1" @change="handleStatusChange(scope.row)" /></template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180"><template #default="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template></el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <template v-if="scope.row.roleId !== 1">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:role:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:role:remove']">删除</el-button>
              <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)" v-hasPermi="['system:role:edit']">
                <span class="el-dropdown-link"><i class="el-icon-d-arrow-right el-icon--right"></i>更多</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="handleDataScope" icon="el-icon-circle-check">数据权限</el-dropdown-item>
                    <el-dropdown-item command="handleAuthUser" icon="el-icon-user">分配用户</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total>0" :total="total" :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event" :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event" @pagination="getList" />
    </template>

    <el-dialog
      :title="title"
      v-model="open"
      :class="isAddMode ? 'access-form-dialog access-role-form-dialog' : 'access-form-dialog access-form-dialog--wide'"
      :width="isAddMode ? '520px' : '720px'"
      append-to-body
      align-center
      destroy-on-close
    >
      <template v-if="isAddMode">
        <div class="access-role-quick-form">
          <p class="access-role-quick-form__intro">从模板选择权限字符，或输入自定义编码；选定后自动建议角色名称。</p>

          <div class="access-role-quick-form__card">
            <el-form
              ref="form"
              :model="form"
              :rules="addRules"
              label-position="top"
              class="access-role-quick-form__body"
            >
              <el-form-item label="权限字符" prop="roleKey">
                <el-select
                  v-model="form.roleKey"
                  filterable
                  allow-create
                  clearable
                  default-first-option
                  placeholder="请选择模板，或输入自定义编码"
                  class="access-role-quick-form__select"
                  popper-class="access-role-key-popper"
                  @change="onRoleKeyChange"
                >
                  <el-option-group v-if="availablePresets.length" label="可选用模板">
                    <el-option
                      v-for="preset in availablePresets"
                      :key="preset.key"
                      :label="preset.key"
                      :value="preset.key"
                    >
                      <div class="access-role-key-opt">
                        <span class="access-role-key-opt__label">{{ preset.label }}</span>
                        <code class="access-role-key-opt__code">{{ preset.key }}</code>
                      </div>
                    </el-option>
                  </el-option-group>
                  <el-option-group v-if="occupiedRoles.length" label="系统已占用">
                    <el-option
                      v-for="role in occupiedRoles"
                      :key="'occ-' + role.roleId"
                      :label="role.roleKey || role.roleCode"
                      :value="role.roleKey || role.roleCode"
                      disabled
                    >
                      <div class="access-role-key-opt is-disabled">
                        <span class="access-role-key-opt__label">{{ role.roleName }}</span>
                        <code class="access-role-key-opt__code">{{ role.roleKey || role.roleCode }}</code>
                      </div>
                    </el-option>
                  </el-option-group>
                </el-select>
              </el-form-item>

              <el-form-item label="角色名称" prop="roleName">
                <el-input
                  v-model="form.roleName"
                  placeholder="如 质检专员"
                  maxlength="100"
                  clearable
                />
              </el-form-item>

              <el-form-item label="显示顺序" prop="roleSort" class="access-role-quick-form__sort">
                <el-input-number
                  v-model="form.roleSort"
                  controls-position="right"
                  :min="0"
                  :max="9999"
                  class="access-role-quick-form__number"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="access-role-quick-form__foot">
            <span class="access-role-quick-form__foot-badge">默认启用</span>
            <span class="access-role-quick-form__foot-text">菜单与数据权限可在创建后通过「修改」或「更多 → 数据权限」配置</span>
          </div>
        </div>
      </template>

      <el-form v-else ref="form" :model="form" :rules="rules" label-width="96px" class="access-form-dialog__form">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" placeholder="请输入角色名称" /></el-form-item>
        <el-form-item prop="roleKey">
          <template #label>
            <span class="access-form-dialog__label-tip">
              <el-tooltip content="控制器中定义的权限字符" placement="top">
                <i class="el-icon-question"></i>
              </el-tooltip>
              权限字符
            </span>
          </template>
          <el-input v-model="form.roleKey" placeholder="请输入权限字符" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色顺序" prop="roleSort">
              <el-input-number v-model="form.roleSort" controls-position="right" :min="0" class="access-form-dialog__number" />
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
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="业务页面菜单由角色编码（role_key）决定；此处勾选仅控制系统管理类操作按钮权限。"
          class="access-role-perm-hint"
        />
        <el-form-item label="菜单权限">
          <div class="access-form-dialog__tree-tools">
            <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">展开/折叠</el-checkbox>
            <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">全选/全不选</el-checkbox>
            <el-checkbox v-model="form.menuCheckStrictly" @change="handleCheckedTreeConnect($event, 'menu')">父子联动</el-checkbox>
          </div>
          <el-tree class="tree-border access-form-dialog__tree" :data="menuOptions" show-checkbox ref="menu" node-key="id" :check-strictly="!form.menuCheckStrictly" empty-text="加载中，请稍候" :props="defaultProps" />
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入内容" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="title" v-model="openDataScope" class="access-form-dialog access-form-dialog--medium" width="640px" append-to-body align-center destroy-on-close>
      <el-form :model="form" label-width="96px" class="access-form-dialog__form">
        <el-form-item label="角色名称"><el-input v-model="form.roleName" :disabled="true" /></el-form-item>
        <el-form-item label="权限字符"><el-input v-model="form.roleKey" :disabled="true" /></el-form-item>
        <el-form-item label="权限范围">
          <el-select v-model="form.dataScope" placeholder="请选择权限范围" @change="dataScopeSelectChange">
            <el-option v-for="item in dataScopeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据权限" v-show="form.dataScope == 2">
          <div class="access-form-dialog__tree-tools">
            <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpand($event, 'dept')">展开/折叠</el-checkbox>
            <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAll($event, 'dept')">全选/全不选</el-checkbox>
            <el-checkbox v-model="form.deptCheckStrictly" @change="handleCheckedTreeConnect($event, 'dept')">父子联动</el-checkbox>
          </div>
          <el-tree class="tree-border access-form-dialog__tree" :data="deptOptions" show-checkbox default-expand-all ref="dept" node-key="id" :check-strictly="!form.deptCheckStrictly" empty-text="加载中，请稍候" :props="defaultProps" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitDataScope">确 定</el-button>
          <el-button @click="cancelDataScope">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listRole, getRole, delRole, addRole, updateRole, dataScope, changeRoleStatus } from "@/yunshu-ui/api/system/role";
import { treeselect as menuTreeselect, roleMenuTreeselect } from "@/yunshu-ui/api/system/menu";
import { treeselect as deptTreeselect, roleDeptTreeselect } from "@/yunshu-ui/api/system/dept";

/** 可选用权限字符模板（未占用时可新建） */
const ROLE_KEY_PRESETS = [
  { key: "PMC_PLANNER", label: "计划员", roleName: "计划员" },
  { key: "QC_LEADER", label: "质检组长", roleName: "质检组长" },
  { key: "QC_OPERATOR", label: "质检操作员", roleName: "质检操作员" },
  { key: "SHIFT_LEADER", label: "班组长", roleName: "班组长" },
  { key: "STORE_KEEPER", label: "库管员", roleName: "库管员" },
  { key: "PROCESS_ENGINEER", label: "工艺工程师", roleName: "工艺工程师" },
  { key: "PROD_MANAGER", label: "生产经理", roleName: "生产经理" },
  { key: "DATA_ANALYST", label: "数据分析员", roleName: "数据分析员" },
  { key: "SYSTEM_OPERATOR", label: "系统操作员", roleName: "系统操作员" },
  { key: "SAFETY_OFFICER", label: "安全员", roleName: "安全员" },
];

export default {
  name: "Role",
  dicts: ['sys_normal_disable'],
  props: {
    embedMode: { type: Boolean, default: false },
    scopeStatus: { type: String, default: null },
    scopeRoleId: { type: [Number, String], default: null },
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
      roleList: [],
      title: "",
      open: false,
      openDataScope: false,
      menuExpand: false,
      menuNodeAll: false,
      deptExpand: true,
      deptNodeAll: false,
      dateRange: [],
      dataScopeOptions: [
        { value: "1", label: "全部数据权限" },
        { value: "2", label: "自定数据权限" },
        { value: "3", label: "本部门数据权限" },
        { value: "4", label: "本部门及以下数据权限" },
        { value: "5", label: "仅本人数据权限" }
      ],
      menuOptions: [],
      deptOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, roleName: undefined, roleKey: undefined, status: undefined },
      form: {},
      defaultProps: { children: "children", label: "label" },
      rules: {
        roleName: [{ required: true, message: "角色名称不能为空", trigger: "blur" }],
        roleKey: [{ required: true, message: "权限字符不能为空", trigger: "blur" }],
        roleSort: [{ required: true, message: "角色顺序不能为空", trigger: "blur" }],
        remark: [{ max: 250, message: '长度必须小于250个字符', trigger: 'blur' }]
      },
      addRules: {
        roleName: [
          { required: true, message: "请输入角色名称", trigger: "blur" },
          { max: 100, message: "不超过 100 个字符", trigger: "blur" },
        ],
        roleKey: [
          { required: true, message: "请选择或输入权限字符", trigger: "change" },
          { pattern: /^[A-Z][A-Z0-9_]{1,62}$/, message: "大写字母开头，仅 A-Z、0-9、下划线", trigger: "change" },
        ],
      }
    };
  },
  computed: {
    isAddMode() {
      return this.open && this.form.roleId == undefined;
    },
    displayRoleList() {
      if (!this.embedMode || !this.scopeRoleId) return this.roleList;
      return this.roleList.filter(r => String(r.roleId) === String(this.scopeRoleId));
    },
    displayTotal() {
      if (this.embedMode && this.scopeRoleId) return this.displayRoleList.length;
      return this.total;
    },
    usedRoleKeySet() {
      const set = new Set();
      (this.roleList || []).forEach((role) => {
        const key = String(role.roleKey || role.roleCode || "").trim().toUpperCase();
        if (key) set.add(key);
      });
      return set;
    },
    availablePresets() {
      return ROLE_KEY_PRESETS.filter((preset) => !this.usedRoleKeySet.has(preset.key));
    },
    occupiedRoles() {
      return (this.roleList || [])
        .filter((role) => role.roleId !== 1 && (role.roleKey || role.roleCode))
        .sort((a, b) => (Number(a.roleSort) || 0) - (Number(b.roleSort) || 0));
    },
  },
  watch: {
    scopeStatus(val) {
      if (!this.embedMode) return;
      this.queryParams.status = val ?? undefined;
      this.handleQuery();
    },
    scopeRoleId() {
      if (!this.embedMode) return;
      this.handleQuery();
    },
  },
  created() {
    if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listRole(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        this.roleList = response.rows;
        this.total = response.total;
      }).finally(() => { this.loading = false; });
    },
    getMenuTreeselect() { menuTreeselect().then(response => { this.menuOptions = response.data; }); },
    getDeptTreeselect() { deptTreeselect().then(response => { this.deptOptions = response.data; }); },
    getMenuAllCheckedKeys() {
      let checkedKeys = this.$refs.menu.getCheckedKeys();
      let halfCheckedKeys = this.$refs.menu.getHalfCheckedKeys();
      checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
      return checkedKeys;
    },
    getDeptAllCheckedKeys() {
      let checkedKeys = this.$refs.dept.getCheckedKeys();
      let halfCheckedKeys = this.$refs.dept.getHalfCheckedKeys();
      checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
      return checkedKeys;
    },
    getRoleMenuTreeselect(roleId) {
      return roleMenuTreeselect(roleId).then(response => { this.menuOptions = response.menus; return response; });
    },
    getRoleDeptTreeselect(roleId) {
      return roleDeptTreeselect(roleId).then(response => { this.deptOptions = response.depts; return response; });
    },
    handleStatusChange(row) {
      let text = row.status === "0" ? "启用" : "停用";
      this.$modal.confirm('确认要"' + text + '""' + row.roleName + '"角色吗？').then(function() {
        return changeRoleStatus(row.roleId, row.status);
      }).then(() => { this.$modal.msgSuccess(text + "成功"); }).catch(function() {
        row.status = row.status === "0" ? "1" : "0";
      });
    },
    cancel() { this.open = false; this.reset(); },
    cancelDataScope() { this.openDataScope = false; this.reset(); },
    reset() {
      if (this.$refs.menu != undefined) this.$refs.menu.setCheckedKeys([]);
      this.menuExpand = false; this.menuNodeAll = false; this.deptExpand = true; this.deptNodeAll = false;
      this.form = { roleId: undefined, roleName: undefined, roleKey: undefined, roleSort: 0, status: "0", menuIds: [], deptIds: [], menuCheckStrictly: true, deptCheckStrictly: true, remark: undefined };
      this.resetForm("form");
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); },
    resetQuery() {
      this.dateRange = [];
      this.resetForm("queryForm");
      if (this.embedMode) this.queryParams.status = this.scopeStatus ?? undefined;
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.roleId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    handleCommand(command, row) {
      if (command === "handleDataScope") this.handleDataScope(row);
      else if (command === "handleAuthUser") this.handleAuthUser(row);
    },
    handleCheckedTreeExpand(value, type) {
      const ref = type === 'menu' ? this.$refs.menu : this.$refs.dept;
      const treeList = type === 'menu' ? this.menuOptions : this.deptOptions;
      for (let i = 0; i < treeList.length; i++) ref.store.nodesMap[treeList[i].id].expanded = value;
    },
    handleCheckedTreeNodeAll(value, type) {
      const ref = type === 'menu' ? this.$refs.menu : this.$refs.dept;
      const treeList = type === 'menu' ? this.menuOptions : this.deptOptions;
      ref.setCheckedNodes(value ? treeList : []);
    },
    handleCheckedTreeConnect(value, type) {
      if (type == 'menu') this.form.menuCheckStrictly = !!value;
      else this.form.deptCheckStrictly = !!value;
    },
    handleAdd() {
      this.reset();
      this.form.status = "0";
      this.form.roleSort = this.nextRoleSort();
      this.form.dataScope = "1";
      this.form.menuCheckStrictly = true;
      this.form.deptCheckStrictly = true;
      this.open = true;
      this.title = "新增角色";
    },
    handleUpdate(row) {
      this.reset();
      const roleId = row.roleId || this.ids;
      const roleMenu = this.getRoleMenuTreeselect(roleId);
      getRole(roleId).then(response => {
        this.form = response.data;
        this.open = true;
        this.$nextTick(() => {
          roleMenu.then(res => {
            res.checkedKeys.forEach((v) => {
              this.$nextTick(() => { this.$refs.menu.setChecked(v, true, false); });
            });
          });
        });
        this.title = "修改角色";
      });
    },
    nextRoleSort() {
      const max = (this.roleList || []).reduce((m, r) => Math.max(m, Number(r.roleSort) || 0), 0);
      return max + 10;
    },
    onRoleKeyChange(val) {
      const key = String(val || "").trim().toUpperCase().replace(/[^A-Z0-9_]/g, "");
      if (key !== val) this.form.roleKey = key || undefined;
      if (!key) return;
      if (this.usedRoleKeySet.has(key)) {
        this.$modal.msgWarning("该权限字符已被占用，请选择其他模板或输入新编码");
        this.form.roleKey = undefined;
        return;
      }
      const preset = ROLE_KEY_PRESETS.find((item) => item.key === key);
      if (preset && !(this.form.roleName || "").trim()) {
        this.form.roleName = preset.roleName;
      }
    },
    dataScopeSelectChange(value) { if (value !== '2') this.$refs.dept.setCheckedKeys([]); },
    handleDataScope(row) {
      this.reset();
      const roleDeptTreeselectFn = this.getRoleDeptTreeselect(row.roleId);
      getRole(row.roleId).then(response => {
        this.form = response.data;
        this.openDataScope = true;
        this.$nextTick(() => { roleDeptTreeselectFn.then(res => { this.$refs.dept.setCheckedKeys(res.checkedKeys); }); });
        this.title = "分配数据权限";
      });
    },
    handleAuthUser(row) { this.$router.push("/app/system/role-auth/user/" + row.roleId); },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        if (this.isAddMode) {
          this.form.roleKey = (this.form.roleKey || "").trim().toUpperCase();
          this.form.menuIds = [];
          this.form.status = this.form.status || "0";
          this.form.dataScope = "1";
          this.form.menuCheckStrictly = true;
          this.form.deptCheckStrictly = true;
          if (this.form.roleSort == null) this.form.roleSort = this.nextRoleSort();
        } else {
          this.form.menuIds = this.getMenuAllCheckedKeys();
        }
        const req = this.form.roleId != undefined ? updateRole(this.form) : addRole(this.form);
        req.then(() => {
          this.$modal.msgSuccess(this.form.roleId != undefined ? "修改成功" : "新增成功");
          this.open = false;
          this.getList();
          if (this.embedMode) this.$emit("changed");
        });
      });
    },
    submitDataScope() {
      if (this.form.roleId == undefined) return;
      this.form.deptIds = this.getDeptAllCheckedKeys();
      dataScope(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.openDataScope = false; this.getList(); });
    },
    handleDelete(row) {
      const roleIds = row.roleId || this.ids;
      this.$modal.confirm('是否确认删除角色编号为"' + roleIds + '"的数据项？').then(function() { return delRole(roleIds); }).then(() => {
        this.getList(); this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleExport() { this.download('system/role/export', { ...this.queryParams }, `role_${new Date().getTime()}.xlsx`); }
  }
};
</script>
