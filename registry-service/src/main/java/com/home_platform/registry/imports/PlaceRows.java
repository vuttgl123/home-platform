package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.export.ExportLabels;
import com.home_platform.registry.imports.PlaceIndex.Found;
import com.home_platform.registry.imports.PlaceIndex.PlaceRef;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceCreateRequest;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.place.PlaceStatus;
import com.home_platform.registry.placetype.PlaceType;
import org.springframework.stereotype.Component;

/**
 * Sheet Địa điểm (spec N2, N3, N5): cha luôn được tạo trước con, dù dòng con đứng trước trong file.
 * Dòng chỉ tới nhau thành vòng thì lỗi.
 */
@Component
class PlaceRows implements SheetRows {

	static final int MAX_CODE = 50;

	static final String CYCLE = "Các dòng chỉ tới nhau thành vòng";

	private static final Map<String, String> COLUMNS = Map.of("name", "Tên", "placeTypeId", "Loại", "description",
			"Mô tả", "capacity", "Sức chứa", "parentId", "Nằm trong", "status", "Trạng thái");

	private static final Map<String, PlaceStatus> STATUSES = new LinkedHashMap<>();

	private static final List<String> STATUS_LABELS = new ArrayList<>();

	static {
		for (PlaceStatus status : PlaceStatus.values()) {
			STATUSES.put(ImportText.key(ExportLabels.placeStatus(status)), status);
			STATUSES.put(ImportText.key(status.name()), status);
			STATUS_LABELS.add(ExportLabels.placeStatus(status));
		}
	}

	private final PlaceService places;

	private final RowRunner runner;

	PlaceRows(PlaceService places, RowRunner runner) {
		this.places = places;
		this.runner = runner;
	}

	@Override
	public SheetKind kind() {
		return SheetKind.PLACE;
	}

	@Override
	public List<RowReport> importAll(ImportSheet sheet, ImportRun run) {
		PlaceIndex index = PlaceIndex.of(run);
		TypeLookup types = TypeLookup.of(run);
		AttributeColumns attributes = run.attributes(SheetKind.PLACE);
		Map<String, String> columns = Columns.withAttributes(COLUMNS, attributes);
		List<Draft> pending = new ArrayList<>();
		Map<String, Integer> codeRows = new HashMap<>();
		for (ImportRow row : sheet.rows()) {
			pending.add(draft(row, types, attributes, codeRows));
		}
		Set<String> fileCodes = new HashSet<>();
		pending.stream().filter(draft -> draft.codeKey != null).forEach(draft -> fileCodes.add(draft.codeKey));
		for (Draft draft : pending) {
			draft.textPath = textPath(draft, index, fileCodes);
		}
		List<Draft> done = new ArrayList<>();
		boolean progress = true;
		while (!pending.isEmpty() && progress) {
			progress = false;
			for (Draft draft : List.copyOf(pending)) {
				if (!waits(draft, pending)) {
					write(draft, index, run, columns);
					pending.remove(draft);
					done.add(draft);
					progress = true;
				}
			}
		}
		for (Draft draft : pending) {
			draft.reader.error("Nằm trong", CYCLE);
			index.addRow(draft.reader.number(), draft.code, draft.name, null);
			done.add(draft);
		}
		return done.stream()
			.sorted(Comparator.comparingInt(draft -> draft.reader.number()))
			.map(draft -> draft.reader.report(label(draft.code, draft.name), draft.code))
			.toList();
	}

