package com.home_platform.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Bản ghi được yêu cầu không tồn tại (HTTP 404).
 */
public class NotFoundException extends ErrorResponseException {

	public NotFoundException(String detail) {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail), null);
	}

	/** Thông báo theo mẫu quen thuộc: {@code of("Place", 42)} cho ra "Place 42 not found". */
	public static NotFoundException of(String what, long id) {
		return new NotFoundException(what + " " + id + " not found");
	}

}
