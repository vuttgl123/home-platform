package com.home_platform.registry.imports;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.home_platform.common.web.ApiExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.web.ErrorResponseException;

/**
 * Dịch lỗi của lệnh tạo (bằng tiếng Anh, cho API) thành lời nhắn tiếng Việt cho từng dòng. Lỗi chưa
 * có trong bảng vẫn hiện, kèm chi tiết gốc, để không lỗi nào bị nuốt.
 */
final class ImportMessages {

	static final String REQUIRED = "Bắt buộc";

	private static final DateTimeFormatter VIETNAMESE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final List<Rule> RULES = List.of(
			rule(".* already has this document", "Số giấy tờ", m -> "Đã có người dùng giấy tờ này"),
			rule("idType and idNumber must be given together", "Loại giấy tờ",
					m -> "Loại giấy tờ và Số giấy tờ phải ghi cùng nhau"),
			rule(".* already has an overlapping occupancy at this place", null,
					m -> "Người này đã có một lần ở chồng ngày tại địa điểm này"),
			rule(".* already has an overlapping ownership at this place", null,
					m -> "Người này đã có một lần sở hữu chồng ngày tại địa điểm này"),
			rule("Ownership shares of place \\d+ would total ([\\d.]+)% on (\\d{4}-\\d{2}-\\d{2})", "Tỷ lệ (%)",
					m -> "Tổng tỷ lệ sở hữu của địa điểm sẽ là " + m.group(1) + "% vào ngày "
							+ VIETNAMESE_DATE.format(LocalDate.parse(m.group(2)))),
			rule("endDate must not be before startDate", "Đến ngày", m -> "Đến ngày không được trước Từ ngày"),
			rule("Places can be nested at most (\\d+) levels deep", "Nằm trong",
					m -> "Cây địa điểm không được sâu quá " + m.group(1) + " cấp"),
			rule("removedAt must not be before placedAt", "Ngày gỡ", m -> "Ngày gỡ không được trước Ngày đặt"),
			rule("Restore person \\d+ first", "Chủ", m -> "Chủ của món đã bị xoá"),
			rule("Code (.+) is already in use(; try again)?", "Mã", m -> "Mã " + m.group(1) + " đã có"));

	private static final Map<String, String> ATTRIBUTE_MESSAGES = Map.of("is required", REQUIRED,
			"must be text of at most 1000 characters", "Dài quá 1000 ký tự", "must be a number", "Phải là số");

	private ImportMessages() {
	}

	/**
	 * @param columnOfField tiêu đề cột theo tên trường của request ({@code fullName}) hay khoá mục tuỳ
	 *     biến ({@code attributes.blood})
	 */
	static List<RowMessage> of(RuntimeException ex, Map<String, String> columnOfField) {
		if (ex instanceof CellError cell) {
			return List.of(RowMessage.error(cell.column(), cell.text()));
		}
		if (ex instanceof ConstraintViolationException violations) {
			return violations.getConstraintViolations().stream().map(v -> violation(v, columnOfField)).toList();
		}
		if (ex instanceof ErrorResponseException response) {
			List<RowMessage> fieldErrors = fieldErrors(response, columnOfField);
			if (!fieldErrors.isEmpty()) {
				return fieldErrors;
			}
			String detail = response.getBody().getDetail();
			return List.of(translate(detail));
		}
		return List.of(RowMessage.error(null, fallback(ex.getMessage())));
	}

	/** Một ràng buộc của request bị vi phạm, dịch theo loại ràng buộc. */
	static RowMessage violation(ConstraintViolation<?> violation, Map<String, String> columnOfField) {
		String column = columnOfField.get(violation.getPropertyPath().toString());
		Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
		String text = switch (annotation) {
			case NotBlank ignored -> REQUIRED;
			case NotNull ignored -> REQUIRED;
			case NotEmpty ignored -> REQUIRED;
			case Size size -> "Dài quá " + size.max() + " ký tự";
			case Min min -> "Từ " + min.value() + " trở lên";
			case Max max -> "Tối đa " + max.value();
			case DecimalMin min -> (min.inclusive() ? "Từ " : "Lớn hơn ") + min.value();
			case DecimalMax max -> "Tối đa " + max.value();
			case Digits digits -> "Tối đa " + digits.fraction() + " chữ số thập phân";
			case Email ignored -> "Email không hợp lệ";
			case PastOrPresent ignored -> "Không được sau hôm nay";
			default -> fallback(violation.getPropertyPath() + " " + violation.getMessage());
		};
		return RowMessage.error(column, text);
	}

	static String fallback(String detail) {
		return "Không nhập được dòng này (" + detail + ")";
	}

	private static RowMessage translate(String detail) {
		if (detail != null) {
			for (Rule rule : RULES) {
				Matcher matcher = rule.pattern().matcher(detail);
				if (matcher.matches()) {
					return RowMessage.error(rule.column(), rule.text().apply(matcher));
				}
			}
		}
		return RowMessage.error(null, fallback(detail));
	}

	/** Lỗi theo từng trường ({@code errors} của Problem Details), ví dụ của mục tuỳ biến. */
	private static List<RowMessage> fieldErrors(ErrorResponseException ex, Map<String, String> columnOfField) {
		Map<String, Object> properties = ex.getBody().getProperties();
		Object errors = (properties != null) ? properties.get(ApiExceptionHandler.ERRORS_PROPERTY) : null;
		List<RowMessage> messages = new ArrayList<>();
		if (errors instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof Map<?, ?> error && error.get("message") instanceof String message) {
					String field = (error.get("field") instanceof String name) ? name : null;
					String text = ATTRIBUTE_MESSAGES.get(message);
					messages.add(RowMessage.error((field != null) ? columnOfField.get(field) : null,
							(text != null) ? text : fallback(((field != null) ? field + " " : "") + message)));
				}
			}
		}
		return messages;
	}

	private static Rule rule(String regex, String column, Function<Matcher, String> text) {
		return new Rule(Pattern.compile(regex), column, text);
	}

	private record Rule(Pattern pattern, String column, Function<Matcher, String> text) {
	}

}
