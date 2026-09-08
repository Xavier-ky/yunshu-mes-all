<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="出库单编号" width="150px" align="center" prop="salesCode" >
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            @click="handleView(scope.row)"
          >{{scope.row.salesCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="出库单名称" width="150px" align="center" prop="salesName" />
      <el-table-column label="销售订单编号" width="120px" align="center" prop="soCode" />
      <el-table-column label="客户编码" align="center" prop="clientCode" />
      <el-table-column label="客户名称" align="center" prop="clientName" />
      <el-table-column label="出库日期" align="center" prop="salesDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.salesDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
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
import {getSaleRecord} from "@/yunshu-ui/api/mes/wm/productsales";

export default {
  name: "productsalesForm",
  dicts: ['sys_yes_no','mes_item_product', 'mes_order_status'],
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
    clientId: undefined
  },
  created() {
    this.getList(this.clientId);
  },
  methods: {
    handleView(row) {
      this.$emit('handleSkip', row)
      this.$router.push({
        path: '/mes/wm/saleOutbound/form',
        query: {
          id: row.salesId
        }
      })
    },
    handleOpen(vendorId) {
      this.getList(vendorId)
    },
    /** 获取表格数据 */
    getList(clientId) {
      this.loading = true;
      getSaleRecord(clientId).then(response => {
        this.recordList = response.data;
        this.loading = false;
      });
    },
  }
};
</script>
