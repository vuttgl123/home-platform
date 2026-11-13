# Platform Common Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bổ sung vào `platform-common` phần nền tổng quát nằm giữa request HTTP và câu SQL: người gọi, quy ước JSON, lỗi, phân trang, repository tổng quát, lịch sử version, tài liệu API, cấu hình MapStruct và bộ luật kiến trúc.

**Architecture:** Mỗi mảng là một package trong `platform-common`, bật qua auto-configuration và thay được bằng bean của service. Repository tổng quát đọc cấu trúc của một Java record một lần lúc khởi động, dựng sẵn các câu SQL cố định rồi chạy chúng qua `JdbcClient`. Test thường không cần database; test `*IT` chạy trên MySQL UAT bằng bảng tạm.

**Tech Stack:** Spring Boot 4.1.2 (Spring Framework 7.0.10, Jackson 3.1.7), Java 21, MySQL 8.0.36 + Connector/J 9.7.0, springdoc-openapi 3.2.0, MapStruct 1.6.3, ArchUnit 1.5.1, JUnit 6 + AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-12-platform-common-base-design.md`

## Global Constraints

- Truy vấn bằng `JdbcClient`. Không JPA, không Hibernate ORM.
- `platform-common` không chứa tên service hay tên bảng nào của dự án.
- Chú thích trong code viết tiếng Việt; tên lớp, tên hàm, thông báo lỗi viết tiếng Anh; trình bày như các lớp hiện có (thụt bằng tab, dùng `this.`).
- Mọi bean mặc định khai báo kèm `@ConditionalOnMissingBean`.
- Tên tham số trong SQL là tên thành phần của record (`:workspaceId`), tên cột là dạng `snake_case` của nó.
- `workspace_id` trong mọi câu SQL luôn lấy từ tham số `workspaceId` hoặc `WorkspaceContext`, không bao giờ lấy từ record truyền vào.
- Thời điểm hiện tại lấy từ bean `Clock`, cắt tới mili giây.
- JSON khi lưu (`StorageJson`): số vẫn là số. JSON trả ra API: `long` thành chuỗi.
- Header: `X-Workspace-Id`, `X-User-Id`. Phân trang: mặc định 20, tối đa 100.
- Phiên bản thư viện khai báo một lần ở pom gốc: springdoc 3.2.0, MapStruct 1.6.3, `lombok-mapstruct-binding` 0.2.0, ArchUnit 1.5.1.
- Test `*IT` chỉ dùng bảng tạm (`CREATE TEMPORARY TABLE IF NOT EXISTS`) và `workspace_id` âm; không tạo, sửa hay xoá bảng thật nào.
- Viết theo TDD: test trước, thấy nó hỏng, rồi mới viết code.
- Không commit trong lúc thực hiện; chỉ commit khi người dùng yêu cầu.

## Môi trường chạy test

- `./mvnw -pl platform-common test` chạy test thường, không cần database.
- `./mvnw -pl platform-common verify` chạy thêm `*IT` trên MySQL UAT. Đã kiểm tra ngày 2026-11-12: tài khoản database có quyền `CREATE TEMPORARY TABLES`, lệnh tạo bảng tạm không tự commit, và dữ liệu trong bảng tạm rollback được.
- Múi giờ của server MySQL là `+07`, không phải UTC. Mọi cột `DATETIME(3)` vì vậy phải được đọc ghi qua `LocalDateTime` tính theo UTC, không qua `Timestamp`.
- Chạy một lớp test: `./mvnw -pl platform-common test -Dtest=<Lớp>`; một lớp IT: `./mvnw -pl platform-common verify -Dit.test=<Lớp> -Dsurefire.skip=true`.

## Review Focus

1. Phân trang qua các dòng có giá trị sắp xếp bằng nhau theo collation của MySQL (`Phòng a`, `Phòng A`, `phòng a`) không được sót hay lặp dòng. Test: `pagingOverEqualNamesNeitherSkipsNorRepeats` (Task 8).
2. Máy chạy ở múi giờ khác UTC vẫn phải lưu và đọc đúng thời điểm. Test: `instantIsStoredAsUtcRegardlessOfJvmTimeZone` (Task 7).
3. Số lớn hơn 2^53 phải nguyên vẹn ở cả ba chỗ: JSON ra API, cột JSON, snapshot. Test: `longsAreWrittenAsStringsAndIntsAsNumbers` (Task 4), `largeNumberInJsonColumnStaysANumber` (Task 7), `recordsVersionsAndComputesChangedFields` (Task 9).
4. Dùng `id` của không gian khác để đọc, sửa, xoá phải nhận "không tồn tại" và dòng kia không đổi. Test: `rowOfAnotherWorkspaceIsInvisibleAndUntouched` (Task 7), `versionsOfOtherEntitiesAndWorkspacesStayOut` (Task 9).
5. Cursor hỏng, cursor lấy từ danh sách khác, `limit=0`, `limit=abc` phải trả 400 chứ không phải 500. Test: `garbageIsRejectedAs400` và `limitOutOfRangeOrNotANumberIs400` (Task 5), `cursorFromAnotherQueryIs400` (Task 8).

## Ngoài phạm vi

Tiện ích gọi service khác, lớp cha cho bảng không có `workspace_id`, ghi nhiều dòng một lần, bộ lọc động, geohash, lưu file, và mọi code nghiệp vụ trong ba service (trừ lớp test kiến trúc ở Task 11).

## Cấu trúc file

Đường dẫn main tính từ `platform-common/src/main/java/com/home_platform/common/`, đường dẫn test tính từ `platform-common/src/test/java/com/home_platform/`.

| File | Trách nhiệm | Task |
|---|---|---|
| `pom.xml`, `platform-common/pom.xml`, `.gitignore` (sửa) | Thư viện, bộ xử lý chú thích, failsafe | 1 |
| `mapping/PlatformMapperConfig.java` | Cấu hình chung cho mọi mapper | 1 |
| `web/BadRequestException.java`; `web/NotFoundException.java`, `web/ApiExceptionHandler.java` (sửa) | Lỗi 400, lỗi kiểm tra dữ liệu, trùng khoá | 2 |
| `context/UserContext.java`, `WorkspaceContext.java`, `ContextResolver.java`, `HeaderContextResolver.java`, `ContextArgumentResolver.java` | Ai đang gọi | 3 |
| `autoconfigure/PlatformWebAutoConfiguration.java` | Bật `context` và phân trang cho ứng dụng web | 3, 5 |
| `json/LongAsStringCustomizer.java`, `json/StorageJson.java` | Hai quy ước JSON | 4 |
| `autoconfigure/PlatformCommonAutoConfiguration.java` (sửa) | Thêm `Clock`, `StorageJson`, `LongAsStringCustomizer` | 4 |
| `web/CursorRequest.java`, `CursorCodec.java`, `CursorRequestArgumentResolver.java`, `PagingProperties.java`; `web/CursorPage.java` (sửa) | Phân trang | 5 |
| `jdbc/JsonColumn.java`, `ColumnType.java`, `ColumnTypes.java`, `EntityMetadata.java` | Cấu trúc record và chuyển kiểu cột | 6 |
| `jdbc/RepositorySupport.java`, `CrudSql.java`, `WorkspaceRepository.java` | Repository tổng quát | 7 |
| `autoconfigure/PlatformJdbcAutoConfiguration.java` | Bật `jdbc` và `history` | 7, 9 |
| `jdbc/KeysetQuery.java` | Bộ dựng truy vấn phân trang | 8 |
| `history/ChangeAction.java`, `EntityVersion.java`, `EntityVersionSummary.java`, `EntityChangedEvent.java`, `EntityHistory.java` | Lịch sử version | 9 |
| `openapi/PlatformOperationCustomizer.java`, `openapi/ProblemDetailCustomizer.java`, `autoconfigure/PlatformOpenApiAutoConfiguration.java` | Tài liệu API | 10 |
| test `common/test/AbstractRepositoryIT.java`, `common/test/PlatformArchitectureRules.java` | Đi vào test-jar cho các service | 7, 11 |
| test `common/CommonTestApplication.java`, `common/CommonIT.java`, `common/jdbc/Sample*.java` | Ứng dụng, khung và bảng mẫu cho IT của module này | 6, 7 |
| test `archfixture/**` | Lớp mẫu đúng và sai cho bộ luật | 11 |
| `<service>/pom.xml` (sửa), `<service>/src/test/java/com/home_platform/<service>/ArchitectureTests.java` | Gắn bộ luật vào ba service | 11 |
| `README.md` (sửa) | Quy ước mới | 12 |

---

### Task 1: Thư viện và cấu hình MapStruct

**Files:**
- Modify: `pom.xml`, `platform-common/pom.xml`, `.gitignore`
- Create: `mapping/PlatformMapperConfig.java`
- Test: `common/mapping/PlatformMapperConfigTests.java`

**Interfaces:**
- Produces: `PlatformMapperConfig` (interface rỗng, `@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)`).

- [ ] **Step 1: Sửa pom gốc.** Thêm bốn property phiên bản (xem Global Constraints). Thêm vào `dependencyManagement`: `org.springdoc:springdoc-openapi-starter-webmvc-ui`, `org.mapstruct:mapstruct`, `com.tngtech.archunit:archunit` (scope test). Ở cả hai execution của `maven-compiler-plugin`, `annotationProcessorPaths` theo đúng thứ tự: `lombok`, `lombok-mapstruct-binding`, `mapstruct-processor` (hai cái sau ghi rõ `<version>`).
- [ ] **Step 2: Sửa `platform-common/pom.xml`.** Thêm dependency `springdoc-openapi-starter-webmvc-ui`, `mapstruct`. Thêm scope test: `com.mysql:mysql-connector-j`, `archunit`, `jakarta.persistence:jakarta.persistence-api` (chỉ để dựng lớp mẫu vi phạm ở Task 11). Thêm `maven-failsafe-plugin` như ở các service.
- [ ] **Step 3: Sửa `.gitignore`.** Thêm dòng `**/src/test/resources/application-uat.yaml` dưới mục "Local connection settings".
- [ ] **Step 4: Viết test hỏng** trong `PlatformMapperConfigTests.java` (khai báo `SampleSource`, `SampleResponse`, `SampleMapper` cùng file):

```java
@Mapper(config = PlatformMapperConfig.class)
interface SampleMapper {
	SampleResponse toResponse(SampleSource source);
}
record SampleSource(long id, String name) {}
record SampleResponse(long id, String name) {}

