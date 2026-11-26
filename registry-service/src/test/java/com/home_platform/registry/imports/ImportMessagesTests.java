package com.home_platform.registry.imports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.home_platform.common.web.ApiExceptionHandler;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.ConflictException;
import org.junit.jupiter.api.Test;

class ImportMessagesTests {

	private static final Map<String, String> COLUMNS = Map.of("startDate", "Từ ngày", "attributes.blood", "Nhóm máu");

	@Test
	void knownServiceErrorsAreTranslated() {
		assertText(new ConflictException("Person 12 already has this document"), "Đã có người dùng giấy tờ này");
		assertText(new ConflictException("Another person already has this document"), "Đã có người dùng giấy tờ này");
		assertText(new BadRequestException("idType and idNumber must be given together"),
				"Loại giấy tờ và Số giấy tờ phải ghi cùng nhau");
		assertText(new ConflictException("Person 12 already has an overlapping occupancy at this place"),
				"Người này đã có một lần ở chồng ngày tại địa điểm này");
		assertText(new ConflictException("Person 12 already has an overlapping ownership at this place"),
				"Người này đã có một lần sở hữu chồng ngày tại địa điểm này");
		assertText(new ConflictException("Ownership shares of place 7 would total 110% on 2026-01-01"),
				"Tổng tỷ lệ sở hữu của địa điểm sẽ là 110% vào ngày 01/01/2026");
		assertText(new BadRequestException("endDate must not be before startDate"), "Đến ngày không được trước Từ ngày");
		assertText(new ConflictException("Places can be nested at most 20 levels deep"),
				"Cây địa điểm không được sâu quá 20 cấp");
		assertText(new BadRequestException("removedAt must not be before placedAt"), "Ngày gỡ không được trước Ngày đặt");
		assertText(new ConflictException("Restore person 12 first"), "Chủ của món đã bị xoá");
		assertText(new ConflictException("Code P301 is already in use"), "Mã P301 đã có");
		assertText(new ConflictException("Code P301 is already in use; try again"), "Mã P301 đã có");
	}

	@Test
	void attributeErrorsLandOnTheirColumn() {
		BadRequestException ex = new BadRequestException("Invalid attributes");
		ex.getBody()
			.setProperty(ApiExceptionHandler.ERRORS_PROPERTY,
					List.of(Map.of("field", "attributes.blood", "message", "is required")));

		List<RowMessage> messages = ImportMessages.of(ex, COLUMNS);

		assertThat(messages).containsExactly(new RowMessage(MessageLevel.ERROR, "Nhóm máu", "Bắt buộc"));
	}

	@Test
	void unknownErrorsKeepTheirDetail() {
		assertText(new ConflictException("boom"), "Không nhập được dòng này (boom)");
		assertText(new IllegalStateException("boom"), "Không nhập được dòng này (boom)");
	}

	@Test
	void cellErrorsKeepTheirColumn() {
		assertThat(ImportMessages.of(new CellError("Ngày sinh", "Ngày không hợp lệ; ghi dạng dd/mm/yyyy"), COLUMNS))
			.containsExactly(new RowMessage(MessageLevel.ERROR, "Ngày sinh", "Ngày không hợp lệ; ghi dạng dd/mm/yyyy"));
	}

	private static void assertText(RuntimeException ex, String text) {
		assertThat(ImportMessages.of(ex, COLUMNS)).extracting(RowMessage::text).containsExactly(text);
	}

}
