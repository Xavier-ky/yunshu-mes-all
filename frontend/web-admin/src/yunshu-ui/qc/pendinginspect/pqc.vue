<template>
  <div class="app-container qc-wb-inspect-form">
    <el-form ref="form" class="qc-wb-inspect-form__scroll" :model="form" :rules="rules" label-width="108px">
      <div v-if="form.workorderCode" class="qc-wb-inspect-form__hint">
        工单 <strong>{{ form.workorderCode }}</strong>
        <span v-if="form.itemCode"> · {{ form.itemCode }}</span>
        <span v-if="form.quantityCheck"> · 待检 {{ form.quantityCheck }}</span>
      </div>

      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="10">
          <el-form-item label="检验单编号" prop="ipqcCode">
            <div class="qc-wb-code-field">
              <el-input v-model="form.ipqcCode" placeholder="可手动输入或自动生成" clearable />
              <el-switch
                v-if="optType != 'view' && form.status == 'PREPARE'"
                v-model="autoGenFlag"
                active-color="#13ce66"
                active-text="自动生成"
                inactive-text="手动"
                @change="handleAutoGenChange"
              />
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="14">
          <el-form-item label="检验单名称" prop="ipqcName">
            <el-input v-model="form.ipqcName" placeholder="请输入检验单名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="检验类型" prop="ipqcType">
            <el-select v-model="form.ipqcType" placeholder="请选择检验类型" style="width: 100%">
              <el-option
                v-for="dict in dict.type.mes_pqc_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检测结果" prop="checkResult">
            <el-select v-model="form.checkResult" placeholder="请选择检验结果" style="width: 100%">
              <el-option
                v-for="dict in dict.type.mes_qc_result"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="检测人员" prop="inspector">
            <el-input v-model="form.inspector" placeholder="请选择检测人员">
              <template #append>
                <el-button icon="el-icon-search" @click="handleUser2Select" />
              </template>
            </el-input>
          </el-form-item>
          <UserSingleSelect ref="user2Select" @onSelected="onUser2Selected" />
        </el-col>
      </el-row>

      <el-divider content-position="left">检测数量</el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="检测数量" prop="quantityCheck">
            <el-input v-model="form.quantityCheck" placeholder="请输入检测数量" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="合格品数量" prop="quantityQualified">
            <el-input-number v-model="form.quantityQualified" :min="0" :max="99999999" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="不合格数" prop="quantityUnqualified">
            <el-input-number v-model="form.quantityUnqualified" :min="0" :max="99999999" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row v-if="form.quantityUnqualified > 0" :gutter="16">
        <el-col :span="8">
          <el-form-item label="工废" prop="quantityLaborScrap">
            <el-input-number :min="0" style="width: 100%" @change="handleScrapChanged" v-model="form.quantityLaborScrap" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="料废" prop="quantityMaterialScrap">
            <el-input-number :min="0" style="width: 100%" @change="handleScrapChanged" v-model="form.quantityMaterialScrap" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="其他" prop="quantityOtherScrap">
            <el-input-number :min="0" style="width: 100%" @change="handleScrapChanged" v-model="form.quantityOtherScrap" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">来源信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="工单编码" prop="workorderCode">
            <el-input v-model="form.workorderCode" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工作站编号" prop="workstationCode">
            <el-input v-model="form.workstationCode" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工作站名称" prop="workstationName">
            <el-input v-model="form.workstationName" readonly />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="产品编码" prop="itemCode">
            <el-input v-model="form.itemCode" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="产品名称" prop="itemName">
            <el-input v-model="form.itemName" readonly />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="任务编号" prop="taskCode">
            <el-input v-model="form.taskCode" readonly />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="检测日期" prop="inspectDate">
            <el-date-picker
              v-model="form.inspectDate"
              clearable
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择检测日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" placeholder="可选" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider v-if="form.ipqcId != null" content-position="center">检测项</el-divider>
      <el-card v-if="form.ipqcId != null" shadow="always" class="qc-wb-inspect-form__lines">
        <Ipqcline ref="line" :ipqcId="form.ipqcId" :optType="optType" />
      </el-card>
    </el-form>

    <div class="qc-wb-form-actions">
      <el-button v-if="form.status != 'PREPARE'" type="primary" @click="cancel">返回</el-button>
      <el-button v-if="form.status == 'PREPARE' && optType != 'view'" type="primary" @click="submitForm">保 存</el-button>
      <el-button v-if="form.status == 'PREPARE' && optType != 'view'" type="success" @click="handleFinish">完成检验</el-button>
      <el-button @click="cancel">取 消</el-button>
    </div>
  </div>
</template>

<script>
import { addIpqc, updateIpqc, getIpqc } from "@/yunshu-ui/api/mes/qc/ipqc";
import { genCode } from "@/yunshu-ui/api/system/autocode/rule";
import UserSingleSelect from "@/yunshu-ui/components/userSelect/single.vue";
import Ipqcline from "../ipqc/line.vue";

