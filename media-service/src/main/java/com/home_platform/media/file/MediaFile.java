package com.home_platform.media.file;

import java.time.Instant;

import lombok.With;

/**
 * Một dòng của {@code med_file}: thông tin về một file đã tải lên. Bản thân file nằm trong kho
 * ({@link com.home_platform.media.storage.FileStorage}) tại {@code storageKey}.
 *
 * <p>TODO(media): repository kế thừa {@code WorkspaceRepository<MediaFile>} (khoá
 * {@code (workspace_id, id)}); tìm file trùng theo {@code checksumSha256} bằng index
 * (workspace_id, checksum_sha256). Xem {@code docs/superpowers/todo/media.md}.
 *
 * @param width chiều rộng ảnh, đọc sau khi tải lên; {@code null} khi chưa đọc
 * @param checksumSha256 64 ký tự hex, để nhận ra file trùng trong cùng không gian
 */
@With
public record MediaFile(long workspaceId, long id, String storageKey, String originalName, String contentType,
		long sizeBytes, Integer width, Integer height, String checksumSha256, FileStatus status, long uploadedBy,
		Instant createdAt) {
}
