<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8" v-if="optType !='view'">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['mes:md:mditem:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:md:mditem:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="printerconfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="打印机编号" align="center" prop="printerCode" :show-overflow-tooltip="true" >
        <template #default="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['print:printerconfig:query']"
          >{{scope.row.printerCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="是否默认" align="center" prop="defaultFlag">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.defaultFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="打印机类型" align="center" prop="printerType">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_printer_type" :value="scope.row.printerType"/>
        </template>
      </el-table-column>
      <el-table-column label="打印机名称" align="center" prop="printerName" />
      <el-table-column label="品牌" align="center" prop="brand" />
      <el-table-column label="型号" align="center" prop="printerModel" />
      <el-table-column label="连接类型" align="center" prop="connectionType">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_conn_type" :value="scope.row.connectionType"/>
        </template>
      </el-table-column>
      <el-table-column label="打印机IP" align="center" prop="printerIp" />
      <el-table-column label="打印机端口" align="center" prop="printerPort" />
      <el-table-column label="启用状态" align="center" prop="enableFlag">
        <template #default="scope">
          <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag"/>
        </template>
      </el-table-column>
      <el-table-column label="打印机状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dict.type.mes_printer_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200px" class-name="small-padding fixed-width" v-if="optType !='view'">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:md:mditem:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:md:mditem:remove']"
          >删除</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-star-off"
            @click="handleDefault(scope.row)"
            v-if="scope.row.defaultFlag != 'Y'"
            v-hasPermi="['print:printerconfig:list']"
          >设置为默认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="50%" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="16">
            <el-row>
              <el-col :span="16">
                <el-form-item label="打印机编号" prop="printerCode">
                  <el-input v-model="form.printerCode" placeholder="请输入打印机编号" />
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
                <el-form-item label="打印机名称" prop="printerName">
                  <el-select v-model="form.printerName" placeholder="请选择打印机">
                    <el-option
                      v-for="item in printerList"
                      :key="item.name"
                      :label="item.displayName"
                      :value="item.name"
                    ></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="打印机类型" prop="printerType">
                  <el-select v-model="form.printerType" placeholder="请选择打印机类型">
                    <el-option
                      v-for="dict in dict.type.mes_printer_type"
                      :key="dict.value"
                      :label="dict.label"
                      :value="dict.value"
                    ></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
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
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="品牌" prop="brand">
                  <el-input v-model="form.brand" placeholder="请输入品牌" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="型号" prop="printerModel">
                  <el-input v-model="form.printerModel" placeholder="请输入型号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="打印机IP" prop="printerIp">
                  <el-input v-model="form.printerIp" placeholder="请输入打印机IP" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="打印机端口" prop="printerPort">
                  <el-input v-model="form.printerPort" placeholder="请输入打印机端口" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图片URL" prop="printerUrl">
              <ImageUpload :limit="1" :value="form.printerUrl" :fileSize="5" @onUploaded="handleImgUplaoded" @onRemoved="handleImgRemoved" ></ImageUpload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
<!--          <el-col :span="12">-->
<!--            <el-form-item label="连接类型">-->
<!--              <el-radio-group v-model="form.connectionType">-->
<!--                <el-radio-->
<!--                  v-for="dict in dict.type.mes_conn_type"-->
<!--                  :key="dict.value"-->
<!--                  :label="dict.value"-->
<!--                >{{dict.label}}</el-radio>-->
<!--              </el-radio-group>-->
<!--            </el-form-item>-->
<!--          </el-col>-->
          <el-col :span="12">
            <el-form-item label="打印机状态">
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="dict in dict.type.mes_printer_status"
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
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import {
  addPrinterconfig,
  delPrinterconfig,
  getPrinterconfig,
  listPrinterconfig,
  setDefaultPrinter,
  updatePrinterconfig
} from "@/yunshu-ui/api/print/printerconfig";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import {hiprintMixin} from "@/yunshu-ui/mixins/hiprintMixin";

export default {
  name: "PrintConfig",
  dicts: ['mes_printer_status', 'mes_printer_type', 'mes_conn_type','sys_yes_no'],
  mixins: [hiprintMixin],
  data() {
    return {
      //自动生成编码
      autoGenFlag:false,
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
      // 打印机配置表格数据
      printerconfigList: [],
      // 打印机列表
      printerList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        printerType: null,    printerCode: null,    printerName: null,        brand: null,        printerModel: null,        connectionType: null,        printerUrl: null,        printerIp: null,        printerPort: null,        clientSid: null,        clientIp: null,        clientPort: null,        enableFlag: null,        status: null,      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        printerCode: [
          { required: true, message: "打印机编号不能为空", trigger: "blur" }
        ],
        printerName: [
          { required: true, message: "打印机名称不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      }
    };
  },
  props: {
    optType: undefined,
    client: undefined
  },
  mounted() {
    this.queryParams.clientId = this.client.clientId;
    this.getList()
  },
  methods: {
    filterClient() {
      console.log("过滤客户端", this.hiprintThis)
      console.log("过滤客户端", this.hiprintThis?.hiwebSocket?.clients)
      console.log("中转服务IP", this.client?.clientIp)
      console.log("中转服务端口", this.client?.clientPort)

      // 重置打印机列表
      this.printerList = []

      try {
        // 检查必要的对象是否存在
        if (!this.hiprintThis?.hiwebSocket?.clients) {
          this.$message.warning("请检查中转站服务或者打印机客户端")
          return false
        }

        const clients = Object.entries(this.hiprintThis.hiwebSocket.clients)
        console.log("所有连接中转服务的打印机客户端", clients)

        if (clients.length === 0) {
          this.$message.info("暂无连接的打印机客户端")
          return false
        }

        // 汇总所有打印机，使用 flatMap 简化代码
        this.printerList = clients.flatMap(([clientId, client]) => {
          return client?.printerList || []
        })

        console.log("汇总后的打印机列表", this.printerList)
        return true

      } catch (error) {
        console.error("获取打印机客户端失败:", error)
        this.$message.error("获取打印机列表失败，请检查中转服务连接")
        return false
      }
    },
    //图片上传成功
    handleImgUplaoded(imgUrl){
      this.form.printerUrl = imgUrl;
    },
    //图片移除
    handleImgRemoved(imgUrl){
      this.form.printerUrl = null;
    },
    handleDownloadClient(){
      const resource = "http://101.43.244.58:9090/api/v1/buckets/printtool/objects/download?prefix=UHJpbnQuZXhl";
      this.$download.saveAs(resource,'client');
    },
    /**
     * 将指定打印机作为默认打印机，存储在浏览器本地
     * @param row
     */
    handleDefault(row){
      setDefaultPrinter(row).then(res => {
        if (res.code == 200) {
          this.getList();
          this.$modal.msgSuccess("设置成功!");
        }
      })
    },
    /** 查询打印机配置列表 */
    getList() {
      this.loading = true;
      listPrinterconfig(this.queryParams).then(response => {
        this.printerconfigList = response.rows;
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
        printerId: null,  printerCode: null,   printerType: null,        printerName: null,        brand: null,        printerModel: null,        connectionType: "0",        printerUrl: null,        printerIp: null,        printerPort: null,        clientSid: null,        clientIp: null,        clientPort: null,        enableFlag: 'Y',        status: "READY",        remark: null,        attr1: null,        attr2: null,        attr3: null,        attr4: null,        createBy: null,        createTime: null,        updateBy: null,        updateTime: null      };
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
      this.ids = selection.map(item => item.printerId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      if (this.filterClient()) {
        this.reset();
        this.open = true;
        this.title = "添加打印机配置";
        this.optType = 'add';
      }
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      if (this.filterClient()) {
        this.reset();
        const printerId = row.printerId || this.ids
        getPrinterconfig(printerId).then(response => {
          this.form = response.data;
          this.open = true;
          this.title = "修改打印机配置";
          this.optType = 'edit';
        });
      }
    },
    /** 修改按钮操作 */
    handleView(row) {
      if (this.filterClient()) {
        this.reset();
        const printerId = row.printerId || this.ids
        getPrinterconfig(printerId).then(response => {
          this.form = response.data;
          this.open = true;
          this.title = "查看打印机配置";
          this.optType = 'view';
        });
      }
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.clientId = this.client.clientId;
          if (this.form.printerId != null) {
            updatePrinterconfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addPrinterconfig(this.form).then(response => {
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
      const printerIds = row.printerId || this.ids;
      this.$modal.confirm('是否确认删除打印机配置编号为"' + printerIds + '"的数据项？').then(function() {
        return delPrinterconfig(printerIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('PRINTER_CODE').then(response =>{
          this.form.printerCode = response;
        });
      }else{
        this.form.printerCode = null;
      }
    }
  }
};
</script>
