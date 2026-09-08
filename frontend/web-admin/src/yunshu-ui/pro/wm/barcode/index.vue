<template>
  <div class="app-container outbound-doc-panel barcode-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="barcodeType">
          <el-select v-model="queryParams.barcodeType" placeholder="条码类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_barcode_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item prop="bussinessCode">
          <el-input v-model="queryParams.bussinessCode" placeholder="业务编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="bussinessName">
          <el-input v-model="queryParams.bussinessName" placeholder="业务名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">条码管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:barcode:add']">新增</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:barcode:remove']">删除</el-button>
        <el-button type="success" plain icon="el-icon-s-tools" size="default" @click="handleConfig" v-hasPermi="['mes:wm:barcodeconfig:list']">条码设置</el-button>
        <el-button type="primary" plain icon="el-icon-printer" size="default" @click="handleBatchPrint" v-hasPermi="['mes:wm:barcode:view']">批量打印</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
      </div>
    </div>

    <Barcodeconfig v-if="checkPermission(['mes:wm:barcodeconfig:list'])" ref="barcodeconfig"></Barcodeconfig>
    <BarcodeBatchPrint ref="barcodebatchprint"></BarcodeBatchPrint>

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
        :data="barcodeList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="条码" align="center" header-align="center" width="148">
          <template #default="scope">
            <el-image
              class="barcode-thumb"
              fit="contain"
              :src="scope.row.barcodeUrl"
              @click="handleView(scope.row)"
            >
              <template #error>
                <div class="barcode-thumb-empty">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="条码格式" align="center" header-align="center" width="100" prop="barcodeFormart">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_barcode_formart" :value="scope.row.barcodeFormart"/>
          </template>
        </el-table-column>
        <el-table-column label="条码类型" align="center" header-align="center" width="100" prop="barcodeType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_barcode_type" :value="scope.row.barcodeType"/>
          </template>
        </el-table-column>
        <el-table-column label="条码内容" align="center" header-align="center" min-width="140" prop="barcodeContent" show-overflow-tooltip />
        <el-table-column label="业务编码" align="center" header-align="center" min-width="120" prop="bussinessCode" show-overflow-tooltip />
        <el-table-column label="业务名称" align="center" header-align="center" min-width="120" prop="bussinessName" show-overflow-tooltip />
        <el-table-column label="生效" align="center" header-align="center" width="80" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
                v-hasPermi="['mes:wm:barcode:view']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:barcode:edit']"
              >编辑</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:barcode:remove']"
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

    <!-- 添加或修改条码清单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="条码格式" prop="barcodeFormart">
              <el-select v-model="form.barcodeFormart" placeholder="请选择条码格式">
                <el-option
                  v-for="dict in dict.type.mes_barcode_formart"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="条码类型" prop="barcodeType">
              <el-select v-model="form.barcodeType" placeholder="请选择条码类型">
                <el-option
                  v-for="dict in dict.type.mes_barcode_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否启用">
              <el-radio-group v-model="form.enableFlag" disabled v-if="optType=='view'">
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
              <el-radio-group v-model="form.enableFlag" v-else>
                <el-radio
                  v-for="dict in dict.type.sys_yes_no"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <!--根据不同的条码类型展示不同的业务内容选择 start-->
        <!--物料产品-->
        <el-row v-if="form.barcodeType=='ITEM'">
          <el-col :span="12">
            <el-form-item label="物料编码"  prop="bussinessCode">
              <el-input v-model="form.bussinessCode" readonly="readonly" placeholder="请选择物料编码" >
                <template #append><el-button @click="handleSelectProduct" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <!--储位-->
        <el-row v-if="form.barcodeType=='AREA'">
          <el-col :span="24">
            <el-form-item label="储位" prop="warehouseId">
              <el-cascader v-model="warehouseInfo"
                ref="warehouseRef"
                :options="warehouseOptions"
                :props="warehouseProps"
                @change="handleWarehouseChanged"
              >
              </el-cascader>
            </el-form-item>
          </el-col>
        </el-row>
        <!--包装码-->
        <el-row v-if="form.barcodeType=='PACKAGE'">
          <el-col :span="12">
            <el-form-item label="装箱单号"  prop="packageCode">
              <el-input v-model="form.bussinessCode" readonly="readonly" placeholder="请选择装箱单" >
                <template #append><el-button @click="handleSelectPackage" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <PackageSelectSingle ref="packageSelect" :status="'FINISHED'" @onSelected="onPackageSelected"></PackageSelectSingle>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <!--供应商-->
        <el-row v-else-if="form.barcodeType=='VENDOR'">
          <el-col :span="12">
            <el-form-item label="供应商编号" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" readonly="readonly" placeholder="请选择供应商" >
                <template #append><el-button @click="handleSelectVendor" icon="el-icon-search"></el-button></template>
              </el-input>
              <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 工作站 -->
        <el-row v-else-if="form.barcodeType=='WORKSTATION'">
          <el-col :span="12">
            <el-form-item label="工作站" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择工作站" >
                <template #append><el-button icon="el-icon-search" @click="handleWorkstationSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelect"  @onSelected="onWorkstationSelected"> </WorkstationSelect>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工作站名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 库存 -->
        <el-row v-else-if="form.barcodeType=='STOCK'">
          <el-col :span="8">
            <el-form-item label="物资编码" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择库存记录" >
                <template #append><el-button icon="el-icon-search" @click="handleMaterialStockSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <StockSelect ref="stockSelect"  @onSelected="onMaterialStockSelected"> </StockSelect>
          </el-col>
          <el-col :span="16">
            <el-form-item label="物资信息" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 批次 -->
        <el-row v-else-if="form.barcodeType=='BATCH'">
          <el-col :span="8">
            <el-form-item label="批次编号" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择批次" >
                <template #append><el-button icon="el-icon-search" @click="handleBatchSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <BatchSelect ref="batchSelect" @onSelected="onBatchSelected" ></BatchSelect>
          </el-col>
          <el-col :span="16">
            <el-form-item label="批次内容" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 车间 -->
        <el-row v-else-if="form.barcodeType=='WORKSHOP'">
          <el-col :span="8">
            <el-form-item label="车间编号" prop="bussinessId">
              <el-select v-model="form.workshopId" @change="changeWorkshop" placeholder="请选择车间">
                    <el-option
                        v-for="item in workshopOptions"
                        :key="item.workshopId"
                        :label="item.workshopName"
                        :value="item.workshopId"
                    ></el-option>
                  </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="车间名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 设备 -->
        <el-row v-else-if="form.barcodeType=='MACHINERY'">
          <el-col :span="8">
            <el-form-item label="设备编号" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择设备" >
                <template #append><el-button icon="el-icon-search" @click="handleMachinerySelect"></el-button></template>
              </el-input>
            </el-form-item>
            <MachinerySelect ref="machinerySelect" @onSelected="onMachinerySelect"></MachinerySelect>
          </el-col>
          <el-col :span="16">
            <el-form-item label="设备名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 人员 -->
        <el-row v-else-if="form.barcodeType=='USER'">
          <el-col :span="8">
            <el-form-item label="人员编号" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择人员" >
                <template #append><el-button icon="el-icon-search" @click="handleUserSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected"></UserSingleSelect>
          </el-col>
          <el-col :span="16">
            <el-form-item label="人员名称" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 流转卡 -->
        <el-row v-else-if="form.barcodeType=='PROCARD'">
          <el-col :span="8">
            <el-form-item label="流转卡编号" prop="bussinessCode">
              <el-input v-model="form.bussinessCode" placeholder="请选择流转卡" >
                <template #append><el-button icon="el-icon-search" @click="handleProcardSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <ProcardSelect ref="procardSelect" @onSelected="onProcardSelected"></ProcardSelect>
          </el-col>
          <el-col :span="16">
            <el-form-item label="工单号" prop="bussinessName">
              <el-input v-model="form.bussinessName" readonly="readonly" ></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <!--根据不同的条码类型展示不同的业务内容选择 end-->
        <el-row>
          <el-col :span="24">
            <el-form-item label="条码内容" prop="barcodeContent">
              <el-input v-model="form.barcodeContent"  placeholder="请填写条码内容"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="条码" prop="url">
              <el-image class="barcodeFormClass" :src="form.barcodeUrl">
                <template #error><div class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div></template>
              </el-image>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view'">返回</el-button>
        <el-button type="primary" @click="submitForm" v-if="optType !='view' ">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listBarcode, getBarcode, delBarcode, addBarcode, updateBarcode } from "@/yunshu-ui/api/mes/wm/barcode";
