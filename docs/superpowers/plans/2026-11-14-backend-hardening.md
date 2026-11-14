# Backend Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sửa các điểm yếu tìm thấy khi review toàn bộ backend ngày 2026-11-14: dữ liệu sai khi hai lệnh ghi chạy cùng lúc, bốn lỗ hổng luật nghiệp vụ, lệnh chờ database không có giới hạn, code lặp và phần thừa.

**Architecture:** `WorkspaceRepository` có thêm hai cách đọc kèm khoá dòng (`FOR SHARE`, `FOR UPDATE`). Service của registry khoá dòng mà quyết định của nó dựa vào (địa điểm cha, địa điểm chứa, người) trước khi kiểm tra và ghi, nên "kiểm tra rồi ghi" không còn bị lệnh khác chen vào giữa. Luật không biểu diễn được bằng khoá dòng (giấy tờ không trùng) chuyển thành khoá duy nhất trong database. Không đổi kiến trúc ba tầng, không thêm thư viện.

**Tech Stack:** Java 21, Spring Boot 4.1.2, `JdbcClient`, MySQL 8.0.36 (InnoDB, `REPEATABLE READ`), Flyway, JUnit 5, MockMvc.

**Spec:** Mục "Phát hiện" ngay dưới đây là yêu cầu của kế hoạch này. Luật nghiệp vụ gốc: `docs/superpowers/specs/2026-11-13-business-rules-design.md` mục 5.1 và 5.5 đến 5.9 (Task 4, 5 và 7 sửa bốn dòng trong đó).

## Phát hiện

Gộp từ hai lượt đọc độc lập toàn bộ mã nguồn chính. Nhóm A: sai dữ liệu khi ghi cùng lúc; L: lỗ hổng luật; B: hiệu năng và độ bền; C: cấu trúc code; D: vận hành. Mọi dòng đều suy ra từ code thành kịch bản cụ thể; chưa kịch bản nào được chạy thử trên database, test của từng task sẽ làm việc đó.

| # | Vấn đề | Chỗ | Task |
|---|---|---|---|
| A1 | Xoá địa điểm đếm con, cư trú, đồ đạc rồi mới xoá. Lệnh tạo đồ, tạo cư trú, tạo địa điểm con chạy xen giữa vẫn thành công: còn đồ đang dùng trong địa điểm đã xoá | `PlaceService.delete`, `ItemService.create/move/putBack/restore`, `OccupancyService.create`, `PlaceService.create/restore` | 3, 4 |
| A2 | Di chuyển địa điểm: (a) tạo con dưới P trong lúc P đang được chuyển thì con mang `path` cũ; (b) chuyển A vào dưới B và B vào dưới A cùng lúc tạo thành vòng | `PlaceService.move` | 3 |
| A3 | Xoá người đếm cư trú rồi mới xoá; tạo cư trú xen giữa vẫn thành công | `PersonService.delete`, `OccupancyService.create` | 4 |
| A4 | Hai lệnh tạo cư trú giao thời gian cho cùng người, cùng chỗ đều qua được bước kiểm tra | `OccupancyService.requireValidRange` | 4 |
| A5 | Hai lệnh tạo người cùng giấy tờ đều qua được bước kiểm tra (spec 5.6 đã ghi "chưa có khoá duy nhất trong database") | `PersonService.requireUniqueDocument` | 5 |
| A6 | Một transaction ghi đã chốt snapshot, rồi danh mục đổi và commit, rồi transaction đó mới nạp cache mục tuỳ biến: cache giữ bản cũ tới 5 phút, mọi request của không gian kiểm tra `attributes` theo bản cũ | `FieldDefService.listAll` (`@Cacheable`), `CatalogCache` | 6 |
| A7 | Deadlock và hết thời gian chờ khoá ra 500 "Unexpected error" thay vì 409 để client thử lại | `ApiExceptionHandler` | 1 |
| L1 | Sửa một dòng cư trú đã rời thành "đang ở" tại địa điểm (hoặc cho người) đã xoá: không có gì chặn, kể cả khi không có lệnh nào chạy cùng lúc. Sau đó người ấy không xoá được nữa | `OccupancyService.update` | 4 |
| L2 | `POST /setup` chỉ xét "chưa có loại địa điểm" rồi tạo cả nhóm đồ và mục tuỳ biến. Không gian đã có nhóm trùng tên mặc định (hoặc người dùng đã xoá hết loại địa điểm) thì mọi lần gọi đều 409, hoặc tạo lại thứ đã cố ý xoá | `SetupService.setup` | 7 |
| L3 | Món A bị thay bằng B, rồi B bị xoá vì nhập nhầm: A không bao giờ "đưa lại" được vì `replaced_by_item_id` không có gì gỡ | `ItemService.putBack` | 7 |
| L4 | `note` trên các lệnh `DELETE` là tham số URL không kiểm tra độ dài, không cắt khoảng trắng: dài hơn 255 thì cả lệnh xoá rollback với 400 "Invalid data" không nêu trường; toàn khoảng trắng thì lưu chuỗi trắng thay vì `NULL` | `PlaceController.delete/removeAddress`, `ItemController.delete`, `PersonController.delete` | 7 |
| B1 | Kết nối database không có giới hạn chờ: `connectTimeout`, `socketTimeout` của driver mặc định là vô hạn, chờ khoá mặc định 50 giây, transaction không có hạn | `platform-common-defaults.properties` | 2 |
| B2 | `CrudSql` ghi "dựng một lần" nhưng nối chuỗi SQL ở mỗi lần gọi; `EntityMetadata.components()` chép danh sách ở mỗi lần gọi; `KeysetQuery.componentOf` dò tuần tự | `platform-common/.../jdbc` | 8 |
| B3 | `registry-service` kéo `spring-boot-starter-batch` và `-batch-test` nhưng không lớp nào dùng | `registry-service/pom.xml` | 9 |
| C1 | Đoạn "lấy thừa một dòng, cắt, tạo cursor" lặp 5 chỗ (hai chỗ thiếu kiểm tra `limit < 1`); nối tiền tố bảng vào danh sách cột lặp 2 chỗ; dựng câu `INSERT` lặp 4 chỗ (2 chỗ dựng lại ở mỗi lần gọi) | `KeysetQuery`, `EntityHistory`, `PlaceRepository.byArea`, `ActivityRepository`, identity `WorkspaceRepository`, `UserRepository`, `PlaceAddressRepository` | 8 |
| C2 | `ActivityRepository` đọc thẳng bảng `reg_entity_version` với tên viết cứng, trong khi `EntityHistory` được ghi là "lối vào duy nhất" và lấy tên bảng từ `platform.history.table`; cách đọc dòng version bị chép làm hai | `registry/history/ActivityRepository` | 8 |
| C3 | `restoreVersion` chép lại chuỗi `with...` của `update` ở cả ba service: thêm một trường sửa được vào `update` mà quên `restoreVersion` thì vẫn biên dịch, và trường đó lặng lẽ không được khôi phục | `PlaceService`, `ItemService`, `PersonService` | 9 |
| C4 | `ItemService`: trường không `private`, trả `Item[]`, tên lớp viết đầy đủ giữa code. Ghi chú "Restored from version" lặp 3 service. `Text` nằm trong `registry.catalog` nhưng 29 file ở mọi package dùng | `registry-service` | 9 |
| D1 | Log không có mã request, không gian, người gọi: một lỗi 500 không lần ngược được về request gây ra nó | `platform-common` | 10 |

