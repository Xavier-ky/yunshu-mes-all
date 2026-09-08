<template>
  <div class="andon-signal-lamp" :class="lampClass" title="产线安灯状态">
    <span class="andon-signal-lamp__dot andon-signal-lamp__dot--red" />
    <span class="andon-signal-lamp__dot andon-signal-lamp__dot--amber" />
    <span class="andon-signal-lamp__dot andon-signal-lamp__dot--green" />
  </div>
</template>

<script>
export default {
  name: "SignalLamp",
  props: {
    activeCount: { type: Number, default: 0 },
    slaOverdue: { type: Number, default: 0 },
  },
  computed: {
    lampClass() {
      if (this.activeCount === 0) return "is-green";
      if (this.slaOverdue > 0) return "is-red is-blink";
      return "is-amber";
    },
  },
};
</script>
