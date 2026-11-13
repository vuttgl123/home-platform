package com.home_platform.registry.person;

import java.time.LocalDate;
import java.util.Optional;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository extends WorkspaceRepository<Person> {

	PersonRepository(RepositorySupport support) {
		super(support, "reg_person", Person.class);
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