@Test
void mapperIsGeneratedAsSpringBean() {
	new ApplicationContextRunner().withUserConfiguration(SampleMapperImpl.class).run(context ->
		assertThat(context.getBean(SampleMapper.class).toResponse(new SampleSource(7, "A")))
			.isEqualTo(new SampleResponse(7, "A")));
}
```

- [ ] **Step 5: Chạy.** `./mvnw -pl platform-common test -Dtest=PlatformMapperConfigTests`. Mong đợi: lỗi biên dịch, chưa có `PlatformMapperConfig`.
- [ ] **Step 6: Tạo `PlatformMapperConfig`.**
- [ ] **Step 7: Chạy lại.** Mong đợi: PASS. Sau đó `./mvnw test` ở gốc: mọi module vẫn PASS (ba service nay có thêm springdoc trên classpath).

---

### Task 2: Lỗi

**Files:**
- Create: `web/BadRequestException.java`
- Modify: `web/NotFoundException.java`, `web/ApiExceptionHandler.java`
- Test: `common/web/ApiExceptionHandlerTests.java` (thêm test và endpoint vào lớp đang có)

**Interfaces:**
- Produces:
  - `BadRequestException(String detail)`: `ErrorResponseException` với 400.
  - `static NotFoundException NotFoundException.of(String what, long id)`: detail `"<what> <id> not found"`.
  - Thân lỗi kiểm tra dữ liệu có thuộc tính `errors`: mảng `{ "field": String hoặc null, "message": String }`.
  - `DuplicateKeyException` trả 409, detail `"Duplicate value"`.

- [ ] **Step 1: Viết test hỏng.** Thêm vào `FailingController`: `GET /bad` ném `new BadRequestException("Invalid cursor")`; `POST /people` nhận `@Valid @RequestBody Person` với `record Person(@NotBlank String name, @Min(0) int age)`; `GET /search` nhận `@RequestParam @Min(1) int size`; `GET /duplicate` ném `new DuplicateKeyException("Duplicate entry 'x' for key 'uk_reg_place_type_code'")`.

```java
@Test void badRequestIsReportedAsProblemDetail()
	// GET /bad -> 400, $.status = 400, $.detail = "Invalid cursor"
@Test void notFoundFactoryBuildsMessage()
	// NotFoundException.of("Place", 42).getBody().getDetail() = "Place 42 not found"
@Test void invalidBodyListsEachField()
	// POST /people {"name":"","age":-1} -> 400, problem+json,
	// $.errors.length() = 2, $.errors[?(@.field=='name')].message có 1 phần tử,
	// $.errors[?(@.field=='age')].message có 1 phần tử
@Test void invalidParameterListsTheParameter()
	// GET /search?size=0 -> 400, $.errors[0].field = "size", $.errors[0].message không rỗng
@Test void duplicateKeyIsReportedAsConflictWithoutSqlText()
	// GET /duplicate -> 409, $.detail = "Duplicate value", thân không chứa "uk_reg"
```

- [ ] **Step 2: Chạy.** `-Dtest=ApiExceptionHandlerTests`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.** Trong `ApiExceptionHandler`: override `handleMethodArgumentNotValid` (lỗi trường lấy `field` và `defaultMessage`; lỗi toàn cục có `field` là `null`) và `handleHandlerMethodValidationException` (`field` là tên tham số); thêm `@ExceptionHandler(DuplicateKeyException.class)`. Cả hai override giữ nguyên `ProblemDetail` của lớp cha và chỉ gọi `setProperty("errors", ...)`.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS, kể cả bốn test cũ.

---

### Task 3: Ai đang gọi

**Files:**
- Create: `context/UserContext.java`, `context/WorkspaceContext.java`, `context/ContextResolver.java`, `context/HeaderContextResolver.java`, `context/ContextArgumentResolver.java`, `autoconfigure/PlatformWebAutoConfiguration.java`
- Modify: `platform-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `common/context/ContextArgumentResolverTests.java`, `common/autoconfigure/PlatformWebAutoConfigurationTests.java`

**Interfaces:**
- Consumes: `BadRequestException` (Task 2).
- Produces:

```java
public record UserContext(long userId) {}
public record WorkspaceContext(long workspaceId, long userId) {}

public interface ContextResolver {
	UserContext resolveUser(HttpServletRequest request);
	WorkspaceContext resolveWorkspace(HttpServletRequest request);
}

public class HeaderContextResolver implements ContextResolver {
	public static final String USER_HEADER = "X-User-Id";
	public static final String WORKSPACE_HEADER = "X-Workspace-Id";
}

public class ContextArgumentResolver implements HandlerMethodArgumentResolver {
	public ContextArgumentResolver(ContextResolver resolver)
}
```

  - Thông báo lỗi: `"Missing header <tên>"` và `"Header <tên> must be a number"`.
  - `PlatformWebAutoConfiguration`: `@AutoConfiguration`, `@ConditionalOnWebApplication(type = SERVLET)`; bean `ContextResolver` mặc định và một `WebMvcConfigurer` đăng ký `ContextArgumentResolver`.

