package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.service.AutocodeGenService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/autocode")
public class AutocodeController {

    private final AutocodeGenService autocodeGenService;

    public AutocodeController(AutocodeGenService autocodeGenService) {
        this.autocodeGenService = autocodeGenService;
    }

    @GetMapping({ "/get/{ruleCode}/{inputCharacter}", "/get/{ruleCode}" })
    public Map<String, Object> getCode(@PathVariable String ruleCode,
                                       @PathVariable(required = false) String inputCharacter) {
        try {
            String code = autocodeGenService.genSerialCode(ruleCode, inputCharacter);
            return MesApiResponse.ok(code);
        } catch (IllegalArgumentException ex) {
            String code = fallbackCode(ruleCode);
            return MesApiResponse.ok(code);
        }
    }

    private String fallbackCode(String ruleCode) {
        return switch (ruleCode) {
            case "WORKORDER_CODE" -> "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            case "WAREHOUSE_CODE" -> "WH" + System.currentTimeMillis() % 100000;
            case "LOCATION_CODE" -> "LOC" + System.currentTimeMillis() % 100000;
            case "AREA_CODE" -> "BIN" + System.currentTimeMillis() % 100000;
            case "ITEMRECPT_CODE" -> "IR" + System.currentTimeMillis() % 100000;
            case "ISSUE_CODE" -> "IS" + System.currentTimeMillis() % 100000;
            case "RTISSUE_CODE" -> "RT" + System.currentTimeMillis() % 100000;
            case "PRODUCTRECPT_CODE" -> "PR" + System.currentTimeMillis() % 100000;
            case "RTVENDOR_CODE" -> "RV" + System.currentTimeMillis() % 100000;
            case "PRODUCTSALES_CODE" -> "PS" + System.currentTimeMillis() % 100000;
            case "RTSALES_CODE" -> "RS" + System.currentTimeMillis() % 100000;
            case "PACKAGE_CODE" -> "PK" + System.currentTimeMillis() % 100000;
            case "QC_IQC_CODE" -> "IQC" + System.currentTimeMillis() % 100000;
            case "QC_PQC_CODE" -> "PQC" + System.currentTimeMillis() % 100000;
            case "QC_OQC_CODE" -> "OQC" + System.currentTimeMillis() % 100000;
            case "QC_RQC_CODE" -> "RQC" + System.currentTimeMillis() % 100000;
            case "QC_INDEX_CODE" -> "QI" + System.currentTimeMillis() % 100000;
            case "QC_TEMPLATE_CODE" -> "QT" + System.currentTimeMillis() % 100000;
            case "DEFECT_CODE" -> "DF" + System.currentTimeMillis() % 100000;
            case "QC_RESULT_CODE" -> "QR" + System.currentTimeMillis() % 100000;
            default -> ruleCode + System.currentTimeMillis();
        };
    }
}
