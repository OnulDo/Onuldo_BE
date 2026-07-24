package com.example.onuldo.domain.party.controller.doc;

import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Party", description = "파티 생성, 조회 관련 API")
public interface PartyControllerDoc {

    @Operation(
            summary = "파티 생성",
            description = "파티 이름, 챌린지, 진행 기간, 도전금, 모집 인원을 입력받아 파티를 생성합니다. "
                    + "생성자는 자동으로 방장(HOST)이 되며, 6자리 초대코드가 발급됩니다. "
                    + "방장의 보유 포인트가 도전금보다 적으면 실패합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "name": "갓생팟",
                          "challengeId": 12,
                          "durationDays": 28,
                          "depositAmount": 30000,
                          "maxMembers": 4
                        }
                        """
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "timestamp": "2026-07-23T13:00:00",
                          "code": "SUCCESS",
                          "message": "요청에 성공하였습니다.",
                          "result": {
                            "partyId": 101,
                            "name": "갓생팟",
                            "inviteCode": "82K3H9",
                            "inviteExpiresAt": "2026-08-20T00:00:00",
                            "status": "WAITING",
                            "hostUserId": 5,
                            "maxMembers": 4,
                            "createdAt": "2026-07-23T13:00:00"
                          }
                        }
                        """
                    )
            )
    )
    BaseResponse<PartyCreateResDto> createParty(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE)
            Long userId,
            @Valid
            @RequestBody
            PartyCreateReqDto request
    );

    @Operation(
            summary = "나의 파티 목록 조회",
            description = "로그인한 사용자가 속한 파티 목록을 조회합니다. "
                    + "모집 중(WAITING)인 파티는 목록에서 제외되며, 진행 중/종료된 파티만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                        {
                          "timestamp": "2026-07-23T13:00:00",
                          "code": "SUCCESS",
                          "message": "요청에 성공하였습니다.",
                          "result": [
                            {
                              "partyId": 101,
                              "name": "30일 헬스 챌린지 파티",
                              "status": "ONGOING",
                              "dDay": 12,
                              "progressRate": 0.72,
                              "verifiedToday": 3,
                              "totalMembers": 4
                            }
                          ]
                        }
                        """
                    )
            )
    )
    BaseResponse<List<PartyListResDto>> getMyParties(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE)
            Long userId
    );
}