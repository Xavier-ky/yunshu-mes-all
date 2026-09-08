<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="产品物料编码" prop="itemCode">
        <el-input
          v-model="queryParams.itemCode"
          placeholder="请输入产品物料编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="产品物料名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入产品物料名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" v-if="optType !='view'" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['mes:pro:proroute:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="default"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:pro:proroute:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:pro:proroute:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="default"
          @click="handleExport"
          v-hasPermi="['mes:pro:proroute:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="routeproductList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="产品物料编码" width="120px" align="center" prop="itemCode" >
        <template #default="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:pro:proroute:query']"
          >{{scope.row.itemCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="产品物料名称" width="150px" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="操作" align="center" v-if="optType !='view'" width="100px" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:pro:proroute:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:pro:proroute:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改产品制程对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
            <el-col :span="8">
                <el-form-item label="产品物料编码" prop="itemCode">
                    <el-input v-model="form.itemCode" placeholder="请选择产品" >
                        <template #append><el-button @click="handleSelectProduct" icon="el-icon-search"></el-button></template>
                    </el-input>
                    <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
                </el-form-item>
            </el-col>
            <el-col :span="8">
                <el-form-item label="产品物料名称" prop="itemName">
                    <el-input v-model="form.itemName" placeholder="请选择产品" disabled/>
                </el-form-item>
            </el-col>
            <el-col :span="8">
                <el-form-item label="单位" prop="unitName">
                    <el-input v-model="form.unitName" placeholder="请选择产品" disabled/>
                </el-form-item>
            </el-col>
        </el-row>
        <el-row>
            <el-col :span="24">
                <el-form-item label="规格型号" prop="specification">
                    <el-input v-model="form.specification" type="textarea" placeholder="请选择产品" disabled/>
                </el-form-item>
            </el-col>
        </el-row>
        <!-- <el-row>
            <el-col :span="8">
                <el-form-item label="生产数量" prop="quantity">
                    <el-input-number :min="1" v-model="form.quantity" placeholder="请输入生产数量" />
                </el-form-item>
            </el-col>
            <el-col :span="8">
                <el-form-item label="生产用时" prop="productionTime">
                    <el-input-number :min="1" :step="1" v-model="form.productionTime" placeholder="请输入生产用时" />
                </el-form-item>
            </el-col>
            <el-col :span="8">
                <el-form-item label="时间单位" prop="timeUnitType">
                    <el-select v-model="form.timeUnitType" placeholder="请选择时间单位">
                        <el-option
                        v-for="dict in dict.type.mes_time_type"
                        :key="dict.value"
                        :label="dict.label"
                        :value="dict.value"
                        ></el-option>
                    </el-select>
                </el-form-item>
            </el-col>
        </el-row> -->
        <el-row>
            <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
                </el-form-item>
            </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card" v-if="form.recordId != null">
        <el-tab-pane v-for="(process,index) in processList" :key="index" :label="process.processName">
          <Routeproductbom :routeId="form.routeId" :productId="form.itemId" :processId="process.processId" :optType="optType"></Routeproductbom>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view'">返回</el-button>
        <el-button type="primary" @click="submitForm" v-else>确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRouteproduct, getRouteproduct, delRouteproduct, addRouteproduct, updateRouteproduct, moveRouteproduct } from "@/yunshu-ui/api/mes/pro/routeproduct";
import { listRouteprocess} from "@/yunshu-ui/api/mes/pro/routeprocess";
import Routeproductbom from "./routeproductbom.vue"
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
export default {
  name: "Routeproduct",
  dicts: ['mes_time_type'],
  components: {ItemSelect,Routeproductbom},
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 产品制程表格数据
      routeproductList: [],
      //当前工艺中配置的工序清单
      processList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        routeId: this.routeId,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantity: null,
        productionTime: null,
        timeUnitType: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        itemId: [
          { required: true, message: "产品物料不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ]
      }
    };
  },
  props :{
      routeId: undefined,
      optType: undefined
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询产品制程列表 */
    getList() {
      this.loading = true;
      listRouteproduct(this.queryParams).then(response => {
        this.routeproductList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    //查询当前配置的工序
    getProcess(){
      let param = {
        routeId: this.form.routeId
      };
      listRouteprocess(param).then(response => {
        this.processList = response.rows;
      });
    },
    //物料选择弹出框
    handleSelectProduct(){
      this.$refs.itemSelect.showFlag = true;
    },
    //物料选择弹出框
    onItemSelected(obj){
        if(obj != undefined && obj != null){
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName
        }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        recordId: null,
        routeId: this.routeId,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantity: null,
        productionTime: null,
        timeUnitType: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.getProcess();//重新获取一次工序的配置
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加产品制程";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const recordId = row.recordId || this.ids;
      getRouteproduct(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看工艺线路信息";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getRouteproduct(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品制程";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateRouteproduct(this.form).then(response => {
                this.$modal.msgSuccess("修改成功");
                this.open = false;
                this.getList();
            });
          } else {
            addRouteproduct(this.form).then(response => {
                this.$modal.msgSuccess("新增成功");
                this.open = false;
                this.getList();
            });


                // this.$confirm('此产品已经配置过工艺路线，是否切换为当前工艺路线？', '提示', {
                //         confirmButtonText: '确定',
                //         cancelButtonText: '取消',
                //         type: 'warning'
                //         }).then(
                //             moveRouteproduct(this.form).then(response =>{
                //                 this.$modal.msgSuccess("切换成功");
                //                 this.open = false;
                //                 this.getList();
                //             })
                //         ).catch(
                //             () => {
                //                 this.$message({
                //                     type: 'info',
                //                     message: '已取消'
                //                 });
                //         });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除产品制程编号为"' + recordIds + '"的数据项？').then(function() {
        return delRouteproduct(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/pro/routeproduct/export', {
        ...this.queryParams
      }, `routeproduct_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
