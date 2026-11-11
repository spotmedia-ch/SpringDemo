package dev.sample.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The committed docs/openapi.json is the API contract. This fails when the code and the file disagree.
 * Run with -Dopenapi.update=true to rewrite the file from the running application.
 */
@IntegrationTest
class OpenApiContractIT {

	private static final Path CONTRACT = Path.of("docs/openapi.json");

	private final JsonMapper mapper = JsonMapper.builder().build();

	@Autowired
	private MockMvc mvc;

	@Test
	void theCommittedContractMatchesTheApplication() throws Exception {
		var generated = generated();
		if (Boolean.getBoolean("openapi.update")) {
			Files.createDirectories(CONTRACT.getParent());
			Files.writeString(CONTRACT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");
		}

		assertThat(CONTRACT).as("run with -Dopenapi.update=true to create it").exists();
		assertThat(generated).isEqualTo(mapper.readTree(Files.readString(CONTRACT)));
	}

	@Test
	void everyEndpointInTheSpecIsDocumented() throws Exception {
		var paths = generated().get("paths");
		int operations = 0;
		for (var path : paths.propertyNames()) {
			operations += paths.get(path).size();
		}

		assertThat(operations).isEqualTo(16);
	}

	private JsonNode generated() throws Exception {
		var body = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
		return mapper.readTree(body);
	}

}
