<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="100px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="入库单编号" prop="recptCode">
            <el-input v-model="form.recptCode" placeholder="请输入入库单编号" />
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
          <el-form-item label="入库单名称" prop="recptName">
            <el-input v-model="form.recptName" placeholder="请输入入库单名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="入库日期" prop="recptDate">
            <el-date-picker clearable
                            v-model="form.recptDate"
                            type="date"
                            value-format="yyyy-MM-dd"
                            placeholder="请选择入库日期">
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="到货通知单" prop="noticeCode">
            <el-input v-model="form.noticeCode" readonly="readonly" placeholder="请选择到货通知单" >
              <template #append><el-button @click="handleSelectNotice" icon="el-icon-search"></el-button></template>
            </el-input>
            <NoticeSelect ref="noticeSelect" @onSelected="onNoticeSelected"></NoticeSelect>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="采购订单号" prop="poCode">
            <el-input v-model="form.poCode" placeholder="请输入采购订单号" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="供应商" prop="vendorName">
            <el-input v-model="form.vendorName" readonly="readonly" placeholder="请选择供应商" >
              <template #append><el-button @click="handleSelectVendor" icon="el-icon-search"></el-button></template>
            </el-input>
            <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
          </el-form-item>
        </el-col>
        <el-col :span="8">

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
    <el-divider v-if="form.recptId !=null" content-position="center">物料信息</el-divider>
    <el-card shadow="always" v-if="form.recptId !=null" class="box-card">
      <Itemrecptline ref="line" :recptId="form.recptId" :noticeId="form.noticeId" :optType="optType"></Itemrecptline>
    </el-card>
  </div>
</template>

<script>
import Itemrecptline from "./itemrecpt/line.vue";
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import NoticeSelect from "@/yunshu-ui/components/noticeSelect/single.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
import {getItemrecpt} from "@/yunshu-ui/api/mes/wm/itemrecpt";

export default {
  name: "itemrecptForm",
  components: {NoticeSelect, VendorSelect, Itemrecptline},
  dicts:['mes_order_status'],
  data() {
    return {
      recptId: '',
      //自动生成编码
      autoGenFlag:false,
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recptCode: [
          { required: true, message: "入库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        recptName: [
          { required: true, message: "入库单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        recptDate: [
          { required: true, message: "入库时间不能为空", trigger: "blur"}
        ],
        vendorName: [
          { required: true, message: "请选择对应的供应商", trigger: "blur"}
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.recptId = this.$route.query.id
    this.getItemrecptForm(this.recptId)
  },
  watch: {
    '$route.query.id': {
      handler(newVal, oldVal) {
        if (newVal != undefined) {
          this.recptId = newVal
          this.getItemrecptForm(this.recptId)
        }
      },
      deep: true
    }
  },
  methods: {
    getItemrecptForm(recptId) {
      getItemrecpt(recptId).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.optType = "view";
      });
    },
    //供应商选择弹出框
    onVendorSelected(obj){
      if(obj != undefined && obj != null){
        this.form.vendorId = obj.vendorId;
        this.form.vendorCode = obj.vendorCode;
        this.form.vendorName = obj.vendorName;
        this.form.vendorNick = obj.vendorNick;
      }
    },
    //供应商选择
    handleSelectVendor(){
      this.$refs.form.resetFields()
      this.$refs.vendorSelect.handleOpen(this.form.vendorId)
    },
    //到货通知单选择弹出框
    onNoticeSelected(obj){
      debugger;
      if(obj != undefined && obj != null){
        this.form.noticeId = obj.noticeId;
        this.form.noticeCode = obj.noticeCode;
        this.form.poCode = obj.poCode;
        this.form.vendorId = obj.vendorId;
        this.form.vendorName = obj.vendorName;
        this.form.vendorNick = obj.vendorNick;
      }
    },
    //到货通知单选择
    handleSelectNotice(){
      this.$refs.noticeSelect.handleOpen(this.form.noticeId)
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('ITEMRECPT_CODE').then(response =>{
          this.form.recptCode = response;
        });
      }else{
        this.form.recptCode = null;
      }
    }
  }
}
</script>