Đã cân nhắc và **không** làm, kèm lý do:

- `@Transactional(readOnly = true)` cho các hàm đọc nhiều truy vấn: với MySQL, mỗi transaction thêm ba lượt đi về (`SET autocommit=0`, `COMMIT`, `SET autocommit=1`); database ở xa nên đọc chậm đi, đổi lại chỉ được một snapshot nhất quán mà các màn hình hiện tại không cần.
- Tính `changed_fields` từ bản ghi service đang giữ thay vì đọc snapshot trước: bớt một truy vấn mỗi lần ghi nhưng đổi nghĩa (hậu duệ bị đổi `path` do cha di chuyển sẽ không còn thấy `path` trong lần sửa kế tiếp).
- Cache prepared statement của driver, dạng điều kiện keyset: chưa có số đo cho thấy cần.
- Bảo mật, Redis, Elasticsearch: theo quyết định trước của bạn.

## Global Constraints

- **Khoá trước, đọc thường sau.** Trong một transaction ghi, câu đọc kèm khoá phải chạy trước câu `SELECT` thường đầu tiên mà kết quả dùng để quyết định (đếm, kiểm tra giao nhau). Lý do: ở `REPEATABLE READ`, `SELECT` thường đầu tiên chốt snapshot của cả transaction; nếu nó chạy trước khi lấy được khoá thì các câu đếm sau đó không thấy dòng mà transaction kia vừa commit.
- Đọc kèm khoá chỉ gọi trong transaction; gọi ngoài transaction ném `IllegalTransactionStateException` (khoá sẽ nhả ngay, không bảo vệ gì).
- Thứ tự lấy khoá trong một lệnh: dòng của chính bản ghi, rồi địa điểm, rồi người.
- Thông báo lỗi tiếng Anh, đúng chữ như ghi trong từng task.
- Không sửa `V1__*.sql` đã chạy; thay đổi cấu trúc là file `V2__*.sql` mới.
- Test `*IT` chạy trên database UAT dùng chung. Test thường của kế hoạch này rollback như cũ. Riêng `ConcurrentWritesIT` **commit dữ liệu thật** vì hai transaction phải thấy nhau: nó chỉ dùng `workspace_id = -9001`, xoá sạch không gian đó trước và sau mỗi test, không bao giờ chạm `workspace_id >= 0`.
- Chạy một lớp IT: `./mvnw -pl <module> -am verify -Dit.test=<Lớp> -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false`. Mỗi task chạy test thường của module cộng các IT nêu trong task; bộ đầy đủ `./mvnw verify` chạy một lần ở task cuối.
- Commit mỗi task bằng danh tính của Windows git (`git -c user.name=... -c user.email=...`), push bằng `/mnt/c/Program Files/Git/cmd/git.exe` sau task cuối. Không đưa `application-uat.yaml`, `application-prod.yaml`, `.remember/`, `target/` vào commit.
- Không thêm thư viện. Không đụng tới bảo mật.

## Review Focus

1. Một request chờ khoá lâu hơn 5 giây phải nhận 409, không treo và không ra 500. Test: `waitingTooLongForALockFails` (Task 3) cùng `lockFailureIsReportedAsConflict` (Task 1).
2. Lệnh xoá bị từ chối vì thấy đồ vừa thêm không để lại version nào và không đổi `version` của địa điểm. Test: `deleteSeesAnItemAddedConcurrently` (Task 3).
3. Hai lệnh chuyển chéo nhau: đúng một lệnh thành công, lệnh kia 409; không địa điểm nào mất đường về gốc. Test: `crossingMovesDoNotMakeACycle` (Task 3).
4. Người đã xoá không chặn người khác dùng lại giấy tờ, kể cả sau khi có khoá duy nhất; khôi phục người đó khi giấy tờ đã thuộc người khác thì 409. Test có sẵn `deletedPersonDoesNotBlockTheSameDocument`, `restoreVersionChecksDocumentDuplicates` phải còn xanh, thêm `restoringAPersonWhoseDocumentWasTakenIsAConflict` (Task 5).
5. Đọc kèm khoá ngoài transaction bị từ chối thay vì lặng lẽ không khoá. Test: `lockingReadOutsideATransactionIsRejected` (Task 1).

---

### Task 1: Đọc kèm khoá dòng và lỗi khoá thành 409

**Files:**
- Modify: `platform-common/src/main/java/com/home_platform/common/jdbc/{CrudSql,WorkspaceRepository}.java`, `.../common/web/ApiExceptionHandler.java`
- Test: `platform-common/src/test/java/com/home_platform/common/jdbc/{CrudSqlTests,WorkspaceRepositoryTests,WorkspaceRepositoryIT}.java`, `.../common/web/ApiExceptionHandlerTests.java`

**Interfaces (Produces):**

```java
// CrudSql
String selectByIdForShare()     // selectById() + " FOR SHARE"
String selectByIdForUpdate()    // selectById() + " FOR UPDATE"

// WorkspaceRepository<T>: dòng chưa xoá mềm; dòng bị giữ tới hết transaction
public Optional<T> findByIdForShare(long workspaceId, long id)    // lệnh khác không sửa, xoá được dòng
public Optional<T> findByIdForUpdate(long workspaceId, long id)   // lệnh khác không sửa, xoá, giữ được dòng

// ApiExceptionHandler
@ExceptionHandler(PessimisticLockingFailureException.class)       // 409
ProblemDetail handleLockFailure(PessimisticLockingFailureException ex)
// detail: "The record is being changed by someone else; try again"
```

