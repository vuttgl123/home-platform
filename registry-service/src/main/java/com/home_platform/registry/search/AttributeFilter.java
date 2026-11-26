package com.home_platform.registry.search;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;

/** Lối vào cho service: điều kiện SQL của các giá trị {@code attr} theo mục tuỳ biến của không gian. */
@Component
public class AttributeFilter {

	/** Tên tham số trên URL. */
	public static final String PARAM = "attr";

	private final FieldDefService fieldDefs;

	AttributeFilter(FieldDefService fieldDefs) {
		this.fieldDefs = fieldDefs;
	}

	/**
	 * Giá trị {@code attr} đúng như client gửi. Không nhận qua {@code @RequestParam List<String>}: khi
	 * chỉ có một giá trị, Spring tách nó ở dấu phẩy, và "ghi chú chứa 'Huế, bờ sông'" thành hai điều kiện.
	 */
	public static List<String> values(HttpServletRequest request) {
		String[] values = request.getParameterValues(PARAM);
		return (values != null) ? List.of(values) : List.of();
	}

	/**
	 * @param typeColumn cột loại của bảng, xem {@link AttributeConditions#toSql}
	 * @throws com.home_platform.common.web.BadRequestException nếu {@code attr} sai
	 */
	public List<SqlCondition> conditions(long workspaceId, FieldTarget target, List<String> attr, String typeColumn) {
		List<AttributeCondition> parsed = AttributeConditions.parse(attr);
		if (parsed.isEmpty()) {
			return List.of();
		}
		List<FieldDef> defs = this.fieldDefs.listAll(workspaceId).stream().filter(def -> def.target() == target).toList();
		return AttributeConditions.toSql(parsed, defs, typeColumn);
	}

}
