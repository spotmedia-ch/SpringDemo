package dev.sample.registry.api;

import java.time.Instant;
import java.util.UUID;

public record ApiResponse(UUID id, String name, String version, String basePath, String ownerTeam,
		String description, ApiStatus status, Instant createdAt, Instant updatedAt, Long rowVersion) {

	static ApiResponse from(Api api) {
		return new ApiResponse(api.getId(), api.getName(), api.getVersion(), api.getBasePath(), api.getOwnerTeam(),
				api.getDescription(), api.getStatus(), api.getCreatedAt(), api.getUpdatedAt(), api.getRowVersion());
	}

}
