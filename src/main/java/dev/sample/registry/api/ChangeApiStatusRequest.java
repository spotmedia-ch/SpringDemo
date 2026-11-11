package dev.sample.registry.api;

import jakarta.validation.constraints.NotNull;

public record ChangeApiStatusRequest(@NotNull ApiStatus status, @NotNull Long rowVersion) {
}
