package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.ChallengePageResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.global.common.base.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Challenge", description = "챌린지 관련 API")
public interface ChallengeControllerDoc {

    @Operation(
            summary = "챌린지 목록 조회",
            description = "오프셋 페이지네이션으로 활성 챌린지 목록을 조회합니다. category와 keyword(검색어)로 필터링할 수 있습니다."
    )
    BaseResponse<ChallengePageResDto> getChallenges(
            @Parameter(description = "조회할 페이지 번호. 기본값 0", example = "0")
            @RequestParam(defaultValue = "0")
            int page,
            @Parameter(description = "페이지 크기. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size,
            @Parameter(description = "카테고리 필터")
            @RequestParam(required = false)
            ChallengeCategory category,
            @Parameter(description = "검색어")
            @RequestParam(required = false)
            String keyword
    );

    @Operation(
            summary = "챌린지 상세 조회",
            description = "활성 상태의 챌린지 1건을 조회합니다."
    )
    BaseResponse<ChallengeResDto> getChallenge(
            @Parameter(description = "챌린지 ID", example = "1")
            @PathVariable Long challengeId
    );
}
