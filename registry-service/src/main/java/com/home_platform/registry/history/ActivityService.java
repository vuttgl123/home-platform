package com.home_platform.registry.history;

import com.home_platform.common.history.EntityChange;
import com.home_platform.common.history.EntityHistory;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.catalog.Text;
import org.springframework.stereotype.Service;

/** Hoạt động gần đây của một không gian; cũng là chỗ người dùng tìm lại thứ đã xoá để khôi phục. */
@Service
public class ActivityService {

	private final EntityHistory history;

	ActivityService(EntityHistory history) {
		this.history = history;
	}

	/** @param entityType PLACE, PERSON, ITEM; {@code null} là mọi loại */
	public CursorPage<EntityChange> list(long workspaceId, String entityType, CursorRequest page) {
		return this.history.listRecent(workspaceId, Text.trimToNull(entityType), page);
	}

}
