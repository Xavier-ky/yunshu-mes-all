<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
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
          <el-form-item label="销售订单编号" prop="soCode">
            <el-input v-model="form.soCode" placeholder="请输入销售订单编号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="客户编码" prop="clientCode">
            <el-input v-model="form.clientCode" placeholder="请输入客户编码" >
              <template #append><el-button @click="handleSelectClient" icon="el-icon-search"></el-button></template>
            </el-input>
            <ClientSelect ref="clientSelect" @onSelected="onClientSelected" > </ClientSelect>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="客户名称" prop="clientName">
            <el-input v-model="form.clientName" readonly="readonly"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>

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
        <el-col :span="8">
          <el-form-item label="单据状态" prop="status">
            <el-select v-model="form.status" disabled placeholder="请选择单据状态">
              <el-option
                v-for="dict in dict.type.mes_order_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
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
    <el-divider v-if="form.salesId !=null" content-position="center">物料信息</el-divider>
    <el-card shadow="always" v-if="form.salesId !=null" class="box-card">
      <Productsalesline ref="line" :salesId="form.salesId" :warehouseId="form.warehouseId" :locationId="form.locationId" :areaId="form.areaId" :optType="optType"></Productsalesline>
    </el-card>
  </div>
</template>

<script>
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
import Productsalesline from "./line.vue";
import ClientSelect from "@/yunshu-ui/components/clientSelect/single.vue";
import {getProductsales} from "@/yunshu-ui/api/mes/wm/productsales";

export default {
  name: "itemForm",
  components: {ClientSelect, Productsalesline},
  dicts: ['sys_yes_no','mes_item_product', 'mes_order_status'],
  data() {
    return {
      salesId: '',
      // 销售出库数据
      form: {},
      //自动生成物料编码标识
      autoGenFlag: false,
      //弹框的操作类型 view add edit
      optType: undefined,
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
    this.salesId = this.$route.query.id
    this.getProdectsalesForm(this.salesId)
  },
  watch: {
    '$route.query.id': {
      handler(newVal, oldVal) {
        if (newVal != undefined) {
          this.salesId = newVal
          this.getProdectsalesForm(this.salesId)
        }
      },
      deep: true
    }
  },
  methods: {
    getProdectsalesForm(salesId) {
      getProductsales(salesId).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.optType = "view";
      });
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
      }
    },
    //自动生成物料编码
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
