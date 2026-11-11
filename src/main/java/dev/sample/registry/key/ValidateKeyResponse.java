package dev.sample.registry.key;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.sample.registry.subscription.SubscriptionPlan;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateKeyResponse(boolean valid, UUID consumerId, UUID subscriptionId, SubscriptionPlan plan,
		Integer rateLimitPerMinute, InvalidKeyReason reason) {

	static ValidateKeyResponse valid(UUID consumerId, UUID subscriptionId, SubscriptionPlan plan, int rateLimit) {
		return new ValidateKeyResponse(true, consumerId, subscriptionId, plan, rateLimit, null);
	}

	static ValidateKeyResponse invalid(InvalidKeyReason reason) {
		return new ValidateKeyResponse(false, null, null, null, null, reason);
	}

}
