package com.yunshu.mes.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Builds a polished DOCX only from the immutable quality-report snapshot created by Agent. */
@Service
public class AgentQualityReportExportService {

    private static final String TITLE_FONT = "SimHei";
    private static final String BODY_FONT = "SimSun";
    private static final int PAGE_WIDTH_DXA = 11906; // A4 portrait
    private static final int PAGE_HEIGHT_DXA = 16838;
    private static final int PAGE_MARGIN_DXA = 900; // 1.59 cm
    private static final int CONTENT_WIDTH_DXA = PAGE_WIDTH_DXA - PAGE_MARGIN_DXA * 2;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AgentQualityReportExportService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public byte[] export(Long userId, String reportId) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList("""
                    SELECT status, snapshot_json
                    FROM agent_report_artifact
                    WHERE report_id = ? AND user_id = ?
                    """, reportId, userId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "质量报告快照暂不可用", ex);
        }
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "质量报告不存在或不属于当前用户");
        }
        String status = String.valueOf(rows.get(0).get("status"));
        if (!"READY".equals(status) && !"EXPORTED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "质量报告尚未生成完成");
        }
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    String.valueOf(rows.get(0).get("snapshot_json")), new TypeReference<>() {});
            return buildDocument(snapshot);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "质量报告 Word 生成失败", ex);
        }
    }

    private byte[] buildDocument(Map<String, Object> snapshot) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePage(document);

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            title.setSpacingBefore(100);
            title.setSpacingAfter(140);
            addRun(title, text(snapshot.get("title"), "质量分析报告"), 22, true, "000000", TITLE_FONT);

            XWPFParagraph meta = document.createParagraph();
            meta.setAlignment(ParagraphAlignment.CENTER);
            meta.setSpacingAfter(200);
            addRun(meta, "统计窗口：最近 " + integer(snapshot.get("window_days")) + " 天    生成时间："
                    + text(snapshot.get("queried_at"), "-") + "    数据来源：MES 实时只读快照", 9, false, "5B5B5B", BODY_FONT);

            Map<String, Object> facts = map(snapshot.get("facts"));
            Map<String, Object> summary = map(facts.get("summary"));
            heading(document, "一、关键质量指标");
            XWPFTable kpi = document.createTable(2, 4);
            configureTable(kpi, 4);
            String[] labels = {"待检任务", "今日完成", "累计合格率", "待处置"};
            String[] values = {
                String.valueOf(integer(summary.get("pendingCount"))),
                String.valueOf(integer(summary.get("todayFinished"))),
                decimal(summary.get("passRate")) + "%",
                String.valueOf(integer(summary.get("pendingDisposition"))),
            };
            for (int index = 0; index < labels.length; index++) {
                cell(kpi.getRow(0).getCell(index), labels[index], true, "EAF1FB", TITLE_FONT, ParagraphAlignment.CENTER);
                cell(kpi.getRow(1).getCell(index), values[index], false, "FFFFFF", BODY_FONT, ParagraphAlignment.CENTER);
            }

            heading(document, "二、趋势与缺陷图表");
            for (Map<String, Object> chart : maps(snapshot.get("charts"))) {
                ChartData data = chartData(chart);
                if (data.labels().isEmpty() || data.values().isEmpty()) continue;
                XWPFParagraph caption = document.createParagraph();
                caption.setAlignment(ParagraphAlignment.CENTER);
                caption.setSpacingBefore(140);
                caption.setSpacingAfter(80);
                addRun(caption, text(chart.get("title"), "质量图表"), 11, true, "000000", TITLE_FONT);

                byte[] image = barChart(text(chart.get("title"), "质量图表"), data.labels(), data.values(), colors(chart.get("colors")));
                XWPFParagraph imageParagraph = document.createParagraph();
                imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                imageParagraph.setSpacingAfter(120);
                XWPFRun imageRun = imageParagraph.createRun();
                imageRun.addPicture(new ByteArrayInputStream(image), Document.PICTURE_TYPE_PNG,
                        // Apache POI's toEMU input is points, not inches.
                        // 6.65 × 2.96 in keeps the chart centered and fills
                        // one A4 content line without crowding the prose.
                        "quality-chart.png", Units.toEMU(478.8), Units.toEMU(213.12));
            }

            heading(document, "三、数据明细");
            for (Map<String, Object> table : maps(snapshot.get("tables"))) {
                List<String> columns = strings(table.get("columns"));
                List<List<Object>> rows = tableRows(table.get("rows"));
                if (columns.isEmpty()) continue;

                XWPFParagraph tableTitle = document.createParagraph();
                tableTitle.setSpacingBefore(120);
                tableTitle.setSpacingAfter(70);
                addRun(tableTitle, text(table.get("title"), "质量明细"), 11, true, "000000", TITLE_FONT);
                XWPFTable wordTable = document.createTable(Math.max(2, rows.size() + 1), columns.size());
                configureTable(wordTable, columns.size());
                for (int col = 0; col < columns.size(); col++) {
                    cell(wordTable.getRow(0).getCell(col), columns.get(col), true, "EAF1FB", TITLE_FONT, ParagraphAlignment.CENTER);
                }
                if (rows.isEmpty()) {
                    cell(wordTable.getRow(1).getCell(0), "当前统计窗口无可用记录", false, "FFFFFF", BODY_FONT, ParagraphAlignment.LEFT);
                } else {
                    for (int row = 0; row < rows.size(); row++) {
                        for (int col = 0; col < columns.size(); col++) {
                            Object value = col < rows.get(row).size() ? rows.get(row).get(col) : "-";
                            cell(wordTable.getRow(row + 1).getCell(col), text(value, "-"), false, "FFFFFF", BODY_FONT,
                                    col == 0 ? ParagraphAlignment.LEFT : ParagraphAlignment.CENTER);
                        }
                    }
                }
            }

            heading(document, "四、风险与行动建议");
            for (Map<String, Object> section : maps(snapshot.get("sections"))) {
                XWPFParagraph sectionTitle = document.createParagraph();
                sectionTitle.setSpacingBefore(90);
                sectionTitle.setSpacingAfter(65);
                addRun(sectionTitle, text(section.get("title"), "分析结论"), 11, true, "000000", TITLE_FONT);
                XWPFParagraph content = document.createParagraph();
                content.setAlignment(ParagraphAlignment.BOTH);
                content.setSpacingAfter(110);
                content.setSpacingBetween(1.45);
                addMultilineRun(content, text(section.get("content"), "当前窗口无可用分析内容。"), 10, "000000", BODY_FONT);
            }

            XWPFParagraph disclaimer = document.createParagraph();
            disclaimer.setSpacingBefore(110);
            disclaimer.setSpacingAfter(80);
            addRun(disclaimer, "说明：本报告由 MES 实时只读数据快照生成，仅用于辅助分析与人工复核，不代表已完成处置、关闭或质量放行。", 9, false, "5B5B5B", BODY_FONT);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] barChart(String title, List<String> labels, List<Double> values, List<String> suppliedColors) throws IOException {
        int width = 1100, height = 490, left = 82, bottom = 82, top = 62, right = 48;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK); g.setFont(new Font(TITLE_FONT, Font.BOLD, 20));
        g.drawString(title, left, 34);
        double max = Math.max(1.0, values.stream().mapToDouble(Double::doubleValue).max().orElse(1.0));
        int chartHeight = height - top - bottom, chartWidth = width - left - right;
        g.setStroke(new BasicStroke(1f)); g.setColor(new Color(224, 231, 244));
        for (int line = 0; line <= 4; line++) {
            int y = top + chartHeight * line / 4;
            g.drawLine(left, y, left + chartWidth, y);
        }
        int count = Math.min(labels.size(), values.size());
        int gap = Math.max(12, chartWidth / Math.max(1, count) / 4);
        int barWidth = Math.max(20, (chartWidth - gap * (count + 1)) / Math.max(1, count));
        g.setFont(new Font(BODY_FONT, Font.PLAIN, 13));
        for (int index = 0; index < count; index++) {
            int x = left + gap + index * (barWidth + gap);
            int barHeight = (int) Math.round(chartHeight * Math.max(0, values.get(index)) / max);
            int y = top + chartHeight - barHeight;
            g.setColor(color(index < suppliedColors.size() ? suppliedColors.get(index) : "#76A5E8"));
            g.fillRoundRect(x, y, barWidth, barHeight, 10, 10);
            g.setColor(new Color(46, 46, 46));
            String label = labels.get(index);
            if (label.length() > 9) label = label.substring(0, 9) + "…";
            g.drawString(label, x, height - 45);
            g.drawString(String.format(Locale.ROOT, "%.1f", values.get(index)), x, Math.max(top + 14, y - 8));
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static void configurePage(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr() : document.getDocument().getBody().addNewSectPr();
        section.addNewPgSz().setW(BigInteger.valueOf(PAGE_WIDTH_DXA));
        section.getPgSz().setH(BigInteger.valueOf(PAGE_HEIGHT_DXA));
        CTPageMar margins = section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(PAGE_MARGIN_DXA));
        margins.setBottom(BigInteger.valueOf(PAGE_MARGIN_DXA));
        margins.setLeft(BigInteger.valueOf(PAGE_MARGIN_DXA));
        margins.setRight(BigInteger.valueOf(PAGE_MARGIN_DXA));
        margins.setHeader(BigInteger.valueOf(450));
        margins.setFooter(BigInteger.valueOf(450));
    }

    private static void configureTable(XWPFTable table, int columns) {
        table.setWidth(Integer.toString(CONTENT_WIDTH_DXA));
        table.setTableAlignment(TableRowAlign.CENTER);
        table.setCellMargins(90, 110, 90, 110);
        int width = CONTENT_WIDTH_DXA / Math.max(1, columns);
        for (var row : table.getRows()) {
            for (var tableCell : row.getTableCells()) {
                tableCell.setWidth(Integer.toString(width));
                tableCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
    }

    private static void heading(XWPFDocument document, String value) {
        XWPFParagraph p = document.createParagraph();
        p.setSpacingBefore(220);
        p.setSpacingAfter(100);
        addRun(p, value, 14, true, "000000", TITLE_FONT);
    }

    private static void cell(XWPFTableCell cell, String value, boolean bold, String color, String font, ParagraphAlignment alignment) {
        cell.setColor(color);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(alignment);
        addRun(paragraph, value, 9, bold, "000000", font);
    }

    private static void addMultilineRun(XWPFParagraph paragraph, String value, int size, String color, String font) {
        String[] lines = (value == null ? "" : value).replace("\r", "").split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            XWPFRun run = paragraph.createRun();
            applyRunStyle(run, size, false, color, font);
            run.setText(lines[index]);
            if (index < lines.length - 1) run.addBreak();
        }
    }

    private static void addRun(XWPFParagraph paragraph, String value, int size, boolean bold, String color, String font) {
        XWPFRun run = paragraph.createRun();
        run.setText(value == null ? "" : value);
        applyRunStyle(run, size, bold, color, font);
    }

    private static void applyRunStyle(XWPFRun run, int size, boolean bold, String color, String font) {
        run.setFontFamily(font);
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor(color);
        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        // This project uses the light Apache POI schema bindings, where CTRPr
        // exposes addNewRFonts() but not the optional getter helpers.
        CTFonts fonts = properties.addNewRFonts();
        fonts.setAscii(font);
        fonts.setHAnsi(font);
        fonts.setEastAsia(font);
    }

    private static ChartData chartData(Map<String, Object> chart) {
        List<String> labels = strings(chart.get("labels"));
        List<Double> values = doubles(chart.get("values"));
        if (values.isEmpty()) {
            for (Object candidate : map(chart.get("series")).values()) {
                List<Double> seriesValues = doubles(candidate);
                if (!seriesValues.isEmpty()) {
                    values = seriesValues;
                    break;
                }
            }
        }
        if (values.isEmpty()) {
            List<Map<String, Object>> items = maps(chart.get("items"));
            if (!items.isEmpty()) {
                labels = items.stream().map(item -> text(item.get("name"), "-")).toList();
                values = items.stream().map(item -> decimalValue(item.get("value"))).toList();
            }
        }
        if (labels.isEmpty() && !values.isEmpty()) {
            labels = java.util.stream.IntStream.range(0, values.size()).mapToObj(index -> "指标" + (index + 1)).toList();
        }
        int size = Math.min(labels.size(), values.size());
        return new ChartData(labels.subList(0, size), values.subList(0, size));
    }

    private static Color color(String hex) {
        try { return Color.decode(hex == null ? "#76A5E8" : hex); }
        catch (NumberFormatException ex) { return new Color(118, 165, 232); }
    }

    private record ChartData(List<String> labels, List<Double> values) {}

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) { return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(); }
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
        return result;
    }
    private static String text(Object value, String fallback) { return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value); }
    private static int integer(Object value) { try { return (int) Math.round(Double.parseDouble(String.valueOf(value))); } catch (Exception ex) { return 0; } }
    private static String decimal(Object value) { try { return String.format(Locale.ROOT, "%.1f", Double.parseDouble(String.valueOf(value))); } catch (Exception ex) { return "0.0"; } }
    private static double decimalValue(Object value) { try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ex) { return 0d; } }
    private static List<String> strings(Object value) { if (!(value instanceof List<?> list)) return List.of(); return list.stream().map(x -> text(x, "-")).toList(); }
    private static List<Double> doubles(Object value) { if (!(value instanceof List<?> list)) return List.of(); return list.stream().map(AgentQualityReportExportService::decimalValue).toList(); }
    private static List<List<Object>> tableRows(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<List<Object>> result = new ArrayList<>();
        for (Object row : list) if (row instanceof List<?> values) result.add(new ArrayList<>(values));
        return result;
    }
    private static List<String> colors(Object value) { return strings(value); }
}
