package com.home_platform.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Yêu cầu mâu thuẫn với trạng thái hiện tại của bản ghi (HTTP 409), ví dụ bản ghi đã bị
 * người khác sửa kể từ lúc đọc.
 */
public class ConflictException extends ErrorResponseException {

	/** Tên thuộc tính chứa mã máy đọc của lỗi, nếu có. */
	public static final String CODE_PROPERTY = "code";

	/** Mã của lỗi "bản ghi đã được người khác sửa": client đọc lại rồi thử lại. */
	public static final String VERSION_CONFLICT = "VERSION_CONFLICT";

	/**
	 * Mã của 409 khi không lấy được khoá dòng: lệnh khác (ví dụ một lần nhập Excel) đang giữ dữ liệu
	 * đó. Client thử lại sau là được; không cần đọc lại như với {@link #VERSION_CONFLICT}.
	 */
	public static final String BUSY = "BUSY";

	public ConflictException(String detail) {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail), null);
	}

	/**
	 * Bản ghi đã đổi {@code version} kể từ lúc client đọc. Kèm thuộc tính {@code code} là
	 * {@value #VERSION_CONFLICT}, để client nhận ra mà không phải đọc câu chữ của {@code detail}.
	 * @param entityName tên loại bản ghi trong thông báo, ví dụ {@code Place}
	 */
	public static ConflictException versionConflict(String entityName, long id) {
		ConflictException ex = new ConflictException(entityName + " " + id + " was changed by someone else");
		ex.getBody().setProperty(CODE_PROPERTY, VERSION_CONFLICT);
		return ex;
	}

}
