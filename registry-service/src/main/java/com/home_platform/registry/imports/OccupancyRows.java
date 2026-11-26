package com.home_platform.registry.imports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.home_platform.registry.export.ExportLabels;
import com.home_platform.registry.occupancy.OccupancyCreateRequest;
import com.home_platform.registry.occupancy.OccupancyService;
import com.home_platform.registry.occupancy.Relation;
import org.springframework.stereotype.Component;

/** Sheet Cư trú (spec N2): mỗi dòng là một lần thêm người ở như ở form, theo thứ tự dòng. */
@Component
class OccupancyRows implements SheetRows {

	static final String OWNER_RETIRED = "Quan hệ Chủ đã bỏ; ghi người này ở sheet Sở hữu";

	private static final Map<String, String> COLUMNS = Map.of("placeId", StayRefs.PLACE, "personId", StayRefs.PERSON,
			"relation", "Quan hệ", "startDate", "Từ ngày", "endDate", "Đến ngày", "note", "Ghi chú");

	private static final Map<String, Relation> RELATIONS = new LinkedHashMap<>();

	private static final List<String> RELATION_LABELS = new ArrayList<>();

	static {
		for (Relation relation : Relation.values()) {
			RELATIONS.put(ImportText.key(ExportLabels.relation(relation)), relation);
			RELATIONS.put(ImportText.key(relation.name()), relation);
			if (relation != Relation.OWNER) {
				RELATION_LABELS.add(ExportLabels.relation(relation));
			}
		}
	}

	private final OccupancyService occupancies;

	private final RowRunner runner;

	OccupancyRows(OccupancyService occupancies, RowRunner runner) {
		this.occupancies = occupancies;
		this.runner = runner;
	}

	@Override
	public SheetKind kind() {
		return SheetKind.OCCUPANCY;
	}

	@Override
	public List<RowReport> importAll(ImportSheet sheet, ImportRun run) {
		List<RowReport> reports = new ArrayList<>();
		for (ImportRow row : sheet.rows()) {
			RowReader reader = new RowReader(row);
			StayRefs refs = StayRefs.read(reader, run);
			Relation relation = reader.choice("Quan hệ", RELATIONS, RELATION_LABELS);
			if (relation == Relation.OWNER) {
				reader.error("Quan hệ", OWNER_RETIRED);
			}
			else if (relation == null && !reader.hasErrorIn("Quan hệ")) {
				relation = Relation.RESIDENT;
			}
			OccupancyCreateRequest request = new OccupancyCreateRequest(refs.placeId(), refs.personId(), relation,
					reader.date("Từ ngày"), reader.date("Đến ngày"), reader.text("Ghi chú"));
			if (refs.resolved()) {
				this.runner.validate(request, reader, COLUMNS);
			}
			else if (request.startDate() == null && !reader.hasErrorIn("Từ ngày")) {
				reader.error("Từ ngày", ImportMessages.REQUIRED);
			}
			this.runner.run(reader, COLUMNS, () -> this.occupancies.create(run.ctx(), request));
			reports.add(reader.report(refs.label(), null));
		}
		return reports;
	}

}
