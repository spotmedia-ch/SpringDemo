package dev.sample.registry.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

	private static final String VALID_BODY = """
			{"name":"payments","version":"v1","basePath":"/payments","ownerTeam":"team-a"}""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ApiService service;

	@Test
	void registeringAnApiReturns201WithALocationHeader() throws Exception {
		var id = UUID.randomUUID();
		when(service.create(any())).thenReturn(response(id));

		mvc.perform(post("/api/v1/apis").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/apis/" + id))
			.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	void registeringAnApiWithInvalidFieldsReturnsAProblemWithAPerFieldList() throws Exception {
		mvc.perform(post("/api/v1/apis").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"","version":"v1","basePath":"no-slash","ownerTeam":"team-a"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
			.andExpect(jsonPath("$.errors[?(@.field=='name')]").exists())
			.andExpect(jsonPath("$.errors[?(@.field=='basePath')]").exists());
	}

	@Test
	void readingAnUnknownApiReturnsA404Problem() throws Exception {
		var id = UUID.randomUUID();
		when(service.get(id)).thenThrow(new NotFoundException("API", id));

		mvc.perform(get("/api/v1/apis/" + id))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}

	@Test
	void anIllegalStatusMoveReturnsA409Problem() throws Exception {
		var id = UUID.randomUUID();
		when(service.changeStatus(any(), any())).thenThrow(new ConflictException("An API cannot move"));

		mvc.perform(post("/api/v1/apis/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status":"RETIRED","rowVersion":0}"""))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith("application/problem+json"));
	}

	@Test
	void aStatusChangeWithoutARowVersionIsRejected() throws Exception {
		mvc.perform(post("/api/v1/apis/" + UUID.randomUUID() + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status":"PUBLISHED"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field=='rowVersion')]").exists());
	}

	private static ApiResponse response(UUID id) {
		return new ApiResponse(id, "payments", "v1", "/payments", "team-a", null, ApiStatus.DRAFT, Instant.now(),
				Instant.now(), 0L);
	}

}
