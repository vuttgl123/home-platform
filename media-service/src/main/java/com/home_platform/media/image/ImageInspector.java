package com.home_platform.media.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import org.springframework.stereotype.Component;

/**
 * Đọc một ảnh tải lên (spec Đợt 7, M1 tới M3): giải mã thật để chắc đó là ảnh đúng định dạng khai
 * báo, xoay theo EXIF Orientation của JPEG, và tạo ảnh thu nhỏ JPEG nền trắng.
 */
@Component
public class ImageInspector {

	/** Cạnh dài nhất của ảnh thu nhỏ; ảnh nhỏ hơn không bị phóng to. */
	static final int THUMBNAIL_EDGE = 480;

	/** Chất lượng JPEG của ảnh thu nhỏ. */
	static final float THUMBNAIL_QUALITY = 0.85f;

	/** Ảnh nhiều điểm ảnh hơn mức này bị từ chối, để một file nhỏ không chiếm hết bộ nhớ khi giải mã. */
	static final long MAX_PIXELS = 100_000_000L;

	/** Kiểu khai báo và tên định dạng mà reader của ImageIO báo (chữ thường). */
	private static final Map<String, String> FORMATS = Map.of("image/jpeg", "jpeg", "image/png", "png", "image/webp",
			"webp");

	/**
	 * @throws UnreadableImageException nếu không giải mã được, định dạng thật khác {@code declaredType},
	 *   hay ảnh quá {@value #MAX_PIXELS} điểm ảnh
	 */
	public InspectedImage inspect(byte[] content, String declaredType) {
		String expected = FORMATS.get(declaredType);
		if (expected == null) {
			throw new UnreadableImageException("Unsupported content type " + declaredType);
		}
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				throw new UnreadableImageException("Content is not an image");
			}
			ImageReader reader = readers.next();
			try {
				String format = reader.getFormatName().toLowerCase(Locale.ROOT);
				if (!format.equals(expected) && !(expected.equals("jpeg") && format.equals("jpg"))) {
					throw new UnreadableImageException("Content is " + format + ", not " + declaredType);
				}
				reader.setInput(in, true, true);
				return read(reader, (expected.equals("jpeg") ? orientation(content) : 1), declaredType);
			}
			finally {
				reader.dispose();
			}
		}
		catch (UnreadableImageException ex) {
			throw ex;
		}
		catch (IOException | RuntimeException ex) {
			throw new UnreadableImageException("Content could not be decoded as " + declaredType, ex);
		}
	}

	private InspectedImage read(ImageReader reader, int orientation, String contentType) throws IOException {
		int storedWidth = reader.getWidth(0);
		int storedHeight = reader.getHeight(0);
		if ((long) storedWidth * storedHeight > MAX_PIXELS) {
			throw new UnreadableImageException("Image has more than " + MAX_PIXELS + " pixels");
		}
		boolean turned = orientation >= 5 && orientation <= 8;
		int width = turned ? storedHeight : storedWidth;
		int height = turned ? storedWidth : storedHeight;
		// Chỉ cần đủ chi tiết cho ảnh thu nhỏ: giải mã thưa với ảnh lớn hơn hai lần cạnh ảnh thu nhỏ.
		ImageReadParam param = reader.getDefaultReadParam();
		int step = Math.max(1, Math.max(storedWidth, storedHeight) / (THUMBNAIL_EDGE * 2));
		param.setSourceSubsampling(step, step, 0, 0);
		BufferedImage upright = orient(reader.read(0, param), orientation);
		return new InspectedImage(contentType, width, height, thumbnail(upright, width, height));
	}

	/** Giá trị EXIF Orientation (1 tới 8) của một JPEG; không có hay đọc lỗi thì 1. */
	private static int orientation(byte[] jpeg) {
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(jpeg))) {
			List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(in, JPEG.APP1, "Exif");
			if (segments.isEmpty()) {
				return 1;
			}
			try (InputStream data = segments.get(0).data()) {
				// Sau "Exif" là hai byte 0; reader đã bỏ byte đầu cùng tên, còn một byte đệm.
				data.read();
				try (ImageInputStream exif = ImageIO.createImageInputStream(data)) {
					Directory directory = new TIFFReader().read(exif);
					Entry entry = directory.getEntryById(TIFF.TAG_ORIENTATION);
					int value = (entry != null && entry.getValue() instanceof Number number) ? number.intValue() : 1;
					return (value >= 1 && value <= 8) ? value : 1;
				}
			}
		}
		catch (IOException | RuntimeException ex) {
			return 1;
		}
	}

	/**
	 * Xoay, lật ảnh theo EXIF Orientation để thành chiều hiển thị. Mỗi giá trị là một phép biến đổi
	 * {@code (x, y) → (x', y')} trên ảnh {@code w × h}.
	 */
	static BufferedImage orient(BufferedImage source, int orientation) {
		if (orientation < 2 || orientation > 8) {
			return source;
		}
		int w = source.getWidth();
		int h = source.getHeight();
		AffineTransform transform = switch (orientation) {
			case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0); // lật ngang
			case 3 -> new AffineTransform(-1, 0, 0, -1, w, h); // xoay 180°
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, h); // lật dọc
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0); // đổi trục x, y
			case 6 -> new AffineTransform(0, 1, -1, 0, h, 0); // xoay 90° theo chiều kim đồng hồ
			case 7 -> new AffineTransform(0, -1, -1, 0, h, w); // đổi trục rồi xoay 180°
			default -> new AffineTransform(0, -1, 1, 0, 0, w); // 8: xoay 90° ngược chiều kim đồng hồ
		};
		boolean turned = orientation >= 5;
		BufferedImage target = new BufferedImage(turned ? h : w, turned ? w : h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.drawImage(source, transform, null);
		}
		finally {
			graphics.dispose();
		}
		return target;
	}

	/** Ảnh thu nhỏ JPEG: vừa {@value #THUMBNAIL_EDGE} px, không phóng to, phần trong suốt thành nền trắng. */
	private static byte[] thumbnail(BufferedImage upright, int width, int height) throws IOException {
		double scale = Math.min(1.0, (double) THUMBNAIL_EDGE / Math.max(width, height));
		int targetWidth = Math.max(1, (int) Math.round(width * scale));
		int targetHeight = Math.max(1, (int) Math.round(height * scale));
		BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, targetWidth, targetHeight);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(upright, 0, 0, targetWidth, targetHeight, null);
		}
		finally {
			graphics.dispose();
		}
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(THUMBNAIL_QUALITY);
			writer.setOutput(out);
			writer.write(null, new IIOImage(target, null, null), param);
		}
		finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

}
