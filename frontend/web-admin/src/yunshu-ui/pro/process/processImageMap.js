const COVER_BY_CODE = {
  "STEP-MOTOR": "/process/cover-motor.jpg",
  "STEP-BLADE": "/process/cover-blade.jpg",
  "STEP-AGING": "/process/cover-aging.jpg",
  "STEP-PACK": "/process/cover-pack.jpg",
};

const STEP_BY_CODE = {
  "STEP-MOTOR": "/process/step-motor-01.jpg",
  "STEP-BLADE": "/process/step-blade-01.jpg",
  "STEP-AGING": "/process/step-aging-01.jpg",
  "STEP-PACK": "/process/step-pack-01.jpg",
};

/** 每道工序、每一步对应独立示意图（与步骤说明一一对应） */
const STEP_IMAGES_BY_CODE = {
  "STEP-MOTOR": {
    1: "/process/step-motor-01.jpg",
    2: "/process/cover-motor.jpg",
    3: "/process/step-aging-01.jpg",
  },
  "STEP-BLADE": {
    1: "/process/step-blade-01.jpg",
    2: "/process/cover-blade.jpg",
  },
  "STEP-AGING": {
    1: "/process/step-aging-01.jpg",
    2: "/process/cover-aging.jpg",
  },
  "STEP-PACK": {
    1: "/process/step-pack-01.jpg",
    2: "/process/cover-pack.jpg",
  },
};

export function resolveProcessCover(process) {
  if (!process) return "";
  const url = process.attr2 || process.coverUrl;
  if (url) return url;
  const code = process.processCode || "";
  return COVER_BY_CODE[code] || COVER_BY_CODE[code.toUpperCase()] || "";
}

export function resolveStepImage(content, processCode) {
  if (!content) return "";
  const code = (processCode || "").toUpperCase();
  const orderNum = Number(content.orderNum);
  const orderMap = STEP_IMAGES_BY_CODE[code];
  if (orderMap && orderNum && orderMap[orderNum]) {
    return orderMap[orderNum];
  }
  if (content.docUrl) return content.docUrl;
  return STEP_BY_CODE[code] || "";
}

const PROCESS_META = {
  "STEP-MOTOR": { typeLabel: "装配工序", station: "装配工位", hint: "电机与主机壳体压装、紧固" },
  "STEP-BLADE": { typeLabel: "装配工序", station: "装配工位", hint: "扇叶定向压装与动平衡检查" },
  "STEP-AGING": { typeLabel: "测试工序", station: "测试工位", hint: "通电老化与噪音电流检测" },
  "STEP-PACK": { typeLabel: "包装工序", station: "包装工位", hint: "缓冲包装、贴标与封箱" },
};

const DEMO_STEPS_BY_CODE = {
  "STEP-MOTOR": [
    { orderNum: 1, contentText: "安装电机到主机壳体，确认定位销与螺丝孔对齐", device: "电批", material: "M4螺丝" },
    { orderNum: 2, contentText: "锁附固定螺丝并校验扭矩值在标准范围内", device: "扭矩扳手", material: "M4螺丝" },
    { orderNum: 3, contentText: "轻载通电测试电机转向与异响", device: "测试仪", material: "—" },
  ],
  "STEP-BLADE": [
    { orderNum: 1, contentText: "扇叶按标记方向压装到电机轴并锁紧", device: "压装治具", material: "锁紧螺母" },
    { orderNum: 2, contentText: "动平衡检测并记录偏差值", device: "动平衡仪", material: "记录表" },
  ],
  "STEP-AGING": [
    { orderNum: 1, contentText: "老化架通电运行，记录电流与噪音", device: "老化架", material: "记录表" },
    { orderNum: 2, contentText: "连续运行30分钟后复测并贴合格标签", device: "老化架", material: "合格标签" },
  ],
  "STEP-PACK": [
    { orderNum: 1, contentText: "放入缓冲泡沫后封箱并贴标签", device: "封箱机", material: "纸箱" },
    { orderNum: 2, contentText: "扫码核对型号并打印外箱标签", device: "条码打印机", material: "外箱标签" },
  ],
};

export function getProcessMeta(processCode) {
  const code = (processCode || "").toUpperCase();
  return PROCESS_META[code] || { typeLabel: "通用工序", station: "—", hint: "标准生产工序" };
}

export function getDemoProcessSteps(processCode) {
  const code = (processCode || "").toUpperCase();
  return (DEMO_STEPS_BY_CODE[code] || []).map((row, index) => ({
    contentId: `demo-${code}-${index + 1}`,
    processId: null,
    orderNum: row.orderNum,
    contentText: row.contentText,
    device: row.device,
    material: row.material,
    docUrl: resolveStepImage({ orderNum: row.orderNum }, code),
    remark: `步骤 ${row.orderNum}`,
    _demo: true,
  }));
}
