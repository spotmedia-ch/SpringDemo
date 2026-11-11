package dev.sample.registry;

import java.util.UUID;

import dev.sample.registry.api.ApiResponse;
import dev.sample.registry.api.ApiService;
import dev.sample.registry.api.ApiStatus;
import dev.sample.registry.api.ChangeApiStatusRequest;
import dev.sample.registry.api.CreateApiRequest;
import dev.sample.registry.consumer.ConsumerResponse;
import dev.sample.registry.consumer.ConsumerService;
import dev.sample.registry.consumer.CreateConsumerRequest;

/** Builds rows with unique names, so tests that share one database never collide. */
public final class TestData {

	private TestData() {
	}

	public static ApiResponse draftApi(ApiService apis) {
		return apis.create(new CreateApiRequest("api-" + UUID.randomUUID(), "v1", "/p", "team-a", null));
	}

	public static ApiResponse moveApi(ApiService apis, ApiResponse api, ApiStatus next) {
		return apis.changeStatus(api.id(), new ChangeApiStatusRequest(next, api.rowVersion()));
	}

	public static ApiResponse publishedApi(ApiService apis) {
		return moveApi(apis, draftApi(apis), ApiStatus.PUBLISHED);
	}

	public static ConsumerResponse consumer(ConsumerService consumers) {
		return consumers.create(new CreateConsumerRequest("consumer-" + UUID.randomUUID(), "c@example.com"));
	}

}
