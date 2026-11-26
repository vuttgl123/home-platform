# Excel Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tải file mẫu, tải lên một file `.xlsx` có năm sheet (Địa điểm, Người, Cư trú, Sở hữu, Đồ đạc), xem trước kết quả từng dòng, rồi nhập tất cả trong một lần ghi; còn một lỗi thì không nhập gì.

**Architecture:** Registry có gói `imports` mới. Gói này đọc file bằng `fastexcel-reader`, đổi từng dòng thành request của các lệnh tạo đang có (`PlaceService.create`, `PersonService.create`, `OccupancyService.create`, `OwnershipService.create`, `ItemService.create`, `ItemService.remove`), rồi gọi chúng. Cả lần chạy nằm trong một savepoint ngoài (`PROPAGATION_NESTED`): xem trước thì luôn quay về savepoint này, nhập thì giữ lại khi không có lỗi. Mỗi dòng có thêm một savepoint riêng, nên dòng hỏng không làm hỏng các dòng sau. Bố cục sheet (tên sheet, tiêu đề, cột mục tuỳ biến, nhãn tiếng Việt) chuyển thành API công khai của gói `export`, để file xuất, file mẫu và lớp đọc dùng chung một nguồn. Frontend có `features/imports`: một trang ba bước.

**Tech Stack:** như `2026-11-25-maintenance-and-reminders.md`: Spring Boot 4, JdbcClient, IT trên MySQL thật; thêm `org.dhatim:fastexcel-reader` (đã có trong `pom.xml`, phạm vi test). React 19, TanStack Query, shadcn (`tabs`, `table`, `scroll-area`, `progress`, `alert`, `switch`), msw.

**Spec:** `docs/superpowers/specs/2026-11-26-excel-import-design.md`.

## Global Constraints

- Lệnh Maven: `cmd.exe /c "cd /d D:\java\home-platform && set JAVA_HOME=C:\Users\vu.phamtuan\.jdks\ms-21.0.12.1&& call mvn""w.cmd -B ..." < /dev/null`. Một IT: `-pl registry-service -am verify -Dit.test=<Tên>IT -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false`; nhiều IT cách nhau bằng dấu phẩy. Test đơn vị: `-pl registry-service -am test -Dtest=<Tên>Tests -Dsurefire.failIfNoSpecifiedTests=false`.
- Frontend: `cmd.exe /c "npm test"`, `npm run build`, `npm run lint` trong `frontend/`. Lint phải 0 lỗi; các cảnh báo cũ trong `components/ui` và `hooks` được phép. Không chạy `npx prettier`.
- Commit:
  - mỗi task một commit bằng git của WSL, trên nhánh `batch-9-excel-import`;
  - không đưa `platform-common/pom.xml`, `pom.xml` vào commit;
  - push chỉ khi bạn đồng ý;
  - dừng registry preview trước khi chạy `verify`.
- Giới hạn:
  - file tối đa 5 MB (`5 * 1024 * 1024` byte), quá thì `413` với chi tiết `"File must be at most 5 MB"`;
  - `MAX_ROWS = 3_000` dòng dữ liệu cộng mọi sheet (hạ từ 5.000 ở Task 8 sau khi đo);
  - multipart của registry: `max-file-size: 5MB`, `max-request-size: 6MB`;
  - mã tối đa 50 ký tự; ghi chú version cắt còn 255 ký tự.
- Ghi chú version của mọi hồ sơ nhập vào: `"Nhập từ Excel: " + tên file` (N9).
- Tên sheet: `"Địa điểm"`, `"Người"`, `"Cư trú"`, `"Sở hữu"`, `"Đồ đạc"`, `"Hướng dẫn"`. Ngăn trong `label` của dòng: `" · "`. Đường dẫn địa điểm ghi ra như file xuất: `" / "`.
- So chữ (N4): `clean` = NFC, đổi NBSP (U+00A0) và mọi khoảng trắng Unicode thành dấu cách, bỏ hai đầu, gộp khoảng trắng liền nhau, rỗng thì `null`. `key` = `clean` rồi `toLowerCase(Locale.ROOT)`. `codeKey` = `key`, rồi tách dấu (NFD), bỏ dấu, `đ` thành `d`; khớp kiểu so `utf8mb4_0900_ai_ci` của khoá duy nhất trên mã.
- Chữ của backend (đúng nguyên văn; `{…}` là chỗ điền):
  - lỗi cả file:
    - "Không đọc được file; hãy lưu lại dưới dạng Excel (.xlsx)"
    - "File có hơn 3.000 dòng; hãy chia thành nhiều file"
    - "File không có dòng nào để nhập"
    - "Sheet {sheet} thiếu cột bắt buộc: {cột, cột} (dòng 1 phải là tiêu đề cột)"
  - ghi chú:
    - "Sheet '{tên}' không có trong mẫu, bị bỏ qua"
    - "Cột '{tên}' ở sheet {sheet} không có trong mẫu, bị bỏ qua"
    - "Cột Đang ở chỉ để xem, bị bỏ qua; khai cư trú ở sheet Cư trú"
  - lỗi ô:
    - "Bắt buộc", "Dài quá {n} ký tự", "Phải là số nguyên", "Phải là số", "Từ {min} tới {max}"
    - "Ngày không hợp lệ; ghi dạng dd/mm/yyyy", "Ô có công thức chưa tính", "Không được sau hôm nay"
    - "Email không hợp lệ", "Giá trị '{x}' không hợp lệ; dùng một trong: {a, b, c}"
    - "Mục '{nhãn}' không áp cho loại {tên loại}"
  - mã: "Mã {mã} đã có", "Mã {mã} lặp với dòng {n}"
  - tham chiếu:
    - "Không tìm thấy địa điểm '{x}'", "Không tìm thấy người '{x}'"
    - "Có {n} địa điểm '{x}'; ghi mã thay cho đường dẫn"
    - "Có {n} người khớp '{x}'; ghi số điện thoại hoặc số giấy tờ thay cho tên"
    - "Chưa có loại '{x}' trong Danh mục", "Loại '{x}' đang ngừng dùng"
    - "Chưa có nhóm '{x}' trong Danh mục", "Nhóm '{x}' đang ngừng dùng"
    - "{Địa điểm|Người} ở dòng {n} (sheet {sheet}) bị lỗi"
    - "Mã địa điểm và Địa điểm chỉ tới hai nơi khác nhau"
    - "Các dòng chỉ tới nhau thành vòng"
    - "Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu"
  - đồ đã gỡ: "Bắt buộc khi trạng thái là Đã gỡ", "Chỉ ghi khi trạng thái là Đã gỡ"
  - dịch lỗi của lệnh tạo:
    - "Đã có người dùng giấy tờ này"
    - "Loại giấy tờ và Số giấy tờ phải ghi cùng nhau"
    - "Người này đã có một lần ở chồng ngày tại địa điểm này"
    - "Người này đã có một lần sở hữu chồng ngày tại địa điểm này"
    - "Tổng tỷ lệ sở hữu của địa điểm sẽ là {x}% vào ngày {dd/mm/yyyy}"
    - "Đến ngày không được trước Từ ngày"
    - "Cây địa điểm không được sâu quá 20 cấp"
    - "Ngày gỡ không được trước Ngày đặt"
    - "Chủ của món đã bị xoá"
    - lỗi lạ: "Không nhập được dòng này ({chi tiết gốc})"
  - cảnh báo:
    - "Đã có người tên '{x}'", "Đã có người có số điện thoại {x}"
    - "Trùng tên với dòng {n}", "Trùng số điện thoại với dòng {n}"
    - "Đã có địa điểm '{đường dẫn}'", "Trùng đường dẫn với dòng {n}"
    - "Đã có món '{tên}' ở '{đường dẫn}'", "Trùng với dòng {n}"
