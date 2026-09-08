<template>
  <div class="dv-line-thumb">
    <img
      v-if="resolvedSrc"
      class="dv-wb-line-thumb"
      :src="resolvedSrc"
      :alt="alt || '现场图片'"
      loading="lazy"
      decoding="async"
      @click="previewOpen = true"
    />
    <span v-else class="dv-wb-line-thumb--empty">—</span>
    <el-dialog v-model="previewOpen" title="现场图片" width="720px" append-to-body destroy-on-close>
      <img v-if="previewSrc" :src="previewSrc" class="dv-line-thumb__preview" :alt="alt || '现场图片'" loading="lazy" />
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "DvLineThumb",
  props: {
    src: { type: String, default: "" },
    alt: { type: String, default: "" },
  },
  data() {
    return { previewOpen: false };
  },
  computed: {
    normalizedSrc() {
      const s = (this.src || "").trim();
      if (!s) return "";
      if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("data:")) return s;
      if (s.startsWith("/")) return s;
      return `/${s}`;
    },
    resolvedSrc() {
      const s = this.normalizedSrc;
      if (!s) return "";
      if (!s.includes("/images/dv/demo/") || s.includes("-thumb.")) return s;
      return s.replace("/images/dv/demo/", "/images/dv/demo/thumbs/").replace(/\.(png|jpe?g|webp)$/i, "-thumb.jpg");
    },
    previewSrc() {
      return this.normalizedSrc || this.resolvedSrc;
    },
  },
};
</script>
