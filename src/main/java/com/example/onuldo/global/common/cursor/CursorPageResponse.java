package com.example.onuldo.global.common.cursor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.function.Function;

@Builder
public record CursorPageResponse<T> (
  @Schema(description = "조회된 목록")
  List<T> content,
  @Schema(description = "다음 페이지 조회용 커서 (없으면 마지막 페이지)", nullable = true)
  String nextCursor,
  @Schema(description = "다음 페이지 존재 여부")
  boolean hasNext
) {
    public static <E, T> CursorPageResponse<T> of(
            List<E> fetched,
            int size,
            Function<E, T> mapper,
            Function<E, String> cursorExtractor
    ) {
        boolean hasNext = fetched.size() > size;
        List<E> sliced = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext ? cursorExtractor.apply(sliced.get(sliced.size() - 1)) : null;

        return CursorPageResponse.<T>builder()
                .content(sliced.stream().map(mapper).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}