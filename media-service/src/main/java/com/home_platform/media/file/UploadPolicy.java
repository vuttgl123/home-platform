package com.home_platform.media.file;

import java.util.Locale;
import java.util.Set;

import com.home_platform.common.web.BadRequestException;

/**
 * Luật nhận file khi tải lên (spec nghiệp vụ mục 6): chỉ ảnh JPEG, PNG, WebP; tối đa 10 MB.
 *
 * <p>TODO(media): kiểm tra thêm nội dung thật của file (magic bytes) thay vì chỉ tin
 * {@code Content-Type} client gửi; đọc kích thước ảnh sau khi lưu.
 */
public final class UploadPolicy {

	public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

	public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private UploadPolicy() {
	}

	/**
	 * @throws BadRequestException nếu loại file không được nhận, file rỗng hoặc lớn hơn 10 MB
	 */
	public static void check(String contentType, long sizeBytes) {
		String type = (contentType != null) ? contentType.strip().toLowerCase(Locale.ROOT) : "";
		if (!ALLOWED_CONTENT_TYPES.contains(type)) {
			throw new BadRequestException("Only JPEG, PNG and WebP images are accepted");
		}
		if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
			throw new BadRequestException("File size must be between 1 byte and at most 10 MB");
		}
	}

}
