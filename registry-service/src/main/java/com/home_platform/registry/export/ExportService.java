package com.home_platform.registry.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceType;
import com.home_platform.registry.placetype.PlaceTypeService;
import com.home_platform.registry.support.BusinessDate;
import org.dhatim.fastexcel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuất dữ liệu của một không gian ra file Excel, ghi thẳng ra luồng nhận được: đọc từng lô
 * {@value #BATCH} dòng, ghi xong lô nào thì bỏ lô đó, nên bộ nhớ không lớn theo dữ liệu.
 *
 * <p>Mỗi file đọc trong một transaction để mọi lô và mọi sheet nhìn cùng một trạng thái dữ liệu. File
 * chỉ được hoàn tất khi mọi dòng đã ghi xong. Lỗi giữa chừng để lại một file dở mà Excel không mở
 * được, thay vì một file mở được nhưng thiếu dòng.
 */
@Service
public class ExportService {

	static final int BATCH = 500;

	private static final String APPLICATION = "home-platform";

	private static final String APPLICATION_VERSION = "1.0";

	private final ExportRepository repository;

	private final FieldDefService fieldDefs;

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService categories;

	private final BusinessDate businessDate;

	ExportService(ExportRepository repository, FieldDefService fieldDefs, PlaceTypeService placeTypes,
			ItemCategoryService categories, BusinessDate businessDate) {
		this.repository = repository;
		this.fieldDefs = fieldDefs;
		this.placeTypes = placeTypes;
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

	/** File một sheet "Đồ đạc", lọc như {@code GET /items} không phạm vi. */
	@Transactional(readOnly = true)
	public void writeItems(long workspaceId, ItemStatus status, Long categoryId, OutputStream out) {
		Workbook workbook = workbook(out);
		writeItemSheet(workbook, workspaceId, ItemFilter.likeTheList(status, categoryId), paths(workspaceId));
		finish(workbook);
	}

	/** File một sheet "Người". */
	@Transactional(readOnly = true)
	public void writePeople(long workspaceId, OutputStream out) {
		Workbook workbook = workbook(out);
		writePersonSheet(workbook, workspaceId, paths(workspaceId));
		finish(workbook);
	}

	/**
	 * File cả không gian, năm sheet: Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc. Chỉ bản ghi chưa xoá;
	 * dòng cư trú, sở hữu đã kết thúc và món đã gỡ vẫn có.
	 */
	@Transactional(readOnly = true)
	public void writeWorkspace(long workspaceId, OutputStream out) {
		Workbook workbook = workbook(out);
		PlacePaths paths = paths(workspaceId);
		writePlaceSheet(workbook, workspaceId, paths);
		writePersonSheet(workbook, workspaceId, paths);
		writeOccupancySheet(workbook, workspaceId, paths);
		writeOwnershipSheet(workbook, workspaceId, paths);
		writeItemSheet(workbook, workspaceId, ItemFilter.everything(), paths);
		finish(workbook);
	}

	private void writePlaceSheet(Workbook workbook, long workspaceId, PlacePaths paths) {
		Map<Long, String> typeNames = new HashMap<>();
		for (PlaceType type : this.placeTypes.list(workspaceId)) {
			typeNames.put(type.id(), type.name());
		}
		PlaceSheet sheet = new PlaceSheet(workbook, paths, typeNames,
				AttributeColumns.of(fieldDefs(workspaceId), FieldTarget.PLACE, typeNames));
		forEachBatch(after -> this.repository.places(workspaceId, after, BATCH), ExportPlaceRow::path,
				batch -> batch.forEach(sheet::write));
		sheet.finish();
	}

	private void writePersonSheet(Workbook workbook, long workspaceId, PlacePaths paths) {
		LocalDate today = this.businessDate.today();
		PersonSheet sheet = new PersonSheet(workbook, paths,
				AttributeColumns.of(fieldDefs(workspaceId), FieldTarget.PERSON, Map.of()));
		forEachBatch(after -> this.repository.people(workspaceId, after, BATCH), ExportPersonRow::cursor, batch -> {
			Map<Long, List<Long>> livingAt = this.repository.currentPlacesOf(workspaceId,
					batch.stream().map(ExportPersonRow::id).toList(), today);
			batch.forEach(person -> sheet.write(person, livingAt.getOrDefault(person.id(), List.of())));
		});
		sheet.finish();
	}

	private void writeOccupancySheet(Workbook workbook, long workspaceId, PlacePaths paths) {
		OccupancySheet sheet = new OccupancySheet(workbook, paths);
		forEachBatch(after -> this.repository.occupancies(workspaceId, after, BATCH), ExportOccupancyRow::cursor,
				batch -> batch.forEach(sheet::write));
		sheet.finish();
	}

	private void writeOwnershipSheet(Workbook workbook, long workspaceId, PlacePaths paths) {
		OwnershipSheet sheet = new OwnershipSheet(workbook, paths);
		forEachBatch(after -> this.repository.ownerships(workspaceId, after, BATCH), ExportOwnershipRow::cursor,
				batch -> batch.forEach(sheet::write));
		sheet.finish();
	}

	private void writeItemSheet(Workbook workbook, long workspaceId, ItemFilter filter, PlacePaths paths) {
		Map<Long, String> categoryNames = new HashMap<>();
		for (ItemCategory category : this.categories.list(workspaceId)) {
			categoryNames.put(category.id(), category.name());
		}
		ItemSheet sheet = new ItemSheet(workbook, paths, categoryNames,
				AttributeColumns.of(fieldDefs(workspaceId), FieldTarget.ITEM, categoryNames));
		forEachBatch(after -> this.repository.items(workspaceId, filter, after, BATCH), ExportItemRow::cursor,
				batch -> batch.forEach(sheet::write));
		sheet.finish();
	}

	/**
	 * Đọc hết một danh sách theo lô: lô sau bắt đầu sau dòng cuối của lô trước; lô ngắn hơn
	 * {@link #BATCH} là lô cuối.
	 * @param read đọc một lô sau mốc đã cho; mốc {@code null} là lô đầu
	 * @param cursorOf mốc đọc tiếp ứng với một dòng
	 */
	private static <R, C> void forEachBatch(Function<C, List<R>> read, Function<R, C> cursorOf,
			Consumer<List<R>> write) {
		C after = null;
		List<R> batch;
		do {
			batch = read.apply(after);
			write.accept(batch);
			if (!batch.isEmpty()) {
				after = cursorOf.apply(batch.getLast());
			}
		}
		while (batch.size() == BATCH);
	}

	private PlacePaths paths(long workspaceId) {
		return PlacePaths.of(this.repository.placeNodes(workspaceId));
	}

	private List<FieldDef> fieldDefs(long workspaceId) {
		return this.fieldDefs.listAll(workspaceId);
	}

	static Workbook workbook(OutputStream out) {
		return new Workbook(out, APPLICATION, APPLICATION_VERSION);
	}

	static void finish(Workbook workbook) {
		try {
			workbook.finish();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
