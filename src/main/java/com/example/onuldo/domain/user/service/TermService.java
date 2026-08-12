package com.example.onuldo.domain.user.service;

import com.example.onuldo.domain.auth.entity.Term;
import com.example.onuldo.domain.auth.enums.TermType;
import com.example.onuldo.domain.auth.repository.TermRepository;
import com.example.onuldo.domain.user.dto.response.TermResDto;
import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.InternalServerException;
import com.example.onuldo.global.common.exception.InvalidRequestException;
import com.example.onuldo.global.common.exception.NotFoundException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
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
            throw new InvalidRequestException(ErrorStatus._INVALID_TERM_TYPE);
        }

        Term term = termRepository.findByType(termType)
                .orElseThrow(() -> new NotFoundException(ErrorStatus._TERM_NOT_FOUND));

        try {
            return TermResDto.builder()
                    .termType(term.getType())
                    .title(term.getTitle())
                    .effectiveDate(term.getEffectiveDate())
                    .content(term.getContent())
                    .build();
        } catch (Exception e) {
            throw new InternalServerException(ErrorStatus._TERM_CONTENT_PARSING_FAILED);
        }
    }
}
