package com.yunshu.mes.system.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UserImportServiceTest {

    @Test
    void parseSexAcceptsChineseAndNumeric() {
        assertEquals("0", UserImportService.parseSex("男"));
        assertEquals("1", UserImportService.parseSex("女"));
        assertEquals("2", UserImportService.parseSex("未知"));
        assertEquals("0", UserImportService.parseSex("0"));
        assertEquals(null, UserImportService.parseSex("其他"));
    }

    @Test
    void parseStatusAcceptsLabels() {
        assertEquals("0", UserImportService.parseStatus("正常"));
        assertEquals("1", UserImportService.parseStatus("停用"));
        assertEquals("0", UserImportService.parseStatus(""));
        assertEquals(null, UserImportService.parseStatus("冻结"));
    }

    @Test
    void validateRowRequiresCoreFields() {
        UserImportService.ParsedRow row = new UserImportService.ParsedRow();
        row.userName = "ab";
        row.nickName = "测试";
        row.deptName = "生产部";
        row.roleName = "产线操作工人";
        List<String> errs = UserImportService.validateRow(row);
        assertTrue(errs.isEmpty());

        row.userName = "x";
        errs = UserImportService.validateRow(row);
        assertTrue(errs.stream().anyMatch(e -> e.contains("登录账号")));
    }

    @Test
    void validateRowRejectsInvalidPhone() {
        UserImportService.ParsedRow row = validRow();
        row.phonenumber = "12345";
        List<String> errs = UserImportService.validateRow(row);
        assertTrue(errs.stream().anyMatch(e -> e.contains("手机")));
    }

    private static UserImportService.ParsedRow validRow() {
        UserImportService.ParsedRow row = new UserImportService.ParsedRow();
        row.userName = "worker99";
        row.nickName = "工人99";
        row.deptName = "生产部";
        row.roleName = "产线操作工人";
        return row;
    }
}
