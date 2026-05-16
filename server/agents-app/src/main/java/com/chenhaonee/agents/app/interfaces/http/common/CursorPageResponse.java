package com.chenhaonee.agents.app.interfaces.http.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 游标分页响应包装。
 */
public class CursorPageResponse<T> extends Response<List<T>> {

    @Schema(description = "下一页游标值，null 表示没有更多数据")
    private String nextCursor;

    @Schema(description = "是否有更多数据")
    private boolean hasMore;

    public CursorPageResponse() {
    }

    public CursorPageResponse(List<T> data, String nextCursor, boolean hasMore) {
        setData(data);
        setSuccess(true);
        setCode(200);
        setErrorMessage("Success");
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public static <T> CursorPageResponse<T> of(List<T> data, String nextCursor, boolean hasMore) {
        return new CursorPageResponse<>(data, nextCursor, hasMore);
    }

    public static <T> CursorPageResponse<T> errorCursor(Response.ErrorInfo errorInfo) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setSuccess(false);
        response.setCode(errorInfo.code());
        response.setErrorMessage(errorInfo.message());
        response.setData(null);
        return response;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
