<template>
  <div class="app-container dv-machinery-page machinery-command-center machinery-hub-panel">
    <div class="dv-machinery-page__head">
      <h2 class="dv-machinery-page__title">设备台账</h2>
      <el-button size="default" @click="manufacturerDrawer = true">制造商配置</el-button>
    </div>

    <el-row :gutter="0" class="machinery-hub-row">
      <el-col :span="5" :xs="24" class="machinery-hub-aside">
        <div class="stock-aside-summary">
          <div class="aside-summary-head">
            <span>设备总览</span>
            <button class="aside-reset" type="button" @click="resetScope">全部设备</button>
          </div>
          <div class="aside-stat-grid">
            <div><b>{{ overview.total }}</b><span>设备总数</span></div>
            <div><b>{{ overview.runningCount }}</b><span>运行 / 正常</span></div>
            <div><b>{{ overview.stopCount }}</b><span>停机</span></div>
            <div><b>{{ overview.maintCount }}</b><span>维修中</span></div>
          </div>
        </div>

        <div class="stock-aside-section machinery-status-section">
          <div class="aside-section-title"><span>状态快捷</span><small>设备运行状态</small></div>
          <div class="machinery-status-grid">
            <button
              type="button"
              class="machinery-status-btn is-all"
              :class="{ active: !queryParams.status }"
              @click="applyStatusFilter(null)"
            >
              <span class="machinery-status-btn__label">全部状态</span>
              <span class="machinery-status-btn__count">{{ overview.total }}</span>
            </button>
            <button
              v-for="dict in statusFilterItems"
              :key="dict.value"
              type="button"
              class="machinery-status-btn"
              :class="[statusTone(dict.value), { active: queryParams.status === dict.value }]"
              @click="applyStatusFilter(dict.value)"
            >
              <span class="machinery-status-btn__dot" aria-hidden="true"></span>
              <span class="machinery-status-btn__label">{{ dict.label }}</span>
              <span class="machinery-status-btn__count">{{ statusCount(dict.value) }}</span>
            </button>
          </div>
        </div>

        <div class="stock-aside-section stock-aside-tree stock-aside-tree--fill machinery-type-section">
          <div class="aside-section-title">
            <span>设备分类</span>
            <small>{{ typeTreeCount }} 个分类</small>
          </div>

          <div v-if="selectedTypeNode" class="machinery-type-selected">
            <span class="machinery-type-selected__tag">当前选中</span>
            <span class="machinery-type-selected__name" :title="selectedTypeNode.machineryTypeName">
              {{ selectedTypeNode.machineryTypeName }}
            </span>
            <button type="button" class="machinery-type-selected__clear" title="清除选中" @click="clearTypeSelection">×</button>
          </div>
          <div v-else class="machinery-type-selected is-empty">
            <span>点击分类节点筛选设备</span>
          </div>

          <div class="machinery-type-tree-toolbar">
            <el-input
              v-model="machineryTypeName"
              placeholder="搜索分类名称 / 编码"
              clearable
              size="small"
              prefix-icon="el-icon-search"
            />
            <div class="machinery-type-tree-actions">
              <button type="button" @click="expandAllTypes">展开</button>
              <button type="button" @click="collapseAllTypes">收起</button>
            </div>
          </div>

          <div class="machinery-type-tree-panel">
            <div class="hub-tree-wrap">
              <el-tree
                v-if="machineryTypeOptions.length"
                :data="machineryTypeOptions"
                :props="defaultProps"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                default-expand-all
                highlight-current
                node-key="machineryTypeId"
                @node-click="handleNodeClick"
              >
                <template #default="{ data }">
                  <div class="machinery-tree-node" :class="{ 'is-current': selectedTypeNode?.machineryTypeId === data.machineryTypeId }">
                    <span class="machinery-tree-node__name">{{ data.machineryTypeName }}</span>
                    <span v-if="data.machineryTypeCode" class="machinery-tree-node__code">{{ data.machineryTypeCode }}</span>
                  </div>
                </template>
              </el-tree>
              <div v-else class="machinery-type-tree-empty">暂无分类，请点击下方新增</div>
            </div>
          </div>

          <div class="machinery-type-footer">
            <el-button size="small" type="primary" plain @click="handleTypeAdd()">新增</el-button>
            <el-button size="small" plain :disabled="!selectedTypeNode" @click="handleTypeEdit">编辑</el-button>
            <el-button
              size="small"
              plain
              type="danger"
              :disabled="!selectedTypeNode || selectedTypeNode.parentTypeId === 0"
              @click="handleTypeDelete"
            >删除</el-button>
          </div>
        </div>
      </el-col>

      <el-col :span="19" :xs="24" class="machinery-hub-main">
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
          <el-form-item prop="machineryCode">
            <el-input
              v-model="queryParams.machineryCode"
              placeholder="设备编码"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item prop="machineryName">
            <el-input
              v-model="queryParams.machineryName"
              placeholder="设备名称"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item prop="workshopId">
            <el-select v-model="queryParams.workshopId" placeholder="所属车间" clearable>
              <el-option
                v-for="item in workshopOptions"
                :key="item.workshopId"
                :label="item.workshopName"
                :value="item.workshopId"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="stock-table-toolbar">
          <div class="toolbar-left">
            <span class="toolbar-page-hint">设备清单</span>
            <el-button type="primary" plain icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:dv:machinery:add']">新增</el-button>
            <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:dv:machinery:edit']">修改</el-button>
            <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:dv:machinery:remove']">删除</el-button>
            <el-button type="info" plain icon="el-icon-upload2" size="default" @click="handleImport" v-hasPermi="['mes:dv:machinery:import']">导入</el-button>
            <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:dv:machinery:export']">导出</el-button>
          </div>
          <div class="toolbar-meta">
            <span>共 {{ total }} 条 · {{ currentScopeLabel }}</span>
            <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList" />
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
            :data="machineryList"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="42" align="center" />
            <el-table-column label="设备编码" min-width="120" align="center" key="machineryCode" prop="machineryCode">
              <template #default="scope">
                <button
                  type="button"
                  class="doc-link-btn"
                  @click="handleView(scope.row)"
                  v-hasPermi="['mes:dv:machinery:query']"
                >{{ scope.row.machineryCode }}</button>
              </template>
            </el-table-column>
            <el-table-column label="设备名称" min-width="120" align="left" key="machineryName" prop="machineryName" show-overflow-tooltip />
            <el-table-column label="品牌" min-width="100" align="left" key="machineryBrand" prop="machineryBrand" show-overflow-tooltip />
            <el-table-column label="规格型号" min-width="120" align="left" key="machinerySpec" prop="machinerySpec" show-overflow-tooltip />
            <el-table-column label="所属车间" min-width="100" align="center" key="workshopName" prop="workshopName" show-overflow-tooltip />
            <el-table-column label="设备状态" min-width="90" align="center" key="status" prop="status">
              <template #default="scope">
                <dict-tag :options="dict.type.mes_machinery_status || []" :value="scope.row.status"/>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" min-width="160" show-overflow-tooltip>
              <template #default="scope">
                <span>{{ parseTime(scope.row.createTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" min-width="200" class-name="col-actions">
              <template #default="scope">
                <div class="yunshu-row-actions">
                  <el-button type="primary" link size="default" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:dv:machinery:edit']">修改</el-button>
                  <el-button type="primary" link size="default" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:dv:machinery:remove']">删除</el-button>
                  <el-button type="primary" link size="default" icon="el-icon-printer" @click="handleHiPrint(scope.row)" v-hasPermi="['mes:dv:machinery:print']">标签打印</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          :page="queryParams.pageNum"
          @update:page="queryParams.pageNum = $event"
          :limit="queryParams.pageSize"
          @update:limit="queryParams.pageSize = $event"
          @pagination="getList"
        />
      </el-col>
    </el-row>

    <!-- 添加或修改设备对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="14">
            <el-row>
              <el-col :span="16">
                <el-form-item label="设备编码" prop="machineryCode">
                  <el-input v-model="form.machineryCode" :disabled="optType != 'add'" readonly="readonly" maxlength="64" v-if="['view','edit'].indexOf(optType)> -1"/>
                  <el-input v-model="form.machineryCode" :disabled="optType != 'add'" placeholder="请输入设备编码" maxlength="64" v-else/>
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
                <el-form-item label="设备名称" prop="machineryName">
                  <el-input v-model="form.machineryName"  maxlength="255" readonly="readonly" v-if="optType=='view'" />
                  <el-input v-model="form.machineryName" placeholder="请输入设备名称" maxlength="255" v-else/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="品牌" prop="machineryBrand">
                  <el-input v-model="form.machineryBrand"  maxlength="255" readonly="readonly" v-if="optType=='view'" />
                  <el-input v-model="form.machineryBrand"  placeholder="请输入品牌" maxlength="255" v-else/>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item  label="设备分类" prop="machineryTypeId">
                  <treeselect v-model="form.machineryTypeId" :options="machineryTypeOptions" :normalizer="normalizer" disabled v-if="optType=='view'"  />
                  <treeselect v-model="form.machineryTypeId" :options="machineryTypeOptions" :normalizer="normalizer" placeholder="请选择所属分类" v-else :disable-branch-nodes='true' />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="所属车间" prop="workshopId">
                  <el-select v-model="form.workshopId" @change="changeWorkshop" placeholder="请选择车间">
                    <el-option
                        v-for="item in workshopOptions"
                        :key="item.workshopId"
                        :label="item.workshopName"
                        :value="item.workshopId"
                    ></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="设备状态" prop="status">
                  <el-select v-model="form.status" placeholder="请选择设备状态">
                    <el-option
                      v-for="item in (dict.type.mes_machinery_status || [])"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    ></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="10">
            <BarcodeImg ref="barcodeImg" :bussinessId="form.machineryId" :bussinessCode="form.machineryCode" barcodeType="MACHINERY"></BarcodeImg>
          </el-col>
        </el-row>
        <el-row v-if="form.machineryId !=null">
          <el-col :span="12">
            <el-form-item label="最近点检时间" prop="lastCheckTime">
              <el-date-picker clearable
                v-model="form.lastCheckTime"
                readonly
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最近保养时间" prop="lastMaintenTime">
              <el-date-picker clearable
                v-model="form.lastMaintenTime"
                readonly
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="规格型号" prop="machinerySpec">
              <el-input v-model="form.machinerySpec" type="textarea" maxlength="255" readonly="readonly" v-if="optType=='view'" />
              <el-input v-model="form.machinerySpec" type="textarea" placeholder="请输入规格型号" maxlength="255" v-else/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" readonly v-if="optType=='view'"></el-input>
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" v-else></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-tabs type="border-card" v-model="activeName" v-if="optType != 'add'" @tab-click="handleActive">
        <el-tab-pane label="点检记录" name="check">
          <CheckPlan ref="checkList" />
        </el-tab-pane>
        <el-tab-pane label="保养记录" name="maintenance">
          <CheckPlan ref="maintenanceList" />
        </el-tab-pane>
        <el-tab-pane label="维修记录" name="repair">
          <Repair ref="repairList" />
        </el-tab-pane>
      </el-tabs>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
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
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的设备数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div></template>
    </el-dialog>

    <el-dialog :title="typeTitle" v-model="typeOpen" width="560px" append-to-body>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="110px">
        <el-form-item label="父类型" prop="parentTypeId">
          <treeselect v-model="typeForm.parentTypeId" :options="machineryTypeOptions" :normalizer="normalizer" placeholder="请选择父类型" />
        </el-form-item>
        <el-form-item label="类型编码" prop="machineryTypeCode">
          <el-input v-model="typeForm.machineryTypeCode" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="machineryTypeName">
          <el-input v-model="typeForm.machineryTypeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="是否启用" prop="enableFlag">
          <el-radio-group v-model="typeForm.enableFlag">
            <el-radio v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitTypeForm">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="manufacturerDrawer" title="制造商配置" size="720px" append-to-body destroy-on-close>
      <ManufacturersPanel embedded />
    </el-drawer>
  </div>
</template>

<script>
import { listMachinery, getMachinery, delMachinery, addMachinery, updateMachinery } from "@/yunshu-ui/api/mes/dv/machinery";
import { listMachinerytype, getMachinerytype, addMachinerytype, updateMachinerytype, delMachinerytype } from "@/yunshu-ui/api/mes/dv/machinerytype";
import { listAllWorkshop } from "@/yunshu-ui/api/mes/md/workshop";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { getToken } from "@/yunshu-ui/utils/auth";
import Treeselect from "@zanmato/vue3-treeselect";
import "@zanmato/vue3-treeselect/dist/vue3-treeselect.min.css";
import BarcodeImg from "@/yunshu-ui/components/barcodeImg/index.vue";
import CheckPlan from "./components/Checkplan.vue"
import Repair from "./components/Repair.vue"
import ManufacturersPanel from "../supplement/Manufacturers.vue"

export default {
  name: "Machinery",
  dicts: ['sys_yes_no','mes_machinery_status'],
  components: { Treeselect,BarcodeImg, CheckPlan, Repair, ManufacturersPanel },
  data() {
    return {
      manufacturerDrawer: false,
      selectedTypeNode: null,
      typeOpen: false,
      typeTitle: "",
      typeForm: {},
      typeRules: {
        parentTypeId: [{ required: true, message: "父类型不能为空", trigger: "blur" }],
        machineryTypeName: [{ required: true, message: "类型名称不能为空", trigger: "blur" }],
        enableFlag: [{ required: true, message: "是否启用不能为空", trigger: "blur" }],
      },
      activeName: "check",
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
      // 物料产品表格数据
      machineryList: [],
      // 弹出层标题
      title: "",
      // 设备类型树选项
      machineryTypeOptions: [],
      //车间选项
      workshopOptions:[],
      // 是否显示弹出层
      open: false,
      // 设备类型名称
      machineryTypeName: undefined,
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "machineryTypeName"
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
        url: "/api/mes/dv/machinery/importData"
      },
      //二维码查询参数
      barcodeParams: {
        bussinessId: null,
        bussinessCode: null,
        barcodeFormart: 'QR_CODE', //模式二维码
        barcodeType: 'MACHINERY' //类型
      },
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
        machineryTypeId: null,
        machineryTypeCode: null,
        machineryTypeName: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        status: null
      },
      overview: {
        total: 0,
        runningCount: 0,
        stopCount: 0,
        maintCount: 0,
        byStatus: {},
      },

      // 表单校验
      rules: {
        machineryCode: [
          { required: true, message: "设备编码不能为空", trigger: "blur" },
          { max: 64, message: '设备编码长度必须小于64个字符', trigger: 'blur' }
        ],
        machineryName: [
          { required: true, message: "设备名称不能为空", trigger: "blur" }
        ],
        workshopId: [
          { required: true, message: "车间不能为空",trigger: "blur"}
        ],
        machineryTypeId: [
          { required: true, message: "设备分类不能为空", trigger: "blur" },
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    statusFilterItems() {
      return this.dict.type.mes_machinery_status || [];
    },
    typeTreeCount() {
      const walk = (nodes) => {
        if (!nodes?.length) return 0;
        return nodes.reduce((sum, node) => sum + 1 + walk(node.children), 0);
      };
      return walk(this.machineryTypeOptions);
    },
    currentScopeLabel() {
      const parts = [];
      if (this.selectedTypeNode?.machineryTypeName) {
        parts.push(`分类：${this.selectedTypeNode.machineryTypeName}`);
      } else if (this.queryParams.machineryTypeId) {
        parts.push("已选分类");
      }
      if (this.queryParams.status) {
        const hit = (this.dict.type.mes_machinery_status || []).find((d) => d.value === this.queryParams.status);
        parts.push(`状态：${hit?.label || this.queryParams.status}`);
      }
      if (this.queryParams.workshopId) {
        const ws = this.workshopOptions.find((w) => w.workshopId === this.queryParams.workshopId);
        if (ws?.workshopName) parts.push(`车间：${ws.workshopName}`);
      }
      return parts.length ? parts.join(" · ") : "全部范围";
    },
  },
  watch: {
    // 根据设备分类名称筛选分类树
    machineryTypeName(val) {
      this.$refs.tree?.filter(val);
    }
  },
  created() {
    this.getList();
    this.getTreeselect();
    this.getWorkshops();
    this.loadOverview();
    if (this.$route.query?.drawer === "manufacturers") {
      this.manufacturerDrawer = true;
    }
    if (this.$route.query?.machineryCode) {
      this.queryParams.machineryCode = this.$route.query.machineryCode;
    }
  },
  methods: {
    handleTypeAdd(row) {
      this.typeForm = {
        machineryTypeId: null,
        machineryTypeCode: null,
        machineryTypeName: null,
        parentTypeId: row?.machineryTypeId ?? 0,
        enableFlag: "Y",
        remark: null,
      };
      this.typeTitle = "新增设备类型";
      this.typeOpen = true;
    },
    handleTypeEdit() {
      if (!this.selectedTypeNode) return;
      getMachinerytype(this.selectedTypeNode.machineryTypeId).then((res) => {
        this.typeForm = res.data || { ...this.selectedTypeNode };
        this.typeTitle = "编辑设备类型";
        this.typeOpen = true;
      });
    },
    handleTypeDelete() {
      if (!this.selectedTypeNode) return;
      this.$modal.confirm(`确认删除类型「${this.selectedTypeNode.machineryTypeName}」？`).then(() => {
        return delMachinerytype(this.selectedTypeNode.machineryTypeId);
      }).then(() => {
        this.$modal.msgSuccess("删除成功");
        this.selectedTypeNode = null;
        this.getTreeselect();
      }).catch(() => {});
    },
    submitTypeForm() {
      this.$refs.typeFormRef.validate((valid) => {
        if (!valid) return;
        const req = this.typeForm.machineryTypeId ? updateMachinerytype(this.typeForm) : addMachinerytype(this.typeForm);
        req.then(() => {
          this.$modal.msgSuccess("保存成功");
          this.typeOpen = false;
          this.getTreeselect();
        });
      });
    },
    // 使用HiPrint打印
    handleHiPrint(row) {
      this.$modal.msgWarning("标签打印待对接");
    },
    handleActive (tab) {
      const query = {}
      query.machineryCode = this.form.machineryCode
      if (tab.name == "check") {
        query.planType = "CHECK"
        this.$refs.checkList.getOpen(query)
      }
      if (tab.name == "maintenance") {
        query.planType = "MAINTEN"
        this.$refs.maintenanceList.getOpen(query)
      }
      if (tab.name == "repair") {
        this.$refs.repairList.getOpen(query)
      }
    },
    changeWorkshop(val) {
      const workshop = this.workshopOptions.filter(item => item.workshopId == val)
      this.form.workshopId = workshop[0].workshopId
      this.form.workshopName = workshop[0].workshopName
      this.form.workshopCode = workshop[0].workshopCode
    },
    /** 查询设备列表 */
    getList(pagination) {
      if (pagination && typeof pagination === "object") {
        this.queryParams.pageNum = pagination.page;
        this.queryParams.pageSize = pagination.limit;
      }
      this.loading = true;
      listMachinery(this.queryParams).then((response) => {
        this.machineryList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
      }).catch(() => {
        this.machineryList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    loadOverview() {
      listMachinery({ pageNum: 1, pageSize: 500 }).then((response) => {
        const rows = response.rows || [];
        const runningStatuses = new Set(["RUNNING", "WORKING"]);
        const stopStatuses = new Set(["STOP"]);
        const maintStatuses = new Set(["REPAIR"]);
        const byStatus = {};
        rows.forEach((r) => {
          if (r.status) byStatus[r.status] = (byStatus[r.status] || 0) + 1;
        });
        this.overview = {
          total: response.total ?? rows.length,
          runningCount: rows.filter((r) => runningStatuses.has(r.status)).length,
          stopCount: rows.filter((r) => stopStatuses.has(r.status)).length,
          maintCount: rows.filter((r) => maintStatuses.has(r.status)).length,
          byStatus,
        };
      }).catch(() => {});
    },
    applyStatusFilter(value) {
      this.queryParams.status = value || null;
      this.handleQuery();
    },
    resetScope() {
      this.queryParams.machineryTypeId = null;
      this.queryParams.status = null;
      this.queryParams.workshopId = null;
      this.queryParams.machineryCode = null;
      this.queryParams.machineryName = null;
      this.selectedTypeNode = null;
      this.machineryTypeName = undefined;
      this.$refs.tree?.setCurrentKey(null);
      this.handleQuery();
    },
    getWorkshops(){
      listAllWorkshop().then( response => {
        this.workshopOptions =response.data;
      });
    },
    /** 转换设备类型数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children;
      }
      return {
        id: node.machineryTypeId,
        label: node.machineryTypeName,
        children: node.children
      };
    },
	/** 查询设备类型下拉树结构 */
    getTreeselect() {
      listMachinerytype().then(response => {
        const rows = response.data || [];
        this.machineryTypeOptions = this.handleTree(rows, "machineryTypeId", "parentTypeId");
      }).catch(() => {
        this.machineryTypeOptions = [];
      });
    },
    // 筛选节点（名称 + 编码）
    filterNode(value, data) {
      if (!value) return true;
      const keyword = String(value).toLowerCase();
      const name = String(data.machineryTypeName || "").toLowerCase();
      const code = String(data.machineryTypeCode || "").toLowerCase();
      return name.includes(keyword) || code.includes(keyword);
    },
    statusCount(value) {
      return this.overview.byStatus?.[value] ?? 0;
    },
    statusTone(value) {
      const map = {
        RUNNING: "tone-ok",
        WORKING: "tone-work",
        STOP: "tone-stop",
        REPAIR: "tone-repair",
      };
      return map[value] || "tone-default";
    },
    expandAllTypes() {
      const tree = this.$refs.tree;
      if (!tree?.store?.nodesMap) return;
      Object.values(tree.store.nodesMap).forEach((node) => {
        node.expanded = true;
      });
    },
    collapseAllTypes() {
      const tree = this.$refs.tree;
      if (!tree?.store?.nodesMap) return;
      Object.values(tree.store.nodesMap).forEach((node) => {
        node.expanded = false;
      });
    },
    clearTypeSelection() {
      this.selectedTypeNode = null;
      this.queryParams.machineryTypeId = null;
      this.$refs.tree?.setCurrentKey(null);
      this.handleQuery();
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.selectedTypeNode = data;
      this.queryParams.machineryTypeId = data.machineryTypeId;
      this.$refs.tree?.setCurrentKey(data.machineryTypeId);
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
        machineryId: null,
        machineryCode: null,
        machineryName: null,
        machineryBrand: null,
        machinerySpec: null,
        machineryTypeId: null,
        machineryTypeCode: null,
        machineryTypeName: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        lastMaintenTime: null,
        lastCheckTime: null,
        status: "STOP",
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
    /** 重置按钮操作（仅搜索栏） */
    resetQuery() {
      this.queryParams.machineryCode = null;
      this.queryParams.machineryName = null;
      this.queryParams.workshopId = null;
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.machineryId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      this.getTreeselect();
      this.getWorkshops();
      const machineryId = row.machineryId || this.ids;
      getMachinery(machineryId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "查看设备信息";
        this.optType = "view";
        this.activeName = "check"
        const query = {
          machineryCode: this.form.machineryCode,
          planType: "CHECK"
        }
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
          this.$refs.checkList.getOpen(query)
        })
      });
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.getTreeselect();
      this.getWorkshops();
      if(this.queryParams.machineryTypeId != 0){
        this.form.machineryTypeId = this.queryParams.machineryTypeId;
      }
      this.optType = "add";
      this.open = true;
      this.title = "新增设备";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      this.getTreeselect();
      this.getWorkshops();
      const machineryId = row.machineryId || this.ids
      getMachinery(machineryId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改设备";
        this.optType = "edit";
        this.activeName = "check"
        const query = {
          machineryCode: this.form.machineryCode,
          planType: "CHECK"
        }
        this.$nextTick(()=>{
          this.$refs.barcodeImg.getBarcode();
          this.$refs.checkList.getOpen(query)
        })
      });
    },

    /** 提交按钮 */
    submitForm: function() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.machineryId != undefined) {
            updateMachinery(this.form).then(() => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.loadOverview();
            });
          } else {
            addMachinery(this.form).then(() => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
              this.loadOverview();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const machineryIds = row.machineryId || this.ids;
      this.$modal.confirm('确认删除数据项？').then(function() {
        return delMachinery(machineryIds);
      }).then(() => {
        this.getList();
        this.loadOverview();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/dv/machinery/export', {
        ...this.queryParams
      }, `machinery_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "设备导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('mes/dv/machinery/importTemplate', {
      }, `machinery_template_${new Date().getTime()}.xlsx`)
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
      this.loadOverview();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('MACHINERY_CODE').then(response =>{
          this.form.machineryCode = response;
        });
      }else{
        this.form.machineryCode = null;
      }
    }
  }
};
</script>
<style scoped>
.flex-container{
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
}
.barcodeClass {
  width: 200px;
  height: 200px;
  border: 1px dashed;
  position: relative;
  display: inline-block;
}
</style>
