<template>
  <div v-if="embedMode" class="hub-embed-panel andon-config-embed">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:pro:andonconfig:add']">新增</el-button>
      </el-col>
      <right-toolbar @query-table="getList" />
    </el-row>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="andonconfigList">
      <el-table-column type="index" width="50" />
      <el-table-column label="呼叫原因" align="center" prop="andonReason" min-width="160">
        <template #header><span class="req-star">*</span> 呼叫原因</template>
        <template #default="scope">
          <el-input type="textarea" v-model="scope.row.andonReason" />
        </template>
      </el-table-column>
      <el-table-column label="级别" align="center" prop="andonLevel" width="120">
        <template #header><span class="req-star">*</span> 级别</template>
        <template #default="scope">
          <el-select v-model="scope.row.andonLevel" style="width:100%">
            <el-option v-for="dict in levelOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="角色" align="center" prop="handlerRoleName" min-width="140">
        <template #default="scope">
          <el-select v-model="scope.row.handlerRoleName" @change="handleRoleSelected(scope.row)" placeholder="请选择角色" style="width:100%">
            <el-option v-for="item in rolesOptions" :key="item.roleId" :label="item.roleName" :value="item.roleName" :disabled="item.status == 1" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="处置人" align="center" prop="handlerNickName" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.handlerNickName" placeholder="请选择处置人">
            <template #append><el-button @click="handleUserSelect(scope.$index)" icon="Search" /></template>
          </el-input>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="col-actions" width="90">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button type="primary" link icon="Delete" @click="handleDelete(scope.$index, scope.row)" v-hasPermi="['mes:pro:andonconfig:remove']">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <UserSingleSelect ref="userSelect" @onSelected="onUserSelected" />
    <div class="andon-config-embed__foot">
      <el-button type="primary" @click="confirm">保存配置</el-button>
    </div>
  </div>
  <el-dialog v-else title="安灯呼叫配置" v-model="showFlag" :modal="false" width="60%" center>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:pro:andonconfig:add']">新增</el-button>
      </el-col>
      <right-toolbar @query-table="getList" />
    </el-row>
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="andonconfigList">
      <el-table-column type="index" width="50" />
      <el-table-column label="呼叫原因" align="center" prop="andonReason" min-width="160">
        <template #header><span class="req-star">*</span> 呼叫原因</template>
        <template #default="scope">
          <el-input type="textarea" v-model="scope.row.andonReason" />
        </template>
      </el-table-column>
      <el-table-column label="级别" align="center" prop="andonLevel" width="120">
        <template #header><span class="req-star">*</span> 级别</template>
        <template #default="scope">
          <el-select v-model="scope.row.andonLevel" style="width:100%">
            <el-option v-for="dict in levelOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="角色" align="center" prop="handlerRoleName" min-width="140">
        <template #default="scope">
          <el-select v-model="scope.row.handlerRoleName" @change="handleRoleSelected(scope.row)" placeholder="请选择角色" style="width:100%">
            <el-option v-for="item in rolesOptions" :key="item.roleId" :label="item.roleName" :value="item.roleName" :disabled="item.status == 1" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="处置人" align="center" prop="handlerNickName" min-width="140">
        <template #default="scope">
          <el-input v-model="scope.row.handlerNickName" placeholder="请选择处置人">
            <template #append><el-button @click="handleUserSelect(scope.$index)" icon="Search" /></template>
          </el-input>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="col-actions" width="90">
        <template #default="scope">
          <div class="yunshu-row-actions">
            <el-button type="primary" link icon="Delete" @click="handleDelete(scope.$index, scope.row)" v-hasPermi="['mes:pro:andonconfig:remove']">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="confirm">确 定</el-button>
        <el-button @click="showFlag = false">关 闭</el-button>
      </div>
    </template>
    <UserSingleSelect ref="userSelect" @onSelected="onUserSelected" />
  </el-dialog>
</template>

<script>
import { listAndonconfig, updateAndonconfig, delAndonconfig } from "@/yunshu-ui/api/mes/pro/andonconfig";
import { listRole } from "@/yunshu-ui/api/system/role";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";

export default {
  name: "AndonConfig",
  components: { UserSingleSelect },
  dicts: ["mes_andon_level"],
  props: {
    embedMode: { type: Boolean, default: false },
  },
  data() {
    return {
      showFlag: false,
      selectedRow: null,
      loading: true,
      andonconfigList: [],
      rolesOptions: [],
      queryParams: {},
    };
  },
  computed: {
    levelOptions() {
      return (this.dict && this.dict.type && this.dict.type.mes_andon_level) || [];
    },
  },
  mounted() {
    if (this.embedMode) {
      this.getList();
      this.getRoles();
    }
  },
  methods: {
    handleOpen() {
      this.showFlag = true;
      this.getList();
      this.getRoles();
    },
    getRoles() {
      listRole().then((response) => {
        this.rolesOptions = response.rows || [];
      });
    },
    getList() {
      this.loading = true;
      listAndonconfig(this.queryParams).then((response) => {
        this.andonconfigList = response.data || [];
        this.loading = false;
      }).catch(() => { this.loading = false; });
    },
    handleAdd() {
      this.andonconfigList.unshift({
        configId: null,
        andonReason: null,
        andonLevel: null,
        handlerRoleId: null,
        handlerRoleName: null,
        handlerUserId: null,
        handlerUserName: null,
        handlerNickName: null,
        flag: "add",
      });
    },
    confirm() {
      if (this.andonconfigList.length === 0) return;
      this.loading = true;
      let ok = true;
      for (const item of this.andonconfigList) {
        if (!item.andonReason) {
          this.$modal.msgError("呼叫原因不能为空");
          ok = false;
          break;
        }
        if (!item.andonLevel) {
          this.$modal.msgError("级别不能为空");
          ok = false;
          break;
        }
        if ((!item.handlerNickName || item.handlerNickName === "") && (!item.handlerRoleId && !item.handlerRoleName)) {
          this.$modal.msgError("请选择处置人或角色");
          ok = false;
          break;
        }
      }
      if (!ok) {
        this.loading = false;
        return;
      }
      updateAndonconfig(this.andonconfigList).then((res) => {
        this.$modal.msgSuccess("修改成功");
        if (res.code === 200) {
          if (!this.embedMode) this.showFlag = false;
          this.getList();
        }
      }).finally(() => { this.loading = false; });
    },
    handleDelete(index, row) {
      const configIds = row.configId;
      this.$modal.confirm("确认删除配置项？").then(() => {
        if (this.andonconfigList[index].flag === "add") {
          this.andonconfigList.splice(index, 1);
        } else {
          delAndonconfig(configIds).then(() => this.getList());
        }
      }).catch(() => {});
    },
    handleUserSelect(index) {
      this.selectedRow = index;
      this.$nextTick(() => {
        this.$refs.userSelect.showFlag = true;
      });
    },
    onUserSelected(row) {
      if (this.selectedRow == null) return;
      this.andonconfigList[this.selectedRow].handlerNickName = row.nickName;
      this.andonconfigList[this.selectedRow].handlerUserId = row.userId;
      this.andonconfigList[this.selectedRow].handlerUserName = row.userName;
    },
    handleRoleSelected(row) {
      const role = this.rolesOptions.find((item) => item.roleName === row.handlerRoleName);
      if (role) row.handlerRoleId = role.roleId;
    },
  },
};
</script>

<style scoped>
.req-star { color: #f56c6c; margin-right: 4px; }
.andon-config-embed__foot { margin-top: 12px; text-align: right; }
</style>