- [ ] **Step 1: Viết test đỏ.**
  - `CrudSqlTests.lockingSelectsAppendTheLockClause`: với bảng có `deleted_at`, `selectByIdForUpdate()` bằng `selectById() + " FOR UPDATE"` và `selectByIdForShare()` bằng `selectById() + " FOR SHARE"`.
  - `WorkspaceRepositoryTests.lockingReadOutsideATransactionIsRejected`: `findByIdForShare(1, 1)` và `findByIdForUpdate(1, 1)` trên repository dựng như các test trong lớp đó ném `IllegalTransactionStateException`.
  - `WorkspaceRepositoryIT.lockingReadsReturnLiveRowsOnly`: sau `insert`, cả hai hàm trả đúng record; sau `softDelete`, cả hai trả `Optional.empty()`; ở `WS_B` trả rỗng.
  - `ApiExceptionHandlerTests.lockFailureIsReportedAsConflict`: endpoint `/locked` của `FailingController` ném `new CannotAcquireLockException("Deadlock found; internal detail")`; mong 409, `application/problem+json`, `$.detail` đúng chuỗi ở trên, body không chứa `"Deadlock"`.
- [ ] **Step 2:** Chạy `./mvnw -pl platform-common test`: ba test thường mới đỏ (chưa có hàm).
- [ ] **Step 3: Cài đặt.** Kiểm tra transaction bằng `TransactionSynchronizationManager.isActualTransactionActive()` như `EntityHistory.record`. `handleLockFailure` ghi `log.warn` thông báo gốc, không trả nó cho client.
- [ ] **Step 4:** `./mvnw -pl platform-common verify -Dit.test=WorkspaceRepositoryIT`: xanh.
- [ ] **Step 5:** Thêm vào mục "Repository" và "Lỗi" của `README.md`: hai hàm mới và việc lỗi khoá trả 409. Commit `Add row-locking reads and report lock failures as 409`.

### Task 2: Giới hạn thời gian chờ database

**Files:**
- Modify: `platform-common/src/main/resources/platform-common-defaults.properties`, `README.md`
- Test: `platform-common/src/test/java/com/home_platform/common/autoconfigure/PlatformCommonAutoConfigurationTests.java`; create `platform-common/src/test/java/com/home_platform/common/jdbc/ConnectionDefaultsIT.java`

Bốn dòng thêm vào file defaults, đúng giá trị:

```properties
spring.datasource.hikari.data-source-properties.connectTimeout=5000
spring.datasource.hikari.data-source-properties.socketTimeout=60000
spring.datasource.hikari.data-source-properties.sessionVariables=innodb_lock_wait_timeout=5
spring.transaction.default-timeout=30s
```

- [ ] **Step 1: Viết test đỏ.**
  - `PlatformCommonAutoConfigurationTests.databaseWaitsAreBounded`: trong `runner.run`, `context.getEnvironment().getProperty(...)` của bốn khoá trên bằng `"5000"`, `"60000"`, `"innodb_lock_wait_timeout=5"`, `"30s"`.
  - `ConnectionDefaultsIT extends CommonIT`, test `sessionWaitsAtMostFiveSecondsForALock`: `jdbc.sql("SELECT @@SESSION.innodb_lock_wait_timeout").query(Integer.class).single()` bằng 5.
- [ ] **Step 2:** Chạy, thấy đỏ. **Step 3:** thêm bốn dòng. **Step 4:** `./mvnw -pl platform-common verify -Dit.test=ConnectionDefaultsIT` xanh.
- [ ] **Step 5:** README, mục "Thời gian": thêm một gạch đầu dòng nêu bốn giới hạn và câu "migration chạy lâu hơn 60 giây thì đặt lại `socketTimeout` cho lần chạy đó". Commit `Bound database connect, read, lock and transaction waits`.

### Task 3: Địa điểm: khoá khi xoá, tạo con, di chuyển, khôi phục

**Files:**
- Modify: `registry-service/src/main/java/com/home_platform/registry/place/PlaceService.java`
- Create: `registry-service/src/test/java/com/home_platform/registry/{CommittedDataIT,ConcurrentWritesIT}.java`

**Interfaces:**
- Consumes: `findByIdForShare`, `findByIdForUpdate` (Task 1).
- Produces (Task 4 dùng):

```java
// PlaceService: giữ địa điểm chưa xoá tới hết transaction, không ai xoá hay chuyển nó được
public Place getForShare(long workspaceId, long id)             // NotFoundException.of("Place", id) nếu không có hoặc đã xoá
public Optional<Place> findForShare(long workspaceId, long id)

// CommittedDataIT (khung test có commit)
protected static final long WS = -9001L;
protected static final WorkspaceContext CTX = new WorkspaceContext(WS, -101L);
protected <T> OpenTransaction<T> holdOpen(Callable<T> work)     // chạy work ở luồng khác, giữ transaction mở
protected <T> Future<T> inBackground(Callable<T> work)          // chạy work ở luồng khác, transaction riêng của service
protected <T> T inTransaction(Callable<T> work)                 // chạy work trong một transaction ở luồng của test, rồi commit
protected void letItBlock()                                     // chờ 700 ms cho luồng nền đi tới chỗ bị chặn
protected record OpenTransaction<T>(T result, ...) { public void commit() }
```