- Chữ của frontend (đúng nguyên văn):
  - đầu trang: "Nhập từ Excel", "Thêm địa điểm, người, cư trú, sở hữu và đồ đạc từ một file Excel."
  - các bước: "Chọn file", "Xem trước", "Nhập"
  - bước ①: "Tải mẫu", "Chọn file .xlsx", "hoặc kéo file vào đây", "Đang đọc file…"
  - bước ②:
    - dải số: "{sheet} {n}" kèm " ({k} lỗi)" hoặc " ({k} cảnh báo)"
    - bảng: "Chỉ hiện dòng có lỗi hoặc cảnh báo", "Dòng", "Hồ sơ", "Kết quả", "Ổn", "Hiện thêm"
    - nút: "Chọn file khác", "Nhập {n} dòng", "Sửa {n} lỗi trong file rồi chọn lại file"
  - bước ③: "Đang nhập, đừng đóng trang", "Đã nhập {n} hồ sơ", "Nhập file khác"
  - trường hợp riêng: "Dữ liệu đã thay đổi từ lúc xem trước", "Không đọc lại được file; chọn lại file", "File lớn hơn 5 MB", "Máy chủ chưa hỗ trợ nhập Excel; cần cập nhật registry"
  - thanh bên "Nhập từ Excel"; nút ở Tổng quan "Nhập Excel"
- Tên file mẫu khi lưu: `exportFileName(tên không gian, 'template', hôm nay)`, tức `<không gian>-mau-nhap-<yyyy-MM-dd>.xlsx`, cùng cách đặt tên với file xuất (spec 4.3).
- `components/ui` không sửa tay. Kiểu API lấy từ `src/api/schema/registry.d.ts`, sinh lại bằng `api:sync --only=registry` với `HP_REGISTRY_PORT=18082` và jar registry đang chạy ở cổng đó (Task 9).

## Review Focus

1. **Số điện thoại hay số CCCD gõ thành ô số trong Excel.** Ô `901234567` hay `1203004567` ở cột chữ phải thành đúng các chữ số đó, không có `.0` hay `E+9`. Cùng chữ số ấy dùng được để nhắc tới người ở sheet Cư trú. Test: `numericCellInATextColumnBecomesPlainDigits` (Task 2), `personIsFoundByANumericPhoneCell` (Task 6).
2. **Dữ liệu chép từ web có NBSP hay khoảng trắng thừa** ("Toà A\u00A0", "Nguyễn  An"). Phải khớp với "Toà A", "Nguyễn An". Test: `nonBreakingAndRepeatedSpacesAreCleaned` (Task 2).
3. **Ngày không có thật** ("31/02/2026", "2026-13-01"). Phải là lỗi ô, không được lặng lẽ cuộn sang tháng sau. Test: `impossibleDateIsAnError` (Task 2).
4. **Người dùng thêm một dòng tiêu đề "DANH SÁCH PHÒNG" ở dòng 1**, tiêu đề cột ở dòng 2. Lỗi cả file phải nói rõ dòng 1 phải là tiêu đề cột. Test: `titleRowAboveHeadersExplainsRowOne` (Task 2).
5. **Người dùng điền cột Đang ở của sheet Người, tưởng là khai cư trú.** Phải có ghi chú, không lặng lẽ bỏ qua. Đây là ruling so với spec N1: ghi chú chỉ hiện khi cột có ít nhất một ô có chữ. Test: `filledLivingAtColumnGetsANotice` (Task 4).

---

### Task 1: Bố cục sheet dùng chung và file mẫu (backend, N1, 4.2)

**Files:**
- Create: `export/ExcelLayout.java`, `export/ImportTemplateWriter.java`, `imports/ImportController.java`; test `imports/ImportTemplateIT.java`
- Modify:
  - `export/PlaceSheet.java`, `PersonSheet.java`, `OccupancySheet.java`, `OwnershipSheet.java`, `ItemSheet.java`: lấy tên sheet và tiêu đề từ `ExcelLayout`;
  - `export/AttributeColumns.java`, `ExportLabels.java`: chuyển thành `public`;
  - `registry-service/pom.xml`: `fastexcel-reader` bỏ `<scope>test</scope>`;
  - `registry-service/src/main/resources/application.yaml`: multipart;
  - `README.md`: thêm dòng API `/imports`.

**Interfaces:**
- Produces:
  - `ExcelLayout` (`public final`):
    - `public static final String PLACES, PEOPLE, OCCUPANCIES, OWNERSHIPS, ITEMS, GUIDE` là tên sheet;
    - `public static final String LIVING_AT = "Đang ở"`;
    - `public static final List<String> PLACE_HEADERS, PERSON_HEADERS, OCCUPANCY_HEADERS, OWNERSHIP_HEADERS, ITEM_HEADERS`: đúng danh sách đang có trong các lớp sheet, kể cả "Đang ở" trong `PERSON_HEADERS`.
  - `AttributeColumns`:
    - `public static AttributeColumns of(List<FieldDef>, FieldTarget, Map<Long, String>)` và `public List<String> headers()`, như cũ;
    - thêm `public Map<String, FieldDef> defsByHeader()`, giữ thứ tự cột.
  - `ExportLabels`: `public static` với `itemStatus`, `placeStatus`, `relation`, `gender`, `yesNo`.
  - `ImportTemplateWriter` (`@Service`, `public`): `public void write(long workspaceId, OutputStream out)`. Ghi "Hướng dẫn" trước, rồi năm sheet chỉ có tiêu đề: tiêu đề cố định (người thì bỏ "Đang ở") cộng `AttributeColumns` của đích, cùng cách gọi như `ExportService`. Đọc trong `@Transactional(readOnly = true)`.
  - `ImportController` `@RequestMapping("/imports")`: `GET /imports/template.xlsx`. Header file như `ExportController.attach`, tên `ExportService.fileName("import-template")`.

