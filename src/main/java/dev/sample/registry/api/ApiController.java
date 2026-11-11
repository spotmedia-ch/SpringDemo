package dev.sample.registry.api;

import java.net.URI;
import java.util.UUID;

import dev.sample.registry.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/apis")
public class ApiController {

	private final ApiService service;

	ApiController(ApiService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<ApiResponse> create(@Valid @RequestBody CreateApiRequest request) {
		var created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping
	PageResponse<ApiResponse> list(@RequestParam(required = false) ApiStatus status,
			@RequestParam(required = false) String ownerTeam, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(status, ownerTeam, page, size);
	}

	@GetMapping("/{id}")
	ApiResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PutMapping("/{id}")
	ApiResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateApiRequest request) {
		return service.update(id, request);
	}

	@PostMapping("/{id}/status")
	ApiResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeApiStatusRequest request) {
		return service.changeStatus(id, request);
	}

}
