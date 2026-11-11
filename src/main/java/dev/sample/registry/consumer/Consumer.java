package dev.sample.registry.consumer;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "consumer")
class Consumer {

	@Id
	private UUID id;

	private String name;

	private String contactEmail;

	@Enumerated(EnumType.STRING)
	private ConsumerStatus status;

	private Instant createdAt;

	private Instant updatedAt;

	@Version
	private Long rowVersion;

	protected Consumer() {
	}

	Consumer(String name, String contactEmail, Instant now) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.contactEmail = contactEmail;
		this.status = ConsumerStatus.ACTIVE;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void moveTo(ConsumerStatus next, Instant now) {
		this.status = next;
		this.updatedAt = now;
	}

	UUID getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getContactEmail() {
		return contactEmail;
	}

	ConsumerStatus getStatus() {
		return status;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	Long getRowVersion() {
		return rowVersion;
	}

}
