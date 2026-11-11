package dev.sample.registry.key;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class KeyController {

	private final KeyService service;

	KeyController(KeyService service) {
		this.service = service;
	}

	@PostMapping("/subscriptions/{subscriptionId}/keys")
	@ResponseStatus(HttpStatus.CREATED)
	IssuedKeyResponse issue(@PathVariable UUID subscriptionId,
			@Valid @RequestBody(required = false) IssueKeyRequest request) {
		return service.issue(subscriptionId, request == null ? null : request.expiresAt());
	}

	@GetMapping("/subscriptions/{subscriptionId}/keys")
	List<KeyMetadataResponse> list(@PathVariable UUID subscriptionId) {
		return service.list(subscriptionId);
	}

	@PostMapping("/keys/{id}/revoke")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revoke(@PathVariable UUID id) {
		service.revoke(id);
	}

	@PostMapping("/keys/validate")
	ValidateKeyResponse validate(@Valid @RequestBody ValidateKeyRequest request) {
		return service.validate(request);
	}

}
