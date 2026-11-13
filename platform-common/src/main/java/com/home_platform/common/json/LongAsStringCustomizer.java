package com.home_platform.common.json;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Ghi mọi giá trị {@code long} và {@code Long} ra JSON dưới dạng chuỗi.
 *
 * <p>{@code id} của hệ thống là số 64 bit, vượt quá độ chính xác của kiểu số trong
 * JavaScript. Áp dụng cho mọi {@code long} thay vì đánh dấu từng trường: quên đánh dấu một
 * trường {@code id} thì trình duyệt làm tròn sai mà không báo lỗi gì. Kiểu {@code int} vẫn
 * là số. Chiều đọc vào không cần gì thêm: Jackson nhận cả chuỗi lẫn số cho một trường
 * {@code long}.
 */
public class LongAsStringCustomizer implements JsonMapperBuilderCustomizer {

	@Override
	public void customize(JsonMapper.Builder builder) {
		SimpleModule module = new SimpleModule("platform-long-as-string");
		module.addSerializer(Long.class, ToStringSerializer.instance);
		module.addSerializer(Long.TYPE, ToStringSerializer.instance);
		builder.addModule(module);
	}

}
