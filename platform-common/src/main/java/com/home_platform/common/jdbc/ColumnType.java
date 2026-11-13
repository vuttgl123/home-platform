package com.home_platform.common.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cách một kiểu Java đi vào và ra khỏi JDBC. Mỗi kiểu được hỗ trợ có một cài đặt trong
 * {@link ColumnTypes}.
 */
interface ColumnType {

	/** Giá trị đưa cho JDBC làm tham số. {@code value} không bao giờ là {@code null}. */
	Object toJdbc(Object value);

	/** Đọc một cột của dòng hiện tại; trả {@code null} khi cột là {@code NULL}. */
	Object read(ResultSet rs, String column) throws SQLException;

	/** Dạng chuỗi của giá trị, dùng để đặt vào cursor. */
	String toText(Object value);

	/**
	 * Ngược của {@link #toText}.
	 * @throws IllegalArgumentException nếu chuỗi không đổi được về kiểu này
	 */
	Object fromText(String text);

}
