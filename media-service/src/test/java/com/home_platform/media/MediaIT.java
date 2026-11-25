package com.home_platform.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.home_platform.common.test.AbstractRepositoryIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/**
 * Khung cho test của media trên database thật: mỗi test chạy trong một transaction được rollback,
 * chỉ dùng {@code workspace_id} âm. Kho file là một thư mục tạm dùng chung cho cả lần chạy test;
 * kho không rollback được, nên test so số file trước và sau thay vì đòi kho rỗng.
 */
@AutoConfigureMockMvc
public abstract class MediaIT extends AbstractRepositoryIT {

	/** Thư mục kho của mọi test media trong lần chạy này. */
	protected static final Path STORAGE = createStorage();

	@Autowired
	protected MockMvc mvc;

	@DynamicPropertySource
	static void storageRoot(DynamicPropertyRegistry registry) {
		registry.add("media.storage.root", STORAGE::toString);
	}

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_A}. */
	protected static <B extends AbstractMockHttpServletRequestBuilder<B>> B asA(B request) {
		return request.header("X-Workspace-Id", WS_A).header("X-User-Id", USER);
	}

	/** Request gửi với tư cách {@code USER} trong không gian {@code WS_B}. */
	protected static <B extends AbstractMockHttpServletRequestBuilder<B>> B asB(B request) {
		return request.header("X-Workspace-Id", WS_B).header("X-User-Id", USER);
	}

	/** Số file đang có trong kho. */
	protected static long storedFiles() {
		try (Stream<Path> files = Files.walk(STORAGE)) {
			return files.filter(Files::isRegularFile).count();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Path createStorage() {
		try {
			return Files.createTempDirectory("media-it-");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
