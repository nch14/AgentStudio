package com.chenhaonee.agents.app.interfaces.http.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 统一分页响应包装。
 */
public class PageResponse<T> extends Response<List<T>> {

    @Schema(description = "当前页码（从0开始）", example = "0")
    private int pageNo;

    @Schema(description = "每页条数", example = "20")
    private int pageSize;

    @Schema(description = "总记录数", example = "127")
    private long total;

    public PageResponse() {
    }

    public PageResponse(List<T> data, boolean success, int code, String errorMessage,
                        int pageNo, int pageSize, long total) {
        setData(data);
        setSuccess(success);
        setCode(code);
        setErrorMessage(errorMessage);
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
    }

    public static <T> PageResponse<T> from(Page<T> pageResult) {
        return new PageResponse<>(
                pageResult.getContent(),
                true,
                200,
                "Success",
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements());
    }

    public static <T> PageResponse<T> errorPage(Response.ErrorInfo errorInfo) {
        PageResponse<T> response = new PageResponse<>();
        response.setSuccess(false);
        response.setCode(errorInfo.code());
        response.setErrorMessage(errorInfo.message());
        response.setData(null);
        return response;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
