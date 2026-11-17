package com.home_platform.registry.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đường dẫn tên của địa điểm ("Toà A / Tầng 3 / P301"), dựng trong bộ nhớ từ một lần đọc mọi địa
 * điểm của không gian. Cột {@code path} trong database là đường dẫn {@code id}, không đọc được.
 */
final class PlacePaths {

	static final String SEPARATOR = " / ";

	private final Map<Long, PlaceNode> nodes = new HashMap<>();

	private final Map<Long, String> paths = new HashMap<>();

	private PlacePaths(List<PlaceNode> nodes) {
		for (PlaceNode node : nodes) {
			this.nodes.put(node.id(), node);
		}
	}

	static PlacePaths of(List<PlaceNode> nodes) {
		return new PlacePaths(nodes);
	}

	/** Tên từ gốc tới địa điểm này; {@code id} không có trong danh sách thì là chuỗi rỗng. */
	String pathOf(long placeId) {
		return this.paths.computeIfAbsent(placeId, this::build);
	}

	/** Đường dẫn của địa điểm cha; địa điểm gốc và {@code id} lạ thì là chuỗi rỗng. */
	String parentPathOf(long placeId) {
		PlaceNode node = this.nodes.get(placeId);
		return (node != null && node.parentId() != null) ? pathOf(node.parentId()) : "";
	}

	/** Đi ngược lên cha; dừng ở gốc, ở cha không có trong danh sách, hoặc khi gặp lại một địa điểm. */
	private String build(long placeId) {
		List<String> names = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		PlaceNode node = this.nodes.get(placeId);
		while (node != null && seen.add(node.id())) {
			names.add(node.name());
			node = (node.parentId() != null) ? this.nodes.get(node.parentId()) : null;
		}
		return String.join(SEPARATOR, names.reversed());
	}

}
