package com.example.onuldo.domain.party.controller;

import com.example.onuldo.domain.party.controller.doc.PartyControllerDoc;
import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.request.PartyJoinReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyFeedResDto;
import com.example.onuldo.domain.party.dto.response.PartyLeaveResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.dto.response.PartyResultResDto;
import com.example.onuldo.domain.party.dto.response.PartyStartResDto;
import com.example.onuldo.domain.party.dto.response.PartyWaitingResDto;
import com.example.onuldo.domain.party.service.PartyFeedService;
import com.example.onuldo.domain.party.service.PartyLifecycleService;
import com.example.onuldo.domain.party.service.PartyMemberService;
import com.example.onuldo.domain.party.service.PartyService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.common.cursor.CursorConstants;
import com.example.onuldo.global.common.cursor.CursorPageResponse;
import com.example.onuldo.global.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parties")
public class PartyController implements PartyControllerDoc {

    private final PartyService partyService;
    private final PartyMemberService partyMemberService;
    private final PartyLifecycleService partyLifecycleService;
    private final PartyFeedService partyFeedService;

    @PostMapping
    public BaseResponse<PartyCreateResDto> createParty(
            @AuthUser
            Long userId,
            @Valid
            @RequestBody
            PartyCreateReqDto request
    ) {
        return BaseResponse.onSuccess(partyService.createParty(userId, request));
    }

    @GetMapping
    public CursorPageResponse<PartyListResDto> getMyParties(
            @AuthUser
            Long userId,
            @RequestParam(required = false)
            String cursor,
            @RequestParam(defaultValue = "" + CursorConstants.DEFAULT_SIZE)
            int size
    ) {
        return partyService.getMyParties(userId, cursor, size);
    }

    @GetMapping("/{partyId}/waiting-room")
    public BaseResponse<PartyWaitingResDto> getPartyWaiting(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess(partyService.getPartyWaiting(partyId, userId));
    }

    @PostMapping("/join")
    public BaseResponse<PartyWaitingResDto> joinParty(
            @AuthUser
            Long userId,
            @Valid
            @RequestBody
            PartyJoinReqDto request
    ) {
        return BaseResponse.onSuccess(partyMemberService.joinParty(userId, request));
    }

    @PostMapping("/{partyId}/leave")
    public BaseResponse<PartyLeaveResDto> leaveParty(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess("파티 이탈에 성공했습니다.", partyMemberService.leaveParty(partyId, userId));
    }

    // 준비완료 전환 API는 파티 API 목록(7개)에 명시되어 있지 않았으나 PAR-05, PAR-ERR-03 근거로 추가함 (BE 확인 필요)
    @PostMapping("/{partyId}/ready")
    public BaseResponse<PartyWaitingResDto> togglePartyMemberReady(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess(partyMemberService.togglePartyMemberReady(partyId, userId));
    }

    @PostMapping("/{partyId}/start")
    public BaseResponse<PartyStartResDto> startParty(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess(partyLifecycleService.startParty(partyId, userId));
    }

    @GetMapping("/{partyId}/feed")
    public BaseResponse<PartyFeedResDto> getPartyFeed(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess(partyFeedService.getPartyFeed(partyId, userId));
    }

    @GetMapping("/{partyId}/results")
    public BaseResponse<PartyResultResDto> getPartyResult(
            @AuthUser
            Long userId,
            @PathVariable
            Long partyId
    ) {
        return BaseResponse.onSuccess(partyService.getPartyResult(partyId, userId));
    }
}
