<template>
    <div class="app-container">
      <!-- 添加或修改退料检验单对话框 -->
        <el-form ref="form" :model="form" :rules="rules" label-width="100px">
          <el-row>
            <el-col :span="8">
              <el-form-item label="检验单编号" prop="rqcCode">
                <el-input v-model="form.rqcCode" placeholder="请输入检验单编号" />
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
              <el-form-item label="检验单名称" prop="rqcName">
                <el-input v-model="form.rqcName" placeholder="请输入检验单名称" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="检验类型" prop="rqcType">
                <el-select v-model="form.rqcType" placeholder="请选择检验类型">
                  <el-option
                    v-for="dict in dict.type.mes_rqc_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="来源单据类型" prop="sourceDocType">
                <el-select v-model="form.sourceDocType" disabled>
                  <el-option
                    v-for="dict in dict.type.mes_source_doc_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="来源单据编号" prop="sourceDocCode">
                <el-input v-model="form.sourceDocCode" readonly="readonly" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="物资编码" prop="itemCode">
                <el-input v-if="form.rqcId ==null " v-model="form.itemCode" readonly placeholder="请选择物资" >
                  <el-button slot="append" @click="handleSelectProduct" icon="el-icon-search"></el-button>
                </el-input>
                <!--如果已经保存过，则产品不允许再修改，需要修改就删除重做-->
                <el-input v-else v-model="form.itemCode">
                </el-input>
              </el-form-item>
              <ItemSelect ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
            </el-col>
            <el-col :span="8">
              <el-form-item label="物资名称" prop="itemName">
                <el-input v-model="form.itemName" placeholder="请选择物资" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="单位" prop="unitName">
                <el-input v-model="form.unitName" placeholder="请选择物资" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="规格型号" prop="specification">
                <el-input v-model="form.specification" type="textarea" placeholder="请选择物资" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="批次号" prop="batchCode">
                <el-input v-model="form.batchCode" placeholder="请输入批次号" />
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
              <el-form-item label="检测日期" prop="inspectDate">
                <el-date-picker clearable
                  v-model="form.inspectDate"
                  type="date"
                  value-format="yyyy-MM-dd"
                  placeholder="请选择检测日期">
                </el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="检测数量" prop="quantityCheck">
                <el-input-number :min="0" :max="99999999" v-model="form.quantityCheck" placeholder="检测数量" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="合格品数量" prop="quantityQualified">
                <el-input-number :min="0" :max="99999999" v-model="form.quantityQualified" placeholder="请输入合格品数量" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="不合格数量" prop="quantityUnqualified">
                <el-input-number :min="0" :max="99999999" v-model="form.quantityUnqualified" placeholder="请输入不合格数" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="审核人" prop="nickName">
                <el-input v-model="form.nickName" placeholder="请选择审核人" >
                  <el-button slot="append" @click="handleUser2Select" icon="el-icon-search"></el-button>
                </el-input>
              </el-form-item>
              <UserSingleSelect ref="user2Select" @onSelected="onUser2Selected"></UserSingleSelect>
            </el-col>
            <el-col :span="8"></el-col>
            <el-col :span="8"></el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <el-tabs type="border-card" v-if="form.rqcId != null">
          <el-tab-pane label="检测项">
            <RqcLine ref=line :rqcId="form.rqcId" :optType="optType"></RqcLine>
          </el-tab-pane>
          <el-tab-pane label="检测结果">
            <QCResutl ref="qcResult" :qcId="form.rqcId" :qcType="'RQC'" :optType="optType"></QCResutl>
          </el-tab-pane>
        </el-tabs>
        <el-form label-width="100px">
          <el-form-item style="text-align: center;margin-left:-120px;margin-top:30px;">
          <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
          <el-button type="warning" @click="handleFinish" v-if="form.status =='PREPARE' && form.rqcId !=null && optType !='view' ">完 成</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </el-form-item>
        </el-form>
    </div>
  </template>
  
  <script>
  import { listRqc, getRqc, delRqc, addRqc, updateRqc } from "@/yunshu-ui/api/mes/qc/rqc";
  import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
  import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue"
  import QCResutl from "../qcresult/index.vue";
  import RqcLine from "../rqc/line.vue"
  import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
  export default {
    name: "Rqc",
    components: {QCResutl,RqcLine,ItemSelect,UserSingleSelect},
    dicts: ['mes_rqc_type','mes_qc_result','mes_source_doc_type','mes_rt_issue_status'],
    props: {
        sourceDocId: null,
        sourceDocCode: null,
        sourceDocType: null,
        sourceLineId: null,
        qcDetailType:null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        batchId: null,
        batchCode: null,
        quantityCheck: null
    },
    data() {
      return {
        autoGenFlag:false,
        optType: 'add',
        // 表单参数
        form: {

        },
        // 表单校验
        rules: {
          rqcCode: [
            { required: true, message: "检验单编号不能为空", trigger: "blur" }
          ],        rqcType: [
            { required: true, message: "请选择退料检测类型", trigger: "blur" }
          ],        checkResult: [
            { required: true, message: "请选择检测结果", trigger: "blur" }
          ],        inspectDate: [
            { required: true, message: "检测日期不能为空", trigger: "blur" }
          ],        nickName: [
            { required: true, message: "请选择检测人员", trigger: "blur" }
          ],        quantityCheck: [
            { required: true, message: "请填写检测数量", trigger: "blur" }
          ],      quantityQualified: [
            { required: true, message: "请填写合格品数量", trigger: "blur" }
          ],    quantityUnqualified: [
            { required: true, message: "请填写不良品数量", trigger: "blur" }
          ],                
        }
      };
    },
    created() {
        this.reset();
    },
    methods: {
      // 表单重置
      reset() {
        const q = this.$route.query || {};
        const p = (k) => (q[k] != null && q[k] !== '' ? q[k] : this[k]);
        this.form = {
          rqcId: null, rqcCode: null, rqcName: null,
          rqcType: p('qcDetailType') || 'PRQC',
          templateId: null,
          sourceDocId: p('sourceDocId'), sourceDocType: p('sourceDocType'),
          sourceDocCode: p('sourceDocCode'), sourceLineId: p('sourceLineId'),
          itemId: p('itemId'), itemCode: p('itemCode'), itemName: p('itemName'),
          specification: p('specification'), unitOfMeasure: p('unitOfMeasure'), unitName: p('unitName'),
          batchId: p('batchId'), batchCode: p('batchCode'),
          quantityCheck: p('quantityCheck'), quantityUnqualified: 0, quantityQualified: 0,
          checkResult: null, inspectDate: new Date(), userId: null, userName: null, nickName: null,
          status: "PREPARE", remark: null, attr1: null, attr2: null, attr3: null, attr4: null,
          createBy: null, createTime: null, updateBy: null, updateTime: null
        };
        this.resetForm("form");
        this.autoGenFlag = false;
      },
    // 取消按钮
        cancel() {
            const obj = { path: '/app/quality/workbench' };
            this.$tab.closeOpenPage(obj);
        },
      /** 保存按钮 */
        saveForm() {
            // 判断接收总数与合格不合格数之间的校验
            if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined && this.form.quantityQualified != 0
            && this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined && this.form.quantityUnqualified != 0) {
            if ((this.form.quantityQualified + this.form.quantityUnqualified) != this.form.quantityCheck) {
                this.$message.warning("合格数与不合格数之和必须等于检测总数！");
                return
            }
            }
            if (this.form.quantityQualified != '' && this.form.quantityQualified != undefined) {
            if (this.form.quantityQualified > this.form.quantityCheck) {
                this.$message.warning("合格数不能大于检测总数！");
                return
            }
            }
            if (this.form.quantityUnqualified != '' && this.form.quantityUnqualified != undefined) {
            if (this.form.quantityUnqualified > this.form.quantityCheck) {
                this.$message.warning("不合格数不能大于检测总数！");
                return
            }
            }
    
            this.$refs["form"].validate(valid => {
            if (valid) {
                if (this.form.rqcId != null) {
                updateRqc(this.form).then(response => {                    
                    this.$modal.msgSuccess("修改成功");
                });
                } else {
                addRqc(this.form).then(response => {
                    this.form.rqcId = response.data;
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
                        this.$modal.confirm('是否完成检验单编制？【完成后将不能更改】').then(function(){
                            that.form.status = 'FINISHED';
                            that.saveForm();
                            that.cancel();
                        });
                    }
                });
        },
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
        //点击人员选择按钮
        handleUser2Select(){
            this.$refs.user2Select.showFlag = true;
        },
        //人员选择返回
        onUser2Selected(row){
            this.form.userId = row.userId;
            this.form.userName = row.userName;
            this.form.nickName = row.nickName;                
        },
        //自动生成编码
        handleAutoGenChange(autoGenFlag){
            if(autoGenFlag){
            genCode('QC_RQC_CODE').then(response =>{
                this.form.rqcCode = response;
            });
            }else{
            this.form.rqcCode = null;
            }
        }
    }
  };
  </script>
  