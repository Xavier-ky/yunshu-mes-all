<template>
  <div class="app-container mditem-command-center mditem-hub-panel">
    <el-row :gutter="0" class="mditem-hub-row">
      <el-col :span="5" :xs="24" class="mditem-hub-aside">
        <div class="stock-aside-summary">
          <div class="aside-summary-head">
            <span>物料总览</span>
            <button class="aside-reset" type="button" @click="resetScope">全部物料</button>
          </div>
          <div class="aside-stat-grid">
            <div><b>{{ overview.total }}</b><span>物料总数</span></div>
            <div><b>{{ overview.productCount }}</b><span>产品数</span></div>
            <div><b>{{ overview.itemCount }}</b><span>原材料数</span></div>
            <div><b>{{ overview.enabledCount }}</b><span>启用数</span></div>
          </div>
        </div>

        <div class="stock-aside-section">
          <div class="aside-section-title"><span>类型快捷</span><small>产品 / 物料</small></div>
          <div class="stock-category-chips">
            <button type="button" :class="{ active: !queryParams.itemOrProduct }" @click="applyTypeFilter(null)">
              <span>全部</span>
            </button>
            <button
              v-for="dict in dict.type.mes_item_product"
              :key="dict.value"
              type="button"
              :class="{ active: queryParams.itemOrProduct === dict.value }"
              @click="applyTypeFilter(dict.value)"
            >
              <span>{{ dict.label }}</span>
            </button>
          </div>
        </div>

        <div class="stock-aside-section stock-aside-tree stock-aside-tree--fill">
          <div class="aside-section-title"><span>物料分类</span><small>点击筛选</small></div>
          <el-input
            v-model="itemTypeName"
            placeholder="搜索分类"
            clearable
            size="small"
            prefix-icon="el-icon-search"
          />
          <div class="hub-tree-wrap">
            <el-tree
              :data="itemTypeOptions"
              :props="defaultProps"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              ref="tree"
              @node-click="handleNodeClick"
            />
          </div>
        </div>

        <div class="stock-aside-section stock-status-filter">
          <div class="aside-section-title"><span>启用状态</span><small>状态筛选</small></div>
          <div class="status-filter-buttons">
            <button type="button" :class="{ active: !queryParams.enableFlag }" @click="applyEnableFilter(null)">全部</button>
            <button type="button" :class="{ active: queryParams.enableFlag === 'Y' }" @click="applyEnableFilter('Y')">启用</button>
            <button type="button" :class="{ active: queryParams.enableFlag === 'N' }" @click="applyEnableFilter('N')">停用</button>
          </div>
        </div>
      </el-col>

      <el-col :span="19" :xs="24" class="mditem-hub-main">
        <el-form
          :model="queryParams"
          ref="queryForm"
          size="small"
          :inline="true"
          v-show="showSearch"
          class="stock-search-form"
          label-width="0"
          @submit.prevent
        >
          <el-form-item prop="itemCode">
            <el-input
              v-model="queryParams.itemCode"
              placeholder="物料编码"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item prop="itemName">
            <el-input
              v-model="queryParams.itemName"
              placeholder="物料名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="stock-table-toolbar">
          <div class="toolbar-left">
            <span class="toolbar-page-hint">物料清单</span>
            <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:md:mditem:add']">新增</el-button>
            <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:md:mditem:edit']">修改</el-button>
            <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:md:mditem:remove']">删除</el-button>
            <el-button type="info" plain icon="el-icon-upload2" size="default" @click="handleImport" v-hasPermi="['mes:md:mditem:import']">导入</el-button>
            <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:md:mditem:export']">导出</el-button>
          </div>
          <div class="toolbar-meta">
            <span>共 {{ total }} 条 · {{ currentScopeLabel }}</span>
            <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" :columns="columns" />
          </div>
        </div>

        <div class="inbound-table-frame stock-table-frame">
          <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>
          <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>
          <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>
          <span class="frame-corner frame-corner--br" aria-hidden="true"></span>
          <el-table
            v-loading="loading"
            class="yunshu-data-table inbound-table stock-main-table"
            stripe
            border
            height="100%"
            :data="itemList"
            @selection-change="handleSelectionChange"
          >
          <el-table-column type="selection" width="42" align="center" header-align="center" />
          <el-table-column label="物料编码" width="120" align="center" header-align="center" key="itemCode" prop="itemCode" v-if="columns[0].visible">
            <template #default="scope">
              <button type="button" class="doc-link-btn" @click="handleView(scope.row)">{{ scope.row.itemCode }}</button>
            </template>
          </el-table-column>
          <el-table-column label="物料名称" min-width="120" align="center" header-align="center" key="itemName" prop="itemName" v-if="columns[1].visible" show-overflow-tooltip />
          <el-table-column label="规格型号" align="center" header-align="center" key="specification" prop="specification" v-if="columns[2].visible" show-overflow-tooltip />
          <el-table-column label="单位" align="center" header-align="center" key="unitName" prop="unitName" v-if="columns[3].visible" show-overflow-tooltip />
          <el-table-column label="物料/产品" align="center" header-align="center" key="itemOrProduct" prop="itemOrProduct" v-if="columns[4].visible" show-overflow-tooltip>
            <template #default="scope">
              <dict-tag :options="dict.type.mes_item_product" :value="scope.row.itemOrProduct"/>
            </template>
          </el-table-column>
          <el-table-column label="所属分类" align="center" header-align="center" key="itemTypeName" prop="itemTypeName" v-if="columns[5].visible" width="120" show-overflow-tooltip />
          <el-table-column label="是否启用" align="center" header-align="center" width="120">
            <template #default="scope">
              <el-switch
                v-model="scope.row.enableFlag"
                active-text="是"
                inactive-text="否"
                active-value="Y"
                inactive-value="N"
                @change="handleEnableFlagChange(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="设置安全库存" align="center" header-align="center" key="safeStockFlag" v-if="columns[7].visible">
            <template #default="scope">
              <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.safeStockFlag"/>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" header-align="center" prop="createTime" v-if="columns[8].visible" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" header-align="center" width="180" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <div class="yunshu-row-actions">
                <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-if="scope.row.enableFlag == 'N'" v-hasPermi="['mes:md:mditem:edit']">修改</el-button>
                <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-if="scope.row.enableFlag == 'N'" v-hasPermi="['mes:md:mditem:remove']">删除</el-button>
                <el-button size="small" link icon="el-icon-printer" @click="handleHiPrint(scope.row)" v-hasPermi="['mes:md:mditem:print']">标签打印</el-button>
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
      </el-col>
    </el-row>

    <!-- 添加或修改物料产品编码对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
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
              <el-radio-group v-model="form.enableFlag" disabled>
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
          <BatchConfig :itemId="form.itemId"  :itemProductFlag="form.itemOrProduct" :optType="optType"></BatchConfig>
        </el-tab-pane>
        <el-tab-pane label="替代品"></el-tab-pane>
        <el-tab-pane label="SIP">
          <SIPTab :itemId="form.itemId" :optType="optType"></SIPTab>
        </el-tab-pane>
        <el-tab-pane label="SOP">
          <SOPTab :itemId="form.itemId" :optType="optType"></SOPTab>
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div></template>
    </el-dialog>

    <!-- 物料导入对话框 -->
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
              <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的用户数据
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
import { getToken } from "@/utils/auth-session";
import { listMdItem, getMdItem, delMdItem, addMdItem, updateMdItem} from "@/yunshu-ui/api/mes/md/mdItem";
import ItemBom from "./components/itembom.vue";
import SOPTab from "./components/sop.vue"
import SIPTab from "./components/sip.vue"
import { listAllUnitmeasure} from "@/yunshu-ui/api/mes/md/unitmeasure";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { treeselect } from "@/yunshu-ui/api/mes/md/itemtype";
import Treeselect from "@zanmato/vue3-treeselect";
import { getBarcodeUrl } from '@/yunshu-ui/api/mes/wm/barcode';
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue"
import printLabel from "@/yunshu-ui/components/printerLabel/index.vue"
import BatchConfig from "./components/batch.vue";

