package com.yunshu.mes.inventory.compat;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WmDocSchemas {

    private WmDocSchemas() {}

    public static final Map<String, String> ITEM_RECPT = Map.ofEntries(
            e("recptId", "recpt_id"), e("recptCode", "recpt_code"), e("recptName", "recpt_name"),
            e("iqcId", "iqc_id"), e("iqcCode", "iqc_code"), e("noticeId", "notice_id"), e("noticeCode", "notice_code"),
            e("poCode", "po_code"), e("vendorId", "vendor_id"), e("vendorCode", "vendor_code"),
            e("vendorName", "vendor_name"), e("vendorNick", "vendor_nick"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"),
            e("recptDate", "recpt_date"), e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> ITEM_RECPT_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("recptId", "recpt_id"), e("noticeLineId", "notice_line_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantityRecived", "quantity_recived"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"),
            e("produceDate", "produce_date"), e("expireDate", "expire_date"), e("lotNumber", "lot_number"),
            e("iqcCheck", "iqc_check"), e("iqcId", "iqc_id"), e("iqcCode", "iqc_code"), e("remark", "remark"));

    public static final Map<String, String> ITEM_RECPT_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("lineId", "line_id"), e("recptId", "recpt_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> ISSUE_HEADER = Map.ofEntries(
            e("issueId", "issue_id"), e("issueCode", "issue_code"), e("issueName", "issue_name"),
            e("workstationId", "workstation_id"), e("workstationCode", "workstation_code"), e("workstationName", "workstation_name"),
            e("workorderId", "workorder_id"), e("workorderCode", "workorder_code"),
            e("taskId", "task_id"), e("taskCode", "task_code"),
            e("clientId", "client_id"), e("clientCode", "client_code"), e("clientName", "client_name"), e("clientNick", "client_nick"),
            e("requiredTime", "required_time"), e("issueDate", "issue_date"), e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> ISSUE_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("issueId", "issue_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantityIssued", "quantity_issued"), e("batchId", "batch_id"),
            e("batchCode", "batch_code"), e("remark", "remark"));

    public static final Map<String, String> ISSUE_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("issueId", "issue_id"), e("lineId", "line_id"),
            e("materialStockId", "material_stock_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> RT_ISSUE = Map.ofEntries(
            e("rtId", "rt_id"), e("rtCode", "rt_code"), e("rtName", "rt_name"),
            e("workorderId", "workorder_id"), e("workorderCode", "workorder_code"),
            e("workstationId", "workstation_id"), e("workstationCode", "workstation_code"), e("workstationName", "workstation_name"),
            e("rtType", "rt_type"), e("rtDate", "rt_date"), e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> RT_ISSUE_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("rtId", "rt_id"), e("materialStockId", "material_stock_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantityRt", "quantity_rt"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("ipqcId", "ipqc_id"), e("ipqcCode", "ipqc_code"), e("qcFlag", "qc_flag"),
            e("qualityStatus", "quality_status"), e("remark", "remark"));

    public static final Map<String, String> RT_ISSUE_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("rtId", "rt_id"), e("lineId", "line_id"),
            e("materialStockId", "material_stock_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_RECPT = Map.ofEntries(
            e("recptId", "recpt_id"), e("recptCode", "recpt_code"), e("recptName", "recpt_name"),
            e("workorderId", "workorder_id"), e("workorderCode", "workorder_code"), e("workorderName", "workorder_name"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("recptDate", "recpt_date"), e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_RECPT_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("recptId", "recpt_id"), e("materialStockId", "material_stock_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantityRecived", "quantity_recived"), e("workorderId", "workorder_id"), e("workorderCode", "workorder_code"),
            e("workorderName", "workorder_name"), e("batchId", "batch_id"), e("batchCode", "batch_code"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_RECPT_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("lineId", "line_id"), e("recptId", "recpt_id"),
            e("materialStockId", "material_stock_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> RT_VENDOR = Map.ofEntries(
            e("rtId", "rt_id"), e("rtCode", "rt_code"), e("rtName", "rt_name"), e("poCode", "po_code"),
            e("vendorId", "vendor_id"), e("vendorCode", "vendor_code"), e("vendorName", "vendor_name"),
            e("vendorNick", "vendor_nick"), e("rtReason", "rt_reason"), e("transportCode", "transport_code"),
            e("transportTel", "transport_tel"), e("batchCode", "batch_code"), e("rtDate", "rt_date"),
            e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> RT_VENDOR_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("rtId", "rt_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantityRted", "quantity_rted"), e("batchId", "batch_id"),
            e("batchCode", "batch_code"), e("remark", "remark"));

    public static final Map<String, String> RT_VENDOR_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("lineId", "line_id"), e("rtId", "rt_id"),
            e("materialStockId", "material_stock_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_SALES = Map.ofEntries(
            e("salesId", "sales_id"), e("salesCode", "sales_code"), e("salesName", "sales_name"),
            e("noticeId", "notice_id"), e("noticeCode", "notice_code"), e("soCode", "so_code"),
            e("clientId", "client_id"), e("clientCode", "client_code"), e("clientName", "client_name"),
            e("clientNick", "client_nick"), e("recipient", "recipient"), e("tel", "tel"), e("address", "address"),
            e("carrier", "carrier"), e("shippingNumber", "shipping_number"), e("salesDate", "sales_date"),
            e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_SALES_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("salesId", "sales_id"), e("materialStockId", "material_stock_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantitySales", "quantity_sales"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("oqcCheck", "oqc_check"), e("oqcId", "oqc_id"), e("oqcCode", "oqc_code"),
            e("qualityStatus", "quality_status"), e("remark", "remark"));

    public static final Map<String, String> PRODUCT_SALES_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("lineId", "line_id"), e("salesId", "sales_id"),
            e("materialStockId", "material_stock_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> RT_SALES = Map.ofEntries(
            e("rtId", "rt_id"), e("rtCode", "rt_code"), e("rtName", "rt_name"), e("soCode", "so_code"),
            e("clientId", "client_id"), e("clientCode", "client_code"), e("clientName", "client_name"),
            e("clientNick", "client_nick"), e("rtDate", "rt_date"), e("rtReason", "rt_reason"),
            e("status", "status"), e("remark", "remark"));

    public static final Map<String, String> RT_SALES_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("rtId", "rt_id"), e("itemId", "item_id"), e("itemCode", "item_code"),
            e("itemName", "item_name"), e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("unitName", "unit_name"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("quantityRted", "quantity_rted"), e("qualityStatus", "quality_status"), e("remark", "remark"));

    public static final Map<String, String> RT_SALES_DETAIL = Map.ofEntries(
            e("detailId", "detail_id"), e("lineId", "line_id"), e("rtId", "rt_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"), e("unitName", "unit_name"),
            e("quantity", "quantity"), e("batchId", "batch_id"), e("batchCode", "batch_code"),
            e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"), e("warehouseName", "warehouse_name"),
            e("locationId", "location_id"), e("locationCode", "location_code"), e("locationName", "location_name"),
            e("areaId", "area_id"), e("areaCode", "area_code"), e("areaName", "area_name"), e("remark", "remark"));

    public static final Map<String, String> BARCODE = Map.ofEntries(
            e("barcodeId", "barcode_id"), e("barcodeFormart", "barcode_formart"), e("barcodeType", "barcode_type"),
            e("barcodeContent", "barcode_content"), e("bussinessId", "bussiness_id"), e("bussinessCode", "bussiness_code"),
            e("bussinessName", "bussiness_name"), e("barcodeUrl", "barcode_url"), e("enableFlag", "enable_flag"),
            e("remark", "remark"), e("attr1", "attr1"), e("attr2", "attr2"), e("attr3", "attr3"), e("attr4", "attr4"),
            e("createBy", "create_by"), e("updateBy", "update_by"));

    public static final Map<String, String> BARCODE_CONFIG = Map.ofEntries(
            e("configId", "config_id"), e("barcodeFormart", "barcode_formart"), e("barcodeType", "barcode_type"),
            e("contentFormart", "content_formart"), e("contentExample", "content_example"), e("autoGenFlag", "auto_gen_flag"),
            e("defaultTemplate", "default_template"), e("enableFlag", "enable_flag"), e("remark", "remark"),
            e("attr1", "attr1"), e("attr2", "attr2"), e("attr3", "attr3"), e("attr4", "attr4"),
            e("createBy", "create_by"), e("updateBy", "update_by"));

    public static final Map<String, String> PACKAGE = Map.ofEntries(
            e("packageId", "package_id"), e("parentId", "parent_id"), e("ancestors", "ancestors"),
            e("packageCode", "package_code"), e("barcodeId", "barcode_id"), e("barcodeContent", "barcode_content"),
            e("barcodeUrl", "barcode_url"), e("packageDate", "package_date"), e("soCode", "so_code"),
            e("invoiceCode", "invoice_code"), e("clientId", "client_id"), e("clientCode", "client_code"),
            e("clientName", "client_name"), e("clientNick", "client_nick"), e("packageLength", "package_length"),
            e("packageWidth", "package_width"), e("packageHeight", "package_height"), e("sizeUnit", "size_unit"),
            e("netWeight", "net_weight"), e("crossWeight", "cross_weight"), e("weightUnit", "weight_unit"),
            e("inspector", "inspector"), e("inspectorName", "inspector_name"), e("status", "status"),
            e("enableFlag", "enable_flag"), e("remark", "remark"), e("attr1", "attr1"), e("attr2", "attr2"),
            e("attr3", "attr3"), e("attr4", "attr4"), e("createBy", "create_by"), e("updateBy", "update_by"));

    public static final Map<String, String> PACKAGE_LINE = Map.ofEntries(
            e("lineId", "line_id"), e("packageId", "package_id"), e("materialStockId", "material_stock_id"),
            e("itemId", "item_id"), e("itemCode", "item_code"), e("itemName", "item_name"),
            e("specification", "specification"), e("unitOfMeasure", "unit_of_measure"),
            e("quantityPackage", "quantity_package"), e("workorderId", "workorder_id"), e("workorderCode", "workorder_code"),
            e("batchCode", "batch_code"), e("warehouseId", "warehouse_id"), e("warehouseCode", "warehouse_code"),
            e("warehouseName", "warehouse_name"), e("locationId", "location_id"), e("locationCode", "location_code"),
            e("locationName", "location_name"), e("areaId", "area_id"), e("areaCode", "area_code"),
            e("areaName", "area_name"), e("expireDate", "expire_date"), e("remark", "remark"),
            e("attr1", "attr1"), e("attr2", "attr2"), e("attr3", "attr3"), e("attr4", "attr4"),
            e("createBy", "create_by"), e("updateBy", "update_by"));

    private static Map.Entry<String, String> e(String camel, String snake) {
        return Map.entry(camel, snake);
    }

    public static Map<String, String> filter(Map<String, String> params, String... keys) {
        Map<String, String> f = new LinkedHashMap<>();
        for (String k : keys) {
            if (params.containsKey(k) && params.get(k) != null && !params.get(k).isBlank()) {
                f.put(k, params.get(k));
            }
        }
        return f;
    }
}
