<template>
  <el-dialog title="班组选择"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
  >
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班组编号" prop="teamCode">
        <el-input v-model="queryParams.teamCode" placeholder="请输入班组编号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="班组名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="请输入班组名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="teamList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="班组编号" align="center" prop="teamCode" />
      <el-table-column label="班组名称" align="center" prop="teamName" />
      <el-table-column label="备注" align="center" prop="remark" />
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
import { listTeam } from "@/yunshu-ui/api/mes/cal/team";
export default {
  name: "TeamSelect",
  data() {
    return {
      showFlag: false,
      loading: true,
      ids: [],
      selectedRows: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      teamList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        teamCode: null,
        teamName: null,
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listTeam(this.queryParams).then(response => {
        this.teamList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.teamId);
      this.selectedRows = selection;
    },
    confirmSelect() {
      if (!this.selectedRows || this.selectedRows.length === 0) {
        this.$notify({ title: '提示', type: 'warning', message: '请至少选择一条数据!' });
        return;
      }
      this.$emit('onSelected', this.selectedRows);
      this.showFlag = false;
    }
  }
};
</script>
