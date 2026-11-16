package com.home_platform.registry.trash;

import java.time.Instant;

public record TrashResponse(TrashType entityType, long id, String name, String code, String context, Instant deletedAt,
		long deletedBy, int version) {

	static TrashResponse of(TrashEntry entry) {
		return new TrashResponse(entry.entityType(), entry.id(), entry.name(), entry.code(), entry.context(),
				entry.deletedAt(), entry.deletedBy(), entry.version());
	}

}
