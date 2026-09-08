<template>
    <div class="app-container workorders-command-center production-progress-command-center">
    <section v-show="showSearch" class="workorders-filter-panel">
            <el-form :model="queryParams" ref="queryForm" :inline="true" class="workorders-filter-form production-progress-filter-form" label-width="68px">
            <el-form-item label="工单编码" prop="workorderCode">
                <el-input
                v-model="queryParams.workorderCode"
                placeholder="请输入工单编码"
                clearable
                @keyup.enter="handleQuery"
                />
            </el-form-item>
            <el-form-item label="产品名称" prop="productName">
                <el-input
                v-model="queryParams.productName"
                placeholder="请输入产品名称"
                clearable
                @keyup.enter="handleQuery"
                />
            </el-form-item>
            <el-form-item class="filter-actions">
                <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
                <el-button class="progress-refresh-button" icon="el-icon-refresh" v-hasPermi="['mes:pro:protask:list']" @click="getList">刷新</el-button>
            </el-form-item>
            </el-form>  
    </section>
    <div class="workorders-table-frame production-progress-table-frame">
        <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
        <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
            <el-table
            v-loading="loading"
            class="yunshu-data-table workorders-table production-progress-table"
            stripe
            border
            :data="workorderList"
            row-key="workorderId"
            default-expand-all
            :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
            >
            <el-table-column label="工单编码" min-width="150" align="center" header-align="center" prop="workorderCode" show-overflow-tooltip>
                <template #default="scope">
                <el-button
                    size="small"
                    link
                    @click="handleView(scope.row)"
                    v-hasPermi="['mes:pro:protask:query']"
                >{{scope.row.workorderCode}}</el-button>
                </template>
            </el-table-column>
            <el-table-column label="工单名称" min-width="160" align="center" header-align="center" prop="workorderName" show-overflow-tooltip/>
            <el-table-column label="订单编号" min-width="130" align="center" header-align="center" prop="sourceCode" show-overflow-tooltip />
            <el-table-column label="产品编号" min-width="120" align="center" header-align="center" prop="productCode" show-overflow-tooltip />
            <el-table-column label="产品名称" min-width="160" align="center" header-align="center" prop="productName" show-overflow-tooltip/>
            <el-table-column label="工单数量" min-width="96" align="center" header-align="center" prop="quantity" />                 
            <el-table-column label="已生产数量" align="center" header-align="center" min-width="110" prop="quantityProduced" /> 
            <el-table-column label="工序进度" align="center" header-align="center" min-width="390">
            <template #default="scope">             
                <div class="progress-route-row">
                  <div v-for="(task,index) in scope.row.routeHomg || []" :key="task.processId" class="progress-route-cell">
                    <span v-if="index !== 0" class="progress-route-separator" aria-hidden="true">—</span>
                    <div class="progress-route-item">
                      <el-progress type="circle" :width="66" :stroke-width="9" :percentage="getPercentage(task)" />
                      <el-tooltip :content="task.processName" placement="bottom">
                        <span class="progress-route-name">{{ task.processName }}</span>
                      </el-tooltip>
                    </div>
                  </div>
                </div>                   
            </template>
            </el-table-column>
            <el-table-column label="需求日期" align="center" header-align="center" prop="requestDate" min-width="120">
                <template #default="scope">
                <span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="工单状态" align="center" header-align="center" prop="status" min-width="100">
                <template #default="scope">
                <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
                </template>
            </el-table-column>
            </el-table>
    </div>
            <pagination
            v-show="total>0"
            :total="total"
            :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
            @pagination="getList"
            />
    </div>
</template>
<script>
import { getHomeList, listWorkorder, listWithTaskJson } from "@/yunshu-ui/api/mes/pro/workorder";
export default {
  name: "processview",
  dicts: ['mes_order_status','mes_workorder_sourcetype'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      //选中的生产工单
      selectedWorkorderId: -1,
      // 生产工单表格数据
      workorderList: [],
      // 生产工单树选项
      workorderOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workorderCode: null,
        workorderName: null,
        workorderType: 'SELF', //这里的排产要排除自产之外的外协和外购
        orderSource: null,
        sourceCode: null,
        productId: null,
        productCode: null,
        productName: null,
        productSpc: null,
        unitOfMeasure: null,
        quantity: null,
        quantityProduced: null,
        quantityChanged: null,
        quantityScheduled: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        requestDate: null,
        parentId: null,
        ancestors: null,
        status: 'CONFIRMED',
      },
      tasks:{
        data: [],
        links: []
      },
      // 表单参数
      form: {},
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询生产工单列表 */
    getList() {
      this.loading = true;
      Promise.all([
        listWithTaskJson(this.queryParams),
        getHomeList(this.queryParams),
      ]).then(([response, progressResponse]) => {
        const progressByWorkorder = this.indexRouteProgress(progressResponse?.data || []);
        this.workorderList = this.attachRouteProgress(response.rows || [], progressByWorkorder);
        if(this.workorderList.length>0){
            this.selectedWorkorderId = this.workorderList[0].workorderId;
        }else{
            this.selectedWorkorderId = -1;
        }
        this.total = response.total;
        this.loading = false;
      }).catch(() => {
        this.workorderList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    indexRouteProgress(rows, progressByWorkorder = {}) {
      (rows || []).forEach((row) => {
        progressByWorkorder[row.workorderId] = row.routeHomg || [];
        this.indexRouteProgress(row.children, progressByWorkorder);
      });
      return progressByWorkorder;
    },
    attachRouteProgress(rows, progressByWorkorder) {
      return (rows || []).map((row) => ({
        ...row,
        routeHomg: progressByWorkorder[row.workorderId] || row.routeHomg || [],
        children: row.children
          ? this.attachRouteProgress(row.children, progressByWorkorder)
          : row.children,
      }));
    },
    /** 转换生产工单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.workorderId,
        label: node.workorderName,
        children: node.children
      };
    },
	/** 查询生产工单下拉树结构 */
    getTreeselect() {
      listWorkorder().then(response => {
        this.workorderOptions = [];
        const data = { workorderId: 0, workorderName: '顶级节点', children: [] };
        data.children = this.handleTree(response.data, "workorderId", "parentId");
        this.workorderOptions.push(data);
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    getPercentage(task){
      const total = Number(task.total) || 0;
      const complete = Number(task.completeNumber) || 0;
      if (total <= 0) return 0;
      return Math.min(100, parseFloat(((complete / total) * 100).toFixed(0)));
    },
    handleView(row) {
      this.$router.push({ path: "/app/planning/work-orders", query: { workorderId: row.workorderId } });
    }
  }
};
</script>