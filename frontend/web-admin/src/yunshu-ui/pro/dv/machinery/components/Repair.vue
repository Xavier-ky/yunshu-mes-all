<template>
  <div class="app-container">
    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="repairList">
      <el-table-column label="维修单编号" width="120px" align="center" prop="repairCode" />
      <el-table-column label="维修单名称" width="150px" align="center" prop="repairName" :show-overflow-tooltip="true"/>
      <el-table-column label="报修日期" align="center" prop="requireDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.requireDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维修完成日期" align="center" prop="finishDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.finishDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="验收日期" align="center" prop="confirmDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.confirmDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="维修结果" align="center" prop="repairResult">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_repair_result" :value="scope.row.repairResult"/>
        </template>
      </el-table-column>
      <el-table-column label="维修人员" align="center" prop="acceptedBy" />
      <el-table-column label="验收人员" align="center" prop="confirmBy" />
      <el-table-column label="单据状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
    </el-table>

    <!--    <pagination-->
    <!--      v-show="total>0"-->
    <!--      :total="total"-->
    <!--      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"-->
    <!--      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"-->
    <!--      @pagination="getList"-->
    <!--    />-->
  </div>
</template>

<script>
import {getRepairList} from "@/yunshu-ui/api/mes/dv/repair";

export default {
  name: "Repair",
  dicts: ['mes_repair_result', 'mes_order_status'],
  data() {
    return {
      // 遮罩层
      loading: false,
      // 总条数
      total: 0,
      // 点检 / 保养数据
      repairList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    };
  },
  methods: {
    getOpen(query) {
      this.queryParams.machineryCode = query.machineryCode
      this.getList()
    },
    getList() {
      this.loading = true;
      getRepairList(this.queryParams).then(response => {
        this.repairList = response.data;
        this.loading = false;
      });
    }
  }
};
</script>
