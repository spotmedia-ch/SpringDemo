package dev.sample.registry.subscription;

import jakarta.validation.constraints.NotNull;

public record ChangeSubscriptionStatusRequest(@NotNull SubscriptionStatus status, @NotNull Long rowVersion) {
}
