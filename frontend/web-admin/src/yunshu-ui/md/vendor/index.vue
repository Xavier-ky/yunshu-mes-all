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
        <el-form-item prop="vendorCode">
          <el-input
            v-model="queryParams.vendorCode"
            placeholder="供应商编码"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="vendorName">
          <el-input
            v-model="queryParams.vendorName"
            placeholder="供应商名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="vendorNick">
          <el-input
            v-model="queryParams.vendorNick"
            placeholder="供应商简称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item prop="vendorEn">
          <el-input
            v-model="queryParams.vendorEn"
            placeholder="英文名称"
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
        <span class="inbound-page-title">供应商管理</span>
        <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:vendor:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:vendor:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:vendor:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:md:vendor:export']">导出</el-button>
        <el-button type="info" plain icon="el-icon-upload2" size="default" @click="handleImport" v-hasPermi="['mes:md:vendor:import']">导入</el-button>
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
        :data="vendorList"
        @selection-change="handleSelectionChange"
      >
      <el-table-column type="selection" width="42" align="center" header-align="center" />
      <el-table-column label="供应商编码" align="center" header-align="center" min-width="120" prop="vendorCode" >
          <template #default="scope">
                <button type="button" class="doc-link-btn" @click="handleView(scope.row)" v-hasPermi="['mes:md:vendor:query']">{{ scope.row.vendorCode }}</button>
          </template>
      </el-table-column>
      <el-table-column label="供应商名称" align="center" header-align="center" min-width="140" prop="vendorName" show-overflow-tooltip />
      <el-table-column label="供应商简称" align="center" header-align="center" min-width="100" prop="vendorNick" show-overflow-tooltip />
      <el-table-column label="供应商等级" align="center" header-align="center" width="100" prop="vendorLevel" >
        <template #default="scope">
          <dict-tag :options="dict.type.mes_vendor_level" :value="scope.row.vendorLevel"/>
        </template>
      </el-table-column>
      <el-table-column label="供应商评分" align="center" header-align="center" width="100" prop="vendorScore" />
      <el-table-column label="供应商电话" align="center" header-align="center" min-width="120" prop="tel" show-overflow-tooltip />
      <el-table-column label="是否启用" align="center" header-align="center" width="100" prop="enableFlag" >
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" header-align="center" min-width="120" prop="remark" show-overflow-tooltip />
      <el-table-column label="操作" align="center" header-align="center" min-width="160" class-name="col-actions small-padding fixed-width">
        <template #default="scope">
          <div class="yunshu-row-actions">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:md:vendor:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:md:vendor:remove']"
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

    <!-- 添加或修改供应商对话框 -->
    <el-dialog :title="title" v-model="open" @close="cancel" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="14">
            <el-row>
              <el-col :span="20">
                <el-form-item label="供应商编码" prop="vendorCode">
                  <el-input v-model="form.vendorCode" placeholder="请输入供应商编码" />
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
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="供应商名称" prop="vendorName">
                  <el-input v-model="form.vendorName" placeholder="请输入供应商名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="供应商简称" prop="vendorNick">
                  <el-input v-model="form.vendorNick" placeholder="请输入供应商简称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="供应商英文名称" prop="vendorEn">
                  <el-input v-model="form.vendorEn" placeholder="请输入供应商英文名称" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="10">
            <div class="flex-container">
              <el-image class="barcodeClass" fit="scale-down" :src="form.barcodeUrl">
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </el-image>
            </div>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="24">
            <el-form-item label="供应商简介" prop="vendorDes">
              <el-input v-model="form.vendorDes" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="供应商地址" prop="address">
              <el-input v-model="form.address" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="供应商等级" prop="vendorLevel">
              <el-select v-model="form.vendorLevel" placeholder="请选择供应商等级">
              <el-option
                  v-for="dict in dict.type.mes_vendor_level"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商评分" prop="vendorScore">
              <el-input-number :min="0" :max="100" v-model="form.vendorScore" placeholder="请输入供应商评分" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="供应商官网地址" prop="website">
              <el-input v-model="form.website" placeholder="请输入供应商官网地址" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商邮箱地址" prop="email">
              <el-input v-model="form.email" placeholder="请输入供应商邮箱地址" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商电话" prop="tel">
              <el-input v-model="form.tel" placeholder="请输入供应商电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="联系人1" prop="contact1">
              <el-input v-model="form.contact1" placeholder="请输入联系人1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人1-电话" prop="contact1Tel">
              <el-input v-model="form.contact1Tel" placeholder="请输入联系人1-电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人1-邮箱" prop="contact1Email">
              <el-input v-model="form.contact1Email" placeholder="请输入联系人1-邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="联系人2" prop="contact2">
              <el-input v-model="form.contact2" placeholder="请输入联系人2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人2-电话" prop="contact2Tel">
              <el-input v-model="form.contact2Tel" placeholder="请输入联系人2-电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人2-邮箱" prop="contact2Email">
              <el-input v-model="form.contact2Email" placeholder="请输入联系人2-邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="统一社会信用代码" prop="creditCode">
              <el-input v-model="form.creditCode" placeholder="请输入统一社会信用代码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
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
          <el-col :span="8">
            <el-form-item label="供应商LOGO地址" prop="vendorLogo">
              <el-input v-model="form.vendorLogo" placeholder="请输入供应商LOGO地址" />
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
      <el-tabs type="border-card" v-model="activeName" v-if="optType != 'add'" @tab-click="handleClick">
        <el-tab-pane label="物料清单" name="item">
          <Item ref="itemList" @handleSkip="handleSkip" :optType="optType" :vendorId="form.vendorId"></Item>
        </el-tab-pane>
        <el-tab-pane label="采购记录" name="purchase">
          <Purchase ref="PurchaseList" @handleSkip="handleSkip" :optType="optType" :vendorId="form.vendorId"></Purchase>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>

    <!-- 供应商导入对话框 -->
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
import { listVendor, getVendor, delVendor, addVendor, updateVendor } from "@/yunshu-ui/api/mes/md/vendor";
import { getBarcodeUrl } from "@/yunshu-ui/api/mes/wm/barcode";
import { getToken } from "@/yunshu-ui/utils/auth";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import Item from "./components/item.vue";
import Purchase from "./components/purchase.vue";
export default {
  name: "Vendor",
  dicts: ['sys_yes_no','mes_vendor_level'],
  components: { Item, Purchase },
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
      // 供应商表格数据
      vendorList: [],
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
        url: (import.meta.env.VITE_APP_BASE_API || "/api") + "/mes/md/vendor/importData"
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        vendorEn: null,
        vendorDes: null,
        vendorLogo: null,
        vendorLevel: null,
        vendorScore: null,
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
      //二维码查询参数
      barcodeParams: {
        bussinessId: null,
        bussinessCode: null,
        barcodeFormart: 'QR_CODE', //模式二维码
        barcodeType: 'VENDOR' //类型为供应商
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        vendorCode: [
          { required: true, message: "供应商编码不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        vendorName: [
          { required: true, message: "供应商名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        vendorNick: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        vendorEn: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        enableFlag: [
          { required: true, message: "是否启用不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ],
        vendorDes: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        address: [
          { max: 250, message: "字段过长", trigger: "blur" }
        ],
        website: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        email: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        contact1: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        contact1Tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        contact1Email: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        contact2: [
          { max: 32, message: "字段过长", trigger: "blur" }
        ],
        contact2Tel: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        contact2Email: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        creditCode: [
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        vendorLogo: [
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
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
      if (tab.name == 'item') {
        this.$refs.itemList.handleOpen(this.form.vendorId)
      }
      if (tab.name == 'purchase') {
        this.$refs.PurchaseList.handleOpen(this.form.vendorId)
      }
    },
    /** 查询供应商列表 */
    getList() {
      this.loading = true;
      listVendor(this.queryParams).then(response => {
        this.vendorList = response.rows;
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
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        vendorEn: null,
        vendorDes: null,
        vendorLogo: null,
        vendorLevel: "B",
        vendorScore: null,
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
        barcodeUrl: null,
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
      this.ids = selection.map(item => item.vendorId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加供应商";
      this.optType = "add";
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const vendorId = row.vendorId || this.ids
      getVendor(vendorId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看供应商";
        this.optType = "view";
        this.activeName = "item"
        this.getBarcodeUrl();
        this.$refs.itemList.handleOpen(vendorId)
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const vendorId = row.vendorId || this.ids
      getVendor(vendorId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改供应商";
        this.optType = "edit";
        this.activeName = "item"
        this.getBarcodeUrl();
        this.$refs.itemList.handleOpen(vendorId)
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.vendorId != null) {
            updateVendor(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addVendor(this.form).then(response => {
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
      const vendorIds = row.vendorId || this.ids;
      this.$modal.confirm('是否确认删除供应商编号为"' + vendorIds + '"的数据项？').then(function() {
        return delVendor(vendorIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('/mes/md/vendor/export', {
        ...this.queryParams
      }, `vendor_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "供应商导入";
      this.upload.open = true;
    },
    /**下载导入模板 */
    exportTemplate() {
      this.download('mes/md/vendor/exportTemplate', {
      }, `vendor_template_${new Date().getTime()}.xlsx`)
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('mes/md/vendor/importTemplate', {
      }, `vendor_template_${new Date().getTime()}.xlsx`)
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

    //获取某个供应商的二维码地址
    getBarcodeUrl(){
      this.barcodeParams.bussinessId = this.form.vendorId;
      this.barcodeParams.bussinessCode = this.form.vendorCode;
      getBarcodeUrl(this.barcodeParams).then( response =>{
        if(response.data != null){
          this.form.barcodeUrl = response.data.barcodeUrl;
        }
      });
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('VENDOR_CODE').then(response =>{
          this.form.vendorCode = response;
        });
      }else{
        this.form.vendorCode = null;
      }
    }
  }
};
</script>
<style scoped>
  .barcodeClass {
    width: 200px;
    height: 200px;
    border: 1px dashed;
    position: relative;
    display: inline-block;
  }

  .flex-container{
    display: flex;
    justify-content: center; /* 水平居中 */
    align-items: center; /* 垂直居中 */
  }
</style>
