package com.home_platform.registry.export;

import java.time.LocalDate;

/** Mốc đọc tiếp của danh sách cư trú và sở hữu: dòng cuối của lô trước, theo đúng thứ tự sắp. */
public record StayCursor(String placePath, LocalDate startDate, long id) {
}
