package com.home_platform.common.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Mọi lỗi của API trả về cùng một dạng: Problem Details (RFC 9457). Lớp cha lo các lỗi của
 * Spring MVC và các exception của platform như {@link NotFoundException}.
 *
 * <p>Lỗi kiểm tra dữ liệu có thêm thuộc tính {@code errors}: mỗi phần tử gồm {@code field}
 * (tên trường hoặc tham số; {@code null} nếu lỗi thuộc về cả đối tượng) và {@code message}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** Tên thuộc tính chứa danh sách lỗi theo từng trường. */
	public static final String ERRORS_PROPERTY = "errors";

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, Object>> errors = new ArrayList<>();
		for (ObjectError error : ex.getBindingResult().getAllErrors()) {
			String field = (error instanceof FieldError fieldError) ? fieldError.getField() : null;
			errors.add(error(field, error.getDefaultMessage()));
		}
		ex.getBody().setProperty(ERRORS_PROPERTY, errors);
		return super.handleMethodArgumentNotValid(ex, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, Object>> errors = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result -> {
			String parameter = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(error -> {
				// Tham số là một đối tượng: lỗi nằm ở trường bên trong nó.
				String field = (error instanceof FieldError fieldError) ? fieldError.getField() : parameter;
				errors.add(error(field, error.getDefaultMessage()));
			});
		});
		ex.getBody().setProperty(ERRORS_PROPERTY, errors);
		return super.handleHandlerMethodValidationException(ex, headers, status, request);
	}

	/**
	 * Trùng khoá duy nhất trong database. Thông báo của database chứa tên index và giá trị,
	 * nên chỉ ghi vào log; client nhận một thông báo chung.
	 */
	@ExceptionHandler(DuplicateKeyException.class)
	ProblemDetail handleDuplicateKey(DuplicateKeyException ex) {
		log.warn("Duplicate key: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Duplicate value");
	}

	/**
	 * Lỗi không lường trước: ghi đầy đủ vào log, chỉ trả cho client một thông báo chung để
	 * không lộ chi tiết bên trong.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
	}

	private static Map<String, Object> error(String field, String message) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("field", field);
		error.put("message", message);
		return error;
	}

}