- [ ] **Step 1: Viết `CommittedDataIT`.** Kế thừa `RegistryIT` để dùng chung application context, tắt transaction của test:

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class CommittedDataIT extends RegistryIT {

	private static final List<String> WIPE = List.of(
			"UPDATE reg_item SET replaced_by_item_id = NULL WHERE workspace_id = :ws",
			"DELETE FROM reg_item WHERE workspace_id = :ws", "DELETE FROM reg_occupancy WHERE workspace_id = :ws",
			"DELETE FROM reg_place_address WHERE workspace_id = :ws",
			"UPDATE reg_place SET parent_id = NULL WHERE workspace_id = :ws",
			"DELETE FROM reg_place WHERE workspace_id = :ws", "DELETE FROM reg_person WHERE workspace_id = :ws",
			"DELETE FROM reg_entity_version WHERE workspace_id = :ws",
			"DELETE FROM reg_field_def WHERE workspace_id = :ws", "DELETE FROM reg_place_type WHERE workspace_id = :ws",
			"DELETE FROM reg_item_category WHERE workspace_id = :ws");

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final ExecutorService threads = Executors.newCachedThreadPool();

	@BeforeEach
	@AfterEach
	void wipeWorkspace() {
		WIPE.forEach(sql -> this.jdbc.sql(sql).param("ws", WS).update());
	}

	protected <T> OpenTransaction<T> holdOpen(Callable<T> work) throws Exception {
		CountDownLatch ran = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<T> result = new AtomicReference<>();
		Future<?> thread = this.threads.submit(
				() -> new TransactionTemplate(this.transactionManager).executeWithoutResult(status -> {
					try {
						result.set(work.call());
					}
					catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
					finally {
						ran.countDown();
					}
					await(release);
				}));
		await(ran);
		if (thread.isDone()) {
			thread.get();   // work ném lỗi: báo ra ngay
		}
		return new OpenTransaction<>(result.get(), release, thread);
	}
	// inBackground: this.threads.submit(work). letItBlock: Thread.sleep(700).
	// inTransaction: new TransactionTemplate(this.transactionManager).execute(...), bọc lỗi của work như holdOpen.
	// await(latch): latch.await(20, SECONDS), ném IllegalStateException nếu hết giờ hoặc bị ngắt.
	// OpenTransaction.commit(): release.countDown(); thread.get(20, SECONDS).
}
```

- [ ] **Step 2: Viết `ConcurrentWritesIT extends CommittedDataIT`** với `@BeforeEach` tạo loại địa điểm `T` (`placeTypes.create(CTX, new PlaceTypeCreateRequest("T", "T", null, null))`), hàm phụ `place(Long parentId, String name)` và `item(long placeId, String name)` gọi thẳng service, và năm test:

```java
@Test
void deleteSeesAnItemAddedConcurrently() throws Exception {
	long room = place(null, "P101");
	OpenTransaction<Item> adding = holdOpen(() -> this.items.create(CTX, item(room, "Tủ")));
	Future<Void> deleting = inBackground(() -> {
		this.places.delete(CTX, room, 1, null);
		return null;
	});
	letItBlock();
	adding.commit();

	assertThatThrownBy(deleting::get).cause()
		.isInstanceOfSatisfying(ConflictException.class, ex -> assertThat(ex.getBody().getDetail())
			.contains("0 child places, 0 occupancies and 1 items"));
	assertThat(this.places.get(WS, room).version()).isEqualTo(1);
	assertThat(this.history.list(WS, "PLACE", room, new CursorRequest(null, 20)).items()).hasSize(1);
}
```

  Các test còn lại của lớp này (ở task này và các task sau) theo đúng dạng trên: lỗi của lệnh nền lấy qua `Future.get()`, loại lỗi xét trên `cause()`, thông báo xét trên `getBody().getDetail()`.

  - `childCreatedDuringADeleteIsRefused`: `holdOpen` xoá cha (version 1); nền tạo con dưới cha; sau `commit()` lệnh nền ném `NotFoundException`.
  - `childCreatedDuringAMoveLandsUnderTheNewPath`: có `X`, `P` ở cấp cao nhất; `holdOpen` chuyển `P` vào dưới `X` (`new PlaceMoveRequest(1, x, null)`); nền tạo `C` dưới `P`; sau `commit()`, `C.path()` bằng `"/" + x + "/" + p + "/" + c + "/"` và `C.depth()` bằng 2.
  - `crossingMovesDoNotMakeACycle`: `holdOpen` chuyển `A` vào dưới `B`; nền chuyển `B` vào dưới `A`; sau `commit()` lệnh nền ném `ConflictException` có thông báo `"A place cannot be moved into itself or its descendants"`; `B.parentId()` vẫn `null`, `A.path()` bằng `"/" + b + "/" + a + "/"`.
  - `waitingTooLongForALockFails`: `holdOpen` xoá `room`; nền xoá `room` lần nữa; **không** commit trong 6 giây; lệnh nền ném lỗi có nguyên nhân là `PessimisticLockingFailureException` trong vòng 10 giây; rồi `commit()`.
- [ ] **Step 3:** Chạy `./mvnw -pl registry-service -am verify -Dit.test=ConcurrentWritesIT -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false`. Mong: bốn test đầu đỏ (lệnh nền thành công khi lẽ ra bị từ chối, hoặc `path` là `"/" + p + "/" + c + "/"`). Test thứ năm có thể đã xanh nhờ khoá của khoá ngoại; không sao.
- [ ] **Step 4: Sửa `PlaceService`.** Mỗi hàm dưới đây lấy khoá ở những câu đầu tiên, trước mọi `SELECT` thường:
  - `delete`: thay `get(...)` bằng `repository.findByIdForUpdate(...)`, không có thì `NotFoundException.of("Place", id)`.
  - `create`: cha đọc bằng `findByIdForShare`.
  - `move`: `current` bằng `findByIdForUpdate`, rồi `parent` (nếu có) bằng `findByIdForShare`; các bước sau giữ nguyên.
  - `restore`: kiểm tra cha còn sống bằng `findByIdForShare(...).isEmpty()`.
  - Thêm `getForShare`, `findForShare`. Giữ `isAlive` cho tới Task 4.
- [ ] **Step 5:** Chạy lại lệnh ở Step 3 cộng `-Dit.test='ConcurrentWritesIT,Place*IT'`: xanh hết.
- [ ] **Step 6:** Commit `Lock places while deleting, moving and adding children`.

### Task 4: Đồ đạc, cư trú, người: giữ địa điểm và người khi ghi

**Files:**
- Modify: `registry-service/src/main/java/com/home_platform/registry/item/ItemService.java`, `.../occupancy/OccupancyService.java`, `.../person/PersonService.java`, `.../place/PlaceService.java` (bỏ `isAlive`), `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 5.7), `README.md`
- Test: `.../registry/ConcurrentWritesIT.java`, `.../registry/occupancy/OccupancyIT.java`

**Luật mới (L1), cần bạn đồng ý trước khi làm:** sửa một dòng cư trú đòi địa điểm và người của nó chưa bị xoá; nếu đã xoá thì 409 `"Restore place <id> first"` hoặc `"Restore person <id> first"`. "Chuyển đi" (`/end`) và xoá dòng không đổi.

**Interfaces:**
- Consumes: `PlaceService.getForShare`, `findForShare` (Task 3); `findByIdForUpdate` (Task 1).
- Produces:

```java
// PersonService: giữ người chưa xoá tới hết transaction; hai lệnh ghi cư trú của cùng một người chạy lần lượt
public Person getForUpdate(long workspaceId, long id)            // NotFoundException.of("Person", id)
public Optional<Person> findForUpdate(long workspaceId, long id)
```

