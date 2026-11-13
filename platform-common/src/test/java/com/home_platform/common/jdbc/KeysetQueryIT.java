package com.home_platform.common.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.home_platform.common.CommonIT;
import com.home_platform.common.context.WorkspaceContext;
import com.home_platform.common.web.BadRequestException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class KeysetQueryIT extends CommonIT {

	@Autowired
	SampleDocRepository docs;

	@Test
	void pagesThroughAllRowsInNameOrder() {
		for (String name : List.of("E", "C", "A", "D", "B")) {
			add(CTX_A, 1, name);
		}
		SampleDoc z = add(CTX_A, 1, "Z");
		this.docs.softDelete(CTX_A, z.id(), 1);
		add(CTX_A, 2, "X");
		add(CTX_B, 1, "Y");

		CursorPage<SampleDoc> first = this.docs.listByParent(WS_A, 1, new CursorRequest(null, 2));
		assertThat(names(first)).containsExactly("A", "B");
		assertThat(first.nextCursor()).isNotNull();

		CursorPage<SampleDoc> second = this.docs.listByParent(WS_A, 1, new CursorRequest(first.nextCursor(), 2));
		assertThat(names(second)).containsExactly("C", "D");
		assertThat(second.nextCursor()).isNotNull();

		CursorPage<SampleDoc> third = this.docs.listByParent(WS_A, 1, new CursorRequest(second.nextCursor(), 2));
		assertThat(names(third)).containsExactly("E");
		assertThat(third.nextCursor()).isNull();
	}

	@Test
	void exactMultipleOfLimitEndsWithoutEmptyPage() {
		for (String name : List.of("A", "B", "C", "D")) {
			add(CTX_A, 1, name);
		}

		CursorPage<SampleDoc> first = this.docs.listByParent(WS_A, 1, new CursorRequest(null, 2));
		CursorPage<SampleDoc> second = this.docs.listByParent(WS_A, 1, new CursorRequest(first.nextCursor(), 2));

		assertThat(names(second)).containsExactly("C", "D");
		assertThat(second.nextCursor()).isNull();
	}

	@Test
	void pagingOverEqualNamesNeitherSkipsNorRepeats() {
		List<Long> added = new ArrayList<>();
		for (String name : List.of("Phòng a", "Phòng A", "phòng a", "Phòng a", "PHÒNG A")) {
			added.add(add(CTX_A, 1, name).id());
		}

		List<Long> seen = collect(page -> this.docs.listByParent(WS_A, 1, page), 2);

		assertThat(seen).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(added);
	}

	@Test
	void descendingOrderWorks() {
		for (String name : List.of("C", "A", "E", "B", "D")) {
			add(CTX_A, 1, name);
		}

		CursorPage<SampleDoc> first = this.docs.listByParentDesc(WS_A, 1, new CursorRequest(null, 2));
		CursorPage<SampleDoc> second = this.docs.listByParentDesc(WS_A, 1, new CursorRequest(first.nextCursor(), 2));
		CursorPage<SampleDoc> third = this.docs.listByParentDesc(WS_A, 1, new CursorRequest(second.nextCursor(), 2));

		assertThat(names(first)).containsExactly("E", "D");
		assertThat(names(second)).containsExactly("C", "B");
		assertThat(names(third)).containsExactly("A");
		assertThat(third.nextCursor()).isNull();
	}

	@Test
	void withoutOrderByRowsComeInIdOrder() {
		List<Long> added = new ArrayList<>();
		for (String name : List.of("C", "A", "B", "E", "D")) {
			added.add(add(CTX_A, 1, name).id());
		}

		List<Long> seen = collect(page -> this.docs.listAll(WS_A, page), 2);

		assertThat(seen).isSortedAccordingTo(Long::compare).containsExactlyInAnyOrderElementsOf(added);
	}

	@Test
	void cursorOnAnInstantColumnWorks() {
		List<Long> added = new ArrayList<>();
		for (String name : List.of("A", "B", "C")) {
			added.add(add(CTX_A, 1, name).id());
		}

		List<Long> seen = collect(page -> this.docs.listByCreatedAt(WS_A, page), 2);

		assertThat(seen).containsExactlyElementsOf(added);
	}

	@Test
	void cursorFromAnotherQueryIs400() {
		for (String name : List.of("A", "B", "C")) {
			add(CTX_A, 1, name);
		}
		String idOnly = this.docs.listAll(WS_A, new CursorRequest(null, 1)).nextCursor();

		assertThatThrownBy(() -> this.docs.listByParent(WS_A, 1, new CursorRequest(idOnly, 1)))
			.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> this.docs.listByParent(WS_A, 1, new CursorRequest("garbage", 1)))
			.isInstanceOf(BadRequestException.class);
		String byName = this.docs.listByParent(WS_A, 1, new CursorRequest(null, 1)).nextCursor();
		assertThatThrownBy(() -> this.docs.listByCreatedAt(WS_A, new CursorRequest(byName, 1)))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void emptyResultIsAnEmptyPage() {
		CursorPage<SampleDoc> page = this.docs.listByParent(WS_A, 1, new CursorRequest(null, 20));

		assertThat(page.items()).isEmpty();
		assertThat(page.nextCursor()).isNull();
	}

	private SampleDoc add(WorkspaceContext ctx, long parentId, String name) {
		return this.docs.insert(ctx, SampleDoc.named(name).withParentId(parentId));
	}

	/** Đi qua mọi trang, gom {@code id} theo thứ tự nhận được. */
	private static List<Long> collect(Function<CursorRequest, CursorPage<SampleDoc>> list, int limit) {
		List<Long> ids = new ArrayList<>();
		String cursor = null;
		for (int guard = 0; guard < 100; guard++) {
			CursorPage<SampleDoc> page = list.apply(new CursorRequest(cursor, limit));
			page.items().forEach(doc -> ids.add(doc.id()));
			cursor = page.nextCursor();
			if (cursor == null) {
				return ids;
			}
		}
		throw new AssertionError("Paging did not end");
	}

	private static List<String> names(CursorPage<SampleDoc> page) {
		return page.items().stream().map(SampleDoc::name).toList();
	}

}
