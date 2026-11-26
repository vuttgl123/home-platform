package com.home_platform.registry.imports;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.export.ExportLabels;
import com.home_platform.registry.imports.ImportLookupRepository.KnownItem;
import com.home_platform.registry.imports.PersonIndex.PersonRef;
import com.home_platform.registry.imports.PlaceIndex.PlaceRef;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemRemoveRequest;
import com.home_platform.registry.item.ItemRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.item.ItemStatus;
import com.home_platform.registry.itemcategory.ItemCategory;
import org.springframework.stereotype.Component;

/**
 * Sheet Đồ đạc (spec N2, N3, N8). Món "Đã gỡ" được tạo như món Đang dùng rồi gỡ với ngày gỡ trong
 * file, trong cùng savepoint của dòng: hai version, như khi làm bằng tay.
 */
@Component
class ItemRows implements SheetRows {

	static final int MAX_QUANTITY = 1_000_000;

	private static final Map<String, String> COLUMNS = Map.ofEntries(Map.entry("name", "Tên"),
			Map.entry("categoryId", "Nhóm"), Map.entry("placeId", "Ở đâu"), Map.entry("quantity", "Số lượng"),
			Map.entry("unit", "Đơn vị"), Map.entry("status", "Trạng thái"), Map.entry("placedAt", "Ngày đặt"),
			Map.entry("warrantyUntil", "Hết bảo hành"), Map.entry("serviceEveryMonths", "Bảo trì (tháng)"),
			Map.entry("ownerPersonId", "Chủ"), Map.entry("description", "Mô tả"), Map.entry("removedAt", "Ngày gỡ"));

	private static final Map<String, ItemStatus> STATUSES = new LinkedHashMap<>();

	private static final List<String> STATUS_LABELS = new ArrayList<>();

	static {
		for (ItemStatus status : ItemStatus.values()) {
			STATUSES.put(ImportText.key(ExportLabels.itemStatus(status)), status);
			STATUSES.put(ImportText.key(status.name()), status);
			STATUS_LABELS.add(ExportLabels.itemStatus(status));
		}
	}

	private final ItemService items;

	private final RowRunner runner;

	ItemRows(ItemService items, RowRunner runner) {
		this.items = items;
		this.runner = runner;
	}

	@Override
	public SheetKind kind() {
		return SheetKind.ITEM;
	}

	@Override
	public List<RowReport> importAll(ImportSheet sheet, ImportRun run) {
		PlaceIndex places = PlaceIndex.of(run);
		PersonIndex people = PersonIndex.of(run);
		TypeLookup types = TypeLookup.of(run);
		AttributeColumns attributes = run.attributes(SheetKind.ITEM);
		Map<String, String> columns = Columns.withAttributes(COLUMNS, attributes);
		Set<String> knownItems = new HashSet<>();
		for (KnownItem item : run.knownItems()) {
			knownItems.add(itemKey(item.placeId(), item.name()));
		}
		Map<String, Integer> itemRows = new HashMap<>();
		Map<String, Integer> codeRows = new HashMap<>();
		List<RowReport> reports = new ArrayList<>();
		for (ImportRow row : sheet.rows()) {
			RowReader reader = new RowReader(row);
			String code = code(reader, codeRows);
			String name = reader.requiredText("Tên");
			ItemCategory category = types.category(reader, "Nhóm");
			Long placeId = null;
			String placePath = null;
			String place = reader.requiredText("Ở đâu");
			if (place != null) {
				PlaceRef ref = places.resolve(place);
				String problem = PlaceIndex.problem(ref);
				if (problem != null) {
					reader.error("Ở đâu", problem);
				}
				else {
					placeId = ((PlaceIndex.Found) ref).id();
					placePath = ((PlaceIndex.Found) ref).path();
				}
			}
			Long ownerId = owner(reader, people);
			ItemStatus status = reader.choice("Trạng thái", STATUSES, STATUS_LABELS);
			boolean removed = status == ItemStatus.REMOVED;
			LocalDate removedAt = reader.date("Ngày gỡ");
			if (removed && removedAt == null && !reader.hasErrorIn("Ngày gỡ")) {
				reader.error("Ngày gỡ", "Bắt buộc khi trạng thái là Đã gỡ");
			}
			else if (!removed && removedAt != null) {
				reader.error("Ngày gỡ", "Chỉ ghi khi trạng thái là Đã gỡ");
			}
			Long categoryId = (category != null) ? category.id() : null;
			String categoryName = (category != null) ? category.name() : "(không nhóm)";
			ItemRequest request = new ItemRequest(placeId, ownerId, categoryId, name,
					reader.integer("Số lượng", 1, MAX_QUANTITY), reader.text("Đơn vị"),
					removed ? ItemStatus.IN_USE : status, reader.date("Ngày đặt"), reader.date("Hết bảo hành"),
					reader.integer("Bảo trì (tháng)", 1, 120), reader.text("Mô tả"),
					AttributeCells.read(reader, attributes, categoryId, categoryName), null, run.versionNote());
			if (placeId != null) {
				this.runner.validate(request, reader, columns);
			}
			Item created = this.runner.run(reader, columns, () -> {
				Item item = this.items.create(run.ctx(), request, code);
				return removed ? this.items.remove(run.ctx(), item.id(),
						new ItemRemoveRequest(item.version(), removedAt, run.versionNote())) : item;
			});
			String finalCode = (created != null) ? created.code() : code;
			if (created != null && !removed) {
				warnDuplicate(reader, created, placePath, knownItems, itemRows);
			}
			reports.add(reader.report(label(finalCode, name), finalCode));
		}
		return reports;
	}

	private static String code(RowReader reader, Map<String, Integer> codeRows) {
		String code = reader.text("Mã");
		if (code == null) {
			return null;
		}
		String codeKey = ImportText.codeKey(code);
		if (code.length() > PlaceRows.MAX_CODE) {
			reader.error("Mã", "Dài quá " + PlaceRows.MAX_CODE + " ký tự");
		}
		else if (codeRows.containsKey(codeKey)) {
			reader.error("Mã", "Mã " + code + " lặp với dòng " + codeRows.get(codeKey));
		}
		else {
			codeRows.put(codeKey, reader.number());
		}
		return code;
	}

	private static Long owner(RowReader reader, PersonIndex people) {
		String owner = reader.text("Chủ");
		if (owner == null) {
			return null;
		}
		PersonRef ref = people.resolve(owner);
		String problem = PersonIndex.problem(ref);
		if (problem != null) {
			reader.error("Chủ", problem);
			return null;
		}
		return ((PersonIndex.Found) ref).id();
	}

	/** Cảnh báo "có vẻ đã có" (spec N7): cùng tên, cùng nơi với một món chưa gỡ, hay với một dòng trước. */
	private static void warnDuplicate(RowReader reader, Item item, String placePath, Set<String> knownItems,
			Map<String, Integer> itemRows) {
		String key = itemKey(item.placeId(), item.name());
		if (knownItems.contains(key)) {
			reader.warning("Tên", "Đã có món '" + item.name() + "' ở '" + placePath + "'");
		}
		else if (itemRows.containsKey(key)) {
			reader.warning("Tên", "Trùng với dòng " + itemRows.get(key));
		}
		itemRows.putIfAbsent(key, reader.number());
	}

	private static String itemKey(long placeId, String name) {
		return placeId + "|" + ImportText.key(name);
	}

	private static String label(String code, String name) {
		if (code == null) {
			return name;
		}
		return (name != null) ? code + " · " + name : code;
	}

}
