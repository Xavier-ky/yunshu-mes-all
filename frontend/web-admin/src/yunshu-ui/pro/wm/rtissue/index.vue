<template>
  <div class="app-container inbound-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="rtCode">
          <el-input v-model="queryParams.rtCode" placeholder="退料单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="workorderCode">
          <el-input v-model="queryParams.workorderCode" placeholder="生产工单" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="rtType">
          <el-select v-model="queryParams.rtType" placeholder="退料类型" clearable>
            <el-option
              v-for="dict in dict.type.mes_rt_issue_type"
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
        <span class="inbound-page-title">生产退料</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:rtissue:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:rtissue:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:rtissue:remove']">删除</el-button>
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
        :data="rtissueList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="退料单编号" align="center" header-align="center" min-width="130" prop="rtCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.rtCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="退料单名称" align="center" header-align="center" min-width="120" prop="rtName" show-overflow-tooltip />
        <el-table-column label="退料类型" align="center" header-align="center" width="100" prop="rtType">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_rt_issue_type" :value="scope.row.rtType"/>
          </template>
        </el-table-column>
        <el-table-column label="生产工单" align="center" header-align="center" min-width="120" prop="workorderCode" show-overflow-tooltip />
        <el-table-column label="工作站" align="center" header-align="center" min-width="100" prop="workstationName" show-overflow-tooltip />
        <el-table-column label="退料日期" align="center" header-align="center" prop="rtDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.rtDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_rt_issue_status" :value="scope.row.status"/>
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
                v-hasPermi="['mes:wm:rtissue:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status =='PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >执行上架</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='UNSTOCK'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='UNEXECUTE'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >执行退料</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:rtissue:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:rtissue:remove']"
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

    <!-- 添加或修改生产退料单头对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="退料单编号" prop="rtCode">
              <el-input v-model="form.rtCode" placeholder="请输入退料单编号" />
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
            <el-form-item label="退料单名称" prop="rtName">
              <el-input v-model="form.rtName" placeholder="请输入退料单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="退料类型" prop="rtType">
              <el-select v-model="form.rtType" placeholder="请选择退料类型">
                <el-option
                  v-for="dict in dict.type.mes_rt_issue_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产工单" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="请输入生产工单" >
                <template #append><el-button icon="el-icon-search" @click="handleWorkorderSelect"></el-button></template>
              </el-input>
            </el-form-item>
            <WorkorderSelect ref="woSelect" @onSelected="onWorkorderSelected"></WorkorderSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退料日期" prop="rtDate">
              <el-date-picker clearable
                v-model="form.rtDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择退料日期">
              </el-date-picker>
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
      <el-card shadow="always" v-if="form.rtId !=null && form.status =='PREPARE'" class="box-card">
        <Rtissueline :rtId="form.rtId" :rtType="form.rtType" :optType="optType"></Rtissueline>
      </el-card>
      <el-card shadow="always" v-if="form.rtId !=null && form.status !='PREPARE'" class="box-card">
        <RtissueDetail :rtId="form.rtId" :status="form.status" :rtType="form.rtType" :optType="optType"></RtissueDetail>
      </el-card>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="saveForm" v-if="form.status =='PREPARE' && optType !='view' ">保 存</el-button>
        <el-button type="warning" @click="submitToStock" v-if="form.status =='PREPARE' && form.rtId !=null && optType !='view' ">提 交</el-button>
        <el-button type="warning" @click="submitToExecute" v-if="form.status =='UNSTOCK' && form.rtId !=null && optType !='view' ">提 交</el-button>        
        <el-button type="danger" @click="cancel"  v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view' " >取 消</el-button>
        <el-button @click="close">关 闭</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRtissue, getRtissue, delRtissue, addRtissue, updateRtissue, execute } from "@/yunshu-ui/api/mes/wm/rtissue";
import WorkorderSelect from "@/yunshu-ui/components/workorderSelect/single.vue"
import Rtissueline from "./line.vue";
import RtissueDetail from "./detail.vue";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Rtissue",
  dicts: ['mes_rt_issue_status','mes_rt_issue_type'],
  components: {
    Rtissueline,WorkorderSelect,RtissueDetail
  },
  data() {
    return {
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
      // 生产退料单头表格数据
      rtissueList: [],
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
        workorderId: null,
        workorderCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        rtDate: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        rtCode: [
          { required: true, message: "退料单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        rtName: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        workorderCode: [
          { required: true, message: "请选择要退料的生产工单", trigger: "blur" }
        ],
        rtType: [
          { required: true, message: "请选择退料类型", trigger: "blur" }
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
    /** 查询生产退料单头列表 */
    getList() {
      this.loading = true;
      listRtissue(this.queryParams).then(response => {
        this.rtissueList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
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
        workorderId: null,
        workorderCode: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        rtType: null,
        rtDate: new Date(),
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
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加生产退料单头";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const rtId = row.rtId || this.ids
      getRtissue(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改生产退料单头";
        this.optType = "edit";
      });
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const rtId = row.rtId
      getRtissue(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看退料单信息";
        this.optType = "view";
      });
    },
    /** 保存按钮 */
    saveForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.rtId != null) {
            updateRtissue(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRtissue(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },

    handleStocking(row){
      this.reset();
      const rtId = row.rtId || this.ids
      getRtissue(rtId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "退料单上架";
        this.optType = "edit";
      });
    },

    //提交按钮(提交到待上架状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'UNSTOCK';
          updateRtissue(this.form).then(response => {
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

    /**
     * 提交到待执行退料状态
     */
    submitToExecute(){
      let oldStatus = this.form.status;
      this.form.status = 'UNEXECUTE';
      updateRtissue(this.form).then(response => {
          this.$modal.msgSuccess("提交成功");
          this.open = false;
          this.getList();
        } 
        ).catch(() => {
          this.form.status = oldStatus;
        });;       
    },


    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销退料单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
        updateRtissue(that.form).then(response => {
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

    //执行退料
    handleExecute(row){
      const rtIds = row.rtId || this.ids;
      this.$modal.confirm('确认执行退料？').then(function() {
        return execute(rtIds)//执行退料
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("退料成功");
      }).catch(() => {});
    },
    /** 列表：草稿提交到待上架 */
    handleSubmit(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认提交该退料单？').then(() => {
        return getRtissue(rtId).then(response => {
          const data = { ...response.data, status: 'UNSTOCK' };
          return updateRtissue(data);
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
        return getRtissue(rtId).then(response => {
          const data = { ...response.data, status: 'UNEXECUTE' };
          return updateRtissue(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const rtId = row.rtId;
      this.$modal.confirm('确认撤销退料单？').then(() => {
        return getRtissue(rtId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateRtissue(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const rtIds = row.rtId || this.ids;
      this.$modal.confirm('是否确认删除生产退料单头编号为"' + rtIds + '"的数据项？').then(function() {
        return delRtissue(rtIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/rtissue/export', {
        ...this.queryParams
      }, `rtissue_${new Date().getTime()}.xlsx`)
    },
    //选择生产工单
    handleWorkorderSelect(){
      this.$refs.woSelect.handleOpen(this.form.workorderId)
    },
    onWorkorderSelected(row){
      if(row != undefined && row != null){
        this.form.workorderId = row.workorderId;
        this.form.workorderCode = row.workorderCode;
      }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('RTISSUE_CODE').then(response =>{
          this.form.rtCode = response;
        });
      }else{
        this.form.rtCode = null;
      }
    }
  }
};
</script>
