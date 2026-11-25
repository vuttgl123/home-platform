package com.home_platform.registry.todo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.catalog.CatalogStatus;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.item.ServiceSchedule;
import com.home_platform.registry.support.BusinessDate;
import com.home_platform.registry.todo.TodoRepository.DateValueRow;
import com.home_platform.registry.todo.TodoRepository.ItemRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Việc cần làm của một không gian (spec Đợt 8, L5), tính mỗi lần đọc từ năm nguồn, không lưu ở đâu cả.
 * Ngày "hôm nay" và đầu ngày theo múi giờ nghiệp vụ.
 */
@Service
public class TodoService {

	/** Bảo hành: nhắc từ 30 ngày trước ngày hết. */
	static final int WARRANTY_DAYS = 30;

	/** Bảo trì: nhắc từ 7 ngày trước hạn, vì chu kỳ có thể chỉ một tháng. */
	static final int SERVICE_DAYS = 7;

	/** Chuyển đi: như "sắp chuyển đi" của trang Tổng quan. */
	static final int MOVE_OUT_DAYS = 30;

	/** Hạn tuỳ biến đã qua còn hiện thêm bấy nhiêu ngày rồi tự rời danh sách. */
	static final int CUSTOM_GRACE_DAYS = 30;

	private final TodoRepository repository;

	private final FieldDefService fieldDefs;

	private final EntityHistory history;

	private final BusinessDate businessDate;

	TodoService(TodoRepository repository, FieldDefService fieldDefs, EntityHistory history,
			BusinessDate businessDate) {
		this.repository = repository;
		this.fieldDefs = fieldDefs;
		this.history = history;
		this.businessDate = businessDate;
	}

	/** Mọi việc, đã sắp theo {@link TodoOrder#ORDER}. */
	@Transactional(readOnly = true)
	public List<Todo> all(long workspaceId) {
		LocalDate today = this.businessDate.today();
		List<Todo> todos = new ArrayList<>();
		todos.addAll(broken(workspaceId));
		todos.addAll(warranties(workspaceId, today));
		todos.addAll(services(workspaceId, today));
		todos.addAll(moveOuts(workspaceId, today));
		todos.addAll(customDates(workspaceId, today));
		return TodoOrder.sort(todos);
	}

	/** Một trang việc, có thể chỉ một loại. */
	@Transactional(readOnly = true)
	public CursorPage<Todo> page(long workspaceId, TodoKind kind, CursorRequest page) {
		List<Todo> all = all(workspaceId);
		return TodoOrder.page((kind != null) ? all.stream().filter(todo -> todo.kind() == kind).toList() : all, page);
	}

	private List<Todo> broken(long workspaceId) {
		List<ItemRow> rows = this.repository.brokenItems(workspaceId);
		Map<Long, Instant> brokeAt = this.history.valueSetAt(workspaceId, ItemService.ENTITY_TYPE,
				rows.stream().map(ItemRow::id).toList(), "$.status");
		return rows.stream().map(row -> {
			Instant since = brokeAt.getOrDefault(row.id(), row.updatedAt());
			return itemTodo(TodoKind.BROKEN, row, LocalDate.ofInstant(since, zone()), since);
		}).toList();
	}

	private List<Todo> warranties(long workspaceId, LocalDate today) {
		return this.repository.warrantiesEnding(workspaceId, today, today.plusDays(WARRANTY_DAYS))
			.stream()
			.map(row -> itemTodo(TodoKind.WARRANTY, row, row.warrantyUntil(),
					startOf(row.warrantyUntil().minusDays(WARRANTY_DAYS))))
			.toList();
	}

	private List<Todo> services(long workspaceId, LocalDate today) {
		List<Todo> todos = new ArrayList<>();
		for (ItemRow row : this.repository.scheduledItems(workspaceId)) {
			LocalDate due = ServiceSchedule.nextServiceOn(row.lastServiceOn(), row.placedAt(),
					LocalDate.ofInstant(row.createdAt(), zone()), row.serviceEveryMonths());
			if (!due.isAfter(today.plusDays(SERVICE_DAYS))) {
				todos.add(itemTodo(TodoKind.SERVICE, row, due, startOf(due.minusDays(SERVICE_DAYS))));
			}
		}
		return todos;
	}

	private List<Todo> moveOuts(long workspaceId, LocalDate today) {
		return this.repository.movingOut(workspaceId, today, today.plusDays(MOVE_OUT_DAYS))
			.stream()
			.map(row -> new Todo(TodoKind.MOVE_OUT + ":" + row.occupancyId(), TodoKind.MOVE_OUT, row.endDate(),
					startOf(row.endDate().minusDays(MOVE_OUT_DAYS)), "PERSON", row.personId(), row.personName(), null,
					row.placeName()))
			.toList();
	}

	private List<Todo> customDates(long workspaceId, LocalDate today) {
		List<Todo> todos = new ArrayList<>();
		for (FieldDef def : this.fieldDefs.listAll(workspaceId)) {
			if (def.status() != CatalogStatus.ACTIVE || def.dataType() != FieldDataType.DATE
					|| def.remindDays() == null) {
				continue;
			}
			for (DateValueRow row : this.repository.dateValues(workspaceId, def.target(), def.typeId(),
					def.fieldKey())) {
				LocalDate date = parse(row.value());
				if (date == null || today.isBefore(date.minusDays(def.remindDays()))
						|| today.isAfter(date.plusDays(CUSTOM_GRACE_DAYS))) {
					continue;
				}
				todos.add(new Todo(TodoKind.CUSTOM_DATE + ":" + def.target() + ":" + row.id() + ":" + def.fieldKey(),
						TodoKind.CUSTOM_DATE, date, startOf(date.minusDays(def.remindDays())), def.target().name(),
						row.id(), row.name(), row.code(), def.label()));
			}
		}
		return todos;
	}

	private static Todo itemTodo(TodoKind kind, ItemRow row, LocalDate dueOn, Instant since) {
		return new Todo(kind + ":" + row.id(), kind, dueOn, since, "ITEM", row.id(), row.name(), row.code(),
				row.placeName());
	}

	/** Giá trị ngày dạng {@code yyyy-MM-dd}; gõ sai thì {@code null} và việc đó bị bỏ qua (L4). */
	private static LocalDate parse(String value) {
		try {
			return LocalDate.parse(value);
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private Instant startOf(LocalDate day) {
		return day.atStartOfDay(zone()).toInstant();
	}

	private ZoneId zone() {
		return this.businessDate.zone();
	}

}
