<template>

  <div class="sys-master-detail sys-master-detail--integration">

    <el-row :gutter="12">

      <el-col :span="8" class="sys-master-detail__aside">

        <div class="sys-master-detail__aside-inner">

          <div class="sys-master-detail__block-head integration-block-head">

            <span class="integration-block-head__title">外部系统</span>

            <span class="integration-block-head__count">共 {{ systemTotal }} 条</span>

          </div>

          <div class="sys-master-detail__filter">

            <el-form :model="systemQuery" size="default" :inline="true" label-width="0" class="inbound-filter-form" @submit.prevent>

              <el-form-item prop="systemName">

                <el-input v-model="systemQuery.systemName" placeholder="系统名称" clearable @keyup.enter="loadSystems" />

              </el-form-item>

              <el-form-item class="filter-actions">

                <el-button type="primary" size="default" icon="el-icon-search" @click="loadSystems">搜索</el-button>

              </el-form-item>

            </el-form>

          </div>

          <div class="sys-master-detail__table-frame inbound-table-frame">

            <span class="frame-corner frame-corner--tl" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--tr" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--bl" aria-hidden="true"></span>

            <span class="frame-corner frame-corner--br" aria-hidden="true"></span>

            <el-table

              ref="systemTable"

              v-loading="systemLoading"

              class="yunshu-data-table sys-table inbound-table"

              stripe

              border

              height="100%"

              highlight-current-row

              :data="systemList"

              @current-change="onSystemSelect"

            >

              <el-table-column label="系统编码" prop="systemCode" align="center" header-align="center" min-width="100" show-overflow-tooltip />

              <el-table-column label="系统名称" prop="systemName" align="center" header-align="center" min-width="110" show-overflow-tooltip />

              <el-table-column label="状态" prop="status" width="80" align="center" header-align="center" />

            </el-table>

          </div>

          <pagination

            v-show="systemTotal > 0"

            :total="systemTotal"

            :page="systemQuery.pageNum"

            @update:page="systemQuery.pageNum = $event"

            :limit="systemQuery.pageSize"

            @update:limit="systemQuery.pageSize = $event"

            @pagination="loadSystems"

          />

        </div>

      </el-col>

      <el-col :span="16" class="sys-master-detail__main">

        <div class="sys-master-detail__main-inner">

          <template v-if="selectedSystem">

            <div class="sys-master-detail__detail-head integration-detail-head">

              <span class="integration-detail-head__section">接口定义</span>

              <div class="integration-detail-head__identity">

                <strong>{{ selectedSystem.systemName }}</strong>

                <span class="sys-master-detail__meta">{{ selectedSystem.systemCode }}</span>

              </div>

            </div>

            <div class="sys-master-detail__detail-body">

              <EndpointsPanel

                embed-mode

                :system-id="selectedSystem.systemId"

                :key="selectedSystem.systemId"

              />

            </div>

          </template>

          <div v-else class="sys-master-detail__empty">

            <el-empty description="请从左侧选择外部系统" />

          </div>

        </div>

      </el-col>

    </el-row>

  </div>

</template>



<script>

import { listSystems, getSystem } from "@/yunshu-ui/api/integration/systems";

import EndpointsPanel from "@/yunshu-ui/integration/endpoints/index.vue";



export default {

  name: "IntegrationMasterDetail",

  components: { EndpointsPanel },

  props: {

    initialSystemId: { type: [String, Number], default: null },

  },

  data() {

    return {

      systemLoading: false,

      systemList: [],

      systemTotal: 0,

      selectedSystem: null,

      systemQuery: {

        pageNum: 1,

        pageSize: 10,

        systemName: undefined,

      },

    };

  },

  mounted() {

    this.loadSystems();

  },

  watch: {

    initialSystemId() {

      this.selectInitial();

    },

  },

  methods: {

    loadSystems() {

      this.systemLoading = true;

      listSystems(this.systemQuery)

        .then((response) => {

          this.systemList = response.rows || [];

          this.systemTotal = response.total || 0;

          this.selectInitial();

        })

        .finally(() => {

          this.systemLoading = false;

        });

    },

    selectInitial() {

      const id = this.initialSystemId;

      if (id) {

        const hit = this.systemList.find((s) => String(s.systemId) === String(id));

        if (hit) {

          this.selectedSystem = hit;

          this.syncCurrentRow(hit);

          return;

        }

        getSystem(id)

          .then((res) => {

            const row = res.data;

            if (row && row.systemId) {

              this.selectedSystem = row;

              this.syncCurrentRow(null);

              return;

            }

            this.fallbackFirst();

          })

          .catch(() => {

            this.fallbackFirst();

          });

        return;

      }

      this.fallbackFirst();

    },

    fallbackFirst() {

      if (!this.systemList.length) {

        this.selectedSystem = null;

        this.syncCurrentRow(null);

        return;

      }

      this.selectedSystem = this.systemList[0];

      this.syncCurrentRow(this.selectedSystem);

    },

    onSystemSelect(row) {

      if (row) this.selectedSystem = row;

    },

    syncCurrentRow(row) {

      this.$nextTick(() => {

        this.$refs.systemTable?.setCurrentRow(row);

      });

    },

  },

};

</script>

