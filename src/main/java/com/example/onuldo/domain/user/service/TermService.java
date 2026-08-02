package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.Term;
import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.auth.repository.TermRepository;
import com.example.onuldo.domain.user.dto.response.TermResDto;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermService {

    private final TermRepository termRepository;

    public TermResDto getTerm(TermType termType) {
        if (termType != TermType.SERVICE && termType != TermType.PRIVACY && termType != TermType.REFUND) {
            throw new RestApiException(GlobalErrorStatus._INVALID_TERM_TYPE);
        }

        Term term = termRepository.findByType(termType)
                .orElseThrow(() -> new RestApiException(GlobalErrorStatus._TERM_NOT_FOUND));

        try {
            return TermResDto.builder()
                    .termType(term.getType())
                    .title(term.getTitle())
                    .effectiveDate(term.getEffectiveDate())
                    .content(term.getContent())
                    .build();
        } catch (Exception e) {
            throw new RestApiException(GlobalErrorStatus._INTERNAL_SERVER_ERROR, "약관 content JSON 변환에 실패했습니다.");
        }
    }
}
