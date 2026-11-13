package com.home_platform.common.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu một thành phần của record được lưu trong cột kiểu {@code JSON}. Kiểu Java có thể
 * là bất kỳ kiểu nào Jackson đọc ghi được, ví dụ {@code Map<String, Object>}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface JsonColumn {

}
