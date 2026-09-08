package com.yunshu.mes.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A single browser camera frame; it is forwarded only to the local verifier. */
public record FaceLoginRequest(
        @NotBlank @Size(max = 3_000_000) String imageBase64) {
}
