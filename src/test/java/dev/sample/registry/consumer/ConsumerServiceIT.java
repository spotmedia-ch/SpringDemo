package dev.sample.registry.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import dev.sample.registry.IntegrationTest;
import dev.sample.registry.common.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class ConsumerServiceIT {

	@Autowired
	private ConsumerService service;

	@Test
	void aNewConsumerIsActiveAndCanBeSuspendedAndReactivated() {
		var created = service.create(new CreateConsumerRequest("c-" + UUID.randomUUID(), "a@example.com"));
		assertThat(created.status()).isEqualTo(ConsumerStatus.ACTIVE);

		var suspended = service.changeStatus(created.id(),
				new ChangeConsumerStatusRequest(ConsumerStatus.SUSPENDED, created.rowVersion()));
		var reactivated = service.changeStatus(created.id(),
				new ChangeConsumerStatusRequest(ConsumerStatus.ACTIVE, suspended.rowVersion()));

		assertThat(suspended.status()).isEqualTo(ConsumerStatus.SUSPENDED);
		assertThat(reactivated.status()).isEqualTo(ConsumerStatus.ACTIVE);
		assertThat(reactivated.rowVersion()).isGreaterThan(created.rowVersion());
	}

	@Test
	void changingToTheCurrentStatusIsAConflict() {
		var created = service.create(new CreateConsumerRequest("c-" + UUID.randomUUID(), "a@example.com"));

		assertThatThrownBy(() -> service.changeStatus(created.id(),
				new ChangeConsumerStatusRequest(ConsumerStatus.ACTIVE, created.rowVersion())))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void consumerNamesMustBeUnique() {
		var name = "c-" + UUID.randomUUID();
		service.create(new CreateConsumerRequest(name, "a@example.com"));

		assertThatThrownBy(() -> service.create(new CreateConsumerRequest(name, "b@example.com")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
