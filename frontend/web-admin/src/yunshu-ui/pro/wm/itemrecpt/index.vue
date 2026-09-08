<template>
  <div class="app-container inbound-doc-panel">
    <div class="recpt-intro">
      <strong>物料入库</strong>
      <span class="recpt-intro__hint">
        点 <em>新增</em> 选供应商并保存 → 添加物料行 → 提交 → 上架明细 → 提交执行 → 执行入库。
      </span>
    </div>
    <section v-show="showSearch" class="inbound-filter-panel">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>
        <el-form-item prop="recptCode">
          <el-input v-model="queryParams.recptCode" placeholder="入库单编号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="vendorName">
          <el-input v-model="queryParams.vendorName" placeholder="供应商名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item prop="recptDate">
          <el-date-picker
            v-model="queryParams.recptDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="入库日期"
            clearable
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="el-icon-search" size="default" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="default" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="inbound-toolbar">
      <div class="inbound-toolbar-left">
        <span class="inbound-page-title">物料入库</span>
        <el-button type="primary" icon="el-icon-plus" size="default" @click="handleAdd" v-hasPermi="['mes:wm:itemrecpt:add']">新增</el-button>
        <el-button type="success" plain icon="el-icon-edit" size="default" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:itemrecpt:edit']">修改</el-button>
        <el-button type="danger" plain icon="el-icon-delete" size="default" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:itemrecpt:remove']">删除</el-button>
        <el-button type="warning" plain icon="el-icon-download" size="default" @click="handleExport" v-hasPermi="['mes:wm:itemrecpt:export']">导出</el-button>
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
        :data="itemrecptList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="42" align="center" header-align="center" />
        <el-table-column label="入库单编号" align="center" header-align="center" min-width="130" prop="recptCode">
          <template #default="scope">
            <button type="button" class="doc-link-btn" @click.stop.prevent="handleView(scope.row)">{{ scope.row.recptCode }}</button>
          </template>
        </el-table-column>
        <el-table-column label="入库单名称" align="center" header-align="center" min-width="120" prop="recptName" show-overflow-tooltip />
        <el-table-column label="供应商" align="center" header-align="center" min-width="110" prop="vendorName" show-overflow-tooltip />
        <el-table-column label="入库日期" align="center" header-align="center" prop="recptDate" width="100">
          <template #default="scope">
            <span>{{ parseTime(scope.row.recptDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="采购订单" align="center" header-align="center" min-width="110" prop="poCode" show-overflow-tooltip />
        <el-table-column label="状态" align="center" header-align="center" width="96" prop="status">
          <template #default="scope">
            <dict-tag :options="dict.type.mes_itemrecpt_status" :value="scope.row.status"/>
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
                v-hasPermi="['mes:wm:itemrecpt:query']"
              >查看</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-edit"
                v-if="scope.row.status =='PREPARE'"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >修改</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-check"
                v-if="scope.row.status =='PREPARE'"
                @click="handleSubmit(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >提交</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-shopping-cart-full"
                v-if="scope.row.status =='APPROVING'"
                @click="handleStocking(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >执行上架</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-check"
                v-if="scope.row.status =='APPROVING'"
                @click="handleSubmitExecute(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >提交执行</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-video-play"
                v-if="scope.row.status =='APPROVED'"
                @click="handleExecute(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >执行入库</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-circle-close"
                v-if="scope.row.status !='PREPARE' && scope.row.status !='FINISHED' && scope.row.status !='CANCELED'"
                @click="handleCancelDoc(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:edit']"
              >撤销</el-button>
              <el-button
                size="small"
                link
                icon="el-icon-delete"
                v-if="scope.row.status =='PREPARE'"
                @click="handleDelete(scope.row)"
                v-hasPermi="['mes:wm:itemrecpt:remove']"
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

    <!-- 添加或修改物料入库单对话框 -->
    <el-dialog :title="title" v-model="open" width="920px" append-to-body class="recpt-form-dialog" destroy-on-close>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="recpt-edit-form">
        <div class="recpt-form-section">
          <div class="recpt-form-section__title">基本信息</div>
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="入库单编号" prop="recptCode">
                <el-input v-model="form.recptCode" placeholder="可自动生成" :disabled="autoGenFlag && optType === 'add'" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label-width="16">
                <el-switch
                  v-if="optType != 'view' && form.status =='PREPARE'"
                  v-model="autoGenFlag"
                  active-text="自动生成编号"
                  @change="handleAutoGenChange(autoGenFlag)"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="入库单名称" prop="recptName">
                <el-input v-model="form.recptName" placeholder="选择供应商后可自动生成" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="入库日期" prop="recptDate">
                <el-date-picker
                  clearable
                  v-model="form.recptDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择入库日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="recpt-form-section recpt-form-section--last">
          <div class="recpt-form-section__title">供应商与关联</div>
          <el-form-item label="供应商" prop="vendorName">
            <div class="vendor-pick-row">
              <el-input
                v-model="form.vendorName"
                readonly
                placeholder="点击右侧按钮选择供应商"
                class="vendor-pick-row__input"
              />
              <el-button v-if="optType != 'view'" type="primary" plain icon="el-icon-office-building" @click="handleSelectVendor">
                选择供应商
              </el-button>
            </div>
            <VendorSelect ref="vendorSelect" @onSelected="onVendorSelected" />
            <IqcSelect ref="iqcSelect" @onSelected="onIqcSelected" />
          </el-form-item>
          <el-collapse v-if="optType != 'view'" class="recpt-more-collapse">
            <el-collapse-item title="更多选项（到货通知 / 采购订单 / 备注）" name="more">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="到货通知单" prop="noticeCode">
                    <el-input v-model="form.noticeCode" readonly placeholder="可选">
                      <template #append>
                        <el-button @click="handleSelectNotice" icon="el-icon-search"></el-button>
                      </template>
                    </el-input>
                    <NoticeSelect ref="noticeSelect" @onSelected="onNoticeSelected"></NoticeSelect>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="采购订单号" prop="poCode">
                    <el-input v-model="form.poCode" placeholder="可选" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="备注" prop="remark">
                <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
          <el-form-item v-else-if="form.remark" label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" readonly />
          </el-form-item>
        </div>
      </el-form>

      <div v-if="form.recptId != null" class="recpt-lines-panel">
        <div class="recpt-form-section__title">{{ form.status === 'PREPARE' ? '物料行' : '上架明细' }}</div>
        <Itemrecptline
          v-if="form.status == 'PREPARE'"
          ref="line"
          :recptId="form.recptId"
          :noticeId="form.noticeId"
          :optType="optType"
        />
        <ItemrecptDetail v-else ref="detail" :recptId="form.recptId" :optType="optType" />
      </div>

      <template #footer>
        <div class="dialog-footer recpt-dialog-footer">
          <template v-if="form.status =='PREPARE' && optType !='view'">
            <el-button type="primary" :loading="saving" @click="saveForm(false)">保 存</el-button>
            <el-button
              v-if="!form.recptId"
              type="success"
              :loading="saving"
              @click="saveForm(true)"
            >保存并添加物料</el-button>
            <el-button
              v-else
              type="success"
              plain
              @click="openLineAdd"
            >添加物料</el-button>
            <el-button type="warning" :disabled="!form.recptId" @click="submitToStock">提 交</el-button>
          </template>
          <el-button type="warning" @click="submitToExecute" v-if="form.status =='APPROVING' && form.recptId !=null && optType !='view'">提 交</el-button>
          <el-button type="danger" @click="cancel" v-if="form.status !='PREPARE' && form.status !='FINISHED' && optType !='view'">取 消</el-button>
          <el-button @click="close">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listItemrecpt, getItemrecpt, delItemrecpt, addItemrecpt, updateItemrecpt, confirmItemrecpt,execute } from "@/yunshu-ui/api/mes/wm/itemrecpt";
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import VendorSelect from "@/yunshu-ui/components/vendorSelect/single.vue";
import IqcSelect from "@/yunshu-ui/components/iqcSelect/single.vue";
import NoticeSelect from "@/yunshu-ui/components/noticeSelect/single.vue"
import Itemrecptline from "./line.vue";
import ItemrecptDetail from "./detail.vue";
export default {
  name: "Itemrecpt",
  dicts:['mes_itemrecpt_status'],
  components :{VendorSelect,IqcSelect,Itemrecptline,ItemrecptDetail, NoticeSelect},
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
      optType: undefined,
      warehouseInfo:[],
      warehouseOptions:[],
      warehouseProps:{
        multiple: false,
        value: 'pId',
        label: 'pName',
      },
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
      // 物料入库单表格数据
      itemrecptList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      saving: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        recptCode: null,
        recptName: null,
        iqcId: null,
        iqcCode: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: null,
        poCode: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recptCode: [
          { required: true, message: "入库单编号不能为空", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" }
        ],
        recptName: [
          { required: true, message: "入库单名称不能为空", trigger: "blur" },
          { max: 100, message: "字段过长", trigger: "blur" }
        ],
        recptDate: [
          { required: true, message: "入库时间不能为空", trigger: "blur"}
        ],
        vendorName: [
          { required: true, message: "请选择对应的供应商", trigger: "blur"}
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getWarehouseList();
  },
  methods: {
    /** 查询物料入库单列表 */
    getList() {
      this.loading = true;
      listItemrecpt(this.queryParams).then(response => {
        this.itemrecptList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    getWarehouseList(){
      getTreeList().then( response =>{
        if(response.data){
          this.warehouseOptions = response.data.filter((el) =>{
              return el.warehouseCode.indexOf('VIR') == -1;
          });;
        }
        this.warehouseOptions.map(w =>{
          w.children.map(l =>{
                  let lstr =JSON.stringify(l.children).replace(/locationId/g,'lId').replace(/areaId/g, 'pId').replace(/areaName/g,'pName');
                  l.children = JSON.parse(lstr);
          });

          let wstr = JSON.stringify(w.children).replace(/warehouseId/g,'wId').replace(/locationId/g, 'pId').replace(/locationName/g,'pName');
          w.children =  JSON.parse(wstr);

        });
        let ostr=JSON.stringify(this.warehouseOptions).replace(/warehouseId/g,'pId').replace(/warehouseName/g, 'pName');
        this.warehouseOptions = JSON.parse(ostr);
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
        recptId: null,
        recptCode: null,
        recptName: null,
        noticeId: null,
        noticeCode: null,
        iqcId: null,
        iqcCode: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        vendorNick: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        recptDate: null,
        poCode: null,
        status: "PREPARE",
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
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recptId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.warehouseInfo = [];
      this.open = true;
      this.title = "新建物料入库单";
      this.optType = "add";
      this.autoGenFlag = true;
      this.form.recptDate = this.formatDate(new Date());
      this.form.recptName = `物料入库-${this.form.recptDate.replace(/-/g, "")}`;
      this.applyAutoRecptCode();
    },
    formatDate(dt) {
      const d = dt instanceof Date ? dt : new Date(dt);
      const pad = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    },
    applyAutoRecptCode() {
      genCode("ITEMRECPT_CODE").then((response) => {
        const code = response?.data ?? response;
        if (typeof code === "string" && code.trim()) {
          this.form.recptCode = code.trim();
        }
      }).catch(() => {
        this.form.recptCode = `IR${Date.now() % 100000}`;
      });
    },
    openLineAdd() {
      this.$nextTick(() => {
        this.$refs.line?.handleAdd?.();
      });
    },
    // 查询明细按钮操作
    handleView(row){
      this.reset();
      const recptIds = row.recptId
      getItemrecpt(recptIds).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.open = true;
        this.title = "查看入库单信息";
        this.optType = "view";
      });
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const recptId = row.recptId || this.ids
      getItemrecpt(recptId).then(response => {
        this.form = response.data;
        this.warehouseInfo[0] = response.data.warehouseId;
        this.warehouseInfo[1] = response.data.locationId;
        this.warehouseInfo[2] = response.data.areaId;
        this.open = true;
        this.title = "修改物料入库单";
        this.optType = "edit";
      });
    },

    /**
     * 上架按钮操作
     */
    handleStocking(row){
      const recptIds = row.recptId
      getItemrecpt(recptIds).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "入库单上架";
        this.optType = "edit";
      });
    },

    //执行入库
    handleExecute(row){
      const recptIds = row.recptId || this.ids;
      this.$modal.confirm('确认执行入库？').then(function() {
        return execute(recptIds)//执行入库
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("入库成功");
      }).catch(() => {});
    },
    /** 列表：草稿提交到待上架 */
    handleSubmit(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认提交该入库单？').then(() => {
        return getItemrecpt(recptId).then(response => {
          const data = { ...response.data, status: 'APPROVING' };
          return updateItemrecpt(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：待上架提交到待执行 */
    handleSubmitExecute(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认提交执行？').then(() => {
        return getItemrecpt(recptId).then(response => {
          const data = { ...response.data, status: 'APPROVED' };
          return updateItemrecpt(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("提交成功");
      }).catch(() => {});
    },
    /** 列表：撤销单据 */
    handleCancelDoc(row) {
      const recptId = row.recptId;
      this.$modal.confirm('确认撤销入库单？').then(() => {
        return getItemrecpt(recptId).then(response => {
          const data = { ...response.data, status: 'CANCELED' };
          return updateItemrecpt(data);
        });
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("撤销成功");
      }).catch(() => {});
    },
    /** 保存按钮 */
    saveForm(openLineAfterSave = false) {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return;
        }
        this.saving = true;
        const done = () => { this.saving = false; };
        if (this.form.recptId != null) {
          updateItemrecpt(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.getList();
            if (openLineAfterSave) {
              this.openLineAdd();
            }
          }).finally(done);
        } else {
          addItemrecpt(this.form).then(response => {
            const id = response?.data?.recptId ?? response?.data;
            if (id != null) {
              this.form.recptId = Number(id);
            }
            this.$modal.msgSuccess("创建成功，可继续添加物料行");
            this.optType = "edit";
            this.getList();
            if (openLineAfterSave) {
              this.$nextTick(() => this.openLineAdd());
            }
          }).finally(done);
        }
      });
    },

    //提交按钮(提交到待上架状态)
    submitToStock(){
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.status = 'APPROVING';
          updateItemrecpt(this.form).then(response => {
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
      this.form.status = 'APPROVED';
        updateItemrecpt(this.form).then(response => {
          this.$modal.msgSuccess("提交成功");
          this.open = false;
          this.getList();
        } 
        ).catch(() => {
          this.form.status = 'APPROVING';
        });;       
    },

    //取消
    cancel(){
      let that = this;
      this.$modal.confirm('确认撤销入库单？').then(function() {        
        const oldStatus = that.form.status;
        that.form.status = 'CANCELED';
          updateItemrecpt(that.form).then(response => {
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


    doconfirm(){
      let that = this;
      this.$modal.confirm('是否完成入库单编制？【完成后将不能更改】').then(function(){
        that.form.status = 'CONFIRMED';
        that.submitForm();
      });
    },

    /** 删除按钮操作 */
    handleDelete(row) {
      const recptIds = row.recptId || this.ids;
      this.$modal.confirm('是否确认删除物料入库单？').then(function() {
        return delItemrecpt(recptIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/wm/itemrecpt/export', {
        ...this.queryParams
      }, `itemrecpt_${new Date().getTime()}.xlsx`)
    },
    //选择默认的仓库、库区、库位
    handleWarehouseChanged(obj){
      if(obj !=null){
        this.form.warehouseId = obj[0];
        this.form.locationId = obj[1];
        this.form.areaId = obj[2];
      }
    },
    //IQC检验单选择
    handleSelectIqc(){
      this.$refs.iqcSelect.showFlag = true;
    },
    //IQC检验单选择弹出框
    onIqcSelected(obj){
        if(obj != undefined && obj != null){
          this.form.iqcId = obj.iqcId;
          this.form.iqcCode = obj.iqcCode;
          this.form.recptName = obj.iqcName;
          this.form.vendorId = obj.vendorId;
          this.form.vendorCode = obj.vendorCode;
          this.form.vendorName = obj.vendorName;
          this.form.vendorNick = obj.vendorNick;
        }
    },
    //供应商选择
    handleSelectVendor(){
      this.$refs.vendorSelect.handleOpen(this.form.vendorId)
    },
    //供应商选择弹出框
    onVendorSelected(obj){
        if(obj != undefined && obj != null){
          this.form.vendorId = obj.vendorId;
          this.form.vendorCode = obj.vendorCode;
          this.form.vendorName = obj.vendorName;
          this.form.vendorNick = obj.vendorNick;
          if (!this.form.recptName || this.form.recptName.startsWith("物料入库-")) {
            this.form.recptName = `物料入库-${obj.vendorName || obj.vendorCode || ""}-${this.formatDate(this.form.recptDate || new Date()).replace(/-/g, "")}`;
          }
        }
    },
    //到货通知单选择
    handleSelectNotice(){
      this.$refs.noticeSelect.handleOpen(this.form.noticeId)
    },
    //到货通知单选择弹出框
    onNoticeSelected(obj){
      if(obj != undefined && obj != null){
          this.form.noticeId = obj.noticeId;
          this.form.noticeCode = obj.noticeCode;
          this.form.poCode = obj.poCode;
          this.form.vendorId = obj.vendorId;
          this.form.vendorName = obj.vendorName;
          this.form.vendorNick = obj.vendorNick;
        }
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        this.applyAutoRecptCode();
      }else{
        this.form.recptCode = null;
      }
    }
  }
};
</script>
<style scoped>
.recpt-intro {
  margin: 0 0 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(5, 150, 105, 0.08);
  border: 1px solid rgba(5, 150, 105, 0.22);
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}
.recpt-intro__hint {
  margin-left: 8px;
  color: #606266;
}
.recpt-intro__hint em {
  font-style: normal;
  color: #059669;
  font-weight: 600;
}
.recpt-form-dialog :deep(.el-dialog__header) {
  padding-bottom: 8px;
  margin-right: 0;
  border-bottom: 1px solid #ebeef5;
}
.recpt-form-dialog :deep(.el-dialog__body) {
  max-height: min(78vh, 820px);
  overflow-x: hidden;
  overflow-y: auto;
  padding-top: 14px;
}
.recpt-form-section {
  margin-bottom: 14px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #ebeef5;
}
.recpt-form-section--last {
  border-bottom: none;
  margin-bottom: 0;
}
.recpt-form-section__title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.recpt-edit-form :deep(.el-input),
.recpt-edit-form :deep(.el-select),
.recpt-edit-form :deep(.el-date-editor) {
  width: 100%;
}
.vendor-pick-row {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}
.vendor-pick-row__input {
  flex: 1;
}
.recpt-more-collapse {
  margin-top: 4px;
  border: none;
}
.recpt-more-collapse :deep(.el-collapse-item__header) {
  height: 36px;
  font-size: 13px;
  color: #606266;
  border: none;
  background: transparent;
}
.recpt-more-collapse :deep(.el-collapse-item__wrap) {
  border: none;
}
.recpt-lines-panel {
  margin-top: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px 6px;
  background: #fafbfc;
}
.recpt-lines-panel :deep(.app-container) {
  padding: 0;
  min-height: 0;
}
.recpt-dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
</style>
