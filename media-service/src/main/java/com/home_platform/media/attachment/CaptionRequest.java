package com.home_platform.media.attachment;

/**
 * Chú thích mới của một ảnh.
 *
 * @param caption bỏ khoảng trắng hai đầu; rỗng là bỏ chú thích; tối đa 255 ký tự
 */
public record CaptionRequest(String caption) {
}