- [ ] **Step 1: Test đỏ trong `ConcurrentWritesIT`.**
  - `itemAddedDuringAPlaceDeleteIsRefused`: `holdOpen` xoá `room`; nền `items.create` vào `room`; sau `commit()` ném `NotFoundException`; `reg_item` của `WS` không có dòng nào.
  - `occupancyAddedDuringAPersonDeleteIsRefused`: `holdOpen` xoá người; nền tạo cư trú cho người đó; ném `NotFoundException`.
  - `personDeleteSeesAnOccupancyAddedConcurrently`: `holdOpen` tạo cư trú (hôm nay, không ngày cuối); nền xoá người; ném `ConflictException` chứa `"1 current or upcoming occupancies"`.
  - `overlappingOccupanciesCreatedTogetherLeaveOne`: `holdOpen` tạo cư trú; nền tạo dòng y hệt; ném `ConflictException` chứa `"already has an overlapping occupancy"`; bảng có đúng một dòng.
- [ ] **Step 2: Test đỏ trong `OccupancyIT`** (rollback như các test cạnh nó):
  - `editingAnOccupancyAtADeletedPlaceIsRefused`: cư trú đã kết thúc hôm qua, xoá địa điểm, `PUT /occupancies/{id}` với `endDate` null: 409, `$.detail` bằng `"Restore place <id> first"`.
  - `editingAnOccupancyOfADeletedPersonIsRefused`: tương tự với người: `"Restore person <id> first"`.
- [ ] **Step 3:** Chạy `ConcurrentWritesIT,OccupancyIT`: sáu test mới đỏ; các test `/end` có sẵn vẫn xanh.
- [ ] **Step 4: Cài đặt.**
  - `ItemService.create`, `move`: `places.getForShare(...)` thay `places.get(...)`. `putBack`, `restore`: `places.findForShare(...).isEmpty()` thay `!places.isAlive(...)`. Xoá `PlaceService.isAlive`.
  - `PersonService.delete`: câu đầu tiên là `repository.findByIdForUpdate`. Thêm `getForUpdate`, `findForUpdate`.
  - `OccupancyService.create`: `places.getForShare`, rồi `people.getForUpdate`, rồi mới `requireValidRange`.
  - `OccupancyService.update`: câu đầu tiên `repository.findByIdForUpdate(id)` (không có thì `NotFoundException.of("Occupancy", id)`), rồi `places.findForShare` và `people.findForUpdate` (rỗng thì 409 với thông báo ở trên), rồi `requireValidRange`.
- [ ] **Step 5:** Chạy `-Dit.test='ConcurrentWritesIT,Occupancy*IT,Item*IT,Person*IT,Place*IT'`: xanh.
- [ ] **Step 6:** Spec 5.7, dòng "**Sửa**": thêm "địa điểm và người của dòng phải chưa xoá, nếu không trả 409". README dòng `/occupancies`: thêm ý đó. Commit `Hold the place and the person while writing items and occupancies`.

### Task 5: Giấy tờ của người: khoá duy nhất trong database

**Files:**
- Create: `registry-service/src/main/resources/db/migration/V2__person_live_document.sql`
- Modify: `registry-service/src/main/java/com/home_platform/registry/person/PersonService.java`, `docs/superpowers/specs/2026-11-13-business-rules-design.md` (mục 5.6), `README.md`
- Test: `.../registry/schema/RegistrySchemaIT.java`, `.../registry/ConcurrentWritesIT.java`, `.../registry/person/PersonIT.java`

```sql
-- Hai người chưa xoá trong một không gian không trùng cặp (id_type, id_number). Cột sinh ra là
-- NULL với người đã xoá hoặc không có giấy tờ, và khoá duy nhất bỏ qua NULL.
ALTER TABLE reg_person
    ADD COLUMN live_document VARCHAR(51)
        AS (IF(deleted_at IS NULL AND id_number IS NOT NULL, CONCAT(id_type, ':', id_number), NULL)) VIRTUAL,
    ADD UNIQUE KEY uk_reg_person_live_document (workspace_id, live_document);
```

`Person` không có thành phần `liveDocument`: repository liệt kê cột theo record nên cột này không bao giờ được đọc hay ghi.

- [ ] **Step 1: Kiểm tra dữ liệu hiện có trước khi thêm migration.** Thêm vào `RegistrySchemaIT` test `noTwoLivePeopleShareADocument`: câu dưới đây trả 0. Chạy `-Dit.test=RegistrySchemaIT`. Nếu khác 0: dừng, báo các dòng trùng cho người dùng, không thêm file `V2` (một migration hỏng giữa chừng trên MySQL phải sửa tay bảng lịch sử của Flyway).

```sql
SELECT COUNT(*) FROM (SELECT 1 FROM reg_person WHERE deleted_at IS NULL AND id_number IS NOT NULL
  GROUP BY workspace_id, id_type, id_number HAVING COUNT(*) > 1) d
```

- [ ] **Step 2: Test đỏ.**
  - `RegistrySchemaIT`: thêm `entry("reg_person.uk_reg_person_live_document", List.of("workspace_id", "live_document"))` vào bảng index mong đợi.
  - `ConcurrentWritesIT.sameDocumentCreatedTogetherLeavesOnePerson`: `holdOpen` tạo người với `idType` `"CCCD"`, `idNumber` `"001"`; nền tạo người khác cùng giấy tờ; sau `commit()` lệnh nền ném `ConflictException` có thông báo `"Another person already has this document"`; `reg_person` của `WS` có một dòng.
  - `PersonIT.restoringAPersonWhoseDocumentWasTakenIsAConflict`: tạo A có giấy tờ, xoá A, tạo B cùng giấy tờ, khôi phục A: 409 (đường kiểm tra ở ứng dụng, thông báo cũ `"Person <idB> already has this document"`).
- [ ] **Step 3:** Chạy `-Dit.test='RegistrySchemaIT,ConcurrentWritesIT,PersonIT'`: hai test đầu đỏ.
- [ ] **Step 4: Cài đặt.** Thêm file `V2`. Trong `PersonService`, bọc `repository.insert`, `update`, `restore` của `create`, `update`, `restore`, `restoreVersion` bằng một hàm riêng bắt `DuplicateKeyException` và ném `ConflictException("Another person already has this document")`. Giữ `requireUniqueDocument` (nó cho thông báo có `id` người đã có trong trường hợp thường gặp).
- [ ] **Step 5:** Chạy lại Step 3 cộng `Person*IT`: xanh.
- [ ] **Step 6:** Spec 5.6: thay "Kiểm tra ở ứng dụng, chưa có khoá duy nhất trong database" bằng "Kiểm tra ở ứng dụng để có thông báo kèm `id`; khoá duy nhất `uk_reg_person_live_document` chặn trường hợp hai lệnh chạy cùng lúc". Commit `Enforce unique person documents in the database`.

### Task 6: Cache danh mục luôn nạp bản đã commit mới nhất

