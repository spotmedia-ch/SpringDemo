package dev.sample.registry.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import dev.sample.registry.IntegrationTest;
import dev.sample.registry.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@IntegrationTest
class ApiPersistenceIT {

	@Autowired
	private ApiRepository repository;

	@Autowired
	private ApiService service;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void flywayCreatesAllRegistryTables() {
		var tables = jdbc.queryForList("""
				select table_name from information_schema.tables
				where table_schema = 'public' and table_name in ('api', 'consumer', 'subscription', 'api_key')""",
				String.class);

		assertThat(tables).containsExactlyInAnyOrder("api", "consumer", "subscription", "api_key");
	}

	@Test
	void twoApisWithTheSameNameAndVersionAreRejectedByTheDatabase() {
		var name = "dup-" + UUID.randomUUID();
		repository.saveAndFlush(newApi(name, "v1"));

		assertThatThrownBy(() -> repository.saveAndFlush(newApi(name, "v1")))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(repository.saveAndFlush(newApi(name, "v2"))).isNotNull();
	}

	@Test
	void savingAStaleCopyFailsWithAnOptimisticLockingConflict() {
		var saved = repository.saveAndFlush(newApi("lock-" + UUID.randomUUID(), "v1"));
		var copyA = repository.findById(saved.getId()).orElseThrow();
		var copyB = repository.findById(saved.getId()).orElseThrow();
		copyB.update("/changed", "team-b", null, Instant.now());
		repository.saveAndFlush(copyB);

		copyA.update("/other", "team-c", null, Instant.now());

		assertThatThrownBy(() -> repository.saveAndFlush(copyA)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void updatingWithAnOutOfDateRowVersionIsAConflict() {
		var created = service.create(new CreateApiRequest("ver-" + UUID.randomUUID(), "v1", "/x", "team-a", null));
		service.update(created.id(), new UpdateApiRequest("/y", "team-a", null, created.rowVersion()));

		assertThatThrownBy(
				() -> service.update(created.id(), new UpdateApiRequest("/z", "team-a", null, created.rowVersion())))
			.isInstanceOf(ConflictException.class);
	}

	private static Api newApi(String name, String version) {
		return new Api(name, version, "/p", "team-a", null, Instant.now());
	}

}
