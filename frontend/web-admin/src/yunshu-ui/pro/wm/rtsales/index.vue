<template>
  <div class="app-container inbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="rtCode">
          <el-input v-model="queryParams.rtCode" placeholder="退货单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="clientName">
          <el-input v-model="queryParams.clientName" placeholder="客户" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="单据状态" clearable>
            <el-option
              v-for="dict in dict.type.mes_order_status"
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
        <span class="inbound-page-title">销售退货</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:rtsales:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:rtsales:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:rtsales:remove']">删除</el-button>
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
        :data="rtsalesList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="退货单编号" align="center" header-align="center" min-width="130" prop="rtCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.rtCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="退货单名称" align="center" header-align="center" min-width="120" prop="rtName" show-overflow-tooltip />
        <el-table-column label="销售订单" align="center" header-align="center" min-width="110" prop="soCode" show-overflow-tooltip />
        <el-table-column label="客户" align="center" header-align="center" min-width="110" prop="clientName" show-overflow-tooltip />
        <el-table-column label="退货原因" align="center" header-align="center" min-width="100" prop="rtReason" show-overflow-tooltip />
        <el-table-column label="退货日期" align="center" header-align="center" prop="rtDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.rtDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_rt_sales_status" :value="scope.row.status"/>
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
                v-hasPermi="['mes:wm:rtsales:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >执行上架</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='UNEXECUTE'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >执行退货</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:rtsales:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status == 'PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:rtsales:remove']"
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

    <!-- 添加或修改产品销售退货单对话框 -->
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
            <el-form-item label="销售订单编号" prop="soCode">
              <el-input v-model="form.soCode" placeholder="请输入销售订单编号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户编号" prop="clientCode">
              <el-input v-model="form.clientCode" readonly="readonly" placeholder="请选择客户" >
                <template #append><el-button @click="handleSelectClient" icon="el-icon-search"></el-button></template>
              </el-input>
            </el-form-item>
            <ClientSelectSingle ref="clientSelect" @onSelected="handleClientSelect"></ClientSelectSingle>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" readonly="readonly" placeholder="请选择客户" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
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
          <el-col :span="8">

          </el-col>
          <el-col :span="8">
            <el-form-item label="单据状态" prop="status">
              <el-select v-model="form.status" disabled placeholder="请选择单据状态">
                <el-option
                  v-for="dict in dict.type.mes_rt_sales_status"
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
            <el-form-item label="退货原因" prop="rtReason">
              <el-input v-model="form.rtReason" type="textarea" placeholder="请输入退货原因" />
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
      <el-divider v-if="form.rtId !=null" content-position="center">产品信息</el-divider>
      <el-card shadow="always" v-if="form.rtId !=null && form.status == 'PREPARE' " class="box-card">
        <Rtsalesline :rtId="form.rtId" :clientId="form.clientId" :coCode="form.coCode" :optType="optType"></Rtsalesline>
      </el-card>
      <el-card v-if="form.rtId !=null && form.status !='PREPARE'" class="box-card">
          <RtsalesDetail ref="detail" :rtId="form.rtId" :optType="optType"></RtsalesDetail>
        </el-card>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="warning" @click="submitToCheck" v-if="form.status =='PREPARE' && form.rtId !=null && optType !='view' ">提 交</el-button>        
        <el-button type="warning" @click="submitToExecute" v-if="form.status =='UNSTOCK' && form.rtId !=null && optType !='view' ">提 交</el-button>
        <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
        <el-button @click="close">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRtsales, getRtsales, delRtsales, addRtsales, updateRtsales ,execute} from "@/yunshu-ui/api/mes/wm/rtsales";
import Rtsalesline from "./line.vue";
import RtsalesDetail from "./detail.vue";
import ClientSelectSingle from "@/yunshu-ui/components/clientSelect/single.vue"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Rtsales",
  dicts: ['mes_rt_sales_status'],
  components: {
    ClientSelectSingle,Rtsalesline,RtsalesDetail
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
      // 产品销售退货单表格数据
      rtsalesList: [],
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
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        rtDate: null,
        rtReason: null,
        status: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        rtCode: [
          { required: true, message: "退货单编号不能为空", trigger: "blur" }
        ],
        rtName: [
          { required: true, message: "退货单名称不能为空", trigger: "blur" }
        ],
        rtDate: [
          { required: true, message: "请选择退货日期", trigger: "blur" }
        ],
        clientName: [
          { required: true, message: "请选择客户", trigger: "blur" }
        ],
        rtReason: [
          { required: true, message: "请填写退货原因", trigger: "blur" }
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
    /** 查询产品销售退货单列表 */
    getList() {
      this.loading = true;
      listRtsales(this.queryParams).then(response => {
        this.rtsalesList = response.rows;
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
        soCode: null,
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,  
        rtDate: new Date(),
        rtReason: null,
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
      this.ids = selection.map(item => item.rtId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    //客户选择事件
    handleSelectClient(){
      this.$refs.clientSelect.handleOpen(this.form.clientId)
    },
    //客户选择
    handleClientSelect(obj){
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
      this.title = "添加产品销售退货单";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const rtIds = row.rtId
      getRtsales(rtIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看销售退货单信息";
        this.optType = "view";
      });
    },
        /**
     * 上架按钮操作
     */
     handleStocking(row){
      const rtIds = row.rtId
      getRtsales(rtIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "销售退货单上架";
        this.optType = "edit";
      });
    },
    //执行入库
    handleExecute(row){
      const rtIds = row.rtId || this.ids;
      this.$modal.confirm('确认执行退货？').then(function() {
        return execute(rtIds)//执行退货
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("退货成功");
      }).catch(() => {});
    },
    /** 列表：草稿提交到待检验 */
    handleSubmit(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认提交该退货单？').then(() => {
        return getRtsales(rtId).then(response => {
          const data = { ...response.data, status: 'UNCHECK' };
          return updateRtsales(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待上架提交到待执行 */
    handleSubmitExecute(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return getRtsales(rtId).then(response => {
          const data = { ...response.data, status: 'UNEXECUTE' };
          return updateRtsales(data);
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
        return getRtsales(rtId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateRtsales(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const rtId = row.rtId || this.ids
      getRtsales(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改销售退货单";
        this.optType = "add";
      });
    },
    /** 提交按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.rtId != null) {
            updateRtsales(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRtsales(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },


    //提交按钮(提交到待检验状态)
    submitToCheck(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'UNCHECK';
          updateRtsales(this.form).then(response => {
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
      this.form.status = 'UNEXECUTE';
        updateRtsales(this.form).then(response => {
          this.$modal.msgSuccess("提交成功");
          this.open = false;
          this.getList();
        } 
        ).catch(() => {
          this.form.status = 'UNSTOCK';
        });;       
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销退货单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
          updateRtsales(that.form).then(response => {
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
      this.$modal.confirm('是否确认删除产品销售退货单编号为"' + row.rtCode + '"的数据项？').then(function() {
        return delRtsales(rtIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/rtsales/export', {
        ...this.queryParams
      }, `rtsales_${new Date().getTime()}.xlsx`)
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('WM_RTSALSE_CODE').then(response =>{
          this.form.rtCode = response;
        });
      }else{
        this.form.rtCode = null;
      }
    }
  }
};
</script>