export default {
  name: "MdItem",
  dicts: ['sys_yes_no','mes_item_product'],
  components: { Treeselect,ItemBom,SOPTab,SIPTab,BarcodeImg,printLabel, BatchConfig },
  data() {
    return {
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
      // 物料产品表格数据
      itemList: [],
      // 弹出层标题
      title: "",
      // 部门树选项
      itemTypeOptions: [],
      // 是否显示弹出层
      open: false,
      //弹框的操作类型 view add edit
      optType: undefined,
      // 部门名称
      itemTypeName: undefined,
      //自动生成物料编码标识
      autoGenFlag: false,
      // 日期范围
      dateRange: [],
      //单位列表
      measureOptions: [],
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label"
      },
      // 用户导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的用户数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: "/api/mes/md/mditem/importData"
      },
      //二维码查询参数
      barcodeParams: {
        bussinessId: null,
        bussinessCode: null,
        barcodeFormart: 'QR_CODE', //模式二维码
        barcodeType: 'ITEM' //类型
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemCode: undefined,
        itemName: undefined,
        itemTypeId: 0,
        itemOrProduct: undefined,
        enableFlag: undefined,
      },
      overview: {
        total: 0,
        productCount: 0,
        itemCount: 0,
        enabledCount: 0,
      },
      // 列信息
      columns: [
        { key: 0, label: `物料/产品编码`, visible: true },
        { key: 1, label: `物料/产品名称`, visible: true },
        { key: 2, label: `规格型号`, visible: true },
        { key: 3, label: `单位`, visible: true },
        { key: 4, label: `物料/产品`, visible: true },
        { key: 5, label: `物料分类`, visible: true },
        { key: 6, label: `是否启用`, visible: true },
        { key: 7, label: `是否设置安全库存`, visible: true },
        { key: 8, label: `创建时间`, visible: true }
      ],
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
  watch: {
    // 根据名称筛选分类树
    itemTypeName(val) {
      this.$refs.tree?.filter(val);
    }
  },
  computed: {
    currentScopeLabel() {
      const parts = [];
      if (this.queryParams.itemTypeId && this.queryParams.itemTypeId !== 0) {
        parts.push("已选分类");
      }
      if (this.queryParams.itemOrProduct) {
        const hit = (this.dict.type.mes_item_product || []).find((d) => d.value === this.queryParams.itemOrProduct);
        parts.push(hit?.label || this.queryParams.itemOrProduct);
      }
      if (this.queryParams.enableFlag === "Y") parts.push("启用");
      if (this.queryParams.enableFlag === "N") parts.push("停用");
      return parts.length ? parts.join(" · ") : "全部范围";
    },
  },
  created() {
    this.getList();
    this.getTreeselect();
    this.getUnits();
    this.loadOverview();
  },
  methods: {
    // 使用HiPrint打印
    handleHiPrint() {
      this.$modal.msgWarning("标签打印待对接");
    },
    /** 查询物料编码列表 */
    getList(pagination) {
      if (pagination && typeof pagination === "object") {
        this.queryParams.pageNum = pagination.page;
        this.queryParams.pageSize = pagination.limit;
      }
      this.loading = true;
      listMdItem(this.queryParams).then(response => {
        this.itemList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      }).catch(() => {
        this.itemList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    loadOverview() {
      listMdItem({ pageNum: 1, pageSize: 500 }).then((response) => {
        const rows = response.rows || [];
        this.overview = {
          total: response.total ?? rows.length,
          productCount: rows.filter((r) => r.itemOrProduct === "PRODUCT").length,
          itemCount: rows.filter((r) => r.itemOrProduct === "ITEM").length,
          enabledCount: rows.filter((r) => r.enableFlag === "Y").length,
        };
      }).catch(() => {});
    },
    applyTypeFilter(value) {
      this.queryParams.itemOrProduct = value || undefined;
      this.handleQuery();
    },
    applyEnableFilter(value) {
      this.queryParams.enableFlag = value || undefined;
      this.handleQuery();
    },
    resetScope() {
      this.queryParams.itemTypeId = 0;
      this.queryParams.itemOrProduct = undefined;
      this.queryParams.enableFlag = undefined;
      this.itemTypeName = undefined;
      this.$refs.tree?.setCurrentKey(null);
      this.handleQuery();
    },
    getUnits(){
      listAllUnitmeasure().then(response =>{
        this.measureOptions = response.data;
      });
    },
    /** 查询分类下拉树结构 */
    getTreeselect() {
      treeselect().then(response => {
        this.itemTypeOptions = response.data;
      });
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.queryParams.itemTypeId = data.id;
      this.handleQuery();
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        itemId: undefined,
        itemTypeId: undefined,
        itemCode: undefined,
        itemName: undefined,
        specification: undefined,
        unitOfMeasrue: undefined,
        unitName: undefined,
        enableFlag: undefined,
        itemOrProduct: undefined,
        enableFlag: 'N',
        safeStockFlag: 'N',
        highValue: 'N',
        batchFlag: 'Y',
        barcodeUrl: null,
        minStock: 0,
        maxStock: 0,
        optType: undefined,
        remark: undefined
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
      this.queryParams.itemTypeId = 0;
      this.queryParams.itemOrProduct = undefined;
      this.queryParams.enableFlag = undefined;
      this.$refs.tree?.setCurrentKey(null);
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.itemId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      this.getTreeselect();
      const itemId = row.itemId || this.ids;
      getMdItem(itemId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看物料/产品";
        this.optType = "view";
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
        })
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.getTreeselect();
      if (this.queryParams.itemTypeId != 0) {
        this.form.itemTypeId = this.queryParams.itemTypeId;
      }
      this.optType = "add";
      this.open = true;
      this.title = "新增物料/产品";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      const itemId = row.itemId || this.ids;
      getMdItem(itemId).then(response => {
        this.form = response.data;
        this.open = true;
        this.optType = "edit";
        this.title = "修改物料/产品";
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
        })
      });
    },
    /**
     * 启用状态变更
     * @param row
     */
     handleEnableFlagChange(row){
      let text = row.enableFlag === "N" ? "禁用" : "启用";
      this.$modal.confirm('确认要"' + text + '""' + row.itemName + '"物料吗？').then(function() {
        return updateMdItem(row);
      }).then(() => {
        this.$modal.msgSuccess(text + "成功");
      }).catch(function() {
        row.enableFlag = row.enableFlag === "N" ? "Y" : "N";
      });
    },

    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.itemId != undefined) {
            updateMdItem(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.form = response.data;
              this.getList();
            });
          } else {
            addMdItem(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.form = response.data;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const itemIds = row.itemId || this.ids;
      this.$modal.confirm('确认删除数据项？').then(function() {
        return delMdItem(itemIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/md/mditem/export', {
        ...this.queryParams
      }, `md_item_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "物料/产品导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('mes/md/mditem/importTemplate', {
      }, `md_item_template${new Date().getTime()}.xlsx`)
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
    //获取二维码地址
    getBarcodeUrl(){
      this.barcodeParams.bussinessId = this.form.itemId;
      this.barcodeParams.bussinessCode = this.form.itemCode;
      getBarcodeUrl(this.barcodeParams).then( response =>{
        if(response.data != null){
          this.form.barcodeUrl = response.data.barcodeUrl;//强制刷新DOM
        }
      });
    },
    //自动生成物料编码
    handleAutoGenChange(autoGenFlag){
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