- [ ] **Step 1: Viết test `ImportTemplateIT extends RegistryIT`.** Đọc file bằng `export/XlsxReader` (test); chuyển lớp này và các record của nó sang `public`:
  - `templateHasGuideThenFiveSheets`: tên sheet đúng thứ tự `"Hướng dẫn", "Địa điểm", "Người", "Cư trú", "Sở hữu", "Đồ đạc"`; năm sheet dữ liệu có `dataRowCount()` 0.
  - `templateHeadersAreTheExportHeadersWithoutLivingAt`: với một mục tuỳ biến PLACE "Diện tích" và một mục PERSON "Quê quán", tiêu đề sheet Địa điểm bằng tiêu đề sheet Địa điểm của `/exports/workspace.xlsx`. Tiêu đề sheet Người bằng của file xuất trừ "Đang ở". Ba sheet còn lại bằng nhau từng cột.
  - `guideListsActiveTypesAndCategories`: tạo loại "Phòng" (đang dùng), loại "Kho" (ngừng dùng), nhóm "Điện máy". Sheet Hướng dẫn có ô chữ "Phòng" và "Điện máy", không có "Kho". Có các nhãn trạng thái "Đang dùng", "Tạm ngưng", "Cất kho" và quan hệ "Người ở" (không có "Chủ").
- [ ] **Step 2: Chạy, thấy FAIL** (404).
- [ ] **Step 3: Cài đặt.**
  - Sheet Hướng dẫn: một cột chữ. Gồm:
    - thứ tự nhập;
    - cột bắt buộc của từng sheet (N2);
    - cách ghi địa điểm (mã hoặc "Toà A / Tầng 3 / P301"), người (tên, số điện thoại hoặc số giấy tờ), ngày (`dd/mm/yyyy`), mã (trống là tự cấp);
    - "tất cả hoặc không";
    - các giá trị hợp lệ: loại địa điểm và nhóm đồ đang dùng, nhãn trạng thái, quan hệ (trừ Chủ), giới tính;
    - mỗi mục tuỳ biến đang dùng: tiêu đề cột, kiểu, lựa chọn, bắt buộc, áp cho loại nào.
  - Có thể dùng `SheetWriter` (cùng gói).
  - Danh sách chọn trong ô: chỉ thêm nếu `fastexcel` có API cho nó. Không có thì bỏ, ghi ruling.
- [ ] **Step 4: Chạy `ImportTemplateIT`, `ExportWorkspaceIT`, `ExportItemsIT`, `AttributeColumnsTests`, `ExportLabelsTests` → PASS.**
- [ ] **Step 5: Commit** "Share the Excel layout and serve an import template".

### Task 2: Đọc file (backend, N1)

**Files:**
- Create:
  - `imports/ImportText.java`, `SheetKind.java`, `ImportCell.java`, `CellError.java`, `ImportRow.java`, `ImportSheet.java`, `ImportWorkbook.java`, `ImportFileException.java`, `WorkbookReader.java`;
  - test `imports/XlsxBuilder.java` (helper), `imports/WorkbookReaderTests.java`, `imports/ImportCellTests.java`, `imports/ImportTextTests.java`.

**Interfaces:**
- Consumes: `ExcelLayout` (Task 1).
- Produces:
  - `ImportText`: `static String clean(String)`, `static String key(String)`, `static String codeKey(String)` (Global Constraints).
  - `SheetKind` enum `PLACE, PERSON, OCCUPANCY, OWNERSHIP, ITEM`, theo thứ tự nhập (N5):
    - `String sheetName()`, `List<String> headers()` (từ `ExcelLayout`);
    - `List<String> requiredHeaders()`: PLACE {Tên, Loại}; PERSON {Họ tên}; OCCUPANCY và OWNERSHIP {Người, Từ ngày}; ITEM {Tên, Ở đâu};
    - với OCCUPANCY, OWNERSHIP thì thiếu cả "Địa điểm" lẫn "Mã địa điểm" cũng là thiếu cột bắt buộc (báo "Địa điểm, Mã địa điểm").
  - `ImportCell` (một ô đã đọc; `column` là tiêu đề như trong `headers()`):
    - `boolean isBlank()`;
    - `String text()`: ô số thì là chữ số dạng `toPlainString()`, bỏ đuôi `.0`; ô ngày thì `dd/MM/yyyy`;
    - `Integer integer(int min, int max)`, `BigDecimal decimal()`, `LocalDate date()`: trống trả `null`; sai ném `CellError`.
  - `CellError extends RuntimeException`: `String column()`, `String text()`.
  - `ImportRow`: record `(int number, Map<String, ImportCell> cells)`; `ImportCell cell(String header)` trả ô trống khi không có cột.
  - `ImportSheet`: record `(SheetKind kind, String name, List<ImportRow> rows, Set<String> columns)`.
  - `ImportWorkbook`: record `(List<ImportSheet> sheets, List<String> notices)`; sheet theo thứ tự `SheetKind`.
  - `ImportFileException extends RuntimeException`: `List<String> errors()`.
  - `WorkbookReader.read(InputStream in, Map<SheetKind, Set<String>> extraHeaders): ImportWorkbook`: `public static`; `extraHeaders` là tiêu đề cột mục tuỳ biến, để không bị coi là cột lạ.
  - `XlsxBuilder` (test): `sheet(String name, String... headers)`, `row(Object... cells)` (chữ, `Number`, `LocalDate` thành ô ngày có định dạng, `XlsxBuilder.formula(String expr, Object cached)`), `byte[] build()`.
    - Dựng bằng `fastexcel`.
    - Ô công thức có giá trị đã lưu: `fastexcel` không ghi giá trị này, nên sau khi ghi thì sửa XML của sheet trong file zip để thêm `<v>`.
    - Ô `null` là ô trống.

