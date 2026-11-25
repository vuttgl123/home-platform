package com.home_platform.media.attachment;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Thứ tự mới của mọi ảnh đang hiện của một bản ghi (spec Đợt 7, M7).
 *
 * @param attachmentIds đủ và đúng các ảnh đang hiện, theo thứ tự mới
 */
public record OrderRequest(@NotNull OwnerType ownerType, @NotNull Long ownerId, @NotNull List<Long> attachmentIds) {
}
