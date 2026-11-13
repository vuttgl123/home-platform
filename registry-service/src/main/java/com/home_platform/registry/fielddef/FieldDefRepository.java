package com.home_platform.registry.fielddef;

import java.util.List;

import com.home_platform.common.jdbc.RepositorySupport;
import com.home_platform.common.jdbc.WorkspaceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class FieldDefRepository extends WorkspaceRepository<FieldDef> {

	private static final String ORDER = " ORDER BY sort_order, label, id";

	FieldDefRepository(RepositorySupport support) {
		super(support, "reg_field_def", FieldDef.class);
	}

	/** Mọi mục của không gian, mọi đích, mọi trạng thái. */
	public List<FieldDef> listAll(long workspaceId) {
		return jdbc().sql("SELECT " + columns() + " FROM reg_field_def WHERE workspace_id = :workspaceId" + ORDER)
			.param("workspaceId", workspaceId)
			.query(rowMapper())
			.list();
	}

	/** Mục của một đích; {@code typeId} khác null thì chỉ giữ mục chung (type_id 0) và mục của loại đó. */
	public List<FieldDef> list(long workspaceId, FieldTarget target, Long typeId) {
		String byType = (typeId != null) ? " AND type_id IN (0, :typeId)" : "";
		var statement = jdbc()
			.sql("SELECT " + columns() + " FROM reg_field_def WHERE workspace_id = :workspaceId AND target = :target"
					+ byType + ORDER)
			.param("workspaceId", workspaceId)
			.param("target", target.name());
		if (typeId != null) {
			statement = statement.param("typeId", typeId);
		}
		return statement.query(rowMapper()).list();
	}

	/**
	 * Đã có mục (mọi trạng thái) cùng đích và cùng khoá ở một phạm vi chồng lên phạm vi
	 * {@code typeId} chưa: phạm vi chung (0) chồng lên mọi phạm vi riêng và ngược lại.
	 */
	public boolean keyOverlaps(long workspaceId, FieldTarget target, long typeId, String fieldKey) {
		return jdbc().sql("SELECT EXISTS (SELECT 1 FROM reg_field_def WHERE workspace_id = :workspaceId "
				+ "AND target = :target AND field_key = :fieldKey "
				+ "AND (type_id = 0 OR type_id = :typeId OR :typeId = 0))")
			.param("workspaceId", workspaceId)
			.param("target", target.name())
			.param("fieldKey", fieldKey)
			.param("typeId", typeId)
			.query(Integer.class)
			.single() == 1;
	}

}
