package dev.sample.registry.key;

import java.time.Instant;

import jakarta.validation.constraints.Future;

public record IssueKeyRequest(@Future Instant expiresAt) {
}
