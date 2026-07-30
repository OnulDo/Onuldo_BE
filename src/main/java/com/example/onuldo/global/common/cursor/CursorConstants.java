package com.example.onuldo.global.common.cursor;

import com.example.onuldo.global.common.exception.RestApiException;
import com.example.onuldo.global.common.exception.code.status.GlobalErrorStatus;

import java.time.ZoneId;

public class CursorConstants {
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private CursorConstants() {

    }

    public static int resolveSize(int size) {
        if (size <= 0) {
            throw new RestApiException(GlobalErrorStatus._BAD_REQUEST, "size는 1 이상이어야 합니다.");
        }
        return Math.min(size, MAX_SIZE);
    }
}
