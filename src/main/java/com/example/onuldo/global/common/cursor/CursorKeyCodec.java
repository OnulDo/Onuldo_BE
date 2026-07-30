package com.example.onuldo.global.common.cursor;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class CursorKeyCodec {

    private static final String DELIMITER = "-";

    private CursorKeyCodec(){

    }

    public static String encode(Object... parts) {
        String raw = Arrays.stream(parts)
                .map(String::valueOf)
                .collect(Collectors.joining(DELIMITER));

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static String[] decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            return raw.split(DELIMITER);
        } catch (IllegalArgumentException e) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }
    }
}
