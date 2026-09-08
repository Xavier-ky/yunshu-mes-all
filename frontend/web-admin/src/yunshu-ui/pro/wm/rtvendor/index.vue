<template>
  <div class="app-container outbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="rtCode">
          <el-input v-model="queryParams.rtCode" placeholder="退货单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="vendorName">
          <el-input v-model="queryParams.vendorName" placeholder="供应商" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="poCode">
          <el-input v-model="queryParams.poCode" placeholder="采购订单" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">供应商退货</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:rtvendor:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:rtvendor:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:rtvendor:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:wm:rtvendor:export']">导出</el-button>
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
        :data="rtvendorList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="退货单编号" align="center" header-align="center" min-width="130" prop="rtCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.rtCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="退货单名称" align="center" header-align="center" min-width="120" prop="rtName" show-overflow-tooltip />
        <el-table-column label="采购订单" align="center" header-align="center" min-width="110" prop="poCode" show-overflow-tooltip />
        <el-table-column label="供应商" align="center" header-align="center" min-width="120" prop="vendorName" show-overflow-tooltip />
        <el-table-column label="退货日期" align="center" header-align="center" prop="rtDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.rtDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_rtvendor_status" :value="scope.row.status"/>
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
                v-hasPermi="['mes:wm:rtvendor:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status =='PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='APPROVING'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >执行拣货</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='APPROVING'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='APPROVED'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >执行退货</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:rtvendor:remove']"
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

    <!-- 添加或修改供应商退货对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="退货单编号" prop="rtCode">
              <el-input v-model="form.rtCode" placeholder="请输入退货单编号" />
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
            <el-form-item label="退货单名称" prop="rtName">
              <el-input v-model="form.rtName" placeholder="请输入退货单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="采购订单编号" prop="poCode">
              <el-input v-model="form.poCode" placeholder="请输入采购订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" prop="vendorName">
              <el-input v-model="form.vendorName" readonly="readonly" placeholder="请选择供应商" >
                <template #append><el-button @click="handleSelectVendor" icon="el-icon-search"></el-button></template>
              </el-input>
              <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货日期" prop="rtDate">
              <el-date-picker clearable
                v-model="form.rtDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择退货日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="退货原因" prop="rtReason">
              <el-input v-model="form.rtReason" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="运单号" prop="transportCode">
              <el-input v-model="form.transportCode" placeholder="请输入运单号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人" prop="transportTel">
              <el-input v-model="form.transportTel" placeholder="请输入联系人" />
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
      <el-divider v-if="form.rtId !=null" content-position="center">物料信息</el-divider>
      <el-card shadow="always" v-if="form.rtId !=null && form.status == 'PREPARE'" class="box-card">
        <Rtvendorline ref="line" :rtId="form.rtId" :batchCode="form.batchCdoe" :vendorId="form.vendorId" :optType="optType"></Rtvendorline>
      </el-card>
      <el-card shadow="always" v-if="form.rtId !=null && form.status != 'PREPARE'" class="box-card">
        <RtvendorDetail ref="detail" :rtId="form.rtId" :batchCode="form.batchCdoe" :optType="optType"></RtvendorDetail>
      </el-card>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.rtId !=null && optType !='view' ">提 交</el-button>
        <el-button type="warning" @click="submitToExecute" v-if="form.status =='APPROVING' && form.rtId !=null && optType !='view' ">提 交</el-button>
        <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
        <el-button @click="close">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRtvendor, getRtvendor, checkQuantity, delRtvendor, addRtvendor, updateRtvendor,execute } from "@/yunshu-ui/api/mes/wm/rtvendor";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import Rtvendorline from "./line.vue"
