<template>
  <div class="app-container outbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="packageCode">
          <el-input v-model="queryParams.packageCode" placeholder="装箱单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="barcodeContent">
          <el-input v-model="queryParams.barcodeContent" placeholder="条码内容" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="soCode">
          <el-input v-model="queryParams.soCode" placeholder="销售订单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientName">
          <el-input v-model="queryParams.clientName" placeholder="客户名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="inspectorName">
          <el-input v-model="queryParams.inspectorName" placeholder="检查员名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">装箱管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:package:add']">新增</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table"
        stripe
        border
        height="100%"
        :data="packageList"
        row-key="packageId"
        default-expand-all
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
        @selection-change="handleSelectionChange"
      >
        <el-table-column label="装箱单编号" align="center" header-align="center" min-width="150" prop="packageCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.packageCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="装箱日期" align="center" header-align="center" prop="packageDate" width="110">
          <template #default="scope">
            <span>{{ parseTime(scope.row.packageDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售订单" align="center" header-align="center" min-width="110" prop="soCode" show-overflow-tooltip />
        <el-table-column label="发票编号" align="center" header-align="center" min-width="110" prop="invoiceCode" show-overflow-tooltip />
        <el-table-column label="客户名称" align="center" header-align="center" min-width="120" show-overflow-tooltip>
          <template #default="scope">
            <span :title="scope.row.clientCode ? `编码：${scope.row.clientCode}` : ''">{{ scope.row.clientName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="箱尺寸" align="center" header-align="center" min-width="140" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ formatDim(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="重量" align="center" header-align="center" min-width="130" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ formatWeight(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检查员" align="center" header-align="center" min-width="90" prop="inspectorName" show-overflow-tooltip />
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
                v-hasPermi="['mes:wm:package:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:package:edit']"
                v-if="scope.row.status =='PREPARE'"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:package:remove']"
                v-if="scope.row.status =='PREPARE'"
              >删除</el-button>
            </div>
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

    <!-- 添加或修改装箱单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-row>
              <el-col :span="16">
                <el-form-item label="装箱单编号" prop="packageCode">
                  <el-input v-model="form.packageCode" placeholder="请输入装箱单编号" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item  label-width="80">
                  <el-switch v-model="autoGenFlag"
                      active-color="#13ce66"
                      active-text="自动生成"
                      @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'">
                  </el-switch>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="装箱日期" prop="packageDate">
                  <el-date-picker clearable
                    v-model="form.packageDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择装箱日期">
                  </el-date-picker>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="17">
                <el-form-item label="销售订单编号" prop="soCode">
                  <el-input v-model="form.soCode" placeholder="请输入销售订单编号" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="12">
            <el-image class="barcodeClass" fit="scale-down" :src="form.barcodeUrl" :preview-src-list="form.barcodeUrl ? [form.barcodeUrl] : []">
              <template #error><div class="image-slot">
                <i class="el-icon-picture-outline"></i>
              </div></template>
            </el-image>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" placeholder="请输入客户编码" >
                <template #append><el-button @click="handleSelectClient" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <ClientSelect ref="clientSelect" @onSelected="onClientSelected" > </ClientSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发票编号" prop="invoiceCode">
              <el-input v-model="form.invoiceCode" placeholder="请输入发票编号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="箱长度" prop="packageLength">
              <el-input v-model="form.packageLength" :max="999999999" placeholder="请输入箱长度" class="input-with-select">
                <template #append>
                  <el-select class="width" v-model="form.sizeUnit">
                    <el-option
                        v-for="item in measureOptions"
                        :key="item.measureCode"
                        :label="item.measureName"
                        :value="item.measureCode"
                      ></el-option>
                  </el-select>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="箱宽度" prop="packageWidth">
              <el-input v-model="form.packageWidth" placeholder="请输入箱宽度" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="箱高度" prop="packageHeight">
              <el-input v-model="form.packageHeight" placeholder="请输入箱高度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="净重" prop="netWeight">
              <el-input v-model="form.netWeight" placeholder="请输入净重" >
                <template #append>
                  <el-select v-model="form.weightUnit">
                    <el-option
                        v-for="item in measureOptions"
                        :key="item.measureCode"
                        :label="item.measureName"
                        :value="item.measureCode"
                      ></el-option>
                  </el-select>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="毛重" prop="crossWeight">
              <el-input v-model="form.crossWeight" placeholder="请输入毛重" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检查员名称" prop="inspectorName">
              <el-input v-model="form.inspectorName" placeholder="请输入检查员名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <el-tabs type="border-card" v-if="form.packageId !=null">
        <el-tab-pane label="子箱">
          <SubPackage ref="subPackge" :parentId="form.packageId" :optType="optType"></SubPackage>
        </el-tab-pane>
        <el-tab-pane label="装箱清单">
          <Packageline ref="packageLine" :packageId="form.packageId" :optType="optType"></Packageline>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="success" @click="doconfirm" v-if="form.status =='PREPARE' && optType !='view' && form.packageId !=null">完成</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listPackage, getPackage, delPackage, addPackage, updatePackage } from "@/yunshu-ui/api/mes/wm/package";
import ClientSelect from "@/yunshu-ui/components/clientSelect/single.vue";
import Packageline from "./line.vue";
import SubPackage from "./subpackge.vue";
import { listAllUnitmeasure} from "@/yunshu-ui/api/mes/md/unitmeasure";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";
export default {
  name: "Package",
  dicts: ['mes_order_status'],
  components: {
    ClientSelect,Packageline,SubPackage,Treeselect
  },
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
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
      // 装箱单表格数据
      packageList: [],
      //单位数据
      measureOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        parentId: null,
        ancestors: null,
        packageCode: null,
        barcodeId: null,
        barcodeContent: null,
        barcodeUrl: null,
        packageDate: null,
        soCode: null,
        invoiceCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        packageLength: null,
        packageWidth: null,
        packageHeight: null,
        sizeUnit: null,
        netWeight: null,
        crossWeight: null,
        weightUnit: null,
        inspector: null,
        inspectorName: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        packageCode: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        soCode: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        invoiceCode: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        inspectorName: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        parentId: [
          { required: true, message: "父箱ID不能为空", trigger: "blur" }
        ],
        ancestors: [
          { required: true, message: "所有父节点ID不能为空", trigger: "blur" }
        ],
        packageDate: [
          { required: true, message: "装箱日期不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getUnits();
  },
  methods: {
    formatDim(row) {
      const l = row.packageLength;
      const w = row.packageWidth;
      const h = row.packageHeight;
      if (l == null && w == null && h == null) return '—';
      const unit = row.sizeUnit ? ` ${row.sizeUnit}` : '';
      return `${l ?? '—'}×${w ?? '—'}×${h ?? '—'}${unit}`;
    },
    formatWeight(row) {
      const net = row.netWeight;
      const gross = row.crossWeight;
      if (net == null && gross == null) return '—';
      const unit = row.weightUnit ? ` ${row.weightUnit}` : '';
      return `${net ?? '—'}/${gross ?? '—'}${unit}`;
    },
    /** 查询装箱单列表 */
    getList() {
      this.loading = true;
      listPackage(this.queryParams).then(response => {
        this.packageList =this.handleTree(response.rows, "packageId", "parentId");
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 转换生产工单数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.packageId,
        label: node.packageCode,
        children: node.children
      };
    },
    //获取单位
    getUnits(){
      listAllUnitmeasure().then(response =>{
        this.measureOptions = response.data;
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
        packageId: null,
        parentId: null,
        ancestors: null,
        packageCode: null,
        barcodeId: null,
        barcodeContent: null,
        barcodeUrl: null,
        packageDate: new Date(),
        soCode: null,
        invoiceCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        packageLength: null,
        packageWidth: null,
        packageHeight: null,
        sizeUnit: null,
        netWeight: null,
        crossWeight: null,
        weightUnit: null,
        inspector: null,
        inspectorName: null,
        enableFlag: "Y",
        status: 'PREPARE',
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      };
      this.autoGenFlag = false;
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
      this.ids = selection.map(item => item.packageId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    //选择客户
    handleSelectClient(){
      this.$refs.clientSelect.handleOpen(this.form.clientId)
    },
    //客户选择弹出框
    onClientSelected(obj){
        if(obj != undefined && obj != null){
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
        }
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加装箱单";
      this.optType="add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const packageIds = row.packageId
      getPackage(packageIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看装箱单信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const packageId = row.packageId || this.ids
      getPackage(packageId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改装箱单";
        this.optType="edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.packageId != null) {
            updatePackage(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPackage(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    //完成
    doconfirm(){
      let that = this;
      this.$modal.confirm('是否完成装箱单编制？【完成后将不能更改】').then(function(){
        that.form.status = 'FINISHED';
        that.submitForm();
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const packageIds = row.packageId || this.ids;
      this.$modal.confirm('是否确认删除装箱单编号为"' + packageIds + '"的数据项？').then(function() {
        return delPackage(packageIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/package/export', {
        ...this.queryParams
      }, `package_${new Date().getTime()}.xlsx`)
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('PACKAGE_CODE').then(response =>{
          this.form.packageCode = response;
        });
      }else{
        this.form.packageCode = null;
      }
    }
  }
};
</script>
<style scoped>
  .barcodeClass {
    width: 200px;
    height: 200px;
  }

</style>
