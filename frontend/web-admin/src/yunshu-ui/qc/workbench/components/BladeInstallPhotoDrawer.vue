<template>
  <el-drawer
    :model-value="visible"
    title="扇叶安装 · 过程影像复核"
    direction="rtl"
    size="62%"
    class="blade-qc-photo-drawer"
    append-to-body
    :close-on-click-modal="true"
    @close="$emit('close')"
  >
    <div class="blade-qc-photo-drawer__intro">
      <p>以下为工人「扇叶安装」工序现场留档，共 <strong>15</strong> 张，供质检员对照复核。</p>
    </div>
    <div class="blade-qc-photo-grid">
      <article
        v-for="item in photos"
        :key="item.id"
        class="blade-qc-photo-card"
      >
        <div class="blade-qc-photo-card__frame">
          <img :src="item.image" :alt="item.title" loading="lazy" />
          <span class="blade-qc-photo-card__index">{{ item.id }}</span>
        </div>
        <div class="blade-qc-photo-card__text">
          <strong>{{ item.title }}</strong>
          <span>{{ item.caption }}</span>
        </div>
      </article>
    </div>
  </el-drawer>
</template>

<script>
import { BLADE_INSTALL_QC_PHOTOS } from "../blade-install-qc-photos";

export default {
  name: "BladeInstallPhotoDrawer",
  props: {
    visible: { type: Boolean, default: false },
  },
  emits: ["close"],
  data() {
    return {
      photos: BLADE_INSTALL_QC_PHOTOS,
    };
  },
};
</script>
