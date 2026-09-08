<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="入库编号" align="center" prop="recptCode" width="120px" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            @click="handleView(scope.row)"
          >{{scope.row.recptCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="入库名称" align="center" prop="recptName" />
      <el-table-column label="采购订单号" align="center" prop="poCode" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120px" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" />
      <el-table-column label="规格型号" align="center" prop="specification" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" :show-overflow-tooltip="true" />
      <el-table-column label="入库数量" align="center" prop="quantityRecived" :show-overflow-tooltip="true" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>

import {getProcurementRecord} from "@/yunshu-ui/api/mes/wm/itemrecpt";

export default {
  name: "Bom",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 产品BOM关系表格数据
      recordList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    };
  },
  props: {
    optType: undefined,
    vendorId: undefined
  },
  created() {
    this.getList(this.vendorId);
  },
  methods: {
    handleView(row) {
      this.$emit('handleSkip', row)
      this.$router.push({
        path: '/mes/wm/purchaseInventory/form',
        query: {
          id: row.recptId
        }
      })
    },
    handleOpen(vendorId) {
      this.getList(vendorId)
    },
    /** 获取表格数据 */
    getList(vendorId) {
      this.loading = true;
      getProcurementRecord(vendorId).then(response => {
        this.recordList = response.data;
        this.loading = false;
      });
    },
  }
};
</script>
