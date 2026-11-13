package com.home_platform.registry.place;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.home_platform.common.jdbc.EntityMetadata;
import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Địa chỉ của địa điểm. Bảng có khoá {@code (workspace_id, place_id)} chứ không phải
 * {@code (workspace_id, id)}, nên SQL viết tay với danh sách cột và bộ đọc dòng của
 * {@link EntityMetadata}.
 */
@Repository
public class PlaceAddressRepository {

	private final JdbcClient jdbc;

	private final EntityMetadata<PlaceAddress> metadata;

	private final String upsert;

	PlaceAddressRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
		this.metadata = EntityMetadata.of(PlaceAddress.class, support.json());
		String values = this.metadata.components().stream().map(c -> ":" + c).collect(Collectors.joining(", "));
		String updates = this.metadata.components()
			.stream()
			.filter(c -> !List.of("workspaceId", "placeId", "createdAt").contains(c))
			.map(c -> this.metadata.column(c) + " = new." + this.metadata.column(c))
			.collect(Collectors.joining(", "));
		this.upsert = "INSERT INTO reg_place_address (" + this.metadata.selectList() + ") VALUES (" + values
				+ ") AS new ON DUPLICATE KEY UPDATE " + updates;
	}

	public Optional<PlaceAddress> find(long workspaceId, long placeId) {
		return this.jdbc
			.sql("SELECT " + this.metadata.selectList()
					+ " FROM reg_place_address WHERE workspace_id = :workspaceId AND place_id = :placeId")
			.param("workspaceId", workspaceId)
			.param("placeId", placeId)
			.query(this.metadata.rowMapper())
			.optional();
	}

	/** Địa chỉ của các địa điểm trong {@code placeIds}; địa điểm không có địa chỉ thì không có phần tử. */
	public List<PlaceAddress> findAll(long workspaceId, List<Long> placeIds) {
		if (placeIds.isEmpty()) {
			return List.of();
		}
		return this.jdbc
			.sql("SELECT " + this.metadata.selectList()
					+ " FROM reg_place_address WHERE workspace_id = :workspaceId AND place_id IN (:placeIds)")
			.param("workspaceId", workspaceId)
			.param("placeIds", placeIds)
			.query(this.metadata.rowMapper())
			.list();
	}

	/** Tạo hoặc thay địa chỉ; khi thay thì giữ {@code created_at} cũ. */
	public void upsert(long workspaceId, PlaceAddress address) {
		this.jdbc.sql(this.upsert).params(this.metadata.parameters(address.withWorkspaceId(workspaceId))).update();
	}

	public boolean delete(long workspaceId, long placeId) {
		return this.jdbc.sql("DELETE FROM reg_place_address WHERE workspace_id = :workspaceId AND place_id = :placeId")
			.param("workspaceId", workspaceId)
			.param("placeId", placeId)
			.update() > 0;
	}

}
