package com.home_platform.registry;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.home_platform.common.context.WorkspaceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Khung cho test cần hai transaction thấy nhau, ví dụ hai lệnh ghi chạy cùng lúc. Khác với
 * {@link RegistryIT}, test ở đây <b>commit dữ liệu thật</b> vào database: transaction của test
 * bị tắt, mỗi lời gọi service tự mở và commit transaction của nó.
 *
 * <p>Chỉ dùng không gian {@link #WS}, và xoá sạch không gian đó trước và sau mỗi test. Không bao
 * giờ dùng {@code workspace_id} từ 0 trở lên.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class CommittedDataIT extends RegistryIT {

	protected static final long WS = -9001L;

	protected static final WorkspaceContext CTX = new WorkspaceContext(WS, USER);

	/** Theo thứ tự khoá ngoại: bảng trỏ tới bảng khác được xoá trước. */
	private static final List<String> WIPE = List.of(
			"UPDATE reg_item SET replaced_by_item_id = NULL WHERE workspace_id = :ws",
			"DELETE FROM reg_maintenance WHERE workspace_id = :ws", "DELETE FROM reg_todo_seen WHERE workspace_id = :ws",
			"DELETE FROM reg_item WHERE workspace_id = :ws", "DELETE FROM reg_occupancy WHERE workspace_id = :ws",
			"DELETE FROM reg_ownership WHERE workspace_id = :ws",
			"DELETE FROM reg_place_address WHERE workspace_id = :ws",
			"UPDATE reg_place SET parent_id = NULL WHERE workspace_id = :ws",
			"DELETE FROM reg_place WHERE workspace_id = :ws", "DELETE FROM reg_person WHERE workspace_id = :ws",
			"DELETE FROM reg_entity_version WHERE workspace_id = :ws",
			"DELETE FROM reg_field_def WHERE workspace_id = :ws", "DELETE FROM reg_place_type WHERE workspace_id = :ws",
			"DELETE FROM reg_item_category WHERE workspace_id = :ws",
			"DELETE FROM reg_code_counter WHERE workspace_id = :ws", "DELETE FROM reg_code_rule WHERE workspace_id = :ws");

	private static final int WAIT_SECONDS = 20;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final ExecutorService threads = Executors.newCachedThreadPool(work -> {
		Thread thread = new Thread(work, "concurrent-write");
		thread.setDaemon(true);
		return thread;
	});

	@BeforeEach
	@AfterEach
	void wipeWorkspace() {
		WIPE.forEach(sql -> this.jdbc.sql(sql).param("ws", WS).update());
	}

	@AfterEach
	void stopThreads() {
		this.threads.shutdownNow();
	}

	/**
	 * Chạy {@code work} trong một transaction ở luồng khác và giữ transaction đó mở cho tới khi gọi
	 * {@link OpenTransaction#commit()}. Trả về sau khi {@code work} chạy xong, nên mọi khoá của nó
	 * đang được giữ.
	 * @throws Exception lỗi của {@code work}, nếu có; transaction khi đó đã rollback
	 */
	protected <T> OpenTransaction<T> holdOpen(Callable<T> work) throws Exception {
		CountDownLatch ran = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> failure = new AtomicReference<>();
		Future<?> thread = this.threads
			.submit(() -> new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
				try {
					result.set(work.call());
				}
				catch (Exception ex) {
					failure.set(ex);
					status.setRollbackOnly();
				}
				finally {
					ran.countDown();
				}
				if (failure.get() == null) {
					await(release);
				}
			}));
		await(ran);
		if (failure.get() != null) {
			throw failure.get();
		}
		return new OpenTransaction<>(result.get(), release, thread);
	}

	/** Chạy {@code work} ở luồng khác; lời gọi service trong đó tự mở và commit transaction của nó. */
	protected <T> Future<T> inBackground(Callable<T> work) {
		return this.threads.submit(work);
	}

	/** Chạy {@code work} trong một transaction ở luồng của test, rồi commit. */
	protected <T> T inTransaction(Callable<T> work) {
		return new TransactionTemplate(this.transactionManager).execute(status -> {
			try {
				return work.call();
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	/**
	 * Chờ cho luồng nền kịp đi tới chỗ nó sẽ bị chặn. Nếu luồng nền chậm hơn khoảng chờ này thì nó
	 * chạy sau khi transaction kia commit: kết quả vẫn phải đúng, chỉ là test không còn thử được
	 * trường hợp xen giữa.
	 */
	protected void letItBlock() throws InterruptedException {
		Thread.sleep(700);
	}

	/** Số dòng của không gian {@link #WS} trong một bảng. */
	protected int rows(String table) {
		return this.jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE workspace_id = :ws")
			.param("ws", WS)
			.query(Integer.class)
			.single();
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timed out waiting for the other thread");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Một transaction đang mở ở luồng khác.
	 *
	 * @param result kết quả của việc đã chạy trong transaction
	 */
	protected record OpenTransaction<T>(T result, CountDownLatch release, Future<?> thread) {

		/** Commit và chờ luồng kia xong. */
		public void commit() throws Exception {
			this.release.countDown();
			this.thread.get(WAIT_SECONDS, TimeUnit.SECONDS);
		}

	}

}
