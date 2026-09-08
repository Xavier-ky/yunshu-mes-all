import provider from "@/yunshu-ui/print/template/ProviderData";
import {hiprint} from 'vue-plugin-hiprint'
import {PRINT_DICT} from "@/yunshu-ui/utils/print";
import webSite from "@/config/website";

/**
 * hiprint初始化
 */
export const hiprintMixin = {
  data() {
    return {
      hiprintTemplate: null,
      hiprintThis: null
    };
  },
  mounted() {
    hiprint.init({
      providers: [provider],
      host: webSite.print_transfer_url,
      token: webSite.print_transfer_token
    });
    this.hiprintThis = hiprint
    this.hiprintTemplate = new hiprint.PrintTemplate();
  },
};
