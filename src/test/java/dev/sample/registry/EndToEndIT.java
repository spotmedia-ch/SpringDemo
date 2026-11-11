package dev.sample.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class EndToEndIT {

	@Autowired
	private MockMvc mvc;

	@Test
	void aKeyValidatesUntilItIsRevoked() throws Exception {
		var suffix = UUID.randomUUID().toString();

		var api = json(post("/api/v1/apis").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"orders-%s","version":"v1","basePath":"/orders","ownerTeam":"team-a"}""".formatted(suffix)));
		var apiId = id(api);
		json(post("/api/v1/apis/" + apiId + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status":"PUBLISHED","rowVersion":%s}""".formatted(field(api, "rowVersion"))));

		var consumer = json(post("/api/v1/consumers").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"acme-%s","contactEmail":"ops@example.com"}""".formatted(suffix)));
		var consumerId = id(consumer);

		var subscription = json(post("/api/v1/subscriptions").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"apiId":"%s","consumerId":"%s","plan":"PREMIUM"}""".formatted(apiId, consumerId)));
		var subscriptionId = id(subscription);

		var issued = json(post("/api/v1/subscriptions/" + subscriptionId + "/keys"));
		var plaintextKey = field(issued, "apiKey");
		var keyId = id(issued);

		var before = validate(plaintextKey, apiId);
		before.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.plan").value("PREMIUM"))
			.andExpect(jsonPath("$.rateLimitPerMinute").value(6000));

		mvc.perform(get("/api/v1/subscriptions/" + subscriptionId + "/keys"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].keyPrefix").exists())
			.andExpect(jsonPath("$[0].apiKey").doesNotExist());

		mvc.perform(post("/api/v1/keys/" + keyId + "/revoke")).andExpect(status().isNoContent());

		validate(plaintextKey, apiId).andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.reason").value("REVOKED"));
	}

	@Test
	void everyListEndpointPagesAndRejectsUnknownIdsWithAProblem() throws Exception {
		mvc.perform(get("/api/v1/apis?size=500"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(100))
			.andExpect(jsonPath("$.items").isArray());
		mvc.perform(get("/api/v1/consumers")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
		mvc.perform(get("/api/v1/subscriptions")).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").exists());
		mvc.perform(get("/api/v1/apis/" + UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	void creatingAnApiReturnsALocationHeaderThatCanBeFollowed() throws Exception {
		var result = mvc.perform(post("/api/v1/apis").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"loc-%s","version":"v1","basePath":"/loc","ownerTeam":"team-a"}""".formatted(UUID.randomUUID())))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andReturn();

		var location = result.getResponse().getHeader("Location");

		mvc.perform(get(java.net.URI.create(location))).andExpect(status().isOk());
	}

	private ResultActions validate(String apiKey, String apiId) throws Exception {
		return mvc.perform(post("/api/v1/keys/validate").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"apiKey":"%s","apiId":"%s"}""".formatted(apiKey, apiId)));
	}

	private String json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
			throws Exception {
		var response = mvc.perform(request).andReturn().getResponse();
		assertThat(response.getStatus()).isIn(200, 201);
		return response.getContentAsString();
	}

	private static String id(String body) {
		return field(body, "id");
	}

	private static String field(String body, String name) {
		return String.valueOf((Object) JsonPath.read(body, "$." + name));
	}

}
