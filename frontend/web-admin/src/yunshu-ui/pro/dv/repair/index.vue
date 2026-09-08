<template>
  <div class="inbound-doc-panel dv-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="repairCode">
          <el-input v-model="queryParams.repairCode" placeholder="维修单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="repairName">
          <el-input v-model="queryParams.repairName" placeholder="维修单名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="machineryCode">
          <el-input v-model="queryParams.machineryCode" placeholder="设备编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="machineryName">
          <el-input v-model="queryParams.machineryName" placeholder="设备名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="repairResult">
          <el-select v-model="queryParams.repairResult" placeholder="维修结果" clearable>
            <el-option v-for="dict in dict.type.mes_repair_result" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item prop="status">
          <el-select v-model="queryParams.status" placeholder="单据状态" clearable>
            <el-option v-for="dict in dict.type.mes_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <span class="inbound-page-title">维修工单</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:dv:repair:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:dv:repair:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:dv:repair:remove']">删除</el-button>
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
      <el-table v-loading="loading" class="yunshu-data-table inbound-table" stripe border height="100%" :data="repairList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="维修单编号" min-width="120" align="center" header-align="center" prop="repairCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.repairCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="维修单名称" min-width="140" align="center" header-align="center" prop="repairName" show-overflow-tooltip />
        <el-table-column label="设备编码" align="center" header-align="center" min-width="110" prop="machineryCode" show-overflow-tooltip />
        <el-table-column label="设备名称" align="center" header-align="center" min-width="120" prop="machineryName" show-overflow-tooltip />
        <el-table-column label="报修日期" align="center" header-align="center" prop="requireDate" width="110">
          <template #default="scope">
            <span>{{ parseTime(scope.row.requireDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="维修完成日期" align="center" header-align="center" prop="finishDate" width="120">
          <template #default="scope">
            <span>{{ parseTime(scope.row.finishDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="验收日期" align="center" header-align="center" prop="confirmDate" width="110">
          <template #default="scope">
            <span>{{ parseTime(scope.row.confirmDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="维修结果" align="center" header-align="center" width="100" prop="repairResult">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_repair_result" :value="scope.row.repairResult"/>
          </template>
        </el-table-column>
        <el-table-column label="维修人员" align="center" header-align="center" width="100" prop="acceptedBy" show-overflow-tooltip />
        <el-table-column label="验收人员" align="center" header-align="center" width="100" prop="confirmBy" show-overflow-tooltip />
        <el-table-column label="单据状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_order_status" :value="scope.row.status"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" header-align="center" min-width="180" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link @click="handleProcess(scope.row)">处理</el-button>
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:repair:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:dv:repair:remove']">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum" @update:page="queryParams.pageNum = $event"
      :limit="queryParams.pageSize" @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改设备维修单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="维修单编号" prop="repairCode">
              <el-input v-model="form.repairCode" placeholder="请输入维修单编号" />
            </el-form-item>
          </el-col>
           <el-col :span="4">
            <el-form-item  label-width="80">
              <el-switch v-model="autoGenFlag"
                  active-color="#13ce66"
                  active-text="自动生成"
                  @change="handleAutoGenChange(autoGenFlag)" v-if="optType != 'view'">
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="维修单名称" prop="repairName">
              <el-input v-model="form.repairName" placeholder="请输入维修单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="设备编号" prop="machineryCode">
              <el-input v-model="form.machineryCode" placeholder="请选择设备" disabled >
                <el-button style="border-color: #46a6ff; background-color: #46a6ff;color: white;" slot="append" @click="handleMachineryAdd" icon="el-icon-search"></el-button>
              </el-input>
            </el-form-item>
            <MachinerySelectSingle ref="machinerySelect" @onSelected="onMachineryAdd"></MachinerySelectSingle>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设备名称" prop="machineryName">
              <el-input v-model="form.machineryName" placeholder="请选择设备" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌" prop="machineryBrand">
              <el-input v-model="form.machineryBrand" placeholder="请选择设备" disabled/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="报修日期" prop="requireDate">
              <el-date-picker clearable
                v-model="form.requireDate"
                style="width: 187px"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择报修日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="维修完成日期" v-if="form.status =='APPROVING' || form.status=='FINISHED' || form.status=='CONFIRMED' " prop="finishDate">
              <el-date-picker clearable
                v-model="form.finishDate"
                style="width: 187px"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择维修完成日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="验收日期" v-if="form.status=='FINISHED' || form.status=='CONFIRMED' " prop="confirmDate">
              <el-date-picker clearable
                v-model="form.confirmDate"
                style="width: 187px"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择验收日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="维修结果" v-if="form.status =='APPROVING' || form.status=='FINISHED' || form.status=='CONFIRMED' ">
              <el-radio-group v-model="form.repairResult">
                <el-radio
                  v-for="dict in dict.type.mes_repair_result"
                  :key="dict.value"
                  :label="dict.value"
                >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="维修人员" prop="acceptedName" v-if="form.status =='APPROVING' || form.status=='FINISHED' || form.status=='CONFIRMED' ">
              <el-input v-model="form.acceptedName" readonly="readonly"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="验收人员" prop="confirmName" v-if="form.status=='FINISHED' || form.status=='CONFIRMED' ">
              <el-input v-model="form.confirmName" readonly="readonly"/>
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
      <el-divider v-if="form.repairId !=null" content-position="center">维修内容</el-divider>
      <el-card shadow="always" v-if="form.repairId !=null" class="box-card">
        <Repairline ref="line" :repairId="form.repairId" :optType="optType"></Repairline>
      </el-card>

      <template #footer><div class="dialog-footer">
        <el-button type="primary" v-if="form.status =='PREPARE' && optType !='view'" @click="submitForm">提 交</el-button>
        <el-button type="primary" v-if="form.status == 'APPROVING' && optType !='view'" @click="finish">完成维修</el-button>
        <el-button type="success" v-if="form.status == 'FINISHED' && optType !='view'" @click="confirm">验收通过</el-button>
        <el-button type="danger" v-if="form.status == 'FINISHED' && optType !='view'" @click="unconfirm">不通过</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listRepair, getRepair, delRepair, addRepair, updateRepair } from "@/yunshu-ui/api/mes/dv/repair";
import MachinerySelectSingle from "@/yunshu-ui/components/machinerySelect/single.vue";
import Repairline from './line.vue'
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
export default {
  name: "Repair",
  dicts: ['mes_repair_result', 'mes_order_status'],
  components: {Repairline,MachinerySelectSingle},
  inject: {
    goRepairWorkbench: { default: null },
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
      // 设备维修单表格数据
      repairList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        repairCode: null,
        repairName: null,
        machineryId: null,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
        machineryTypeId: null,
        requireDate: null,
        finishDate: null,
        confirmDate: null,
        repairResult: null,
        acceptedBy: null,
        confirmBy: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        repairCode: [
          { required: true, message: "维修单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        repairName: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        machineryId: [
          { required: true, message: "设备ID不能为空", trigger: "blur" }
        ],
        machineryCode: [
          { required: true, message: "设备编码不能为空", trigger: "blur" }
        ],
        machineryName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],
        requireDate: [
          { required: true, message: "请选择报修日期", trigger: "blur" }
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
    /** 查询设备维修单列表 */
    getList() {
      this.loading = true;
      listRepair(this.queryParams).then(response => {
        this.repairList = response.rows;
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
        repairId: null,
        repairCode: null,
        repairName: null,
        machineryId: null,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
        machineryTypeId: null,
        requireDate: null,
        finishDate: null,
        confirmDate: null,
        repairResult: "",
        acceptedBy: null,
        confirmBy: null,
        status: 'PREPARE',
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
      this.autoGenFlag= false;
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
      this.ids = selection.map(item => item.repairId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加设备维修单";
    },
    /** 修改按钮操作 */
    handleView(row) {
      this.reset();
      const repairId = row.repairId || this.ids;
      getRepair(repairId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看维修单";
        this.optType = "view";
      });
    },
    handleProcess(row) {
      if (typeof this.goRepairWorkbench === "function") {
        this.goRepairWorkbench(row);
      } else {
        this.$router.push("/app/equipment/workbench?queue=repair");
      }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const repairId = row.repairId || this.ids
      getRepair(repairId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备维修单";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.repairId != null) {
            updateRepair(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addRepair(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const repairIds = row.repairId || this.ids;
      this.$modal.confirm('是否确认删除设备维修单编号为"' + repairIds + '"的数据项？').then(function() {
        return delRepair(repairIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('dv/repair/export', {
        ...this.queryParams
      }, `repair_${new Date().getTime()}.xlsx`)
    },
    //设备资源选择弹出
    handleMachineryAdd(){
      this.$refs.machinerySelect.handleOpen(this.form.machineryId)
    },
    //设备资源选择回调
    onMachineryAdd(row){
      this.form.machineryId = row.machineryId;
      this.form.machineryTypeId = row.machineryTypeId;
      this.form.machineryCode = row.machineryCode;
      this.form.machineryName = row.machineryName;
      this.form.machineryBrand = row.machineryBrand;
    },
        //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('DV_REPAIR_CODE').then(response =>{
          this.form.repairCode = response;
        });
      }else{
        this.form.repairCode = null;
      }
    }
  }
};
</script>