**Files:**
- Modify: `registry-service/src/main/java/com/home_platform/registry/fielddef/FieldDefRepository.java`, `.../placetype/PlaceTypeRepository.java`, `.../itemcategory/ItemCategoryRepository.java`, `.../catalog/CatalogCache.java` (chú thích)
- Test: `registry-service/src/test/java/com/home_platform/registry/ConcurrentWritesIT.java`

Ba hàm `listAll` là nguồn nạp của ba cache danh mục. Thêm ` FOR SHARE` vào cuối câu SQL của cả ba: câu đọc kèm khoá luôn thấy bản đã commit mới nhất, bất kể transaction đang gọi đã chốt snapshot từ lúc nào; lệnh sửa danh mục chạy cùng lúc phải chờ transaction này xong rồi mới commit và xoá cache, nên cache không thể giữ bản cũ hơn database. Gọi ngoài transaction thì khoá nhả ngay, không ảnh hưởng gì.

- [ ] **Step 1: Test đỏ** `ConcurrentWritesIT.catalogChangeCommittedMidTransactionIsNotCachedStale`:

```java
long room = place(null, "P101");
inTransaction(() -> {
	this.places.get(WS, room);   // SELECT thường: chốt snapshot của transaction này
	inBackground(() -> this.fieldDefs.create(CTX, new FieldDefCreateRequest(FieldTarget.PLACE, 0L, "floor_no",
			"Tầng", FieldDataType.NUMBER, null, false, 10))).get();
	return this.attributeValidation.validate(WS, FieldTarget.PLACE, this.type, Map.of("floor_no", 3));
});
assertThat(this.attributeValidation.validate(WS, FieldTarget.PLACE, this.type, Map.of("floor_no", 3)))
	.containsEntry("floor_no", 3);
```

- [ ] **Step 2:** Chạy `-Dit.test=ConcurrentWritesIT`: đỏ với `BadRequestException` "Invalid attributes" (cache nạp từ snapshot cũ).
- [ ] **Step 3:** Thêm ` FOR SHARE` vào ba câu `listAll`; ghi lý do ở trên vào javadoc của `CatalogCache`.
- [ ] **Step 4:** `-Dit.test='ConcurrentWritesIT,FieldDefIT,AttributeValidationIT,PlaceTypeIT,ItemCategoryIT,SetupIT'`: xanh.
- [ ] **Step 5:** Commit `Load catalog caches from the latest committed rows`.

### Task 7: Ba lỗ hổng luật: setup, đưa lại món, ghi chú khi xoá

**Files:**
- Modify: `registry-service/src/main/java/com/home_platform/registry/setup/SetupService.java`, `.../itemcategory/{ItemCategoryService,ItemCategoryRepository}.java`, `.../fielddef/{FieldDefService,FieldDefRepository}.java`, `.../item/{ItemService,ItemController}.java`, `.../place/PlaceController.java`, `.../person/PersonController.java`, spec mục 5.1 và 5.9, `README.md`
- Test: `.../registry/setup/SetupIT.java`, `.../registry/item/ItemLifecycleIT.java`, `.../registry/place/PlaceDeleteIT.java`, `.../registry/person/PersonIT.java`

**Hai luật đổi, cần bạn đồng ý trước khi làm:**
- **L2:** setup chỉ chạy khi không gian chưa có loại địa điểm, chưa có nhóm đồ **và** chưa có mục tuỳ biến nào; có bất kỳ thứ gì thì không làm gì và trả thành công như lần gọi lặp hiện nay.
- **L3:** món đã bị thay được "đưa lại" nếu món thay nó đã bị xoá; khi đó `replaced_by_item_id` về `NULL`. Món thay còn sống thì vẫn 409 như cũ.

**Interfaces (Produces):** `ItemCategoryService.anyExists(long workspaceId)`, `FieldDefService.anyExists(long workspaceId)` (cùng dạng `PlaceTypeService.anyExists`, mỗi cái một câu `SELECT EXISTS` ở repository).

- [ ] **Step 1: Test đỏ.**
  - `SetupIT.setupDoesNothingWhenACategoryAlreadyExists`: tạo nhóm `"Khác"`, gọi `POST /setup`: 200, không gian không có loại địa điểm nào, vẫn một nhóm đồ.
  - `SetupIT.setupDoesNotBringBackDeletedPlaceTypes`: setup, xoá cả bảy loại địa điểm, setup lần nữa: 200, vẫn không có loại nào.
  - `ItemLifecycleIT.itemWhoseReplacementWasDeletedCanBePutBack`: thay A bằng B, xoá B, `POST /items/{A}/put-back`: 200, `$.replacedByItemId` null, `$.status` `"IN_USE"`. Test có sẵn `replacedItemCannotBeReturned` phải còn xanh.
  - `PlaceDeleteIT.overlongDeleteNoteNamesTheField`: `DELETE /places/{id}?version=1&note=<256 ký tự>`: 400, `$.errors[0].field` bằng `"note"`; địa điểm còn đó, `version` vẫn 1. `blankDeleteNoteIsStoredAsNull`: `note=%20`: 204 và `version(id, 2).note()` null.
  - `ItemLifecycleIT.overlongDeleteNoteNamesTheField`, `PersonIT.overlongDeleteNoteNamesTheField`: cùng dạng.
- [ ] **Step 2:** Chạy `-Dit.test='SetupIT,ItemLifecycleIT,PlaceDeleteIT,PersonIT'`: bảy test mới đỏ.
- [ ] **Step 3: Cài đặt.** `SetupService.setup` trả `false` khi một trong ba `anyExists` đúng. `ItemService.putBack`: nếu `replacedByItemId` khác null thì `repository.findById` món thay; còn sống thì 409 như cũ, không còn thì ghi kèm `withReplacedByItemId(null)`. Bốn tham số `note` của các lệnh `DELETE` thành `@RequestParam(required = false) @Size(max = 255) String note` và đi qua `Text.trimToNull` trước khi vào service.
- [ ] **Step 4:** Chạy lại Step 2: xanh.
- [ ] **Step 5:** Spec 5.1: "Chỉ chạy khi không gian chưa có loại địa điểm, nhóm đồ hay mục tuỳ biến nào". Spec 5.9 dòng "Đưa lại": thêm "món đã bị thay chỉ đưa lại được khi món thay nó đã bị xoá". README dòng `POST /setup` và `/items`. Commit `Close three rule gaps: setup, put-back after a deleted replacement, delete notes`.

### Task 8: platform-common: SQL dựng một lần, gom code lặp

