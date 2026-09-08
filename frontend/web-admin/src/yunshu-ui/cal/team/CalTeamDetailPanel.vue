<template>
  <section class="cal-workbench-detail">
    <div class="cal-workbench-detail__head">
      <div>
        <h3>{{ headline }}</h3>
        <p>{{ subtitle }}</p>
      </div>
      <div class="cal-workbench-detail__actions">
        <template v-if="hasSelection">
          <el-switch
            v-if="editing"
            :model-value="autoGenFlag"
            active-text="自动编号"
            @update:model-value="$emit('toggle-auto-gen', $event)"
          />
          <el-button v-if="!editing" type="primary" plain @click="$emit('edit')">编辑</el-button>
          <el-button v-if="editing" type="primary" :loading="saving" @click="$emit('save')">保存</el-button>
          <el-button v-if="editing" @click="$emit('cancel-edit')">取消</el-button>
          <el-button v-if="!editing && !isNew" type="danger" plain @click="$emit('delete')">删除</el-button>
        </template>
        <el-button v-else type="primary" @click="$emit('create')">新增班组</el-button>
      </div>
    </div>

    <div v-if="hasSelection" class="cal-workbench-detail__body">
      <el-form class="cal-workbench-detail__form" :model="form" label-width="88px" :disabled="!editing">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="班组编号" required>
              <el-input v-model="form.teamCode" placeholder="请输入班组编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班组名称" required>
              <el-input v-model="form.teamName" placeholder="请输入班组名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班组类型" required>
              <el-select v-model="form.calendarType" placeholder="请选择班组类型" style="width: 100%">
                <el-option
                  v-for="item in calendarTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-select v-model="form.enableFlag" style="width: 100%">
                <el-option label="启用" value="Y" />
                <el-option label="停用" value="N" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="form.teamId" class="cal-workbench-detail__section">
        <h4 class="cal-workbench-detail__section-title">班组成员</h4>
        <CalTeamMemberPanel :team-id="form.teamId" :readonly="!editing" />
      </div>
      <p v-else class="cal-workbench-detail__empty">保存班组基础信息后可维护成员。</p>
    </div>

    <div v-else class="cal-workbench-detail__empty">
      <b>请选择左侧班组</b>
      <span>或点击右上角「新增班组」开始创建</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import CalTeamMemberPanel from "./CalTeamMemberPanel.vue";

const props = defineProps({
  form: { type: Object, required: true },
  editing: Boolean,
  isNew: Boolean,
  saving: Boolean,
  autoGenFlag: Boolean,
  calendarTypes: { type: Array, default: () => [] },
});

defineEmits(["edit", "save", "cancel-edit", "delete", "create", "toggle-auto-gen"]);

const hasSelection = computed(() => props.isNew || Boolean(props.form.teamId));

const headline = computed(() => {
  if (props.isNew) return "新增班组";
  return props.form.teamName || props.form.teamCode || "班组详情";
});

const subtitle = computed(() => {
  if (props.isNew) return "填写基础信息并保存";
  return [props.form.teamCode, props.editing ? "编辑中" : "查看"].filter(Boolean).join(" · ");
});
</script>
