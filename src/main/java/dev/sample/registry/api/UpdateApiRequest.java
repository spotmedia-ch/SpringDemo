package dev.sample.registry.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateApiRequest(
		@NotBlank @Size(max = 200) @Pattern(regexp = "/.*", message = "must start with /") String basePath,
		@NotBlank @Size(max = 100) String ownerTeam,
		@Size(max = 1000) String description,
		@NotNull Long rowVersion) {
}
