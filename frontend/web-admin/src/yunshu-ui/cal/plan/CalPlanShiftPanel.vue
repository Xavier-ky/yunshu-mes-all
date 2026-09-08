<template>
  <div class="cal-workbench-subpanel">
    <div v-if="!readonly" class="cal-workbench-subpanel__bar">
      <el-button type="primary" plain :icon="Plus" @click="openCreate">新增班次</el-button>
    </div>
    <div class="cal-workbench-subpanel__table-wrap" v-loading="loading">
      <el-table class="yunshu-data-table" stripe border :data="rows">
        <el-table-column label="序号" prop="orderNum" width="72" align="center" />
        <el-table-column label="班次名称" prop="shiftName" min-width="120" show-overflow-tooltip />
        <el-table-column label="开始时间" prop="startTime" width="100" align="center" />
        <el-table-column label="结束时间" prop="endTime" width="100" align="center" />
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column v-if="!readonly" label="操作" width="120" align="center" class-name="col-actions" fixed="right">
          <template #default="{ row }">
            <div class="yunshu-row-actions">
              <el-button link type="primary" @click="openEdit(row)">修改</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogOpen" :title="dialogTitle" width="520px" append-to-body>
      <el-form ref="formRef" :model="shiftForm" :rules="rules" label-width="88px">
        <el-form-item label="序号" prop="orderNum">
          <el-input-number v-model="shiftForm.orderNum" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="班次名称" prop="shiftName">
          <el-input v-model="shiftForm.shiftName" placeholder="请输入班次名称" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker v-model="shiftForm.startTime" value-format="HH:mm" format="HH:mm" placeholder="开始时间" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker v-model="shiftForm.endTime" value-format="HH:mm" format="HH:mm" placeholder="结束时间" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listShift, getShift, addShift, updateShift, delShift } from "@/yunshu-ui/api/mes/cal/shift";

const props = defineProps({
  planId: { type: [Number, String], default: null },
  readonly: Boolean,
});

const loading = ref(false);
const saving = ref(false);
const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const dialogOpen = ref(false);
const dialogTitle = ref("新增班次");
const formRef = ref(null);
const shiftForm = ref(createEmptyShift());
const rules = {
  shiftName: [{ required: true, message: "班次名称不能为空", trigger: "blur" }],
  startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
  endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }],
};

function createEmptyShift() {
  return {
    shiftId: null,
    planId: props.planId,
    orderNum: 1,
    shiftName: "",
    startTime: "",
    endTime: "",
    remark: "",
  };
}

async function load() {
  if (!props.planId) {
    rows.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const response = await listShift({
      pageNum: page.value,
      pageSize: pageSize.value,
      planId: props.planId,
    });
    rows.value = response?.rows || [];
    total.value = Number(response?.total) || 0;
  } catch (cause) {
    ElMessage.error(cause?.message || "班次列表加载失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  shiftForm.value = createEmptyShift();
  dialogTitle.value = "新增班次";
  dialogOpen.value = true;
}

async function openEdit(row) {
  const response = await getShift(row.shiftId);
  shiftForm.value = { ...createEmptyShift(), ...(response?.data || response) };
  dialogTitle.value = "修改班次";
  dialogOpen.value = true;
}

async function submit() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    shiftForm.value.planId = props.planId;
    if (shiftForm.value.shiftId) {
      await updateShift(shiftForm.value);
      ElMessage.success("班次已更新");
    } else {
      await addShift(shiftForm.value);
      ElMessage.success("班次已创建");
    }
    dialogOpen.value = false;
    await load();
  } catch (cause) {
    if (cause?.message) ElMessage.error(cause.message);
  } finally {
    saving.value = false;
  }
}

async function remove(row) {
  await ElMessageBox.confirm("是否确认删除该班次？", "提示", { type: "warning" });
  await delShift(row.shiftId);
  ElMessage.success("删除成功");
  await load();
}

watch(
  () => props.planId,
  () => {
    page.value = 1;
    load();
  },
  { immediate: true },
);
</script>
