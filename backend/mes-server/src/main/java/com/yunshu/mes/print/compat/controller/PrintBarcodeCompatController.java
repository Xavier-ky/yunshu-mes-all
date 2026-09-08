package com.yunshu.mes.print.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 浏览器打印桩 — Phase 1 不接 Netty 客户端。 */
@RestController
@RequestMapping("/api/print/barcodePrint")
public class PrintBarcodeCompatController {

    @PostMapping("/printing")
    public Map<String, Object> printing(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(Map.of("printed", true, "mode", "browser"));
    }
}
