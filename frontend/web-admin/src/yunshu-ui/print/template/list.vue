<template>
  <div class="app-container" :class="{ 'hub-embed-panel': embedMode }">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模板编号" prop="templateCode">
        <el-input
          v-model="queryParams.templateCode"
          placeholder="请输入模板编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模板名称" prop="templateName">
        <el-input
          v-model="queryParams.templateName"
          placeholder="请输入模板名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模板类型" prop="templateType">
        <el-select v-model="queryParams.templateType" placeholder="请选择模板类型" clearable>
          <el-option label="请选择字典生成" value="" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用状态" prop="enableFlag">
        <el-input
          v-model="queryParams.enableFlag"
          placeholder="请输入启用状态"
          clearable
          @keyup.enter="handleQuery"
        />
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

    <el-row :gutter="16" class="card-grid">
      <el-col :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="templateCard card-add-wrap" shadow="hover">
          <div class="card-add" @click="handleAdd">
            <el-icon class="card-add-icon"><Plus /></el-icon>
            <span class="card-add-text">新建模板</span>
          </div>
        </el-card>
      </el-col>
      <el-col
        v-for="item in templateList"
        :key="item.templateId"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <el-card class="templateCard">
          <template #header>
            <div class="clearfix template-card-header">
              <span class="template-card-title">{{ item.templateName }}</span>
              <div class="template-card-actions">
                <el-tooltip effect="dark" content="编辑模板" placement="top">
                  <el-icon class="template-action-icon" @click="handleEdit(item)"><Edit /></el-icon>
                </el-tooltip>
                <el-tooltip effect="dark" content="删除模板" placement="top">
                  <el-icon class="template-action-icon" @click="handleDelete(item)"><Delete /></el-icon>
                </el-tooltip>
              </div>
            </div>
          </template>
          <div class="template-card-preview" @click="handleEdit(item)">
            <el-image v-if="item.templatePic" :src="item.templatePic" fit="contain" class="template-preview-img" />
            <div v-else class="template-preview-placeholder">
              <el-icon :size="48"><Printer /></el-icon>
              <p>{{ item.templateCode }}</p>
              <span>{{ item.paperType }} · {{ item.templateWidth }}×{{ item.templateHeight }}mm</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 添加或修改打印模板配置对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="模板编号" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编号" />
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
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" placeholder="请选择模板类型" style="width: 100%">
                <el-option
                      v-for="dict in dict.type.print_template_type"
                      :key="dict.value"
                      :label="dict.label"
                      :value="dict.value"
                    ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纸张类型" prop="paperType">
              <el-select v-model="form.paperType" @change="changePaperType" placeholder="请选择纸张类型" style="width: 100%">
                <el-option
                      v-for="dict in dict.type.print_paper_type"
                      :key="dict.value"
                      :label="dict.label"
                      :value="dict.value"
                    ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="this.form.paperType == 'other'">
          <el-col :span="12">
            <el-form-item label="纸张宽度" prop="templateWidth">
              <el-input-number v-model="form.templateWidth" placeholder="请输入纸张宽度(mm)" style=" width: 100%; text-align: center" controls-position="right"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纸张高度" prop="templateHeight">
              <el-input-number v-model="form.templateHeight" placeholder="请输入纸张高度(mm)" style=" width: 100%; text-align: center" controls-position="right"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="是否默认" prop="isDefault">
              <el-radio-group v-model="form.isDefault">
                  <el-radio
                    v-for="dict in dict.type.sys_yes_no"
                    :key="dict.value"
                    :label="dict.value"
                  >{{dict.label}}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
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
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
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
import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate } from "@/yunshu-ui/api/print/template";
import {genCode} from "@/yunshu-ui/api/system/autocode/rule"
import { Edit, Delete, Printer, Plus } from '@element-plus/icons-vue'
export default {
  name: "Template",
  components: { Edit, Delete, Printer, Plus },
  dicts: ['print_template_type','sys_yes_no','print_paper_type'],
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
      // 打印模板配置表格数据
      templateList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        templateCode: null,        templateName: null,        templateType: null,        templateJson: null, paperType: null,     templateWidth: null, templateHeight: null,   isDefault: null,        enableFlag: null,        templatePic: null      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        templateCode: [
          { required: true, message: "模板编号不能为空", trigger: "blur" }
        ],
        templateName: [
          { required: true, message: "模板名称不能为空", trigger: "blur" }
        ],        templateType: [
          { required: true, message: "模板类型不能为空", trigger: "change" }
        ],      paperType: [
          { required: true, message: "请选择纸张类型", trigger: "change" }
        ],
        remark: [
          { max: 250, message: '长度必须小于250个字符', trigger: 'blur' }
        ]
      },
      // 纸张类型
      paperTypes: {
        'A3': {
          width: 420,
          height: 296.6
        },
        'A4': {
          width: 210,
          height: 296.6
        },
        'A5': {
          width: 210,
          height: 147.6
        },
        'B3': {
          width: 500,
          height: 352.6
        },
        'B4': {
          width: 250,
          height: 352.6
        },
        'B5': {
          width: 250,
          height: 175.6
        }
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    changePaperType(val) {
      if (val == 'other') {
        this.form.templateWidth = undefined
        this.form.templateHeight = undefined
      } else {
        this.form.templateWidth = this.paperTypes[val].width
        this.form.templateHeight = this.paperTypes[val].height
      }
    },
    /** 查询打印模板配置列表 */
    getList() {
      this.loading = true;
      listTemplate(this.queryParams).then(response => {
        this.templateList = response.rows;
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
        templateId: null,
        templateCode: null,
        templateName: null,
        templateType: null,
        templateJson: null,
        paperType: null,
        templateWidth: undefined,
        templateHeight: undefined,
        isDefault: 'Y',
        enableFlag: 'Y',
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        templatePic: null
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
      this.title = "添加打印模板";
    },
    handleEdit(item) {
      this.$router.push({
        path: "/app/analytics/print",
        query: {
          templateId: String(item.templateId),
          paperType: item.paperType || "A4",
        },
      });
    },
    /** 删除按钮操作 */
    handleDelete(item) {
      const templateIds = item.templateId || this.ids;
      this.$modal.confirm('是否确认删除打印模板配置编号为"' + templateIds + '"的数据项？').then(function() {
        return delTemplate(templateIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 提交按钮 */
    submitForm() {
      // 校验纸张宽高
      if (!(this.form.templateWidth >= 1)) {
        this.$message.warning("请输入纸张宽度")
        return
      }
      if (!(this.form.templateHeight >= 1)) {
        this.$message.warning("请输入纸张高度")
        return
      }
      this.$refs["form"].validate(valid => {
        if (valid) {
            addTemplate(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.form.templateId = response.data.templateId;
              this.handleEdit(this.form)
            });
        }
      });
    },
    //自动生成模板编码
    handleAutoGenChange(autoGenFlag){
      if(autoGenFlag){
        genCode('PRINT_TEMPLATE_CODE').then(response =>{
          this.form.templateCode = response;
        });
      }else{
        this.form.templateCode = null;
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



.templateCard {
  border: solid 1px rgba(0, 0, 0, .08);
  width: 100%;
  min-height: 280px;
  cursor: default;
  font-size: 12px;
  position: relative;
  overflow: hidden;
  color: #666;
  background: #fff;
  box-shadow: 4px 4px 40px rgba(0, 0, 0, .05);
  border-radius: 10px;
}

.template-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.template-card-title {
  font-weight: 600;
  color: #1a1a1a;
}

.template-card-actions {
  display: flex;
  gap: 12px;
}

.template-action-icon {
  cursor: pointer;
  font-size: 16px;
  color: #606266;
}

.template-action-icon:hover {
  color: #409eff;
}

.template-card-preview {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.template-preview-img {
  width: 100%;
  height: 180px;
}

.template-preview-placeholder {
  text-align: center;
  color: #909399;
  padding: 24px 12px;
}

.template-preview-placeholder p {
  margin: 8px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.template-preview-placeholder span {
  font-size: 12px;
}

.card-grid {
  margin-top: 8px;
}

.card-add-wrap :deep(.el-card__body) {
  padding: 0;
}

.card-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  cursor: pointer;
  color: #909399;
  transition: color 0.2s, background 0.2s;
}

.card-add:hover {
  color: #409eff;
  background: #f5f9ff;
}

.card-add-icon {
  font-size: 72px;
  margin-bottom: 12px;
}

.card-add-text {
  font-size: 15px;
  font-weight: 500;
  color: #606266;
}

.cardHeader {
  float: right;
  padding: 2px 0;
}

</style>

