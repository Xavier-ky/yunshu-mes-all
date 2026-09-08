<template>
  <el-dialog v-model="showFlag" title="销售通知单行（待对接）" width="400px" append-to-body>
    <p>销售发货通知单行选择功能待对接。</p>
    <template #footer><el-button @click="showFlag=false">关闭</el-button></template>
  </el-dialog>
</template>
<script>
export default {
  name: 'SalesNoticeLineSelectStub',
  data() { return { showFlag: false }; }
};
</script>
