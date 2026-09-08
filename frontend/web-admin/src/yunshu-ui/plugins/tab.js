import router from "@/router";

export default {
  closeOpenPage(obj) {
    if (obj?.path) router.push(obj.path);
    else router.back();
  },
  closePage() {
    router.back();
  },
};
