<template>
  <div class="app-container inbound-doc-panel md-doc-panel">
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
        <el-form-item prop="clientCode">
          <el-input
            v-model="queryParams.clientCode"
            placeholder="客户编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="clientName">
          <el-input
            v-model="queryParams.clientName"
            placeholder="客户名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="clientNick">
          <el-input
            v-model="queryParams.clientNick"
            placeholder="客户简称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="clientEn">
          <el-input
            v-model="queryParams.clientEn"
            placeholder="客户英文名称"
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
        <span class="inbound-page-title">客户管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:client:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:client:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:client:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:md:client:export']">导出</el-button>
        <el-button type="info" plain icon="el-icon-upload2" size="default" @click="handleImport" v-hasPermi="['mes:md:client:import']">导入</el-button>
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
        class="yunshu-data-table inbound-table md-doc-table"
        stripe
        border
        height="100%"
        :data="clientList"
        @selection-change="handleSelectionChange"
      >
      <el-table-column type="selection" width="42" align="center" header-align="center" />
      <el-table-column label="客户编码" align="center" header-align="center" min-width="120" prop="clientCode" >
         <template #default="scope">
                <button type="button" class="doc-link-btn" @click="handleView(scope.row)" v-hasPermi="['mes:md:client:query']">{{ scope.row.clientCode }}</button>
          </template>
      </el-table-column>
      <el-table-column label="客户名称" min-width="140" align="center" header-align="center" prop="clientName" show-overflow-tooltip />
      <el-table-column label="客户简称" align="center" header-align="center" min-width="100" prop="clientNick" show-overflow-tooltip />
      <el-table-column label="客户类型" align="center" header-align="center" width="100" prop="clientType" >
        <template #default="scope">
          <dict-tag :options="dict.type.mes_client_type" :value="scope.row.clientType"/>
        </template>
      </el-table-column>
      <el-table-column label="客户电话" align="center" header-align="center" min-width="120" prop="tel" show-overflow-tooltip />
      <el-table-column label="联系人" align="center" header-align="center" min-width="100" prop="contact1" show-overflow-tooltip />
      <el-table-column label="联系人-电话" align="center" header-align="center" min-width="120" prop="contact1Tel" show-overflow-tooltip />
      <el-table-column label="是否启用" align="center" header-align="center" width="100" prop="enableFlag" >
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" header-align="center" min-width="160" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:md:client:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:md:client:remove']"
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

    <!-- 添加或修改客户对话框 -->
    <el-dialog :title="title" v-model="open" @close="cancel" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户编码" prop="clientCode">
              <el-input v-model="form.clientCode" placeholder="请输入客户编码" />
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
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户简称" prop="clientNick">
              <el-input v-model="form.clientNick" placeholder="请输入客户简称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户英文名称" prop="clientEn">
              <el-input v-model="form.clientEn" placeholder="请输入客户英文名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户类型" prop="clientType">
              <el-select v-model="form.clientType" placeholder="请选择客户类型">
              <el-option
                  v-for="dict in dict.type.mes_client_type"
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
            <el-form-item label="客户简介" prop="clientDes">
              <el-input v-model="form.clientDes" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="客户地址" prop="address">
              <el-input v-model="form.address" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="客户官网地址" prop="website">
              <el-input v-model="form.website" placeholder="请输入客户官网地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户邮箱地址" prop="email">
              <el-input v-model="form.email" v-if="optType=='view'" />
              <el-input v-model="form.email" placeholder="请输入客户邮箱地址" v-else/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="客户电话" prop="tel">
              <el-input v-model="form.tel" v-if="optType=='view'"  />
              <el-input v-model="form.tel" placeholder="请输入客户电话" v-else/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户LOGO" prop="clientLogo">
              <el-input v-model="form.clientLogo" v-if="optType=='view'" />
              <el-input v-model="form.clientLogo" placeholder="请输入客户LOGO" v-else/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="联系人1" prop="contact1">
              <el-input v-model="form.contact1" v-if="optType=='view'" />
              <el-input v-model="form.contact1" placeholder="请输入联系人1" v-else/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人1-电话" prop="contact1Tel">
              <el-input v-model="form.contact1Tel" v-if="optType=='view'" />
              <el-input v-model="form.contact1Tel" placeholder="请输入联系人1-电话" v-else />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人1-邮箱" prop="contact1Email">
              <el-input v-model="form.contact1Email" v-if="optType=='view'" />
              <el-input v-model="form.contact1Email" placeholder="请输入联系人1-邮箱" v-else/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="联系人2" prop="contact2">
              <el-input v-model="form.contact2" v-if="optType=='view'" />
              <el-input v-model="form.contact2" placeholder="请输入联系人2" v-else />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人2-电话" prop="contact2Tel">
              <el-input v-model="form.contact2Tel" v-if="optType=='view'" />
              <el-input v-model="form.contact2Tel" placeholder="请输入联系人2-电话"  v-else/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人2-邮箱" prop="contact2Email">
              <el-input v-model="form.contact2Email" v-if="optType=='view'" />
              <el-input v-model="form.contact2Email" placeholder="请输入联系人2-邮箱" v-else/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="社会信用代码" prop="creditCode">
              <el-input v-model="form.creditCode" readonly v-if="optType=='view'" />
              <el-input v-model="form.creditCode" placeholder="请输入统一社会信用代码" v-else />
            </el-form-item>
          </el-col >
          <el-col :span="12">
            <el-form-item label="是否有效" prop="enableFlag">
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
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" readonly v-if="optType=='view'" />
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" v-else />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card" v-model="activeName" v-if="optType != 'add'" @tab-click="handleClick">
        <el-tab-pane label="产品清单" name="item">
          <Item ref="itemList" @handleSkip="handleSkip" :optType="optType" :clientId="form.clientId"></Item>
        </el-tab-pane>
        <el-tab-pane label="销售记录" name="salesRecord">
          <SalesRecord ref="salesRecordList" @handleSkip="handleSkip" :optType="optType" :clientId="form.clientId"></SalesRecord>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>

    <!-- 客户导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
        </template>
      </el-upload>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div></template>
    </el-dialog>


  </div>
