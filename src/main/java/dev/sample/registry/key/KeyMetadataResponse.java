package dev.sample.registry.key;

import java.time.Instant;
import java.util.UUID;

public record KeyMetadataResponse(UUID id, UUID subscriptionId, String keyPrefix, KeyStatus status,
		Instant createdAt, Instant expiresAt, Instant revokedAt, Instant lastUsedAt) {

	static KeyMetadataResponse from(ApiKey key) {
		return new KeyMetadataResponse(key.getId(), key.getSubscriptionId(), key.getKeyPrefix(), key.getStatus(),
				key.getCreatedAt(), key.getExpiresAt(), key.getRevokedAt(), key.getLastUsedAt());
	}

}