- [ ] **Step 1: Viết test hỏng.** `ContextArgumentResolverTests` dùng `MockMvcBuilders.standaloneSetup` với `setCustomArgumentResolvers(new ContextArgumentResolver(new HeaderContextResolver()))` và `setControllerAdvice(new ApiExceptionHandler())`. Controller mẫu: `GET /ws` nhận `WorkspaceContext` trả `workspaceId + ":" + userId`; `GET /me` nhận `UserContext` trả `userId`.

```java
@Test void workspaceContextComesFromHeaders()   // X-Workspace-Id: 12, X-User-Id: 34 -> "12:34"
@Test void userContextNeedsOnlyUserHeader()     // GET /me, X-User-Id: 34 -> "34"
@Test void negativeIdsAreAccepted()             // X-Workspace-Id: -1, X-User-Id: 34 -> "-1:34"
@Test void missingHeaderIs400NamingTheHeader()  // GET /ws chỉ có X-User-Id -> 400, $.detail = "Missing header X-Workspace-Id"
@Test void nonNumericHeaderIs400()              // X-User-Id: abc -> 400, $.detail = "Header X-User-Id must be a number"
```

`PlatformWebAutoConfigurationTests`:

```java
@Test void providesHeaderResolverByDefault()   // WebApplicationContextRunner: bean ContextResolver là HeaderContextResolver
@Test void serviceCanReplaceTheResolver()      // withBean(ContextResolver.class, ...) -> getBean trả đúng bean đó
@Test void notActiveOutsideAWebApplication()   // ApplicationContextRunner: không có bean ContextResolver
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code** và thêm `PlatformWebAutoConfiguration` vào file `.imports`.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.

---

### Task 4: Quy ước JSON và đồng hồ

**Files:**
- Create: `json/LongAsStringCustomizer.java`, `json/StorageJson.java`
- Modify: `autoconfigure/PlatformCommonAutoConfiguration.java`
- Test: `common/json/LongAsStringCustomizerTests.java`, `common/json/StorageJsonTests.java`, `common/autoconfigure/PlatformCommonAutoConfigurationTests.java` (thêm test)

**Interfaces:**
- Produces:

```java
/** Ghi mọi long và Long ra JSON dưới dạng chuỗi. */
public class LongAsStringCustomizer implements JsonMapperBuilderCustomizer

/** JSON để lưu vào database: số vẫn là số. Có JsonMapper riêng, không phải bean JsonMapper. */
public final class StorageJson {
	public StorageJson()
	public String write(Object value)
	public <T> T read(String json, java.lang.reflect.Type type)
	public JsonNode readTree(String json)
	public <T> T convert(JsonNode tree, Class<T> type)
}
```

  - Bean mới trong `PlatformCommonAutoConfiguration`: `LongAsStringCustomizer`, `StorageJson`, `Clock` (`Clock.systemUTC()`).
  - `StorageJson` không được đăng ký như một bean kiểu `JsonMapper`, nếu không `JsonMapper` của Spring Boot sẽ lùi.

- [ ] **Step 1: Viết test hỏng.** `LongAsStringCustomizerTests` lấy `JsonMapper` từ `ApplicationContextRunner` có `JacksonAutoConfiguration` và `PlatformCommonAutoConfiguration`, với `record Sample(long id, Long parentId, int count, Instant at, LocalDate day, Map<String, Object> attrs)`.

```java
@Test void longsAreWrittenAsStringsAndIntsAsNumbers()
	// ghi Sample(9007199254740993L, null, 3, 2026-11-12T06:40:00.123Z, 2026-11-12, {"big": 9007199254740993L})
	// đọc lại bằng readTree: id là chuỗi "9007199254740993"; parentId là null; count là số 3;
	// at = "2026-11-12T06:40:00.123Z"; day = "2026-11-12"; attrs.big là chuỗi "9007199254740993"
@Test void longsAreReadFromStringsAndFromNumbers()
	// {"id":"9007199254740993","parentId":5,"count":3} -> id = 9007199254740993L, parentId = 5L
```

`StorageJsonTests`:

```java
@Test void numbersStayNumbers()       // write(Map.of("n", 9007199254740993L)) = {"n":9007199254740993}
@Test void readsBackTypedValues()     // read("{\"n\":9007199254740993}", kiểu Map<String,Object>).get("n") = 9007199254740993L
@Test void convertsTreeToType()       // convert(readTree("{\"id\":7,\"name\":\"A\"}"), Pair.class) = new Pair(7, "A")
```

Thêm vào `PlatformCommonAutoConfigurationTests`:

```java
@Test void providesUtcClockAndStorageJson()
	// getBean(Clock.class).getZone() = ZoneOffset.UTC; hasSingleBean(StorageJson.class)
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.** `LongAsStringCustomizer` đăng ký một `SimpleModule` gắn `ToStringSerializer` cho `Long.class` và `long.class`.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.

---

### Task 5: Phân trang

**Files:**
- Create: `web/CursorRequest.java`, `web/CursorCodec.java`, `web/CursorRequestArgumentResolver.java`, `web/PagingProperties.java`
- Modify: `web/CursorPage.java`, `autoconfigure/PlatformWebAutoConfiguration.java`
- Test: `common/web/CursorCodecTests.java`, `common/web/CursorRequestArgumentResolverTests.java`, `common/web/CursorPageTests.java`

**Interfaces:**
- Consumes: `BadRequestException` (Task 2).
- Produces:

```java
public record CursorRequest(String cursor, int limit) {}   // cursor null nghĩa là trang đầu

@ConfigurationProperties("platform.paging")
public record PagingProperties(@DefaultValue("20") int defaultLimit, @DefaultValue("100") int maxLimit) {}

public final class CursorCodec {
	public static String encode(List<String> parts);    // Base64URL không đệm của một mảng JSON các chuỗi
	public static List<String> decode(String cursor);   // sai dạng -> BadRequestException("Invalid cursor")
}

public class CursorRequestArgumentResolver implements HandlerMethodArgumentResolver {
	public CursorRequestArgumentResolver(PagingProperties properties)
}

// trong CursorPage<T>
public <R> CursorPage<R> map(Function<? super T, ? extends R> mapper)
```

  - Tham số URL: `cursor`, `limit`. `limit` không phải số hoặc ngoài `1..maxLimit`: `BadRequestException("limit must be between 1 and <maxLimit>")`.
  - `PlatformWebAutoConfiguration` thêm `@EnableConfigurationProperties(PagingProperties.class)` và đăng ký resolver này.

- [ ] **Step 1: Viết test hỏng.**

```java
// CursorCodecTests
@Test void roundTripsPartsIncludingVietnamese()
	// decode(encode(List.of("Phòng họp – tầng 3", "123"))) = danh sách ban đầu;
	// encode(...) khớp [A-Za-z0-9_-]+
@Test void garbageIsRejectedAs400()
	// với mỗi giá trị: "", "!!!", Base64URL của {"a":1}, Base64URL của [1,2], Base64URL của "abc"
	// decode ném BadRequestException có detail "Invalid cursor"

// CursorRequestArgumentResolverTests: standaloneSetup, PagingProperties(20, 100),
// controller GET /list nhận CursorRequest trả cursor + ":" + limit
@Test void defaultsApplyWhenNothingIsSent()       // GET /list -> "null:20"
@Test void cursorAndLimitAreRead()                // ?cursor=abc&limit=50 -> "abc:50"
@Test void limitOutOfRangeOrNotANumberIs400()     // limit=0, limit=101, limit=abc -> 400, $.detail = "limit must be between 1 and 100"

// CursorPageTests
@Test void mapConvertsItemsAndKeepsCursor()
	// new CursorPage<>(List.of(1, 2), "c").map(String::valueOf) = new CursorPage<>(List.of("1", "2"), "c")
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.

---

### Task 6: Cấu trúc record và chuyển kiểu cột

**Files:**
- Create: `jdbc/JsonColumn.java`, `jdbc/ColumnType.java`, `jdbc/ColumnTypes.java`, `jdbc/EntityMetadata.java`
- Test: `common/jdbc/EntityMetadataTests.java`; mẫu dùng chung `common/jdbc/SampleStatus.java`, `SampleDoc.java`, `SampleTag.java`

**Interfaces:**
- Consumes: `StorageJson` (Task 4).
- Produces:

```java
@Retention(RUNTIME) @Target(RECORD_COMPONENT)
public @interface JsonColumn {}

