package com.home_platform.media.storage;

/** Một file đã nằm trong kho. */
public record StoredObject(String key, long sizeBytes, String contentType) {
}
