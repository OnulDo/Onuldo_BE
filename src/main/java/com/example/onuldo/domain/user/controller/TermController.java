package com.example.onuldo.domain.user.controller;

import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.user.controller.doc.TermControllerDoc;
import com.example.onuldo.domain.user.dto.response.TermResDto;
import com.example.onuldo.domain.user.service.TermService;
import com.example.onuldo.global.common.base.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
public class TermController implements TermControllerDoc {

    private final TermService termService;

    @GetMapping("/{termType}")
    public BaseResponse<TermResDto> getTerm(TermType termType) {
        return BaseResponse.onSuccess(termService.getTerm(termType));
    }
}
