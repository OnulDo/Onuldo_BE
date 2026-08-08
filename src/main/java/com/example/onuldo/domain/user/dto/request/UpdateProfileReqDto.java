package com.example.onuldo.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UpdateProfileReqDto (
        @Schema(example = "야호")
        String nickname,
        @Schema(example = "https://cdn.onuldo.com/profile/default.png")
        String profileImageUrl
){
}
