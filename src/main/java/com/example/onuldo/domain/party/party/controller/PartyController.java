package com.example.onuldo.domain.party.controller;

import com.example.onuldo.domain.party.controller.doc.PartyControllerDoc;
import com.example.onuldo.domain.party.dto.request.PartyCreateReqDto;
import com.example.onuldo.domain.party.dto.response.PartyCreateResDto;
import com.example.onuldo.domain.party.dto.response.PartyListResDto;
import com.example.onuldo.domain.party.service.PartyService;
import com.example.onuldo.global.common.base.BaseResponse;
import com.example.onuldo.global.security.JwtAuthenticationInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parties")
public class PartyController implements PartyControllerDoc {

    private final PartyService partyService;

    @PostMapping
    public BaseResponse<PartyCreateResDto> createParty(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE)
            Long userId,
            @Valid
            @RequestBody
            PartyCreateReqDto request
    ) {
        return BaseResponse.onSuccess(partyService.createParty(userId, request));
    }

    @GetMapping
    public BaseResponse<List<PartyListResDto>> getMyParties(
            @RequestAttribute(JwtAuthenticationInterceptor.AUTHENTICATED_USER_ID_ATTRIBUTE)
            Long userId
    ) {
        return BaseResponse.onSuccess(partyService.getMyParties(userId));
    }
}