- [ ] **Step 1: Viết test.**
  - `ImportTextTests`:
    - `nonBreakingAndRepeatedSpacesAreCleaned`: `clean("Toà A\u00A0")` là "Toà A"; `clean(" Nguyễn  An ")` là "Nguyễn An"; `clean(" \u00A0 ")` là `null`;
    - `keyFoldsCaseAndComposedForms`: `key("NGUYỄN AN")` bằng `key` của chuỗi NFD "Nguyễn An";
    - `codeKeyIgnoresAccents`: `codeKey("Phòng-Đ1")` bằng `codeKey("phong-d1")`.
  - `ImportCellTests`:
    - `numericCellInATextColumnBecomesPlainDigits`: số `901234567` thành "901234567"; số `1.203004567E9` thành "1203004567";
    - `integerAcceptsWholeNumbersAndDigitText`: `12`, `12.0`, "12" đều là 12; `12.5` lỗi "Phải là số nguyên"; 10000 với max 9999 lỗi "Từ 0 tới 9999";
    - `datesFromExcelAndThreeTextForms`: ô ngày, "05/03/2026", "5/3/2026", "2026-03-05" đều là 2026-03-05;
    - `impossibleDateIsAnError`: "31/02/2026", "2026-13-01", "hôm qua" lỗi "Ngày không hợp lệ; ghi dạng dd/mm/yyyy";
    - `formulaUsesTheStoredValueOrFails`: có giá trị đã lưu thì dùng; không có thì "Ô có công thức chưa tính".
  - `WorkbookReaderTests`:
    - `readsKnownSheetsInImportOrder`: file có sheet Đồ đạc trước Người, trả theo thứ tự PERSON, ITEM;
    - `unknownSheetsAndColumnsBecomeNotices`: sheet "Ghi chép" và cột "Tầng" thành hai ghi chú đúng chữ; sheet "Hướng dẫn" không có ghi chú; cột trong `extraHeaders` không có ghi chú;
    - `headersMatchIgnoringCaseSpacesAndComposition`: tiêu đề " họ TÊN " và tên sheet NFD "Người" được nhận;
    - `blankRowsAreSkippedAndRowNumbersAreExcelRows`: dòng 3 trống, dữ liệu ở dòng 2 và 4, số dòng trả về là 2 và 4;
    - `missingRequiredColumnIsAFileError`: sheet Người không có "Họ tên" thì lỗi đúng chữ; sheet Cư trú không có cả hai cột địa điểm thì lỗi kèm "Địa điểm, Mã địa điểm";
    - `titleRowAboveHeadersExplainsRowOne`: dòng 1 là "DANH SÁCH PHÒNG", dòng 2 là tiêu đề, thì lỗi có "(dòng 1 phải là tiêu đề cột)";
    - `tooManyRowsIsAFileError`: 5.001 dòng ở hai sheet;
    - `notAnXlsxIsAFileError`: byte của một file chữ;
    - `nothingToImportIsAFileError`: chỉ có tiêu đề.
- [ ] **Step 2: Chạy, thấy FAIL** (không biên dịch).
- [ ] **Step 3: Cài đặt** bằng `ReadableWorkbook`, `ReadingOptions(true, false)` để biết ô nào có định dạng ngày.
  - Ô số có định dạng ngày thì là ngày.
  - Ô công thức dùng giá trị đã lưu; không có thì `ImportCell` ghi lại để `text()`, `date()` ném lỗi.
  - Đọc chữ theo `SheetKind.headers()` cộng `extraHeaders`, so bằng `key`.
- [ ] **Step 4: Chạy ba lớp test → PASS.**
- [ ] **Step 5: Commit** "Read import workbooks".

### Task 3: Tạo với mã cho trước (backend, N3)

**Files:**
- Modify: `place/PlaceService.java`, `item/ItemService.java`, `person/PersonFields.java` (`phone` thành `public static`, lớp thành `public`)
- Create: test `code/GivenCodeIT.java`

**Interfaces:**
- Produces:
  - `PlaceService.create(WorkspaceContext ctx, PlaceCreateRequest request, String givenCode): Place`;
  - `ItemService.create(WorkspaceContext ctx, ItemRequest request, String givenCode): Item`;
  - `givenCode` là `null` thì cấp mã như cũ. Có chữ thì dùng nguyên chữ đó (đã `clean`, ≤ 50 ký tự). Mã đã có trên bất kỳ dòng nào (kể cả đã xoá) thì ném `ConflictException("Code " + code + " is already in use")`. Javadoc ghi "chỉ dùng khi nhập từ Excel".
  - `create(ctx, request)` cũ gọi bản mới với `null`.
  - `PersonFields.phone(String): String` dùng được từ gói `imports`.

- [ ] **Step 1: Viết test**
  - `givenCodeIsKept`: tạo địa điểm và món với mã "P301" thì `code()` là "P301".
  - `givenCodeTakenEvenByADeletedRowIsAConflict`: xoá một địa điểm mã "P301", tạo mới với "p301" thì `ConflictException`.
  - `issuedCodesSkipAGivenCode`: mẫu mã địa điểm `P`, 3 chữ số, không theo năm (`codeRule` của `RegistryIT`). Tạo với mã "P002", rồi tạo hai địa điểm không mã: được "P001", "P003".
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Kiểm `repository.codeExists` trước khi ghi. `insertWithCode` vẫn là chốt cuối.
- [ ] **Step 4: Chạy các IT vừa sửa, cùng `PlaceIT`, `ItemIT`, `CodeConcurrencyIT` → PASS.**
- [ ] **Step 5: Commit** "Let imports create places and items with a given code".

### Task 4: Bộ chạy nhập, báo cáo, sheet Người (backend, N1, N6, N7, N9)

**Files:**
- Create:
  - `imports/ImportReport.java`, `SheetReport.java`, `RowReport.java`, `RowMessage.java`, `RowStatus.java`, `MessageLevel.java`;
  - `imports/ImportService.java`, `ImportRun.java`, `RowRunner.java`, `ImportMessages.java`, `AttributeCells.java`, `ImportLookupRepository.java`, `PersonRows.java`;
  - test `imports/ImportTestData.java` (lớp cơ sở: `RegistryIT` + `XlsxBuilder` + `preview(byte[])`, `commit(byte[])` qua MockMvc `multipart`), `imports/ImportPeopleIT.java`, `imports/ImportMessagesTests.java`.
- Modify: `imports/ImportController.java` (`POST /imports/preview`, `POST /imports`), `README.md`.

