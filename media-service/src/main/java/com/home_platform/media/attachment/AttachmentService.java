package com.home_platform.media.attachment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.media.file.MediaFile;
import com.home_platform.media.file.MediaFileRepository;
import com.home_platform.media.file.UploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.multipart.MultipartFile;

/** Ảnh gắn vào địa điểm, người, món đồ (spec Đợt 7, M4 tới M7). */
@Service
public class AttachmentService {

	/** Số ảnh đang hiện nhiều nhất của một bản ghi (M5). */
	static final int MAX_VISIBLE = 100;

	/** Chú thích dài nhất. */
	static final int MAX_CAPTION = 255;

	private final AttachmentRepository attachments;

	private final MediaFileRepository files;

	private final UploadService uploads;

	private final TransactionOperations transactions;

	AttachmentService(AttachmentRepository attachments, MediaFileRepository files, UploadService uploads,
			TransactionOperations transactions) {
		this.attachments = attachments;
		this.files = files;
		this.uploads = uploads;
		this.transactions = transactions;
	}

	/**
	 * Lưu file (hay dùng lại file trùng) rồi gắn vào bản ghi, đứng cuối. Đếm giới hạn hai lần: trước
	 * khi lưu (khỏi lưu một file sẽ bị từ chối) và trong transaction gắn, có khoá.
	 * @throws ConflictException nếu bản ghi đã có {@value #MAX_VISIBLE} ảnh đang hiện
	 */
	public AttachmentResponse upload(WorkspaceContext ctx, OwnerType ownerType, long ownerId, MultipartFile content,
			String caption) {
		String text = caption(caption);
		requireRoom(this.attachments.countVisible(ctx.workspaceId(), ownerType, ownerId));
		MediaFile file = this.uploads.upload(ctx, content);
		Attachment attachment = this.transactions.execute((status) -> {
			List<Attachment> current = this.attachments.lockVisible(ctx.workspaceId(), ownerType, ownerId);
			requireRoom(current.size());
			int last = current.stream().mapToInt(Attachment::sortOrder).max().orElse(-1);
			return this.attachments.insert(ctx, ownerType, ownerId, file.id(), text, last + 1);
		});
		return AttachmentResponse.of(attachment, file);
	}

	/** Ảnh đang hiện của một bản ghi, theo thứ tự. */
	public List<AttachmentResponse> list(long workspaceId, OwnerType ownerType, long ownerId) {
		return withFiles(workspaceId, this.attachments.visible(workspaceId, ownerType, ownerId));
	}

	List<AttachmentResponse> withFiles(long workspaceId, List<Attachment> rows) {
		Map<Long, MediaFile> byId = this.files
			.findAll(workspaceId, rows.stream().map(Attachment::fileId).distinct().toList())
			.stream()
			.collect(Collectors.toMap(MediaFile::id, Function.identity()));
		return rows.stream().map((row) -> AttachmentResponse.of(row, byId.get(row.fileId()))).toList();
	}

	private static void requireRoom(int visible) {
		if (visible >= MAX_VISIBLE) {
			throw new ConflictException("At most " + MAX_VISIBLE + " photos per record");
		}
	}

	/** Chú thích đã bỏ khoảng trắng hai đầu; rỗng thì {@code null}. */
	static String caption(String caption) {
		String text = (caption != null) ? caption.strip() : "";
		if (text.length() > MAX_CAPTION) {
			throw new BadRequestException("caption must be at most " + MAX_CAPTION + " characters");
		}
		return text.isEmpty() ? null : text;
	}

}