/** Cách một kiểu Java đi vào và ra khỏi JDBC. Dùng trong package. */
interface ColumnType {
	Object toJdbc(Object value);                                   // value khác null
	Object read(ResultSet rs, String column) throws SQLException;  // trả null khi cột NULL
	String toText(Object value);                                   // dùng trong cursor
	Object fromText(String text);                                  // sai dạng -> IllegalArgumentException
}

public final class EntityMetadata<T extends Record> {
	public static <T extends Record> EntityMetadata<T> of(Class<T> type, StorageJson json)
	public Class<T> type()
	public List<String> components()                      // theo thứ tự khai báo
	public boolean has(String component)
	public String column(String component)                // "validFrom" -> "valid_from"
	public String selectList()                            // các cột, cách nhau bằng ", "
	public RowMapper<T> rowMapper()                       // đọc theo tên cột
	public Map<String, Object> parameters(T entity)       // tên thành phần -> giá trị cho JDBC, giữ cả null
	public Object get(T entity, String component)
	public T with(T entity, Map<String, Object> changes)  // bản sao, thay các thành phần được nêu
	ColumnType typeOfColumn(String column)                // dùng trong package; cột lạ -> IllegalArgumentException
}
```

  - Kiểu hỗ trợ và giá trị JDBC: `long`/`Long`, `int`/`Integer`, `boolean`/`Boolean`, `String`, `BigDecimal` giữ nguyên; `Instant` thành `LocalDateTime` theo UTC; `LocalDate` giữ nguyên; enum thành `name()`; `@JsonColumn` thành chuỗi của `StorageJson.write`.
  - Kiểu khác: `IllegalArgumentException` với thông báo `"Unsupported type <tên kiểu> for <Record>.<thành phần>"`.
  - Mẫu dùng chung cho Task 6 đến 8:

```java
enum SampleStatus { ACTIVE, ARCHIVED }

@With
record SampleDoc(long workspaceId, long id, Long parentId, String name, SampleStatus status, boolean pinned,
		Integer rating, BigDecimal price, LocalDate validFrom, @JsonColumn Map<String, Object> attributes,
		Instant createdAt, long createdBy, Instant updatedAt, long updatedBy, int version, Instant deletedAt) {}

@With
record SampleTag(long workspaceId, long id, String code, String name, Instant createdAt, Instant updatedAt) {}
```

- [ ] **Step 1: Viết test hỏng.**

```java
@Test void columnsAreSnakeCaseInDeclarationOrder()
	// of(SampleDoc).selectList() = "workspace_id, id, parent_id, name, status, pinned, rating, price, "
	//   + "valid_from, attributes, created_at, created_by, updated_at, updated_by, version, deleted_at"
	// với record Sha(long workspaceId, long id, String checksumSha256): column("checksumSha256") = "checksum_sha256"
@Test void parametersUseJdbcFriendlyValues()
	// SampleDoc có status ACTIVE, pinned true, parentId null, createdAt 2026-11-12T06:40:00.123Z,
	// attributes {"n": 9007199254740993L}:
	// "status" -> "ACTIVE"; "pinned" -> true; có khoá "parentId" với giá trị null;
	// "createdAt" -> LocalDateTime.of(2026, 11, 12, 6, 40, 0, 123_000_000);
	// "attributes" -> "{\"n\":9007199254740993}"
@Test void withReplacesOnlyNamedComponents()
	// with(doc, Map.of("version", 2, "name", "B")): version 2, name "B", các thành phần khác bằng doc
@Test void unsupportedTypeFailsNamingRecordAndComponent()
	// record Bad(long workspaceId, long id, UUID token): of(Bad) ném IllegalArgumentException,
	// thông báo chứa "Bad.token" và "java.util.UUID"
@Test void unknownComponentIsRejected()   // column("nope") ném IllegalArgumentException
```

- [ ] **Step 2: Chạy.** `-Dtest=EntityMetadataTests`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.** `with` dựng lại record qua hàm khởi tạo chính tắc. `rowMapper` được kiểm tra trên database ở Task 7.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.

---

### Task 7: Repository tổng quát

**Files:**
- Create: `jdbc/RepositorySupport.java`, `jdbc/CrudSql.java`, `jdbc/WorkspaceRepository.java`, `autoconfigure/PlatformJdbcAutoConfiguration.java`
- Modify: file `.imports`
- Create (test): `common/CommonTestApplication.java`, `common/CommonIT.java`, `common/test/AbstractRepositoryIT.java`, `common/jdbc/SampleDocRepository.java`, `common/jdbc/SampleTagRepository.java`, `platform-common/src/test/resources/application-uat.yaml` (chỉ trên máy)
- Test: `common/jdbc/CrudSqlTests.java`, `common/jdbc/WorkspaceRepositoryTests.java`, `common/jdbc/WorkspaceRepositoryIT.java`, `common/autoconfigure/PlatformJdbcAutoConfigurationTests.java`

**Interfaces:**
- Consumes: `EntityMetadata`, mẫu `SampleDoc`/`SampleTag` (Task 6); `WorkspaceContext` (Task 3); `StorageJson`, `Clock` (Task 4); `IdGenerator`, `NotFoundException`, `ConflictException` (đã có).
- Produces:

```java
public record RepositorySupport(JdbcClient jdbc, IdGenerator idGenerator, Clock clock, StorageJson json) {}

/** Dùng trong package: các câu SQL cố định của một bảng. */
final class CrudSql {
	CrudSql(String table, EntityMetadata<?> metadata)
	String insert(); String selectById(); String selectByIdIncludingDeleted(); String update();
	String softDelete(); String restore(); String deletePermanently();
	boolean versioned(); boolean softDeletable();
}

public abstract class WorkspaceRepository<T extends Record> {
	protected WorkspaceRepository(RepositorySupport support, String table, Class<T> type)
	public T insert(WorkspaceContext ctx, T entity)
	public Optional<T> findById(long workspaceId, long id)
	public T getById(long workspaceId, long id)
	public Optional<T> findByIdIncludingDeleted(long workspaceId, long id)
	public T update(WorkspaceContext ctx, T entity)
	public T softDelete(WorkspaceContext ctx, long id, int expectedVersion)
	public T restore(WorkspaceContext ctx, long id, int expectedVersion)
	public boolean deletePermanently(long workspaceId, long id)   // true nếu có dòng bị xoá
	protected JdbcClient jdbc()
	protected RowMapper<T> rowMapper()
	protected String columns()    // metadata.selectList()
	protected String table()
	protected EntityMetadata<T> metadata()
}
```

  - Hàm khởi tạo ném `IllegalArgumentException("<Record> must declare long workspaceId and long id")` nếu record thiếu một trong hai, và ghi các câu SQL ra log mức DEBUG.
  - Cột chuẩn theo bảng ở mục 5.5 của spec. Cột nghiệp vụ là mọi cột trừ `workspace_id`, `id`, `created_at`, `created_by`, `updated_at`, `updated_by`, `version`, `deleted_at`.
  - `insert` và `update` trả về bản sao của record truyền vào với cột chuẩn đã điền; không đọc lại database. `softDelete` và `restore` đọc lại dòng sau khi ghi.
  - Không trúng dòng nào: gọi `findByIdIncludingDeleted`. Không có, hoặc đã xoá mềm (với `update`, `softDelete`): `NotFoundException.of(<tên record>, id)`. Có nhưng lệch `version`: `ConflictException("<tên record> <id> was changed by someone else")`. `restore` trên dòng chưa xoá: `ConflictException("<tên record> <id> is not deleted")`.
  - `softDelete`, `restore` trên bảng không có `deleted_at`, và `deletePermanently` trên bảng có `deleted_at`: `UnsupportedOperationException`.
  - `PlatformJdbcAutoConfiguration`: `@AutoConfiguration(after = JdbcClientAutoConfiguration.class)`, bean `RepositorySupport` với `@ConditionalOnBean(JdbcClient.class)`.
  - Khung test:

```java
// test-jar, cho mọi service
@SpringBootTest
@Transactional
public abstract class AbstractRepositoryIT {
	protected static final long WS_A = -1L;
	protected static final long WS_B = -2L;
	protected static final long USER = -101L;
	protected static final WorkspaceContext CTX_A = new WorkspaceContext(WS_A, USER);
	protected static final WorkspaceContext CTX_B = new WorkspaceContext(WS_B, USER);
	@Autowired protected JdbcClient jdbc;
}

