package com.example.onuldo.domain.auth.support;

import com.example.onuldo.global.common.exception.InvalidRequestException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class NicknameValidator {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[ㄱ-ㅎ가-힣a-zA-Z0-9]+$");
    private static final int MIN_NICKNAME_LENGTH = 2;
    private static final int MAX_NICKNAME_LENGTH = 8;

    public void validate(String nickname) {
        if (nickname.length() < MIN_NICKNAME_LENGTH) {
            throw new InvalidRequestException(ErrorStatus._NICKNAME_TOO_SHORT);
        }

        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new InvalidRequestException(ErrorStatus._NICKNAME_TOO_LONG);
        }

        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new InvalidRequestException(ErrorStatus._INVALID_NICKNAME);
        }

        for (String bannedWord : NicknameBannedWords.VALUES) {
            if (nickname.contains(bannedWord.toLowerCase(Locale.ROOT))) {
                throw new InvalidRequestException(ErrorStatus._INVALID_NICKNAME);
            }
        }
    }
}
