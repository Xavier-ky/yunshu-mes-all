<template>
  <div class="app-container">

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="checkplanList">
      <el-table-column label="计划编码" align="center" prop="planCode" :show-overflow-tooltip="true"></el-table-column>
      <el-table-column label="计划名称" align="center" width="200px" prop="planName" :show-overflow-tooltip="true"/>
      <el-table-column label="开始日期" align="center" prop="startDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="频率" align="center" prop="cycleType">
        <template #default="scope">
          {{scope.row.cycleCount}}
          <dict-tag :options="dict.type.mes_cycle_type" :value="scope.row.cycleType"/>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
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

import {getCheckPlan} from "@/yunshu-ui/api/mes/dv/checkplan";

export default {
  name: "CheckPlan",
  dicts: ['mes_cycle_type','mes_order_status','dv_plan_type'],
  data() {
    return {
      // 遮罩层
      loading: false,
      // 总条数
      total: 0,
      // 点检 / 保养数据
      checkplanList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
      },
    };
  },

  created() {
  },
  methods: {
    getOpen(query) {
      this.queryParams.planType = query.planType
      this.queryParams.machineryCode = query.machineryCode
      this.getList()
    },
    getList() {
      this.loading = true;
      getCheckPlan(this.queryParams).then(response => {
        this.checkplanList = response.data;
        // this.checkplanList = response.rows;
        // this.total = response.total;
        this.loading = false;
      });
    }
  }
};
</script>
