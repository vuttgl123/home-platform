package com.home_platform.media.storage;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình kho file.
 *
 * @param root thư mục gốc của kho; đường dẫn tương đối tính từ thư mục chạy service
 */
@ConfigurationProperties("media.storage")
public record StorageProperties(@DefaultValue("data/media") Path root) {
}
