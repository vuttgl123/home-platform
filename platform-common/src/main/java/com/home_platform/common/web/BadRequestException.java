package com.home_platform.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Dữ liệu vào sai mà code tự phát hiện (HTTP 400), ví dụ cursor hỏng hoặc thiếu header.
 */
public class BadRequestException extends ErrorResponseException {

	public BadRequestException(String detail) {
		super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
	}

}
