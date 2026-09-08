<template>
  <div class="cal-workbench-subpanel">
    <div v-if="!readonly" class="cal-workbench-subpanel__bar">
      <el-button type="primary" plain :icon="Plus" @click="openUserSelect">添加成员</el-button>
      <el-button type="danger" plain :icon="Delete" :disabled="!selectedIds.length" @click="removeSelected">
        删除
      </el-button>
    </div>
    <div class="cal-workbench-subpanel__table-wrap" v-loading="loading">
      <el-table
        class="yunshu-data-table"
        stripe
        border
        :data="rows"
        @selection-change="onSelectionChange"
      >
        <el-table-column v-if="!readonly" type="selection" width="48" align="center" />
        <el-table-column label="用户名" prop="userName" min-width="100" show-overflow-tooltip />
        <el-table-column label="用户昵称" prop="nickName" min-width="100" show-overflow-tooltip />
        <el-table-column label="电话" prop="tel" min-width="110" show-overflow-tooltip />
        <el-table-column v-if="!readonly" label="操作" width="88" align="center" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <div class="yunshu-row-actions">
              <el-button link type="danger" @click="removeOne(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="total > 0" class="cal-workbench-subpanel__footer">
      <span class="cal-workbench-subpanel__footer-total">共 {{ total }} 条</span>
      <pagination
        :total="total"
        :page="page"
        :limit="pageSize"
        layout="prev, pager, next"
        :pager-count="5"
        :auto-scroll="false"
        @update:page="page = $event; load()"
        @update:limit="pageSize = $event; page = 1; load()"
      />
    </div>
    <UserMultiSelect ref="userSelectRef" @on-selected="onUsersSelected" />
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { Delete, Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listTeammember, addTeammember, delTeammember } from "@/yunshu-ui/api/mes/cal/teammember";
import UserMultiSelect from "@/yunshu-ui/components/userSelect/multi.vue";

const props = defineProps({
  teamId: { type: [Number, String], default: null },
  readonly: Boolean,
});

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const selectedIds = ref([]);
const userSelectRef = ref(null);

async function load() {
  if (!props.teamId) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const response = await listTeammember({
      pageNum: page.value,
      pageSize: pageSize.value,
      teamId: props.teamId,
    });
    rows.value = response?.rows || [];
    total.value = Number(response?.total) || 0;
  } catch (cause) {
    ElMessage.error(cause?.message || "成员列表加载失败");
  } finally {
    loading.value = false;
  }
}

function onSelectionChange(selection) {
  selectedIds.value = selection.map((item) => item.memberId);
}

function openUserSelect() {
  if (props.readonly) return;
  userSelectRef.value.showFlag = true;
}

async function onUsersSelected(users) {
  if (!props.teamId || !Array.isArray(users)) return;
  for (const user of users) {
    await addTeammember({
      teamId: props.teamId,
      userId: user.userId,
      userName: user.userName,
      nickName: user.nickName,
      tel: user.phonenumber,
    });
  }
  ElMessage.success("成员已添加");
  await load();
}

async function removeOne(row) {
  await ElMessageBox.confirm("是否确认删除该成员？", "提示", { type: "warning" });
  await delTeammember(row.memberId);
  ElMessage.success("删除成功");
  await load();
}

async function removeSelected() {
  if (!selectedIds.value.length) return;
  await ElMessageBox.confirm("是否确认删除所选成员？", "提示", { type: "warning" });
  await delTeammember(selectedIds.value.join(","));
  ElMessage.success("删除成功");
  selectedIds.value = [];
  await load();
}

watch(
  () => props.teamId,
  () => {
    page.value = 1;
    load();
  },
  { immediate: true },
);
</script>