// chỉ của platform-common
@ActiveProfiles("uat")
@TestPropertySource(properties = "platform.history.table=tmp_entity_version")
@Import(CommonIT.FixedClock.class)
abstract class CommonIT extends AbstractRepositoryIT {
	static final Instant NOW = Instant.parse("2026-11-12T06:40:00.123Z");
	// FixedClock: bean Clock cố định tại 2026-11-12T06:40:00.123456Z (có phần micro để thử việc cắt)
	// @BeforeEach: tạo ba bảng tạm bên dưới bằng CREATE TEMPORARY TABLE IF NOT EXISTS
}
```

  - Bảng tạm (`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`):

```sql
tmp_sample_doc (workspace_id BIGINT NOT NULL, id BIGINT NOT NULL, parent_id BIGINT NULL, name VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL, pinned TINYINT(1) NOT NULL, rating INT NULL, price DECIMAL(12,2) NULL,
  valid_from DATE NULL, attributes JSON NULL, created_at DATETIME(3) NOT NULL, created_by BIGINT NOT NULL,
  updated_at DATETIME(3) NOT NULL, updated_by BIGINT NOT NULL, version INT NOT NULL, deleted_at DATETIME(3) NULL,
  PRIMARY KEY (workspace_id, id), KEY idx_tmp_doc_parent (workspace_id, parent_id, name))

tmp_sample_tag (workspace_id BIGINT NOT NULL, id BIGINT NOT NULL, code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL,
  created_at DATETIME(3) NOT NULL, updated_at DATETIME(3) NOT NULL,
  PRIMARY KEY (workspace_id, id), UNIQUE KEY uk_tmp_tag_code (workspace_id, code))

tmp_entity_version (workspace_id BIGINT NOT NULL, entity_type VARCHAR(20) NOT NULL, entity_id BIGINT NOT NULL,
  version_no INT NOT NULL, action VARCHAR(10) NOT NULL, snapshot JSON NOT NULL, changed_fields JSON NULL,
  changed_by BIGINT NOT NULL, changed_at DATETIME(3) NOT NULL, note VARCHAR(255) NULL,
  PRIMARY KEY (workspace_id, entity_type, entity_id, version_no))
```

- [ ] **Step 1: Viết test thường hỏng.**

```java
// CrudSqlTests, với tag = new CrudSql("tmp_sample_tag", of(SampleTag)), doc = new CrudSql("tmp_sample_doc", of(SampleDoc))
@Test void statementsForAPlainTable()
	// tag.insert() = "INSERT INTO tmp_sample_tag (workspace_id, id, code, name, created_at, updated_at) "
	//   + "VALUES (:workspaceId, :id, :code, :name, :createdAt, :updatedAt)"
	// tag.selectById() = "SELECT workspace_id, id, code, name, created_at, updated_at FROM tmp_sample_tag "
	//   + "WHERE workspace_id = :workspaceId AND id = :id"
	// tag.update() = "UPDATE tmp_sample_tag SET code = :code, name = :name, updated_at = :updatedAt "
	//   + "WHERE workspace_id = :workspaceId AND id = :id"
	// tag.deletePermanently() = "DELETE FROM tmp_sample_tag WHERE workspace_id = :workspaceId AND id = :id"
	// tag.versioned() = false; tag.softDeletable() = false
@Test void statementsForATableWithStandardColumns()
	// doc.selectById() kết thúc bằng "WHERE workspace_id = :workspaceId AND id = :id AND deleted_at IS NULL"
	// doc.selectByIdIncludingDeleted() kết thúc bằng "WHERE workspace_id = :workspaceId AND id = :id"
	// doc.update() = "UPDATE tmp_sample_doc SET parent_id = :parentId, name = :name, status = :status, "
	//   + "pinned = :pinned, rating = :rating, price = :price, valid_from = :validFrom, attributes = :attributes, "
	//   + "updated_at = :updatedAt, updated_by = :updatedBy, version = version + 1 "
	//   + "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NULL"
	// doc.softDelete() = "UPDATE tmp_sample_doc SET deleted_at = :deletedAt, updated_at = :updatedAt, "
	//   + "updated_by = :updatedBy, version = version + 1 "
	//   + "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NULL"
	// doc.restore() = "UPDATE tmp_sample_doc SET deleted_at = NULL, updated_at = :updatedAt, "
	//   + "updated_by = :updatedBy, version = version + 1 "
	//   + "WHERE workspace_id = :workspaceId AND id = :id AND version = :version AND deleted_at IS NOT NULL"

// WorkspaceRepositoryTests (JdbcClient giả)
@Test void recordWithoutWorkspaceIdAndIdIsRejected()
	// record NoKey(long id, String name): tạo repository ném IllegalArgumentException
	// có thông báo "NoKey must declare long workspaceId and long id"

// PlatformJdbcAutoConfigurationTests
@Test void repositorySupportNeedsAJdbcClient()
	// không có bean JdbcClient -> không có RepositorySupport; có JdbcClient giả -> có đúng một RepositorySupport
```

- [ ] **Step 2: Chạy.** `-Dtest='CrudSqlTests,WorkspaceRepositoryTests,PlatformJdbcAutoConfigurationTests'`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết `RepositorySupport`, `CrudSql`, `WorkspaceRepository`, `PlatformJdbcAutoConfiguration`.** Chạy lại lệnh trên. Mong đợi: PASS.
- [ ] **Step 4: Dựng khung IT.** Tạo `CommonTestApplication` (`@SpringBootConfiguration` + `@EnableAutoConfiguration`), `AbstractRepositoryIT`, `CommonIT`, hai repository mẫu. Tạo `platform-common/src/test/resources/application-uat.yaml` bằng cách chép phần `spring.datasource` từ `registry-service/src/main/resources/application-uat.yaml`. Kiểm tra `git status` không liệt kê file này.
- [ ] **Step 5: Viết IT hỏng** trong `WorkspaceRepositoryIT extends CommonIT`.

```java
@Test void insertFillsStandardColumnsAndReturnsWhatIsStored()
	// thêm doc: id 0, name "Phòng họp 🏢", status ACTIVE, pinned true, rating 4, price 12.50,
	// validFrom 2026-11-01, attributes {"area": 42}
	// kết quả: id > 0; workspaceId = WS_A; version = 1; createdBy = updatedBy = USER;
	// createdAt = updatedAt = NOW; deletedAt = null
	// docs.findById(WS_A, id) chứa đúng record đó (so bằng equals)
