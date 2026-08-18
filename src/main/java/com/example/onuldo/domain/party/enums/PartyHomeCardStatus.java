package com.example.onuldo.domain.party.enums;

// HOME-03: 오늘 상태 4종 표기 (미인증=[인증하기] / 검토대기 / 실패 / 성공)
public enum PartyHomeCardStatus {
    NOT_VERIFIED,
    PENDING,
    SUCCESS,
    FAIL
}
