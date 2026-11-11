package dev.sample.registry.key;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidateKeyRequest(@NotBlank String apiKey, @NotNull UUID apiId) {

	@Override
	public String toString() {
		return "ValidateKeyRequest[apiId=" + apiId + "]";
	}

}
