package dev.sample.registry.api;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "api")
class Api {

	@Id
	private UUID id;

	private String name;

	@Column(name = "version")
	private String version;

	private String basePath;

	private String ownerTeam;

	private String description;

	@Enumerated(EnumType.STRING)
	private ApiStatus status;

	private Instant createdAt;

	private Instant updatedAt;

	@Version
	private Long rowVersion;

	protected Api() {
	}

	Api(String name, String version, String basePath, String ownerTeam, String description, Instant now) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.version = version;
		this.basePath = basePath;
		this.ownerTeam = ownerTeam;
		this.description = description;
		this.status = ApiStatus.DRAFT;
		this.createdAt = now;
		this.updatedAt = now;
	}

	void update(String basePath, String ownerTeam, String description, Instant now) {
		this.basePath = basePath;
		this.ownerTeam = ownerTeam;
		this.description = description;
		this.updatedAt = now;
	}

	void moveTo(ApiStatus next, Instant now) {
		this.status = next;
		this.updatedAt = now;
	}

	UUID getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getVersion() {
		return version;
	}

	String getBasePath() {
		return basePath;
	}

	String getOwnerTeam() {
		return ownerTeam;
	}

	String getDescription() {
		return description;
	}

	ApiStatus getStatus() {
		return status;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	Long getRowVersion() {
		return rowVersion;
	}

}
