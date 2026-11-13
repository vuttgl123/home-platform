package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TimeZone;

import com.home_platform.common.CommonIT;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.NotFoundException;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.ErrorResponse;

class WorkspaceRepositoryIT extends CommonIT {

	@Autowired
	SampleDocRepository docs;

	@Autowired
	SampleTagRepository tags;

	@Test
	void insertFillsStandardColumnsAndReturnsWhatIsStored() {
		SampleDoc stored = this.docs.insert(CTX_A, room());

		assertThat(stored.id()).isPositive();
		assertThat(stored.workspaceId()).isEqualTo(WS_A);
		assertThat(stored.version()).isEqualTo(1);
		assertThat(stored.createdBy()).isEqualTo(USER);
		assertThat(stored.updatedBy()).isEqualTo(USER);
		assertThat(stored.createdAt()).isEqualTo(NOW);
		assertThat(stored.updatedAt()).isEqualTo(NOW);
		assertThat(stored.deletedAt()).isNull();
		assertThat(stored.name()).isEqualTo("Phòng họp 🏢");
		assertThat(this.docs.findById(WS_A, stored.id())).contains(stored);
	}

	@Test
	void presetIdIsKept() {
		SampleDoc stored = this.docs.insert(CTX_A, room().withId(555));

		assertThat(stored.id()).isEqualTo(555);
		assertThat(this.docs.findById(WS_A, 555)).contains(stored);
	}

	@Test
	void nullableColumnsRoundTripAsNull() {
		SampleDoc stored = this.docs.insert(CTX_A, SampleDoc.named("bare"));

		SampleDoc read = this.docs.getById(WS_A, stored.id());
		assertThat(read.parentId()).isNull();
		assertThat(read.rating()).isNull();
		assertThat(read.price()).isNull();
		assertThat(read.validFrom()).isNull();
		assertThat(read.attributes()).isNull();
		assertThat(read).isEqualTo(stored);
	}

	@Test
	void largeNumberInJsonColumnStaysANumber() {
		SampleDoc stored = this.docs.insert(CTX_A, room().withAttributes(Map.of("big", 9007199254740993L)));

		String jsonType = this.jdbc
			.sql("SELECT JSON_TYPE(JSON_EXTRACT(attributes, '$.big')) FROM tmp_sample_doc "
					+ "WHERE workspace_id = :workspaceId AND id = :id")
			.param("workspaceId", WS_A)
			.param("id", stored.id())
			.query(String.class)
			.single();
		// MySQL ghi số nguyên dương lớn với kiểu UNSIGNED INTEGER; điều cần giữ là nó vẫn là số, không thành chuỗi.
		assertThat(jsonType).isIn("INTEGER", "UNSIGNED INTEGER");
		assertThat(this.docs.getById(WS_A, stored.id()).attributes()).containsEntry("big", 9007199254740993L);
	}

	@Test
	void instantIsStoredAsUtcRegardlessOfJvmTimeZone() {
		TimeZone original = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		try {
			SampleDoc stored = this.docs.insert(CTX_A, room());

			String raw = this.jdbc
				.sql("SELECT CAST(created_at AS CHAR) FROM tmp_sample_doc WHERE workspace_id = :workspaceId AND id = :id")
				.param("workspaceId", WS_A)
				.param("id", stored.id())
				.query(String.class)
				.single();
			assertThat(raw).isEqualTo("2026-11-12 06:40:00.123");
			assertThat(this.docs.getById(WS_A, stored.id()).createdAt()).isEqualTo(NOW);
		}
		finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	void updateWritesBusinessColumnsAndBumpsVersion() {
		SampleDoc stored = this.docs.insert(CTX_A, room());
		WorkspaceContext editor = new WorkspaceContext(WS_A, -202L);

		SampleDoc updated = this.docs.update(editor, stored.withName("B").withAttributes(Map.of("k", "v")));

		assertThat(updated.name()).isEqualTo("B");
		assertThat(updated.attributes()).isEqualTo(Map.of("k", "v"));
		assertThat(updated.version()).isEqualTo(2);
		assertThat(updated.updatedBy()).isEqualTo(-202L);
		assertThat(updated.createdBy()).isEqualTo(USER);
		assertThat(this.docs.findById(WS_A, stored.id())).contains(updated);
	}

	@Test
	void staleVersionIsAConflict() {
		SampleDoc read = this.docs.insert(CTX_A, room());
		this.docs.update(CTX_A, read.withName("first"));

		assertThatThrownBy(() -> this.docs.update(CTX_A, read.withName("second")))
			.isInstanceOf(ConflictException.class)
			.satisfies(detail("SampleDoc " + read.id() + " was changed by someone else"));
		assertThat(this.docs.getById(WS_A, read.id()).name()).isEqualTo("first");
	}

	@Test
	void updateOfMissingRowIsNotFound() {
		assertThatThrownBy(() -> this.docs.update(CTX_A, room().withId(999).withVersion(1)))
			.isInstanceOf(NotFoundException.class)
			.satisfies(detail("SampleDoc 999 not found"));
	}

	@Test
	void rowOfAnotherWorkspaceIsInvisibleAndUntouched() {
		SampleDoc a = this.docs.insert(CTX_A, room());
		SampleTag tag = this.tags.insert(CTX_A, SampleTag.of("red", "Red"));

		assertThat(this.docs.findById(WS_B, a.id())).isEmpty();
		assertThat(this.docs.findByIdIncludingDeleted(WS_B, a.id())).isEmpty();
		assertThatThrownBy(() -> this.docs.getById(WS_B, a.id())).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.docs.update(CTX_B, a.withName("x"))).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.docs.softDelete(CTX_B, a.id(), 1)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.docs.restore(CTX_B, a.id(), 1)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.tags.update(CTX_B, tag.withName("x"))).isInstanceOf(NotFoundException.class);
		assertThat(this.tags.deletePermanently(WS_B, tag.id())).isFalse();

