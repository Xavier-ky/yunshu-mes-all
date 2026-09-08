<template>
    <div class="app-container">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="图表编号" prop="chartCode">
          <el-input
            v-model="queryParams.chartCode"
            placeholder="请输入图表编号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="图表名称" prop="chartName">
          <el-input
            v-model="queryParams.chartName"
            placeholder="请输入图表名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="业务模块" prop="businessType">
          <el-select v-model="queryParams.businessType" placeholder="请选择业务模块" clearable>
            <el-option
                        v-for="dict in dict.type.mes_model_type"
                        :key="dict.value"
                        :label="dict.label"
                        :value="dict.value"
                      ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态" prop="enableFlag">
          <el-select v-model="queryParams.enableFlag" placeholder="请选择" clearable>
            <el-option
                        v-for="dict in dict.type.sys_yes_no"
                        :key="dict.value"
                        :label="dict.label"
                        :value="dict.value"
                      ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['template:template:add']"
          >新增</el-button>
        </el-form-item>
      </el-form>
  
      <el-row>
        <el-col
          :span="6"
          style="margin-bottom: 10px"
          :offset="1"
        >
          <el-card class="chartCard" >
            <div @click="handleAdd()" style="text-align: center; margin-top: 150px;">
              <svg-icon icon-class="add" style="width: 100px; height: 100px;"></svg-icon>
            </div>
          </el-card>
        </el-col>
        <el-col
          v-for="item in chartList"
          :key="item.chartId"
          :span="6"
          style="margin-bottom: 10px"
          :offset="1"
        >
          <el-card class="chartCard">
            <div slot="header" class="clearfix">
              <span style="margin-left: 5px; font-weight: bold;">{{
                item.chartName
              }}</span>
              <div
                style="display: inline-block; float: right; cursor: pointer"
                @click="handleDelete(item)"
              >
                <el-tooltip effect="dark" content="删除图表" placement="top">
                  <i class="el-icon-delete" style="margin-left: 15px" />
                </el-tooltip>
              </div>
              <div
                style="display: inline-block; float: right; cursor: pointer"
                @click="handleEdit(item)"
              >
                <el-tooltip effect="dark" content="编辑图表" placement="top">
                  <i class="el-icon-edit-outline" style="margin-left: 15px" />
                </el-tooltip>
              </div>
            </div>
            <div style="margin-bottom: 10px; width : 100%; height: 100%;">
              <chart :chartOptions="item.options" :api="item.api"></chart>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <!-- 添加或修改打印模板配置对话框 -->
      <el-dialog :title="title" v-model="open" width="960px" append-to-body>
        <el-form ref="form" :model="form" :rules="rules" label-width="100px">
          <el-row>
            <el-col :span="8">
              <el-form-item label="图表编号" prop="chartCode">
                <el-input v-model="form.chartCode" placeholder="请输入图表编号" />
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
            <el-col :span=12>
              <el-form-item label="图表名称" prop="chartName">
                <el-input v-model="form.chartName" placeholder="请输入图表名称" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="业务模块" prop="businessType">
                <el-select v-model="form.businessType" placeholder="请选择业务模块" style="width: 100%">
                  <el-option
                        v-for="dict in dict.type.mes_model_type"
                        :key="dict.value"
                        :label="dict.label"
                        :value="dict.value"
                      ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角色">
                <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
                  <el-option
                    v-for="item in roleOptions"
                    :key="item.roleId"
                    :label="item.roleName"
                    :value="item.roleId"
                    :disabled="item.status == 1"
                  ></el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="接口地址" prop="api">
                <el-input v-model="form.api" placeholder="请输入接口地址" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="图表参数" prop="options">
                <el-input v-model="form.options" rows="20" type="textarea"  placeholder="请输入options内容" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="图表说明" prop="remark">
                <el-input v-model="form.remark" type="textarea" placeholder="请输入备注说明" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="8">
              <el-form-item label="启用状态" prop="enableFlag">
                <el-radio-group v-model="form.enableFlag">
                    <el-radio
                      v-for="dict in dict.type.sys_yes_no"
                      :key="dict.value"
                      :label="dict.value"
                    >{{dict.label}}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              
            </el-col>
          </el-row>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </el-dialog>
    </div>
  </template>
  
  <script>
  import { listChart,getChart,addChart,updateChart,delChart } from "@/yunshu-ui/api/mes/report/chart";
  import { listRole } from "@/api/system/role";
  import {genCode} from "@/api/system/autocode/rule"
  import chart from './chart'
  export default {
    name: "Template",
    components: {
      chart
    },
    dicts: ['mes_model_type','sys_yes_no'],
    data() {
      return {
        //自动生成编码
        autoGenFlag:false,
        optType: undefined,
        observer: null,
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
        // 角色选项
        roleOptions: [],
        // 表格数据
        chartList: [],
        // 弹出层标题
        title: "",
        // 是否显示弹出层
        open: false,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          chartCode: null,        chartName: null,        chartType: null,        businessType: null,        api: null,        options: null,        chartPic: null,        enableFlag: null,        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null},
        // 表单参数
        form: {},
        // 表单校验
        rules: {
          chartCode: [
            { required: true, message: "图表编号不能为空", trigger: "blur" }
          ],
          chartName: [
            { required: true, message: "图表名称不能为空", trigger: "blur" }
          ],        
          api: [
            { required: true, message: "接口地址不能为空", trigger: "change" }
          ],      
          businessType: [
            { required: true, message: "请选择业务类型", trigger: "change" }
          ],
          options: [
            { required: true, message: "请填写options内容", trigger: "change" }
          ]
        },
      };
    },
    created() {
      this.getRoles();
      this.getList();
    },
    methods: {
      getRoles(){
        listRole().then(response => {
          this.roleOptions = response.rows;        
        })
      },
      getList() {
        this.loading = true;
        listChart(this.queryParams).then(response => {
          this.chartList = response.rows.map(item => {
            if(typeof item.options === 'string'){
              try{
                item.options = JSON.parse(item.options);
              } catch(e) {
                console.log(e);
                item.options = {};
              }
            }
            return item;
          });
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
          chartId: null,
          chartCode: null,
          chartName: null,
          chartType: null,
          businessType: null,
          api: null,
          options: null,
          chartPic: null,
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
  
      /** 新增按钮操作 */
      handleAdd() {
        this.reset();
        this.open = true;
        this.optType = 'add';
        this.title = "添加图表";
      },
      handleEdit(item){
        this.reset();
        getChart(item.chartId).then(response => {
          this.form = response.data;
          this.form.roleIds = response.roleIds;
          this.optType = 'edit';
        })
        this.open = true;
        this.title = "添加图表";
      },
      /** 删除按钮操作 */
      handleDelete(item) {
        const chartIds = item.chartId || this.ids;
        this.$modal.confirm('是否确认删除编号为"' + chartIds + '"的数据项？').then(function() {
          return delChart(chartIds);
        }).then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        }).catch(() => {});
      },
      /** 提交按钮 */
      submitForm() {
        this.$refs["form"].validate(valid => {
          if (valid) {
              if(!this.form.chartId){
                  addChart(this.form).then(response => {
                    this.$modal.msgSuccess("新增成功");
                    this.open = false;
                    this.form.chartId = response.data.chartId;
                    this.handleEdit(this.form)
                  });
              }else{
                updateChart(this.form).then(response => {
                  this.$modal.msgSuccess("修改成功");
                  this.open = false;
                  this.getList();
                });
              }
          }
        });
      },
      //自动生成模板编码
      handleAutoGenChange(autoGenFlag){
        if(autoGenFlag){
          genCode('REPORT_CHART_CODE').then(response =>{
            this.form.chartCode = response;
          });
        }else{
          this.form.chartCode = null;
        }
      }
    }
  };
  </script>
  <style>
  
  .cardGroup {
    margin-top: 50px;
    display: flex;
    height: 400px;
  }
  
  
  
  .chartCard {
    border: solid 1px;
    width: 500px;
    height: 400px;
    cursor: pointer;
    font-size: 12px;
    position: relative;
    overflow: hidden;
    color: #666;
    background: #fff;
    box-shadow: 4px 4px 40px rgba(0, 0, 0, .05);
    border-color: rgba(0, 0, 0, .05);
    border-radius: 10px;
  }
  
  .cardHeader {
    float: right;
    padding: 2px 0;
  }
  
  </style>
  
  