<template>
  <el-dialog title="赋码设置"
    v-if="showFlag"
    v-model="showFlag"
    :modal="false"
    width="80%"
    center
    >
    <el-table v-loading="loading" :data="barcodeconfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="赋码类型" align="center" prop="barcodeType">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_barcode_type" :value="scope.row.barcodeType"/>
        </template>
      </el-table-column>
      <el-table-column label="赋码格式" align="center" prop="barcodeFormart">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_barcode_formart" :value="scope.row.barcodeFormart"/>
        </template>
      </el-table-column>
      <el-table-column label="内容格式" align="center" prop="contentFormart" />
      <el-table-column label="内容样例" align="center" prop="contentExample" />
      <el-table-column label="是否自动生成" align="center" prop="autoGenFlag">
        <template #default="scope">
              <el-switch
                v-model="scope.row.autoGenFlag"
                active-value="Y"
                inactive-value="N"
                @change="handleAutoGenChange(scope.row)"
              ></el-switch>
        </template>
      </el-table-column>
      <el-table-column label="默认打印模板" align="center" prop="defaultTemplate" >
        <template #default="scope">
          {{scope.row.defaultTemplate}}
          <el-button
            type="text"
            @click="handleSetReport(scope.row)"
          >设置</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />
    <ReportSelect v-if="checkPermission(['mes:wm:barcodeconfig:edit'])" ref="reportRef" @onSelected="onReportSelected"></ReportSelect>
    <template #footer><div class="dialog-footer">
      <el-button type="primary" v-hasPermi="['mes:wm:barcodeconfig:edit']" @click="showFlag=false">确 定</el-button>
      <el-button @click="showFlag=false">取 消</el-button>
    </div></template>
  </el-dialog>
</template>

<script>
import { listBarcodeconfig, getBarcodeconfig, delBarcodeconfig, addBarcodeconfig, updateBarcodeconfig } from "@/yunshu-ui/api/mes/wm/barcodeconfig";
import ReportSelect from "@/yunshu-ui/components/reportSelect/single.vue";
export default {
  name: "Barcodeconfig",
  components: {ReportSelect},
  dicts: ['sys_yes_no', 'mes_barcode_type','mes_barcode_formart'],
  data() {
    return {
      showFlag: false,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      //当前编辑的行
      selectedConfigId: null,
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 条码配置表格数据
      barcodeconfigList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        barcodeFormart: null,        barcodeType: null,        contentFormart: null,        contentExample: null,        autoGenFlag: null,        enableFlag: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        barcodeFormart: [
          { required: true, message: "条码格式不能为空", trigger: "blur" }
        ],        barcodeType: [
          { required: true, message: "条码类型不能为空", trigger: "change" }
        ],        contentFormart: [
          { required: true, message: "内容格式不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询条码配置列表 */
    getList() {
      this.loading = true;
      listBarcodeconfig(this.queryParams).then(response => {
        this.barcodeconfigList = response.rows || [];
        this.total = response.total || 0;
      }).catch(() => {
        this.barcodeconfigList = [];
        this.total = 0;
      }).finally(() => {
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        configId: null,        barcodeFormart: null,        barcodeType: null,        contentFormart: null,        contentExample: null,        autoGenFlag: "0",        enableFlag: "0",        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.ids = selection.map(item => item.configId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加条码配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const configId = row.configId || this.ids
      getBarcodeconfig(configId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改条码配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.configId != null) {
            updateBarcodeconfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addBarcodeconfig(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //弹出选择框
    handleSetReport(row){
      this.$refs.reportRef.showFlag = true;
      this.selectedConfigId = row.configId;
      this.form = row;
    },
    //报表选择返回
    onReportSelected(row){
      if(row != null && this.form !=null ){
        this.form.defaultTemplate = row.name;
        updateBarcodeconfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
      }
    },
    //赋码自动生成修改
    handleAutoGenChange(row) {
      let text = row.autoGenFlag === "Y" ? "启用" : "停用";
      this.$modal.confirm('确认要' + text + '自动生成吗？').then(function() {
            updateBarcodeconfig(row).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.autoGenFlag = row.autoGenFlag === "N" ? "Y" : "N";
      });
    }
  }
};
</script>
