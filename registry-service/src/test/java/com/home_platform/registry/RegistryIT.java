package com.home_platform.registry;

import com.home_platform.common.test.AbstractRepositoryIT;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
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

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_A}. */
	protected static MockHttpServletRequestBuilder asA(MockHttpServletRequestBuilder request) {
		return request.header("X-Workspace-Id", WS_A).header("X-User-Id", USER);
	}

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_B}. */
	protected static MockHttpServletRequestBuilder asB(MockHttpServletRequestBuilder request) {
		return request.header("X-Workspace-Id", WS_B).header("X-User-Id", USER);
	}

}