@Test void presetIdIsKept()                       // thêm với id 555 -> kết quả có id 555
@Test void nullableColumnsRoundTripAsNull()       // parentId, rating, price, validFrom, attributes đều null -> đọc lên vẫn null
@Test void largeNumberInJsonColumnStaysANumber()
	// attributes {"big": 9007199254740993L};
	// SELECT JSON_TYPE(JSON_EXTRACT(attributes, '$.big')) = "INTEGER"; đọc lên get("big") = 9007199254740993L
@Test void instantIsStoredAsUtcRegardlessOfJvmTimeZone()
	// đặt TimeZone mặc định là Asia/Ho_Chi_Minh trong test (trả lại sau); thêm doc;
	// SELECT CAST(created_at AS CHAR) = "2026-11-12 06:40:00.123"; findById(...).createdAt() = NOW
@Test void updateWritesBusinessColumnsAndBumpsVersion()
	// sửa bằng context có userId -202: name "B", attributes {"k": "v"}
	// kết quả: version 2, updatedBy -202, createdBy USER; findById trả đúng kết quả đó
@Test void staleVersionIsAConflict()
	// sửa hai lần từ cùng một bản đã đọc: lần hai ném ConflictException; name trong database là của lần một
@Test void updateOfMissingRowIsNotFound()         // id 999 không tồn tại -> NotFoundException
@Test void rowOfAnotherWorkspaceIsInvisibleAndUntouched()
	// a = thêm bằng CTX_A; findById(WS_B, a.id()) rỗng;
	// update(CTX_B, a.withName("x")), softDelete(CTX_B, a.id(), 1) đều ném NotFoundException;
	// findById(WS_A, a.id()) vẫn bằng a
@Test void softDeleteHidesTheRowAndRestoreBringsItBack()
	// softDelete(CTX_A, id, 1): version 2, deletedAt = NOW; findById rỗng; getById ném NotFoundException;
	// findByIdIncludingDeleted có; update và softDelete tiếp theo ném NotFoundException;
	// restore(CTX_A, id, 2): version 3, deletedAt null; findById có;
	// restore(CTX_A, id, 3) ném ConflictException
@Test void softDeleteWithStaleVersionIsAConflict()   // softDelete(CTX_A, id, 99) ném ConflictException; dòng vẫn còn
@Test void tableWithoutStandardColumnsStillWorks()
	// SampleTag: insert điền createdAt, updatedAt = NOW; update hai lần từ cùng một bản đều thành công;
	// deletePermanently trả true rồi false; softDelete ném UnsupportedOperationException
@Test void deletePermanentlyIsRefusedOnSoftDeletableTable()   // docs.deletePermanently ném UnsupportedOperationException
@Test void duplicateKeySurfacesAsDuplicateKeyException()      // hai tag cùng code trong WS_A -> DuplicateKeyException
```

- [ ] **Step 6: Chạy.** `./mvnw -pl platform-common verify -Dit.test=WorkspaceRepositoryIT -Dsurefire.skip=true`. Mong đợi: có test hỏng (hành vi chưa đủ) hoặc PASS nếu Step 3 đã đủ; sửa `WorkspaceRepository` và `ColumnTypes` tới khi PASS.
- [ ] **Step 7: Chạy toàn bộ module.** `./mvnw -pl platform-common verify`. Mong đợi: PASS.

---

### Task 8: Truy vấn phân trang

**Files:**
- Create: `jdbc/KeysetQuery.java`
- Modify: `jdbc/WorkspaceRepository.java` (thêm `protected KeysetQuery<T> keyset()`), `common/jdbc/SampleDocRepository.java` (test)
- Test: `common/jdbc/KeysetQueryTests.java`, `common/jdbc/KeysetQueryIT.java`

**Interfaces:**
- Consumes: `CursorRequest`, `CursorCodec`, `CursorPage` (Task 5); `EntityMetadata`, `ColumnType` (Task 6); `WorkspaceRepository` (Task 7).
- Produces:

```java
public final class KeysetQuery<T extends Record> {
	public KeysetQuery<T> where(String condition, Map<String, ?> params)
	public KeysetQuery<T> orderBy(String column)       // tăng dần
	public KeysetQuery<T> orderByDesc(String column)
	public CursorPage<T> fetch(long workspaceId, CursorRequest page)
	String sql(boolean afterCursor)                    // dùng trong package, để test
}
```

  - Tên tham số dành riêng: `workspaceId`, `cursorSort`, `cursorId`, `limit`. `where` dùng trùng tên: `IllegalArgumentException`.
  - `orderBy` với cột không thuộc record: `IllegalArgumentException`.
  - Cursor: `[giá trị sắp xếp dạng chuỗi, id]`, hoặc `[id]` khi không gọi `orderBy`. Số phần tử sai hoặc giá trị không đổi được về kiểu của cột: `BadRequestException("Invalid cursor")`.
  - Lấy `limit + 1` dòng để biết còn trang sau; `nextCursor` dựng từ dòng cuối của trang.
  - Mẫu thêm vào `SampleDocRepository`: `listByParent(long workspaceId, long parentId, CursorRequest page)` sắp theo `name`; `listByParentDesc(...)` sắp theo `name` giảm dần; `listAll(long workspaceId, CursorRequest page)` không `orderBy`.

- [ ] **Step 1: Viết test thường hỏng** (`KeysetQueryTests`, `<cols>` là `selectList()` của `SampleDoc`).

```java
@Test void sqlForNameOrder()
	// where("parent_id = :parentId", ...).orderBy("name"):
	// sql(false) = "SELECT <cols> FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND deleted_at IS NULL "
	//   + "AND (parent_id = :parentId) ORDER BY name ASC, id ASC LIMIT :limit"
	// sql(true) = "... AND (parent_id = :parentId) "
	//   + "AND (name > :cursorSort OR (name = :cursorSort AND id > :cursorId)) ORDER BY name ASC, id ASC LIMIT :limit"
@Test void sqlForDescendingOrder()
	// orderByDesc("name"): sql(true) chứa "(name < :cursorSort OR (name = :cursorSort AND id < :cursorId)) "
	//   + "ORDER BY name DESC, id DESC LIMIT :limit"
@Test void sqlWithoutOrderByUsesId()
	// sql(true) kết thúc bằng "AND id > :cursorId ORDER BY id ASC LIMIT :limit"
@Test void tableWithoutDeletedAtHasNoDeletedFilter()   // với SampleTag: sql(false) không chứa "deleted_at"
@Test void unknownSortColumnIsRejected()               // orderBy("nope") ném IllegalArgumentException
@Test void reservedParameterNamesAreRejected()         // where("x = :limit", Map.of("limit", 1)) ném IllegalArgumentException
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết `KeysetQuery` và `keyset()`.** Chạy lại. Mong đợi: PASS.
- [ ] **Step 4: Viết IT hỏng** (`KeysetQueryIT extends CommonIT`).

```java
@Test void pagesThroughAllRowsInNameOrder()
	// WS_A, cha 1: "E", "C", "A", "D", "B", thêm một dòng "Z" đã xoá mềm; cha 2: "X"; WS_B, cha 1: "Y"
	// limit 2: trang 1 [A, B] có nextCursor; trang 2 [C, D] có nextCursor; trang 3 [E], nextCursor null
@Test void exactMultipleOfLimitEndsWithoutEmptyPage()   // 4 dòng, limit 2: trang 2 có nextCursor null
@Test void pagingOverEqualNamesNeitherSkipsNorRepeats()
	// 5 dòng tên "Phòng a", "Phòng A", "phòng a", "Phòng a", "PHÒNG A"; limit 2;
	// gom id qua các trang: đúng 5 id đã thêm, không trùng
@Test void descendingOrderWorks()                       // [E, D], [C, B], [A]
@Test void withoutOrderByRowsComeInIdOrder()            // id tăng dần qua các trang
@Test void cursorFromAnotherQueryIs400()
	// nextCursor của listAll đưa vào listByParent: BadRequestException; cursor "garbage": BadRequestException
@Test void emptyResultIsAnEmptyPage()                   // items rỗng, nextCursor null
```

