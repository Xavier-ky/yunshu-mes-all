<template>

  <div class="app-container sys-hub-page sys-comms-hub">

    <div class="sys-hub-head">

      <h2 class="sys-hub-head__title">通知消息</h2>

    </div>

    <el-tabs v-model="activeTab" class="sys-hub-tabs" @tab-change="onTabChange">

      <el-tab-pane label="通知公告" name="notices">

        <NoticePanel v-if="activeTab === 'notices'" />

      </el-tab-pane>

      <el-tab-pane label="站内消息" name="message">

        <MessagePanel v-if="activeTab === 'message'" />

      </el-tab-pane>

    </el-tabs>

  </div>

</template>



<script>

import NoticePanel from "../../notice/index.vue";

import MessagePanel from "../../message/index.vue";



const TABS = new Set(["notices", "message"]);



export default {

  name: "SysCommsHub",

  components: { NoticePanel, MessagePanel },

  data() {

    return { activeTab: "notices" };

  },

  mounted() {

    this.syncFromRoute();

  },

  watch: {

    "$route.query"() {

      this.syncFromRoute();

    },

  },

  methods: {

    syncFromRoute() {

      const tab = this.$route.query?.tab;

      if (tab && TABS.has(tab)) {

        this.activeTab = tab;

      }

    },

    onTabChange(name) {

      this.$router.replace({ query: { ...this.$route.query, tab: name } });

    },

  },

};

</script>

