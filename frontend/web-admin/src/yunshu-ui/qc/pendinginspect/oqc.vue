<template>
    <div class="app-container">
        <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="检验单编号" prop="oqcCode">
              <el-input v-model="form.oqcCode" placeholder="请输入出货检验单编号" />
            </el-form-item>
          </el-col>

          <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view' && form.status =='PREPARE'" >               
              </el-switch>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="检验单名称" prop="oqcName">
              <el-input v-model="form.oqcName" placeholder="请输入内容" />
            </el-form-item>
          </el-col>          
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="产品编码" prop="itemCode">
              <el-input v-model="form.itemCode"  readonly="readonly">
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="产品名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" ></el-input>            
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="specification">
              <el-input readonly="readonly" v-model="form.specification" type="textarea" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编号" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" >
              </el-input>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input readonly="readonly" v-model="form.clientName"  />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="8">
            <el-form-item label="检测数量" prop="quantityCheck">
              <el-input-number :min="0" v-model="form.quantityCheck" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="合格品数量" prop="quantityQualified">
              <el-input-number :min="0" v-model="form.quantityQualified" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="不合格数量" prop="quantityUnqualified">
              <el-input-number :min="0" readonly="readonly" v-model="form.quantityUnqualified" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="检测日期" prop="inspectDate">
              <el-date-picker clearable
                v-model="form.inspectDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择检测日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="检测结果" prop="checkResult">
              <el-select v-model="form.checkResult" placeholder="请选择检测结果">
                <el-option
                  v-for="dict in dict.type.mes_qc_result"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
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
        <el-collapse accordion>
          <el-collapse-item title="结果统计">
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷数量" prop="crQuantity">
                  <el-input readonly="readonly" v-model="form.crQuantity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷数量" prop="majQuantity">
                  <el-input readonly="readonly" v-model="form.majQuantity" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷数量" prop="minQuantity">
                  <el-input readonly="readonly" v-model="form.minQuantity" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="8">
                <el-form-item label="致命缺陷率" prop="crQuantity">
                  <el-input readonly="readonly" v-model="form.crRate" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="严重缺陷率" prop="majRate">
                  <el-input readonly="readonly" v-model="form.majRate" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="轻微缺陷率" prop="minRate">
                  <el-input readonly="readonly" v-model="form.minRate" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>        
      </el-form>
      <el-tabs type="border-card" v-if="form.rqcId != null">
        <el-tab-pane label="检测项">
          <Oqcline ref=line :oqcId="form.oqcId" :optType="optType"></Oqcline>
        </el-tab-pane>
        <el-tab-pane label="检测结果">
          <QCResutl ref="qcResult" :qcId="form.oqcId" :qcType="'OQC'" :optType="optType"></QCResutl>
        </el-tab-pane>
      </el-tabs>
      <el-form label-width="100px">
        <el-form-item style="text-align: center;margin-left:-120px;margin-top:30px;">
          <el-button type="primary" @click="cancel" v-if="form.status !='PREPARE' ">返回</el-button>
          <el-button type="primary" @click="submitForm" v-if="form.status =='PREPARE' ">保 存</el-button>
          <el-button type="success" @click="handleFinish" v-if="form.status =='PREPARE' && form.oqcId !=null">完成</el-button>
          <el-button @click="cancel">取 消</el-button>
        </el-form-item>
      </el-form>
    </div>
</template>
<script>
import { listOqc, getOqc, delOqc, addOqc, updateOqc } from "@/yunshu-ui/api/mes/qc/oqc";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
import Oqcline from "../oqc/line.vue";
export default {
  name: "Oqc",
  dicts: ['mes_order_status','mes_qc_result'],
  components: {
    Oqcline
  },
  data(){
    return {
      autoGenFlag:false,
      optType: 'add',
        // 表单参数
      form: {},
      // 表单校验
      rules: {
        oqcCode: [
          { required: true, message: "出货检验单编号不能为空", trigger: "blur" }
        ], 
        itemCode: [
          { required: true, message: "产品不能为空", trigger: "blur" }
        ],
        quantityCheck: [
          { required: true, message: "检测数量不能为空", trigger: "blur" }
        ],
        quantityQualified: [
          { required: true, message: "合格品数量不能为空", trigger: "blur" }
        ],
        quantityUnqualified: [
          { required: true, message: "不合格品数量不能为空", trigger: "blur" }
        ],
        checkResult: [
          { required: true, message: "检测结果", trigger: "blur" }
        ],
        inspectDate: [
          { required: true, message: "请选择检测日期", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.reset();
  },
  methods: {
    // 取消按钮
    cancel() {
      const obj = { path: '/app/quality/workbench' };
      this.$tab.closeOpenPage(obj);
    },
    // 表单重置
    reset() {
      this.form = {
        oqcId: null,
        oqcCode: null,
        oqcName: '销售出库单【'+this.$route.query.sourceDocCode+'】出货检验单',
        sourceDocId: this.$route.query.sourceDocId,
        sourceDocType: this.$route.query.sourceDocType,
        sourceDocCode: this.$route.query.sourceDocCode,
        sourceLineId: this.$route.query.sourceLineId,
        templateId: null,
        clientId: this.$route.query.clientId,
        clientCode: this.$route.query.clientCode,
        clientName: this.$route.query.clientName,
        itemId: this.$route.query.itemId,
        itemCode: this.$route.query.itemCode,
        itemName: this.$route.query.itemName,
        specification: this.$route.query.specification,        
        unitOfMeasure: this.$route.query.unitOfMeasure,
        unitName: this.$route.query.unitName,
        batchCode: this.$route.query.batchCode,
        quantityMinCheck: null,
        quantityMaxUnqualified: null,
        quantityOut: this.$route.query.quantityCheck,
        quantityCheck: this.$route.query.quantityCheck,
        quantityUnqualified: 0,
        quantityQualified: 0,
        crRate: null,
        majRate: null,
        minRate: null,
        crQuantity: null,
        majQuantity: null,
        minQuantity: null,
        checkResult: null,
        outDate: null,
        inspectDate: new Date(),
        inspector: null,
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
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.oqcId != null) {
            updateOqc(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
            });
          } else {
            addOqc(this.form).then(response => {
              this.form.oqcId = response.data;
              this.$modal.msgSuccess("新增成功");
            });
          }
        }
      });
    },
    //点击完成
    handleFinish(){
      let that = this;
      if(this.form.checkResult == null){
        this.$modal.msgError("请选择检测结果！");
        return;
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          this.$modal.confirm('是否完成出货检验单编制？【完成后将不能更改】').then(function(){
            that.form.status = 'FINISHED';
            that.submitForm();
            that.cancel();
          });
        }
        });
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('QC_OQC_CODE').then(response =>{
          this.form.oqcCode = response;
        });
      }else{
        this.form.oqcCode = null;
      }
    },
  }

}
</script>