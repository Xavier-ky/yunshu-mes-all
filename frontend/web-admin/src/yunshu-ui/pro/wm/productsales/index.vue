<template>
  <div class="app-container outbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="salesCode">
          <el-input v-model="queryParams.salesCode" placeholder="出库单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientName">
          <el-input v-model="queryParams.clientName" placeholder="客户" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="单据状态" clearable>
            <el-option
              v-for="dict in dict.type.mes_product_sales_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">销售出库</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:productsales:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:productsales:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:productsales:remove']">删除</el-button>
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
        :data="productsalesList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="出库单编号" align="center" header-align="center" min-width="130" prop="salesCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.salesCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="出库单名称" align="center" header-align="center" min-width="120" prop="salesName" show-overflow-tooltip />
        <el-table-column label="销售订单" align="center" header-align="center" min-width="110" prop="soCode" show-overflow-tooltip />
        <el-table-column label="客户" align="center" header-align="center" min-width="110" prop="clientName" show-overflow-tooltip />
        <el-table-column label="出库日期" align="center" header-align="center" prop="salesDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.salesDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_product_sales_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="300" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button
                size="small"
                link
                icon="el-icon-view"
                @click="handleView(scope.row)"
                v-hasPermi="['mes:wm:productsales:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >执行拣货</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleSubmitShipping(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >提交运单</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-truck"
                v-if="scope.row.status =='UNSHIPPING'"
                @click="handleShipping(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >填写运单</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='UNSHIPPING'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status == 'UNEXECUTE'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >执行出库</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:productsales:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:productsales:remove']"
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

    <!-- 添加或修改销售出库单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <!--草稿状态下可修改的部分-->
        <div v-if="form.status =='PREPARE'">
        <el-row>
          <el-col :span="8">
            <el-form-item label="出库单编号" prop="salesCode">
              <el-input v-model="form.salesCode" placeholder="请输入出库单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'">
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出库单名称" prop="salesName">
              <el-input v-model="form.salesName" placeholder="请输入出库单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="发货通知单" prop="noticeCode">
              <el-input v-model="form.noticeCode" readonly="readonly" placeholder="请选择发货通知单" >
                <template #append><el-button @click="handleSelectNotice" icon="el-icon-search"></el-button></template>
              </el-input>
              <NoticeSelect ref="noticeSelect" @onSelected="onNoticeSelected"></NoticeSelect>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="soCode">
              <el-input v-model="form.soCode" placeholder="请输入销售订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库日期" prop="salesDate">
              <el-date-picker clearable
                v-model="form.salesDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出库日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" placeholder="请输入客户编码" >
                <template #append><el-button @click="handleSelectClient" icon="el-icon-search"></el-button></template>
              </el-input>
              <ClientSelect ref="clientSelect" @onSelected="onClientSelected" > </ClientSelect>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="收货地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入收货地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人" prop="recipient">
              <el-input v-model="form.recipient" placeholder="请输入收货人" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系方式" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入联系方式" />
            </el-form-item>
          </el-col>
        </el-row>
        </div>
        <div v-else>
          <el-row>
          <el-col :span="8">
            <el-form-item label="出库单编号" prop="salesCode">
              <el-input v-model="form.salesCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="4">

          </el-col>
          <el-col :span="12">
            <el-form-item label="出库单名称" prop="salesName">
              <el-input v-model="form.salesName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="发货通知单" prop="noticeCode">
              <el-input v-model="form.noticeCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售订单编号" prop="soCode">
              <el-input v-model="form.soCode" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库日期" disabled prop="salesDate">
              <el-date-picker clearable
                v-model="form.salesDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择出库日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="收货地址" prop="address">
              <el-input v-model="form.address" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货人" prop="recipient">
              <el-input v-model="form.recipient" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系方式" prop="tel">
              <el-input v-model="form.tel" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        </div>
        <!--待填写运单信息状态下可修改的部分-->

        <el-row v-if="form.status =='UNSHIPPING'||form.status =='UNEXECUTE'||form.status =='FINISHED'">
          <el-col :span="8">
            <el-form-item label="承运商" prop="carrier">
              <el-input v-model="form.carrier" placeholder="请输入承运商" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运输单号" prop="shippingNumber">
              <el-input v-model="form.shippingNumber" placeholder="请输入运输单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">

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
      <el-divider v-if="form.salesId !=null" content-position="center">物料信息</el-divider>
        <el-card shadow="always" v-if="form.salesId !=null && form.status =='PREPARE' " class="box-card">
          <Productsalesline ref="line" :salesId="form.salesId" :noticeId="form.noticeId" :optType="optType"></Productsalesline>
        </el-card>
        <el-card shadow="always" v-if="form.salesId !=null && form.status !='PREPARE' " class="box-card">
          <ProductsalesDetail ref="line" :salesId="form.salesId" :optType="optType"></ProductsalesDetail>
        </el-card>
        <template #footer><div class="dialog-footer">
          <el-button type="primary" @click="saveForm" v-if="(form.status =='PREPARE' || form.status =='UNSHIPPING')&& optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToShipping" v-if="form.status =='UNSTOCK' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='UNSHIPPING' && form.salesId !=null && optType !='view' ">提 交</el-button>
          <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listProductsales, getProductsales, delProductsales, addProductsales, updateProductsales, checkQuantity, execute } from "@/yunshu-ui/api/mes/wm/productsales";