</template>

<script>
import { listClient, getClient, delClient, addClient, updateClient } from "@/yunshu-ui/api/mes/md/client";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { getToken } from "@/yunshu-ui/utils/auth";
import SalesRecord from "./components/salesRecord.vue";
import Item from "./components/item.vue";
export default {
  name: "Client",
  components: { Item, SalesRecord },
  dicts: ['mes_client_type','sys_yes_no'],
  data() {
    return {
      activeName: "item",
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
      // 客户表格数据
      clientList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 数据导入参数
      upload: {
        // 是否显示弹出层
        open: false,
        // 弹出层标题
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: (import.meta.env.VITE_APP_BASE_API || "/api") + "/mes/md/client/importData"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        clientCode: null,
        clientName: null,
        clientNick: null,
        clientEn: null,
        clientDes: null,
        clientLogo: null,
        clientType: null,
        address: null,
        website: null,
        email: null,
        tel: null,
        contact1: null,
        contact1Tel: null,
        contact1Email: null,
        contact2: null,
        contact2Tel: null,
        contact2Email: null,
        creditCode: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        clientCode: [
          { required: true, message: "客户编码不能为空", trigger: "blur" }
        ],
        clientName: [
          { required: true, message: "客户名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        clientNick: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        clientEn: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        address: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        website: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        clientLogo: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        contact1: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        contact1Tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        contact2: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        contact2Tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        creditCode: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        clientDes: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        email:[
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        contact1Email:[
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        contact2Email:[
          {
            type: "email",
            message: "请输入正确的邮箱地址",
            trigger: ["blur", "change"]
          }
        ],
        enableFlag: [
          { required: true, message: "是否启用不能为空", trigger: "blur" }
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
    handleSkip() {
      this.cancel()
    },
    handleClick(tab) {
      if (tab.name === 'item') {
        this.$refs.itemList.handleOpen(this.form.clientId)
      }
      if (tab.name === 'salesRecord') {
        this.$refs.salesRecordList.handleOpen(this.form.clientId)
      }
    },
    /** 查询客户列表 */
    getList() {
      this.loading = true;
      listClient(this.queryParams).then(response => {
        this.clientList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.activeName = "item"
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        clientId: null,
        clientCode: null,
        clientName: null,
        clientNick: null,
        clientEn: null,
        clientDes: null,
        clientLogo: null,
        clientType: null,
        address: null,
        website: null,
        email: null,
        tel: null,
        contact1: null,
        contact1Tel: null,
        contact1Email: null,
        contact2: null,
        contact2Tel: null,
        contact2Email: null,
        creditCode: null,
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
      this.ids = selection.map(item => item.clientId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加客户";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const clientId = row.clientId || this.ids
      getClient(clientId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看客户";
        this.optType = "view";
        this.activeName = 'item'
        this.$refs.itemList.handleOpen(this.form.clientId)
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const clientId = row.clientId || this.ids
      getClient(clientId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改客户";
        this.optType = "edit";
        this.activeName = 'item'
        this.$refs.itemList.handleOpen(this.form.clientId)
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.clientId != null) {
            updateClient(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addClient(this.form).then(response => {
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
      const clientIds = row.clientId || this.ids;
      this.$modal.confirm('是否确认删除客户编号为"' + clientIds + '"的数据项？').then(function() {
        return delClient(clientIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('/mes/md/client/export', {
        ...this.queryParams
      }, `client_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "客户导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('mes/md/client/importTemplate', {
      }, `client_template_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },

    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('CLIENT_CODE').then(response =>{
          this.form.clientCode = response;
        });
      }else{
        this.form.clientCode = null;
      }
    }
  }
};
</script>
