<template>
  <el-dialog v-model="showFlag" title="选择（待对接）" width="400px" append-to-body>
    <p>通知单选择功能待对接，请直接在表单中填写相关信息。</p>
    <template #footer><el-button @click="showFlag=false">关闭</el-button></template>
  </el-dialog>
</template>
<script>
export default {
  name: 'NoticeSelectStub',
  data() { return { showFlag: false }; },
  methods: { handleSelect() { this.showFlag = true; } }
};
</script>
