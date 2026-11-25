package com.home_platform.media.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** Đọc ảnh tải lên (spec Đợt 7, M1 tới M3). */
class ImageInspectorTests {

	private final ImageInspector inspector = new ImageInspector();

	@Test
	void readsJpegPngWebp() throws IOException {
		assertThat(this.inspector.inspect(image(1000, 500, "jpg"), "image/jpeg")).satisfies((image) -> {
			assertThat(image.contentType()).isEqualTo("image/jpeg");
			assertThat(image.width()).isEqualTo(1000);
			assertThat(image.height()).isEqualTo(500);
		});
		assertThat(this.inspector.inspect(image(30, 20, "png"), "image/png")).satisfies((image) -> {
			assertThat(image.contentType()).isEqualTo("image/png");
			assertThat(image.width()).isEqualTo(30);
			assertThat(image.height()).isEqualTo(20);
		});
		assertThat(this.inspector.inspect(fixture("small.webp"), "image/webp")).satisfies((image) -> {
			assertThat(image.contentType()).isEqualTo("image/webp");
			assertThat(image.width()).isEqualTo(20);
			assertThat(image.height()).isEqualTo(10);
		});
	}

	@Test
	void exifRotationIsApplied() throws IOException {
		// Ảnh lưu 64×32 (nửa trái đỏ, nửa phải xanh), EXIF Orientation 6: hiển thị 32×64, nửa trên đỏ.
		InspectedImage image = this.inspector.inspect(fixture("portrait-exif6.jpg"), "image/jpeg");
		assertThat(image.width()).isEqualTo(32);
		assertThat(image.height()).isEqualTo(64);
		BufferedImage thumbnail = decode(image.thumbnailJpeg());
		assertThat(thumbnail.getWidth()).isEqualTo(32);
		assertThat(thumbnail.getHeight()).isEqualTo(64);
		assertThat(new Color(thumbnail.getRGB(16, 8))).satisfies((top) -> {
			assertThat(top.getRed()).isGreaterThan(200);
			assertThat(top.getBlue()).isLessThan(60);
		});
		assertThat(new Color(thumbnail.getRGB(16, 56))).satisfies((bottom) -> {
			assertThat(bottom.getBlue()).isGreaterThan(200);
			assertThat(bottom.getRed()).isLessThan(60);
		});
	}

	@Test
	void thumbnailFitsIn480() throws IOException {
		BufferedImage wide = decode(this.inspector.inspect(image(1000, 500, "jpg"), "image/jpeg").thumbnailJpeg());
		assertThat(wide.getWidth()).isEqualTo(480);
		assertThat(wide.getHeight()).isEqualTo(240);
		BufferedImage small = decode(this.inspector.inspect(image(300, 200, "png"), "image/png").thumbnailJpeg());
		assertThat(small.getWidth()).isEqualTo(300);
		assertThat(small.getHeight()).isEqualTo(200);
	}

	@Test
	void transparencyBecomesWhite() throws IOException {
		BufferedImage thumbnail = decode(this.inspector.inspect(fixture("transparent.png"), "image/png").thumbnailJpeg());
		Color pixel = new Color(thumbnail.getRGB(5, 5));
		assertThat(pixel.getRed()).isGreaterThanOrEqualTo(250);
		assertThat(pixel.getGreen()).isGreaterThanOrEqualTo(250);
		assertThat(pixel.getBlue()).isGreaterThanOrEqualTo(250);
	}

	@Test
	void textIsUnreadable() {
		byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> this.inspector.inspect(text, "image/jpeg")).isInstanceOf(UnreadableImageException.class);
	}

	@Test
	void mismatchedTypeIsUnreadable() throws IOException {
		byte[] png = image(10, 10, "png");
		assertThatThrownBy(() -> this.inspector.inspect(png, "image/jpeg")).isInstanceOf(UnreadableImageException.class);
	}

	/** Ảnh một màu xám, mã hoá bằng ImageIO ({@code jpg} hay {@code png}). */
	static byte[] image(int width, int height, String format) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		var graphics = image.createGraphics();
		graphics.setColor(new Color(90, 120, 150));
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, format, out);
		return out.toByteArray();
	}

	static byte[] fixture(String name) throws IOException {
		try (InputStream in = ImageInspectorTests.class.getResourceAsStream("/images/" + name)) {
			return in.readAllBytes();
		}
	}

	private static BufferedImage decode(byte[] jpeg) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(jpeg));
	}

}
