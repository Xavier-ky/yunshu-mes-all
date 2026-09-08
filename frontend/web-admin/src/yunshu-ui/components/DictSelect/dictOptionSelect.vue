<template>
  <div>
    <el-select v-model="selectedValue">
      <el-option
        v-for="item in dict"
        :key="item.dictValue"
        :label="item.dictLabel"
        :value="item.dictValue"
      >{{ item.dictLabel }}</el-option>
    </el-select>
  </div>
</template>
<script>
import { getDicts } from "@/yunshu-ui/api/system/dict/data";
export default {
  props: {
    dictName: {
      type: String,
      required: true
    },
    initialValue: {
      type: String,
      default: null
    },
    modelValue: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      selectedValue: this.modelValue || this.initialValue,
      dict: []
    };
  },
  watch: {
    selectedValue(newValue) {
      this.$emit("input", newValue);
      this.$emit("update:modelValue", newValue);
    },
    modelValue(v) {
      this.selectedValue = v;
    }
  },
  created() {
    this.getDictionary(this.dictName);
  },
  methods: {
    getDictionary(name) {
      getDicts(name).then(response => {
        this.dict = response.data || [];
        if (this.initialValue && this.dict.some(item => item.dictValue === this.initialValue)) {
          this.selectedValue = this.initialValue;
        }
      }).catch(() => {
        this.dict = [];
      });
    }
  }
};
</script>
