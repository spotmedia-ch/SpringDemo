package dev.sample.registry.subscription;

import java.net.URI;
import java.util.UUID;

import dev.sample.registry.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

	private final SubscriptionService service;

	SubscriptionController(SubscriptionService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody CreateSubscriptionRequest request) {
		var created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	PageResponse<SubscriptionResponse> list(@RequestParam(required = false) UUID apiId,
			@RequestParam(required = false) UUID consumerId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(apiId, consumerId, page, size);
	}

	@PostMapping("/{id}/status")
	SubscriptionResponse changeStatus(@PathVariable UUID id,
			@Valid @RequestBody ChangeSubscriptionStatusRequest request) {
		return service.changeStatus(id, request);
	}

}
