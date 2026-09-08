<template>
  <el-dialog v-model="showFlag" title="选择（待对接）" width="400px" append-to-body>
    <p>通知单行选择功能待对接。</p>
    <template #footer><el-button @click="showFlag=false">关闭</el-button></template>
  </el-dialog>
</template>
<script>
export default {
  name: 'NoticeLineSelectStub',
  data() { return { showFlag: false }; }
};
</script>
