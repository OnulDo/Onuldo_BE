package com.example.onuldo.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record EmailExistsResDto (
        @Schema(example = "false")
        Boolean exists
){
}
