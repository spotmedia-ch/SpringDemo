package dev.sample.registry.subscription;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** References the API and consumer by id only: no entity of another feature crosses a package boundary. */
@Entity
@Table(name = "subscription")
class Subscription {

	@Id
	private UUID id;

	private UUID apiId;

	private UUID consumerId;

	@Enumerated(EnumType.STRING)
	private SubscriptionPlan plan;

	private int rateLimitPerMinute;

	@Enumerated(EnumType.STRING)
	private SubscriptionStatus status;

	private Instant createdAt;

	private Instant updatedAt;

	@Version
	private Long rowVersion;

	protected Subscription() {
	}

	Subscription(UUID apiId, UUID consumerId, SubscriptionPlan plan, Instant now) {
		this.id = UUID.randomUUID();
		this.apiId = apiId;
		this.consumerId = consumerId;
		this.plan = plan;
		this.rateLimitPerMinute = plan.rateLimitPerMinute();
		this.status = SubscriptionStatus.ACTIVE;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void moveTo(SubscriptionStatus next, Instant now) {
		this.status = next;
		this.updatedAt = now;
	}

	UUID getId() {
		return id;
	}

	UUID getApiId() {
		return apiId;
	}

	UUID getConsumerId() {
		return consumerId;
	}

	SubscriptionPlan getPlan() {
		return plan;
	}

	int getRateLimitPerMinute() {
		return rateLimitPerMinute;
	}

	SubscriptionStatus getStatus() {
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
