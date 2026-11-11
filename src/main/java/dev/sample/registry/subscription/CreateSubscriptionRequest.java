package dev.sample.registry.subscription;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionRequest(@NotNull UUID apiId, @NotNull UUID consumerId,
		@NotNull SubscriptionPlan plan) {
}
