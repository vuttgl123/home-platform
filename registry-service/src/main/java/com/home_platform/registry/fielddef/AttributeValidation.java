package com.home_platform.registry.fielddef;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Lối vào cho service của địa điểm, người, đồ đạc: kiểm tra {@code attributes} theo các mục
 * tuỳ biến của không gian. Danh sách mục đọc qua cache của {@link FieldDefService#listAll}.
 */
@Component
public class AttributeValidation {

	private final FieldDefService fieldDefs;

	AttributeValidation(FieldDefService fieldDefs) {
		this.fieldDefs = fieldDefs;
	}

	/**
	 * @param typeId loại địa điểm hoặc nhóm đồ của bản ghi; 0 nếu không có (người luôn là 0)
	 * @return giá trị sẽ lưu; {@code null} nếu không còn khoá nào
	 * @throws com.home_platform.common.web.BadRequestException nếu có giá trị không hợp lệ
	 */
	public Map<String, Object> validate(long workspaceId, FieldTarget target, long typeId,
			Map<String, Object> attributes) {
		List<FieldDef> applicable = this.fieldDefs.listAll(workspaceId)
			.stream()
			.filter(def -> def.target() == target && (def.typeId() == 0 || def.typeId() == typeId))
			.toList();
		return AttributeValidator.validate(applicable, attributes);
	}

}
