package dev.sample.registry.api;

import java.util.UUID;

/**
 * In-process Spring event, published inside the retiring transaction. It lets the subscription feature react
 * without this package depending on it, which would otherwise create a cycle (subscriptions already need APIs).
 */
public record ApiRetired(UUID apiId) {
}
