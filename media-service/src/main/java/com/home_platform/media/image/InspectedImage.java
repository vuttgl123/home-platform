package com.home_platform.media.image;

/**
 * Một ảnh đã đọc được.
 *
 * @param contentType định dạng thật: {@code image/jpeg}, {@code image/png} hay {@code image/webp}
 * @param width chiều rộng theo chiều hiển thị (sau khi xoay theo EXIF)
 * @param height chiều cao theo chiều hiển thị
 * @param thumbnailJpeg ảnh thu nhỏ JPEG, cạnh dài tối đa {@value ImageInspector#THUMBNAIL_EDGE} px, đã xoay đúng chiều
 */
public record InspectedImage(String contentType, int width, int height, byte[] thumbnailJpeg) {
}
