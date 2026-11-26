package com.home_platform.registry.imports;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Điều cần biết về file xlsx trước khi đọc nó: tổng dung lượng giải nén (chặn "bom zip"), có phần định
 * dạng ({@code xl/styles.xml}) không, và workbook dùng hệ ngày nào. Quét qua mục lục của zip (file
 * xlsx ghi dạng luồng chỉ đọc đúng được như vậy), nên ghi tạm ra đĩa.
 *
 * @param hasStyles file có {@code xl/styles.xml}; không có thì không có ô ngày
 * @param date1904 workbook dùng hệ ngày 1904 (Excel trên Mac cũ)
 */
record ZipFacts(boolean hasStyles, boolean date1904) {

	static final String TOO_BIG_UNZIPPED = "File quá lớn khi giải nén; hãy lưu lại file chỉ gồm dữ liệu cần nhập";

	private static final Pattern DATE_1904 = Pattern.compile("date1904\\s*=\\s*\"(1|true)\"");

	/**
	 * @throws ImportFileException nếu không phải zip, hoặc giải nén ra hơn {@code maxUnzippedBytes}
	 */
	static ZipFacts scan(byte[] file, long maxUnzippedBytes) {
		Path temp = null;
		try {
			temp = Files.createTempFile("import", ".xlsx");
			Files.write(temp, file);
			try (ZipFile zip = new ZipFile(temp.toFile())) {
				long total = 0;
				boolean styles = false;
				boolean date1904 = false;
				byte[] buffer = new byte[1 << 16];
				for (ZipEntry entry : Collections.list(zip.entries())) {
					styles |= entry.getName().equals("xl/styles.xml");
					boolean workbook = entry.getName().equals("xl/workbook.xml");
					StringBuilder xml = workbook ? new StringBuilder() : null;
					try (InputStream in = zip.getInputStream(entry)) {
						for (int read = in.read(buffer); read >= 0; read = in.read(buffer)) {
							total += read;
							if (total > maxUnzippedBytes) {
								throw new ImportFileException(TOO_BIG_UNZIPPED);
							}
							if (xml != null) {
								xml.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
							}
						}
					}
					if (xml != null) {
						date1904 = DATE_1904.matcher(xml).find();
					}
				}
				return new ZipFacts(styles, date1904);
			}
		}
		catch (IOException | RuntimeException ex) {
			if (ex instanceof ImportFileException importError) {
				throw importError;
			}
			throw new ImportFileException(WorkbookReader.UNREADABLE);
		}
		finally {
			if (temp != null) {
				try {
					Files.deleteIfExists(temp);
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
		}
	}

}
