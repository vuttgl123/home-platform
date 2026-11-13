package com.home_platform.registry.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.common.history.ChangeAction;
import com.home_platform.registry.fielddef.FieldDataType;
import com.home_platform.registry.fielddef.FieldDefCreateRequest;
import com.home_platform.registry.fielddef.FieldDefService;
import com.home_platform.registry.fielddef.FieldTarget;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ItemIT extends ItemTestData {

	@Autowired
	FieldDefService fieldDefs;

	@Test
	void createsAnItemInAPlace() throws Exception {
		long tv = category("Điện tử");

		postItem("""
				{"placeId": "%d", "categoryId": "%d", "name": " Tivi ", "code": "SN-1", "unit": "cái",
				 "placedAt": "2025-01-15"}""".formatted(this.room, tv))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isString())
			.andExpect(jsonPath("$.name").value("Tivi"))
			.andExpect(jsonPath("$.quantity").value(1))
			.andExpect(jsonPath("$.status").value("IN_USE"))
			.andExpect(jsonPath("$.placeName").value("P101"))
			.andExpect(jsonPath("$.categoryName").value("Điện tử"))
			.andExpect(jsonPath("$.removedAt").value(nullValue()))
			.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void createRecordsVersionOne() throws Exception {
		long id = item(this.room, "Ghế");

		assertThat(version(id, 1).action()).isEqualTo(ChangeAction.CREATE);
		assertThat(version(id, 1).snapshot().get("name").asString()).isEqualTo("Ghế");
		assertThat(version(id, 1).snapshot().has("version")).isFalse();
	}

	@Test
	void placeMustBeAlive() throws Exception {
		long gone = place(null, "Gone");
		this.places.delete(CTX_A, gone, 1, null);

		postItem("{\"placeId\": \"" + gone + "\", \"name\": \"x\"}").andExpect(status().isNotFound());
		postItem("{\"name\": \"x\"}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("placeId is required"));
	}

	@Test
	void archivedCategoryIsRejectedForNewItems() throws Exception {
		long old = category("Cũ");
		this.categories.archive(CTX_A, old);

		postItem("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"x\"}".formatted(this.room, old))
			.andExpect(status().isConflict());
	}

	@Test
	void keepingAnArchivedCategoryIsAllowed() throws Exception {
		long old = category("Cũ");
		long id = item("{\"placeId\": \"%d\", \"categoryId\": \"%d\", \"name\": \"x\"}".formatted(this.room, old));
		this.categories.archive(CTX_A, old);

		putItem(id, """
				{"version": 1, "categoryId": "%d", "name": "y", "status": "BROKEN"}""".formatted(old))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("BROKEN"));
	}

	@Test
	void statusRemovedCannotBeSetDirectly() throws Exception {
		postItem("{\"placeId\": \"%d\", \"name\": \"x\", \"status\": \"REMOVED\"}".formatted(this.room))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("status REMOVED is set by removing or replacing an item"));
		long id = item(this.room, "y");
		putItem(id, """
				{"version": 1, "name": "y", "status": "REMOVED"}""").andExpect(status().isBadRequest());
	}

	@Test
	void quantityMustBePositive() throws Exception {
		postItem("{\"placeId\": \"%d\", \"name\": \"x\", \"quantity\": 0}".formatted(this.room))
			.andExpect(status().isBadRequest());
	}

	@Test
	void attributesFollowTheCategory() throws Exception {
		long tv = category("Điện tử");
		this.fieldDefs.create(CTX_A, new FieldDefCreateRequest(FieldTarget.ITEM, tv, "warranty_until", "Bảo hành",
				FieldDataType.DATE, null, null, null));

		postItem("""
				{"placeId": "%d", "categoryId": "%d", "name": "Tivi", "attributes": {"warranty_until": "2027-01-01"}}"""
			.formatted(this.room, tv)).andExpect(status().isCreated());
		postItem("""
				{"placeId": "%d", "name": "Ghế", "attributes": {"warranty_until": "2027-01-01"}}""".formatted(this.room))
			.andExpect(status().isBadRequest());
	}

	@Test
	void updateWritesFieldsAndChecksVersion() throws Exception {
		long id = item(this.room, "Ghế");
		String json = """
				{"version": 1, "name": "Ghế gỗ", "quantity": 4, "status": "STORED", "note": "kiểm kê"}""";

		putItem(id, json).andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.quantity").value(4));
		assertThat(version(id, 2).changedFields()).contains("name", "quantity", "status");
		assertThat(version(id, 2).note()).isEqualTo("kiểm kê");
		putItem(id, json).andExpect(status().isConflict());
	}

	@Test
	void updateNeverMovesTheItem() throws Exception {
		long other = place(null, "P102");
		long id = item(this.room, "Ghế");

		putItem(id, """
				{"version": 1, "name": "Ghế", "status": "IN_USE", "placeId": "%d"}""".formatted(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.placeId").value(String.valueOf(this.room)));
	}

	@Test
	void otherWorkspaceSeesNothing() throws Exception {
		long id = item(this.room, "Ghế");

		this.mvc.perform(asB(get("/items/" + id))).andExpect(status().isNotFound());
	}

}
