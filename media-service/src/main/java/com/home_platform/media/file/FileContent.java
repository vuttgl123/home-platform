package com.home_platform.media.file;

import java.io.InputStream;

/**
 * Nội dung một ảnh để trả cho trình duyệt; người nhận đóng {@code stream}.
 *
 * @param sizeBytes số byte; {@code -1} khi không biết trước (ảnh thu nhỏ)
 */
public record FileContent(String contentType, long sizeBytes, InputStream stream) {
}
