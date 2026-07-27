package com.example.onuldo.domain.challenge.controller;

import com.example.onuldo.domain.challenge.controller.doc.ChallengeControllerDoc;
import com.example.onuldo.domain.challenge.dto.response.ChallengePageResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.domain.challenge.service.ChallengeService;
import com.example.onuldo.global.common.base.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/challenges")
public class ChallengeController implements ChallengeControllerDoc {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "10";

    private final ChallengeService challengeService;

    @GetMapping
    public BaseResponse<ChallengePageResDto> getChallenges(
            @RequestParam(defaultValue = DEFAULT_PAGE)
            int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
            int size,
            @RequestParam(required = false)
            ChallengeCategory category,
            @RequestParam(name = "s", required = false)
            String keyword
    ) {
        return BaseResponse.onSuccess(challengeService.getChallenges(page, size, category, keyword));
    }

    @GetMapping("/{challengeId}")
    public BaseResponse<ChallengeResDto> getChallenge(
            @PathVariable Long challengeId
    ) {
        return BaseResponse.onSuccess(challengeService.getChallenge(challengeId));
    }
}
