package com.home_platform.media.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.home_platform.common.web.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;

class UploadPolicyTests {

	@Test
	void acceptsJpegPngAndWebpUpToTenMegabytes() {
		assertThatCode(() -> UploadPolicy.check("image/jpeg", 1)).doesNotThrowAnyException();
		assertThatCode(() -> UploadPolicy.check("image/png", 10 * 1024 * 1024)).doesNotThrowAnyException();
		assertThatCode(() -> UploadPolicy.check("IMAGE/WEBP", 500)).doesNotThrowAnyException();
	}

	@Test
	void rejectsOtherTypes() {
		assertThatThrownBy(() -> UploadPolicy.check("image/gif", 10)).isInstanceOf(BadRequestException.class)
			.hasMessageContaining("Only JPEG, PNG and WebP images are accepted");
		assertThatThrownBy(() -> UploadPolicy.check(null, 10)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void rejectsEmptyAndTooLargeFiles() {
		assertThatThrownBy(() -> UploadPolicy.check("image/png", 0)).isInstanceOf(BadRequestException.class);
		// Quá cỡ là 413, như khi chính server chặn một request multipart quá lớn.
		assertThatThrownBy(() -> UploadPolicy.check("image/png", 10 * 1024 * 1024 + 1))
			.isInstanceOfSatisfying(ResponseStatusException.class,
					(ex) -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE))
			.hasMessageContaining("at most 10 MB");
	}

}
