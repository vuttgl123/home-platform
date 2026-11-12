package com.home_platform.common.id;

/**
 * Sinh giá trị cho cột {@code id}: số dương, không trùng và tăng dần theo thời gian.
 */
public interface IdGenerator {

	long nextId();

}
