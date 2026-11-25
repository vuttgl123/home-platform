package com.home_platform.media.file;

import java.io.InputStream;

import com.home_platform.common.web.NotFoundException;
import com.home_platform.media.storage.FileStorage;
import com.home_platform.media.storage.StoredObjectNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Đọc nội dung ảnh gốc và ảnh thu nhỏ. Chỉ file READY của đúng không gian; ảnh đã gỡ vẫn đọc được,
 * vì ảnh bìa hay một version cũ có thể còn trỏ tới file đó.
 */
@Service
public class FileContentService {

	private final MediaFileRepository files;

	private final FileStorage storage;

	FileContentService(MediaFileRepository files, FileStorage storage) {
		this.files = files;
		this.storage = storage;
	}

	/** @throws NotFoundException nếu file không có, thuộc không gian khác, hay chưa READY */
	public FileContent original(long workspaceId, long fileId) {
		MediaFile file = ready(workspaceId, fileId);
		return new FileContent(file.contentType(), file.sizeBytes(), open(file.storageKey(), fileId));
	}

	/** @throws NotFoundException nếu file không có, thuộc không gian khác, hay chưa READY */
	public FileContent thumbnail(long workspaceId, long fileId) {
		MediaFile file = ready(workspaceId, fileId);
		return new FileContent("image/jpeg", -1, open(UploadService.thumbnailKey(file.storageKey()), fileId));
	}

	private MediaFile ready(long workspaceId, long fileId) {
		return this.files.findById(workspaceId, fileId)
			.filter((file) -> file.status() == FileStatus.READY)
			.orElseThrow(() -> NotFoundException.of("File", fileId));
	}

	private InputStream open(String key, long fileId) {
		try {
			return this.storage.open(key);
		}
		catch (StoredObjectNotFoundException ex) {
			throw NotFoundException.of("File", fileId);
		}
	}

}
