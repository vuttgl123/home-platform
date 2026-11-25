package com.home_platform.media.attachment;

/**
 * Thông tin của file đứng sau một ảnh đã gắn.
 *
 * @param width chiều rộng theo chiều hiển thị
 * @param height chiều cao theo chiều hiển thị
 */
public record FileInfo(String originalName, String contentType, long sizeBytes, int width, int height) {
}
