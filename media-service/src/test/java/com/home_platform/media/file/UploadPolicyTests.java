package com.home_platform.media.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.home_platform.common.web.BadRequestException;
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
		assertThatThrownBy(() -> UploadPolicy.check("image/png", 10 * 1024 * 1024 + 1))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("at most 10 MB");
	}

}
