package com.yunshu.mes.system.compat.service;

import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.repository.AutocodePartCompatRepository;
import com.yunshu.mes.system.compat.repository.AutocodeResultCompatRepository;
import com.yunshu.mes.system.compat.repository.AutocodeRuleCompatRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AutocodeGenService {

    private final AutocodeRuleCompatRepository ruleRepo;
    private final AutocodePartCompatRepository partRepo;
    private final AutocodeResultCompatRepository resultRepo;

    public AutocodeGenService(AutocodeRuleCompatRepository ruleRepo,
                              AutocodePartCompatRepository partRepo,
                              AutocodeResultCompatRepository resultRepo) {
        this.ruleRepo = ruleRepo;
        this.partRepo = partRepo;
        this.resultRepo = resultRepo;
    }

    public synchronized String genSerialCode(String ruleCode, String inputCharacter) {
        Map<String, Object> rule = ruleRepo.findByCode(ruleCode)
                .orElseThrow(() -> new IllegalArgumentException("未获取到指定类型:[" + ruleCode + "]的业务编码生成规则"));
        Long ruleId = SysCompatHelper.longVal(rule.get("ruleId"));
        List<Map<String, Object>> parts = partRepo.listByRuleId(ruleId);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("规则:[" + ruleCode + "]无组成分段");
        }

        StringBuilder buff = new StringBuilder();
        int lastSerialNo = 0;
        Long serialPartId = null;

        for (Map<String, Object> part : parts) {
            String partType = SysCompatHelper.str(part.get("partType"));
            String segment = switch (partType == null ? "" : partType) {
                case "FIXCHAR" -> SysCompatHelper.strOr(part.get("fixCharacter"), "");
                case "INPUTCHAR" -> inputCharacter == null ? "" : inputCharacter;
                case "NOWDATE" -> formatDate(SysCompatHelper.str(part.get("dateFormat")));
                case "SERIALNO" -> {
                    serialPartId = SysCompatHelper.longVal(part.get("partId"));
                    int start = SysCompatHelper.intObj(part.get("seriaStartNo")) == null ? 1 : SysCompatHelper.intObj(part.get("seriaStartNo"));
                    int now = SysCompatHelper.intObj(part.get("seriaNowNo")) == null ? start : SysCompatHelper.intObj(part.get("seriaNowNo"));
                    int step = SysCompatHelper.intObj(part.get("seriaStep")) == null ? 1 : SysCompatHelper.intObj(part.get("seriaStep"));
                    int next = now + step;
                    partRepo.updateSerialNow(serialPartId, next);
                    lastSerialNo = next;
                    int len = SysCompatHelper.intObj(part.get("partLength")) == null ? 4 : SysCompatHelper.intObj(part.get("partLength"));
                    yield String.format("%0" + len + "d", next);
                }
                default -> "";
            };
            buff.append(segment);
        }

        String autoCode = padding(rule, buff);
        String genDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        saveResult(ruleId, genDate, autoCode, lastSerialNo, inputCharacter);
        return autoCode;
    }

    private String padding(Map<String, Object> rule, StringBuilder sb) {
        if (!"Y".equals(SysCompatHelper.str(rule.get("isPadded")))) {
            return sb.toString();
        }
        Integer maxLength = SysCompatHelper.intObj(rule.get("maxLength"));
        if (maxLength == null || sb.length() >= maxLength) {
            return sb.toString();
        }
        String paddingChar = SysCompatHelper.strOr(rule.get("paddedChar"), "0");
        int padLen = maxLength - sb.length();
        StringBuilder result = new StringBuilder();
        if ("R".equals(SysCompatHelper.str(rule.get("paddedMethod")))) {
            result.append(sb);
            result.append(paddingChar.repeat(padLen));
        } else {
            result.append(paddingChar.repeat(padLen));
            result.append(sb);
        }
        return result.toString();
    }

    private void saveResult(Long ruleId, String genDate, String autoCode, int lastSerialNo, String inputChar) {
        Optional<Map<String, Object>> existing = resultRepo.findByRuleId(ruleId);
        if (existing.isEmpty()) {
            resultRepo.insert(ruleId, genDate, autoCode, 1, lastSerialNo, inputChar);
        } else {
            Map<String, Object> rs = existing.get();
            int genIndex = SysCompatHelper.intObj(rs.get("genIndex")) == null ? 1 : SysCompatHelper.intObj(rs.get("genIndex"));
            resultRepo.update(SysCompatHelper.longVal(rs.get("codeId")), genDate, autoCode, genIndex + 1, lastSerialNo, inputChar);
        }
    }

    private String formatDate(String pattern) {
        String fmt = StringUtils.hasText(pattern) ? pattern : "yyyyMMdd";
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(fmt.replace("YYYY", "yyyy").replace("DD", "dd")));
    }
}
