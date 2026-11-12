package com.home_platform.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Yêu cầu mâu thuẫn với trạng thái hiện tại của bản ghi (HTTP 409), ví dụ bản ghi đã bị
 * người khác sửa kể từ lúc đọc.
 */
public class ConflictException extends ErrorResponseException {

	public ConflictException(String detail) {
		super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail), null);
	}

}