import RtvendorDetail from "./detail.vue"
export default {
  name: "Rtvendor",
  components:{VendorSelect,Rtvendorline,RtvendorDetail},
  dicts: ['mes_rtvendor_status'],
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
      // 供应商退货表格数据
      rtvendorList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        rtCode: null,
        rtName: null,
        poCode: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        rtDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        rtCode: [
          { required: true, message: "退货单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        rtName: [
          { required: true, message: "退货单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        poCode: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        vendorName: [
          { required: true, message: "供应商不能为空", trigger: "blur" }
        ],
        rtDate: [
          { required: true, message: "退货日期不能为空", trigger: "blur" }
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
    /** 查询供应商退货列表 */
    getList() {
      this.loading = true;
      listRtvendor(this.queryParams).then(response => {
        this.rtvendorList = response.rows;
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
        rtId: null,
        rtCode: null,
        rtName: null,
        poCode: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        rtReason: null,
        transportCode: null,
        transportTel: null,
        rtDate: new Date(),
        status: "PREPARE",
        remark: null,
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
      this.ids = selection.map(item => item.rtId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加采购退货";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const rtIds = row.rtId
      getRtvendor(rtIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看退货单信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const rtId = row.rtId || this.ids
      getRtvendor(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改采购退货单";
        this.optType = "edit";
      });
    },


    /**
     * 拣货按钮操作
     */
     handleStocking(row){
      this.reset();
      const rtId = row.rtId || this.ids
      getRtvendor(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "采购退货单拣货";
        this.optType = "edit";
      });
    },

    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.rtId != null) {
            updateRtvendor(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRtvendor(this.form).then(response => {
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
          this.form.status = 'APPROVING';
          updateRtvendor(this.form).then(response => {
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

    submitToExecute(){
      let that = this;
      checkQuantity(this.form.rtId).then( response =>{
          debugger;
          if(response.data){
            that.form.status = 'APPROVED';
            updateRtvendor(that.form).then(response => {
              that.$modal.msgSuccess("提交成功");
              that.open = false;
              that.getList();
            } 
            ).catch(() => {
              that.form.status = 'APPROVING';
            });    
          }else{
            this.$modal.confirm('退货数量与拣货数量不一致，确认提交?').then(function() {
              that.form.status = 'APPROVED';
              updateRtvendor(that.form).then(response => {
                that.$modal.msgSuccess("提交成功");
                that.open = false;
                that.getList();
              } 
              ).catch(() => {
                that.form.status = 'APPROVING';
              });    
            }).catch(() => {

            });
          }
        }
      );


    },
    //执行退货
    handleExecute(row){
      const rtIds = row.rtId || this.ids;
      this.$modal.confirm('确认执行退货？').then(function() {
        return execute(rtIds)//执行入库
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("退货成功");
      }).catch(() => {});
    },
    /** 列表：草稿提交到待拣货 */
    handleSubmit(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认提交该退货单？').then(() => {
        return getRtvendor(rtId).then(response => {
          const data = { ...response.data, status: 'APPROVING' };
          return updateRtvendor(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待拣货提交到待执行 */
    handleSubmitExecute(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return checkQuantity(rtId).then(response => {
          const doSubmit = () => getRtvendor(rtId).then(res => {
            const data = { ...res.data, status: 'APPROVED' };
            return updateRtvendor(data);
          });
          if (response.data) {
            return doSubmit();
          }
          return this.$modal.confirm('退货数量与拣货数量不一致，确认提交?').then(() => doSubmit());
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认撤销退货单？').then(() => {
        return getRtvendor(rtId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateRtvendor(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销退货单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
          updateRtvendor(that.form).then(response => {
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


    /** 删除按钮操作 */
    handleDelete(row) {
      const rtIds = row.rtId || this.ids;
      this.$modal.confirm('是否确认删除供应商退货编号为"' + row.rtCode + '"的数据项？').then(function() {
        return delRtvendor(rtIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/rtvendor/export', {
        ...this.queryParams
      }, `rtvendor_${new Date().getTime()}.xlsx`)
    },
    //供应商选择
    handleSelectVendor(){
      this.$refs.vendorSelect.handleOpen(this.form.vendorId)
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
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('WM_RTVENDOR_CODE').then(response =>{
          this.form.rtCode = response;
        });
      }else{
        this.form.rtCode = null;
      }
    }
  }
};
</script>
