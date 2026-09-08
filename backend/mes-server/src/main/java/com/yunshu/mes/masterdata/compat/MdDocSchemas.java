package com.yunshu.mes.masterdata.compat;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MdDocSchemas {

    private MdDocSchemas() {}

    public static final Map<String, String> MD_ITEM = schema(
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure", "unitName", "unit_name",
            "itemOrProduct", "item_or_product", "itemTypeId", "item_type_id", "itemTypeCode", "item_type_code",
            "itemTypeName", "item_type_name", "enableFlag", "enable_flag", "safeStockFlag", "safe_stock_flag",
            "minStock", "min_stock", "maxStock", "max_stock", "highValue", "high_value", "batchFlag", "batch_flag",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> MD_PRODUCT_BOM = schema(
            "bomId", "bom_id", "itemId", "item_id", "bomItemId", "bom_item_id", "bomItemCode", "bom_item_code",
            "bomItemName", "bom_item_name", "bomItemSpec", "bom_item_spec", "unitOfMeasure", "unit_of_measure",
            "itemOrProduct", "item_or_product", "quantity", "quantity", "enableFlag", "enable_flag", "remark", "remark");

    public static final Map<String, String> MD_ITEM_TYPE = schema(
            "itemTypeId", "item_type_id", "itemTypeCode", "item_type_code", "itemTypeName", "item_type_name",
            "parentTypeId", "parent_type_id", "ancestors", "ancestors", "itemOrProduct", "item_or_product",
            "orderNum", "order_num", "enableFlag", "enable_flag", "remark", "remark");

    public static final Map<String, String> MD_UNIT_MEASURE = schema(
            "measureId", "measure_id", "measureCode", "measure_code", "measureName", "measure_name",
            "primaryFlag", "primary_flag", "primaryId", "primary_id", "changeRate", "change_rate",
            "enableFlag", "enable_flag", "remark", "remark");

    public static final Map<String, String> WORKSHOP = schema(
            "workshopId", "workshop_id", "workshopCode", "workshop_code", "workshopName", "workshop_name",
            "area", "area", "charge", "charge", "enableFlag", "status", "remark", "remark");

    public static final Map<String, String> PRO_PROCESS = schema(
            "processId", "process_id", "processCode", "process_code", "processName", "process_name",
            "attention", "attention", "enableFlag", "enable_flag", "remark", "remark",
            "attr2", "attr2");

    public static final Map<String, String> PRO_PROCESS_CONTENT = schema(
            "contentId", "content_id", "processId", "process_id", "orderNum", "order_num",
            "contentText", "content_text", "device", "device", "material", "material", "docUrl", "doc_url", "remark", "remark");

    public static final Map<String, String> PRO_ROUTE = schema(
            "routeId", "route_id", "routeCode", "route_code", "routeName", "route_name",
            "routeDesc", "route_desc", "enableFlag", "enable_flag", "remark", "remark");

    public static final Map<String, String> PRO_ROUTE_PROCESS = schema(
            "recordId", "record_id", "routeId", "route_id", "processId", "process_id",
            "processCode", "process_code", "processName", "process_name", "orderNum", "order_num",
            "nextProcessId", "next_process_id", "nextProcessCode", "next_process_code", "nextProcessName", "next_process_name",
            "linkType", "link_type", "defaultPreTime", "default_pre_time", "defaultSufTime", "default_suf_time",
            "colorCode", "color_code", "keyFlag", "key_flag", "isCheck", "is_check", "remark", "remark");

    public static final Map<String, String> PRO_ROUTE_PRODUCT = schema(
            "recordId", "record_id", "routeId", "route_id", "itemId", "item_id", "itemCode", "item_code",
            "itemName", "item_name", "specification", "specification", "unitOfMeasure", "unit_of_measure",
            "unitName", "unit_name", "quantity", "quantity", "productionTime", "production_time",
            "timeUnitType", "time_unit_type", "remark", "remark");

    public static final Map<String, String> PRO_ROUTE_PRODUCT_BOM = schema(
            "recordId", "record_id", "routeId", "route_id", "processId", "process_id", "productId", "product_id",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name", "specification", "specification",
            "unitOfMeasure", "unit_of_measure", "unitName", "unit_name", "quantity", "quantity", "remark", "remark");

    public static final Map<String, String> PRO_FEEDBACK = schema(
            "recordId", "record_id", "feedbackType", "feedback_type", "feedbackCode", "feedback_code",
            "workstationId", "workstation_id", "workstationCode", "workstation_code", "workstationName", "workstation_name",
            "workorderId", "workorder_id", "workorderCode", "workorder_code", "workorderName", "workorder_name",
            "routeId", "route_id", "routeCode", "route_code", "processId", "process_id", "processCode", "process_code",
            "processName", "process_name", "taskId", "task_id", "taskCode", "task_code",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "unitOfMeasure", "unit_of_measure", "unitName", "unit_name", "specification", "specification",
            "quantity", "quantity", "quantityFeedback", "quantity_feedback", "quantityQualified", "quantity_qualified",
            "quantityUnquanlified", "quantity_unquanlified", "quantityUncheck", "quantity_uncheck",
            "userName", "user_name", "nickName", "nick_name", "feedbackChannel", "feedback_channel",
            "feedbackTime", "feedback_time", "recordUser", "record_user", "recordNick", "record_nick",
            "status", "status", "remark", "remark");

    public static final Map<String, String> WM_ITEM_CONSUME_LINE = schema(
            "lineId", "line_id", "recordId", "record_id", "materialStockId", "material_stock_id",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name", "specification", "specification",
            "unitOfMeasure", "unit_of_measure", "unitName", "unit_name", "quantityConsume", "quantity_consume",
            "batchId", "batch_id", "batchCode", "batch_code", "remark", "remark");

    public static final Map<String, String> WM_PRODUCT_PRODUCE_LINE = schema(
            "lineId", "line_id", "recordId", "record_id", "materialStockId", "material_stock_id",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name", "specification", "specification",
            "unitOfMeasure", "unit_of_measure", "unitName", "unit_name", "quantityProduce", "quantity_produce",
            "batchId", "batch_id", "batchCode", "batch_code", "remark", "remark");

    public static final Map<String, String> MD_WORKSTATION_MACHINE = schema(
            "recordId", "record_id", "workstationId", "workstation_id", "machineId", "machinery_id",
            "machineCode", "machinery_code", "machineName", "machinery_name", "quantity", "quantity", "remark", "remark");

    public static final Map<String, String> MD_WORKSTATION_TOOL = schema(
            "recordId", "record_id", "workstationId", "workstation_id", "toolTypeId", "tool_id",
            "toolTypeCode", "tool_code", "toolTypeName", "tool_name", "quantity", "quantity", "remark", "remark");

    public static final Map<String, String> MD_WORKSTATION_WORKER = schema(
            "recordId", "record_id", "workstationId", "workstation_id", "postId", "user_id",
            "postCode", "user_name", "postName", "nick_name", "remark", "remark");

    public static final Map<String, String> MD_ITEM_BATCH_CONFIG = schema(
            "configId", "config_id", "itemId", "item_id", "produceDateFlag", "produce_date_flag",
            "expireDateFlag", "expire_date_flag", "recptDateFlag", "recpt_date_flag", "vendorFlag", "vendor_flag",
            "clientFlag", "client_flag", "coCodeFlag", "co_code_flag", "poCodeFlag", "po_code_flag",
            "workorderFlag", "workorder_flag", "taskFlag", "task_flag", "workstationFlag", "workstation_flag",
            "toolFlag", "tool_flag", "moldFlag", "mold_flag", "lotNumberFlag", "lot_number_flag",
            "qualityStatusFlag", "quality_status_flag", "enableFlag", "enable_flag", "remark", "remark");

    private static Map<String, String> schema(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    public static Map<String, String> filter(Map<String, String> params, String... keys) {
        Map<String, String> f = new LinkedHashMap<>();
        for (String k : keys) {
            if (params.containsKey(k) && params.get(k) != null && !params.get(k).isBlank()) {
                String v = params.get(k);
                if ("itemTypeId".equals(k) && "0".equals(v)) {
                    continue;
                }
                f.put(k, v);
            }
        }
        return f;
    }
}
