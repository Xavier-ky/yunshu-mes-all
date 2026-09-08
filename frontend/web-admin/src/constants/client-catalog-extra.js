/** 仅采购页展示用，不写入 product 表；与后端 VIRTUAL_CATALOG_PRODUCT_ID 一致 */
export const VIRTUAL_CATALOG_PRODUCT_ID = 999999;

export const VIRTUAL_CATALOG_ITEM = {
  productId: VIRTUAL_CATALOG_PRODUCT_ID,
  productCode: "FAN-HM28-H",
  productName: "28cm手持电风扇H型",
  productModel: "HM28-H",
  category: "HANDHELD_FAN",
  categoryLabel: "手持扇",
  specs: ["扇叶直径：28", "电池续航：8h", "颜色：天蓝"],
  imageUrl: "/images/client-catalog/FAN-HM28-H.webp",
  displayPrice: "¥169",
  tagline: "无线便携 · 桌面手持两用",
};

export function appendVirtualCatalogItem(items) {
  const list = [...(items || [])];
  if (!list.some((p) => p.productCode === VIRTUAL_CATALOG_ITEM.productCode)) {
    list.push({ ...VIRTUAL_CATALOG_ITEM });
  }
  return list;
}
