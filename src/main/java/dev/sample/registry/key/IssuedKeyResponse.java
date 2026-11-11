package dev.sample.registry.key;

import java.time.Instant;
import java.util.UUID;

/** The only response that ever carries the plaintext key. */
public record IssuedKeyResponse(UUID id, UUID subscriptionId, String apiKey, String keyPrefix, Instant createdAt,
		Instant expiresAt) {

	@Override
	public String toString() {
		return "IssuedKeyResponse[id=" + id + ", keyPrefix=" + keyPrefix + "]";
	}

}
