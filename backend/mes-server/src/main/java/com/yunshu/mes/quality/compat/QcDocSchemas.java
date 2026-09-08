package com.yunshu.mes.quality.compat;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QcDocSchemas {

    private QcDocSchemas() {}

    public static final Map<String, String> QC_INDEX = schema(
            "indexId", "index_id", "indexCode", "index_code", "indexName", "index_name",
            "indexType", "index_type", "qcTool", "qc_tool", "qcResultType", "qc_result_type",
            "qcResultSpc", "qc_result_spc", "remark", "remark", "attr1", "attr1", "attr2", "attr2",
            "attr3", "attr3", "attr4", "attr4", "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_TEMPLATE = schema(
            "templateId", "template_id", "templateCode", "template_code", "templateName", "template_name",
            "qcTypes", "qc_types", "enableFlag", "enable_flag", "remark", "remark",
            "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_TEMPLATE_INDEX = schema(
            "recordId", "record_id", "templateId", "template_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "docUrl", "doc_url", "remark", "remark", "attr1", "attr1", "attr2", "attr2",
            "attr3", "attr3", "attr4", "attr4", "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_TEMPLATE_PRODUCT = schema(
            "recordId", "record_id", "templateId", "template_id", "itemId", "item_id",
            "itemCode", "item_code", "itemName", "item_name", "specification", "specification",
            "unitOfMeasure", "unit_of_measure", "quantityCheck", "quantity_check",
            "quantityUnqualified", "quantity_unqualified", "crRate", "cr_rate", "majRate", "maj_rate",
            "minRate", "min_rate", "remark", "remark", "attr1", "attr1", "attr2", "attr2",
            "attr3", "attr3", "attr4", "attr4", "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_DEFECT = schema(
            "defectId", "defect_id", "defectCode", "defect_code", "defectName", "defect_name",
            "indexType", "index_type", "defectLevel", "defect_level", "processMethod", "process_method",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_IQC = schema(
            "iqcId", "iqc_id", "iqcCode", "iqc_code", "iqcName", "iqc_name", "templateId", "template_id",
            "sourceDocId", "source_doc_id", "sourceDocType", "source_doc_type", "sourceDocCode", "source_doc_code",
            "sourceLineId", "source_line_id", "vendorId", "vendor_id", "vendorCode", "vendor_code",
            "vendorName", "vendor_name", "vendorNick", "vendor_nick", "vendorBatch", "vendor_batch",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure",
            "quantityMinCheck", "quantity_min_check", "quantityMaxUnqualified", "quantity_max_unqualified",
            "quantityRecived", "quantity_recived", "quantityCheck", "quantity_check",
            "quantityQualified", "quantity_qualified", "quantityUnqualified", "quantity_unqualified",
            "crRate", "cr_rate", "majRate", "maj_rate", "minRate", "min_rate",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "checkResult", "check_result", "reciveDate", "recive_date", "inspectDate", "inspect_date",
            "inspector", "inspector", "status", "status", "remark", "remark",
            "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_IQC_LINE = schema(
            "lineId", "line_id", "iqcId", "iqc_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_IPQC = schema(
            "ipqcId", "ipqc_id", "ipqcCode", "ipqc_code", "ipqcName", "ipqc_name", "ipqcType", "ipqc_type",
            "templateId", "template_id", "sourceDocId", "source_doc_id", "sourceDocType", "source_doc_type",
            "sourceDocCode", "source_doc_code", "sourceLineId", "source_line_id",
            "workorderId", "workorder_id", "workorderCode", "workorder_code", "workorderName", "workorder_name",
            "taskId", "task_id", "taskCode", "task_code", "taskName", "task_name",
            "workstationId", "workstation_id", "workstationCode", "workstation_code", "workstationName", "workstation_name",
            "processId", "process_id", "processCode", "process_code", "processName", "process_name",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure",
            "quantityCheck", "quantity_check", "quantityUnqualified", "quantity_unqualified",
            "quantityQualified", "quantity_qualified",
            "crRate", "cr_rate", "majRate", "maj_rate", "minRate", "min_rate",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "checkResult", "check_result", "inspectDate", "inspect_date", "inspector", "inspector",
            "status", "status", "remark", "remark",
            "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_IPQC_LINE = schema(
            "lineId", "line_id", "ipqcId", "ipqc_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_OQC = schema(
            "oqcId", "oqc_id", "oqcCode", "oqc_code", "oqcName", "oqc_name", "templateId", "template_id",
            "sourceDocId", "source_doc_id", "sourceDocType", "source_doc_type", "sourceDocCode", "source_doc_code",
            "sourceLineId", "source_line_id", "clientId", "client_id", "clientCode", "client_code",
            "clientName", "client_name", "batchCode", "batch_code",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure",
            "quantityMinCheck", "quantity_min_check", "quantityMaxUnqualified", "quantity_max_unqualified",
            "quantityOut", "quantity_out", "quantityCheck", "quantity_check",
            "quantityUnqualified", "quantity_unqualified",
            "quantityQuanlified", "quantity_quanlified", "quantityQualified", "quantity_quanlified",
            "crRate", "cr_rate", "majRate", "maj_rate", "minRate", "min_rate",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "checkResult", "check_result", "outDate", "out_date", "inspectDate", "inspect_date",
            "inspector", "inspector", "status", "status", "remark", "remark",
            "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_OQC_LINE = schema(
            "lineId", "line_id", "oqcId", "oqc_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_RQC = schema(
            "rqcId", "rqc_id", "rqcCode", "rqc_code", "rqcName", "rqc_name", "templateId", "template_id",
            "sourceDocId", "source_doc_id", "sourceDocType", "source_doc_type", "sourceDocCode", "source_doc_code",
            "sourceLineId", "source_line_id", "rqcType", "rqc_type",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure", "unitName", "unit_name",
            "batchId", "batch_id", "batchCode", "batch_code",
            "quantityCheck", "quantity_check", "quantityUnqualified", "quantity_unqualified",
            "quantityQualified", "quantity_qualified", "checkResult", "check_result",
            "inspectDate", "inspect_date", "userId", "user_id", "userName", "user_name", "nickName", "nick_name",
            "status", "status", "remark", "remark",
            "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_RQC_LINE = schema(
            "lineId", "line_id", "rqcId", "rqc_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "crQuantity", "cr_quantity", "majQuantity", "maj_quantity", "minQuantity", "min_quantity",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_DEFECT_RECORD = schema(
            "recordId", "record_id", "qcType", "qc_type", "qcId", "qc_id", "lineId", "line_id",
            "defectName", "defect_name", "defectLevel", "defect_level", "defectQuantity", "defect_quantity",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_RESULT = schema(
            "resultId", "result_id", "resultCode", "result_code",
            "sourceDocId", "source_doc_id", "sourceDocCode", "source_doc_code",
            "sourceDocName", "source_doc_name", "sourceDocType", "source_doc_type",
            "itemId", "item_id", "itemCode", "item_code", "itemName", "item_name",
            "specification", "specification", "unitOfMeasure", "unit_of_measure", "snCode", "sn_code",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

    public static final Map<String, String> QC_RESULT_DETAIL = schema(
            "detailId", "detail_id", "resultId", "result_id", "indexId", "index_id",
            "indexCode", "index_code", "indexName", "index_name", "indexType", "index_type",
            "qcTool", "qc_tool", "checkMethod", "check_method", "standerVal", "stander_val",
            "unitOfMeasure", "unit_of_measure", "thresholdMax", "threshold_max", "thresholdMin", "threshold_min",
            "qcResultType", "qc_result_type", "qcResultSpc", "qc_result_spc",
            "qcValFloat", "qc_val_float", "qcValInteger", "qc_val_integer", "qcValText", "qc_val_text",
            "qcValDict", "qc_val_dict", "qcValFile", "qc_val_file", "defectFlag", "defect_flag",
            "remark", "remark", "attr1", "attr1", "attr2", "attr2", "attr3", "attr3", "attr4", "attr4",
            "createBy", "create_by", "updateBy", "update_by");

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
                f.put(k, params.get(k));
            }
        }
        return f;
    }
}
