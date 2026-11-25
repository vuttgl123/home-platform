package com.home_platform.registry.todo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.home_platform.common.context.WorkspaceContext;
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

	/** Số việc mới nhất trong tóm tắt, cho chuông. */
	static final int LATEST = 10;

	private final TodoRepository repository;

	private final FieldDefService fieldDefs;

	private final EntityHistory history;

	private final BusinessDate businessDate;

	private final TodoSeenRepository seen;

	private final Clock clock;

	TodoService(TodoRepository repository, FieldDefService fieldDefs, EntityHistory history,
			BusinessDate businessDate, TodoSeenRepository seen, Clock clock) {
		this.repository = repository;
		this.fieldDefs = fieldDefs;
		this.history = history;
		this.businessDate = businessDate;
		this.seen = seen;
		this.clock = clock;
	}

	/**
	 * Mọi việc, đã sắp theo {@link TodoOrder#ORDER}. {@code since} của mỗi việc là mốc muộn hơn giữa đầu
	 * khung nhắc và lúc giá trị gây ra việc được đặt (L6), không bao giờ sau lúc đọc.
	 */
	@Transactional(readOnly = true)
	public List<Todo> all(long workspaceId) {
		LocalDate today = this.businessDate.today();
		Instant now = this.clock.instant();
		List<Todo> todos = new ArrayList<>();
		todos.addAll(broken(workspaceId));
		todos.addAll(warranties(workspaceId, today));
		todos.addAll(services(workspaceId, today));
		todos.addAll(moveOuts(workspaceId, today));
		todos.addAll(customDates(workspaceId, today));
		return TodoOrder.sort(todos.stream().map(todo -> todo.since().isAfter(now) ? withSince(todo, now) : todo).toList());
	}

	/** Một trang việc, có thể chỉ một loại; {@code isNew} so với mốc đã xem của người gọi. */
	@Transactional(readOnly = true)
	public CursorPage<TodoResponse> page(WorkspaceContext ctx, TodoKind kind, CursorRequest page) {
		List<Todo> all = all(ctx.workspaceId());
		Predicate<Todo> isNew = newSince(ctx);
		return TodoOrder.page((kind != null) ? all.stream().filter(todo -> todo.kind() == kind).toList() : all, page)
			.map(todo -> TodoResponse.of(todo, isNew.test(todo)));
	}

	/** Tóm tắt cho thanh bên và chuông, cùng một lần tính. */
	@Transactional(readOnly = true)
	public TodoSummaryResponse summary(WorkspaceContext ctx) {
		List<Todo> all = all(ctx.workspaceId());
		Predicate<Todo> isNew = newSince(ctx);
		Map<TodoKind, Integer> byKind = new EnumMap<>(TodoKind.class);
		for (TodoKind kind : TodoKind.values()) {
			byKind.put(kind, 0);
		}
		all.forEach(todo -> byKind.merge(todo.kind(), 1, Integer::sum));
		List<TodoResponse> latest = all.stream()
			.sorted(Comparator.comparing(Todo::since).reversed().thenComparing(Todo::key))
			.limit(LATEST)
			.map(todo -> TodoResponse.of(todo, isNew.test(todo)))
			.toList();
		return new TodoSummaryResponse(all.size(), byKind, (int) all.stream().filter(isNew).count(), latest);
	}

	/** Người gọi vừa mở chuông: mọi việc đang có không còn là mới với họ. */
	@Transactional
	public void markSeen(WorkspaceContext ctx) {
		this.seen.markSeen(ctx.workspaceId(), ctx.userId(), this.clock.instant());
	}

	/** Chưa mở chuông lần nào thì mọi việc là mới. */
	private Predicate<Todo> newSince(WorkspaceContext ctx) {
		Optional<Instant> seenAt = this.seen.seenAt(ctx.workspaceId(), ctx.userId());
		return todo -> seenAt.map(at -> todo.since().isAfter(at)).orElse(true);
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
		List<ItemRow> rows = this.repository.warrantiesEnding(workspaceId, today, today.plusDays(WARRANTY_DAYS));
		Map<Long, Instant> setAt = this.history.valueSetAt(workspaceId, ItemService.ENTITY_TYPE,
				rows.stream().map(ItemRow::id).toList(), "$.warrantyUntil");
		return rows.stream()
			.map(row -> itemTodo(TodoKind.WARRANTY, row, row.warrantyUntil(),
					latest(startOf(row.warrantyUntil().minusDays(WARRANTY_DAYS)), setAt.get(row.id()))))
			.toList();
	}

	private List<Todo> services(long workspaceId, LocalDate today) {
		List<ItemRow> due = new ArrayList<>();
		Map<Long, LocalDate> dueOn = new HashMap<>();
		for (ItemRow row : this.repository.scheduledItems(workspaceId)) {
			LocalDate next = ServiceSchedule.nextServiceOn(row.lastServiceOn(), row.placedAt(),
					LocalDate.ofInstant(row.createdAt(), zone()), row.serviceEveryMonths());
			if (!next.isAfter(today.plusDays(SERVICE_DAYS))) {
				due.add(row);
				dueOn.put(row.id(), next);
			}
		}
		Map<Long, Instant> intervalSetAt = this.history.valueSetAt(workspaceId, ItemService.ENTITY_TYPE,
				due.stream().map(ItemRow::id).toList(), "$.serviceEveryMonths");
		return due.stream().map(row -> {
			LocalDate next = dueOn.get(row.id());
			Instant since = latest(latest(startOf(next.minusDays(SERVICE_DAYS)), intervalSetAt.get(row.id())),
					row.serviceLogChangedAt());
			return itemTodo(TodoKind.SERVICE, row, next, since);
		}).toList();
	}

	private List<Todo> moveOuts(long workspaceId, LocalDate today) {
		return this.repository.movingOut(workspaceId, today, today.plusDays(MOVE_OUT_DAYS))
			.stream()
			.map(row -> new Todo(TodoKind.MOVE_OUT + ":" + row.occupancyId(), TodoKind.MOVE_OUT, row.endDate(),
					latest(startOf(row.endDate().minusDays(MOVE_OUT_DAYS)), row.updatedAt()), "PERSON", row.personId(), row.personName(), null,
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
			Map<DateValueRow, LocalDate> inWindow = new LinkedHashMap<>();
			for (DateValueRow row : this.repository.dateValues(workspaceId, def.target(), def.typeId(),
					def.fieldKey())) {
				LocalDate date = parse(row.value());
				if (date != null && !today.isBefore(date.minusDays(def.remindDays()))
						&& !today.isAfter(date.plusDays(CUSTOM_GRACE_DAYS))) {
					inWindow.put(row, date);
				}
			}
			// Loại bản ghi trong bảng version trùng tên đích: PLACE, PERSON, ITEM.
			Map<Long, Instant> setAt = this.history.valueSetAt(workspaceId, def.target().name(),
					inWindow.keySet().stream().map(DateValueRow::id).toList(), "$.attributes." + def.fieldKey());
			inWindow.forEach((row, date) -> {
				Instant since = latest(latest(startOf(date.minusDays(def.remindDays())), setAt.get(row.id())),
						def.updatedAt());
				todos.add(new Todo(TodoKind.CUSTOM_DATE + ":" + def.target() + ":" + row.id() + ":" + def.fieldKey(),
						TodoKind.CUSTOM_DATE, date, since, def.target().name(), row.id(), row.name(), row.code(),
						def.label()));
			});
		}
		return todos;
	}

	/** Mốc muộn hơn; {@code other} trống thì giữ {@code base}. */
	private static Instant latest(Instant base, Instant other) {
		return (other != null && other.isAfter(base)) ? other : base;
	}

	private static Todo withSince(Todo todo, Instant since) {
		return new Todo(todo.key(), todo.kind(), todo.dueOn(), since, todo.subjectType(), todo.subjectId(),
				todo.subjectName(), todo.subjectCode(), todo.context());
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
