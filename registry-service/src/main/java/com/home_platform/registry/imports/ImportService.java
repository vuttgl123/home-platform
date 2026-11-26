package com.home_platform.registry.imports;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.registry.export.AttributeColumns;
import com.home_platform.registry.fielddef.FieldDef;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import com.home_platform.registry.imports.ImportReport.Totals;
import com.home_platform.registry.itemcategory.ItemCategory;
import com.home_platform.registry.itemcategory.ItemCategoryService;
import com.home_platform.registry.placetype.PlaceType;
import com.home_platform.registry.placetype.PlaceTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Xem trước và nhập một file Excel (spec Đợt 9). Hai việc chạy cùng một đoạn code, gọi đúng các lệnh
 * tạo của form, trong một savepoint: xem trước luôn quay về savepoint đó, nhập thì chỉ giữ lại khi
 * không có lỗi nào (tất cả hoặc không, N7).
 */
@Service
public class ImportService {

	/**
	 * Giới hạn thời gian của một lần chạy, thay cho mặc định 30 giây của nền tảng: một file đủ
	 * {@value WorkbookReader#MAX_ROWS} dòng cần khoảng 40 giây trên database UAT. Hết hạn thì mọi dòng
	 * còn lại lỗi, nên giới hạn này để dư gấp ba.
	 */
	static final int TIMEOUT_SECONDS = 120;

	private final TransactionTemplate savepoint;

	private final ImportLookupRepository lookups;

	private final FieldDefService fieldDefs;

	private final PlaceTypeService placeTypes;

	private final ItemCategoryService categories;

	private final Map<SheetKind, SheetRows> sheetRows = new EnumMap<>(SheetKind.class);

	ImportService(PlatformTransactionManager transactions, ImportLookupRepository lookups, FieldDefService fieldDefs,
			PlaceTypeService placeTypes, ItemCategoryService categories, List<SheetRows> sheetRows) {
		this.savepoint = new TransactionTemplate(transactions);
		this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		this.savepoint.setTimeout(TIMEOUT_SECONDS);
		this.lookups = lookups;
		this.fieldDefs = fieldDefs;
		this.placeTypes = placeTypes;
		this.categories = categories;
		sheetRows.forEach(rows -> this.sheetRows.put(rows.kind(), rows));
	}

	/** Chạy thử cả file rồi huỷ: không để lại dòng, version hay số đếm mã nào. */
	public ImportReport preview(WorkspaceContext ctx, String fileName, byte[] file) {
		return run(ctx, fileName, file, false);
	}

	/** Chạy cả file; lưu lại chỉ khi không có lỗi nào, không thì như xem trước. */
	public ImportReport commit(WorkspaceContext ctx, String fileName, byte[] file) {
		return run(ctx, fileName, file, true);
	}

	private ImportReport run(WorkspaceContext ctx, String fileName, byte[] file, boolean keep) {
		return this.savepoint.execute(status -> {
			ImportReport report = importFile(ctx, fileName, file);
			if (keep && !report.hasErrors()) {
				return report.asCommitted();
			}
			status.setRollbackOnly();
			return report;
		});
	}

	private ImportReport importFile(WorkspaceContext ctx, String fileName, byte[] file) {
		long workspaceId = ctx.workspaceId();
		List<PlaceType> types = this.placeTypes.list(workspaceId);
		List<ItemCategory> categoryList = this.categories.list(workspaceId);
		Map<Long, String> typeNames = new HashMap<>();
		types.forEach(type -> typeNames.put(type.id(), type.name()));
		Map<Long, String> categoryNames = new HashMap<>();
		categoryList.forEach(category -> categoryNames.put(category.id(), category.name()));
		List<FieldDef> defs = this.fieldDefs.listAll(workspaceId);
		Map<SheetKind, AttributeColumns> attributes = new EnumMap<>(SheetKind.class);
		attributes.put(SheetKind.PLACE, AttributeColumns.of(defs, FieldTarget.PLACE, typeNames));
		attributes.put(SheetKind.PERSON, AttributeColumns.of(defs, FieldTarget.PERSON, Map.of()));
		attributes.put(SheetKind.ITEM, AttributeColumns.of(defs, FieldTarget.ITEM, categoryNames));
		Map<SheetKind, Set<String>> extraHeaders = new EnumMap<>(SheetKind.class);
		attributes.forEach((kind, columns) -> extraHeaders.put(kind, new HashSet<>(columns.headers())));

		ImportWorkbook workbook;
		try {
			workbook = WorkbookReader.read(new ByteArrayInputStream(file), extraHeaders);
		}
		catch (ImportFileException ex) {
			return new ImportReport(fileName, false, ex.errors(), List.of(), new Totals(0, 0, 0), List.of());
		}
		ImportRun run = new ImportRun(ctx, fileName, attributes, types, categoryList, this.lookups,
				workbook.notices());
		List<SheetReport> sheets = new ArrayList<>();
		for (ImportSheet sheet : workbook.sheets()) {
			SheetRows rows = this.sheetRows.get(sheet.kind());
			if (rows != null) {
				sheets.add(sheetReport(sheet, rows.importAll(sheet, run)));
			}
		}
		int total = sheets.stream().mapToInt(SheetReport::rows).sum();
		int errors = sheets.stream().mapToInt(SheetReport::errors).sum();
		int warnings = sheets.stream().mapToInt(SheetReport::warnings).sum();
		return new ImportReport(fileName, false, List.of(), run.notices(), new Totals(total, errors, warnings),
				List.copyOf(sheets));
	}

	private static SheetReport sheetReport(ImportSheet sheet, List<RowReport> rows) {
		int errors = (int) rows.stream().filter(row -> row.status() == RowStatus.ERROR).count();
		int warnings = (int) rows.stream().filter(row -> row.status() == RowStatus.WARNING).count();
		return new SheetReport(sheet.kind(), sheet.name(), rows.size(), errors, warnings, List.copyOf(rows));
	}

}
