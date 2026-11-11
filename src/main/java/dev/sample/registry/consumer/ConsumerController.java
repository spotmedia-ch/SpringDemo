package dev.sample.registry.consumer;

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
@RequestMapping("/api/v1/consumers")
public class ConsumerController {

	private final ConsumerService service;

	ConsumerController(ConsumerService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ConsumerResponse> create(@Valid @RequestBody CreateConsumerRequest request) {
		var created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	PageResponse<ConsumerResponse> list(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(page, size);
	}

	@GetMapping("/{id}")
	ConsumerResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PostMapping("/{id}/status")
	ConsumerResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeConsumerStatusRequest request) {
		return service.changeStatus(id, request);
	}

}
