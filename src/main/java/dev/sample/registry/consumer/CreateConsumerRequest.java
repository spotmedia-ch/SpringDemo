package dev.sample.registry.consumer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConsumerRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 200) String contactEmail) {
}
