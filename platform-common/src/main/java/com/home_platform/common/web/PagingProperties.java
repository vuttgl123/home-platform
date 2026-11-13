package com.home_platform.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Giới hạn cỡ trang, đặt bằng {@code platform.paging.default-limit} và
 * {@code platform.paging.max-limit}.
 *
 * @param defaultLimit cỡ trang khi client không gửi {@code limit}
 * @param maxLimit cỡ trang lớn nhất client được yêu cầu
 */
@ConfigurationProperties("platform.paging")
public record PagingProperties(@DefaultValue("20") int defaultLimit, @DefaultValue("100") int maxLimit) {
}
