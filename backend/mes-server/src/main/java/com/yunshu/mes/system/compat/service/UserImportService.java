package com.yunshu.mes.system.compat.service;

import com.yunshu.mes.system.compat.ResolveResult;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.repository.ConfigCompatRepository;
import com.yunshu.mes.system.compat.repository.DeptCompatRepository;
import com.yunshu.mes.system.compat.repository.RoleCompatRepository;
import com.yunshu.mes.system.compat.repository.UserCompatRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserImportService {

    static final String[] HEADERS = {
            "登录账号", "姓名", "部门名称", "部门编码", "角色名称", "角色编码",
            "手机号码", "邮箱", "性别", "状态", "初始密码", "备注"
    };

    /** 用户列表导出列（与页面字段一致，且可再次导入更新） */
    static final String[] EXPORT_HEADERS = {
            "用户编号", "登录账号", "姓名", "工号", "部门名称", "部门编码",
            "角色名称", "角色编码", "手机号码", "邮箱", "性别", "状态", "备注", "创建时间"
    };

    private static final String SHEET_DATA = "用户数据";
    private static final String SHEET_GUIDE = "填写说明";
    private static final String SHEET_REF = "参照数据";
    private static final String EXAMPLE_USER = "demo_user";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UserCompatRepository userRepo;
    private final DeptCompatRepository deptRepo;
    private final RoleCompatRepository roleRepo;
    private final ConfigCompatRepository configRepo;
    private final PasswordEncoder passwordEncoder;

    public UserImportService(UserCompatRepository userRepo, DeptCompatRepository deptRepo,
                             RoleCompatRepository roleRepo, ConfigCompatRepository configRepo,
                             PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.roleRepo = roleRepo;
        this.configRepo = configRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public record ImportResult(int total, int created, int updated, int failed, String htmlMsg) {}

    public void writeTemplate(OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            writeDataSheet(wb, true);
            writeGuideSheet(wb);
            writeReferenceSheet(wb);
            wb.write(out);
        }
    }

    public void writeExport(OutputStream out, Map<String, String> params) throws IOException {
        List<Map<String, Object>> users = userRepo.search(params == null ? Map.of() : params, 0, 10000);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("用户列表");
            writeHeaderRow(sheet, wb, EXPORT_HEADERS);
            int rowIdx = 1;
            for (Map<String, Object> user : users) {
                Row row = sheet.createRow(rowIdx++);
                Long userId = SysCompatHelper.longObj(user.get("userId"));
                String roleName = "";
                String roleKey = "";
                if (userId != null) {
                    List<Map<String, Object>> roles = userRepo.selectRolesForUser(userId);
                    if (!roles.isEmpty()) {
                        roleName = SysCompatHelper.str(roles.get(0).get("roleName"));
                        roleKey = SysCompatHelper.str(roles.get(0).get("roleKey"));
                    }
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> dept = user.get("dept") instanceof Map<?, ?> d
                        ? (Map<String, Object>) d : Map.of();
                setCell(row, 0, userId == null ? "" : String.valueOf(userId));
                setCell(row, 1, SysCompatHelper.str(user.get("userName")));
                setCell(row, 2, SysCompatHelper.str(user.get("nickName")));
                setCell(row, 3, SysCompatHelper.str(user.get("employeeNo")));
                setCell(row, 4, SysCompatHelper.str(dept.get("deptName")));
                setCell(row, 5, SysCompatHelper.str(dept.get("deptCode")));
                setCell(row, 6, roleName);
                setCell(row, 7, roleKey);
                setCell(row, 8, SysCompatHelper.str(user.get("phonenumber")));
                setCell(row, 9, SysCompatHelper.str(user.get("email")));
                setCell(row, 10, formatSexLabel(SysCompatHelper.str(user.get("sex"))));
                setCell(row, 11, "0".equals(SysCompatHelper.str(user.get("status"))) ? "正常" : "停用");
                setCell(row, 12, SysCompatHelper.str(user.get("remark")));
                setCell(row, 13, formatCreateTime(user.get("createTime")));
            }
            autosize(sheet, EXPORT_HEADERS.length);
            wb.write(out);
        }
    }

    public ImportResult importUsers(MultipartFile file, boolean updateSupport) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ImportResult(0, 0, 0, 0, "上传文件不能为空");
        }
        String defaultPwd = configRepo.findValueByKey("sys.user.initPassword").orElse("123456");
        int total = 0;
        int created = 0;
        int updated = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheet(SHEET_DATA);
            if (sheet == null) {
                sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            }
            if (sheet == null) {
                return new ImportResult(0, 0, 0, 0, "Excel 中未找到「用户数据」工作表");
            }
            Map<String, Integer> colMap = parseHeaderMap(sheet.getRow(0));
            if (!colMap.containsKey("登录账号") || !colMap.containsKey("姓名")) {
                return new ImportResult(0, 0, 0, 0, "表头缺少必填列：登录账号、姓名");
            }
            DataFormatter fmt = new DataFormatter();
            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, colMap, fmt)) {
                    continue;
                }
                String userName = cellVal(row, colMap.get("登录账号"), fmt);
                if (EXAMPLE_USER.equalsIgnoreCase(userName)) {
                    continue;
                }
                total++;
                int excelRow = r + 1;
                try {
                    ParsedRow parsed = parseRow(row, colMap, fmt);
                    List<String> rowErrors = validateRow(parsed);
                    if (!rowErrors.isEmpty()) {
                        failed++;
                        errors.add("第 " + excelRow + " 行：" + String.join("；", rowErrors));
                        continue;
                    }
                    ResolveResult deptRes = deptRepo.resolveDeptId(parsed.deptCode, parsed.deptName);
                    if (!deptRes.ok()) {
                        failed++;
                        errors.add("第 " + excelRow + " 行：" + deptRes.error());
                        continue;
                    }
                    ResolveResult roleRes = roleRepo.resolveRoleId(parsed.roleKey, parsed.roleName);
                    if (!roleRes.ok()) {
                        failed++;
                        errors.add("第 " + excelRow + " 行：" + roleRes.error());
                        continue;
                    }
                    Optional<Map<String, Object>> existing = userRepo.findByUsername(parsed.userName);
                    if (existing.isPresent()) {
                        if (!updateSupport) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：登录账号「" + parsed.userName + "」已存在，未勾选更新");
                            continue;
                        }
                        Long userId = SysCompatHelper.longObj(existing.get().get("userId"));
                        if (StringUtils.hasText(parsed.phonenumber)
                                && userRepo.phoneExists(parsed.phonenumber, userId)) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：手机号码「" + parsed.phonenumber + "」已被占用");
                            continue;
                        }
                        if (StringUtils.hasText(parsed.email) && userRepo.emailExists(parsed.email, userId)) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：邮箱「" + parsed.email + "」已被占用");
                            continue;
                        }
                        Map<String, Object> body = toUserBody(parsed, deptRes.id(), roleRes.id());
                        body.put("userId", userId);
                        userRepo.update(body);
                        updated++;
                    } else {
                        if (userRepo.usernameExists(parsed.userName, null)) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：登录账号「" + parsed.userName + "」已存在");
                            continue;
                        }
                        if (StringUtils.hasText(parsed.phonenumber) && userRepo.phoneExists(parsed.phonenumber, null)) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：手机号码「" + parsed.phonenumber + "」已被占用");
                            continue;
                        }
                        if (StringUtils.hasText(parsed.email) && userRepo.emailExists(parsed.email, null)) {
                            failed++;
                            errors.add("第 " + excelRow + " 行：邮箱「" + parsed.email + "」已被占用");
                            continue;
                        }
                        Map<String, Object> body = toUserBody(parsed, deptRes.id(), roleRes.id());
                        String pwd = StringUtils.hasText(parsed.password) ? parsed.password : defaultPwd;
                        userRepo.insert(body, passwordEncoder.encode(pwd));
                        created++;
                    }
                } catch (IllegalArgumentException ex) {
                    failed++;
                    errors.add("第 " + excelRow + " 行：" + ex.getMessage());
                } catch (Exception ex) {
                    failed++;
                    errors.add("第 " + excelRow + " 行：导入失败 - " + ex.getMessage());
                }
            }
        }
        return new ImportResult(total, created, updated, failed, formatResultHtml(total, created, updated, failed, errors));
    }

    static String parseSex(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "0";
        }
        String s = raw.trim();
        return switch (s) {
            case "男", "0" -> "0";
            case "女", "1" -> "1";
            case "未知", "2" -> "2";
            default -> null;
        };
    }

    static String parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "0";
        }
        String s = raw.trim();
        if ("正常".equals(s) || "0".equals(s) || "启用".equals(s)) {
            return "0";
        }
        if ("停用".equals(s) || "1".equals(s) || "禁用".equals(s)) {
            return "1";
        }
        return null;
    }

    static List<String> validateRow(ParsedRow row) {
        List<String> errs = new ArrayList<>();
        if (!StringUtils.hasText(row.userName)) {
            errs.add("登录账号不能为空");
        } else if (!USERNAME_PATTERN.matcher(row.userName).matches()) {
            errs.add("登录账号须为 2–20 位字母、数字或下划线");
        }
        if (!StringUtils.hasText(row.nickName)) {
            errs.add("姓名不能为空");
        } else if (row.nickName.length() > 64) {
            errs.add("姓名不超过 64 个字符");
        }
        if (!StringUtils.hasText(row.deptCode) && !StringUtils.hasText(row.deptName)) {
            errs.add("部门名称或部门编码至少填写一项");
        }
        if (!StringUtils.hasText(row.roleKey) && !StringUtils.hasText(row.roleName)) {
            errs.add("角色名称或角色编码至少填写一项");
        }
        if (StringUtils.hasText(row.phonenumber) && !PHONE_PATTERN.matcher(row.phonenumber).matches()) {
            errs.add("手机号码格式不正确");
        }
        if (StringUtils.hasText(row.email) && !EMAIL_PATTERN.matcher(row.email).matches()) {
            errs.add("邮箱格式不正确");
        }
        if (StringUtils.hasText(row.sexRaw) && parseSex(row.sexRaw) == null) {
            errs.add("性别只能填写：男/女/未知 或 0/1/2");
        }
        if (StringUtils.hasText(row.statusRaw) && parseStatus(row.statusRaw) == null) {
            errs.add("状态只能填写：正常/停用 或 0/1");
        }
        if (StringUtils.hasText(row.password) && row.password.length() < 5) {
            errs.add("初始密码长度不能少于 5 位");
        }
        if (StringUtils.hasText(row.remark) && row.remark.length() > 500) {
            errs.add("备注不超过 500 个字符");
        }
        return errs;
    }

    private void writeDataSheet(Workbook wb, boolean includeExample) {
        Sheet sheet = wb.createSheet(SHEET_DATA);
        writeHeaderRow(sheet, wb, HEADERS);
        if (includeExample) {
            Row example = sheet.createRow(1);
            CellStyle gray = wb.createCellStyle();
            gray.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            gray.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            String[] sample = {
                    EXAMPLE_USER, "演示用户", "一车间", "PROD_WS1", "产线操作工人", "LINE_OPERATOR",
                    "13800138000", "demo@example.com", "男", "正常", "", "示例行，导入时自动跳过"
            };
            for (int i = 0; i < sample.length; i++) {
                Cell cell = example.createCell(i);
                cell.setCellValue(sample[i]);
                cell.setCellStyle(gray);
            }
        }
        autosize(sheet, HEADERS.length);
    }

    private void writeGuideSheet(Workbook wb) {
        Sheet sheet = wb.createSheet(SHEET_GUIDE);
        String[] lines = {
                "【必填列】登录账号、姓名、部门（名称或编码）、角色（名称或编码）",
                "【部门/角色】若同时填写编码与名称，以编码为准；部门/角色名称须与系统完全一致",
                "【性别】男/女/未知 或 0/1/2，留空默认男",
                "【状态】正常/停用 或 0/1，留空默认正常",
                "【初始密码】仅新建用户生效；留空使用系统默认密码（sys.user.initPassword）",
                "【更新模式】勾选「是否更新已经存在的用户数据」时，按登录账号更新资料，不修改密码",
                "【示例行】第 2 行 demo_user 为示例，导入时自动跳过",
                "【参照数据】请查看「参照数据」Sheet 中的部门与角色列表"
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 120 * 256);
    }

    private void writeReferenceSheet(Workbook wb) {
        Sheet sheet = wb.createSheet(SHEET_REF);
        int rowIdx = 0;
        Row h1 = sheet.createRow(rowIdx++);
        h1.createCell(0).setCellValue("部门编码");
        h1.createCell(1).setCellValue("部门名称");
        for (Map<String, Object> dept : deptRepo.listAllForReference()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(SysCompatHelper.str(dept.get("deptCode")));
            row.createCell(1).setCellValue(SysCompatHelper.str(dept.get("deptName")));
        }
        rowIdx++;
        Row h2 = sheet.createRow(rowIdx++);
        h2.createCell(0).setCellValue("角色编码");
        h2.createCell(1).setCellValue("角色名称");
        for (Map<String, Object> role : roleRepo.listAllForReference()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(SysCompatHelper.str(role.get("roleKey")));
            row.createCell(1).setCellValue(SysCompatHelper.str(role.get("roleName")));
        }
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 28 * 256);
    }

    private void writeHeaderRow(Sheet sheet, Workbook wb, String[] headers) {
        Row header = sheet.createRow(0);
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static String formatCreateTime(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private Map<String, Integer> parseHeaderMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (headerRow == null) {
            return map;
        }
        DataFormatter fmt = new DataFormatter();
        for (Cell cell : headerRow) {
            String name = fmt.formatCellValue(cell).trim();
            if (StringUtils.hasText(name)) {
                map.put(name, cell.getColumnIndex());
            }
        }
        return map;
    }

    private ParsedRow parseRow(Row row, Map<String, Integer> colMap, DataFormatter fmt) {
        ParsedRow p = new ParsedRow();
        p.userName = trim(cellVal(row, colMap.get("登录账号"), fmt));
        p.nickName = trim(cellVal(row, colMap.get("姓名"), fmt));
        p.deptName = trim(cellVal(row, colMap.get("部门名称"), fmt));
        p.deptCode = trim(cellVal(row, colMap.get("部门编码"), fmt));
        p.roleName = trim(cellVal(row, colMap.get("角色名称"), fmt));
        p.roleKey = trim(cellVal(row, colMap.get("角色编码"), fmt));
        p.phonenumber = trim(cellVal(row, colMap.get("手机号码"), fmt));
        p.email = trim(cellVal(row, colMap.get("邮箱"), fmt));
        p.sexRaw = trim(cellVal(row, colMap.get("性别"), fmt));
        p.statusRaw = trim(cellVal(row, colMap.get("状态"), fmt));
        p.password = cellVal(row, colMap.get("初始密码"), fmt);
        p.remark = trim(cellVal(row, colMap.get("备注"), fmt));
        return p;
    }

    private Map<String, Object> toUserBody(ParsedRow row, Long deptId, Long roleId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userName", row.userName);
        body.put("nickName", row.nickName);
        body.put("deptId", deptId);
        body.put("roleId", roleId);
        body.put("phonenumber", row.phonenumber);
        body.put("email", row.email);
        body.put("sex", parseSex(row.sexRaw));
        body.put("status", parseStatus(row.statusRaw));
        body.put("remark", row.remark);
        return body;
    }

    private String formatResultHtml(int total, int created, int updated, int failed, List<String> errors) {
        StringBuilder sb = new StringBuilder();
        if (failed == 0 && total > 0) {
            sb.append("<div style='color: #059669;'>恭喜，全部导入成功！</div>");
        } else if (failed > 0 && created + updated == 0) {
            sb.append("<div style='color: #dc2626;'>很抱歉，导入失败！</div>");
        } else {
            sb.append("<div>导入完成（部分成功）。</div>");
        }
        sb.append("<p>共 ").append(total).append(" 条，新增 ").append(created)
                .append(" 条，更新 ").append(updated).append(" 条，失败 ").append(failed).append(" 条。</p>");
        if (!errors.isEmpty()) {
            sb.append("<p style='margin-top:8px;font-weight:600;'>失败明细：</p><ul style='padding-left:18px;margin:4px 0;'>");
            int show = Math.min(errors.size(), 50);
            for (int i = 0; i < show; i++) {
                sb.append("<li>").append(escapeHtml(errors.get(i))).append("</li>");
            }
            if (errors.size() > show) {
                sb.append("<li>… 还有 ").append(errors.size() - show).append(" 条未显示</li>");
            }
            sb.append("</ul>");
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatSexLabel(String sex) {
        if ("1".equals(sex)) {
            return "女";
        }
        if ("2".equals(sex)) {
            return "未知";
        }
        return "男";
    }

    private static boolean isBlankRow(Row row, Map<String, Integer> colMap, DataFormatter fmt) {
        Integer col = colMap.get("登录账号");
        if (col == null) {
            return true;
        }
        return !StringUtils.hasText(cellVal(row, col, fmt));
    }

    private static String cellVal(Row row, Integer colIdx, DataFormatter fmt) {
        if (colIdx == null || row == null) {
            return "";
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return "";
        }
        return fmt.formatCellValue(cell).trim();
    }

    private static void setCell(Row row, int col, String val) {
        row.createCell(col).setCellValue(val == null ? "" : val);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(w + 512, 255 * 256));
        }
    }

    static final class ParsedRow {
        String userName;
        String nickName;
        String deptName;
        String deptCode;
        String roleName;
        String roleKey;
        String phonenumber;
        String email;
        String sexRaw;
        String statusRaw;
        String password;
        String remark;
    }
}