**Interfaces:**
- Consumes: `WorkbookReader`, `ImportCell`, `ImportRow`, `SheetKind` (Task 2); `AttributeColumns.defsByHeader()`, `ExportLabels` (Task 1); `PersonService.create`, `PersonFields.phone` (Task 3).
- Produces:
  - **Báo cáo**, đúng tên trường trong spec 4.2:
    - `ImportReport(String fileName, boolean committed, List<String> fileErrors, List<String> notices, Totals totals, List<SheetReport> sheets)`, với `Totals(int rows, int errors, int warnings)`;
    - `SheetReport(SheetKind sheet, String name, int rows, int errors, int warnings, List<RowReport> items)`;
    - `RowReport(int row, String label, String code, RowStatus status, List<RowMessage> messages)`;
    - `RowMessage(MessageLevel level, String column, String text)`;
    - enum `RowStatus { OK, WARNING, ERROR }`, `MessageLevel { ERROR, WARNING }`.
  - **`ImportService`**:
    - `public ImportReport preview(WorkspaceContext ctx, String fileName, byte[] file)` và `public ImportReport commit(WorkspaceContext ctx, String fileName, byte[] file)`;
    - cả hai chạy `TransactionTemplate` `PROPAGATION_NESTED`. Xem trước luôn `setRollbackOnly()`; nhập thì `setRollbackOnly()` khi `totals.errors > 0` hoặc có `fileErrors`;
    - `committed = true` chỉ khi đã giữ lại;
    - `ImportFileException` thành `fileErrors`, `sheets` rỗng.
  - **`RowRunner`**: chạy một dòng trong `TransactionTemplate` `PROPAGATION_NESTED`. `CellError`, `ErrorResponseException` và `ConstraintViolationException` thành `RowMessage`. Lỗi khác cũng được bắt, thành "Không nhập được dòng này (…)", và ghi log ở mức WARN.
  - **`ImportMessages`**:
    - `static List<RowMessage> of(RuntimeException ex, Map<String, String> columnOfField)`: dịch theo bảng trong Global Constraints. Lỗi ràng buộc dịch theo loại annotation: `NotBlank`/`NotNull` → "Bắt buộc"; `Size` → "Dài quá {max} ký tự"; `Min`/`Max`/`DecimalMin`/`DecimalMax` → "Từ {min} tới {max}"; `Email`; `PastOrPresent`; `Digits` → "Phải là số". Lỗi `errors` của `BadRequestException` có `field` dạng `attributes.<key>` thì gắn vào cột của mục đó;
    - `columnOfField` nối tên trường request với tiêu đề cột (`fullName` → "Họ tên").
  - **`AttributeCells.read(ImportRow row, AttributeColumns columns, Long typeId, String typeName): Map<String, Object>`** (N6):
    - TEXT ra chữ; NUMBER ra `BigDecimal`; DATE ra `"yyyy-MM-dd"`;
    - BOOLEAN: "Có"/"Không", không phân biệt hoa thường, ra `Boolean`;
    - SELECT ra đúng chữ của lựa chọn;
    - mục không áp cho loại mà có chữ thì `CellError` "Mục '{nhãn}' không áp cho loại {tên loại}".
  - **`ImportLookupRepository`** (`@Repository`, mọi method nhận `workspaceId` đầu tiên; chỉ bản ghi chưa xoá):
    - `places(ws): List<KnownPlace(id, parentId, code, name)>`;
    - `people(ws): List<KnownPerson(id, fullName, phone, idNumber)>`;
    - `items(ws): List<KnownItem(placeId, name)>`, chỉ món chưa gỡ.
  - **`ImportRun`**: trạng thái của một lần chạy. Gồm lookups, chỉ mục người (Task 6 dùng), `SheetReport` đang dựng, ghi chú version `"Nhập từ Excel: " + fileName` cắt 255. Có `String versionNote()`.
  - **`PersonRows.importAll(ImportSheet sheet, ImportRun run)`**: theo N2. `label` là họ tên. Cảnh báo theo N7: so `key(họ tên)` và `PersonFields.phone` với người đã có và với các dòng trước của file.
  - **Controller**:
    - `POST /imports/preview` và `POST /imports`: multipart `file`, trả `ImportReport`, `200`;
    - file > 5 MB thì `ResponseStatusException(CONTENT_TOO_LARGE, "File must be at most 5 MB")`;
    - tên file lấy `getOriginalFilename()`, trống thì `"import.xlsx"`.

- [ ] **Step 1: Viết test `ImportPeopleIT extends ImportTestData`**
  - `previewListsEveryRowAndWritesNothing`: 2 dòng hợp lệ thì `$.committed` false. `$.sheets[0].items[*].status` là OK, OK. `$.sheets[0].items[0].row` là 2. Sau đó `GET /people` không có hai người đó; `reg_entity_version` không thêm dòng.
  - `commitCreatesPeopleWithTheImportNote`: tên file "nguoi.xlsx" thì `committed` true. Mỗi người có version 1 với ghi chú "Nhập từ Excel: nguoi.xlsx". Ngày sinh, giới tính "Nữ", ghi chú được lưu.
  - `oneErrorImportsNothing`: dòng 3 có email "abc" thì `committed` false. Dòng 3 là ERROR, cột "Email", "Email không hợp lệ". Dòng 2 vẫn OK. Không người nào được tạo.
  - `aFailedRowDoesNotSpoilTheNext`: dòng 2 gây lỗi của lệnh tạo (giấy tờ trùng với người đã có), dòng 3 hợp lệ thì dòng 3 là OK.
  - `serviceErrorsAreTranslated`: giấy tờ trùng thì "Đã có người dùng giấy tờ này". Chỉ có Số giấy tờ thì "Loại giấy tờ và Số giấy tờ phải ghi cùng nhau". Ngày sinh sau hôm nay thì "Không được sau hôm nay" ở cột "Ngày sinh".
  - `duplicateNamesAndPhonesWarnButDoNotBlock`: đã có "Nguyễn An" thì một dòng "nguyễn  an" có cảnh báo "Đã có người tên 'nguyễn an'". Hai dòng cùng số điện thoại thì dòng sau có "Trùng số điện thoại với dòng 2". Nhập vẫn `committed` true.
  - `filledLivingAtColumnGetsANotice`: cột Đang ở có chữ thì `notices` có "Cột Đang ở chỉ để xem, bị bỏ qua; khai cư trú ở sheet Cư trú". Cột có nhưng trống thì không có ghi chú.
  - `personAttributesAreRead`: mục PERSON "Quê quán" (TEXT), "Đã tiêm" (BOOLEAN), "Nhóm máu" (SELECT A, B, O) và "Hạn CCCD" (DATE, bắt buộc). Giá trị "Có", "b", ô ngày được lưu thành `true`, "B", `"2030-01-01"`. "X" ở Nhóm máu lỗi "Giá trị 'X' không hợp lệ; dùng một trong: A, B, O". Hạn CCCD trống lỗi "Bắt buộc".
  - `fileErrorsComeBackAsReport`: file chữ thì `$.fileErrors[0]` đúng chữ, `committed` false.
  - `oversizedFileIs413`: 5 MB + 1 byte.
  - `ImportMessagesTests`: một case cho mỗi dòng của bảng dịch. Lỗi lạ thì "Không nhập được dòng này (boom)".
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** Request được kiểm bằng `jakarta.validation.Validator` trước khi gọi service. Đếm `totals` sau khi mọi sheet xong.
- [ ] **Step 4: Chạy `ImportPeopleIT`, `ImportMessagesTests`, `ArchitectureTests` → PASS.**
- [ ] **Step 5: Commit** "Preview and import people from Excel".

### Task 5: Sheet Địa điểm (backend, N2, N3, N4, N5)

**Files:**
- Create: `imports/PlaceIndex.java`, `imports/PlaceRows.java`, `imports/TypeLookup.java`; test `imports/ImportPlacesIT.java`
- Modify: `imports/ImportRun.java`, `ImportService.java` (chạy sheet Địa điểm trước Người)

