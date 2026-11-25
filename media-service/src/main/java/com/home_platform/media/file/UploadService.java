package com.home_platform.media.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.media.image.ImageInspector;
import com.home_platform.media.image.InspectedImage;
import com.home_platform.media.image.UnreadableImageException;
import com.home_platform.media.storage.FileStorage;
import com.home_platform.media.storage.StorageKeys;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nhận một file ảnh (spec Đợt 7, M1 tới M4). Không có transaction bao ngoài: dòng PENDING được ghi
 * trước khi đưa file vào kho, để lần tải hỏng giữa chừng vẫn để lại dấu cho việc dọn dẹp.
 */
@Service
public class UploadService {

	/** Khoá của ảnh thu nhỏ trong kho, đứng cạnh khoá của ảnh gốc. */
	public static String thumbnailKey(String fileKey) {
		return fileKey + "/thumb";
	}

	private final MediaFileRepository files;

	private final FileStorage storage;

	private final ImageInspector inspector;

	private final IdGenerator ids;

	UploadService(MediaFileRepository files, FileStorage storage, ImageInspector inspector, IdGenerator ids) {
		this.files = files;
		this.storage = storage;
		this.inspector = inspector;
		this.ids = ids;
	}

	/**
	 * File READY cho nội dung này: file cũ cùng checksum trong không gian nếu có, không thì lưu mới.
	 * @throws BadRequestException nếu kiểu không được nhận, file rỗng, hay nội dung không phải ảnh đúng kiểu
	 * @throws org.springframework.web.server.ResponseStatusException 413 nếu file lớn hơn 10 MB
	 */
	public MediaFile upload(WorkspaceContext ctx, MultipartFile file) {
		String contentType = (file.getContentType() != null) ? file.getContentType().strip().toLowerCase(Locale.ROOT)
				: null;
		UploadPolicy.check(contentType, file.getSize());
		byte[] content = bytes(file);
		InspectedImage image;
		try {
			image = this.inspector.inspect(content, contentType);
		}
		catch (UnreadableImageException ex) {
			throw new BadRequestException("File is not a valid JPEG, PNG or WebP image");
		}
		String checksum = sha256(content);
		Optional<MediaFile> same = this.files.findReadyByChecksum(ctx.workspaceId(), checksum);
		if (same.isPresent()) {
			return same.get();
		}
		// Biết {@code id} trước khi ghi dòng, vì khoá trong kho chứa nó.
		long id = this.ids.nextId();
		String key = StorageKeys.forFile(ctx.workspaceId(), id);
		MediaFile pending = this.files.insert(ctx, new MediaFile(ctx.workspaceId(), id, key, originalName(file),
				contentType, content.length, null, null, checksum, FileStatus.PENDING, ctx.userId(), null));
		this.storage.put(key, new ByteArrayInputStream(content), content.length, contentType);
		this.storage.put(thumbnailKey(key), new ByteArrayInputStream(image.thumbnailJpeg()),
				image.thumbnailJpeg().length, "image/jpeg");
		return this.files.update(ctx,
				pending.withWidth(image.width()).withHeight(image.height()).withStatus(FileStatus.READY));
	}

	private static byte[] bytes(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not read the uploaded file", ex);
		}
	}

	/** Tên file người dùng gửi, bỏ đường dẫn, tối đa 255 ký tự; không có thì "photo". */
	private static String originalName(MultipartFile file) {
		String name = file.getOriginalFilename();
		if (name == null || name.isBlank()) {
			return "photo";
		}
		name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).strip();
		return name.isEmpty() ? "photo" : name.substring(0, Math.min(name.length(), 255));
	}

	private static String sha256(byte[] content) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
