package com.home_platform.media.storage;

/**
 * Cách đặt tên file trong kho: nhóm theo không gian, để sau này xoá hoặc chuyển dữ liệu của một
 * không gian là thao tác trên một tiền tố.
 */
public final class StorageKeys {

	private StorageKeys() {
	}

	public static String forFile(long workspaceId, long fileId) {
		return "workspaces/" + workspaceId + "/files/" + fileId;
	}

}
