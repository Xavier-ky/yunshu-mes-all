<template>
  <el-dialog v-model="showFlag" title="检验单（待对接）" width="400px" append-to-body>
    <p>来料检验单选择功能待对接。</p>
    <template #footer><el-button @click="showFlag=false">关闭</el-button></template>
  </el-dialog>
</template>
<script>
export default {
  name: 'IqcSelectStub',
  data() { return { showFlag: false }; }
};
</script>
