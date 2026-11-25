package com.home_platform.media.storage;

import java.io.InputStream;

/**
 * Kho lưu nội dung file. Database chỉ lưu thông tin về file; nội dung nằm ở đây. Code nghiệp vụ
 * chỉ biết interface này nên đổi kho (thư mục trên đĩa, MinIO, S3) không phải sửa nghiệp vụ.
 *
 * <p>Bản cài đặt hôm nay là {@link LocalFileStorage} (thư mục {@code media.storage.root}). Mọi bản
 * cài đặt phải qua test hợp đồng {@code FileStorageContract}.
 */
public interface FileStorage {

	/**
	 * Ghi nội dung vào {@code key}; ghi đè nếu đã có.
	 * @param sizeBytes số byte của {@code content}, đã kiểm tra bằng {@code UploadPolicy}
	 */
	StoredObject put(String key, InputStream content, long sizeBytes, String contentType);

	/**
	 * Mở nội dung đã lưu; người gọi đóng luồng.
	 * @throws StoredObjectNotFoundException nếu khoá không có nội dung
	 */
	InputStream open(String key);

	/** Xoá nội dung; không có thì không làm gì. */
	void delete(String key);

}
