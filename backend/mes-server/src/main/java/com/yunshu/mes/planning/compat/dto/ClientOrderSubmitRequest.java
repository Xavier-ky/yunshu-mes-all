package com.yunshu.mes.planning.compat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClientOrderSubmitRequest(
        @NotBlank @Size(max = 32) String deliveryDate,
        @Size(max = 500) String remark,
        @NotEmpty @Valid List<ClientOrderLineRequest> items
) {
}
