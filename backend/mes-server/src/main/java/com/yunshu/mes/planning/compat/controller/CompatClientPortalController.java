package com.yunshu.mes.planning.compat.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.planning.compat.dto.ClientOrderSubmitRequest;
import com.yunshu.mes.planning.compat.service.ClientPortalService;
import com.yunshu.mes.planning.compat.vo.ClientCatalogItemVO;
import com.yunshu.mes.planning.compat.vo.ClientOrderResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
public class CompatClientPortalController {

    private final ClientPortalService clientPortalService;

    public CompatClientPortalController(ClientPortalService clientPortalService) {
        this.clientPortalService = clientPortalService;
    }

    @GetMapping("/catalog")
    public ApiResponse<List<ClientCatalogItemVO>> catalog(HttpServletRequest request) {
        return ApiResponse.success(clientPortalService.listCatalog(), request);
    }

    @PostMapping("/orders")
    public ApiResponse<ClientOrderResultVO> submitOrder(
            @Valid @RequestBody ClientOrderSubmitRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(clientPortalService.submitOrder(body), request);
    }

    @GetMapping("/orders")
    public ApiResponse<List<ClientOrderResultVO>> listOrders(HttpServletRequest request) {
        return ApiResponse.success(clientPortalService.listOrdersForClient(), request);
    }
}