	/** Đọc các ô của một dòng; chưa ghi gì. */
	private static Draft draft(ImportRow row, TypeLookup types, AttributeColumns attributes,
			Map<String, Integer> codeRows) {
		RowReader reader = new RowReader(row);
		Draft draft = new Draft(reader);
		draft.code = reader.text("Mã");
		if (draft.code != null) {
			String codeKey = ImportText.codeKey(draft.code);
			if (draft.code.length() > MAX_CODE) {
				reader.error("Mã", "Dài quá " + MAX_CODE + " ký tự");
			}
			else if (codeRows.containsKey(codeKey)) {
				reader.error("Mã", "Mã " + draft.code + " lặp với dòng " + codeRows.get(codeKey));
			}
			else {
				codeRows.put(codeKey, row.number());
			}
		}
		draft.codeKey = (draft.code != null) ? ImportText.codeKey(draft.code) : null;
		draft.name = reader.requiredText("Tên");
		draft.nameKey = ImportText.key(draft.name);
		PlaceType type = types.placeType(reader, "Loại");
		draft.typeId = (type != null) ? type.id() : null;
		draft.capacity = reader.integer("Sức chứa", 0, 9999);
		draft.parent = reader.text("Nằm trong");
		if (draft.parent != null) {
			draft.parentCodeKey = ImportText.codeKey(draft.parent);
			draft.parentPathKey = PlaceIndex.pathKey(draft.parent);
			String[] segments = draft.parentPathKey.split("/");
			draft.parentLastKey = (segments.length > 0) ? segments[segments.length - 1] : "";
		}
		draft.status = reader.choice("Trạng thái", STATUSES, STATUS_LABELS);
		draft.description = reader.text("Mô tả");
		draft.attributes = (type != null) ? AttributeCells.read(reader, attributes, type.id(), type.name())
				: AttributeCells.readShared(reader, attributes);
		return draft;
	}

	/**
	 * Đường dẫn (khoá) của dòng khi đọc được từ chữ: dòng cấp cao nhất, hoặc dòng có "Nằm trong" là
	 * đường dẫn. {@code null} khi "Nằm trong" là một mã: đường dẫn chỉ biết sau khi tìm ra cha.
	 */
	private static String textPath(Draft draft, PlaceIndex index, Set<String> fileCodes) {
		if (draft.parent == null) {
			return draft.nameKey;
		}
		boolean isCode = index.isCode(draft.parent) || fileCodes.contains(draft.parentCodeKey);
		return isCode ? null : draft.parentPathKey + "/" + draft.nameKey;
	}

	/** Dòng này phải chờ một dòng chưa ghi: "Nằm trong" có thể chỉ tới dòng đó. */
	private static boolean waits(Draft draft, List<Draft> pending) {
		if (draft.parent == null) {
			return false;
		}
		for (Draft other : pending) {
			if (other.codeKey != null && other.codeKey.equals(draft.parentCodeKey)) {
				return true;
			}
			if ((other.textPath != null) ? other.textPath.equals(draft.parentPathKey)
					: draft.parentLastKey.equals(other.nameKey)) {
				return true;
			}
		}
		return false;
	}

	private void write(Draft draft, PlaceIndex index, ImportRun run, Map<String, String> columns) {
		RowReader reader = draft.reader;
		Long parentId = null;
		String parentPath = null;
		if (draft.parent != null) {
			PlaceRef ref = index.resolve(draft.parent);
			String problem = PlaceIndex.problem(ref);
			if (problem != null) {
				reader.error("Nằm trong", problem);
			}
			else {
				Found found = (Found) ref;
				parentId = found.id();
				parentPath = found.path();
			}
		}
		String path = (parentPath != null) ? parentPath + PlaceIndex.SEPARATOR + draft.name : draft.name;
		PlaceCreateRequest request = new PlaceCreateRequest(parentId, draft.typeId, draft.name, draft.description,
				draft.status, draft.capacity, draft.attributes, null, null, run.versionNote());
		this.runner.validate(request, reader, columns);
		Place created = this.runner.run(reader, columns,
				() -> this.places.create(run.ctx(), request, draft.code));
		if (created != null) {
			draft.code = created.code();
			String warning = index.duplicateWarning(path);
			if (warning != null) {
				reader.warning("Tên", warning);
			}
		}
		index.addRow(reader.number(), draft.code, path, (created != null) ? created.id() : null);
	}

	private static String label(String code, String name) {
		if (code == null) {
			return name;
		}
		return (name != null) ? code + " · " + name : code;
	}

	/** Một dòng đã đọc, chờ được ghi. */
	private static final class Draft {

		private final RowReader reader;

		private String code;

		private String name;

		private Long typeId;

		private Integer capacity;

		private String parent;

		private PlaceStatus status;

		private String description;

		private Map<String, Object> attributes;

		private String textPath;

		private String codeKey;

		private String nameKey;

		private String parentCodeKey;

		private String parentPathKey;

		private String parentLastKey;

		private Draft(RowReader reader) {
			this.reader = reader;
		}

	}

}
