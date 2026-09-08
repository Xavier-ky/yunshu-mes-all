<template>

  <div class="sys-master-detail sys-master-detail--settings">

    <el-row :gutter="12">

      <el-col :span="8" class="sys-master-detail__aside">

        <div class="sys-master-detail__aside-inner">

          <div class="sys-master-detail__block-head sys-settings-block-head">

            <span class="sys-settings-block-head__title">编码规则</span>

            <span class="sys-settings-block-head__count">共 {{ ruleTotal }} 条</span>

          </div>

          <div class="sys-master-detail__filter">

            <section class="inbound-filter-panel">

              <el-form :model="ruleQuery" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>

                <el-form-item>

                  <el-input v-model="ruleQuery.ruleName" placeholder="规则名称" clearable @keyup.enter="loadRules" />

                </el-form-item>

                <el-form-item class="filter-actions">

                  <el-button type="primary" size="default" icon="el-icon-search" @click="loadRules">搜索</el-button>

                </el-form-item>

              </el-form>

            </section>

          </div>

          <div class="sys-master-detail__table-frame inbound-table-frame">

            <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--br" aria-hidden="true"></span>

            <el-table

              ref="ruleTable"

              v-loading="ruleLoading"

              class="yunshu-data-table sys-table inbound-table"

              stripe

              border

              height="100%"

              highlight-current-row

              :data="ruleList"

              @current-change="onRuleSelect"

            >

              <el-table-column label="规则编码" prop="ruleCode" min-width="110" show-overflow-tooltip />

              <el-table-column label="规则名称" prop="ruleName" min-width="100" show-overflow-tooltip />

              <el-table-column label="可用" prop="enableFlag" width="72" align="center">

                <template #default="scope">

                  <dict-tag :options="dict.type.sys_yes_no" :value="scope.row.enableFlag" />

                </template>

              </el-table-column>

            </el-table>

          </div>

          <pagination

            v-show="ruleTotal > 0"

            :total="ruleTotal"

            :page="ruleQuery.pageNum"

            @update:page="ruleQuery.pageNum = $event"

            :limit="ruleQuery.pageSize"

            @update:limit="ruleQuery.pageSize = $event"

            @pagination="loadRules"

          />

        </div>

      </el-col>

      <el-col :span="16" class="sys-master-detail__main">

        <div class="sys-master-detail__main-inner">

          <template v-if="selectedRule">

            <div class="sys-master-detail__detail-head sys-settings-detail-head">

              <span class="sys-settings-detail-head__section">规则组成</span>

              <div class="sys-settings-detail-head__identity">

                <strong>{{ selectedRule.ruleName }}</strong>

                <span class="sys-master-detail__meta">{{ selectedRule.ruleCode }}</span>

                <dict-tag :options="dict.type.sys_yes_no" :value="selectedRule.enableFlag" />

              </div>

            </div>

            <div class="sys-master-detail__detail-body">

              <AutocodePartPanel

                embed-mode

                :embed-rule-id="selectedRule.ruleId"

                :key="selectedRule.ruleId"

              />

            </div>

          </template>

          <div v-else class="sys-master-detail__empty">

            <el-empty description="请从左侧选择编码规则" />

          </div>

        </div>

      </el-col>

    </el-row>

  </div>

</template>



<script>

import { listRule } from "@/yunshu-ui/api/system/autocode/rule";

import AutocodePartPanel from "../../autocode/part.vue";



export default {

  name: "AutocodeMasterDetail",

  dicts: ["sys_yes_no"],

  components: { AutocodePartPanel },

  props: {

    initialRuleId: { type: [String, Number], default: null },

  },

  data() {

    return {

      ruleLoading: false,

      ruleList: [],

      ruleTotal: 0,

      selectedRule: null,

      ruleQuery: {

        pageNum: 1,

        pageSize: 10,

        ruleCode: undefined,

        ruleName: undefined,

      },

    };

  },

  mounted() {

    this.loadRules();

  },

  watch: {

    initialRuleId() {

      this.selectInitial();

    },

  },

  methods: {

    loadRules() {

      this.ruleLoading = true;

      listRule(this.ruleQuery)

        .then((res) => {

          this.ruleList = res.rows || [];

          this.ruleTotal = res.total || 0;

          this.selectInitial();

        })

        .finally(() => {

          this.ruleLoading = false;

        });

    },

    selectInitial() {

      if (!this.ruleList.length) {

        this.selectedRule = null;

        this.syncCurrentRow(null);

        return;

      }

      const id = this.initialRuleId;

      const hit = id ? this.ruleList.find((r) => String(r.ruleId) === String(id)) : null;

      this.selectedRule = hit || this.ruleList[0];

      this.syncCurrentRow(this.selectedRule);

    },

    onRuleSelect(row) {

      if (row) this.selectedRule = row;

    },

    syncCurrentRow(row) {

      this.$nextTick(() => {

        this.$refs.ruleTable?.setCurrentRow(row);

      });

    },

  },

};

</script>

