<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="itemList">
      <el-table-column label="物料编码" align="center" width="120px" prop="itemCode">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            @click="handleView(scope.row)"
          >{{scope.row.itemCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitOfMeasure" />
      <el-table-column label="入库数量" align="center" prop="quantityRecived" />
      <el-table-column label="批次号" align="center" prop="batchCode" />
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
import {getItem} from "@/yunshu-ui/api/mes/wm/productsales";

export default {
  name: "Bom",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 产品BOM关系表格数据
      itemList: [],
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
        path: '/mes/md/item/form',
        query: {
          id: row.itemId
        }
      })
    },
    handleOpen(vendorId) {
      this.getList(vendorId)
    },
    /** 获取表格数据 */
    getList(clientId) {
      this.loading = true;
      getItem(clientId).then(response => {
        this.itemList = response.data;
        this.loading = false;
      });
    },
  }
};
</script>
