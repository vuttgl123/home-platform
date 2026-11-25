package com.home_platform.media.file;

import java.util.Locale;
import java.util.Set;

import com.home_platform.common.web.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Luật nhận file khi tải lên (spec nghiệp vụ mục 6): chỉ ảnh JPEG, PNG, WebP; tối đa 10 MB.
 * Đây chỉ là bước lọc nhanh theo kiểu khai báo; nội dung thật được giải mã ở
 * {@link com.home_platform.media.image.ImageInspector}.
 */
public final class UploadPolicy {

	public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

	public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private UploadPolicy() {
	}

	/**
	 * @throws BadRequestException nếu loại file không được nhận hay file rỗng
	 * @throws ResponseStatusException 413 nếu file lớn hơn 10 MB, như khi server tự chặn một
	 *   request multipart quá lớn
	 */
	public static void check(String contentType, long sizeBytes) {
		String type = (contentType != null) ? contentType.strip().toLowerCase(Locale.ROOT) : "";
		if (!ALLOWED_CONTENT_TYPES.contains(type)) {
			throw new BadRequestException("Only JPEG, PNG and WebP images are accepted");
		}
		if (sizeBytes <= 0) {
			throw new BadRequestException("File is empty");
		}
		if (sizeBytes > MAX_SIZE_BYTES) {
			throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File size must be at most 10 MB");
		}
	}

}
