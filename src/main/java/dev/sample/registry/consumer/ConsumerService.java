package dev.sample.registry.consumer;

import java.time.Clock;
import java.util.UUID;

import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.common.PageResponse;
import dev.sample.registry.common.RowVersions;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumerService {

	private final ConsumerRepository repository;

	private final Clock clock;

	ConsumerService(ConsumerRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public ConsumerResponse create(CreateConsumerRequest request) {
		var consumer = new Consumer(request.name(), request.contactEmail(), clock.instant());
		return ConsumerResponse.from(repository.saveAndFlush(consumer));
	}

	@Transactional(readOnly = true)
	public PageResponse<ConsumerResponse> list(int page, int size) {
		var pageable = PageResponse.request(page, size, Sort.by("name"));
		return PageResponse.of(repository.findAll(pageable), ConsumerResponse::from);
	}

	@Transactional(readOnly = true)
	public ConsumerResponse get(UUID id) {
		return ConsumerResponse.from(find(id));
	}

	@Transactional
	public ConsumerResponse changeStatus(UUID id, ChangeConsumerStatusRequest request) {
		var consumer = find(id);
		RowVersions.check(consumer.getRowVersion(), request.rowVersion());
		if (!consumer.getStatus().canMoveTo(request.status())) {
			throw new ConflictException("The consumer is already " + consumer.getStatus());
		}
		consumer.moveTo(request.status(), clock.instant());
		return ConsumerResponse.from(repository.saveAndFlush(consumer));
	}

	private Consumer find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Consumer", id));
	}

}
