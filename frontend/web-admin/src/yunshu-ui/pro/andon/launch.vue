<template><span aria-hidden="true" style="display:none" /></template>

<script>
/** @deprecated drawer removed — navigates to full launch page */
export default {
  name: "AndonLaunch",
  emits: ["submitted"],
  methods: {
    open(query = {}) {
      this.$router.push({
        path: "/app/andon/launch",
        query: { tab: "quick", ...query },
      });
    },
  },
};
</script>
