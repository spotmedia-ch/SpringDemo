package dev.sample.registry.subscription;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.sample.registry.api.ApiRetired;
import dev.sample.registry.api.ApiService;
import dev.sample.registry.api.ApiStatus;
import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.common.PageResponse;
import dev.sample.registry.common.RowVersions;
import dev.sample.registry.consumer.ConsumerService;
import dev.sample.registry.consumer.ConsumerStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

	private final SubscriptionRepository repository;

	private final ApiService apis;

	private final ConsumerService consumers;

	private final Clock clock;

	private final ApplicationEventPublisher events;

	SubscriptionService(SubscriptionRepository repository, ApiService apis, ConsumerService consumers, Clock clock,
			ApplicationEventPublisher events) {
		this.repository = repository;
		this.apis = apis;
		this.consumers = consumers;
		this.clock = clock;
		this.events = events;
	}

	@Transactional
	public SubscriptionResponse create(CreateSubscriptionRequest request) {
		var api = apis.get(request.apiId());
		var consumer = consumers.get(request.consumerId());
		if (api.status() != ApiStatus.PUBLISHED) {
			throw new ConflictException("Only a PUBLISHED API can be subscribed to; this one is " + api.status());
		}
		if (consumer.status() != ConsumerStatus.ACTIVE) {
			throw new ConflictException("Only an ACTIVE consumer can subscribe; this one is " + consumer.status());
		}
		var subscription = new Subscription(api.id(), consumer.id(), request.plan(), clock.instant());
		return SubscriptionResponse.from(repository.saveAndFlush(subscription));
	}

	@Transactional(readOnly = true)
	public PageResponse<SubscriptionResponse> list(UUID apiId, UUID consumerId, int page, int size) {
		Specification<Subscription> filter = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (apiId != null) {
				predicates.add(cb.equal(root.get("apiId"), apiId));
			}
			if (consumerId != null) {
				predicates.add(cb.equal(root.get("consumerId"), consumerId));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
		var pageable = PageResponse.request(page, size, Sort.by("createdAt", "id"));
		return PageResponse.of(repository.findAll(filter, pageable), SubscriptionResponse::from);
	}

	@Transactional(readOnly = true)
	public SubscriptionResponse get(UUID id) {
		return SubscriptionResponse.from(find(id));
	}

	@Transactional
	public SubscriptionResponse changeStatus(UUID id, ChangeSubscriptionStatusRequest request) {
		var subscription = find(id);
		RowVersions.check(subscription.getRowVersion(), request.rowVersion());
		if (!subscription.getStatus().canMoveTo(request.status())) {
			throw new ConflictException(
					"A subscription cannot move from " + subscription.getStatus() + " to " + request.status());
		}
		return SubscriptionResponse.from(moveTo(subscription, request.status()));
	}

	/** Runs inside the transaction that retires the API, so either everything is cancelled or nothing is. */
	@EventListener
	@Transactional
	void onApiRetired(ApiRetired event) {
		for (var subscription : repository.findByApiIdAndStatusNot(event.apiId(), SubscriptionStatus.CANCELLED)) {
			moveTo(subscription, SubscriptionStatus.CANCELLED);
		}
	}

	private Subscription moveTo(Subscription subscription, SubscriptionStatus next) {
		subscription.moveTo(next, clock.instant());
		var saved = repository.saveAndFlush(subscription);
		if (next == SubscriptionStatus.CANCELLED) {
			events.publishEvent(new SubscriptionCancelled(saved.getId()));
		}
		return saved;
	}

	private Subscription find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Subscription", id));
	}

}
