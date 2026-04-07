package com.vbforge.educationapi.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {

    private List<T> content;       // the items on this page
    private int page;              // current page number (0-based)
    private int size;              // items per page
    private long totalElements;   // total items across all pages
    private int totalPages;       // total number of pages
    private boolean last;         // is this the last page?


}
