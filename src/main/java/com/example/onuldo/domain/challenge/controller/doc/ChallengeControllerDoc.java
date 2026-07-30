package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeVerificationResDto;
import com.example.onuldo.domain.challenge.dto.request.ChallengeVerificationReqDto;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;

@Tag(name = "Challenge", description = "챌린지 관련 API")
public interface ChallengeControllerDoc {

    @Operation(
            summary = "챌린지 목록 조회",
            description = """
                    커서 기반 페이지네이션으로 활성 챌린지 목록을 조회합니다.
                    참여자 수가 많은 순으로 정렬되며, cursor를 넘기지 않으면 첫 페이지를 반환합니다.
                    category와 keyword(검색어)로 필터링할 수 있습니다.

                    챌린지 상세 설명 json의 type 값은 다음으로 구성됩니다.
                    - h2
                    - h3
                    - paragraph
                    - linebreak
                    - blockquote
                    """
    )
    BaseResponse<CursorPageResponse<ChallengeResDto>> getChallenges(
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지는 비워둠")
            @RequestParam(required = false)
            String cursor,
            @Parameter(description = "페이지 크기. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size,
            @Parameter(description = "카테고리 필터", example = "FITNESS")
            @RequestParam(required = false)
            ChallengeCategory category,
            @Parameter(description = "검색어", example = "run")
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

    @Operation(
            summary = "챌린지 인증",
            description = "challengeId로 참여 중인 챌린지를 찾아 fileId 기준 AWS Rekognition 라벨을 검사하고 인증합니다."
    )
    BaseResponse<ChallengeVerificationResDto> verifyChallenge(
            @AuthUser
            Long userId,
            @Parameter(description = "챌린지 ID", example = "1")
            @PathVariable
            Long challengeId,
            @Valid
            @RequestBody
            ChallengeVerificationReqDto request
    );
}
