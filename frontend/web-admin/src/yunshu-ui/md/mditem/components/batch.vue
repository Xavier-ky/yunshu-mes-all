<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="2" :offset="22">
        <el-button
          type="danger"
          plain
          icon="el-icon-save"
          size="default"
          v-if="optType === 'add' || optType === 'edit'"
          @click="submitForm"
          v-hasPermi="['mes:md:batchconfig:update']"
        >保存</el-button>
      </el-col>
    </el-row>
    <el-form ref="form" :model="form" :rules="rules">
      <el-form-item prop="selectedAttributes">
      <el-row>
        <el-col :span="4">
          <el-checkbox v-model="form.vendorFlag" v-if="itemProductFlag === 'ITEM'" true-label="Y" false-label="N">供应商</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.poCodeFlag" v-if="itemProductFlag === 'ITEM'" true-label="Y" false-label="N">采购订单编号</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.lotNumberFlag" v-if="itemProductFlag === 'ITEM'" true-label="Y" false-label="N">生产批号</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.expireDateFlag" v-if="itemProductFlag === 'ITEM'" true-label="Y" false-label="N">有效期</el-checkbox>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="4">
          <el-checkbox v-model="form.clientFlag"  v-if="itemProductFlag === 'PRODUCT'" true-label="Y" false-label="N">客户</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.coCodeFlag"  v-if="itemProductFlag === 'PRODUCT'" true-label="Y" false-label="N">销售订单编号</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.workorderFlag" v-if="itemProductFlag === 'PRODUCT'" true-label="Y" false-label="N">生产工单</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.workstationFlag" v-if="itemProductFlag === 'PRODUCT'" true-label="Y" false-label="N">工作站</el-checkbox>
        </el-col>
        <el-col :span="4">
          <el-checkbox v-model="form.produceDateFlag" true-label="Y" false-label="N">生产日期</el-checkbox>
        </el-col>
      </el-row>
    </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {  getBatchconfig, addBatchconfig, updateBatchconfig } from "@/yunshu-ui/api/mes/md/batchconfig";

export default {
  name: "Batchconfig",
  props: {
    itemId: null,
    itemProductFlag: null,
    optType: null
  },
  data() {
    return {

  // 表单参数
      form: {
        configId: null,
        itemId: this.itemId,
        produceDateFlag: 'N',
        expireDateFlag: 'N',
        recptDateFlag: 'N',
        vendorFlag: 'N',
        clientFlag: 'N',
        coCodeFlag: 'N',
        poCodeFlag: 'N',
        workorderFlag: 'N',
        taskFlag: 'N',
        workstationFlag: 'N',
        toolFlag: 'N',
        moldFlag: 'N',
        lotNumberFlag: 'N',
        qualityStatusFlag: 'N'
      },
      // 表单校验
      rules: {
        selectedAttributes: [
          { validator: this.checkSelectedAttributes, trigger: 'change' }
        ]
      }
    };
  },
  created() {
    this.getBatchconfig();
  },
  methods: {
    checkSelectedAttributes(rule, value, callback) {
      if (
        this.form.produceDateFlag === 'N' &&
        this.form.expireDateFlag === 'N' &&
        this.form.recptDateFlag === 'N' &&
        this.form.vendorFlag === 'N' &&
        this.form.clientFlag === 'N' &&
        this.form.coCodeFlag === 'N' &&
        this.form.poCodeFlag === 'N' &&
        this.form.workorderFlag === 'N' &&
        this.form.taskFlag === 'N' &&
        this.form.workstationFlag === 'N' &&
        this.form.toolFlag === 'N' &&
        this.form.moldFlag === 'N' &&
        this.form.lotNumberFlag === 'N' &&
        this.form.qualityStatusFlag === 'N'
      ) {        
        callback(
            new Error("至少选择一个属性")
        );
      } else {
        callback();
      }
    },

    /** 获取批次配置 */
    getBatchconfig() {      
      getBatchconfig(this.itemId).then(response => {
        if(response.data ){
          this.form = response.data;
        }        
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.configId != null) {
            updateBatchconfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
            });
          } else {
            addBatchconfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
            });
          }
        } 
      });
    }
  }
};
</script>
