package com.example.onuldo.domain.challenge.controller.doc;

import com.example.onuldo.domain.challenge.dto.response.UserChallengeListResDto;
import com.example.onuldo.domain.challenge.enums.ParticipationStatus;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Challenge")
public interface UserChallengeControllerDoc {

    @Operation(
            summary = "내 챌린지 참여 목록 조회",
            description = """
    참가 상태를 기준으로 챌린지 참여 정보를 조회합니다. 
    status가 없으면 모든 상태를 조회합니다.
    
    참여중인 챌린지의 상세 설명 json의 type 값은 다음으로 구성됩니다.
        - h2
        - h3
        - paragraph
        - linebreak
        - blockquote
    """
    )
    BaseResponse<UserChallengeListResDto> getUserChallenges(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            ParticipationStatus status
    );
}
