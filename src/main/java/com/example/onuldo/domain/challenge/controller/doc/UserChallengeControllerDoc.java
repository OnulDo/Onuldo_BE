package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.UserChallengeResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = "커서 기반 페이지네이션으로 챌린지 참여 정보를 조회합니다. "
                    + "참여 id가 최신순으로 정렬되며, cursor를 넘기지 않으면 첫 페이지를 반환합니다. "
                    + "status가 없으면 모든 상태를 조회합니다."
    )
    BaseResponse<CursorPageResponse<UserChallengeResDto>> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지는 비워둠")
            @RequestParam(required = false)
            String cursor,
            @Parameter(description = "페이지 크기. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size
    );
}
