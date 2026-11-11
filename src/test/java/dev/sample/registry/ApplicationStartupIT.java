package dev.sample.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ApplicationStartupIT {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void healthEndpointReportsUpWhenTheDatabaseIsReachable() throws Exception {
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).build();

		var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
	}

	@Test
	void flywayAppliesTheBaselineMigrationOnStartup() {
		var applied = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class);

		assertThat(applied).isEqualTo(1);
	}

}
