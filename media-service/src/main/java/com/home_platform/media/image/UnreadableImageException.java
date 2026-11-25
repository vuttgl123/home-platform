package com.home_platform.media.image;

/** Nội dung không giải mã được, hay định dạng thật khác định dạng khai báo. */
public class UnreadableImageException extends RuntimeException {

	public UnreadableImageException(String message) {
		super(message);
	}

	public UnreadableImageException(String message, Throwable cause) {
		super(message, cause);
	}

}
