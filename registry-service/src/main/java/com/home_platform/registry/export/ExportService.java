package com.home_platform.registry.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.support.BusinessDate;
import org.dhatim.fastexcel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuất dữ liệu của một không gian ra file Excel, ghi thẳng ra luồng nhận được: đọc từng lô
 * {@value #BATCH} dòng, ghi xong lô nào thì bỏ lô đó, nên bộ nhớ không lớn theo dữ liệu.
 *
 * <p>File chỉ được hoàn tất khi mọi dòng đã ghi xong. Lỗi giữa chừng để lại một file dở mà Excel
 * không mở được, thay vì một file mở được nhưng thiếu dòng.
 */
@Service
public class ExportService {

	static final int BATCH = 500;

	private static final String APPLICATION = "home-platform";

	private static final String APPLICATION_VERSION = "1.0";

	private final ExportRepository repository;

	private final FieldDefService fieldDefs;

	private final ItemCategoryService categories;

	private final BusinessDate businessDate;

	ExportService(ExportRepository repository, FieldDefService fieldDefs, ItemCategoryService categories,
			BusinessDate businessDate) {
		this.repository = repository;
		this.fieldDefs = fieldDefs;
		this.categories = categories;
		this.businessDate = businessDate;
	}

	/**
	 * Tên file dự phòng gửi kèm phản hồi; giao diện tự đặt tên có tên không gian khi lưu.
	 * @param kind phần giữa của tên: {@code items}, {@code people}, {@code workspace}
	 */
	public String fileName(String kind) {
		return "home-platform-" + kind + "-" + this.businessDate.today() + ".xlsx";
	}

	/**
	 * Kiểm bộ lọc của file đồ đạc. Gọi trước {@link #writeItems}: khi đã bắt đầu ghi file thì không
	 * còn trả lỗi cho client được nữa.
	 * @throws com.home_platform.common.web.NotFoundException nếu nhóm không có
	 */
	public void requireItemFilter(long workspaceId, Long categoryId) {
		if (categoryId != null) {
			this.categories.get(workspaceId, categoryId);
		}
	}

	/**
	 * File một sheet "Đồ đạc", lọc như {@code GET /items} không phạm vi. Đọc trong một transaction để
	 * các lô nhìn cùng một trạng thái dữ liệu.
	 */
	@Transactional(readOnly = true)
	public void writeItems(long workspaceId, ItemStatus status, Long categoryId, OutputStream out) {
		Workbook workbook = new Workbook(out, APPLICATION, APPLICATION_VERSION);
		writeItemSheet(workbook, workspaceId, status, categoryId,
				PlacePaths.of(this.repository.placeNodes(workspaceId)));
		finish(workbook);
	}

	private void writeItemSheet(Workbook workbook, long workspaceId, ItemStatus status, Long categoryId,
			PlacePaths paths) {
		Map<Long, String> categoryNames = new HashMap<>();
		for (ItemCategory category : this.categories.list(workspaceId)) {
			categoryNames.put(category.id(), category.name());
		}
		ItemSheet sheet = new ItemSheet(workbook, paths, categoryNames,
				AttributeColumns.of(this.fieldDefs.listAll(workspaceId), FieldTarget.ITEM, categoryNames));
		ItemCursor after = null;
		List<ExportItemRow> batch;
		do {
			batch = this.repository.items(workspaceId, status, categoryId, after, BATCH);
			batch.forEach(sheet::write);
			after = batch.isEmpty() ? after : ItemCursor.after(batch.getLast());
		}
		while (batch.size() == BATCH);
		sheet.finish();
	}

	private static void finish(Workbook workbook) {
		try {
			workbook.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
