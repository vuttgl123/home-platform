package com.home_platform.common.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đổi vị trí trong một danh sách thành chuỗi {@code cursor} và ngược lại. Cursor là Base64URL
 * của một mảng JSON các chuỗi; client chỉ giữ và gửi lại, không cần hiểu nội dung.
 */
public final class CursorCodec {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();

	private CursorCodec() {
	}

	public static String encode(List<String> parts) {
		byte[] json = MAPPER.writeValueAsBytes(parts);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
	}

	/**
	 * @throws BadRequestException nếu chuỗi không phải một cursor do {@link #encode} tạo ra
	 */
	public static List<String> decode(String cursor) {
		try {
			JsonNode tree = MAPPER.readTree(Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)));
			if (tree == null || !tree.isArray() || tree.isEmpty()) {
				throw invalid();
			}
			List<String> parts = new ArrayList<>(tree.size());
			for (JsonNode part : tree) {
				if (!part.isString()) {
					throw invalid();
				}
				parts.add(part.asString());
			}
			return parts;
		}
		catch (IllegalArgumentException | JacksonException ex) {
			throw invalid();
		}
	}

	private static BadRequestException invalid() {
		return new BadRequestException("Invalid cursor");
	}

}
