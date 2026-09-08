<template>
  <div class="app-container">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-row>
        <el-col :span="14">
          <el-row>
            <el-col :span="16">
              <el-form-item label="物料编码" prop="itemCode">
                <el-input v-model="form.itemCode" readonly="readonly" maxlength="64" v-if="optType == 'view'"/>
                <el-input v-model="form.itemCode" placeholder="请输入物料编码" maxlength="64" v-else/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item  label-width="80">
                <el-switch v-model="autoGenFlag"
                           active-color="#13ce66"
                           active-text="自动生成"
                           @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view'">
                </el-switch>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="物料名称" prop="itemName">
                <el-input v-model="form.itemName"  maxlength="255" readonly="readonly" v-if="optType=='view'" />
                <el-input v-model="form.itemName" placeholder="请输入物料名称" maxlength="255" v-else/>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="规格型号" prop="specification">
                <el-input v-model="form.specification" type="textarea" maxlength="500" readonly="readonly" v-if="optType=='view'" />
                <el-input v-model="form.specification" type="textarea" placeholder="请输入规格型号" maxlength="500" v-else/>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="单位" prop="unitOfMeasure">
                <el-select v-model="form.unitOfMeasure" disabled v-if="optType=='view'">
                  <el-option
                    v-for="item in measureOptions"
                    :key="item.measureCode"
                    :label="item.measureName"
                    :value="item.measureCode"
                    :disabled="item.enableFlag == 'N'"
                  ></el-option>
                </el-select>

                <el-select v-model="form.unitOfMeasure" placeholder="请选择单位" v-else>
                  <el-option
                    v-for="item in measureOptions"
                    :key="item.measureCode"
                    :label="item.measureName"
                    :value="item.measureCode"
                    :disabled="item.enableFlag == 'N'"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="10">
          <BarcodeImg ref="barcodeImg" :bussinessId="form.itemId" :bussinessCode="form.itemCode" barcodeType="ITEM"></BarcodeImg>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="14">
          <el-form-item  label="物料/产品分类" prop="itemTypeId">
            <treeselect v-model="form.itemTypeId" :options="itemTypeOptions" :show-count="true" disabled v-if="optType=='view'"  />
            <treeselect v-model="form.itemTypeId" :options="itemTypeOptions" :show-count="true" placeholder="请选择所属分类" v-else :disable-branch-nodes="true"/>
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item  label="高价值/易被盗物品" label-width="150px" prop="highValue">
            <el-checkbox v-model="form.highValue" :true-label="'Y'" :false-label="'N'"></el-checkbox>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
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
        <el-col :span="8">
          <el-form-item label="批次管理">
            <el-switch
              v-model="form.batchFlag"
              active-text="是"
              inactive-text="否"
              active-value="Y"
              inactive-value="N"
            ></el-switch>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="安全库存">
            <el-radio-group v-model="form.safeStockFlag" disabled v-if="optType=='view'">
              <el-radio
                v-for="dict in dict.type.sys_yes_no"
                :key="dict.value"
                :label="dict.value"
              >{{dict.label}}</el-radio>
            </el-radio-group>

            <el-radio-group v-model="form.safeStockFlag" v-else>
              <el-radio
                v-for="dict in dict.type.sys_yes_no"
                :key="dict.value"
                :label="dict.value"
              >{{dict.label}}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="form.safeStockFlag == 'Y'">
        <el-col :span="12">
          <el-form-item label="最小库存量">
            <el-input-number v-model="form.minStock" :percision="2" :step="1" disabled v-if="optType=='view'" />
            <el-input-number v-model="form.minStock" :percision="2" :step="1" placeholder="请输入最小安全库存量" v-else />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最大库存量">
            <el-input-number v-model="form.maxStock" :percision="2" :step="1" disabled v-if="optType=='view'" />
            <el-input-number v-model="form.maxStock" :percision="2" :step="1" placeholder="请输入最大安全库存量" v-else/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" readonly v-if="optType=='view'"></el-input>
            <el-input v-model="form.remark" type="textarea" maxlength="500" placeholder="请输入内容" v-else></el-input>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <el-tabs type="border-card" v-if="form.itemId != null">
      <el-tab-pane label="BOM组成">
        <ItemBom :optType="optType" :itemId="form.itemId"></ItemBom>
      </el-tab-pane>
      <el-tab-pane v-if="form.batchFlag =='Y'" label="批次属性">
        <BatchConfig :itemId="form.itemId" :optType="optType"></BatchConfig>
      </el-tab-pane>
      <el-tab-pane label="替代品"></el-tab-pane>
      <el-tab-pane label="SIP">
        <SIPTab :itemId="form.itemId" :optType="optType"></SIPTab>
      </el-tab-pane>
      <el-tab-pane label="SOP">
        <SOPTab :itemId="form.itemId" :optType="optType"></SOPTab>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import SOPTab from "./components/sop.vue";
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue";
import SIPTab from "./components/sip.vue";
import ItemBom from "./components/itembom.vue";
import Treeselect from "@zanmato/vue3-treeselect";
import BatchConfig from "./components/batch.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule";
import {listAllUnitmeasure} from "@/yunshu-ui/api/mes/md/unitmeasure";
import {treeselect} from "@/yunshu-ui/api/mes/md/itemtype";
import {getMdItem} from "@/yunshu-ui/api/mes/md/mdItem";

export default {
  name: "itemForm",
  components: {ItemBom, SIPTab, Treeselect, BarcodeImg, SOPTab, BatchConfig},
  dicts: ['sys_yes_no','mes_item_product'],
  data() {
    return {
      itemId: '',
      // 物料数据
      form: {},
      // 部门树选项
      itemTypeOptions: undefined,
      //单位列表
      measureOptions: [],
      //自动生成物料编码标识
      autoGenFlag: false,
      //弹框的操作类型 view add edit
      optType: undefined,
      // 表单校验
      rules: {
        itemCode: [
          { required: true, message: "物料/产品编码不能为空", trigger: "blur" },
          { max: 64, message: '物料/产品编码长度必须小于64个字符', trigger: 'blur' }
        ],
        itemName: [
          { required: true, message: "物料/产品名称不能为空", trigger: "blur" }
        ],
        unitOfMeasure: [
          { required: true, message: "单位不能为空",trigger: "blur"}
        ],
        itemTypeId: [
          { required: true, message: "物料分类不能为空", trigger: "blur" },
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.itemId = this.$route.query.id
    this.getMdItemForm(this.itemId)
    this.getUnits()
    this.getTreeselect()
  },
  watch: {
    '$route.query.id': {
      handler(newVal, oldVal) {
        if (newVal != undefined) {
          this.itemId = newVal
          this.getMdItemForm(this.itemId)
        }
      },
      deep: true
    }
  },
  methods: {
    // 获取
    getMdItemForm(itemId) {
      getMdItem(itemId).then(response => {
        this.form = response.data;
        this.optType = "view";
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
        })
      });
    },
    /** 查询分类下拉树结构 */
    getTreeselect() {
      treeselect().then(response => {
        this.itemTypeOptions = response.data;
      });
    },
    getUnits(){
      listAllUnitmeasure().then(response =>{
        this.measureOptions = response.data;
      });
    },
    //自动生成物料编码
    handleAutoGenChange(autoGenFlag){
      debugger;
      if(autoGenFlag){
        genCode('ITEM_CODE').then(response =>{
          this.form.itemCode = response;
        });
      }else{
        this.form.itemCode = null;
      }
    }
  }
};
</script>
