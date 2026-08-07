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

    public static boolean isBlank(String cursor) {
        return cursor == null || cursor.isBlank();
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

    public static String[] decodeParts(String cursor, int expectedPartCount) {
        String[] parts = decode(cursor);
        if (parts.length != expectedPartCount) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }
        return parts;
    }

    public static long[] decodeAsLongs(String cursor, int expectedPartCount) {
        String[] parts = decodeParts(cursor, expectedPartCount);

        try {
            long[] values = new long[expectedPartCount];
            for (int i = 0; i < expectedPartCount; i++) {
                values[i] = Long.parseLong(parts[i]);
            }
            return values;
        } catch (NumberFormatException e) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }
    }

    public static int toIntCursorValue(long value) {
        if (value < 0) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }

        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException e) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");
        }
    }
}
