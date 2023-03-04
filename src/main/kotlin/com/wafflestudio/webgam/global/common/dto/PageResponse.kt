package com.wafflestudio.webgam.global.common.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PageResponse<T> (
        val content: List<T>,
        val page: Int,
        val size: Int,
        @JsonProperty("number_of_elements")
        val numberOfElements: Int,
) {

    /*constructor(page: Page<T>, pageNum: Int, pageSize: Int) : this(
            page.content,
            pageSize,
            pageNum,
            page.numberOfElements,
    )*/
}