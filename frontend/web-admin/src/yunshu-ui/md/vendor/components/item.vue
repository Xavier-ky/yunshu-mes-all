<template>
  <div class="app-container">
    <el-table v-loading="loading" :data="itemList">
      <el-table-column label="物料编码" align="center" width="120px" prop="itemCode">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:md:mditem:edit']"
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
import {getItem} from "@/yunshu-ui/api/mes/wm/itemrecpt";

export default {
  name: "Bom",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 总条数
      total: 0,
      // 物料数据
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
    vendorId: undefined
  },
  created() {
    this.getList(this.vendorId);
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
    getList(vendorId) {
      this.loading = true;
      getItem(vendorId).then(response => {
        this.itemList = response.data;
        this.loading = false;
      });
    },
    /** 点击跳转按钮 */
    handleSkip(row) {
      this.$emit('handleSkip', row)
    },
  }
};
</script>
