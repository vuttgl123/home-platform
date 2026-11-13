package com.home_platform.common.jdbc;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import com.home_platform.common.json.StorageJson;

/**
 * Chọn {@link ColumnType} cho từng thành phần của record.
 */
final class ColumnTypes {

	private static final Map<Class<?>, ColumnType> SIMPLE = Map.ofEntries(Map.entry(long.class, new LongType()),
			Map.entry(Long.class, new LongType()), Map.entry(int.class, new IntType()),
			Map.entry(Integer.class, new IntType()), Map.entry(boolean.class, new BooleanType()),
			Map.entry(Boolean.class, new BooleanType()), Map.entry(String.class, new StringType()),
			Map.entry(BigDecimal.class, new DecimalType()), Map.entry(Instant.class, new InstantType()),
			Map.entry(LocalDate.class, new LocalDateType()));

	private ColumnTypes() {
	}

	static ColumnType resolve(RecordComponent component, StorageJson json) {
		Class<?> type = component.getType();
		if (component.isAnnotationPresent(JsonColumn.class)) {
			return new JsonType(json, component.getGenericType());
		}
		if (type.isEnum()) {
			return new EnumType(type);
		}
		ColumnType simple = SIMPLE.get(type);
		if (simple == null) {
			throw new IllegalArgumentException("Unsupported type " + type.getName() + " for "
					+ component.getDeclaringRecord().getSimpleName() + "." + component.getName());
		}
		return simple;
	}

	private static final class LongType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			long value = rs.getLong(column);
			return rs.wasNull() ? null : value;
		}

		@Override
		public String toText(Object value) {
			return value.toString();
		}

		@Override
		public Object fromText(String text) {
			return Long.valueOf(text);
		}

	}

	private static final class IntType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			int value = rs.getInt(column);
			return rs.wasNull() ? null : value;
		}

		@Override
		public String toText(Object value) {
			return value.toString();
		}

		@Override
		public Object fromText(String text) {
			return Integer.valueOf(text);
		}

	}

	private static final class BooleanType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			boolean value = rs.getBoolean(column);
			return rs.wasNull() ? null : value;
		}

		@Override
		public String toText(Object value) {
			return value.toString();
		}

		@Override
		public Object fromText(String text) {
			return Boolean.valueOf(text);
		}

	}

	private static final class StringType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			return rs.getString(column);
		}

		@Override
		public String toText(Object value) {
			return (String) value;
		}

		@Override
		public Object fromText(String text) {
			return text;
		}

	}

	private static final class DecimalType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			return rs.getBigDecimal(column);
		}

		@Override
		public String toText(Object value) {
			return ((BigDecimal) value).toPlainString();
		}

		@Override
		public Object fromText(String text) {
			return new BigDecimal(text);
		}

	}

	/**
	 * Cột {@code DATETIME(3)} giữ giờ UTC. Đi qua {@link LocalDateTime} để driver không đổi
	 * múi giờ: kết quả không phụ thuộc múi giờ của máy chạy hay của server database.
	 */
	private static final class InstantType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC);
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			LocalDateTime value = rs.getObject(column, LocalDateTime.class);
			return (value != null) ? value.toInstant(ZoneOffset.UTC) : null;
		}

		@Override
		public String toText(Object value) {
			return value.toString();
		}

		@Override
		public Object fromText(String text) {
			try {
				return Instant.parse(text);
			}
			catch (DateTimeException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

	}

	private static final class LocalDateType implements ColumnType {

		@Override
		public Object toJdbc(Object value) {
			return value;
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			return rs.getObject(column, LocalDate.class);
		}

		@Override
		public String toText(Object value) {
			return value.toString();
		}

		@Override
		public Object fromText(String text) {
			try {
				return LocalDate.parse(text);
			}
			catch (DateTimeException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

	}

	/** Lưu theo tên của hằng enum, khớp với các cột mã trạng thái kiểu {@code VARCHAR}. */
	private static final class EnumType implements ColumnType {

		private final Class<?> type;

		EnumType(Class<?> type) {
			this.type = type;
		}

		@Override
		public Object toJdbc(Object value) {
			return ((Enum<?>) value).name();
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			String value = rs.getString(column);
			return (value != null) ? fromText(value) : null;
		}

		@Override
		public String toText(Object value) {
			return ((Enum<?>) value).name();
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object fromText(String text) {
			return Enum.valueOf((Class<? extends Enum>) this.type, text);
		}

	}

	static final class JsonType implements ColumnType {

		private final StorageJson json;

		private final Type type;

		JsonType(StorageJson json, Type type) {
			this.json = json;
			this.type = type;
		}

		@Override
		public Object toJdbc(Object value) {
			return this.json.write(value);
		}

		@Override
		public Object read(ResultSet rs, String column) throws SQLException {
			String value = rs.getString(column);
			return (value != null) ? this.json.read(value, this.type) : null;
		}

		@Override
		public String toText(Object value) {
			throw new IllegalArgumentException("A JSON column cannot be used for ordering");
		}

		@Override
		public Object fromText(String text) {
			throw new IllegalArgumentException("A JSON column cannot be used for ordering");
		}

	}

}
