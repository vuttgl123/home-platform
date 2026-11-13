package com.home_platform.registry.support;

import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Ghi giá trị của {@code attributes} ra API với số giữ nguyên dạng số.
 *
 * <p>Quy ước chung của API ghi mọi {@code long} thành chuỗi, vì {@code id} vượt độ chính xác
 * của JavaScript. Nhưng giá trị trong {@code attributes} là số người dùng nhập (giá tiền, diện
 * tích), không phải {@code id}: nếu ra chuỗi thì client gửi lại nguyên dữ liệu vừa đọc sẽ bị
 * bộ kiểm tra từ chối vì mục NUMBER phải là số. Dùng làm {@code contentUsing} trên thành phần
 * {@code attributes}.
 */
public final class PlainNumbers extends ValueSerializer<Object> {

	@Override
	public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) {
		switch (value) {
			case BigDecimal decimal -> gen.writeNumber(decimal);
			case BigInteger integer -> gen.writeNumber(integer);
			case Double number -> gen.writeNumber(number);
			case Float number -> gen.writeNumber(number);
			case Number number -> gen.writeNumber(number.longValue());
			default -> ctxt.writeValue(gen, value);
		}
	}

}
