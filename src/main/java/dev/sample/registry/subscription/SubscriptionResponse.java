package dev.sample.registry.subscription;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(UUID id, UUID apiId, UUID consumerId, SubscriptionPlan plan,
		int rateLimitPerMinute, SubscriptionStatus status, Instant createdAt, Instant updatedAt, Long rowVersion) {

	static SubscriptionResponse from(Subscription s) {
		return new SubscriptionResponse(s.getId(), s.getApiId(), s.getConsumerId(), s.getPlan(),
				s.getRateLimitPerMinute(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt(), s.getRowVersion());
	}

}
