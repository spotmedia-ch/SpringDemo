package dev.sample.registry.subscription;

import java.util.UUID;

/** In-process event: the key feature revokes the subscription's keys in the same transaction. */
public record SubscriptionCancelled(UUID subscriptionId) {
}