Chỉ đổi cấu trúc, không đổi hành vi: mọi test hiện có phải xanh mà không sửa.

**Files:**
- Modify: `platform-common/src/main/java/com/home_platform/common/jdbc/{CrudSql,EntityMetadata,KeysetQuery}.java`, `.../common/web/CursorPage.java`, `.../common/history/EntityHistory.java`; `registry-service/.../place/{PlaceRepository,PlaceAddressRepository}.java`, `.../history/{ActivityService,ActivityMapper}.java`; `identity-service/.../user/UserRepository.java`, `.../workspace/WorkspaceRepository.java`
- Create: `platform-common/src/main/java/com/home_platform/common/history/EntityChange.java`
- Delete: `registry-service/.../history/{ActivityRepository,ActivityEntry}.java`
- Test: `platform-common/src/test/java/com/home_platform/common/jdbc/{CrudSqlTests,EntityMetadataTests}.java`, `.../web/CursorPageTests.java`, `.../history/EntityHistoryIT.java`

**Interfaces (Produces):**

```java
// EntityMetadata<T>
public List<String> components()                 // cùng một danh sách không đổi được ở mọi lần gọi
public String selectList(String alias)           // "p.workspace_id, p.id, p.parent_id, ..."
public String insertInto(String table)           // "INSERT INTO t (workspace_id, id, ...) VALUES (:workspaceId, :id, ...)"
public String componentOfColumn(String column)   // "valid_from" ra "validFrom"; IllegalArgumentException nếu không có

// CursorPage<T>
/** rows là kết quả của truy vấn LIMIT limit + 1; cursorOf cho các phần của cursor từ dòng cuối được giữ. */
public static <T> CursorPage<T> of(List<T> rows, int limit, Function<? super T, List<String>> cursorOf)
// limit < 1: IllegalArgumentException("limit must be positive: <limit>")

// EntityHistory: mọi version của không gian, mới nhất trước; entityType null là mọi loại.
// Thứ tự và cursor như ActivityRepository hiện nay: changed_at, entity_type, entity_id, version_no, đều giảm dần.
public CursorPage<EntityChange> listRecent(long workspaceId, String entityType, CursorRequest page)

public record EntityChange(String entityType, long entityId, int versionNo, ChangeAction action,
		List<String> changedFields, long changedBy, Instant changedAt, String note) {}
```

- [ ] **Step 1: Test đỏ.**
  - `CrudSqlTests.statementsAreBuiltOnce`: `sql.insert()`, `sql.update()`, `sql.selectById()` mỗi cái `isSameAs` chính nó ở lần gọi thứ hai.
  - `EntityMetadataTests`: `aliasedSelectListPrefixesEveryColumn`, `insertIntoListsColumnsAndNamedParameters`, `componentsAreNotCopiedPerCall` (`isSameAs`), `componentOfColumnMapsBack`.
  - `CursorPageTests.ofCutsTheExtraRowAndEncodesTheLastKeptOne`: `of(List.of("a", "b", "c"), 2, List::of)` có `items` `["a", "b"]` và `nextCursor` bằng `CursorCodec.encode(List.of("b"))`. `ofWithoutAnExtraRowHasNoCursor`: `of(List.of("a", "b"), 2, List::of)` có `nextCursor` null. `ofRejectsANonPositiveLimit`: `limit` 0 ném `IllegalArgumentException`.
  - `EntityHistoryIT.listRecentIsNewestFirstAcrossEntities`: ghi version cho hai bản ghi khác loại ở ba thời điểm; `listRecent(WS_A, null, limit 2)` trả hai dòng mới nhất và một cursor, trang sau trả dòng còn lại; `listRecent(WS_A, "PLACE", ...)` chỉ có dòng loại đó; `WS_B` rỗng.
- [ ] **Step 2:** `./mvnw -pl platform-common test`: các test mới đỏ.
- [ ] **Step 3: Cài đặt trong platform-common.** `CrudSql` tính mọi câu trong hàm khởi tạo vào trường `final` (kể cả hai câu khoá của Task 1); `softDelete()`, `restore()`, `deletePermanently()` vẫn ném `UnsupportedOperationException` lúc được gọi như hiện nay. `CrudSql.insert` dùng `metadata.insertInto(table)`. `KeysetQuery.fetch` và `EntityHistory.list` dùng `CursorPage.of`; `KeysetQuery` dùng `componentOfColumn`. `EntityHistory.listRecent` lấy SQL và cursor của `ActivityRepository.list`, dùng tên bảng của `EntityHistory` và chung hàm đọc dòng với `list`.
- [ ] **Step 4: Áp dụng ở service.** `PlaceRepository.byArea` (`metadata().selectList("p")`, `CursorPage.of`), `PlaceAddressRepository` (`insertInto` rồi nối `" AS new ON DUPLICATE KEY UPDATE ..."`), identity `WorkspaceRepository` (`selectList("w")`, `insertInto` tính một lần trong hàm khởi tạo, `CursorPage.of`), `UserRepository.insert` (`insertInto` tính một lần). `ActivityService` gọi `history.listRecent`, `ActivityMapper.toResponse` nhận `EntityChange`; xoá `ActivityRepository` và `ActivityEntry`.
- [ ] **Step 5:** `./mvnw verify -Dit.test='*Keyset*IT,*History*IT,WorkspaceRepositoryIT,PlaceListIT,PlaceAddressIT,ActivityIT,WorkspaceIT,UserIT'`: xanh, không test cũ nào phải sửa.
- [ ] **Step 6:** README mục "Lịch sử version": thêm `listRecent` và yêu cầu index `(workspace_id, changed_at)` trên bảng version. Commit `Build CRUD statements once and share cursor-page assembly`.

### Task 9: registry: bỏ Spring Batch, một đường ghi cho sửa và khôi phục version, dọn `ItemService`, `Text`

**Files:**
- Modify: `registry-service/pom.xml`; `registry-service/.../item/{ItemService,ItemController}.java`; `.../place/PlaceService.java`, `.../person/PersonService.java`; `platform-common/.../history/EntityHistory.java`; mọi file `import com.home_platform.registry.catalog.Text`
- Create: `registry-service/.../item/ItemReplacement.java`; move `registry/catalog/Text.java` và `TextTests.java` sang `registry/support/`
- Test: `platform-common/src/test/java/com/home_platform/common/history/EntityHistoryTests.java`

**Interfaces (Produces):**