- [ ] **Step 5: Chạy.** `-Dit.test=KeysetQueryIT -Dsurefire.skip=true`. Sửa tới khi PASS.

---

### Task 9: Lịch sử version

**Files:**
- Create: `history/ChangeAction.java`, `history/EntityVersion.java`, `history/EntityVersionSummary.java`, `history/EntityChangedEvent.java`, `history/EntityHistory.java`
- Modify: `autoconfigure/PlatformJdbcAutoConfiguration.java`
- Test: `common/history/EntityHistoryIT.java`, `common/autoconfigure/PlatformJdbcAutoConfigurationTests.java` (thêm test)

**Interfaces:**
- Consumes: `RepositorySupport` (Task 7); `CursorRequest`, `CursorPage`, `CursorCodec` (Task 5); `WorkspaceContext` (Task 3); bảng tạm `tmp_entity_version` và `CommonIT` (Task 7).
- Produces:

```java
public enum ChangeAction { CREATE, UPDATE, DELETE, RESTORE }

public record EntityVersionSummary(int versionNo, ChangeAction action, List<String> changedFields,
		long changedBy, Instant changedAt, String note) {}

public record EntityVersion(int versionNo, ChangeAction action, JsonNode snapshot, List<String> changedFields,
		long changedBy, Instant changedAt, String note) {}

public record EntityChangedEvent(long workspaceId, String entityType, long entityId, int versionNo,
		ChangeAction action, long changedBy, Instant changedAt) {}

public class EntityHistory {
	public EntityHistory(RepositorySupport support, ApplicationEventPublisher events, String table)
	public void record(WorkspaceContext ctx, String entityType, long entityId, int versionNo,
			ChangeAction action, Object snapshot, String note)
	public CursorPage<EntityVersionSummary> list(long workspaceId, String entityType, long entityId, CursorRequest page)
	public EntityVersion get(long workspaceId, String entityType, long entityId, int versionNo)
	public <S> S snapshotAs(EntityVersion version, Class<S> type)
}
```

  - `record` ngoài transaction: `IllegalTransactionStateException` (kiểm tra bằng `TransactionSynchronizationManager.isActualTransactionActive()`).
  - `changed_fields`: so `snapshot` mới với `snapshot` của dòng có `version_no` lớn nhất nhỏ hơn `versionNo`; lấy tên các trường cấp một có giá trị khác, kể cả trường thêm hoặc mất. Với `CREATE` hoặc không có dòng trước: `NULL`.
  - `list` sắp `version_no` giảm dần; cursor là `[version_no]`.
  - `get` không có dòng: `NotFoundException("<entityType> <entityId> version <versionNo> not found")`.
  - Bean `EntityHistory` trong `PlatformJdbcAutoConfiguration`: `@ConditionalOnProperty("platform.history.table")` và `@ConditionalOnBean(JdbcClient.class)`. Tên bảng không khớp `[A-Za-z0-9_]+`: `IllegalArgumentException` có chứa `"platform.history.table"`.

- [ ] **Step 1: Viết test hỏng.** IT dùng `record PlaceSnapshot(long id, String name, String note, Map<String, Object> attributes)` và `@RecordApplicationEvents`.

```java
@Test void recordsVersionsAndComputesChangedFields()
	// record(CTX_A, "PLACE", 10, 1, CREATE, PlaceSnapshot(10, "A", null, {"big": 9007199254740993L}), null)
	// record(CTX_A, "PLACE", 10, 2, UPDATE, PlaceSnapshot(10, "B", null, {"big": 9007199254740993L}), "đổi tên")
	// record(CTX_A, "PLACE", 10, 3, UPDATE, PlaceSnapshot(10, "B", "ghi chú", {"big": 9007199254740993L}), null)
	// get(..., 1): action CREATE, changedFields null, changedBy USER, changedAt NOW,
	//   snapshot.get("name") là "A", snapshot.get("id") là số, snapshot.get("attributes").get("big") = 9007199254740993
	// get(..., 2): changedFields = ["name"], note "đổi tên"
	// get(..., 3): changedFields = ["note"]
	// snapshotAs(get(..., 2), PlaceSnapshot.class) = snapshot thứ hai
@Test void listReturnsNewestFirstInPages()
	// 3 version; limit 2: trang 1 có versionNo [3, 2] và nextCursor; trang 2 [1], nextCursor null
@Test void versionsOfOtherEntitiesAndWorkspacesStayOut()
	// ghi (WS_A, PLACE, 10), (WS_A, ITEM, 10), (WS_B, PLACE, 10), mỗi cái một version với name khác nhau
	// list(WS_A, "PLACE", 10) có 1 phần tử; get(WS_B, "PLACE", 10, 1) mang name của WS_B;
	// get(WS_A, "PERSON", 10, 1) ném NotFoundException
@Test void publishesAnEventAfterRecording()
	// sau record(CTX_A, "PLACE", 10, 1, CREATE, ...): sự kiện EntityChangedEvent duy nhất bằng
	// new EntityChangedEvent(WS_A, "PLACE", 10, 1, CREATE, USER, NOW)
@Test void recordingTheSameVersionTwiceFails()             // DuplicateKeyException
@Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
void recordOutsideATransactionIsRefused()                  // IllegalTransactionStateException
```

Thêm vào `PlatformJdbcAutoConfigurationTests`:

```java
@Test void historyIsOffWithoutTheProperty()     // có JdbcClient giả, không có property -> không có EntityHistory
@Test void historyIsOnWithTheProperty()         // platform.history.table=reg_entity_version -> có đúng một EntityHistory
@Test void strangeTableNameStopsStartup()       // platform.history.table="x; DROP" -> context hỏng, lỗi chứa "platform.history.table"
```

- [ ] **Step 2: Chạy.** Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết code.**
- [ ] **Step 4: Chạy lại.** `-Dtest=PlatformJdbcAutoConfigurationTests`, rồi `-Dit.test=EntityHistoryIT -Dsurefire.skip=true`. Mong đợi: PASS.

---

### Task 10: Tài liệu API

**Files:**
- Create: `openapi/PlatformOperationCustomizer.java`, `openapi/ProblemDetailCustomizer.java`, `autoconfigure/PlatformOpenApiAutoConfiguration.java`
- Modify: file `.imports`
- Test: `common/openapi/PlatformOpenApiTests.java`

**Interfaces:**
- Consumes: `WorkspaceContext`, `UserContext`, `HeaderContextResolver` (Task 3); `CursorRequest`, `PagingProperties` (Task 5).
- Produces:
  - `PlatformOperationCustomizer implements GlobalOperationCustomizer`: thêm tham số header cho API nhận `WorkspaceContext` hoặc `UserContext`, thêm tham số `cursor` và `limit` cho API nhận `CursorRequest`, thêm phản hồi `default` kiểu `application/problem+json` trỏ tới schema `ProblemDetail`.
  - `ProblemDetailCustomizer implements GlobalOpenApiCustomizer`: thêm schema `ProblemDetail` (`type`, `title`, `status`, `detail`, `instance`) vào `components`.
  - `PlatformOpenApiAutoConfiguration`: `@ConditionalOnClass` trên lớp của springdoc và `@ConditionalOnWebApplication`; bean `OpenAPI` có `info.title` là `spring.application.name`; bỏ qua ba kiểu `WorkspaceContext`, `UserContext`, `CursorRequest` khi sinh tham số; thay schema của `Long` và `long` bằng `string`.

