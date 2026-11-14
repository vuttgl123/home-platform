package com.home_platform.registry.item;

/**
 * Kết quả của việc thay một món đồ.
 *
 * @param removed món cũ, đã gỡ và trỏ tới món mới
 * @param replacement món mới
 */
public record ItemReplacement(Item removed, Item replacement) {
}
