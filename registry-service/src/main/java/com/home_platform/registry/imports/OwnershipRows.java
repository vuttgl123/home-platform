package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.ownership.OwnershipCreateRequest;
import com.home_platform.registry.ownership.OwnershipService;
import org.springframework.stereotype.Component;

/** Sheet Sở hữu (spec N2): mỗi dòng là một lần thêm chủ sở hữu như ở form, theo thứ tự dòng. */
@Component
class OwnershipRows implements SheetRows {

	private static final Map<String, String> COLUMNS = Map.of("placeId", StayRefs.PLACE, "personId", StayRefs.PERSON,
			"sharePercent", "Tỷ lệ (%)", "startDate", "Từ ngày", "endDate", "Đến ngày", "documentNo", "Số giấy tờ",
			"note", "Ghi chú");

	private final OwnershipService ownerships;

	private final RowRunner runner;

	OwnershipRows(OwnershipService ownerships, RowRunner runner) {
		this.ownerships = ownerships;
		this.runner = runner;
	}

	@Override
	public SheetKind kind() {
		return SheetKind.OWNERSHIP;
	}

	@Override
	public List<RowReport> importAll(ImportSheet sheet, ImportRun run) {
		List<RowReport> reports = new ArrayList<>();
		for (ImportRow row : sheet.rows()) {
			RowReader reader = new RowReader(row);
			StayRefs refs = StayRefs.read(reader, run);
			OwnershipCreateRequest request = new OwnershipCreateRequest(refs.placeId(), refs.personId(),
					reader.decimal("Tỷ lệ (%)"), reader.date("Từ ngày"), reader.date("Đến ngày"),
					reader.text("Số giấy tờ"), reader.text("Ghi chú"));
			if (refs.resolved()) {
				this.runner.validate(request, reader, COLUMNS);
			}
			else if (request.startDate() == null && !reader.hasErrorIn("Từ ngày")) {
				reader.error("Từ ngày", ImportMessages.REQUIRED);
			}
			this.runner.run(reader, COLUMNS, () -> this.ownerships.create(run.ctx(), request));
			reports.add(reader.report(refs.label(), null));
		}
		return reports;
	}

}
