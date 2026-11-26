package com.home_platform.registry.imports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.home_platform.registry.RegistryIT;
import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** Khung cho IT của việc nhập: gửi file qua {@code POST /imports/preview} và {@code POST /imports}. */
abstract class ImportTestData extends RegistryIT {

	static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	protected ResultActions preview(byte[] file) throws Exception {
		return send("/imports/preview", "import.xlsx", file);
	}

	protected ResultActions commit(byte[] file) throws Exception {
		return commit("import.xlsx", file);
	}

	protected ResultActions commit(String fileName, byte[] file) throws Exception {
		return send("/imports", fileName, file);
	}

	/** Gửi file vào không gian {@code WS_B}. */
	protected ResultActions commitToB(byte[] file) throws Exception {
		return this.mvc.perform(upload("/imports", "import.xlsx", file, WS_B));
	}

	protected ResultActions send(String path, String fileName, byte[] file) throws Exception {
		return this.mvc.perform(upload(path, fileName, file, WS_A));
	}

	/** Request multipart mang file ở phần {@code file}, với tư cách {@code USER} trong không gian {@code workspaceId}. */
	protected static MockMultipartHttpServletRequestBuilder upload(String path, String fileName, byte[] file,
			long workspaceId) {
		return multipart(path).file(new MockMultipartFile("file", fileName, XLSX, file))
			.header("X-Workspace-Id", workspaceId)
			.header("X-User-Id", USER);
	}

	/** Báo cáo của một lần gửi trả 200. */
	protected static String report(ResultActions result) throws Exception {
		return result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	protected static <T> T read(String json, String path) {
		return JsonPath.read(json, path);
	}

	/** {@code id} của người chưa xoá tên {@code fullName} trong {@code WS_A}. */
	protected long personId(String fullName) {
		return this.jdbc.sql("SELECT id FROM reg_person WHERE workspace_id = :ws AND full_name = :name AND deleted_at IS NULL")
			.param("ws", WS_A)
			.param("name", fullName)
			.query(Long.class)
			.single();
	}

	protected int count(String table) {
		return this.jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE workspace_id = :ws")
			.param("ws", WS_A)
			.query(Integer.class)
			.single();
	}

	/** Ghi chú của version {@code versionNo} của một bản ghi. */
	protected String versionNote(String entityType, long id, int versionNo) {
		return this.jdbc.sql("SELECT note FROM reg_entity_version WHERE workspace_id = :ws AND entity_type = :type "
				+ "AND entity_id = :id AND version_no = :no")
			.param("ws", WS_A)
			.param("type", entityType)
			.param("id", id)
			.param("no", versionNo)
			.query(String.class)
			.single();
	}

}
