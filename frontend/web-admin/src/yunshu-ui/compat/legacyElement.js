import { h, defineComponent } from "vue";
import { ElButton, ElInput } from "element-plus";

function isLegacyIcon(icon) {
  return typeof icon === "string" && icon.startsWith("el-icon-");
}

function legacyIconNode(icon) {
  return h("i", { class: icon });
}

export const YunshuButton = defineComponent({
  name: "ElButton",
  inheritAttrs: false,
  props: {
    ...ElButton.props,
    icon: { type: [String, Object, Function], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () => {
      const { icon, ...rest } = props;
      if (isLegacyIcon(icon)) {
        return h(
          ElButton,
          { ...rest, ...attrs, icon: undefined },
          {
            ...slots,
            icon: () => legacyIconNode(icon),
          },
        );
      }
      return h(ElButton, { ...props, ...attrs }, slots);
    };
  },
});

export const YunshuInput = defineComponent({
  name: "ElInput",
  inheritAttrs: false,
  props: {
    ...ElInput.props,
    prefixIcon: { type: [String, Object, Function], default: undefined },
    suffixIcon: { type: [String, Object, Function], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () => {
      const { prefixIcon, suffixIcon, ...rest } = props;
      const inputSlots = { ...slots };
      if (isLegacyIcon(prefixIcon)) {
        inputSlots.prefix = () => legacyIconNode(prefixIcon);
      }
      if (isLegacyIcon(suffixIcon)) {
        inputSlots.suffix = () => legacyIconNode(suffixIcon);
      }
      return h(
        ElInput,
        {
          ...rest,
          ...attrs,
          prefixIcon: isLegacyIcon(prefixIcon) ? undefined : prefixIcon,
          suffixIcon: isLegacyIcon(suffixIcon) ? undefined : suffixIcon,
        },
        inputSlots,
      );
    };
  },
});

export function installLegacyElement(app) {
  app.component("ElButton", YunshuButton);
  app.component("ElInput", YunshuInput);
}
