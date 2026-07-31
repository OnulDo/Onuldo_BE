package com.example.onuldo.global.common.cursor;

import com.example.onuldo.global.common.base.BaseResponse;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
@JsonPropertyOrder({"timestamp", "code", "message", "content", "nextCursor", "hasNext"})
public class CursorPageResponse<T> extends BaseResponse<Void> {

    @Schema(description = "조회된 목록")
    private final List<T> content;

    @Schema(description = "다음 페이지 조회용 커서 (없으면 마지막 페이지)", nullable = true)
    private final String nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private final boolean hasNext;

    private CursorPageResponse(List<T> content, String nextCursor, boolean hasNext) {
        super("SUCCESS", "요청에 성공하였습니다.", null);
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <E, T> CursorPageResponse<T> of(
            List<E> fetched,
            int size,
            Function<E, T> mapper,
            Function<E, String> cursorExtractor
    ) {
        boolean hasNext = fetched.size() > size;
        List<E> sliced = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext ? cursorExtractor.apply(sliced.get(sliced.size() - 1)) : null;

        return new CursorPageResponse<>(
                sliced.stream().map(mapper).toList(),
                nextCursor,
                hasNext
        );
    }
}
