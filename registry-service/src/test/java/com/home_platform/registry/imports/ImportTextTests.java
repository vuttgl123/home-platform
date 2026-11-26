package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;

import org.junit.jupiter.api.Test;

class ImportTextTests {

	@Test
	void nonBreakingAndRepeatedSpacesAreCleaned() {
		assertThat(ImportText.clean("Toà A ")).isEqualTo("Toà A");
		assertThat(ImportText.clean(" Nguyễn  An ")).isEqualTo("Nguyễn An");
		assertThat(ImportText.clean("Nguyễn\tAn")).isEqualTo("Nguyễn An");
		assertThat(ImportText.clean("   ")).isNull();
		assertThat(ImportText.clean(null)).isNull();
	}

	@Test
	void cleanComposesVietnamese() {
		String decomposed = Normalizer.normalize("Nguyễn An", Normalizer.Form.NFD);

		assertThat(decomposed).isNotEqualTo("Nguyễn An");
		assertThat(ImportText.clean(decomposed)).isEqualTo("Nguyễn An");
	}

	@Test
	void keyFoldsCaseAndComposedForms() {
		String decomposed = Normalizer.normalize("Nguyễn An", Normalizer.Form.NFD);

		assertThat(ImportText.key("NGUYỄN AN")).isEqualTo(ImportText.key(decomposed)).isEqualTo("nguyễn an");
	}

	@Test
	void codeKeyIgnoresAccents() {
		assertThat(ImportText.codeKey("Phòng-Đ1")).isEqualTo(ImportText.codeKey("phong-d1")).isEqualTo("phong-d1");
	}

}
