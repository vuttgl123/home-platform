package com.home_platform.media.storage;

/** Không có nội dung nào ở khoá này trong kho. */
public class StoredObjectNotFoundException extends RuntimeException {

	public StoredObjectNotFoundException(String key) {
		super("No stored object at " + key);
	}

}
