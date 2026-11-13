package com.home_platform.common.json;

import java.lang.reflect.Type;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON để lưu vào database (cột {@code JSON}, snapshot của version): số vẫn là số.
 *
 * <p>Quy ước "{@code long} thành chuỗi" chỉ dành cho JSON trả ra API. Nếu áp dụng nó lúc lưu,
 * một số lớn sẽ được lưu thành chuỗi và đọc lên thành kiểu khác. Vì vậy lớp này giữ một
 * {@link JsonMapper} riêng và cố ý không đăng ký nó thành bean: {@code JsonMapper} của
 * Spring Boot vẫn là mapper duy nhất của tầng web.
 */
public final class StorageJson {

	private final JsonMapper mapper = JsonMapper.builder().build();

	public String write(Object value) {
		return this.mapper.writeValueAsString(value);
	}

	public <T> T read(String json, Type type) {
		return this.mapper.readValue(json, this.mapper.constructType(type));
	}

	public JsonNode readTree(String json) {
		return this.mapper.readTree(json);
	}

	public <T> T convert(JsonNode tree, Class<T> type) {
		return this.mapper.treeToValue(tree, type);
	}

}
