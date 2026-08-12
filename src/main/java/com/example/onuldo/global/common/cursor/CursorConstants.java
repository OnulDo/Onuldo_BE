package com.example.onuldo.global.common.cursor;

import com.example.onuldo.global.common.exception.InvalidRequestException;
import com.example.onuldo.global.common.exception.code.status.ErrorStatus;

public class CursorConstants {
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    private CursorConstants() {

    }

    public static int resolveSize(int size) {
        if (size <= 0) {
            throw new InvalidRequestException(ErrorStatus._CURSOR_SIZE_INVALID);
        }
        return Math.min(size, MAX_SIZE);
    }
}
