package com.example.onuldo.domain.auth.support;

import java.util.List;

/** 사용자의 닉네임 사용 불가 키워드 정의 Class
* */
public final class NicknameBannedWords {

    public static final List<String> VALUES = List.of("씨발", "시발", "병신", "새끼");

    private NicknameBannedWords() {
    }
}
