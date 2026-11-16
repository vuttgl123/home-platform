package com.home_platform.registry.trash;

import java.util.Map;
import java.util.Objects;

import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.item.Item;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.person.Person;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.Place;
import com.home_platform.registry.place.PlaceService;
import org.springframework.stereotype.Service;

/**
 * Thùng rác: địa điểm, người, đồ đạc đang bị xoá mềm, mỗi loại một danh sách, mới xoá trước. Không
 * có xoá hẳn; khôi phục dùng lệnh khôi phục của từng loại.
 */
@Service
public class TrashService {

	private final PlaceService places;

	private final PersonService people;

	private final ItemService items;

	TrashService(PlaceService places, PersonService people, ItemService items) {
		this.places = places;
		this.people = people;
		this.items = items;
	}

	/** Một trang của loại {@code type}. Tên chỗ chứa (chỗ đặt) của cả trang lấy bằng một truy vấn. */
	public CursorPage<TrashEntry> list(long workspaceId, TrashType type, CursorRequest page) {
		return switch (type) {
			case PLACE -> {
				CursorPage<Place> found = this.places.deleted(workspaceId, page);
				Map<Long, String> parents = this.places.names(workspaceId,
						found.items().stream().map(Place::parentId).filter(Objects::nonNull).distinct().toList());
				yield found.map(place -> new TrashEntry(type, place.id(), place.name(), place.code(),
						(place.parentId() != null) ? parents.get(place.parentId()) : null, place.deletedAt(),
						place.updatedBy(), place.version()));
			}
			case ITEM -> {
				CursorPage<Item> found = this.items.deleted(workspaceId, page);
				Map<Long, String> placeNames = this.places.names(workspaceId,
						found.items().stream().map(Item::placeId).distinct().toList());
				yield found.map(item -> new TrashEntry(type, item.id(), item.name(), item.code(),
						placeNames.get(item.placeId()), item.deletedAt(), item.updatedBy(), item.version()));
			}
			case PERSON -> this.people.deleted(workspaceId, page)
				.map((Person person) -> new TrashEntry(type, person.id(), person.fullName(), null, null,
						person.deletedAt(), person.updatedBy(), person.version()));
		};
	}

}
