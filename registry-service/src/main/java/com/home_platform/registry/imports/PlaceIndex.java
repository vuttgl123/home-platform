package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.home_platform.registry.imports.ImportLookupRepository.KnownPlace;

/**
 * Nhận ra địa điểm được nhắc tới (spec N4): theo mã trước, rồi theo đường dẫn tên từ cấp cao nhất
 * ("Toà A / Tầng 3 / P301"). Gồm địa điểm đã có và các dòng của sheet Địa điểm, kể cả dòng lỗi (để
 * dòng nhắc tới nó biết là nó lỗi).
 */
final class PlaceIndex {

	/** Cách ghi đường dẫn khi hiện ra, như file xuất. */
	static final String SEPARATOR = " / ";

	/** Kết quả khi nhắc tới một địa điểm. */
	sealed interface PlaceRef permits Found, Missing, Ambiguous, FailedRow {
	}

	/** @param path đường dẫn tên, ngăn bằng {@value #SEPARATOR} */
	record Found(long id, String path) implements PlaceRef {
	}

	record Missing(String value) implements PlaceRef {
	}

	record Ambiguous(String value, int count) implements PlaceRef {
	}

	record FailedRow(SheetKind sheet, int row) implements PlaceRef {
	}

	/** @param row dòng của sheet Địa điểm; {@code null} là địa điểm đã có */
	private record Entry(Long id, String path, Integer row) {
	}

	private final Map<String, Entry> byCode = new HashMap<>();

	private final Map<String, List<Entry>> byPath = new HashMap<>();

	private PlaceIndex() {
	}

	static PlaceIndex of(ImportRun run) {
		return run.shared(PlaceIndex.class, () -> build(run.knownPlaces()));
	}

	private static PlaceIndex build(List<KnownPlace> places) {
		PlaceIndex index = new PlaceIndex();
		Map<Long, KnownPlace> byId = new HashMap<>();
		places.forEach(place -> byId.put(place.id(), place));
		for (KnownPlace place : places) {
			index.put(place.code(), pathOf(place, byId), place.id(), null);
		}
		return index;
	}

	/** Tên từ gốc tới địa điểm; dừng khi gặp lại một địa điểm (dữ liệu hỏng), như {@code PlacePaths}. */
	private static String pathOf(KnownPlace place, Map<Long, KnownPlace> byId) {
		List<String> names = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		KnownPlace node = place;
		while (node != null && seen.add(node.id())) {
			names.add(node.name());
			node = (node.parentId() != null) ? byId.get(node.parentId()) : null;
		}
		return String.join(SEPARATOR, names.reversed());
	}

	/**
	 * Ghi một dòng của sheet Địa điểm.
	 * @param code mã giữ từ file hay mã được cấp; {@code null} khi không có
	 * @param id {@code null} khi dòng lỗi
	 */
	void addRow(int row, String code, String path, Long id) {
		put(code, path, id, row);
	}

	private void put(String code, String path, Long id, Integer row) {
		Entry entry = new Entry(id, path, row);
		String codeKey = ImportText.codeKey(code);
		if (codeKey != null) {
			this.byCode.putIfAbsent(codeKey, entry);
		}
		this.byPath.computeIfAbsent(pathKey(path), key -> new ArrayList<>()).add(entry);
	}

	/** Giá trị có phải là mã của một địa điểm đã biết không. */
	boolean isCode(String value) {
		String codeKey = ImportText.codeKey(value);
		return codeKey != null && this.byCode.containsKey(codeKey);
	}

	PlaceRef resolve(String value) {
		Entry byCode = this.byCode.get(ImportText.codeKey(value));
		if (byCode != null) {
			return found(byCode);
		}
		List<Entry> matches = this.byPath.getOrDefault(pathKey(value), List.of());
		if (matches.isEmpty()) {
			return new Missing(ImportText.clean(value));
		}
		if (matches.size() > 1) {
			return new Ambiguous(ImportText.clean(value), matches.size());
		}
		return found(matches.getFirst());
	}

	/**
	 * Cảnh báo "có vẻ đã có" cho một dòng sắp ghi với đường dẫn {@code path}: lời nhắn nếu đường dẫn
	 * đó đã thuộc về một địa điểm đã có hay một dòng trước; {@code null} nếu không.
	 */
	String duplicateWarning(String path) {
		List<Entry> matches = this.byPath.getOrDefault(pathKey(path), List.of());
		for (Entry entry : matches) {
			if (entry.row() == null) {
				return "Đã có địa điểm '" + path + "'";
			}
		}
		return matches.stream()
			.filter(entry -> entry.id() != null)
			.findFirst()
			.map(entry -> "Trùng đường dẫn với dòng " + entry.row())
			.orElse(null);
	}

	/** Lời nhắn lỗi khi tham chiếu không dùng được; {@code null} khi tìm thấy. */
	static String problem(PlaceRef ref) {
		return switch (ref) {
			case Found found -> null;
			case Missing missing -> "Không tìm thấy địa điểm '" + missing.value() + "'";
			case Ambiguous ambiguous ->
				"Có " + ambiguous.count() + " địa điểm '" + ambiguous.value() + "'; ghi mã thay cho đường dẫn";
			case FailedRow failed -> "Địa điểm ở dòng " + failed.row() + " (sheet " + failed.sheet().sheetName() + ") bị lỗi";
		};
	}

	/** Khoá của một đường dẫn: từng phần (ngăn bằng {@code /}) qua {@link ImportText#key}. */
	static String pathKey(String path) {
		if (path == null) {
			return "";
		}
		return Arrays.stream(path.split("/"))
			.map(ImportText::key)
			.map(segment -> (segment != null) ? segment : "")
			.collect(Collectors.joining("/"));
	}

	private static PlaceRef found(Entry entry) {
		return (entry.id() != null) ? new Found(entry.id(), entry.path()) : new FailedRow(SheetKind.PLACE, entry.row());
	}

}
