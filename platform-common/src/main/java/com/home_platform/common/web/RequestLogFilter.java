package com.home_platform.common.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gắn cho mỗi request một mã, để mọi dòng log của request đó (và lỗi 500 trả cho client) lần
 * ngược được về nhau.
 *
 * <p>Mã lấy từ header {@value #HEADER} nếu client gửi một giá trị đơn giản (chữ, số, {@code .},
 * {@code _}, {@code -}, tối đa 64 ký tự), ngược lại được sinh mới; nó được trả lại trong cùng
 * header. Mã nằm trong log context dưới khoá {@value #REQUEST_ID}; không gian và người gọi được
 * {@code ContextArgumentResolver} thêm vào dưới {@value #WORKSPACE_ID} và {@value #USER_ID}. Khi
 * request xong, filter ghi một dòng tóm tắt (trừ {@code /actuator}) rồi xoá cả ba khoá, vì luồng
 * xử lý được dùng lại cho request khác.
 */
public class RequestLogFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";

	public static final String REQUEST_ID = "requestId";

	public static final String WORKSPACE_ID = "workspaceId";

	public static final String USER_ID = "userId";

	private static final Pattern PLAIN_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String sent = request.getHeader(HEADER);
		String requestId = (sent != null && PLAIN_ID.matcher(sent).matches()) ? sent : UUID.randomUUID().toString();
		MDC.put(REQUEST_ID, requestId);
		response.setHeader(HEADER, requestId);
		long started = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			if (!request.getRequestURI().startsWith("/actuator")) {
				log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(),
						(System.nanoTime() - started) / 1_000_000);
			}
			MDC.remove(REQUEST_ID);
			MDC.remove(WORKSPACE_ID);
			MDC.remove(USER_ID);
		}
	}

}
