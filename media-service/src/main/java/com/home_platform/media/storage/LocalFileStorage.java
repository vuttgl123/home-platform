package com.home_platform.media.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Kho là một thư mục trên đĩa. Khoá {@code K} là file {@code <root>/K.bin}; nhờ đuôi {@code .bin},
 * {@code files/9.bin} và thư mục {@code files/9/} (ảnh thu nhỏ {@code files/9/thumb.bin}) cùng tồn
 * tại được. Ghi vào file tạm cùng thư mục rồi đổi tên, nên không ai đọc được file ghi dở.
 */
public class LocalFileStorage implements FileStorage {

	private final Path root;

	public LocalFileStorage(Path root) {
		this.root = root.toAbsolutePath().normalize();
	}

	@Override
	public StoredObject put(String key, InputStream content, long sizeBytes, String contentType) {
		Path target = pathOf(key);
		try {
			Files.createDirectories(target.getParent());
			Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
			try {
				Files.copy(content, temp, StandardCopyOption.REPLACE_EXISTING);
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			finally {
				Files.deleteIfExists(temp);
			}
			return new StoredObject(key, Files.size(target), contentType);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not store " + key, ex);
		}
	}

	@Override
	public InputStream open(String key) {
		try {
			return Files.newInputStream(pathOf(key));
		}
		catch (NoSuchFileException ex) {
			throw new StoredObjectNotFoundException(key);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not open " + key, ex);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(pathOf(key));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not delete " + key, ex);
		}
	}

	/**
	 * Đường dẫn của khoá, luôn nằm trong {@code root}.
	 * @throws IllegalArgumentException nếu khoá có {@code ..}, {@code \} hay bắt đầu bằng {@code /}
	 */
	private Path pathOf(String key) {
		if (key.isEmpty() || key.startsWith("/") || key.contains("\\") || key.contains("..")) {
			throw new IllegalArgumentException("Invalid storage key: " + key);
		}
		Path path = this.root.resolve(key + ".bin").normalize();
		if (!path.startsWith(this.root)) {
			throw new IllegalArgumentException("Invalid storage key: " + key);
		}
		return path;
	}

}
