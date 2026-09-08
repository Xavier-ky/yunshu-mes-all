<template>
  <div class="app-container">
    <el-row v-if="optType != 'view'" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="default"
          @click="handleAdd"
          v-hasPermi="['mes:wm:itemrecpt:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="default"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:wm:itemrecpt:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @update:showSearch="showSearch = $event" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="itemrecptlineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="物料编码" align="center" width="120px" prop="itemCode" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true"/>
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true"/>
      <el-table-column label="单位" align="center" prop="unitName" />
      <el-table-column label="入库数量" align="center" prop="quantityRecived" />
      <el-table-column label="批次号" align="center" prop="batchCode" />
      <el-table-column label="操作" align="center" width="100px" v-if="optType != 'view'" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="default"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:wm:itemrecpt:edit']"
          >修改</el-button>
          <el-button
            size="default"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:wm:itemrecpt:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @update:page="queryParams.pageNum = $event"
      @update:limit="queryParams.pageSize = $event"
      @pagination="getList"
    />

    <!-- 添加或修改物料入库单行对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="物料编码"  prop="itemCode">
              <el-input v-model="form.itemCode" readonly="readonly" placeholder="请选择物料编码" >
                <template #append>
                  <el-button v-if="noticeId !=null" @click="handleSelectNoticeLine" icon="el-icon-search"></el-button>
                  <el-button v-else @click="handleSelectProduct" icon="el-icon-search"></el-button>
                </template>
              </el-input>
            </el-form-item>
            <NoticeLineSelect v-if="noticeId !=null" ref="noticeLineSelect" :noticeId="noticeId" @onSelected="onNoticeLineSelectd"></NoticeLineSelect>
            <ItemSelect v-else ref="itemSelect" @onSelected="onItemSelected" > </ItemSelect>
            <IqcSelect ref="iqcSelect" @onSelected="onIqcSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="form.itemName" readonly="readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly="readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库数量" prop="quantityRecived">
              <el-input-number :min="0.01" :max="99999999" v-model="form.quantityRecived" placeholder="请输入入库数量" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产日期期" prop="produceDate">
              <el-date-picker clearable
                v-model="form.produceDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择生产日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期" prop="expireDate">
              <el-date-picker clearable
                v-model="form.expireDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择有效期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产批号" prop="lotNumber">
              <el-input v-model="form.lotNumber" placeholder="请输入生产批号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly="readonly"/>
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
      <template #footer><div class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div></template>
    </el-dialog>
  </div>
</template>

<script>
import { listItemrecptline, getItemrecptline, delItemrecptline, addItemrecptline, updateItemrecptline } from "@/yunshu-ui/api/mes/wm/itemrecptline";
import ItemSelect  from "@/yunshu-ui/components/itemSelect/single.vue";
import NoticeLineSelect from "@/yunshu-ui/components/noticeSelect/lineSingle.vue"
import IqcSelect from "@/yunshu-ui/components/iqcSelect/single.vue";
import {getTreeList} from "@/yunshu-ui/api/mes/wm/warehouse"
export default {
  name: "Itemrecptline",
  dicts: ['sys_yes_no'],
  components :{ItemSelect,IqcSelect,NoticeLineSelect},
  props:{
    recptId: null,
    noticeId: null,
    optType: null,
    warehouseId: null,
    locationId: null,
    areaId: null
  },
  data() {
    return {
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
      // 物料入库单行表格数据
      itemrecptlineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recptId: this.recptId,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityRecived: null,
        batchCode: null,
        warehouseId: null,
        warehouseCode: null,
        warehouseName: null,
        locationId: null,
        locationCode: null,
        locationName: null,
        areaId: null,
        areaCode: null,
        areaName: null,
        expireDate: null,
        produceDate: null,
        lotNumber: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        itemId: [
          { required: true, message: "产品物料ID不能为空", trigger: "blur" }
        ],
        quantityRecived: [
          { required: true, message: "入库数量不能为空", trigger: "blur" }
        ],
        warehouseId:[
          { required: true, message: "请选择入库的仓库" }
        ],
        itemCode: [
          { required: true, message: "物料编码不能为空", trigger: "blur" }
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
    /** 查询物料入库单行列表 */
    getList() {
      this.loading = true;
      listItemrecptline(this.queryParams).then(response => {
        this.itemrecptlineList = response.rows;
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
        debugger;
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
    //选择仓库、库区、库位
    handleWarehouseChanged(obj){
      debugger;
      if(obj !=null){
        this.form.warehouseId = obj[0];
        this.form.locationId = obj[1];
        this.form.areaId = obj[2];
      }
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        lineId: null,
        recptId: this.recptId,
        noticeLineId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        quantityRecived: 1,
        batchId: null,
        batchCode: null,
        warehouseId: this.warehouseId,
        warehouseCode: null,
        warehouseName: null,
        locationId: this.locationId,
        locationCode: null,
        locationName: null,
        areaId: this.areaId,
        areaCode: null,
        areaName: null,
        iqcCheck: 'N',
        iqcId: null,
        iqcCode: null,
        expireDate: null,
        produceDate: null,
        lotNumber: null,
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
      this.warehouseInfo = [];
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
      this.ids = selection.map(item => item.lineId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.warehouseInfo = [];
      if(this.warehouseId != null){
        let tempList = []
        tempList.push(this.warehouseId)
        tempList.push(this.locationId)
        tempList.push(this.areaId)
        this.warehouseInfo = tempList;
      }
      this.open = true;
      this.title = "添加物料入库单行";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const lineId = row.lineId || this.ids
      getItemrecptline(lineId).then(response => {
        this.form = response.data;
        let tempList = []
        tempList.push(response.data.warehouseId)
        tempList.push(response.data.locationId)
        tempList.push(response.data.areaId)
        this.warehouseInfo = tempList;
        this.open = true;
        this.title = "修改物料入库单行";
      });
    },
    handleSelectProduct(){
      this.$refs.itemSelect.showFlag = true;
    },
    //物料选择弹出框
    onItemSelected(obj){
        if(obj != undefined && obj != null){
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName
        }
    },
    //选择到货通知单行
    handleSelectNoticeLine(){
      this.$refs.noticeLineSelect.showFlag = true;
    },
    onNoticeLineSelectd(obj){
      if(obj != undefined && obj != null){
          this.form.noticeLineId = obj.lineId;
          this.form.itemId = obj.itemId;
          this.form.itemCode = obj.itemCode;
          this.form.itemName = obj.itemName;
          this.form.specification = obj.specification;
          this.form.unitOfMeasure = obj.unitOfMeasure;
          this.form.unitName = obj.unitName;
          this.form.quantityRecived = obj.quantityQuanlified;
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
        }
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {
            updateItemrecptline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.form = response.data;
              this.open = false;
              this.getList();
            });
          } else {
            addItemrecptline(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.form = response.data;
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const lineIds = row.lineId || this.ids;
      this.$modal.confirm('是否确认删除物料入库单行编号为"' + lineIds + '"的数据项？').then(function() {
        return delItemrecptline(lineIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('wm/itemrecptline/export', {
        ...this.queryParams
      }, `itemrecptline_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
