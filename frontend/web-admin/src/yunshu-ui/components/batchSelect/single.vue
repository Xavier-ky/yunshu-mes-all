<template>
  <el-dialog v-model="showFlag" title="批次选择（待对接）" width="400px" append-to-body>
    <p>批次选择功能待对接，请手动输入批次号。</p>
    <template #footer><el-button @click="showFlag=false">关闭</el-button></template>
  </el-dialog>
</template>
<script>
export default {
  name: 'BatchSelectStub',
  data() { return { showFlag: false }; }
};
</script>
