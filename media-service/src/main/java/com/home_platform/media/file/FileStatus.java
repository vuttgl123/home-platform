package com.home_platform.media.file;

/**
 * Vòng đời của một file: tạo dòng ở {@code PENDING}, đưa file vào kho, đọc kích thước ảnh, rồi
 * chuyển sang {@code READY}. Hỏng giữa chừng thì dòng nằm ở {@code PENDING} và được dọn sau.
 */
public enum FileStatus {

	PENDING, READY, DELETED

}
