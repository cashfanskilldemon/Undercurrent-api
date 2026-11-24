package com.undercurrent.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果。前端列表页统一用这个结构。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;
    private boolean hasMore;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.list = list;
        r.total = total;
        r.page = page;
        r.size = size;
        r.hasMore = (long) page * size < total;
        return r;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return of(List.of(), 0, page, size);
    }
}
