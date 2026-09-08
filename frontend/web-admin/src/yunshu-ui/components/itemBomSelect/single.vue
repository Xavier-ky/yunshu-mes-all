<template>
  <el-dialog title="产品BOM物料选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
  >
    <el-table v-loading="loading" :data="bomList" @current-change="handleCurrent" @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedItemId" :label="scope.row.bomItemId" @change="handleRowChange(scope.row)">{{""}}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="物料编码" align="center" prop="bomItemCode" />
      <el-table-column label="物料名称" align="center" prop="bomItemName" :show-overflow-tooltip="true" />
      <el-table-column label="规格" align="center" prop="bomItemSpec" :show-overflow-tooltip="true" />
      <el-table-column label="单位" width="60px" align="center" prop="unitName" />
      <el-table-column label="使用比例" width="90px" align="center" prop="quantity" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />
    <template #footer><div class="dialog-footer">
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import { listBom } from "@/yunshu-ui/api/mes/md/bom";

export default {
  name: "ItemBomSelectSingle",
  props: {
    itemId: undefined
  },
  data() {
    return {
      showFlag: false,
      loading: true,
      selectedItemId: undefined,
      selectedRows: undefined,
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      bomList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemId: this.itemId,
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listBom(this.queryParams).then(response => {
        this.bomList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      });
    },
    handleCurrent(row) {
      if (row) this.selectedRows = row;
    },
    handleRowDbClick(row) {
      if (row) {
        this.selectedRows = row;
        this.$emit('onSelected', this.selectedRows);
        this.showFlag = false;
      }
    },
    handleRowChange(row) {
      if (row) this.selectedRows = row;
    },
    confirmSelect() {
      if (this.selectedItemId == null || this.selectedItemId === 0) {
        this.$notify({ title: '提示', type: 'warning', message: '请至少选择一条数据!' });
        return;
      }
      this.$emit('onSelected', this.selectedRows);
      this.showFlag = false;
    }
  }
};
</script>
