package com.example.onuldo.global.common.cursor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class CursorPageable {
    private CursorPageable() {

    }

    public static Pageable of(int size) {
        return PageRequest.of(0, size + 1);
    }

    public static Pageable of(int size, Sort sort) {
        return PageRequest.of(0, size + 1, sort);
    }
}
