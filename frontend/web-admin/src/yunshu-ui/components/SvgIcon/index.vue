<template>
  <div v-if="isExternal" :style="styleExternalIcon" class="svg-external-icon svg-icon" />
  <i v-else-if="isElementIcon" :class="iconClass" aria-hidden="true" />
  <svg v-else :class="svgClass" aria-hidden="true">
    <use :href="iconName" />
  </svg>
</template>

<script>
import { isExternal } from "@/yunshu-ui/utils/validate";

export default {
  name: "SvgIcon",
  props: {
    iconClass: {
      type: String,
      default: "",
    },
    className: {
      type: String,
      default: "",
    },
  },
  computed: {
    isExternal() {
      return isExternal(this.iconClass);
    },
    isElementIcon() {
      const raw = (this.iconClass || "").trim();
      return raw.startsWith("el-icon-");
    },
    iconName() {
      return `#icon-${this.iconClass}`;
    },
    svgClass() {
      return this.className ? `svg-icon ${this.className}` : "svg-icon";
    },
    styleExternalIcon() {
      return {
        mask: `url(${this.iconClass}) no-repeat 50% 50%`,
        "-webkit-mask": `url(${this.iconClass}) no-repeat 50% 50%`,
      };
    },
  },
};
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}

.svg-external-icon {
  background-color: currentColor;
  mask-size: cover !important;
  display: inline-block;
  width: 1em;
  height: 1em;
}
</style>
