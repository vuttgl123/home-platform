package com.home_platform.registry.person;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.home_platform.common.jdbc.KeysetQuery;
import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.registry.search.SqlCondition;
import com.home_platform.registry.search.TextSearch;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository extends WorkspaceRepository<Person> {

	/**
	 * Các dòng cư trú hôm nay ({@code :today}) của người ở dòng ngoài, tại địa điểm chưa xoá, mọi quan
	 * hệ; dùng trong {@code EXISTS} nên một người ở hai nơi vẫn chỉ là một dòng.
	 */
	private static final String STAYS_TODAY = "SELECT 1 FROM reg_occupancy o JOIN reg_place pl "
			+ "ON pl.workspace_id = o.workspace_id AND pl.id = o.place_id "
			+ "WHERE o.workspace_id = reg_person.workspace_id AND o.person_id = reg_person.id "
			+ "AND o.deleted_at IS NULL AND pl.deleted_at IS NULL AND o.start_date <= :today "
			+ "AND (o.end_date IS NULL OR o.end_date >= :today)";

	PersonRepository(RepositorySupport support) {
		super(support, "reg_person", Person.class);
	}

	/** Tên của các người (kể cả đã xoá) theo {@code id}. */
	public Map<Long, String> names(long workspaceId, List<Long> ids) {
		Map<Long, String> names = new HashMap<>();
		if (ids.isEmpty()) {
			return names;
		}
		jdbc().sql("SELECT id, full_name FROM reg_person WHERE workspace_id = :workspaceId AND id IN (:ids)")
			.param("workspaceId", workspaceId)
			.param("ids", ids)
			.query((rs, rowNum) -> names.put(rs.getLong("id"), rs.getString("full_name")))
			.list();
		return names;
	}

	/** Dòng đang bị xoá mềm, mới xoá trước. Index (workspace_id, deleted_at). */
	public CursorPage<Person> deleted(long workspaceId, CursorRequest page) {
		return keyset().onlyDeleted().orderByDesc("deleted_at").fetch(workspaceId, page);
	}

	/**
	 * Người chưa xoá theo bộ lọc, theo tên (spec Đợt 11, S4). Không bộ lọc thì đi theo index
	 * (workspace_id, full_name); tìm chữ và mục tuỳ biến quét các dòng của không gian.
	 */
	public CursorPage<Person> list(long workspaceId, PersonFilter filter, LocalDate today, CursorRequest page) {
		KeysetQuery<Person> query = keyset();
		if (filter.text() != null) {
			SqlCondition text = TextSearch.anyColumnContains(filter.text(), "full_name", "phone", "email", "id_number",
					"note");
			query = query.where(text.sql(), text.params());
		}
		if (filter.rootPath() != null) {
			query = query.where("EXISTS (" + STAYS_TODAY + " AND pl.path LIKE :rootPrefix)",
					Map.of("today", today, "rootPrefix", filter.rootPath() + "%"));
		}
		else if (filter.living() != null) {
			query = query.where((filter.living() ? "EXISTS (" : "NOT EXISTS (") + STAYS_TODAY + ")",
					Map.of("today", today));
		}
		for (SqlCondition condition : filter.attributes()) {
			query = query.where(condition.sql(), condition.params());
		}
		return query.orderBy("full_name").fetch(workspaceId, page);
	}

	/** @param likePrefix mẫu từ {@code Text.likePrefix} */
	public CursorPage<Person> searchByName(long workspaceId, String likePrefix, CursorRequest page) {
		return keyset().where("full_name LIKE :namePrefix", Map.of("namePrefix", likePrefix))
			.orderBy("full_name")
			.fetch(workspaceId, page);
	}

	/** Số điện thoại bắt đầu bằng; theo số điện thoại. Index (workspace_id, phone). */
	public CursorPage<Person> searchByPhone(long workspaceId, String likePrefix, CursorRequest page) {
		return keyset().where("phone LIKE :phonePrefix", Map.of("phonePrefix", likePrefix))
			.orderBy("phone")
			.fetch(workspaceId, page);
	}

	/** Đúng số giấy tờ; theo {@code id}. Index (workspace_id, id_number). */
	public CursorPage<Person> findByIdNumber(long workspaceId, String idNumber, CursorRequest page) {
		return keyset().where("id_number = :idNumber", Map.of("idNumber", idNumber)).fetch(workspaceId, page);
	}

	/** Người chưa xoá mang giấy tờ này. Index (workspace_id, id_number). */
	public Optional<Person> findByDocument(long workspaceId, String idType, String idNumber) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_person WHERE workspace_id = :workspaceId "
				+ "AND id_number = :idNumber AND id_type = :idType AND deleted_at IS NULL LIMIT 1")
			.param("workspaceId", workspaceId)
			.param("idNumber", idNumber)
			.param("idType", idType)
			.query(rowMapper())
			.optional();
	}

	/** Số dòng sở hữu chưa xoá đang hoặc sắp có hiệu lực. Index (workspace_id, person_id, start_date). */
	public int countCurrentOwnerships(long workspaceId, long personId, LocalDate today) {
		return jdbc().sql("SELECT COUNT(*) FROM reg_ownership WHERE workspace_id = :workspaceId "
				+ "AND person_id = :personId AND deleted_at IS NULL AND (end_date IS NULL OR end_date >= :today)")
			.param("workspaceId", workspaceId)
			.param("personId", personId)
			.param("today", today)
			.query(Integer.class)
			.single();
	}

	/** Số món đồ chưa xoá, chưa gỡ mà người này là chủ. Index (workspace_id, owner_person_id, removed_at, name). */
	public int countOwnedItems(long workspaceId, long personId) {
		return jdbc().sql("SELECT COUNT(*) FROM reg_item WHERE workspace_id = :workspaceId "
				+ "AND owner_person_id = :personId AND removed_at IS NULL AND deleted_at IS NULL")
			.param("workspaceId", workspaceId)
			.param("personId", personId)
			.query(Integer.class)
			.single();
	}

	/** Số dòng cư trú chưa xoá đang hoặc sắp có hiệu lực. Index (workspace_id, person_id, start_date). */
	public int countCurrentOccupancies(long workspaceId, long personId, LocalDate today) {
		return jdbc().sql("SELECT COUNT(*) FROM reg_occupancy WHERE workspace_id = :workspaceId "
				+ "AND person_id = :personId AND deleted_at IS NULL AND (end_date IS NULL OR end_date >= :today)")
			.param("workspaceId", workspaceId)
			.param("personId", personId)
			.param("today", today)
			.query(Integer.class)
			.single();
	}

}
