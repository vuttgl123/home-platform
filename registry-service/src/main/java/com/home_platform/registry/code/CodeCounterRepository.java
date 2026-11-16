package com.home_platform.registry.code;

import com.home_platform.common.jdbc.RepositorySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Bộ đếm của từng dãy mã. */
@Repository
public class CodeCounterRepository {

	private final JdbcClient jdbc;

	CodeCounterRepository(RepositorySupport support) {
		this.jdbc = support.jdbc();
	}

	/**
	 * Tăng bộ đếm của dãy (tạo bằng 1 nếu chưa có) và trả số mới. Dòng đếm bị giữ tới hết
	 * transaction: lệnh khác cùng dãy chờ, và transaction này rollback thì số được trả lại.
	 * @throws IllegalTransactionStateException nếu không có transaction đang mở
	 */
	public long next(long workspaceId, CodeTarget target, String series) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			throw new IllegalTransactionStateException(
					"CodeCounterRepository.next must run inside the transaction that writes the code");
		}
		this.jdbc.sql("INSERT INTO reg_code_counter (workspace_id, target, series, last_no) "
				+ "VALUES (:workspaceId, :target, :series, 1) ON DUPLICATE KEY UPDATE last_no = last_no + 1")
			.param("workspaceId", workspaceId)
			.param("target", target.name())
			.param("series", series)
			.update();
		return this.jdbc.sql("SELECT last_no FROM reg_code_counter WHERE workspace_id = :workspaceId "
				+ "AND target = :target AND series = :series")
			.param("workspaceId", workspaceId)
			.param("target", target.name())
			.param("series", series)
			.query(Long.class)
			.single();
	}

}
