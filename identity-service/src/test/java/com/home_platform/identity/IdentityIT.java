package com.home_platform.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import com.home_platform.common.test.AbstractRepositoryIT;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Khung cho test của identity trên database thật: mỗi test rollback, email đuôi
 * {@code @example.invalid}, gọi API qua {@link MockMvc} trong cùng transaction.
 */
@AutoConfigureMockMvc
public abstract class IdentityIT extends AbstractRepositoryIT {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	@Autowired
	protected MockMvc mvc;

	/** Một email chưa ai dùng trong lần chạy test này. */
	protected static String email() {
		return "user" + System.nanoTime() + "-" + SEQUENCE.incrementAndGet() + "@example.invalid";
	}

	/** Tạo người dùng qua API, trả {@code id}. */
	protected long user(String displayName) throws Exception {
		String body = this.mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "displayName": "%s"}""".formatted(email(), displayName)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return Long.parseLong(JsonPath.read(body, "$.id"));
	}

	/** Request gửi với tư cách người dùng {@code userId}. */
	protected static MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
		return request.header("X-User-Id", userId);
	}

}
