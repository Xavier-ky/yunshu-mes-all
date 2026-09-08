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
            v-if="editing && canEdit"
            :model-value="autoGenFlag"
            active-text="自动编号"
            @update:model-value="$emit('toggle-auto-gen', $event)"
          />
          <el-button v-if="canEdit && !editing" type="primary" plain @click="$emit('edit')">编辑</el-button>
          <el-button v-if="canEdit && editing" type="primary" :loading="saving" @click="$emit('save')">保存</el-button>
          <el-button
            v-if="canEdit && editing && form.planId"
            type="success"
            :loading="saving"
            @click="$emit('finish')"
          >
            完成
          </el-button>
          <el-button v-if="canEdit && editing" @click="$emit('cancel-edit')">取消</el-button>
          <el-button v-if="canEdit && !editing && form.planId" type="danger" plain @click="$emit('delete')">
            删除
          </el-button>
        </template>
        <el-button v-else type="primary" @click="$emit('create')">新增计划</el-button>
      </div>
    </div>

    <div v-if="hasSelection" class="cal-workbench-detail__body">
      <el-form class="cal-workbench-detail__form" :model="form" label-width="88px" :disabled="!editing || !canEdit">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="计划编号" required>
              <el-input v-model="form.planCode" placeholder="请输入计划编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计划名称" required>
              <el-input v-model="form.planName" placeholder="请输入计划名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班组类型" required>
              <el-select v-model="form.calendarType" placeholder="请选择" style="width: 100%">
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
            <el-form-item label="开始日期" required>
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束日期" required>
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单据状态">
              <dict-tag :options="orderStatuses" :value="form.status" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="轮班方式">
              <el-radio-group v-model="form.shiftType">
                <el-radio
                  v-for="item in shiftTypes"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.shiftType !== 'SINGLE'" :span="6">
            <el-form-item label="倒班方式">
              <el-select v-model="form.shiftMethod" style="width: 100%">
                <el-option
                  v-for="item in shiftMethods"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.shiftMethod === 'DAY' && form.shiftType !== 'SINGLE'" :span="6">
            <el-form-item label="天数">
              <el-input-number v-model="form.shiftCount" :min="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>

      <el-tabs v-if="form.planId" class="cal-workbench-detail__tabs" type="border-card">
        <el-tab-pane label="班次">
          <CalPlanShiftPanel :plan-id="form.planId" :readonly="!canEdit || !editing" />
        </el-tab-pane>
        <el-tab-pane label="关联班组">
          <CalPlanTeamPanel
            :plan-id="form.planId"
            :calendar-type="form.calendarType"
            :readonly="!canEdit || !editing"
          />
        </el-tab-pane>
      </el-tabs>
      <p v-else class="cal-workbench-detail__empty">保存计划基础信息后可维护班次与关联班组。</p>
    </div>

    <div v-else class="cal-workbench-detail__empty">
      <b>请选择左侧排班计划</b>
      <span>或点击右上角「新增计划」开始创建</span>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import CalPlanShiftPanel from "./CalPlanShiftPanel.vue";
import CalPlanTeamPanel from "./CalPlanTeamPanel.vue";
import { isPlanEditable } from "./calPlanModel.js";

const props = defineProps({
  form: { type: Object, required: true },
  editing: Boolean,
  isNew: Boolean,
  saving: Boolean,
  autoGenFlag: Boolean,
  calendarTypes: { type: Array, default: () => [] },
  shiftTypes: { type: Array, default: () => [] },
  shiftMethods: { type: Array, default: () => [] },
  orderStatuses: { type: Array, default: () => [] },
});

defineEmits(["edit", "save", "finish", "cancel-edit", "delete", "create", "toggle-auto-gen"]);

const hasSelection = computed(() => props.isNew || Boolean(props.form.planId));
const canEdit = computed(() => isPlanEditable(props.form));

const headline = computed(() => {
  if (props.isNew) return "新增排班计划";
  return props.form.planName || props.form.planCode || "计划详情";
});

const subtitle = computed(() => {
  if (props.isNew) return "填写基础信息并保存";
  const status = canEdit.value ? (props.editing ? "编制中 · 编辑" : "编制中 · 查看") : "已确认 · 只读";
  return [props.form.planCode, status].filter(Boolean).join(" · ");
});
</script>
