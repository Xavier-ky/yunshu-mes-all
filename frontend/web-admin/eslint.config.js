import pluginVue from "eslint-plugin-vue";

export default [
  ...pluginVue.configs["flat/recommended"],
  {
    files: ["**/*.{js,mjs,cjs,vue}"],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
    },
    rules: {
      "vue/no-v-model-argument": "off",
      "vue/no-v-for-with-v-if": "off",
      "vue/no-v-for-template-key-on-child": "off",
      "vue/multi-word-component-names": "off",
      "no-unused-vars": "warn",
    },
  },
];