import { listAllWorkshop } from "@/yunshu-ui/api/mes/md/workshop";
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import PackageSelectSingle from "@/yunshu-ui/components/package/single.vue";
import WorkstationSelect from "@/yunshu-ui/components/workstationSelect/simpletableSingle.vue"
import StockSelect from "@/yunshu-ui/components/stockSelect/single.vue"
import BatchSelect from "@/yunshu-ui/components/batchSelect/single.vue";
import MachinerySelect from "@/yunshu-ui/components/machinerySelect/single.vue";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue"
import ProcardSelect from "@/yunshu-ui/components/procardSelect/single.vue";
import Barcodeconfig from "./config.vue"
import BarcodeBatchPrint from "./batchprint.vue"
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
export default {
  name: "Barcode",
  dicts: ['mes_barcode_type','mes_barcode_formart','sys_yes_no'],
  components: {
    ItemSelect,VendorSelect,PackageSelectSingle,Barcodeconfig,WorkstationSelect,StockSelect,BatchSelect,MachinerySelect,
    UserSingleSelect,ProcardSelect,BarcodeBatchPrint
  },
  data() {
    return {
      warehouseInfo:[],
      warehouseOptions:[],
      warehouseProps:{
        multiple: false,
        value: 'pId',
        label: 'pName',
      },
      //车间选项
      workshopOptions:[],
      optType: null,
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
      // 条码清单表格数据
      barcodeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        barcodeFormart: null,
        barcodeType: null,
        barcodeContent: null,
        bussinessId: null,
        bussinessCode: null,
        bussinessName: null,
        barcodeUrl: null,
        enableFlag: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        barcodeFormart: [
          { required: true, message: "条码格式不能为空", trigger: "blur" }
        ],
        barcodeType: [
          { required: true, message: "条码类型不能为空", trigger: "change" }
        ],
        barcodeContent: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" },
          { max: 100, message: '字符过长', trigger: 'blur' }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getWarehouseList();
    this.getWorkshops();
  },
  methods: {
    /** 查询条码清单列表 */
    getList() {
      this.loading = true;
      listBarcode(this.queryParams).then(response => {
        this.barcodeList = response.rows || [];
        this.total = response.total || 0;
      }).catch(() => {
        this.barcodeList = [];
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
        barcodeId: null,
        barcodeFormart: 'QR_CODE',
        barcodeType: null,
        barcodeContent: null,
        bussinessId: null,
        bussinessCode: null,
        bussinessName: null,
        barcodeUrl: null,
        enableFlag: 'Y',
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
      this.ids = selection.map(item => item.barcodeId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加条码";
      this.optType = "add";
    },
    handleView(row){
      this.reset();
      const barcodeId = row.barcodeId
      getBarcode(barcodeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看条码";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const barcodeId = row.barcodeId || this.ids
      getBarcode(barcodeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改条码";
        this.optType = "edit";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.barcodeId != null) {
            updateBarcode(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addBarcode(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const barcodeIds = row.barcodeId || this.ids;
      this.$modal.confirm('是否确认删除条码清单编号为"' + barcodeIds + '"的数据项？').then(function() {
        return delBarcode(barcodeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/barcode/export', {
        ...this.queryParams
      }, `barcode_${new Date().getTime()}.xlsx`)
    },
    //物料选择
    handleSelectProduct(){
      this.$refs.itemSelect.showFlag = true;
    },
    //物料选择弹出框
    onItemSelected(obj){
        if(obj != undefined && obj != null){
          this.form.bussinessId = obj.itemId;
          this.form.bussinessCode = obj.itemCode;
          this.form.bussinessName = obj.itemName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },
    //选择仓库、库区、库位
    getWarehouseList(){
      getTreeList().then(response => {
        const raw = response.data;
        if (!raw || !Array.isArray(raw)) {
          this.warehouseOptions = [];
          return;
        }
        this.warehouseOptions = raw.filter((el) => el.warehouseCode && el.warehouseCode.indexOf('VIR') === -1);
        this.warehouseOptions.forEach(w => {
          if (!Array.isArray(w.children)) return;
          w.children.forEach(l => {
            if (!Array.isArray(l.children)) return;
            const lstr = JSON.stringify(l.children).replace(/locationId/g,'lId').replace(/areaId/g, 'pId').replace(/areaName/g,'pName');
            l.children = JSON.parse(lstr);
          });
          const wstr = JSON.stringify(w.children).replace(/warehouseId/g,'wId').replace(/locationId/g, 'pId').replace(/locationName/g,'pName');
          w.children = JSON.parse(wstr);
        });
        const ostr = JSON.stringify(this.warehouseOptions).replace(/warehouseId/g,'pId').replace(/warehouseName/g, 'pName');
        this.warehouseOptions = JSON.parse(ostr);
      }).catch(() => {
        this.warehouseOptions = [];
      });
    },
    handleWarehouseChanged(obj){
      const checkedNode = this.$refs['warehouseRef'].getCheckedNodes();
      if(obj !=null){
        this.form.bussinessId = checkedNode[0].data.pId;
        this.form.bussinessCode = checkedNode[0].data.areaCode;
        this.form.bussinessName = checkedNode[0].data.pName;
        this.form.barcodeContent = "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
      }
    },
    //装箱单选择
    handleSelectPackage(){
      this.$refs.packageSelect.showFlag=true;
    },
    /**选择装箱单返回 */
    onPackageSelected(obj){
      if(obj != undefined && obj != null){
          this.form.bussinessId = obj.packageId;
          this.form.bussinessCode = obj.packageCode;
          this.form.bussinessName = obj.clientName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },
    /**选择工作站 */
    handleWorkstationSelect(){
      this.$refs.wsSelect.showFlag = true;
    },
    /**工作站选择返回 */
    onWorkstationSelected(obj){
      if(obj != undefined && obj != null){
          this.form.bussinessId = obj.workstationId;
          this.form.bussinessCode = obj.workstationCode;
          this.form.bussinessName = obj.workstationName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },

    /**
     * 选择库存
     */
     handleMaterialStockSelect(){
        this.$refs.stockSelect.showFlag = true;
     },

     /**库存选择返回 */
     onMaterialStockSelected(obj){
      if(obj != undefined && obj != null){
          this.form.bussinessId = obj.materialStockId;
          this.form.bussinessCode = obj.itemCode;
          this.form.bussinessName = "".concat(obj.itemName,'|',obj.specification,'|',obj.vendorName,'|',obj.batchCode);
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessId);
        }
     },
     handleProcardSelect(){
       this.$refs.procardSelect.showFlag = true;
     },
     onProcardSelected(obj){
        if(obj != undefined && obj != null){
          this.form.bussinessId = obj.cardId;
          this.form.bussinessCode = obj.cardCode;
          this.form.bussinessName = obj.workorderName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
      },
     /**批次选择  */
     handleBatchSelect(){
       this.$refs.batchSelect.showFlag = true;
     },
     onBatchSelected(obj){
        if(obj != undefined && obj != null){
          this.form.bussinessId = obj.batchId;
          this.form.bussinessCode = obj.batchCode;
          this.form.bussinessName = "".concat(obj.itemName,'|',obj.batchCode);
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },
    getWorkshops(){
      listAllWorkshop().then( response => {
        this.workshopOptions =response.data;
      });
    },
    //车间选择
    changeWorkshop(val) {
      const workshop = this.workshopOptions.filter(item => item.workshopId == val)
      this.form.bussinessId = workshop[0].workshopId
      this.form.bussinessName = workshop[0].workshopName
      this.form.bussinessCode = workshop[0].workshopCode
      this.form.barcodeContent = "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
    },
    handleMachinerySelect(){
      this.$refs.machinerySelect.showFlag = true;
    },
    onMachinerySelect(obj){
      if(obj != undefined && obj != null){
          this.form.bussinessId = obj.machineryId;
          this.form.bussinessCode = obj.machineryCode;
          this.form.bussinessName = obj.machineryName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },
    //点击人员选择按钮
    handleUserSelect(){
      this.$refs.userSelect.showFlag = true;
    },
    //人员选择返回
    onUserSelected(obj){
      if(obj != undefined && obj != null){
          this.form.bussinessId = obj.userId;
          this.form.bussinessCode = obj.userName;
          this.form.bussinessName = obj.nickName;
          this.form.barcodeContent= "".concat(this.form.barcodeType,'-',this.form.bussinessCode);
        }
    },
    handleConfig(){
      this.$refs.barcodeconfig.showFlag = true;
    },

    handleBatchPrint(){
      this.$refs.barcodebatchprint.showFlag = true;
    }

  }
};
</script>
<style scoped>
.barcode-thumb {
  width: 120px;
  height: 120px;
  cursor: pointer;
  border-radius: 2px;
}
.barcode-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  color: #9aa8bc;
  background: #f4f7fb;
  font-size: 20px;
}
.barcodeFormClass {
  width: 200px;
  height: 200px;
}
</style>