**Interfaces:**
- Consumes: `PlaceService.create(ctx, request, givenCode)` (Task 3); `ImportRun`, `RowRunner`, `AttributeCells` (Task 4); `ImportLookupRepository.places`.
- Produces:
  - **`PlaceIndex`** (trong `ImportRun`):
    - `PlaceRef resolve(String value)`: thử mã (`codeKey`) trước, rồi đường dẫn (tách theo `/`, mỗi phần `key`). Khớp cả địa điểm đã có lẫn dòng của file;
    - `PlaceRef` (sealed interface) là một trong bốn: `Found(long id, String path)`, `Missing(String value)`, `Ambiguous(String value, int count)`, `FailedRow(SheetKind sheet, int row)`;
    - `void add(int row, String code, String path, Long id)` ghi dòng đã tạo; `id = null` là dòng lỗi;
    - `Optional<Integer> givenCodeRow(String codeKey)` để bắt mã lặp;
    - `boolean pathExists(String pathKey)` cho cảnh báo.
  - **`TypeLookup`**: `placeType(String value)` tìm theo tên hoặc mã; `category(String value)` tìm theo tên; trả loại đang dùng hoặc ném `CellError` theo đúng chữ.
  - **`PlaceRows.importAll(ImportSheet, ImportRun)`**:
    - xếp dòng để cha đứng trước con: cha nhắc theo đường dẫn hay theo mã của một dòng trong file;
    - dòng trong vòng: "Các dòng chỉ tới nhau thành vòng";
    - `label` là `mã · tên`, `code` là mã giữ hoặc mã vừa cấp.

- [ ] **Step 1: Viết test `ImportPlacesIT`**
  - `placesNestByPathAndKeepCodes`: các dòng "Toà A" (mã TA), "Tầng 3" (Nằm trong "Toà A"), "P301" (mã P301, Nằm trong "toà a / tầng 3", Sức chứa 2, Tạm ngưng). Nhập xong P301 có cha là Tầng 3, mã "P301", trạng thái `INACTIVE`, `capacity` 2; Toà A có mã "TA"; Tầng 3 có mã tự cấp.
  - `childBeforeParentInTheFileStillWorks`: dòng con ở dòng 2, cha ở dòng 3 → OK. Số dòng trong báo cáo vẫn là số dòng của file.
  - `parentByCodeOfAnExistingPlace`: Nằm trong "TA" (đã có trong database) → OK.
  - `codeErrors`: mã trùng với địa điểm đã có thì "Mã TA đã có". Hai dòng cùng mã "x1", "X1" thì dòng sau có "Mã X1 lặp với dòng 2". Mã 51 ký tự thì "Dài quá 50 ký tự".
  - `referenceErrors`: loại "Kho" chưa có thì "Chưa có loại 'Kho' trong Danh mục". Loại ngừng dùng thì "Loại 'X' đang ngừng dùng". Nằm trong "Toà Z" thì "Không tìm thấy địa điểm 'Toà Z'". Hai "Toà A" ở cấp cao nhất thì "Có 2 địa điểm 'Toà A'; ghi mã thay cho đường dẫn".
  - `childOfAFailedRowFails`: cha lỗi thì con có "Địa điểm ở dòng 2 (sheet Địa điểm) bị lỗi".
  - `cycleIsAnError`: A (mã A) Nằm trong "B", B (mã B) Nằm trong "A" → cả hai dòng có "Các dòng chỉ tới nhau thành vòng".
  - `existingPathWarns`: đã có "Toà A" thì dòng "Toà A" có cảnh báo "Đã có địa điểm 'Toà A'". Nhập vẫn được.
  - `placeAttributeOfAnotherTypeIsAnError`: mục "Diện tích" của loại Phòng, có chữ trên dòng loại Toà → "Mục 'Diện tích' không áp cho loại Toà".
  - `tooDeepIsTranslated`: chuỗi 21 cấp → "Cây địa điểm không được sâu quá 20 cấp".
  - `previewShowsTheCodeThatWouldBeIssued`: mẫu mã `P` 3 chữ số. Xem trước thì dòng không mã có `code` "P001". Xem trước lần nữa vẫn "P001" (bộ đếm đã quay về).
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.**
- [ ] **Step 4: Chạy `ImportPlacesIT`, `ImportPeopleIT` → PASS.**
- [ ] **Step 5: Commit** "Import places with their codes and nesting".

### Task 6: Sheet Cư trú và Sở hữu (backend, N2, N4)

**Files:**
- Create: `imports/PersonIndex.java`, `OccupancyRows.java`, `OwnershipRows.java`; test `imports/ImportStaysIT.java`
- Modify: `imports/ImportRun.java`, `PersonRows.java` (ghi người vừa tạo vào `PersonIndex`), `ImportService.java`

**Interfaces:**
- Consumes: `PlaceIndex` (Task 5); `OccupancyService.create`, `OwnershipService.create`.
- Produces:
  - **`PersonIndex`**: `PersonRef resolve(String value)`, cùng bốn dạng như `PlaceRef` (`Found(long id, String name)`, `Missing`, `Ambiguous`, `FailedRow`). Khớp khi `key(họ tên)`, hoặc `PersonFields.phone(value)`, hoặc `clean(số giấy tờ)` bằng giá trị. Đếm số người khác nhau khớp.
  - Cột địa điểm của hai sheet (N4): "Mã địa điểm" có chữ thì dùng nó; cả hai có chữ mà ra hai `id` khác nhau thì lỗi đúng chữ.
  - `label` là `tên người · đường dẫn địa điểm`.

- [ ] **Step 1: Viết test `ImportStaysIT`**
  - `staysAndOwnershipsLinkFileRowsAndExistingRecords`: file có P301 (sheet Địa điểm) và Trần Bình (sheet Người). Cư trú gồm Trần Bình ở P301 từ 01/03/2026, và Lê Cúc (đã có) ở "P101" (mã, đã có) quan hệ "Khách". Sở hữu: Trần Bình 50% P301 từ 01/01/2026, số giấy tờ "GCN-1". Nhập xong có đủ ba dòng, đúng quan hệ, ngày, tỷ lệ.
  - `blankRelationIsResident`.
  - `ownerRelationIsRejected`: "Chủ" → "Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu".
  - `personIsFoundByANumericPhoneCell`: sheet Người có Điện thoại là ô số `901234567`, nên được lưu "901234567". Sheet Cư trú có Người là ô số `901234567`, khớp đúng người đó.
  - `composedNamesMatch`: Người ghi "Trần Bình" dạng NFD khớp "Trần Bình" dạng NFC đã có.
  - `ambiguousPersonAsksForPhoneOrDocument`: hai "Nguyễn An" → "Có 2 người khớp 'Nguyễn An'; ghi số điện thoại hoặc số giấy tờ thay cho tên". Ghi số điện thoại của một người thì OK.
  - `codeAndPathMustAgree`: Mã địa điểm "P301", Địa điểm "Toà A / P101" → lỗi đúng chữ.
  - `overlapsInTheFileAreCaught`: hai dòng cư trú chồng ngày của cùng người, cùng nơi → dòng sau "Người này đã có một lần ở chồng ngày tại địa điểm này".
  - `sharesOverAHundredAreCaught`: 60% và 50% cùng nơi, cùng ngày → dòng sau "Tổng tỷ lệ sở hữu của địa điểm sẽ là 110% vào ngày 01/01/2026".
  - `endBeforeStartAndMissingStart`: Đến ngày trước Từ ngày thì "Đến ngày không được trước Từ ngày". Từ ngày trống thì "Bắt buộc".
  - `rowOfAFailedPersonFails`: người ở dòng 4 sheet Người lỗi → "Người ở dòng 4 (sheet Người) bị lỗi".
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.**
- [ ] **Step 4: Chạy `ImportStaysIT`, `ImportPlacesIT`, `ImportPeopleIT` → PASS.**
- [ ] **Step 5: Commit** "Import stays and ownerships".

