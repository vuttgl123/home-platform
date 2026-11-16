package com.home_platform.registry.code;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.item.ItemService;
import com.home_platform.registry.place.PlaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cấp mã cho bản ghi có từ trước khi có mã tự cấp, từng lô: địa điểm trước, rồi đồ đạc; trong mỗi
 * loại, cũ trước. Lô nhỏ để bộ đếm không bị giữ lâu.
 */
@Service
public class CodeBackfillService {

	static final int BATCH = 100;

	private final PlaceService places;

	private final ItemService items;

	CodeBackfillService(PlaceService places, ItemService items) {
		this.places = places;
		this.items = items;
	}

	public CodeStatusResponse status(long workspaceId) {
		return new CodeStatusResponse(this.places.countWithoutCode(workspaceId),
				this.items.countWithoutCode(workspaceId));
	}

	@Transactional
	public CodeBackfillResponse backfill(WorkspaceContext ctx) {
		int placesAssigned = this.places.assignMissingCodes(ctx, BATCH);
		int itemsAssigned = (placesAssigned < BATCH) ? this.items.assignMissingCodes(ctx, BATCH - placesAssigned) : 0;
		CodeStatusResponse remaining = status(ctx.workspaceId());
		return new CodeBackfillResponse(placesAssigned, itemsAssigned, remaining.placesWithoutCode(),
				remaining.itemsWithoutCode());
	}

}