export default {
  name: "IpqcAdd",
  components: { Ipqcline, UserSingleSelect },
  dicts: ["mes_pqc_type", "mes_qc_result", "mes_order_status"],
  data() {
    return {
      autoGenFlag: false,
      optType: "add",
      form: {},
      rules: {
        ipqcCode: [
          { required: true, message: "请输入或自动生成检验单编号", trigger: "blur" },
          { max: 64, message: "字段过长", trigger: "blur" },
        ],
        ipqcName: [{ max: 100, message: "字段过长", trigger: "blur" }],
        ipqcType: [{ required: true, message: "请选择检验类型", trigger: "change" }],
        quantityCheck: [{ required: true, message: "检测数量不能为空", trigger: "blur" }],
        quantityUnqualified: [{ required: true, message: "不合格品数量不能为空", trigger: "blur" }],
        checkResult: [{ required: true, message: "请选择检测结果", trigger: "change" }],
        quantityQualified: [{ required: true, message: "合格品数量不能为空", trigger: "blur" }],
      },
    };
  },
  created() {
    this.reset();
  },
  mounted() {
    this.$nextTick(() => {
      if (this.form.status === "PREPARE" && !this.form.ipqcCode) {
        this.autoGenFlag = true;
        this.handleAutoGenChange(true);
      }
    });
  },
  methods: {
    cancel() {
      this.$router.push({ path: "/app/quality/workbench" });
    },
    reset() {
      const q = this.$route.query || {};
      const qty = Number(q.quantityCheck) || null;
      this.form = {
        ipqcId: null,
        ipqcCode: null,
        ipqcName: q.workOrderCode ? `${q.workOrderCode}-过程检验` : null,
        ipqcType: q.qcDetailType || "IPQC",
        templateId: null,
        sourceDocId: q.sourceDocId,
        sourceDocType: q.sourceDocType,
        sourceDocCode: q.sourceDocCode,
        sourceLineId: q.sourceLineId,
        workorderId: q.workOrderId,
        workorderCode: q.workOrderCode,
        workorderName: q.workOrderName,
        taskId: q.taskId,
        taskCode: q.taskCode,
        taskName: q.taskName,
        workstationId: q.workstationId,
        workstationCode: q.workstationCode,
        workstationName: q.workstationName,
        processId: null,
        processCode: null,
        processName: null,
        itemId: q.itemId,
        itemCode: q.itemCode,
        itemName: q.itemName,
        specification: q.specification,
        unitOfMeasure: q.unitOfMeasure,
        unitName: q.unitName,
        quantityCheck: q.quantityCheck,
        quantityUnqualified: 0,
        quantityQualified: qty,
        quantityLaborScrap: 0,
        quantityMaterialScrap: 0,
        quantityOtherScrap: 0,
        crQuantity: 0,
        majQuantity: 0,
        minQuantity: 0,
        checkResult: "ACCEPT",
        inspectDate: new Date().toISOString().slice(0, 10),
        inspector: null,
        status: "PREPARE",
        remark: null,
      };
      this.autoGenFlag = false;
      this.resetForm("form");
    },
    extractId(response) {
      const raw = response?.data ?? response;
      if (raw == null || raw === "") return null;
      const n = Number(raw);
      return Number.isFinite(n) ? n : raw;
    },
    buildPayload(statusOverride) {
      const payload = { ...this.form };
      if (statusOverride) payload.status = statusOverride;
      Object.keys(payload).forEach((key) => {
        if (payload[key] === null || payload[key] === undefined || payload[key] === "") {
          if (key !== "quantityUnqualified" && key !== "quantityQualified" && key !== "quantityCheck") {
            delete payload[key];
          }
        }
      });
      return payload;
    },
    persistForm(statusOverride) {
      const payload = this.buildPayload(statusOverride);
      if (this.form.ipqcId != null) {
        payload.ipqcId = this.form.ipqcId;
        return updateIpqc(payload).then(() => this.form.ipqcId);
      }
      return addIpqc(payload).then((response) => {
        const id = this.extractId(response);
        if (id != null) {
          this.form.ipqcId = id;
          return getIpqc(id)
            .then((detail) => {
              const row = detail?.data || detail;
              if (row?.templateId != null) this.form.templateId = row.templateId;
              return id;
            })
            .catch(() => id);
        }
        return id;
      });
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        this.persistForm()
          .then(() => {
            this.$modal.msgSuccess(this.form.ipqcId ? "保存成功" : "新增成功");
          })
          .catch((err) => {
            this.$modal.msgError(err?.message || "保存失败");
          });
      });
    },
    handleFinish() {
      if (!this.form.checkResult) {
        this.$modal.msgError("请选择检测结果！");
        return;
      }
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        this.$modal
          .confirm("是否完成检验？【完成后将不能更改】")
          .then(() => this.persistForm("PREPARE"))
          .then(() => this.persistForm("FINISHED"))
          .then(() => {
            this.$modal.msgSuccess("检验完成");
            this.cancel();
          })
          .catch((err) => {
            if (err !== "cancel" && err !== "close") {
              this.$modal.msgError(err?.message || "完成失败");
            }
          });
      });
    },
    handleUser2Select() {
      this.$refs.user2Select.showFlag = true;
    },
    onUser2Selected(row) {
      this.form.inspector = row.nickName;
    },
    handleScrapChanged() {
      this.form.quantityUnqualified =
        (Number(this.form.quantityLaborScrap) || 0) +
        (Number(this.form.quantityMaterialScrap) || 0) +
        (Number(this.form.quantityOtherScrap) || 0);
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) {
        genCode("QC_PQC_CODE")
          .then((response) => {
            const code = response?.data ?? response;
            if (typeof code === "string" && code.trim()) {
              this.form.ipqcCode = code.trim();
            } else {
              this.form.ipqcCode = `PQC-${Date.now() % 1000000}`;
            }
          })
          .catch(() => {
            this.form.ipqcCode = `PQC-${Date.now() % 1000000}`;
          });
      } else {
        this.form.ipqcCode = null;
      }
    },
  },
};
</script>