		assertThat(this.docs.findById(WS_A, a.id())).contains(a);
		assertThat(this.tags.findById(WS_A, tag.id())).contains(tag);
	}

	@Test
	void softDeleteHidesTheRowAndRestoreBringsItBack() {
		SampleDoc stored = this.docs.insert(CTX_A, room());
		long id = stored.id();

		SampleDoc deleted = this.docs.softDelete(CTX_A, id, 1);

		assertThat(deleted.version()).isEqualTo(2);
		assertThat(deleted.deletedAt()).isEqualTo(NOW);
		assertThat(this.docs.findById(WS_A, id)).isEmpty();
		assertThatThrownBy(() -> this.docs.getById(WS_A, id)).isInstanceOf(NotFoundException.class);
		assertThat(this.docs.findByIdIncludingDeleted(WS_A, id)).contains(deleted);
		assertThatThrownBy(() -> this.docs.update(CTX_A, deleted.withName("x"))).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.docs.softDelete(CTX_A, id, 2)).isInstanceOf(NotFoundException.class);

		SampleDoc restored = this.docs.restore(CTX_A, id, 2);

		assertThat(restored.version()).isEqualTo(3);
		assertThat(restored.deletedAt()).isNull();
		assertThat(this.docs.findById(WS_A, id)).contains(restored);
		assertThatThrownBy(() -> this.docs.restore(CTX_A, id, 3)).isInstanceOf(ConflictException.class)
			.satisfies(detail("SampleDoc " + id + " is not deleted"));
	}

	@Test
	void softDeleteWithStaleVersionIsAConflict() {
		SampleDoc stored = this.docs.insert(CTX_A, room());

		assertThatThrownBy(() -> this.docs.softDelete(CTX_A, stored.id(), 99)).isInstanceOf(ConflictException.class)
			.satisfies(detail("SampleDoc " + stored.id() + " was changed by someone else"));
		assertThat(this.docs.findById(WS_A, stored.id())).contains(stored);
	}

	@Test
	void restoreOfMissingRowIsNotFoundAndStaleVersionIsAConflict() {
		SampleDoc stored = this.docs.insert(CTX_A, room());
		SampleDoc deleted = this.docs.softDelete(CTX_A, stored.id(), 1);

		assertThatThrownBy(() -> this.docs.restore(CTX_A, 999, 1)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> this.docs.restore(CTX_A, stored.id(), 99)).isInstanceOf(ConflictException.class)
			.satisfies(detail("SampleDoc " + stored.id() + " was changed by someone else"));
		assertThat(this.docs.findByIdIncludingDeleted(WS_A, stored.id())).contains(deleted);
	}

	@Test
	void tableWithoutStandardColumnsStillWorks() {
		SampleTag tag = this.tags.insert(CTX_A, SampleTag.of("red", "Red"));

		assertThat(tag.id()).isPositive();
		assertThat(tag.workspaceId()).isEqualTo(WS_A);
		assertThat(tag.createdAt()).isEqualTo(NOW);
		assertThat(tag.updatedAt()).isEqualTo(NOW);
		assertThat(this.tags.findById(WS_A, tag.id())).contains(tag);

		// Không có cột version: hai lần sửa từ cùng một bản đã đọc đều thành công.
		this.tags.update(CTX_A, tag.withName("one"));
		SampleTag second = this.tags.update(CTX_A, tag.withName("two"));
		assertThat(second.name()).isEqualTo("two");
		assertThat(this.tags.findById(WS_A, tag.id())).contains(second);

		assertThat(this.tags.deletePermanently(WS_A, tag.id())).isTrue();
		assertThat(this.tags.deletePermanently(WS_A, tag.id())).isFalse();
		assertThatThrownBy(() -> this.tags.softDelete(CTX_A, tag.id(), 0))
			.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> this.tags.restore(CTX_A, tag.id(), 0))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void updateThatChangesNothingStillSucceeds() {
		SampleTag tag = this.tags.insert(CTX_A, SampleTag.of("red", "Red"));

		// Đồng hồ đứng yên nên dòng không đổi một byte nào; lệnh vẫn phải được coi là trúng dòng.
		assertThat(this.tags.update(CTX_A, tag)).isEqualTo(tag);
	}

	@Test
	void deletePermanentlyIsRefusedOnSoftDeletableTable() {
		SampleDoc stored = this.docs.insert(CTX_A, room());

		assertThatThrownBy(() -> this.docs.deletePermanently(WS_A, stored.id()))
			.isInstanceOf(UnsupportedOperationException.class);
		assertThat(this.docs.findById(WS_A, stored.id())).contains(stored);
	}

	@Test
	void duplicateKeySurfacesAsDuplicateKeyException() {
		this.tags.insert(CTX_A, SampleTag.of("red", "Red"));

		assertThatThrownBy(() -> this.tags.insert(CTX_A, SampleTag.of("red", "Other")))
			.isInstanceOf(DuplicateKeyException.class);
	}

	/** Các lỗi của repository là {@link ErrorResponse}: nội dung cho client nằm ở {@code detail}. */
	private static ThrowingConsumer<Throwable> detail(String expected) {
		return ex -> assertThat(((ErrorResponse) ex).getBody().getDetail()).isEqualTo(expected);
	}

	private static SampleDoc room() {
		return SampleDoc.named("Phòng họp 🏢")
			.withPinned(true)
			.withRating(4)
			.withPrice(new BigDecimal("12.50"))
			.withValidFrom(LocalDate.of(2026, 11, 1))
			.withAttributes(Map.of("area", 42));
	}

}
