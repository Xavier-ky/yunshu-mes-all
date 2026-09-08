<template>
  <section class="cal-workbench-detail process-workbench-detail">
    <div class="cal-workbench-detail__head">
      <div class="process-workbench-detail__head-main">
        <img
          v-if="coverUrl"
          :src="coverUrl"
          alt=""
          class="process-workbench-detail__cover"
        />
        <div>
          <h3>{{ headline }}</h3>
          <p>{{ subtitle }}</p>
        </div>
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
        <el-button v-else type="primary" @click="$emit('create')">新增工序</el-button>
      </div>
    </div>

    <div v-if="hasSelection" class="cal-workbench-detail__body">
      <div v-if="!isNew && form.processId" class="process-detail-summary">
        <span class="process-detail-summary__tag">{{ meta.typeLabel }}</span>
        <span class="process-detail-summary__tag" :class="form.enableFlag === 'Y' ? 'is-on' : 'is-off'">
          {{ form.enableFlag === "Y" ? "启用" : "停用" }}
        </span>
        <span class="process-detail-summary__item">工位 {{ meta.station }}</span>
        <span class="process-detail-summary__item">操作步骤 <b>{{ stats.stepCount }}</b> 项</span>
        <span class="process-detail-summary__item">引用路线 <b>{{ stats.routeCount }}</b> 条</span>
      </div>

      <el-form class="cal-workbench-detail__form" :model="form" label-width="88px" :disabled="!editing">
        <el-row v-if="editing" :gutter="16">
          <el-col :span="24">
            <el-form-item label="封面图">
              <ImageUpload
                :limit="1"
                :value="form.attr2"
                :file-size="5"
                @onUploaded="handleCoverUploaded"
                @onRemoved="handleCoverRemoved"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="工序编码" required>
              <el-input v-model="form.processCode" placeholder="请输入工序编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="工序名称" required>
              <el-input v-model="form.processName" placeholder="请输入工序名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-select v-model="form.enableFlag" style="width: 100%">
                <el-option label="启用" value="Y" />
                <el-option label="停用" value="N" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="工序说明">
              <el-input
                v-model="form.attention"
                type="textarea"
                :rows="2"
                :placeholder="meta.hint || '工序说明'"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.remark || editing" :gutter="16">
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div v-if="form.processId" class="cal-workbench-detail__section">
        <h4 class="cal-workbench-detail__section-title">操作步骤与示意图</h4>
        <Processcontent
          :processId="form.processId"
          :optType="editing ? 'edit' : 'view'"
          :processCode="form.processCode"
          workbench-table
        />
      </div>
      <p v-else class="cal-workbench-detail__empty">保存工序基础信息后可维护操作步骤与步骤图。</p>
    </div>

    <div v-else class="cal-workbench-detail__empty">
      <b>请选择左侧工序</b>
      <span>或点击右上角「新增工序」开始创建</span>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import ImageUpload from "@/yunshu-ui/components/ImageUpload/index.vue";
import Processcontent from "../content.vue";
import { resolveProcessCover, getProcessMeta, getDemoProcessSteps } from "../processImageMap.js";
import { listProcesscontent } from "@/yunshu-ui/api/mes/pro/processcontent";
import { listRouteprocess } from "@/yunshu-ui/api/mes/pro/routeprocess";

const props = defineProps({
  form: { type: Object, required: true },
  editing: Boolean,
  isNew: Boolean,
  saving: Boolean,
  autoGenFlag: Boolean,
});

const emit = defineEmits(["edit", "save", "cancel-edit", "delete", "create", "toggle-auto-gen", "update:cover"]);

const stats = ref({ stepCount: 0, routeCount: 0 });
let statsToken = 0;

const hasSelection = computed(() => props.isNew || Boolean(props.form.processId));
const coverUrl = computed(() => resolveProcessCover(props.form));
const meta = computed(() => getProcessMeta(props.form.processCode));

const headline = computed(() => {
  if (props.isNew) return "新增工序";
  return props.form.processName || "工序详情";
});

const subtitle = computed(() => {
  if (props.isNew) return "填写基础信息后保存，再维护操作步骤";
  const parts = [props.form.processCode, meta.value.typeLabel].filter(Boolean);
  return parts.join(" · ");
});

async function loadDetailStats() {
  const processId = props.form.processId;
  if (!processId) {
    stats.value = { stepCount: 0, routeCount: 0 };
    return;
  }
  const token = ++statsToken;
  try {
    const [contentRes, routeProcRes] = await Promise.all([
      listProcesscontent({ processId, pageNum: 1, pageSize: 1 }),
      listRouteprocess({ processId, pageNum: 1, pageSize: 1 }),
    ]);
    if (token !== statsToken) return;
    const steps = contentRes?.rows || [];
    const stepTotal = Number(contentRes?.total) || 0;
    const demoSteps = getDemoProcessSteps(props.form.processCode);
    const useDemoSteps =
      demoSteps.length > 0 &&
      (!stepTotal || steps.every((row) => !row.contentText || !/[\u4e00-\u9fa5]/.test(String(row.contentText))));
    stats.value = {
      stepCount: useDemoSteps ? demoSteps.length : stepTotal,
      routeCount: Number(routeProcRes?.total) || 0,
    };
  } catch {
    if (token !== statsToken) return;
    stats.value = { stepCount: 0, routeCount: 0 };
  }
}

watch(
  () => props.form.processId,
  () => loadDetailStats(),
  { immediate: true },
);

function handleCoverUploaded(file) {
  emit("update:cover", file.url);
}

function handleCoverRemoved() {
  emit("update:cover", "");
}
</script>
