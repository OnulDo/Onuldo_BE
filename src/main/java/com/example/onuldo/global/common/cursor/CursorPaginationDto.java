package com.example.onuldo.global.common.cursor;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPaginationDto {

    @Parameter(description = "이전 응답의 nextCursor 값. 첫 조회 시 미입력")
    private String cursor;

    @Parameter(description = "조회할 개수. 기본값 10, 최대 50", example = "10")
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = CursorConstants.MAX_SIZE, message = "size는 최대 50까지 가능합니다.")
    private int size = CursorConstants.DEFAULT_SIZE;
}
