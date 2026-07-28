package com.example.onuldo.domain.user.controller.doc;

import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.user.dto.response.TermResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Term", description = "약관 관련 API")
public interface TermControllerDoc {
    @Operation(summary = "약관 조회", description = "서비스 이용 약관, 개인정보 처리방침, 환급 정책을 조회합니다.")
    @GetMapping("/{termType}")
    BaseResponse<TermResDto> getTerm(
            @Parameter(description = "약관 종류")
            @PathVariable TermType termType
    );
}
