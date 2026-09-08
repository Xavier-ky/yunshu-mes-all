<template>
  <div class="app-container inbound-doc-panel factory-doc-panel">
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        class="inbound-filter-form"
        label-width="0"
        @submit.prevent
      >
        <el-form-item prop="workshopCode">
          <el-input
            v-model="queryParams.workshopCode"
            placeholder="车间编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="workshopName">
          <el-input
            v-model="queryParams.workshopName"
            placeholder="车间名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="enableFlag">
          <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable>
            <el-option
              v-for="dict in dict.type.sys_yes_no"
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
        <span class="inbound-page-title">车间管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:workshop:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:workshop:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:workshop:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:md:workshop:export']">导出</el-button>
      </div>
      <div class="inbound-toolbar-meta">
        <span>共 {{ total }} 条</span>
        <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
      </div>
    </div>

    <div class="inbound-table-frame">
      <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
      <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
      <el-table
        v-loading="loading"
        class="yunshu-data-table inbound-table factory-doc-table"
        stripe
        border
        height="100%"
        :data="workshopList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="车间编码" align="center" header-align="center" min-width="120" prop="workshopCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click="handleView(scope.row)">{{ scope.row.workshopCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="车间名称" align="center" header-align="center" min-width="120" prop="workshopName" show-overflow-tooltip />
        <el-table-column label="面积" align="center" header-align="center" width="88" prop="area" />
        <el-table-column label="负责人" align="center" header-align="center" min-width="100" prop="charge" show-overflow-tooltip />
        <el-table-column label="是否启用" align="center" header-align="center" width="88" prop="enableFlag">
          <template #default="scope">
            <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" header-align="center" min-width="120" prop="remark" show-overflow-tooltip />
        <el-table-column label="操作" min-width="160" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
          <template #default="scope">
            <div class="yunshu-row-actions">
              <el-button size="small" link icon="el-icon-view" @click="handleView(scope.row)" v-hasPermi="['mes:md:workshop:query']">查看</el-button>
              <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:workshop:edit']">修改</el-button>
              <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:workshop:remove']">删除</el-button>
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

    <!-- 添加或修改车间对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="14">
            <el-row>
              <el-col :span="16">
                <el-form-item label="车间编码" prop="workshopCode">
                  <el-input v-model="form.workshopCode" readonly="readonly" maxlength="64" v-if="['view','edit'].indexOf(optType)> -1"/>
                  <el-input v-model="form.workshopCode" placeholder="请输入车间编码" maxlength="64" v-else/>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item  label-width="80">
                  <el-switch v-model="autoGenFlag"
                             active-color="#13ce66"
                             active-text="自动生成"
                             @change="handleAutoGenChange(autoGenFlag)" v-if="['view','edit'].indexOf(optType)< 0">
                  </el-switch>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="车间名称" prop="workshopName">
                  <el-input v-model="form.workshopName" placeholder="请输入车间名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="面积" prop="area">
                  <el-input-number :min="0" :max="99999999" :percision="2" :step="1" v-model="form.area" placeholder="请输入面积" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="负责人" prop="charge">
                  <el-input v-model="form.charge" readonly placeholder="请选择负责人" >
                    <template #append><el-button @click="handleCharge" icon="el-icon-search"></el-button></template>
                  </el-input>
                </el-form-item>
                <UserSingleSelect ref="userSelect" @onSelected="onUserSelected"></UserSingleSelect>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="是否启用" prop="enableFlag">
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
            </el-row>
          </el-col>
          <el-col :span="10">
            <BarcodeImg ref="barcodeImg" :bussinessId="form.workshopId" :bussinessCode="form.workshopCode" barcodeType="WORKSHOP"></BarcodeImg>
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkshop, getWorkshop, delWorkshop, addWorkshop, updateWorkshop } from "@/yunshu-ui/api/mes/md/workshop";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue"
import {getBarcodeUrl} from "@/yunshu-ui/api/mes/wm/barcode";
import {listUser} from "@/yunshu-ui/api/system/user";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";
export default {
  components:{UserSingleSelect, BarcodeImg},
  name: "Workshop",
  dicts: ['sys_yes_no'],
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
      // 车间表格数据
      workshopList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      //二维码查询参数
      barcodeParams: {
        bussinessId: null,
        bussinessCode: null,
        barcodeFormart: 'QR_CODE', //模式二维码
        barcodeType: 'WORKSHOP' //类型
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workshopCode: null,
        workshopName: null,
        area: null,
        charge: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        workshopCode: [
          { required: true, message: "车间编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        workshopName: [
          { required: true, message: "车间名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        charge: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        enableFlag: [
          { required: true, message: "是否启用不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 选择用户 */
    onUserSelected(val) {
      if (val) {
        this.form.charge = val.nickName
        this.form.chargeId = val.userId
      }
    },
    /** 点击负责人输入框 */
    handleCharge() {
      this.$refs.userSelect.showFlag = true;
    },
    /** 查询车间列表 */
    getList() {
      this.loading = true;
      listWorkshop(this.queryParams).then(response => {
        this.workshopList = response.rows;
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
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        area: null,
        charge: null,
        enableFlag: 'Y',
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
      this.ids = selection.map(item => item.workshopId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加车间";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const workshopId = row.workshopId || this.ids
      getWorkshop(workshopId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看车间";
        this.optType = "view";
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
        })
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const workshopId = row.workshopId || this.ids
      getWorkshop(workshopId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改车间";
        this.optType = "edit";
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
        })
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.workshopId != null) {
            updateWorkshop(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addWorkshop(this.form).then(response => {
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
      const workshopIds = row.workshopId || this.ids;
      this.$modal.confirm('是否确认删除车间编号为"' + workshopIds + '"的数据项？').then(function() {
        return delWorkshop(workshopIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('md/workshop/export', {
        ...this.queryParams
      }, `workshop_${new Date().getTime()}.xlsx`)
    },
    //获取二维码地址
    getBarcodeUrl(){
      this.barcodeParams.bussinessId = this.form.workshopId;
      this.barcodeParams.bussinessCode = this.form.workshopCode;
      getBarcodeUrl(this.barcodeParams).then( response =>{
        if(response.data != null){
          this.form.barcodeUrl = response.data.barcodeUrl;//强制刷新DOM
        }
      });
    },
    //自动生成物料编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('WORKSHOP_CODE').then(response =>{
          this.form.workshopCode = response;
        });
      }else{
        this.form.workshopCode = null;
      }

    }
  }
};
</script>
