package com.home_platform.registry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.test.AbstractRepositoryIT;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Khung cho test của registry trên database thật: mỗi test chạy trong một transaction được
 * rollback, chỉ dùng {@code workspace_id} âm, và gọi API qua {@link MockMvc} trong cùng
 * transaction đó.
 */
@AutoConfigureMockMvc
public abstract class RegistryIT extends AbstractRepositoryIT {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	private CacheManager cacheManager;

	/** Dữ liệu của test trước đã rollback nhưng cache vẫn có thể còn giữ; xoá sạch trước mỗi test. */
	@BeforeEach
	void clearCaches() {
		this.cacheManager.getCacheNames().forEach(name -> this.cacheManager.getCache(name).clear());
	}

	/** Đặt mẫu mã của {@code WS_A} qua API; {@code typeId} 0 là mẫu chung của đích. */
	protected void codeRule(String target, long typeId, String prefix, int digits, boolean yearly) throws Exception {
		this.mvc.perform(asA(put("/code-rules/" + target + "/" + typeId)).contentType(MediaType.APPLICATION_JSON)
			.content("{\"prefix\": \"%s\", \"digits\": %d, \"yearly\": %s}".formatted(prefix, digits, yearly)))
			.andExpect(status().isOk());
	}

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_A}. */
	protected static MockHttpServletRequestBuilder asA(MockHttpServletRequestBuilder request) {
		return request.header("X-Workspace-Id", WS_A).header("X-User-Id", USER);
	}

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_B}. */
	protected static MockHttpServletRequestBuilder asB(MockHttpServletRequestBuilder request) {
		return request.header("X-Workspace-Id", WS_B).header("X-User-Id", USER);
	}

}
