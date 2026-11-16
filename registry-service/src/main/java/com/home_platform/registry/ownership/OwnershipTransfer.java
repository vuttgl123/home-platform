package com.home_platform.registry.ownership;

/** Kết quả chuyển nhượng: dòng của chủ cũ vừa kết thúc, dòng của người nhận vừa bắt đầu. */
public record OwnershipTransfer(Ownership ended, Ownership started) {
}
