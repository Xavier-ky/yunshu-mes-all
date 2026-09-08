package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.repository.DictDataCompatRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/dict/data")
public class DictController {

    private static final Map<String, List<Map<String, Object>>> FALLBACK = buildFallback();

    private final DictDataCompatRepository dictDataRepo;

    public DictController(DictDataCompatRepository dictDataRepo) {
        this.dictDataRepo = dictDataRepo;
    }

    @GetMapping("/type/{dictType}")
    public Map<String, Object> getByType(@PathVariable String dictType) {
        List<Map<String, Object>> rows = dictDataRepo.selectByType(dictType);
        if (rows.isEmpty()) {
            rows = FALLBACK.getOrDefault(dictType, List.of());
        }
        return MesApiResponse.ok(rows);
    }

    private static Map<String, List<Map<String, Object>>> buildFallback() {
        Map<String, List<Map<String, Object>>> d = new LinkedHashMap<>();
        d.put("mes_order_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "已确认", "CONFIRMED"),
                row(3, "审批中", "APPROVING"),
                row(4, "已审批", "APPROVED"),
                row(5, "已完成", "FINISHED"),
                row(6, "已取消", "CANCELED")));
        d.put("mes_workorder_sourcetype", List.of(
                row(1, "客户订单", "ORDER"),
                row(2, "库存备货", "STORE")));
        d.put("mes_workorder_type", List.of(
                row(1, "自制", "SELF"),
                row(2, "外协", "OUTSOURCE"),
                row(3, "采购", "PURCHASE")));
        d.put("mes_item_product", List.of(
                row(1, "物料", "ITEM"),
                row(2, "产品", "PRODUCT")));
        d.put("mes_task_status", List.of(
                row(1, "未开始", "PREPARE"),
                row(2, "进行中", "NORMAL"),
                row(3, "已完成", "FINISHED"),
                row(4, "已取消", "CANCELED")));
        d.put("sys_yes_no", List.of(
                row(1, "是", "Y"),
                row(2, "否", "N")));
        d.put("mes_itemrecpt_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待上架", "APPROVING"),
                row(3, "待执行", "APPROVED"),
                row(4, "已完成", "FINISHED"),
                row(5, "已确认", "CONFIRMED")));
        d.put("mes_issue_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待拣货", "APPROVING"),
                row(3, "待执行", "APPROVED"),
                row(4, "已完成", "FINISHED")));
        d.put("mes_rt_issue_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待上架", "UNSTOCK"),
                row(3, "待检验", "UNCHECK"),
                row(4, "待执行", "UNEXECUTE"),
                row(5, "已完成", "FINISHED")));
        d.put("mes_rt_issue_type", List.of(
                row(1, "余料", "余料"),
                row(2, "废料", "废料")));
        d.put("mes_productrecpt_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待上架", "APPROVING"),
                row(3, "待执行", "APPROVED"),
                row(4, "已完成", "FINISHED"),
                row(5, "已确认", "CONFIRMED")));
        d.put("mes_rt_vendor_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待拣货", "APPROVING"),
                row(3, "待执行", "APPROVED"),
                row(4, "已完成", "FINISHED")));
        d.put("mes_product_sales_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待拣货", "APPROVING"),
                row(3, "待执行", "APPROVED"),
                row(4, "已完成", "FINISHED")));
        d.put("mes_rt_sales_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "待上架", "UNSTOCK"),
                row(3, "待检验", "UNCHECK"),
                row(4, "待执行", "UNEXECUTE"),
                row(5, "已完成", "FINISHED")));
        d.put("mes_client_type", List.of(
                row(1, "企业", "ENTERPRISE"),
                row(2, "个人", "PERSON")));
        d.put("mes_barcode_type", List.of(
                row(1, "物料产品条码", "ITEM"),
                row(2, "小包装条码", "BOX_SMALL"),
                row(3, "库存条码", "STOCK"),
                row(4, "装箱单条码", "PACKAGE"),
                row(5, "批次条码", "BATCH"),
                row(6, "仓库条码", "WAREHOUSE"),
                row(7, "库区条码", "LOCATION"),
                row(8, "库位条码", "AREA"),
                row(9, "工作站条码", "WORKSTATION"),
                row(10, "设备条码", "MACHINERY"),
                row(11, "供应商条码", "VENDOR"),
                row(12, "客户条码", "CLIENT"),
                row(13, "车间条码", "WORKSHOP"),
                row(14, "生产工单条码", "WORKORDER"),
                row(15, "流转卡条码", "PROCARD"),
                row(16, "人员条码", "USER")));
        d.put("mes_barcode_formart", List.of(
                row(1, "QR二维码", "QR_CODE"),
                row(2, "EAN码", "EAN_CODE"),
                row(3, "UPC码", "UPC_CODE")));
        d.put("sys_normal_disable", List.of(
                row(1, "正常", "0"),
                row(2, "停用", "1")));
        d.put("sys_user_sex", List.of(
                row(1, "男", "0"),
                row(2, "女", "1"),
                row(3, "未知", "2")));
        d.put("mes_machinery_status", List.of(
                row(1, "正常", "RUNNING"),
                row(2, "运行", "WORKING"),
                row(3, "停机", "STOP"),
                row(4, "维修", "REPAIR")));
        d.put("dv_plan_type", List.of(
                row(1, "点检", "CHECK"),
                row(2, "保养", "MAINTEN")));
        d.put("mes_dvsubject_type", List.of(
                row(1, "点检", "CHECK"),
                row(2, "保养", "MAINTEN")));
        d.put("mes_cycle_type", List.of(
                row(1, "小时", "HOUR"),
                row(2, "天", "DAY"),
                row(3, "周", "WEEK"),
                row(4, "月", "MONTH"),
                row(5, "季度", "QUARTER"),
                row(6, "年", "YEAR")));
        d.put("mes_repair_result", List.of(
                row(1, "修复成功", "SUCCESS"),
                row(2, "报废", "SCRAP")));
        d.put("dv_cm_result_status", List.of(
                row(1, "正常", "Y"),
                row(2, "异常", "N")));
        d.put("mes_feedback_type", List.of(
                row(1, "自行报工", "SELF"),
                row(2, "统一报工", "UNIFY")));
        d.put("mes_feedback_status", List.of(
                row(1, "草稿", "PREPARE"),
                row(2, "已完成", "FINISHED")));
        d.put("mes_link_type", List.of(
                row(1, "SS", "SS"),
                row(2, "SF", "SF"),
                row(3, "FS", "FS"),
                row(4, "FF", "FF")));
        d.put("mes_index_type", List.of(
                row(1, "外观", "外观"),
                row(2, "尺寸", "尺寸"),
                row(3, "性能", "性能"),
                row(4, "功能", "功能"),
                row(5, "重量", "重量"),
                row(6, "其他", "其他")));
        d.put("mes_qc_type", List.of(
                row(1, "来料检验", "IQC"),
                row(2, "过程检验", "PQC"),
                row(3, "出货检验", "OQC"),
                row(4, "退料检验", "RQC")));
        d.put("mes_qc_detail_type", List.of(
                row(1, "来料检验", "IQC"),
                row(2, "外协来料检验", "OIQC"),
                row(3, "过程检验", "IPQC"),
                row(4, "出货检验", "OQC"),
                row(5, "生产退料检验", "PRQC"),
                row(6, "客户退料检验", "CRQC")));
        d.put("mes_qc_result", List.of(
                row(1, "合格", "ACCEPT"),
                row(2, "不合格", "REJECT")));
        d.put("mes_defect_level", List.of(
                row(1, "致命", "CR"),
                row(2, "严重", "MAJ"),
                row(3, "轻微", "MIN")));
        d.put("mes_qc_result_type", List.of(
                row(1, "浮点数", "FLOAT"),
                row(2, "整数", "INTEGER"),
                row(3, "文本", "TEXT"),
                row(4, "字典", "DICT"),
                row(5, "文件", "FILE"),
                row(6, "图片", "IMAGE")));
        d.put("mes_ipqc_type", List.of(
                row(1, "首检", "FIRST"),
                row(2, "末检", "FINAL"),
                row(3, "自检", "SELF"),
                row(4, "巡检", "PATROL"),
                row(5, "点检", "CHECK"),
                row(6, "成品检验", "FQC")));
        d.put("mes_pqc_type", List.of(
                row(1, "首检", "FIRST"),
                row(2, "末检", "FINAL"),
                row(3, "自检", "SELF"),
                row(4, "巡检", "PATROL"),
                row(5, "点检", "CHECK"),
                row(6, "成品检验", "FQC")));
        d.put("mes_rqc_type", List.of(
                row(1, "生产退料检验", "PRQC"),
                row(2, "客户退料检验", "CRQC")));
        d.put("mes_source_doc_type", List.of(
                row(1, "到货通知单", "ARRIVAL_NOTICE"),
                row(2, "外协入库单", "OUTSOURCE_RECPT"),
                row(3, "生产报工单", "FEEDBACK"),
                row(4, "销售出库单", "PRODUCT_SALES"),
                row(5, "生产退料单", "RT_ISSUE"),
                row(6, "销售退货单", "RT_SALES")));
        d.put("mes_andon_status", List.of(
                row(1, "待处置", "ACTIVE"),
                row(2, "已处置", "HANDLED")));
        d.put("mes_andon_level", List.of(
                row(1, "一级", "LEVEL1"),
                row(2, "二级", "LEVEL2"),
                row(3, "三级", "LEVEL3")));
        return d;
    }

    private static Map<String, Object> row(int sort, String label, String value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dictSort", sort);
        m.put("dictLabel", label);
        m.put("dictValue", value);
        m.put("listClass", "default");
        m.put("status", "0");
        return m;
    }
}
