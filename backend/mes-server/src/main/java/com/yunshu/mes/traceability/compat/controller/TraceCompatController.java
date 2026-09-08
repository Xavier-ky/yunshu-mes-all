package com.yunshu.mes.traceability.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.traceability.service.TraceabilityService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/trace")
public class TraceCompatController {

    private final TraceabilityService traceabilityService;

    public TraceCompatController(TraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @GetMapping("/product/{sn}")
    public Map<String, Object> traceProduct(@PathVariable String sn) {
        return traceabilityService.traceProduct(sn)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("未找到 SN: " + sn));
    }
}
