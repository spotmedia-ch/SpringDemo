package dev.sample.registry.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.sample.registry.IntegrationTest;
import dev.sample.registry.TestData;
import dev.sample.registry.api.ApiService;
import dev.sample.registry.api.ApiStatus;
import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.consumer.ChangeConsumerStatusRequest;
import dev.sample.registry.consumer.ConsumerService;
import dev.sample.registry.consumer.ConsumerStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@IntegrationTest
class SubscriptionServiceIT {

	@Autowired
	private SubscriptionService subscriptions;

	@Autowired
	private ApiService apis;

	@Autowired
	private ConsumerService consumers;

	@Test
	void subscribingToAPublishedApiAppliesThePlanRateLimit() {
		var api = TestData.publishedApi(apis);
		var consumer = TestData.consumer(consumers);

		var created = subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.STANDARD));

		assertThat(created.status()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(created.rateLimitPerMinute()).isEqualTo(600);
	}

	@Test
	void subscribingToAnApiThatIsNotPublishedIsAConflict() {
		var draft = TestData.draftApi(apis);
		var consumer = TestData.consumer(consumers);

		assertThatThrownBy(() -> subscriptions.create(new CreateSubscriptionRequest(draft.id(), consumer.id(), SubscriptionPlan.FREE)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void aSuspendedConsumerCannotSubscribe() {
		var api = TestData.publishedApi(apis);
		var consumer = TestData.consumer(consumers);
		consumers.changeStatus(consumer.id(), new ChangeConsumerStatusRequest(ConsumerStatus.SUSPENDED, consumer.rowVersion()));

		assertThatThrownBy(() -> subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.FREE)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void subscribingToAnUnknownApiIsNotFound() {
		var consumer = TestData.consumer(consumers);

		assertThatThrownBy(() -> subscriptions.create(new CreateSubscriptionRequest(UUID.randomUUID(), consumer.id(), SubscriptionPlan.FREE)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void aConsumerCanSubscribeToTheSameApiOnlyOnce() {
		var api = TestData.publishedApi(apis);
		var consumer = TestData.consumer(consumers);
		subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.FREE));

		assertThatThrownBy(() -> subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.PREMIUM)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void retiringAnApiCancelsItsSubscriptions() {
		var api = TestData.publishedApi(apis);
		var consumer = TestData.consumer(consumers);
		var subscription = subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.FREE));

		var deprecated = TestData.moveApi(apis, api, ApiStatus.DEPRECATED);
		assertThat(subscriptions.get(subscription.id()).status()).isEqualTo(SubscriptionStatus.ACTIVE);
		TestData.moveApi(apis, deprecated, ApiStatus.RETIRED);

		assertThat(subscriptions.get(subscription.id()).status()).isEqualTo(SubscriptionStatus.CANCELLED);
	}

	@Test
	void listingFiltersByApiAndConsumer() {
		var api = TestData.publishedApi(apis);
		var first = TestData.consumer(consumers);
		var second = TestData.consumer(consumers);
		subscriptions.create(new CreateSubscriptionRequest(api.id(), first.id(), SubscriptionPlan.FREE));
		subscriptions.create(new CreateSubscriptionRequest(api.id(), second.id(), SubscriptionPlan.FREE));

		assertThat(subscriptions.list(api.id(), null, 0, 20).totalItems()).isEqualTo(2);
		assertThat(subscriptions.list(api.id(), first.id(), 0, 20).items()).singleElement()
			.satisfies(s -> assertThat(s.consumerId()).isEqualTo(first.id()));
	}

}