### Task 7: Sheet Đồ đạc (backend, N2, N3, N8)

**Files:**
- Create: `imports/ItemRows.java`; test `imports/ImportItemsIT.java`
- Modify: `imports/ImportRun.java`, `ImportService.java`

**Interfaces:**
- Consumes: `ItemService.create(ctx, request, givenCode)` (Task 3), `ItemService.remove`; `PlaceIndex`, `PersonIndex`, `TypeLookup.category`; `ImportLookupRepository.items` cho cảnh báo.
- Produces:
  - `ItemRows.importAll(ImportSheet, ImportRun)`. Đã gỡ thì tạo với `IN_USE`, rồi `remove(ctx, id, new ItemRemoveRequest(version, ngày gỡ, versionNote))` trong cùng savepoint của dòng.
  - `label` là `mã · tên`.

- [ ] **Step 1: Viết test `ImportItemsIT`**
  - `itemsLandWithEveryColumn`: nhập đủ Mã, Tên, Nhóm, Ở đâu (đường dẫn), Số lượng, Đơn vị, Trạng thái "Hỏng", Ngày đặt, Hết bảo hành, Bảo trì (tháng) 6, Chủ, Mô tả, cùng một mục ITEM của nhóm. `GET /items/{id}` có đúng từng giá trị.
  - `blankQuantityIsOneAndBlankStatusIsInUse`.
  - `englishCodesAndAnyCaseAreAccepted`: Trạng thái "BROKEN" và "cất kho" được nhận là Hỏng và Cất kho.
  - `removedItemsGetTwoVersions`: "Đã gỡ" với Ngày gỡ 10/10/2026 → `status` REMOVED, `removedAt` 2026-10-10; version 1 CREATE và 2 REMOVE, cả hai ghi chú "Nhập từ Excel: …".
  - `removalDateRules`: Đã gỡ không có ngày → "Bắt buộc khi trạng thái là Đã gỡ" ở cột "Ngày gỡ". Có ngày với Đang dùng → "Chỉ ghi khi trạng thái là Đã gỡ". Ngày gỡ trước Ngày đặt → "Ngày gỡ không được trước Ngày đặt".
  - `categoryAndPlaceErrors`: nhóm chưa có, ngừng dùng; Ở đâu không tìm thấy; Ở đâu trống thì "Bắt buộc".
  - `itemCodesShareTheirOwnSpace`: mã đồ "TA" trùng mã địa điểm "TA" vẫn OK. Trùng mã đồ đã có thì "Mã TA đã có".
  - `sameNameAtSamePlaceWarns`: đã có "Quạt" ở P301 thì dòng "quạt" ở P301 có "Đã có món 'quạt' ở 'Toà A / P301'". Hai dòng giống nhau trong file thì dòng sau có "Trùng với dòng 2".
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.**
- [ ] **Step 4: Chạy `ImportItemsIT` cùng các IT nhập khác → PASS.**
- [ ] **Step 5: Commit** "Import items, including removed ones".

### Task 8: Khứ hồi, thời gian, tài liệu backend (spec mục 5)

**Files:**
- Create: test `imports/ImportRoundTripIT.java`, `imports/ImportTimingIT.java`
- Modify: `README.md` (API `/imports`, giới hạn), spec mục 4.2 nếu phải hạ `MAX_ROWS`

**Interfaces:**
- Consumes: `/exports/workspace.xlsx`, `/imports`, `export/XlsxReader`.

- [ ] **Step 1: Viết test**
  - `ImportRoundTripIT.exportImportExportIsTheSameFile`:
    - Danh mục giống nhau ở `WS_A` và `WS_B`: loại Toà, Phòng; nhóm Điện máy; một mục tuỳ biến mỗi đích; mẫu mã;
    - dữ liệu ở `WS_A`: ba cấp địa điểm, ba người không trùng tên (một người có giấy tờ), cư trú đang ở và đã kết thúc, sở hữu, món đang dùng, hỏng, đã gỡ, có chủ;
    - xuất `WS_A`, nhập vào `WS_B` (`asB`), xuất `WS_B`;
    - mọi sheet, mọi ô của hai file bằng nhau (so `XlsxCell.text()`, số, ngày).
  - `ImportTimingIT.fiveThousandRowsPreviewInUnderAMinute`: chỉ chạy khi có `-Dimport.timing=true` (`@EnabledIfSystemProperty`). File 1.000 địa điểm, 2.000 người, 2.000 đồ đạc; xem trước dưới 60 giây; in thời gian ra log.
- [ ] **Step 2: Chạy `ImportRoundTripIT` → PASS** (Task 4–7 đã làm đủ). Nếu FAIL thì sửa phần nhập, không sửa test. Chạy `ImportTimingIT` với cờ, ghi thời gian vào ledger. Quá 60 giây thì hạ `MAX_ROWS` (cả chữ lỗi và Global Constraints), ghi ruling.
- [ ] **Step 3: `mvnw -B verify` cả repo → BUILD SUCCESS.**
- [ ] **Step 4: Commit** "Prove an exported workspace imports back cell for cell".

### Task 9: Trang nhập, bước ① và ② (frontend)

**Files:**
- Modify:
  - `src/api/schema/registry.{json,d.ts}` (sinh lại);
  - `features/exports/fileName.ts` (`ExportKind` thêm `'template'`, slug `mau-nhap`), `components/ExportButton.tsx` (`template: '/imports/template.xlsx'`);
  - `app/router.tsx` (`import`), `app/layout/AppLayout.tsx` (mục "Nhập từ Excel" ngay sau "Danh mục", biểu tượng `FileUpIcon`);
  - `features/overview/components/OverviewPage.tsx` (nút "Nhập Excel", `variant="outline"`, link `/w/:ws/import`, trước "Tải Excel");
  - `src/test/fixtures.ts`.
- Create:
  - `features/imports/types.ts`, `api.ts`, `queries.ts`, `index.ts`;
  - `components/ImportSteps.tsx`, `FilePicker.tsx`, `ReportView.tsx`;
  - `pages/ImportPage.tsx`;
  - test `features/imports/ImportPage.test.tsx`, `src/test/importBackend.ts`.

