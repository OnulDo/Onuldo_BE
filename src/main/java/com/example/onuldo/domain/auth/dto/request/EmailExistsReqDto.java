package com.example.onuldo.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record EmailExistsReqDto (
        @Schema(example = "onuldo@onuldo.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Pattern(
                regexp = "^[a-zA-Z0-9+_-]+(\\.[a-zA-Z0-9+_-]+)*@[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                        "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$",
                message = "올바른 이메일 형식이 아닙니다."
        )
        String email
){
}
