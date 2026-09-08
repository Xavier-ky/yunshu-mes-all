<template>
  <div class="app-container" :class="{ 'hub-embed-panel': embedMode }">

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['print:printerconfig:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['print:printerconfig:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['print:printerconfig:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :show-search="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>
    <el-table v-loading="loading" :data="clientList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="客户端编号" align="center" prop="clientCode">
        <template #default="scope">
          <el-button
            type="text"
            @click="handleView(scope.row)"
            v-hasPermi="['print:printerconfig:query']"
          >{{scope.row.clientCode}}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="客户端名称" align="center" prop="clientName" />
      <el-table-column label="客户端IP" align="center" prop="clientIp" />
      <el-table-column label="客户端端口" align="center" prop="clientPort" />
      <el-table-column label="客户端token" align="center" prop="clientToken" />
      <el-table-column label="车间名称" align="center" prop="workshopName" />
      <el-table-column label="工作站名称" align="center" prop="workstationName" />
      <el-table-column label="操作" align="center" width="200px" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['print:printerconfig:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['print:printerconfig:remove']"
          >删除</el-button>
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

    <!-- 添加或修改打印机配置对话框 -->
    <el-dialog :title="title" v-model="open" width="70%" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" :disabled="optType == 'view'" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-row>
              <el-col :span="16">
                <el-form-item label="客户端编码：" prop="clientCode">
                  <el-input v-model="form.clientCode" placeholder="请输入客户端编号" />
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户端名称：" prop="clientName">
              <el-input v-model="form.clientName" placeholder="请输入打印客户端名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户端IP：" prop="clientIp">
              <el-input v-model="form.clientIp" placeholder="请输入打印客户端IP" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户端端口：" prop="clientPort">
              <el-input v-model="form.clientPort" placeholder="请输入打印客户端端口" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户端token：" prop="clientToken">
              <el-input v-model="form.clientToken" placeholder="请输入打印客户端token" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属工作站：" prop="workstationName">
              <el-select v-model="form.workstationName" @change="changeWorkstation" placeholder="请选择所属工作站" style="width: 100%;">
                <el-option
                  v-for="item in workstationList"
                  :key="item.workstationId"
                  :label="item.workstationName"
                  :value="item.workstationName">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属车间：" prop="workshopName">
              <el-input v-model="form.workshopName" readonly placeholder="请输入所属车间" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注：" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="center" v-if="form.clientId !=null">打印机配置</el-divider>
      <PrintConfig ref="printConfig" v-if="form.clientId !=null" :optType="optType" :client="form"></PrintConfig>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" v-if="optType !='view'">确 定</el-button>
        <el-button @click="cancel">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { addClient, delClient, getClient, getClientList, getClientPage, getWorkshopAndWorkstation, updateClient } from "@/yunshu-ui/api/print/client";
import PrintConfig from "./components/printConfig.vue";

export default {
  name: "Printerconfig",
  dicts: ['mes_printer_status', 'mes_printer_type', 'mes_conn_type','sys_yes_no'],
  components: { PrintConfig },
  props: {
    embedMode: { type: Boolean, default: false },
  },
  data() {
    return {
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
      // 打印机配置表格数据
      clientList: [],
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
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        clientCode: [
          { required: true, message: "客户端编号不能为空", trigger: "blur" }
        ],

        clientName: [
          { required: true, message: "客户端名称不能为空", trigger: "blur" }
        ],

        clientIp: [
          { required: true, message: "客户端IP地址不能为空", trigger: "blur" }
        ],
        clientPort: [
          { required: true, message: "客户端端口地址不能为空", trigger: "blur" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      },
      // 所属车间
      workshopList: [],
      // 所属工作站
      workstationList: []
    };
  },
  created() {
    this.getList();
  },
  mounted() {
    this.getWorkshopAndWorkstation()
  },
  methods: {
    changeWorkstation(val) {
      const workstation = this.workstationList.filter(item => item.workstationName == val)[0]
      this.form.workshopId = workstation.workshopId
      this.form.workshopCode = workstation.workshopCode
      this.form.workshopName = workstation.workshopName
      this.form.workstationId = workstation.workstationId
      this.form.workstationCode = workstation.workstationCode
    },
    /** 查询打印机配置列表 */
    getList() {
      this.loading = true;
      getClientPage(this.queryParams).then(response => {
        this.clientList = response.rows;
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
        clientId: null,
        clientCode: null,
        clientName: null,
        clientIp: null,
        clientPort: null,
        clientToken: null,
        workshopId: null,
        workshopCode: null,
        workshopName: null,
        workstationId: null,
        workstationCode: null,
        workstationName: null,
        status: "READY",
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
      this.title = "添加客户端配置";
      this.optType = 'add';
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const clientId = row.clientId || this.ids
      getClient(clientId).then(res => {
        this.form = res.data
        this.open = true
        this.title = "修改客户端配置";
        this.optType = 'edit';
      })
    },
    /** 修改按钮操作 */
    handleView(row) {
      this.reset();
      const clientId = row.clientId || this.ids
      getClient(clientId).then(res => {
        this.form = res.data
        this.open = true
        this.title = "查看客户端配置";
        this.optType = 'view'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.clientId != null) {
            updateClient(this.form).then(res => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            })
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
      this.$modal.confirm('是否确认删除客户端配置编号为"' + clientIds + '"的数据项？').then(function() {
        return delClient(clientIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    //自动生成编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('PRINTER_CLIENT_CODE').then(response =>{
          /*$set*/(this.form, "clientCode", response)
        });
      }else{
        this.form.clientCode = null;
      }
    },
    // 获取所属车间和所属工作站数据
    getWorkshopAndWorkstation(){
      getWorkshopAndWorkstation().then(res => {
        // this.workshopList = res.data.workshopList;
        this.workstationList = res.data.workstations;
      })
    },
  }
};
</script>
<style lang="scss" scoped>

.printer-card{
  width: 300px;
}

</style>
