package com.home_platform.registry.history;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.home_platform.common.history.ChangeFilter;
import com.home_platform.common.history.EntityChange;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.person.PersonService;
import com.home_platform.registry.place.PlaceService;
import com.home_platform.registry.support.Text;
import org.springframework.stereotype.Service;

/** Hoạt động gần đây của một không gian; cũng là chỗ người dùng tìm lại thứ đã xoá để khôi phục. */
@Service
public class ActivityService {

	private final EntityHistory history;

	private final PlaceService places;

	private final PersonService people;

	private final ItemService items;

	ActivityService(EntityHistory history, PlaceService places, PersonService people, ItemService items) {
		this.history = history;
		this.places = places;
		this.people = people;
		this.items = items;
	}

	/**
	 * Các version mới nhất trước, mỗi dòng kèm tên hiện tại của bản ghi. Mỗi loại bản ghi có mặt
	 * trong trang được tra tên bằng một truy vấn.
	 * @param filter loại bản ghi (PLACE, PERSON, ITEM), hành động, người làm, khoảng thời gian;
	 * trường {@code null} là không lọc
	 * @throws BadRequestException nếu {@code from} không trước {@code to}
	 */
	public CursorPage<ActivityEntry> list(long workspaceId, ChangeFilter filter, CursorRequest page) {
		if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
			throw new BadRequestException("from must be before to");
		}
		ChangeFilter trimmed = new ChangeFilter(Text.trimToNull(filter.entityType()), filter.action(),
				filter.changedBy(), filter.from(), filter.to());
		CursorPage<EntityChange> changes = this.history.listRecent(workspaceId, trimmed, page);
		Map<String, List<Long>> idsByType = changes.items()
			.stream()
			.collect(Collectors.groupingBy(EntityChange::entityType,
					Collectors.mapping(EntityChange::entityId, Collectors.collectingAndThen(Collectors.toList(),
							ids -> ids.stream().distinct().toList()))));
		Map<String, Map<Long, String>> names = new HashMap<>();
		idsByType.forEach((type, ids) -> names.put(type, namesOf(workspaceId, type, ids)));
		return changes.map(change -> new ActivityEntry(change,
				names.getOrDefault(change.entityType(), Map.of()).get(change.entityId())));
	}

	private Map<Long, String> namesOf(long workspaceId, String entityType, List<Long> ids) {
		return switch (entityType) {
			case PlaceService.ENTITY_TYPE -> this.places.names(workspaceId, ids);
			case PersonService.ENTITY_TYPE -> this.people.names(workspaceId, ids);
			case ItemService.ENTITY_TYPE -> this.items.names(workspaceId, ids);
			default -> Map.of();
		};
	}

}
