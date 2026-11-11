package dev.sample.registry.consumer;

import jakarta.validation.constraints.NotNull;

public record ChangeConsumerStatusRequest(@NotNull ConsumerStatus status, @NotNull Long rowVersion) {
}
