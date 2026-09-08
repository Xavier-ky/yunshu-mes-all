import Dict from "./Dict";
import { mergeOptions } from "./DictOptions";

export function installDict(app, options = {}) {
  mergeOptions(options);
  app.mixin({
    data() {
      const dictTypes = this.$options?.dicts;
      if (!dictTypes?.length) return {};
      const dict = new Dict();
      dict.owner = this;
      // 首屏渲染前预置空数组，避免 dict.type.xxx 为 undefined 导致白屏
      for (const entry of dictTypes) {
        const type = typeof entry === "string" ? entry : entry?.type;
        if (type) {
          dict.type[type] = [];
          dict.label[type] = {};
        }
      }
      return { dict };
    },
    created() {
      if (!(this.dict instanceof Dict)) return;
      options.onCreated?.(this.dict);
      this.dict.init(this.$options.dicts).then(() => {
        options.onReady?.(this.dict);
        this.$nextTick(() => {
          this.$emit("dictReady", this.dict);
          if (typeof this.onDictReady === "function") {
            this.onDictReady(this.dict);
          }
        });
      });
    },
  });
}

export default installDict;