**Interfaces:**
- Consumes: `POST /imports/preview`, `GET /imports/template.xlsx` (Task 1, 4).
- Produces:
  - Kiểu: `ImportReport`, `SheetReport`, `RowReport`, `RowMessage` lấy từ `registry.d.ts`.
  - `previewImport(ws: string, file: File): Promise<ImportReport>` và `runImport(ws, file)`: multipart như `uploadPhoto`.
  - `usePreviewImport(ws)`, `useRunImport(ws)` (mutation).
  - `ImportPage` xuất từ `features/imports/index.ts`.
  - Fixture: `importReport(overrides)`, `sheetReport(overrides)`, `rowReport(overrides)`.
  - `mockImportBackend({ preview, commit })`: ghi lại request, trả báo cáo.

- [ ] **Step 1: Viết test `ImportPage.test.tsx`**
  - `templateDownloadsWithTheWorkspaceName`: bấm "Tải mẫu" thì gọi `GET /api/registry/imports/template.xlsx`, tên file bắt đầu bằng slug của không gian và có `-mau-nhap-`.
  - `choosingAFileSendsAPreview`: `user.upload` vào ô "Chọn file .xlsx" thì có `POST /imports/preview` mang file. Thấy "Đang đọc file…", rồi bước "Xem trước" được tô.
  - `summaryTabsAndRows`: báo cáo có Người 2 (1 lỗi) và Đồ đạc 3. Dải số là "Người 2 (1 lỗi)" và "Đồ đạc 3". Tab Người có dòng 3 với "Email không hợp lệ" và tên cột "Email". Dòng ổn có "Ổn".
  - `errorsOnlySwitchIsOnWhenThereAreErrors`: công tắc bật sẵn, chỉ hiện dòng lỗi. Tắt thì hiện mọi dòng.
  - `importIsLockedWhileThereAreErrors`: nút "Nhập 5 dòng" bị khoá, có "Sửa 1 lỗi trong file rồi chọn lại file".
  - `fileErrorsAndNotices`: `fileErrors` hiện trong `Alert` đỏ (role alert). `notices` hiện riêng.
  - `showMoreAfterTwoHundredRows`: 250 dòng thì hiện 200, bấm "Hiện thêm" thì đủ 250.
  - `sidebarAndOverviewLinkToTheImportPage`: thanh bên có link "Nhập từ Excel" tới `/w/7/import`; Tổng quan có link "Nhập Excel".
  - `tooLargeAndOldRegistry`: 413 thì "File lớn hơn 5 MB". 404 thì "Máy chủ chưa hỗ trợ nhập Excel; cần cập nhật registry".
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.**
  - Sinh lại kiểu: chạy jar registry ở cổng 18082 (bộ preview), rồi `HP_REGISTRY_PORT=18082 npm run api:sync -- --only=registry`.
  - Ô chọn file `accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"`, có nhãn "Chọn file .xlsx". Vùng thả nhận `drop`.
  - Bảng: `Table` trong `ScrollArea`.
  - Tab là `Tabs`, theo thứ tự `sheets`. Tab đầu tiên có lỗi được chọn sẵn.
- [ ] **Step 4: `npm test` cả bộ, `npm run build`, `npm run lint` → xanh.**
- [ ] **Step 5: Commit** "Add the import page with a preview".

### Task 10: Bước ③ nhập (frontend)

**Files:**
- Create: `features/imports/components/ImportResult.tsx`
- Modify: `features/imports/pages/ImportPage.tsx`, `queries.ts`; test `ImportPage.test.tsx`

**Interfaces:**
- Consumes: `POST /imports` (Task 4), `useRunImport` (Task 9).
- Produces: `useRunImport` khi `committed` thì `queryClient.invalidateQueries()` (toàn bộ).

- [ ] **Step 1: Viết test**
  - `importSendsTheSameFileAndShowsTheResult`: báo cáo xem trước không lỗi. Bấm "Nhập 5 dòng" thì `POST /imports` mang cùng tên file và cùng kích thước. Trong lúc chờ có "Đang nhập, đừng đóng trang" và một `progressbar`. Xong thì "Đã nhập 5 hồ sơ", số theo sheet, link "Tổng quan", "Người", "Đồ đạc", "Hoạt động".
  - `importButtonLocksWhileRunning`: bấm hai lần thì chỉ có một `POST /imports`.
  - `dataChangedSincePreview`: `committed: false` kèm lỗi thì quay về bước ②, có "Dữ liệu đã thay đổi từ lúc xem trước", bảng là báo cáo mới.
  - `unreadableFileAsksToChooseAgain`: sau bước xem trước, thay `arrayBuffer` của file đã chọn bằng hàm trả `Promise.reject(new DOMException('gone', 'NotReadableError'))`. Bấm Nhập thì hiện "Không đọc lại được file; chọn lại file" và không có `POST /imports`.
  - `importAnotherFileStartsOver`: "Nhập file khác" thì về bước ①, không còn báo cáo.
  - `successRefreshesWorkspaceData`: sau khi nhập, query `['overview', '7']` được tải lại.
- [ ] **Step 2: Chạy, thấy FAIL.**
- [ ] **Step 3: Cài đặt.** `previewImport` và `runImport` đọc `await file.arrayBuffer()` trước khi gửi (file tối đa 5 MB), rồi gửi `new File([buffer], file.name, { type: file.type })`. Đọc hỏng thì ném `UnreadableFileError` (lớp riêng của `features/imports`), và trang hiện chữ "Không đọc lại được file; chọn lại file".
- [ ] **Step 4: `npm test`, `npm run build`, `npm run lint` → xanh.**
- [ ] **Step 5: Commit** "Import the previewed file and show what landed".

### Task 11: Xem bằng mắt, tài liệu

**Files:**
- Modify:
  - `frontend/README.md`: cấu trúc `features/imports`, luật phụ thuộc "`imports` chỉ nhập `exports` (`ExportButton` cho file mẫu)", mục "Nhập từ Excel";
  - spec: trạng thái "đã xây";
  - `.superpowers/preview/` (file nhập mẫu cho preview, không commit nếu thư mục bị git bỏ qua).

- [ ] **Step 1:** Dựng bộ preview như Đợt 8 (mock identity 18081, jar registry 18082, Vite 15173, Chrome 9222). Tạo một file mẫu có dòng ổn, cảnh báo và lỗi.
- [ ] **Step 2:** Chụp khổ 1920, 1440, 390, nền sáng và nền tối: bước ①, bước ② (tab có lỗi, công tắc), bước ③ đang chạy và xong. Sửa chỗ vỡ: bảng tràn, tab xuống dòng, nút trên điện thoại.
- [ ] **Step 3:** `npm test`, `npm run build`, `npm run lint`, `mvnw -B verify` → xanh. Dừng bộ preview, xoá `chrome-profile/`.
- [ ] **Step 4: Commit** "Document the Excel import".
