package com.example.onuldo.domain.user.controller.doc;

import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.user.dto.response.TermResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.config.swagger.ApiErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Term", description = "약관 관련 API")
public interface TermControllerDoc {
    @Operation(summary = "약관 조회", description = "서비스 이용 약관, 개인정보 처리방침, 환급 정책을 조회합니다.")
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._INVALID_TERM_TYPE,
            ErrorStatus._TERM_NOT_FOUND,
            ErrorStatus._TERM_CONTENT_PARSING_FAILED
    })
    BaseResponse<TermResDto> getTerm(
            @Parameter(description = "약관 종류")
            @PathVariable TermType termType
    );
}
