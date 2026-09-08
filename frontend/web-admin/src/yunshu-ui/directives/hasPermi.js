import { checkPermi, checkRole } from "@/yunshu-ui/utils/permission";

function applyPermi(el, binding) {
  const { value } = binding;
  const ok = value && Array.isArray(value) && value.length > 0 ? checkPermi(value) : true;
  if (!ok && el.parentNode) {
    el.parentNode.removeChild(el);
  }
}

function applyRole(el, binding) {
  const { value } = binding;
  const ok = value && Array.isArray(value) && value.length > 0 ? checkRole(value) : true;
  if (!ok && el.parentNode) {
    el.parentNode.removeChild(el);
  }
}

export default {
  mounted(el, binding) {
    applyPermi(el, binding);
  },
  updated(el, binding) {
    applyPermi(el, binding);
  },
};

export const hasRole = {
  mounted(el, binding) {
    applyRole(el, binding);
  },
  updated(el, binding) {
    applyRole(el, binding);
  },
};
