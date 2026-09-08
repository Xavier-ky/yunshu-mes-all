/** 扇叶安装工序 — 模拟质检过程照片（纯前端展示，不连库） */
export const BLADE_INSTALL_QC_PHOTOS = [
  { id: 1, title: "来料核对", caption: "扇叶来料与工单BOM核对", image: "/images/qc-blade-install/step-01.jpg" },
  { id: 2, title: "方向标记", caption: "确认扇叶旋转方向标识", image: "/images/qc-blade-install/step-02.jpg" },
  { id: 3, title: "轴端清洁", caption: "电机轴端清洁与异物检查", image: "/images/qc-blade-install/step-03.jpg" },
  { id: 4, title: "治具就位", caption: "压装治具定位与锁止", image: "/images/qc-blade-install/step-04.jpg" },
  { id: 5, title: "压装对位", caption: "扇叶与电机轴对位检查", image: "/images/qc-blade-install/step-05.jpg" },
  { id: 6, title: "压装过程", caption: "扇叶定向压装进行中", image: "/images/qc-blade-install/step-06.jpg" },
  { id: 7, title: "螺母预紧", caption: "锁紧螺母手工预紧", image: "/images/qc-blade-install/step-07.jpg" },
  { id: 8, title: "扭矩终紧", caption: "扭矩扳手终紧至标准值", image: "/images/qc-blade-install/step-08.jpg" },
  { id: 9, title: "间隙测量", caption: "压装后间隙游标卡尺测量", image: "/images/qc-blade-install/step-09.jpg" },
  { id: 10, title: "上平衡仪", caption: "扇叶组件上动平衡仪", image: "/images/qc-blade-install/step-10.jpg" },
  { id: 11, title: "平衡检测", caption: "动平衡运行检测中", image: "/images/qc-blade-install/step-11.jpg" },
  { id: 12, title: "偏差读数", caption: "记录动平衡偏差值", image: "/images/qc-blade-install/step-12.jpg" },
  { id: 13, title: "合格标识", caption: "贴附工序合格标签", image: "/images/qc-blade-install/step-13.jpg" },
  { id: 14, title: "外观复检", caption: "扇叶外观与装配复检", image: "/images/qc-blade-install/step-14.jpg" },
  { id: 15, title: "完工留档", caption: "工序完工拍照留档", image: "/images/qc-blade-install/step-15.jpg" },
];

export function isBladeInstallQcTask(row) {
  if (!row) return false;
  const text = [
    row.processName,
    row.taskCode,
    row.workstationName,
    row.itemName,
    row.sourceDocName,
  ]
    .filter(Boolean)
    .join(" ");
  return text.includes("扇叶安装");
}
