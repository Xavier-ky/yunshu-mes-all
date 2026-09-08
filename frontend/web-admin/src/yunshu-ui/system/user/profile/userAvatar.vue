<template>
  <div class="user-info-head">
    <img :src="avatarUrl" title="头像" class="img-circle img-lg" alt="avatar" />
    <el-upload
      class="avatar-uploader"
      action="#"
      :show-file-list="false"
      :http-request="handleUpload"
    >
      <el-button size="small" type="primary" plain style="margin-top: 8px">更换头像</el-button>
    </el-upload>
  </div>
</template>

<script>
import { uploadAvatar } from "@/yunshu-ui/api/system/user";

export default {
  name: "UserAvatar",
  props: {
    user: { type: Object, default: () => ({}) },
  },
  computed: {
    avatarUrl() {
      return this.user.avatar || "/images/my-logo.jpg";
    },
  },
  methods: {
    handleUpload({ file }) {
      const formData = new FormData();
      formData.append("avatarfile", file);
      uploadAvatar(formData).then(() => {
        this.$modal.msgSuccess("头像已更新");
        this.$emit("refresh");
      });
    },
  },
};
</script>

<style scoped>
.user-info-head {
  text-align: center;
}
.img-circle.img-lg {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
