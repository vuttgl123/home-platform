package com.home_platform.registry.export;

import java.math.BigInteger;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thứ tự của sheet Địa điểm: theo cây (cha ngay trước các con), các địa điểm cùng cha xếp theo tên
 * như người đọc mong đợi: so theo tiếng Việt, đoạn chữ số so như số ("P2" trước "P10"), bằng nhau thì
 * theo {@code id} để file xuất hai lần vẫn y hệt.
 */
final class PlaceOrder {

	private static final Pattern PARTS = Pattern.compile("\\d+|\\D+");

	/** {@link Collator} không dùng chung được giữa các luồng. */
	private static final ThreadLocal<Collator> VIETNAMESE = ThreadLocal.withInitial(() -> {
		Collator collator = Collator.getInstance(Locale.of("vi"));
		collator.setStrength(Collator.PRIMARY);
		return collator;
	});

	/** So hai tên: từng đoạn chữ theo collation tiếng Việt (bỏ qua hoa thường), từng đoạn số theo giá trị. */
	static final Comparator<String> NAME = PlaceOrder::compareNames;

	private static final Comparator<ExportPlaceRow> SIBLINGS = Comparator.comparing(ExportPlaceRow::name, NAME)
		.thenComparingLong(ExportPlaceRow::id);

	private PlaceOrder() {
	}

	/**
	 * Cây theo thứ tự đọc từ trên xuống. Địa điểm có cha không nằm trong danh sách đứng như cấp cao
	 * nhất, để không dòng nào bị bỏ sót.
	 */
	static List<ExportPlaceRow> treeOrder(List<ExportPlaceRow> places) {
		Set<Long> ids = new HashSet<>();
		places.forEach(place -> ids.add(place.id()));
		Map<Long, List<ExportPlaceRow>> children = new HashMap<>();
		List<ExportPlaceRow> roots = new ArrayList<>();
		for (ExportPlaceRow place : places) {
			Long parent = parentOf(place);
			if (parent != null && ids.contains(parent)) {
				children.computeIfAbsent(parent, key -> new ArrayList<>()).add(place);
			}
			else {
				roots.add(place);
			}
		}
		List<ExportPlaceRow> ordered = new ArrayList<>(places.size());
		roots.sort(SIBLINGS);
		ArrayList<ExportPlaceRow> stack = new ArrayList<>(roots.reversed());
		Set<Long> seen = new HashSet<>();
		while (!stack.isEmpty()) {
			ExportPlaceRow place = stack.removeLast();
			if (!seen.add(place.id())) {
				continue;
			}
			ordered.add(place);
			List<ExportPlaceRow> kids = new ArrayList<>(children.getOrDefault(place.id(), List.of()));
			kids.sort(SIBLINGS);
			stack.addAll(kids.reversed());
		}
		return ordered;
	}

	/** Cha theo {@code path} ("/1/5/6/" có cha 5); {@code null} với địa điểm cấp cao nhất. */
	private static Long parentOf(ExportPlaceRow place) {
		String[] ids = place.path().split("/");
		List<String> parts = new ArrayList<>();
		for (String id : ids) {
			if (!id.isEmpty()) {
				parts.add(id);
			}
		}
		return (parts.size() >= 2) ? Long.valueOf(parts.get(parts.size() - 2)) : null;
	}

	private static int compareNames(String left, String right) {
		Collator collator = VIETNAMESE.get();
		Matcher a = PARTS.matcher((left != null) ? left : "");
		Matcher b = PARTS.matcher((right != null) ? right : "");
		while (true) {
			boolean hasA = a.find();
			boolean hasB = b.find();
			if (!hasA || !hasB) {
				return Boolean.compare(hasA, hasB);
			}
			String partA = a.group();
			String partB = b.group();
			boolean digitsA = Character.isDigit(partA.charAt(0));
			boolean digitsB = Character.isDigit(partB.charAt(0));
			int result = (digitsA && digitsB) ? new BigInteger(partA).compareTo(new BigInteger(partB))
					: collator.compare(partA, partB);
			if (result != 0) {
				return result;
			}
		}
	}

}
