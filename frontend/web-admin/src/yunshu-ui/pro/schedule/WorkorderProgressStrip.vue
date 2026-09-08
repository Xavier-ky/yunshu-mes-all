<template>
  <div class="app-container progress-strip">
    <div class="strip-head">
      <h3>工序进度</h3>
      <RouterLink to="/app/planning/progress" class="strip-link">查看全部</RouterLink>
    </div>
    <el-table
      v-loading="loading"
      class="yunshu-data-table"
      stripe
      border
      :data="workorderList"
      row-key="workorderId"
      default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column label="工单编码" width="160" prop="workorderCode" show-overflow-tooltip />
      <el-table-column label="产品名称" width="160" align="center" prop="productName" show-overflow-tooltip />
      <el-table-column label="工单数量" width="90" align="center" prop="quantity" />
      <el-table-column label="工序进度" align="center" min-width="320">
        <template #default="scope">
          <div class="route-row">
            <div v-for="(item, index) in scope.row.routeHomg || []" :key="index" class="route-cell">
              <span v-if="index !== 0" class="route-sep">—</span>
              <div class="route-item">
                <el-progress type="circle" :width="70" :percentage="pct(item)" />
                <el-tooltip :content="item.processName" placement="bottom">
                  <span class="route-name">{{ item.processName }}</span>
                </el-tooltip>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { getHomeList } from "@/yunshu-ui/api/mes/pro/workorder";

export default {
  name: "WorkorderProgressStrip",
  data() {
    return {
      loading: false,
      workorderList: [],
    };
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      getHomeList({ status: "CONFIRMED" }).then((response) => {
        const data = response?.data || [];
        this.workorderList = this.handleTree(data, "workorderId", "parentId");
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    pct(item) {
      const total = Number(item.total) || 0;
      const done = Number(item.completeNumber) || 0;
      if (total <= 0) return 0;
      return parseFloat(((done / total) * 100).toFixed(0));
    },
    handleTree(data, id, parentId) {
      const map = {};
      const roots = [];
      (data || []).forEach((row) => {
        map[row[id]] = { ...row, children: [] };
      });
      (data || []).forEach((row) => {
        const pid = row[parentId];
        if (pid && pid !== 0 && map[pid]) {
          map[pid].children.push(map[row[id]]);
        } else {
          roots.push(map[row[id]]);
        }
      });
      return roots;
    },
  },
};
</script>

<style scoped>
.progress-strip { margin-top: 16px; }
.strip-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.strip-head h3 { margin: 0; font-size: 16px; }
.strip-link { font-size: 13px; color: #2563eb; text-decoration: none; }
.route-row { display: flex; overflow-x: auto; align-items: center; }
.route-cell { display: flex; align-items: center; flex-shrink: 0; }
.route-sep { color: #5e75ff; margin: 0 4px; }
.route-item { display: flex; flex-direction: column; align-items: center; margin: 0 4px; }
.route-name {
  display: inline-block; width: 56px; overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap; font-size: 12px; text-align: center;
}
</style>
