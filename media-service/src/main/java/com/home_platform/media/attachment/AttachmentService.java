package com.home_platform.media.attachment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.NotFoundException;
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

	/**
	 * Đổi chú thích của một ảnh đang hiện.
	 * @throws NotFoundException nếu ảnh không có, đã gỡ, hay thuộc không gian khác
	 */
	public AttachmentResponse editCaption(WorkspaceContext ctx, long id, String caption) {
		String text = caption(caption);
		Attachment edited = this.transactions.execute((status) -> {
			Attachment current = visible(ctx.workspaceId(), id);
			this.attachments.updateCaption(ctx.workspaceId(), id, text);
			return current.withCaption(text);
		});
		return withFiles(ctx.workspaceId(), List.of(edited)).get(0);
	}

	/**
	 * Sắp lại mọi ảnh đang hiện của một bản ghi theo {@code request.attachmentIds()}.
	 * @throws ConflictException nếu danh sách thiếu, thừa, trùng hay sai id: đó là danh sách cũ
	 */
	public List<AttachmentResponse> reorder(WorkspaceContext ctx, OrderRequest request) {
		List<Long> ids = request.attachmentIds();
		List<Attachment> reordered = this.transactions.execute((status) -> {
			List<Attachment> current = this.attachments.lockVisible(ctx.workspaceId(), request.ownerType(),
					request.ownerId());
			HashSet<Long> wanted = new HashSet<>(ids);
			boolean same = wanted.size() == ids.size() && ids.size() == current.size()
					&& current.stream().allMatch((row) -> wanted.contains(row.id()));
			if (!same) {
				throw new ConflictException("The photos of this record have changed; reload and try again");
			}
			Map<Long, Attachment> byId = current.stream().collect(Collectors.toMap(Attachment::id, Function.identity()));
			for (int position = 0; position < ids.size(); position++) {
				this.attachments.updateSortOrder(ctx.workspaceId(), ids.get(position), position);
			}
			return IntStream.range(0, ids.size())
				.mapToObj((position) -> byId.get(ids.get(position)).withSortOrder(position))
				.toList();
		});
		return withFiles(ctx.workspaceId(), reordered);
	}

	/**
	 * Gỡ một ảnh (M6): ghi người gỡ và lúc gỡ; dòng và file đều còn.
	 * @throws NotFoundException nếu ảnh không có, đã gỡ, hay thuộc không gian khác
	 */
	public void remove(WorkspaceContext ctx, long id) {
		this.transactions.executeWithoutResult((status) -> {
			visible(ctx.workspaceId(), id);
			this.attachments.markRemoved(ctx, id);
		});
	}

	private Attachment visible(long workspaceId, long id) {
		return this.attachments.findVisibleForUpdate(workspaceId, id)
			.orElseThrow(() -> NotFoundException.of("Attachment", id));
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
