package com.yunshu.mes.andon.controller;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.andon.repository.AndonEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/andon")
public class AndonEventWriteController {

    private final AndonEventRepository repo;
    public AndonEventWriteController(AndonEventRepository repo) { this.repo = repo; }

    @PostMapping("/events/create")
    public ApiResponse<Long> create(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        try {
            Long id = repo.insert(
                (String)body.getOrDefault("andonNo","AD-"+System.currentTimeMillis()),
                Long.valueOf(body.get("andonTypeId").toString()),
                Long.valueOf(body.get("reasonId").toString()),
                Long.valueOf(body.get("lineId").toString()),
                body.get("stationId")!=null?Long.valueOf(body.get("stationId").toString()):null,
                Long.valueOf(body.get("reportUserId").toString()),
                (String)body.getOrDefault("exceptionDesc",""));
            return ApiResponse.success(id, req);
        } catch(DataAccessException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }

    @PutMapping("/events/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id, @RequestBody Map<String,String> body, HttpServletRequest req) {
        try { repo.updateStatus(id, body.getOrDefault("status","CLOSED")); return ApiResponse.success(null, req); }
        catch(DataAccessException e) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }
}