import Productsalesline from "./line.vue"
import ProductsalesDetail from "./detail.vue"
import NoticeSelect from "@/yunshu-ui/components/noticeSelect/salesNoticeSingle.vue"
import ClientSelect from "@/yunshu-ui/components/clientSelect/single.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Productsales",
  dicts: ['mes_product_sales_status'],
  components: {NoticeSelect,Productsalesline,ProductsalesDetail,ClientSelect},
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
      // 销售出库单表格数据
      productsalesList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        salesCode: null,
        salesName: null,
        oqcId: null,
        oqcCode: null,
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        salesDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        salesCode: [
          { required: true, message: "出库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        salesName: [
          { required: true, message: "出库单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        clientCode: [
          { required: true, message: "请指定客户", trigger: "blur" }
        ],
        salesDate: [
          { required: true, message: "请选择出库日期", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询销售出库单列表 */
    getList() {
      this.loading = true;
      listProductsales(this.queryParams).then(response => {
        this.productsalesList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 关闭按钮
    close() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        salesId: null,
        salesCode: null,
        salesName: null,
        noticeId: null,
        noticeCode: null,
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        recipient: null,
        tel: null,
        address: null,
        carrier: null,
        shippingNumber: null,
        salesDate: new Date(),
        status: "PREPARE",
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
      this.ids = selection.map(item => item.salesId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const salesId = row.salesId
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看出库单信息";
        this.optType = "view";
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加销售出库单";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改销售出库单";
        this.optType = "edit";
      });
    },

    /**
     * 拣货按钮操作
     */
     handleStocking(row){
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "出库单拣货";
        this.optType = "edit";
      });
    },

    /**
     * 填写运单信息按钮操作
     */
     handleShipping(row){
      this.reset();
      const salesId = row.salesId || this.ids
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "出库单填写运单信息";
        this.optType = "shipping";
      });
    },

    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.salesId != null) {
            updateProductsales(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProductsales(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },

    //提交按钮(提交到待拣货状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'UNSTOCK';
          updateProductsales(this.form).then(response => {
            this.$modal.msgSuccess("提交成功");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'PREPARE';
         });;          
        }
      });
    },

    submitToShipping(){
      let that = this;
      checkQuantity(this.form.salesId).then( response =>{
        if(response.data){
            that.form.status = 'UNSHIPPING';
            updateProductsales(that.form).then(response => {
              that.$modal.msgSuccess("提交成功");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'UNSTOCK';
            });    
          }else{
            this.$modal.msgError("拣货数量与出库数量不一致!");
          }
        }
      );   
    },

    //提交按钮(提交到待执行出库状态)
    submitToExecute(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'UNEXECUTE';
          updateProductsales(this.form).then(response => {
            this.$modal.msgSuccess("提交成功");
            this.open = false;
            this.getList();
          } 
         ).catch(() => {
            this.form.status = 'UNSHIPPING';
         });;          
        }
      });
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销出库单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
        updateProductsales(that.form).then(response => {
            that.$modal.msgSuccess("撤销成功");
            that.open = false;
            that.getList();
          } 
          ).catch(() => {
            that.form.status = oldStatus;
          });
          return true;
      }).catch(() => {});
    },
    /** 列表：草稿提交到待拣货 */
    handleSubmit(row) {
      const salesId = row.salesId;
      this.$modal.confirm('确认提交该出库单？').then(() => {
        return getProductsales(salesId).then(response => {
          const data = { ...response.data, status: 'UNSTOCK' };
          return updateProductsales(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待拣货提交到待运单 */
    handleSubmitShipping(row) {
      const salesId = row.salesId;
      this.$modal.confirm('确认提交运单环节？').then(() => {
        return checkQuantity(salesId).then(response => {
          if (!response.data) {
            this.$modal.msgError("拣货数量与出库数量不一致!");
            return Promise.reject();
          }
          return getProductsales(salesId).then(res => {
            const data = { ...res.data, status: 'UNSHIPPING' };
            return updateProductsales(data);
          });
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待运单提交到待执行 */
    handleSubmitExecute(row) {
      const salesId = row.salesId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return getProductsales(salesId).then(response => {
          const data = { ...response.data, status: 'UNEXECUTE' };
          return updateProductsales(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const salesId = row.salesId;
      this.$modal.confirm('确认撤销出库单？').then(() => {
        return getProductsales(salesId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateProductsales(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const salesIds = row.salesId || this.ids;
      this.$modal.confirm('是否确认删除销售出库单编号为"' + salesIds + '"的数据项？').then(function() {
        return delProductsales(salesIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/productsales/export', {
        ...this.queryParams
      }, `productsales_${new Date().getTime()}.xlsx`)
    },
    //执行出库
    handleExecute(row){
      const salesIds = row.salesId || this.ids;
      this.$modal.confirm('确认执行出库？').then(function() {
        return execute(salesIds)//执行入库
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("出库成功");
      }).catch(() => {});
    },
    //到货通知单选择
    handleSelectNotice(){
      this.$refs.noticeSelect.handleOpen(this.form.noticeId)
    },
    //到货通知单选择弹出框
    onNoticeSelected(obj){      
      if(obj != undefined && obj != null){
          this.form.noticeId = obj.noticeId;
          this.form.noticeCode = obj.noticeCode;
          this.form.soCode = obj.soCode;
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
          this.form.clientNick = obj.clientNick;
        }
    },
    handleSelectClient(){
      this.$refs["form"].clearValidate()
      this.$refs.clientSelect.handleOpen(this.form.clientId)
    },
    //客户选择弹出框
    onClientSelected(obj){
        if(obj != undefined && obj != null){
          this.form.clientId = obj.clientId;
          this.form.clientCode = obj.clientCode;
          this.form.clientName = obj.clientName;
          this.form.clientNick = obj.clientNick;
        }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('PRODUCTSALSE_CODE').then(response =>{
          this.form.salesCode = response;
        });
      }else{
        this.form.salesCode = null;
      }
    }
  }
};
</script>
