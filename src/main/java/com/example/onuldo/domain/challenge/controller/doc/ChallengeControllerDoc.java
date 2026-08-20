package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.ChallengeManualReviewResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeResDto;
import com.example.onuldo.domain.challenge.dto.response.ChallengeVerificationResDto;
import com.example.onuldo.domain.challenge.dto.request.ChallengeVerificationReqDto;
import com.example.onuldo.domain.challenge.enums.ChallengeCategory;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPaginationDto;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import com.example.onuldo.global.config.swagger.ApiErrorCodes;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._CURSOR_INVALID_FORMAT,
            ErrorStatus._CURSOR_SIZE_INVALID
    })
    CursorPageResponse<ChallengeResDto> getChallenges(
            @Valid
            @ParameterObject
            CursorPaginationDto pagination,

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
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._CHALLENGE_NOT_FOUND
    })
    BaseResponse<ChallengeResDto> getChallenge(
            @Parameter(description = "챌린지 ID", example = "1")
            @PathVariable Long challengeId
    );

    @Operation(
            summary = "챌린지 인증",
            description = "challengeId로 참여 중인 챌린지를 찾아 fileId 기준 AWS Rekognition 라벨을 검사하고 인증합니다."
    )
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._UNAUTHORIZED,
            ErrorStatus._INVALID_TOKEN,
            ErrorStatus._TOKEN_EXPIRED,
            ErrorStatus._USER_NOT_FOUND,
            ErrorStatus._CHALLENGE_NOT_FOUND,
            ErrorStatus._PARTICIPATION_NOT_FOUND,
            ErrorStatus._FILE_NOT_FOUND,
            ErrorStatus._S3_FILE_ID_REQUIRED,
            ErrorStatus._S3_BUCKET_REQUIRED,
            ErrorStatus._CHALLENGE_NOT_STARTED,
            ErrorStatus._CHALLENGE_PARTICIPATION_ENDED,
            ErrorStatus._CHALLENGE_VERIFICATION_TIME_UNAVAILABLE,
            ErrorStatus._ALREADY_VERIFIED_TODAY,
            ErrorStatus._DUPLICATE_VERIFICATION_PHOTO,
            ErrorStatus._INTERNAL_SERVER_ERROR,
            ErrorStatus._S3_BUCKET_NOT_CONFIGURED,
            ErrorStatus._FILE_EXISTENCE_CHECK_FAILED,
            ErrorStatus._S3_PUBLIC_BASE_URL_NOT_CONFIGURED,
            ErrorStatus._VERIFICATION_RESULT_SERIALIZATION_FAILED
    })
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

    @Operation(
            summary = "인증 재검토 요청",
            description = """
                    오늘 자동 실패(AUTO_FAIL) 처리된 본인의 인증 기록에 대해 재검토를 요청합니다.
                    그 값을 복제한 MANUAL_REVIEW 요청 기록을 남긴 뒤, PASS 상태의 승인 인증 기록을 새로 추가합니다.
                    정산/달성률 계산에는 승인된 인증으로 즉시 반영됩니다.
                    """
    )
    @ApiErrorCodes({
            ErrorStatus._BAD_REQUEST,
            ErrorStatus._UNAUTHORIZED,
            ErrorStatus._INVALID_TOKEN,
            ErrorStatus._TOKEN_EXPIRED,
            ErrorStatus._USER_NOT_FOUND,
            ErrorStatus._CHALLENGE_NOT_FOUND,
            ErrorStatus._PARTICIPATION_NOT_FOUND,
            ErrorStatus._AUTO_FAIL_VERIFICATION_NOT_FOUND,
            ErrorStatus._ALREADY_VERIFIED_TODAY
    })
    BaseResponse<ChallengeManualReviewResDto> manualReviewVerification(
            @AuthUser
            Long userId,

            @Parameter(description = "챌린지 ID", example = "1")
            @PathVariable
            Long challengeId
    );
}
