package com.kk.klist.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private final List<T> content;
    private final Long totalElements;
    private final Integer totalPages;
    private final int currentPage;
    private final int pageSize;
    private final boolean hasNext;

    /**
     * count 쿼리를 포함하는 {@link Page} 기반 페이징 응답을 생성한다.
     * totalElements/totalPages를 포함한다.
     */
    private PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.currentPage = page.getNumber();
        this.pageSize = page.getSize();
        this.hasNext = page.hasNext();
    }

    /**
     * count 쿼리 없는 {@link Slice} 기반 커서 응답을 생성한다.
     * totalElements/totalPages는 포함하지 않는다.
     */
    private PageResponse(Slice<T> slice) {
        this.content = slice.getContent();
        this.totalElements = null;
        this.totalPages = null;
        this.currentPage = slice.getNumber();
        this.pageSize = slice.getSize();
        this.hasNext = slice.hasNext();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }

    public static <T> PageResponse<T> of(Slice<T> slice) {
        return new PageResponse<>(slice);
    }
}
