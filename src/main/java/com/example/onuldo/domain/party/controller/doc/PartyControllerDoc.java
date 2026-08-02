package com.example.onuldo.domain.party.controller.doc;

import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.request.PartyJoinReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyFeedResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.dto.response.PartyStartResDto;
import com.example.onuldo.domain.party.dto.response.PartyWaitingResDto;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Party", description = "파티 생성, 조회, 참여, 시작, 진행 피드 관련 API")
public interface PartyControllerDoc {

    @Operation(
            summary = "파티 생성",
            description = "파티 이름, 챌린지, 진행 기간, 도전금, 모집 인원을 입력 받아 파티를 생성합니다. "
                    + "생성자는 자동으로 방장(HOST)이 되며, 6자리 초대코드가 발급됩니다. "
                    + "방장의 보유 포인트가 도전금보다 적으면 실패합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyCreateResDto> createParty(
            @AuthUser Long userId,
            @Valid @RequestBody PartyCreateReqDto request
    );

    @Operation(
            summary = "나의 파티 목록 조회",
            description = "커서 기반 페이지네이션으로 로그인한 사용자가 속한 파티 목록을 조회합니다. "
                    + "생성일 최신순으로 정렬되며, cursor를 넘기지 않으면 첫 페이지를 반환합니다. "
                    + "모집 중(WAITING)인 파티는 목록에서 제외되며, 진행 중/종료된 파티만 반환합니다."
    )
    @ApiResponse(responseCode = "200")
    CursorPageResponse<PartyListResDto> getMyParties(
            @AuthUser Long userId,
            @Parameter(description = "이전 응답의 nextCursor 값. 첫 페이지는 비워둠")
            @RequestParam(required = false)
            String cursor,
            @Parameter(description = "조회할 개수. 기본값 10", example = "10")
            @RequestParam(defaultValue = "10")
            int size
    );

    @Operation(
            summary = "파티 대기방 조회",
            description = "파티 정보(모집 상태, 진행 기간, 1인 도전금)와 파티원 목록(닉네임, 방장 여부, 준비 상태)을 조회합니다. "
                    + "요청자가 해당 파티의 파티원이 아니면 조회할 수 없습니다. "
                    + "isHost, canStart 필드로 방장 여부와 [시작하기] 버튼 활성화 여부를 판단할 수 있습니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyWaitingResDto> getPartyWaiting(
            @AuthUser Long userId,
            @PathVariable Long partyId
    );

    @Operation(
            summary = "초대코드로 파티 참여",
            description = "6자리 초대코드를 입력받아 유효성을 검증하고 파티에 참여합니다. "
                    + "무효한 코드, 이미 시작된 파티, 인원 초과, 만료된 코드인 경우 각각 다른 오류 메시지를 반환합니다. "
                    + "참여 성공 시 파티 대기방 정보를 함께 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyWaitingResDto> joinParty(
            @AuthUser Long userId,
            @Valid @RequestBody PartyJoinReqDto request
    );

    @Operation(
            summary = "파티원 준비완료 상태 토글",
            description = "파티원이 대기방에서 [준비완료]/[대기] 상태를 전환합니다. "
                    + "방장은 준비완료 대상이 아닙니다. "
                    + "준비완료로 전환 시 보유 포인트가 도전금보다 부족하면 전환에 실패합니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyWaitingResDto> togglePartyMemberReady(
            @AuthUser Long userId,
            @PathVariable Long partyId
    );

    @Operation(
            summary = "파티 시작",
            description = "방장이 파티를 시작합니다. 파티원이 2인 이상 모이고 전원 준비완료 상태여야 시작할 수 있습니다. "
                    + "시작 시 파티원 전원의 도전금이 일괄 예치(포인트 차감)되며, 파티 상태가 진행 중(ONGOING)으로 전환되고 초대코드가 만료됩니다. "
                    + "파티원별 챌린지 참여 기록도 이 시점에 생성됩니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyStartResDto> startParty(
            @AuthUser Long userId,
            @PathVariable Long partyId
    );

    @Operation(
            summary = "파티 진행 피드 조회",
            description = "파티의 오늘 팀 진행률(오늘 인증 완료 파티원 비율)과 파티원별 오늘 인증 현황을 조회합니다. "
                    + "인증이 AI 자동 심사에서 통과(PASS)한 경우에만 인증 완료로 집계합니다. "
                    + "요청자가 해당 파티의 파티원이 아니면 조회할 수 없습니다."
    )
    @ApiResponse(responseCode = "200")
    BaseResponse<PartyFeedResDto> getPartyFeed(
            @AuthUser Long userId,
            @PathVariable Long partyId
    );
}
