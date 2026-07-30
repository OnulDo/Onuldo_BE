package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.DailyChallengeListResDto;
import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = """
                    커서 기반 페이지네이션으로 챌린지 참여 정보를 조회합니다.
                    참여 id가 최신순으로 정렬되며, cursor를 넘기지 않으면 첫 페이지를 반환합니다. 
                    status가 없으면 모든 상태를 조회합니다.
                    
                    참여중인 챌린지의 상세 설명 json의 type 값은 다음으로 구성됩니다.
                    - h2
                    - h3
                    - paragraph
                    - linebreak
                    - blockquote
                    """
    )
    BaseResponse<CursorPageResponse<UserChallengeResDto>> getUserChallenges(
            @AuthUser
            Long userId,

            @RequestParam(required = false)
            ParticipationStatus status,

            @Parameter(description = "이전 응답의 nextCursor 값. 첫 조회 시 미입력")
            @RequestParam(required = false)
            String cursor,

            @Parameter(description = "조회할 개수 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size
    );

    @Operation(
            summary = "지정한 날짜의 챌린지 조회",
            description = """
    지정한 날짜 기준으로 수행해야 하는 챌린지를 조회합니다.
    조건은 `ONGOING` 상태이면서 startDate <= date <= endDate 입니다.

    각 챌린지에는 해당 날짜 인증 기록이 있는지 여부(`verifiedOnDate`)가 포함됩니다.
    """
    )
    BaseResponse<DailyChallengeListResDto> getDailyChallenges(
            @Parameter(hidden = true)
            @AuthUser
            Long userId,

            @Parameter(description = "조회할 날짜", example = "2026-07-29")
            @RequestParam(required = true)
            LocalDate date
    );
}
