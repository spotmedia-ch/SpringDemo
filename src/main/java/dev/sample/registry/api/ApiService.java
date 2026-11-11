package dev.sample.registry.api;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.common.PageResponse;
import dev.sample.registry.common.RowVersions;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiService {

	private final ApiRepository repository;

	private final Clock clock;

	private final ApplicationEventPublisher events;

	ApiService(ApiRepository repository, Clock clock, ApplicationEventPublisher events) {
		this.repository = repository;
		this.clock = clock;
		this.events = events;
	}

	@Transactional
	public ApiResponse create(CreateApiRequest request) {
		var api = new Api(request.name(), request.version(), request.basePath(), request.ownerTeam(),
				request.description(), clock.instant());
		return ApiResponse.from(repository.saveAndFlush(api));
	}

	@Transactional(readOnly = true)
	public PageResponse<ApiResponse> list(ApiStatus status, String ownerTeam, int page, int size) {
		Specification<Api> filter = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (ownerTeam != null) {
				predicates.add(cb.equal(root.get("ownerTeam"), ownerTeam));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
		var pageable = PageResponse.request(page, size, Sort.by("name", "version"));
		return PageResponse.of(repository.findAll(filter, pageable), ApiResponse::from);
	}

	@Transactional(readOnly = true)
	public ApiResponse get(UUID id) {
		return ApiResponse.from(find(id));
	}

	@Transactional
	public ApiResponse update(UUID id, UpdateApiRequest request) {
		var api = find(id);
		RowVersions.check(api.getRowVersion(), request.rowVersion());
		api.update(request.basePath(), request.ownerTeam(), request.description(), clock.instant());
		return ApiResponse.from(repository.saveAndFlush(api));
	}

	@Transactional
	public ApiResponse changeStatus(UUID id, ChangeApiStatusRequest request) {
		var api = find(id);
		RowVersions.check(api.getRowVersion(), request.rowVersion());
		if (!api.getStatus().canMoveTo(request.status())) {
			throw new ConflictException("An API cannot move from " + api.getStatus() + " to " + request.status());
		}
		api.moveTo(request.status(), clock.instant());
		var saved = repository.saveAndFlush(api);
		if (saved.getStatus() == ApiStatus.RETIRED) {
			events.publishEvent(new ApiRetired(saved.getId()));
		}
		return ApiResponse.from(saved);
	}

	private Api find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("API", id));
	}

}
