package dev.sample.registry.consumer;

import java.time.Instant;
import java.util.UUID;

public record ConsumerResponse(UUID id, String name, String contactEmail, ConsumerStatus status, Instant createdAt,
		Instant updatedAt, Long rowVersion) {

	static ConsumerResponse from(Consumer consumer) {
		return new ConsumerResponse(consumer.getId(), consumer.getName(), consumer.getContactEmail(),
				consumer.getStatus(), consumer.getCreatedAt(), consumer.getUpdatedAt(), consumer.getRowVersion());
	}

}