```java
// EntityHistory
/** "Restored from version 3" hoặc "Restored from version 3: <note>", cắt còn 255 ký tự của cột note. */
public static String restoredNote(int versionNo, String note)

public record ItemReplacement(Item removed, Item replacement) {}
// ItemService.replace trả ItemReplacement thay cho Item[]

// Một đường ghi nội dung cho cả update và restoreVersion: kiểm tra (loại hoặc nhóm còn dùng được, giấy tờ
// không trùng, attributes), rồi repository.update. Không ghi version; hàm gọi tự ghi.
private Place  applyContent(WorkspaceContext ctx, Place current, PlaceUpdateRequest request)    // PlaceService
private Item   applyContent(WorkspaceContext ctx, Item current, ItemUpdateRequest request)      // ItemService
private Person applyContent(WorkspaceContext ctx, Person current, PersonUpdateRequest request)  // PersonService
```

`restoreVersion` dựng `...UpdateRequest` từ snapshot (`version` lấy từ request khôi phục, ghi chú là `restoredNote`) rồi gọi `applyContent`. Request là record nên thêm một trường sửa được mà quên `restoreVersion` sẽ không biên dịch. Phần riêng giữ ở `restoreVersion`: địa chỉ của địa điểm; với món đồ, `status` của snapshot là `REMOVED` thì dùng `status` hiện tại.

- [ ] **Step 1: Test đỏ** `EntityHistoryTests.restoredNoteIsCutToTheColumnLength`: `restoredNote(3, null)` bằng `"Restored from version 3"`; `restoredNote(3, "x")` bằng `"Restored from version 3: x"`; với `note` 300 ký tự kết quả dài đúng 255.
- [ ] **Step 2: Cài đặt.** `restoredNote`; ba `applyContent` và hai hàm gọi của mỗi cái. `ItemService`: mọi trường `private final`, `import java.util.HashMap`, `replace` trả `ItemReplacement`; `ItemController.replace` dùng `removed()`, `replacement()`. Chuyển `Text` sang `registry.support` và sửa import.
- [ ] **Step 3:** Xoá hai dependency `spring-boot-starter-batch` và `spring-boot-starter-batch-test` khỏi `registry-service/pom.xml`.
- [ ] **Step 4:** `./mvnw -pl registry-service -am verify -Dit.test='Item*IT,Place*IT,Person*IT'`: xanh mà không sửa test nào (các `*VersionIT` là lưới an toàn của việc gộp đường ghi). `./mvnw -pl registry-service -am package -DskipTests` rồi chạy jar, `curl localhost:8082/actuator/health` trả `UP`; tắt service.
- [ ] **Step 5:** Commit `Drop unused Spring Batch, share one write path for edits and version restores`.

### Task 10: Mã request trong log và trong lỗi 500

**Files:**
- Create: `platform-common/src/main/java/com/home_platform/common/web/RequestLogFilter.java`; test `.../common/web/RequestLogFilterTests.java`
- Modify: `.../common/autoconfigure/PlatformWebAutoConfiguration.java`, `.../common/context/ContextArgumentResolver.java`, `.../common/web/ApiExceptionHandler.java`, `platform-common/src/main/resources/logback-spring.xml`, `README.md`
- Test: `.../common/web/ApiExceptionHandlerTests.java`, `.../common/autoconfigure/PlatformWebAutoConfigurationTests.java`, `.../common/context/ContextArgumentResolverTests.java`

**Hành vi:**
- `RequestLogFilter extends OncePerRequestFilter`. Header `X-Request-Id` được giữ nếu khớp `[A-Za-z0-9._-]{1,64}`, ngược lại thay bằng `UUID.randomUUID().toString()`. Đặt MDC `requestId`, trả header `X-Request-Id` cùng giá trị. Sau khi xử lý xong ghi một dòng INFO `"{method} {uri} -> {status} in {ms} ms"`, trừ đường dẫn bắt đầu bằng `/actuator`. Trong `finally` xoá ba khoá MDC `requestId`, `workspaceId`, `userId`.
- `ContextArgumentResolver` đặt MDC `userId` (và `workspaceId` khi là `WorkspaceContext`) sau khi lấy được người gọi. Đặt ở đây chứ không đọc header trong filter, để khi thay `ContextResolver` bằng đăng nhập thì log vẫn đúng.
- `ApiExceptionHandler.handleUnexpected`: nếu MDC có `requestId` thì thêm thuộc tính `requestId` vào Problem Details.
- `PlatformWebAutoConfiguration`: bean `FilterRegistrationBean<RequestLogFilter>` với thứ tự `Ordered.HIGHEST_PRECEDENCE + 10`.
- `logback-spring.xml`: hai mẫu text (console và file) thêm `[%X{requestId:-}]` ngay sau `${LOG_CORRELATION_PATTERN:-}`. Bản JSON đã tự mang mọi giá trị MDC.

- [ ] **Step 1: Test đỏ.**
  - `RequestLogFilterTests` (MockMvc `standaloneSetup` có `addFilters(new RequestLogFilter())`): `generatesAnIdWhenNoneIsSent` (header trả về dài 36), `keepsTheCallersId` (gửi `abc-123`, nhận `abc-123`), `replacesAnIdThatIsNotPlain` (gửi `"a b"`, nhận giá trị khác dài 36), `logContextIsEmptyAfterTheRequest` (`MDC.get("requestId")` null sau `perform`).
  - `ContextArgumentResolverTests.putsTheCallerIntoTheLogContext`: sau `resolveArgument` cho `WorkspaceContext`, `MDC.get("workspaceId")` và `MDC.get("userId")` bằng giá trị header; dọn MDC trong `@AfterEach`.
  - `ApiExceptionHandlerTests.unexpectedErrorCarriesTheRequestId`: đặt `MDC.put("requestId", "r-1")` trước khi gọi `/boom`; `$.requestId` bằng `"r-1"`.
  - `PlatformWebAutoConfigurationTests.registersTheRequestLogFilter`: context có một `FilterRegistrationBean` mà `getFilter()` là `RequestLogFilter`.
- [ ] **Step 2:** `./mvnw -pl platform-common test`: đỏ. **Step 3:** cài đặt. **Step 4:** xanh.
- [ ] **Step 5:** README, mục "Quy ước": thêm gạch đầu dòng "**Log:** mỗi request có `X-Request-Id` (nhận từ client hoặc tự sinh), có trong mọi dòng log của request và trong lỗi 500".
- [ ] **Step 6: Kết thúc kế hoạch.** `./mvnw verify` toàn dự án: xanh. Kiểm tra `SELECT COUNT(*) FROM reg_place WHERE workspace_id = -9001` bằng 0 (qua một lần chạy `ConcurrentWritesIT`, test tự xoá). Commit `Carry a request id through logs and 500 responses`, push `main`.
