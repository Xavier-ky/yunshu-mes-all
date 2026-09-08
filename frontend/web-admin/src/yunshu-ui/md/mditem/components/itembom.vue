<template>
  <div :class="embedded ? 'itembom-embedded' : 'app-container'">
    <template v-if="embedded">
      <div class="inbound-toolbar">
        <div class="inbound-toolbar-left">
          <span class="inbound-page-title">BOM 子件</span>
          <el-button
            v-if="optType != 'view'"
            type="primary"
            plain
            icon="el-icon-plus"
            size="default"
            @click="handleAdd"
            v-hasPermi="['mes:md:mditem:add']"
          >新增</el-button>
          <el-button
            v-if="optType != 'view'"
            type="danger"
            plain
            icon="el-icon-delete"
            size="default"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['mes:md:mditem:remove']"
          >删除</el-button>
        </div>
        <div class="inbound-toolbar-meta">
          <span>共 {{ total }} 条</span>
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
          :data="bomList"
          @selection-change="handleSelectionChange"
        >
          <el-table-column v-if="optType != 'view'" type="selection" width="42" align="center" header-align="center" />
          <el-table-column label="物料编码" align="center" header-align="center" min-width="110" prop="bomItemCode" show-overflow-tooltip />
          <el-table-column label="物料名称" align="center" header-align="center" min-width="120" prop="bomItemName" show-overflow-tooltip />
          <el-table-column label="规格" align="center" header-align="center" min-width="100" prop="bomItemSpec" show-overflow-tooltip />
          <el-table-column label="单位" width="72" align="center" header-align="center" prop="unitName" />
          <el-table-column label="使用比例" width="88" align="center" header-align="center" prop="quantity" />
          <el-table-column label="备注" align="center" header-align="center" min-width="100" prop="remark" show-overflow-tooltip />
          <el-table-column v-if="optType != 'view'" label="操作" width="120" align="center" header-align="center" class-name="col-actions small-padding fixed-width">
            <template #default="scope">
              <div class="yunshu-row-actions">
                <el-button size="small" link icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:mditem:edit']">修改</el-button>
                <el-button size="small" link icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:mditem:remove']">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <template v-else>
      <el-row :gutter="10" class="mb8" v-if="optType !='view'">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="default"
            @click="handleAdd"
            v-hasPermi="['mes:md:mditem:add']"
          >新增</el-button>
          <ItemSelect ref="itemSelect" @onSelected="onItemSelected" />
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="default"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['mes:md:mditem:remove']"
          >删除</el-button>
        </el-col>
        <right-toolbar :showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="bomList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="物料编码" align="center" prop="bomItemCode" />
        <el-table-column label="物料名称" align="center" prop="bomItemName" :show-overflow-tooltip="true" />
        <el-table-column label="规格" align="center" prop="bomItemSpec" :show-overflow-tooltip="true" />
        <el-table-column label="单位" width="60px" align="center" prop="unitName" />
        <el-table-column label="使用比例" width="90px" align="center" prop="quantity" />
        <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" v-if="optType !='view'">
          <template #default="scope">
            <el-button size="default" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:mditem:edit']">修改</el-button>
            <el-button size="default" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:mditem:remove']">删除</el-button>
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
    </template>

    <ItemSelect v-if="embedded" ref="itemSelect" @onSelected="onItemSelected" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="BOM物料编码" prop="bomItemCode">
          <el-input v-model="form.bomItemCode" readonly placeholder="请输入BOM物料编码" />
        </el-form-item>
        <el-form-item label="BOM物料名称" prop="bomItemName">
          <el-input v-model="form.bomItemName" readonly placeholder="请输入BOM物料名称" />
        </el-form-item>
        <el-form-item label="BOM物料规格" prop="bomItemSpec">
          <el-input v-model="form.bomItemSpec" readonly type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="BOM物料单位" prop="unitName">
          <el-input v-model="form.unitName" readonly placeholder="请输入BOM物料单位" />
        </el-form-item>
        <el-form-item label="物料使用比例" prop="quantity">
          <el-input-number :precision="4" :step="0.1" :min="0" v-model="form.quantity" placeholder="请输入物料使用比例" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listBom, getBom, delBom, addBom, updateBom } from "@/yunshu-ui/api/mes/md/bom";
import ItemSelect from "@/yunshu-ui/components/itemSelect/index.vue";

export default {
  name: "Bom",
  components: { ItemSelect },
  emits: ["lines-changed"],
  props: {
    optType: undefined,
    itemId: undefined,
    embedded: { type: Boolean, default: false },
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      bomList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemId: this.itemId,
        bomItemId: null,
        bomItemCode: null,
        bomItemName: null,
        bomItemSpec: null,
        unitOfMeasure: null,
        quantity: null,
        enableFlag: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
      },
      form: {},
      rules: {
        quantity: [{ required: true, message: "物料使用比例不能为空", trigger: "blur" }],
        remark: [{ max: 250, message: "字段过长", trigger: "blur" }],
      },
    };
  },
  watch: {
    itemId: {
      immediate: true,
      handler(val) {
        this.queryParams.itemId = val;
        if (val) this.getList();
        else {
          this.bomList = [];
          this.total = 0;
        }
      },
    },
  },
  created() {
    if (this.itemId) this.getList();
    else this.loading = false;
  },
  methods: {
    getList() {
      if (!this.queryParams.itemId) {
        this.bomList = [];
        this.total = 0;
        this.loading = false;
        return;
      }
      this.loading = true;
      listBom(this.queryParams).then((response) => {
        this.bomList = response.rows || [];
        this.total = response.total || 0;
        this.loading = false;
        this.$emit("lines-changed", this.total);
      }).catch(() => {
        this.bomList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        bomId: null,
        itemId: null,
        bomItemId: null,
        bomItemCode: null,
        bomItemName: null,
        bomItemSpec: null,
        unitOfMeasure: null,
        itemOrProduct: null,
        quantity: null,
        enableFlag: "Y",
        remark: null,
        attr1: null,
        attr2: null,
        attr3: null,
        attr4: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.bomId);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.$refs.itemSelect.showFlag = true;
    },
    handleUpdate(row) {
      this.reset();
      const bomId = row.bomId || this.ids;
      getBom(bomId).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = "修改产品BOM关系";
      });
    },
    submitForm() {
      this.$refs["form"].validate((valid) => {
        if (valid) {
          updateBom(this.form).then(() => {
            this.$modal.msgSuccess("修改成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },
    handleDelete(row) {
      const bomIds = row.bomId || this.ids;
      this.$modal.confirm('是否确认删除产品BOM关系编号为"' + bomIds + '"的数据项？').then(() => delBom(bomIds)).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    onItemSelected(obj) {
      this.form.itemId = this.itemId;
      if (obj != undefined && obj != null) {
        obj.forEach((element) => {
          this.form.bomItemId = element.itemId;
          this.form.bomItemCode = element.itemCode;
          this.form.bomItemName = element.itemName;
          this.form.bomItemSpec = element.specification;
          this.form.unitOfMeasure = element.unitOfMeasure;
          this.form.itemOrProduct = element.itemOrProduct;
          this.form.unitName = element.unitName;
          this.form.quantity = 1;
          this.form.enableFlag = "Y";
          addBom(this.form).then(() => {
            this.$modal.msgSuccess("新增成功");
            this.getList();
          });
        });
      }
    },
  },
};
</script>



























