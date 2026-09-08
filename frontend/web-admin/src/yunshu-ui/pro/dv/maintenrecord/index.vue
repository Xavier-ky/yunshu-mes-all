<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">

      <el-form-item label="计划名称" prop="planName">
        <el-input
          v-model="queryParams.planName"
          placeholder="请输入计划名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>

      <el-form-item label="设备编码" prop="machineryCode">
        <el-input
          v-model="queryParams.machineryCode"
          placeholder="请输入设备编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="machineryName">
        <el-input
          v-model="queryParams.machineryName"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="保养人" prop="nickName">
        <el-input
          v-model="queryParams.nickName"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="保养时间" prop="maintenTime">
        <el-date-picker clearable
          v-model="queryParams.maintenTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择保养时间">
        </el-date-picker>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['mes:dv:maintenrecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:dv:maintenrecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:dv:maintenrecord:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" class="yunshu-data-table" stripe border :data="maintenrecordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="设备编码" align="center" prop="machineryCode" >
        <template #default="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['mes:dv:maintenrecord:query']"
          >{{scope.row.machineryCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="设备名称" align="center" prop="machineryName" />
      <el-table-column label="品牌" align="center" prop="machineryBrand" />
      <el-table-column label="规格型号" align="center" prop="machinerySpec" />
      <el-table-column label="计划名称" align="center" prop="planName" />
      <el-table-column label="保养时间" align="center" prop="maintenTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.maintenTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="保养人" align="center" prop="nickName" />
      <el-table-column label="状态" align="center" prop="status" >
        <template #default="scope">
          <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-if="scope.row.status === 'PREPARE'"
            v-hasPermi="['dv:maintenrecord:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-if="scope.row.status === 'PREPARE'"
            v-hasPermi="['dv:maintenrecord:remove']"
          >删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改设备保养记录对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="设备编码" prop="machineryCode">
              <el-input v-model="form.machineryCode" placeholder="请选择设备" >
                <el-button slot="append" @click="handleSelectMachinery" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <MachinerySelectSingle ref="machinerySelect" @onSelected="onMachineryAdd"></MachinerySelectSingle>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设备名称" prop="machineryName">
              <el-input v-model="form.machineryName" readonly="readonly" placeholder="请选择设备" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌" prop="machineryBrand">
              <el-input v-model="form.machineryBrand" readonly="readonly" placeholder="请选择设备" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="machinerySpec">
              <el-input v-model="form.machinerySpec" readonly="readonly" type="textarea" placeholder="请选择设备" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="保养计划" prop="planName">
              <el-input v-model="form.planName" placeholder="请选择设备保养计划" >
                <el-button slot="append" @click="handleSelectPlan" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <CheckplanSelect ref="checkplanSelect" planType="MAINTEN" @onSelected="onPlanAdd"></CheckplanSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保养人" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请选择保养人" >
                <el-button slot="append" @click="handleUserSelect" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <UserSingleSelect ref="userSelect" @onSelected="onUserSelected"></UserSingleSelect>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保养时间" prop="maintenTime">
              <el-date-picker clearable
                v-model="form.maintenTime"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="请选择保养时间">
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
      <el-divider v-if="form.recordId !=null" content-position="center">保养项目</el-divider>
      <el-card shadow="always" v-if="form.recordId !=null" class="box-card">
        <MaintenRecordLine ref="maintenRecordLine" :optType="optType" :recordId="form.recordId"></MaintenRecordLine>
      </el-card>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="cancel" v-if="optType =='view' || form.status !='PREPARE' ">返 回</el-button>
        <el-button type="primary" @click="submitForm" v-if="optType !=='view' && form.status =='PREPARE' " >保 存</el-button>
        <el-button type="success" @click="handleFinish" v-if="optType !=='view' && form.status =='PREPARE' " >提 交</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listMaintenrecord, getMaintenrecord, delMaintenrecord, addMaintenrecord, updateMaintenrecord } from "@/yunshu-ui/api/mes/dv/maintenrecord";
import MachinerySelectSingle from "@/yunshu-ui/components/machinerySelect/single.vue";
import CheckplanSelect from "@/yunshu-ui/components/dvplanSelect/index.vue";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue"
import MaintenRecordLine from "./line.vue"
export default {
  name: "Maintenrecord",
  dicts: ['mes_order_status'],
  components: {MachinerySelectSingle, CheckplanSelect, UserSingleSelect, MaintenRecordLine},
  data() {
    return {
      optType: null,
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
      // 设备保养记录表格数据
      maintenrecordList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planId: null,        planCode: null,        planName: null,        planType: null,        machineryId: null,        machineryCode: null,        machineryName: null,        machineryBrand: null,        machinerySpec: null,        maintenTime: null,        userId: null,        userName: null,        nickName: null,        status: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        machineryId: [
          { required: true, message: "设备ID不能为空", trigger: "blur" }
        ],        machineryCode: [
          { required: true, message: "设备编码不能为空", trigger: "blur" }
        ],        machineryName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],        maintenTime: [
          { required: true, message: "保养时间不能为空", trigger: "blur" }
        ],      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询设备保养记录列表 */
    getList() {
      this.loading = true;
      listMaintenrecord(this.queryParams).then(response => {
        this.maintenrecordList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
      recordId: null,        planId: null,        planCode: null,        planName: null,        planType: null,        machineryId: null,        machineryCode: null,        machineryName: null,        machineryBrand: null,        machinerySpec: null,        maintenTime: null,        userId: null,        userName: null,        nickName: null,        status: "PREPARE",        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
      this.form.userId= this.$store.state.user.id;
      this.form.nickName= this.$store.state.user.nick;
      this.form.userName= this.$store.state.user.name;
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
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加设备保养记录";
      this.optType = "add";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getMaintenrecord(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备保养记录";
        this.optType = "edit";
      });
    },
    handleView(row) {
      this.reset();
      const recordId = row.recordId || this.ids
      getMaintenrecord(recordId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看设备保养记录";
        this.optType = "view";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateMaintenrecord(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addMaintenrecord(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleFinish(){
      if(this.form.recordId != null && this.form.status =='PREPARE'){
        this.form.status='FINISHED';
        updateMaintenrecord(this.form).then(response => {
                this.$modal.msgSuccess("已提交");
                this.open = false;
                this.getList();
        },err =>{
          this.form.status='PREPARE';
        });
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids;
      this.$modal.confirm('是否确认删除设备保养记录编号为"' + recordIds + '"的数据项？').then(function() {
        return delMaintenrecord(recordIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },

    handleSelectMachinery(){      
      this.$refs.machinerySelect.showFlag = true;
    },

    //设备资源选择回调
    onMachineryAdd(obj){      
      if(obj !=null && obj != undefined){
        this.form.machineryId = obj.machineryId;
        this.form.machineryCode = obj.machineryCode;
        this.form.machineryName = obj.machineryName;
        this.form.machineryBrand = obj.machineryBrand;
        this.form.machinerySpec = obj.machinerySpec;
      }
    },

    handleSelectPlan(){      
      this.$refs.checkplanSelect.showFlag = true;
    },

    onPlanAdd(obj){      
      if(obj !=null && obj != undefined){
        this.form.planId = obj.planId;
        this.form.planCode = obj.planCode;
        this.form.planName = obj.planName;
        this.form.planType = obj.planType;
      }
    },

    //点击人员选择按钮
    handleUserSelect(){
      this.$refs.userSelect.showFlag = true;
    },
    //人员选择返回
    onUserSelected(row){
      this.form.userId = row.userId;
      this.form.nickName = row.nickName;
      this.form.userName = row.userName;
    },

  }
};
</script>
