package com.home_platform.media.storage;

import java.io.InputStream;

/**
 * Kho lưu nội dung file. Database chỉ lưu thông tin về file; nội dung nằm ở đây. Code nghiệp vụ
 * chỉ biết interface này nên đổi kho (thư mục trên đĩa, MinIO, S3) không phải sửa nghiệp vụ.
 *
 * <p>TODO(media): chưa có bản cài đặt nào và chưa có bean nào được đăng ký. Chờ chọn kho
 * (xem {@code docs/superpowers/todo/media.md}). Khi có, thêm một lớp cài đặt cùng thuộc tính
 * cấu hình (ví dụ {@code media.storage.type}) và một test hợp đồng chạy chung cho mọi bản cài đặt.
 */
public interface FileStorage {

	/**
	 * Ghi nội dung vào {@code key}; ghi đè nếu đã có.
	 * @param sizeBytes số byte của {@code content}, đã kiểm tra bằng {@code UploadPolicy}
	 */
	StoredObject put(String key, InputStream content, long sizeBytes, String contentType);

	/** Mở nội dung đã lưu; người gọi đóng luồng. */
	InputStream open(String key);

	/** Xoá nội dung; không có thì không làm gì. */
	void delete(String key);

}
