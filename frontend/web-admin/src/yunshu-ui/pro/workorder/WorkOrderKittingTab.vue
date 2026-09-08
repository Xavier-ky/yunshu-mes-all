<template>
  <div class="wo-kitting-tab">
    <div v-if="!workorder?.workorderId" class="wo-kitting-empty">请先保存工单后再做齐套分析</div>
    <template v-else>
      <div class="wo-kitting-toolbar">
        <el-button type="primary" size="small" :loading="loading" @click="analyze">
          {{ loading ? "分析中…" : "分析齐套" }}
        </el-button>
        <el-button
          v-if="result"
          type="success"
          size="small"
          :disabled="isKittingDone"
          @click="confirmReserve"
        >
          {{ isKittingDone ? "已齐套锁定" : "确认齐套并锁定库存" }}
        </el-button>
        <el-button v-if="result && !isKittingDone" size="small" @click="confirmRelease">
          释放锁定
        </el-button>
        <el-tag v-if="isKittingDone" type="success" size="small">流程状态：齐套完成</el-tag>
        <el-button v-if="isKittingDone" type="primary" size="small" link @click="goScheduling">
          去排产派工 →
        </el-button>
        <el-tag v-else-if="lifecycleStatus === 'RELEASED'" type="info" size="small">流程状态：已下达</el-tag>
      </div>

      <el-alert
        v-if="result && !result.allSufficient"
        type="warning"
        :closable="false"
        show-icon
        class="wo-kitting-alert"
        title="存在欠料，仍可锁定当前可用库存并推进流程；请安排补料后再领料生产。"
      />

      <div v-if="result" class="wo-kitting-summary" :class="result.allSufficient ? 'is-ok' : 'is-warn'">
        <strong>{{ result.allSufficient ? "齐套充足" : "存在欠料" }}</strong>
        <span>
          {{ result.workOrderNo }} · {{ result.productName }} · 计划 {{ formatQty(result.planQty) }}
        </span>
      </div>

      <el-table
        v-if="result?.details?.length"
        v-loading="loading"
        :data="result.details"
        class="yunshu-data-table"
        stripe
        border
        size="small"
        max-height="320"
      >
        <el-table-column label="物料编码" prop="materialCode" min-width="120" show-overflow-tooltip />
        <el-table-column label="物料名称" prop="materialName" min-width="140" show-overflow-tooltip />
        <el-table-column label="关键" width="64" align="center">
          <template #default="scope">{{ scope.row.isKey ? "是" : "否" }}</template>
        </el-table-column>
        <el-table-column label="单耗" prop="qtyPer" width="72" align="right" />
        <el-table-column label="需求量" width="88" align="right">
          <template #default="scope">{{ formatQty(scope.row.required) }}</template>
        </el-table-column>
        <el-table-column label="可用库存" width="96" align="right">
          <template #default="scope">{{ formatQty(scope.row.available) }}</template>
        </el-table-column>
        <el-table-column label="短缺" width="80" align="right">
          <template #default="scope">
            <span :class="scope.row.sufficient ? '' : 'wo-kitting-short'">
              {{ scope.row.sufficient ? "0" : formatQty(scope.row.shortage) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.sufficient ? 'success' : 'warning'" size="small">
              {{ scope.row.sufficient ? "充足" : "短缺" }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-else-if="analyzed && !loading" class="wo-kitting-empty">暂无 BOM 明细或库存数据，请确认产品已维护 BOM</div>
    </template>
  </div>
</template>

<script>
import { fetchKitting, releaseKitting, reserveKitting } from "@/yunshu-ui/api/mes/kitting";

export default {
  name: "WorkOrderKittingTab",
  props: {
    workorder: { type: Object, default: () => ({}) },
    lifecycleStatus: { type: String, default: "" },
  },
  emits: ["kitting-done"],
  data() {
    return {
      loading: false,
      analyzed: false,
      result: null,
    };
  },
  computed: {
    isKittingDone() {
      const ls = this.lifecycleStatus || this.workorder?.lifecycleStatus || "";
      return ls === "KITTING_OK" || ls === "SCHEDULED" || ls === "MATERIAL_ISSUED"
        || ls === "IN_PROGRESS" || ls === "QC_PENDING" || ls === "QC_PASSED" || ls === "COMPLETED";
    },
  },
  watch: {
    "workorder.workorderId": {
      immediate: true,
      handler(id) {
        this.result = null;
        this.analyzed = false;
        if (id) {
          this.analyze();
        }
      },
    },
  },
  methods: {
    formatQty(v) {
      return Number(v || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 });
    },
    analyze() {
      const id = this.workorder?.workorderId;
      if (!id) return;
      this.loading = true;
      fetchKitting(id)
        .then((res) => {
          this.result = res?.data || null;
          this.analyzed = true;
        })
        .catch((e) => {
          this.result = null;
          this.$modal?.msgError?.(e?.message || "齐套分析失败");
        })
        .finally(() => {
          this.loading = false;
        });
    },
    confirmReserve() {
      const id = this.workorder?.workorderId;
      if (!id) return;
      const warn = this.result && !this.result.allSufficient
        ? "当前存在欠料，系统将锁定可用库存并标记齐套完成。是否继续？"
        : "确认齐套并锁定该工单所需物料库存？";
      this.$modal.confirm(warn).then(() => {
        return reserveKitting(id);
      }).then(() => {
        this.$modal.msgSuccess("齐套完成，库存已锁定");
        this.$emit("kitting-done");
        this.analyze();
      }).catch(() => {});
    },
    confirmRelease() {
      const id = this.workorder?.workorderId;
      if (!id) return;
      this.$modal.confirm("确认释放该工单已锁定的库存？").then(() => {
        return releaseKitting(id);
      }).then(() => {
        this.$modal.msgSuccess("锁定已释放");
        this.analyze();
      }).catch(() => {});
    },
    goScheduling() {
      const code = this.workorder?.workorderCode;
      if (code) {
        this.$router.push({ path: "/app/planning/scheduling", query: { wo: code } });
      } else {
        this.$router.push("/app/planning/scheduling");
      }
    },
  },
};
</script>

<style scoped>
.wo-kitting-tab {
  display: grid;
  gap: 12px;
}
.wo-kitting-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.wo-kitting-summary {
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.wo-kitting-summary.is-ok {
  border-left: 3px solid #67c23a;
  background: #f0f9eb;
}
.wo-kitting-summary.is-warn {
  border-left: 3px solid #e6a23c;
  background: #fdf6ec;
}
.wo-kitting-summary strong {
  font-size: 14px;
  color: #303133;
}
.wo-kitting-summary span {
  font-size: 12px;
  color: #606266;
}
.wo-kitting-empty {
  padding: 28px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.wo-kitting-short {
  color: #f56c6c;
  font-weight: 600;
}
.wo-kitting-alert {
  margin-bottom: 0;
}
</style>
