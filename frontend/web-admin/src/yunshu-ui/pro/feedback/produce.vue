<template>
  <div class="cal-workbench-subpanel feedback-subpanel">
    <div class="cal-workbench-subpanel__table-wrap" v-loading="loading">
      <el-table
        class="yunshu-data-table inbound-table"
        stripe
        border
        :data="productproducelineList"
      >
        <el-table-column label="物资编码" align="center" header-align="center" min-width="100" prop="itemCode" show-overflow-tooltip />
        <el-table-column label="物资名称" align="center" header-align="center" min-width="100" prop="itemName" show-overflow-tooltip />
        <el-table-column label="规格型号" align="center" header-align="center" min-width="100" prop="specification" show-overflow-tooltip />
        <el-table-column label="单位" align="center" header-align="center" width="72" prop="unitName" />
        <el-table-column label="产出数量" align="center" header-align="center" width="88" prop="quantityProduce" />
        <el-table-column label="批次号" align="center" header-align="center" min-width="100" prop="batchCode" show-overflow-tooltip />
        <el-table-column label="质量状态" align="center" header-align="center" width="96" prop="qualityStatus">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_quality_status" :value="scope.row.qualityStatus" />
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="total > 0" class="cal-workbench-subpanel__footer">
      <span class="cal-workbench-subpanel__footer-total">共 {{ total }} 条</span>
      <pagination
        :total="total"
        :page="queryParams.pageNum"
        :limit="queryParams.pageSize"
        layout="prev, pager, next"
        :pager-count="5"
        :auto-scroll="false"
        @update:page="queryParams.pageNum = $event"
        @update:limit="queryParams.pageSize = $event"
        @pagination="getList"
      />
    </div>
  </div>
</template>

<script>
import { listProductproduceline } from "@/yunshu-ui/api/mes/wm/productproduceline";

export default {
  name: "Productproduceline",
  dicts: ["mes_quality_status"],
  props: {
    feedbackId: { type: [Number, String], default: null },
  },
  data() {
    return {
      loading: true,
      total: 0,
      productproducelineList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        feedbackId: this.feedbackId,
      },
    };
  },
  watch: {
    feedbackId(value) {
      this.queryParams.feedbackId = value;
      this.queryParams.pageNum = 1;
      this.getList();
    },
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      if (!this.queryParams.feedbackId) {
        this.productproducelineList = [];
        this.total = 0;
        this.loading = false;
        return;
      }
      this.loading = true;
      listProductproduceline(this.queryParams).then((response) => {
        this.productproducelineList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
  },
};
</script>
