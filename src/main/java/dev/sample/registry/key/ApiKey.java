package dev.sample.registry.key;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** No toString on purpose: the hash must never reach a log line. */
@Entity
@Table(name = "api_key")
class ApiKey {

	@Id
	private UUID id;

	private UUID subscriptionId;

	private String keyPrefix;

	private String keyHash;

	@Enumerated(EnumType.STRING)
	private KeyStatus status;

	private Instant createdAt;

	private Instant expiresAt;

	private Instant revokedAt;

	private Instant lastUsedAt;

	protected ApiKey() {
	}

	ApiKey(UUID subscriptionId, String keyPrefix, String keyHash, Instant expiresAt, Instant now) {
		this.id = UUID.randomUUID();
		this.subscriptionId = subscriptionId;
		this.keyPrefix = keyPrefix;
		this.keyHash = keyHash;
		this.status = KeyStatus.ACTIVE;
		this.createdAt = now;
		this.expiresAt = expiresAt;
	}

	void revoke(Instant now) {
		this.status = KeyStatus.REVOKED;
		this.revokedAt = now;
	}

	void markUsed(Instant now) {
		this.lastUsedAt = now;
	}

	boolean isExpiredAt(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}

	UUID getId() {
		return id;
	}

	UUID getSubscriptionId() {
		return subscriptionId;
	}

	String getKeyPrefix() {
		return keyPrefix;
	}

	String getKeyHash() {
		return keyHash;
	}

	KeyStatus getStatus() {
		return status;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}

	Instant getLastUsedAt() {
		return lastUsedAt;
	}

}
