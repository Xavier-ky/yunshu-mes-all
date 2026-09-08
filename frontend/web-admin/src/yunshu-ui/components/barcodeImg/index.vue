<template>
  <div class="flex-container">
    <el-image ref="image" class="barcodeClass" fit="contain" :src="barcodeUrl">
      <template #error>
        <div class="image-slot">
          <span class="image-slot__icon" aria-hidden="true">▦</span>
          <small v-if="!bussinessCode">暂无编号</small>
          <small v-else>二维码加载失败</small>
        </div>
      </template>
    </el-image>
  </div>
</template>
<script>
import { getBarcodeUrl } from "@/yunshu-ui/api/mes/wm/barcode";

const QR_PREFIX = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=";

function buildQrUrl(content) {
  if (!content) {
    return "";
  }
  return QR_PREFIX + encodeURIComponent(String(content));
}

export default {
  name: "BarcodeImg",
  props: {
    bussinessId: {
      type: Number,
      default: -1,
    },
    bussinessCode: {
      type: String,
      default: null,
    },
    barcodeFormart: {
      type: String,
      default: "QR_CODE",
    },
    barcodeType: {
      type: String,
      default: null,
    },
  },
  data() {
    return {
      barcodeUrl: "",
    };
  },
  watch: {
    bussinessCode: {
      immediate: true,
      handler() {
        this.getBarcode();
      },
    },
  },
  methods: {
    getBarcode() {
      this.barcodeUrl = "";
      if (!this.bussinessCode) {
        return;
      }

      const barcodeParams = {
        bussinessId: this.bussinessId,
        bussinessCode: this.bussinessCode,
        barcodeFormart: this.barcodeFormart,
        barcodeType: this.barcodeType,
      };

      getBarcodeUrl(barcodeParams)
        .then((response) => {
          const row = response?.data;
          if (row?.barcodeUrl) {
            this.barcodeUrl = row.barcodeUrl;
            return;
          }
          const content = row?.barcodeContent || this.bussinessCode;
          this.barcodeUrl = buildQrUrl(content);
        })
        .catch(() => {
          this.barcodeUrl = buildQrUrl(this.bussinessCode);
        });
    },
  },
};
</script>
<style scoped>
.barcodeClass {
  width: 100%;
  max-width: 168px;
  height: 168px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  position: relative;
  display: block;
}

.flex-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.image-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 12px;
}

.image-slot__icon {
  font-size: 28px;
  line-height: 1;
  opacity: 0.45;
}
</style>
