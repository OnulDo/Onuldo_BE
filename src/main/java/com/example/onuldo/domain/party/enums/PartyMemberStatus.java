package com.example.onuldo.domain.party.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartyMemberStatus {

    WAITING("대기"),
    READY("준비완료");

    private final String description;
}
