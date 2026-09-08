<template>

  <div class="sys-master-detail sys-master-detail--settings">

    <el-row :gutter="12">

      <el-col :span="8" class="sys-master-detail__aside">

        <div class="sys-master-detail__aside-inner">

          <div class="sys-master-detail__block-head sys-settings-block-head">

            <span class="sys-settings-block-head__title">字典类型</span>

            <span class="sys-settings-block-head__count">共 {{ typeTotal }} 条</span>

          </div>

          <div class="sys-master-detail__filter">

            <section class="inbound-filter-panel">

              <el-form :model="typeQuery" size="default" :inline="true" class="inbound-filter-form" label-width="0" @submit.prevent>

                <el-form-item>

                  <el-input v-model="typeQuery.dictName" placeholder="字典名称" clearable @keyup.enter="loadTypes" />

                </el-form-item>

                <el-form-item class="filter-actions">

                  <el-button type="primary" size="default" icon="el-icon-search" @click="loadTypes">搜索</el-button>

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

              ref="typeTable"

              v-loading="typeLoading"

              class="yunshu-data-table sys-table inbound-table"

              stripe

              border

              height="100%"

              highlight-current-row

              :data="typeList"

              @current-change="onTypeSelect"

            >

              <el-table-column label="字典名称" prop="dictName" min-width="100" show-overflow-tooltip />

              <el-table-column label="字典类型" prop="dictType" min-width="110" show-overflow-tooltip />

              <el-table-column label="状态" prop="status" width="72" align="center">

                <template #default="scope">

                  <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status" />

                </template>

              </el-table-column>

            </el-table>

          </div>

          <pagination

            v-show="typeTotal > 0"

            :total="typeTotal"

            :page="typeQuery.pageNum"

            @update:page="typeQuery.pageNum = $event"

            :limit="typeQuery.pageSize"

            @update:limit="typeQuery.pageSize = $event"

            @pagination="loadTypes"

          />

        </div>

      </el-col>

      <el-col :span="16" class="sys-master-detail__main">

        <div class="sys-master-detail__main-inner">

          <template v-if="selectedType">

            <div class="sys-master-detail__detail-head sys-settings-detail-head">

              <span class="sys-settings-detail-head__section">字典数据</span>

              <div class="sys-settings-detail-head__identity">

                <strong>{{ selectedType.dictName }}</strong>

                <span class="sys-master-detail__meta">{{ selectedType.dictType }}</span>

                <dict-tag :options="dict.type.sys_normal_disable" :value="selectedType.status" />

              </div>

            </div>

            <div class="sys-master-detail__detail-body">

              <DictDataPanel

                embed-mode

                :embed-dict-id="selectedType.dictId"

                :key="selectedType.dictId"

              />

            </div>

          </template>

          <div v-else class="sys-master-detail__empty">

            <el-empty description="请从左侧选择字典类型" />

          </div>

        </div>

      </el-col>

    </el-row>

  </div>

</template>



<script>

import { listType } from "@/yunshu-ui/api/system/dict/type";

import DictDataPanel from "../../dict/data.vue";



export default {

  name: "DictMasterDetail",

  dicts: ["sys_normal_disable"],

  components: { DictDataPanel },

  props: {

    initialDictId: { type: [String, Number], default: null },

  },

  data() {

    return {

      typeLoading: false,

      typeList: [],

      typeTotal: 0,

      selectedType: null,

      typeQuery: {

        pageNum: 1,

        pageSize: 10,

        dictName: undefined,

        dictType: undefined,

        status: undefined,

      },

    };

  },

  mounted() {

    this.loadTypes();

  },

  watch: {

    initialDictId() {

      this.selectInitial();

    },

  },

  methods: {

    loadTypes() {

      this.typeLoading = true;

      listType(this.typeQuery)

        .then((res) => {

          this.typeList = res.rows || [];

          this.typeTotal = res.total || 0;

          this.selectInitial();

        })

        .finally(() => {

          this.typeLoading = false;

        });

    },

    selectInitial() {

      if (!this.typeList.length) {

        this.selectedType = null;

        this.syncCurrentRow(null);

        return;

      }

      const id = this.initialDictId;

      const hit = id ? this.typeList.find((r) => String(r.dictId) === String(id)) : null;

      this.selectedType = hit || this.typeList[0];

      this.syncCurrentRow(this.selectedType);

    },

    onTypeSelect(row) {

      if (row) this.selectedType = row;

    },

    syncCurrentRow(row) {

      this.$nextTick(() => {

        this.$refs.typeTable?.setCurrentRow(row);

      });

    },

  },

};

</script>

