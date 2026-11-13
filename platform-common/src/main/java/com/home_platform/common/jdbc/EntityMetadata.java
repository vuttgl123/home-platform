package com.home_platform.common.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.common.json.StorageJson;
import org.springframework.jdbc.core.RowMapper;

/**
 * Cấu trúc của một record ứng với một bảng: mỗi thành phần là một cột, tên cột là dạng
 * {@code snake_case} của tên thành phần. Đọc bằng reflection một lần lúc tạo, sau đó dùng
 * lại cho mọi câu truy vấn.
 *
 * <p>Dùng được độc lập với {@link WorkspaceRepository}, cho những bảng có khoá chính khác
 * {@code (workspace_id, id)}: {@link #selectList()} cho danh sách cột, {@link #rowMapper()}
 * đổi dòng thành record, {@link #parameters(Record)} đổi record thành tham số.
 *
 * @param <T> kiểu record
 */
public final class EntityMetadata<T extends Record> {

	private final Class<T> type;

	private final Constructor<T> constructor;

	/** Theo thứ tự khai báo; khoá là tên thành phần. */
	private final Map<String, Column> byComponent;

	private final Map<String, Column> byColumn;

	private final String selectList;

	private EntityMetadata(Class<T> type, Constructor<T> constructor, List<Column> columns) {
		this.type = type;
		this.constructor = constructor;
		Map<String, Column> byComponent = new LinkedHashMap<>();
		Map<String, Column> byColumn = new LinkedHashMap<>();
		for (Column column : columns) {
			byComponent.put(column.component(), column);
			byColumn.put(column.name(), column);
		}
		this.byComponent = Collections.unmodifiableMap(byComponent);
		this.byColumn = Collections.unmodifiableMap(byColumn);
		this.selectList = String.join(", ", byColumn.keySet());
	}

	/**
	 * @throws IllegalArgumentException nếu record có thành phần thuộc kiểu không được hỗ trợ
	 */
	public static <T extends Record> EntityMetadata<T> of(Class<T> type, StorageJson json) {
		RecordComponent[] components = type.getRecordComponents();
		List<Column> columns = new ArrayList<>(components.length);
		Class<?>[] parameterTypes = new Class<?>[components.length];
		for (int i = 0; i < components.length; i++) {
			RecordComponent component = components[i];
			Method accessor = component.getAccessor();
			accessor.setAccessible(true);
			columns.add(new Column(component.getName(), toSnakeCase(component.getName()),
					ColumnTypes.resolve(component, json), accessor, component.getType().isPrimitive()));
			parameterTypes[i] = component.getType();
		}
		try {
			Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			return new EntityMetadata<>(type, constructor, columns);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No canonical constructor on " + type.getName(), ex);
		}
	}

	public Class<T> type() {
		return this.type;
	}

	/** Tên các thành phần theo thứ tự khai báo. */
	public List<String> components() {
		return List.copyOf(this.byComponent.keySet());
	}

	public boolean has(String component) {
		return this.byComponent.containsKey(component);
	}

	/** Tên cột của một thành phần, ví dụ {@code "validFrom"} cho ra {@code "valid_from"}. */
	public String column(String component) {
		return column0(component).name();
	}

	/** Mọi cột của bảng, cách nhau bằng dấu phẩy, để đặt sau {@code SELECT}. */
	public String selectList() {
		return this.selectList;
	}

	/** Đổi một dòng thành record. Đọc theo tên cột nên thứ tự cột trong câu SELECT không quan trọng. */
	public RowMapper<T> rowMapper() {
		return (rs, rowNum) -> {
			Object[] arguments = new Object[this.byComponent.size()];
			int i = 0;
			for (Column column : this.byComponent.values()) {
				Object value = column.type().read(rs, column.name());
				if (value == null && column.primitive()) {
					throw new IllegalStateException("Column " + column.name() + " is NULL but "
							+ this.type.getSimpleName() + "." + column.component() + " is a primitive");
				}
				arguments[i++] = value;
			}
			return instantiate(arguments);
		};
	}

	/**
	 * Tham số cho một câu SQL: tên thành phần ứng với giá trị đã đổi sang dạng JDBC nhận
	 * được. Thành phần {@code null} vẫn có mặt với giá trị {@code null}.
	 */
	public Map<String, Object> parameters(T entity) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		for (Column column : this.byComponent.values()) {
			Object value = read(entity, column);
			parameters.put(column.component(), (value != null) ? column.type().toJdbc(value) : null);
		}
		return parameters;
	}

	public Object get(T entity, String component) {
		return read(entity, column0(component));
	}

	/** Một bản sao của record với các thành phần được nêu mang giá trị mới. */
	public T with(T entity, Map<String, Object> changes) {
		for (String component : changes.keySet()) {
			column0(component);
		}
		Object[] arguments = new Object[this.byComponent.size()];
		int i = 0;
		for (Column column : this.byComponent.values()) {
			arguments[i++] = changes.containsKey(column.component()) ? changes.get(column.component())
					: read(entity, column);
		}
		return instantiate(arguments);
	}

	ColumnType typeOfColumn(String column) {
		Column found = this.byColumn.get(column);
		if (found == null) {
			throw new IllegalArgumentException(this.type.getSimpleName() + " has no column " + column);
		}
		return found.type();
	}

	private Column column0(String component) {
		Column column = this.byComponent.get(component);
		if (column == null) {
			throw new IllegalArgumentException(this.type.getSimpleName() + " has no component " + component);
		}
		return column;
	}

	private Object read(T entity, Column column) {
		try {
			return column.accessor().invoke(entity);
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot read " + this.type.getSimpleName() + "." + column.component(), ex);
		}
	}

	private T instantiate(Object[] arguments) {
		try {
			return this.constructor.newInstance(arguments);
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException("Cannot create " + this.type.getSimpleName(), ex);
		}
	}

	private static String toSnakeCase(String name) {
		StringBuilder result = new StringBuilder(name.length() + 4);
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				result.append('_').append(Character.toLowerCase(c));
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}

	private record Column(String component, String name, ColumnType type, Method accessor, boolean primitive) {
	}

}