- [ ] **Step 1: Viết test hỏng.** `@SpringBootTest` + `@AutoConfigureMockMvc` với lớp cấu hình lồng bên trong (`@SpringBootConfiguration`, `@EnableAutoConfiguration` loại `DataSourceAutoConfiguration`), property `spring.application.name=sample-service`, và controller mẫu:

```java
record ThingResponse(long id, String name, int count) {}
@GetMapping("/things/{id}") ThingResponse get(WorkspaceContext ctx, @PathVariable long id)
@GetMapping("/things")      CursorPage<ThingResponse> list(WorkspaceContext ctx, CursorRequest page)
@GetMapping("/me")          String me(UserContext user)
```

```java
@Test void documentDescribesContextPagingAndIds()   // GET /v3/api-docs -> 200
	// $.info.title = "sample-service"
	// /things/{id} GET: tên tham số là đúng ba cái "X-Workspace-Id", "X-User-Id", "id";
	//   hai header có in = "header", required = true; schema của "id" có type string
	// /things GET: tên tham số là đúng bốn cái "X-Workspace-Id", "X-User-Id", "cursor", "limit";
	//   "limit" có schema integer, default 20, maximum 100; "cursor" và "limit" không bắt buộc
	// /me GET: tên tham số là đúng một cái "X-User-Id"
	// components.schemas.ThingResponse.properties: id có type string, count có type integer
	// /things/{id} GET responses.default.content["application/problem+json"].schema.$ref
	//   = "#/components/schemas/ProblemDetail"; components.schemas.ProblemDetail tồn tại
```

- [ ] **Step 2: Chạy.** `-Dtest=PlatformOpenApiTests`. Mong đợi: FAIL (tiêu đề và tham số chưa đúng).
- [ ] **Step 3: Viết code.** Đọc Javadoc của `SpringDocUtils`, `GlobalOperationCustomizer`, `GlobalOpenApiCustomizer` trong springdoc 3.2.0 để dùng đúng hàm; nếu một trong ba không còn tồn tại ở bản này, dùng cơ chế tương đương của nó và giữ nguyên các khẳng định của test.
- [ ] **Step 4: Chạy lại.** Mong đợi: PASS.

---

### Task 11: Bộ luật kiến trúc

**Files:**
- Create (test): `common/test/PlatformArchitectureRules.java`, `common/arch/PlatformArchitectureRulesTests.java`, các lớp mẫu trong `archfixture/`
- Modify: `identity-service/pom.xml`, `registry-service/pom.xml`, `media-service/pom.xml` (thêm `archunit` scope test)
- Create: `<service>/src/test/java/com/home_platform/<service>/ArchitectureTests.java` cho cả ba service

**Interfaces:**
- Consumes: `WorkspaceRepository` (Task 7), `WorkspaceContext` (Task 3).
- Produces:

```java
public final class PlatformArchitectureRules {
	public static final ArchRule NO_JPA;
	public static final ArchRule REPOSITORY_METHODS_START_WITH_WORKSPACE;
	public static final ArchRule ONLY_REPOSITORIES_USE_JDBC;
	public static final ArchRule CONTROLLERS_DO_NOT_USE_REPOSITORIES;
	public static ArchRule noOtherServices(String servicePackage);
	/** Nạp các lớp main của package (bỏ lớp test) và kiểm tra cả năm luật. */
	public static void check(String servicePackage);
}
```

  - `NO_JPA`: không phụ thuộc `jakarta.persistence..`, và `org.hibernate..` trừ `org.hibernate.validator..`.
  - `noOtherServices`: lớp trong `servicePackage..` không phụ thuộc lớp trong `com.home_platform..` nằm ngoài `servicePackage..` và `com.home_platform.common..`.
  - `REPOSITORY_METHODS_START_WITH_WORKSPACE`: hàm public khai báo trong lớp kế thừa `WorkspaceRepository` có tham số đầu tiên kiểu `long` hoặc `WorkspaceContext`.
  - `ONLY_REPOSITORIES_USE_JDBC`: lớp cấp cao nhất không có `@Repository` không phụ thuộc `JdbcClient`, `JdbcTemplate`, `NamedParameterJdbcTemplate`.
  - `CONTROLLERS_DO_NOT_USE_REPOSITORIES`: lớp có `@RestController` không phụ thuộc lớp có `@Repository`.
  - Mọi luật gọi `allowEmptyShould(true)`.
  - Lớp mẫu, mỗi package một ca: `archfixture.clean` (controller gọi service gọi repository kế thừa `WorkspaceRepository`, hàm `findByCode(long workspaceId, String code)`); `archfixture.jpa` (trường kiểu `jakarta.persistence.EntityManager`); `archfixture.crossa` phụ thuộc `archfixture.crossb`; `archfixture.badrepo` (repository có `findByCode(String code)` và `findAllEverywhere()`); `archfixture.leaky` (service thường có trường `JdbcClient`; controller có trường là repository).

- [ ] **Step 1: Viết lớp mẫu và test hỏng.**

```java
@Test void cleanFixturePassesEveryRule()            // năm luật đều không ném lỗi trên archfixture.clean
@Test void jpaIsCaught()                            // NO_JPA trên archfixture.jpa ném AssertionError
@Test void dependencyOnAnotherServiceIsCaught()     // noOtherServices("com.home_platform.archfixture.crossa") ném AssertionError
@Test void repositoryMethodWithoutWorkspaceIsCaught()
	// trên archfixture.badrepo ném AssertionError, thông báo chứa "findByCode" và "findAllEverywhere"
@Test void jdbcOutsideRepositoryIsCaught()          // trên archfixture.leaky ném AssertionError
@Test void controllerUsingRepositoryIsCaught()      // trên archfixture.leaky ném AssertionError
```

- [ ] **Step 2: Chạy.** `-Dtest=PlatformArchitectureRulesTests`. Mong đợi: lỗi biên dịch.
- [ ] **Step 3: Viết `PlatformArchitectureRules`.** Chạy lại. Mong đợi: PASS.
- [ ] **Step 4: Gắn vào ba service.** Thêm dependency, tạo `ArchitectureTests`:

```java
class ArchitectureTests {
	@Test
	void followsPlatformRules() {
		PlatformArchitectureRules.check("com.home_platform.registry");   // đổi theo từng service
	}
}
```

- [ ] **Step 5: Chạy.** `./mvnw test` ở gốc. Mong đợi: PASS ở cả bốn module.

---

### Task 12: Tài liệu và kiểm tra toàn bộ

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Cập nhật README.** Dòng `platform-common` trong bảng module; mục "Chuẩn bị" thêm file `platform-common/src/test/resources/application-uat.yaml`; mục "Quy ước" thêm: người gọi (`WorkspaceContext`, `UserContext`, hai header), JSON (`long` ra chuỗi), repository (`WorkspaceRepository`, cột chuẩn, `keyset()`), lịch sử (`platform.history.table`, `EntityHistory`), mapper (`PlatformMapperConfig`), Swagger (`/swagger-ui.html`), luật kiến trúc.
- [ ] **Step 2: Kiểm tra toàn bộ.** `./mvnw verify` ở gốc. Mong đợi: `BUILD SUCCESS`, không test nào bị bỏ qua.
- [ ] **Step 3: Chạy thử một service.** `./mvnw -pl registry-service -am package -DskipTests`, chạy jar, rồi `curl -s -o /dev/null -w '%{http_code}' localhost:8082/v3/api-docs` trả `200`, `curl -s localhost:8082/actuator/health` trả `{"status":"UP"...}`. Dừng service.
- [ ] **Step 4: Kiểm tra database không bị thêm gì.** `SHOW TABLES` trên `home_platform` chỉ có 14 bảng nghiệp vụ và ba bảng `flyway_history_*`, không có bảng `tmp_`.
