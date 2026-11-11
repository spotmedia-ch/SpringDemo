package dev.sample.registry.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import dev.sample.registry.IntegrationTest;
import dev.sample.registry.TestData;
import dev.sample.registry.api.ApiResponse;
import dev.sample.registry.api.ApiService;
import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.consumer.ConsumerService;
import dev.sample.registry.subscription.ChangeSubscriptionStatusRequest;
import dev.sample.registry.subscription.CreateSubscriptionRequest;
import dev.sample.registry.subscription.SubscriptionPlan;
import dev.sample.registry.subscription.SubscriptionResponse;
import dev.sample.registry.subscription.SubscriptionService;
import dev.sample.registry.subscription.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class KeyServiceIT {

	@Autowired
	private KeyService keys;

	@Autowired
	private SubscriptionService subscriptions;

	@Autowired
	private ApiService apis;

	@Autowired
	private ConsumerService consumers;

	@Autowired
	private JdbcTemplate jdbc;

	private ApiResponse api;

	private SubscriptionResponse subscription;

	private void givenAnActiveSubscription() {
		api = TestData.publishedApi(apis);
		var consumer = TestData.consumer(consumers);
		subscription = subscriptions.create(new CreateSubscriptionRequest(api.id(), consumer.id(), SubscriptionPlan.STANDARD));
	}

	private ValidateKeyResponse validate(String plaintext) {
		return keys.validate(new ValidateKeyRequest(plaintext, api.id()));
	}

	@Test
	void anIssuedKeyValidatesAndReturnsThePlanAndRateLimit() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);

		var result = validate(issued.apiKey());

		assertThat(result.valid()).isTrue();
		assertThat(result.subscriptionId()).isEqualTo(subscription.id());
		assertThat(result.consumerId()).isEqualTo(subscription.consumerId());
		assertThat(result.plan()).isEqualTo(SubscriptionPlan.STANDARD);
		assertThat(result.rateLimitPerMinute()).isEqualTo(600);
	}

	@Test
	void onlyThePrefixAndHashAreStored() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);

		var stored = jdbc.queryForMap("select key_prefix, key_hash from api_key where id = ?", issued.id());

		assertThat(stored.get("key_prefix")).isEqualTo(issued.keyPrefix());
		assertThat(stored.get("key_hash")).isEqualTo(ApiKeys.hash(issued.apiKey()));
		assertThat(jdbc.queryForObject("select count(*) from api_key where key_hash = ?", Integer.class, issued.apiKey()))
			.isZero();
	}

	@Test
	void validatingUpdatesLastUsedAt() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);
		assertThat(keys.list(subscription.id())).singleElement().satisfies(k -> assertThat(k.lastUsedAt()).isNull());

		validate(issued.apiKey());

		assertThat(keys.list(subscription.id())).singleElement().satisfies(k -> assertThat(k.lastUsedAt()).isNotNull());
	}

	@Test
	void issuingAThirdActiveKeyIsRejected() {
		givenAnActiveSubscription();
		keys.issue(subscription.id(), null);
		keys.issue(subscription.id(), null);

		assertThatThrownBy(() -> keys.issue(subscription.id(), null)).isInstanceOf(ConflictException.class);
	}

	@Test
	void revokingAKeyFreesASlotForRotation() {
		givenAnActiveSubscription();
		var first = keys.issue(subscription.id(), null);
		keys.issue(subscription.id(), null);

		keys.revoke(first.id());

		assertThat(keys.issue(subscription.id(), null)).isNotNull();
	}

	@Test
	void aRevokedKeyFailsValidationWithReasonRevoked() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);
		assertThat(validate(issued.apiKey()).valid()).isTrue();

		keys.revoke(issued.id());

		assertThat(validate(issued.apiKey())).isEqualTo(ValidateKeyResponse.invalid(InvalidKeyReason.REVOKED));
	}

	@Test
	void revokingTwiceIsHarmless() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);

		keys.revoke(issued.id());
		keys.revoke(issued.id());

		assertThat(keys.list(subscription.id())).singleElement().satisfies(k -> assertThat(k.status()).isEqualTo(KeyStatus.REVOKED));
	}

	@Test
	void anUnknownOrMalformedKeyFailsWithReasonUnknownKey() {
		givenAnActiveSubscription();
		var wellFormedButUnissued = ApiKeys.generate(new java.security.SecureRandom()).plaintext();

		assertThat(validate(wellFormedButUnissued).reason()).isEqualTo(InvalidKeyReason.UNKNOWN_KEY);
		assertThat(validate("not-a-key").reason()).isEqualTo(InvalidKeyReason.UNKNOWN_KEY);
	}

	@Test
	void aKeyPastItsExpiryFailsWithReasonExpired() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), Instant.now().plus(1, ChronoUnit.DAYS));
		jdbc.update("update api_key set expires_at = now() - interval '1 minute' where id = ?", issued.id());

		assertThat(validate(issued.apiKey()).reason()).isEqualTo(InvalidKeyReason.EXPIRED);
	}

	@Test
	void aKeyUsedForADifferentApiFailsWithReasonWrongApi() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);
		var otherApi = TestData.publishedApi(apis);

		var result = keys.validate(new ValidateKeyRequest(issued.apiKey(), otherApi.id()));

		assertThat(result.reason()).isEqualTo(InvalidKeyReason.WRONG_API);
	}

	@Test
	void aSuspendedSubscriptionFailsWithReasonSubscriptionInactive() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);
		subscriptions.changeStatus(subscription.id(),
				new ChangeSubscriptionStatusRequest(SubscriptionStatus.SUSPENDED, subscription.rowVersion()));

		assertThat(validate(issued.apiKey()).reason()).isEqualTo(InvalidKeyReason.SUBSCRIPTION_INACTIVE);
	}

	@Test
	void anApiThatIsNoLongerPublishedFailsWithReasonApiNotAvailable() {
		givenAnActiveSubscription();
		var issued = keys.issue(subscription.id(), null);
		jdbc.update("update api set status = 'DRAFT' where id = ?", api.id());

		assertThat(validate(issued.apiKey()).reason()).isEqualTo(InvalidKeyReason.API_NOT_AVAILABLE);
	}

	@Test
	void cancellingASubscriptionRevokesAllItsKeys() {
		givenAnActiveSubscription();
		var first = keys.issue(subscription.id(), null);
		var second = keys.issue(subscription.id(), null);

		subscriptions.changeStatus(subscription.id(),
				new ChangeSubscriptionStatusRequest(SubscriptionStatus.CANCELLED, subscription.rowVersion()));

		assertThat(keys.list(subscription.id())).hasSize(2).allSatisfy(k -> assertThat(k.status()).isEqualTo(KeyStatus.REVOKED));
		assertThat(validate(first.apiKey()).reason()).isEqualTo(InvalidKeyReason.REVOKED);
		assertThat(validate(second.apiKey()).reason()).isEqualTo(InvalidKeyReason.REVOKED);
	}

	@Test
	void keysCannotBeIssuedForAnInactiveSubscription() {
		givenAnActiveSubscription();
		subscriptions.changeStatus(subscription.id(),
				new ChangeSubscriptionStatusRequest(SubscriptionStatus.SUSPENDED, subscription.rowVersion()));

		assertThatThrownBy(() -> keys.issue(subscription.id(), null)).isInstanceOf(ConflictException.class);
	}

	@Test
	void issuingForAnUnknownSubscriptionIsNotFound() {
		assertThatThrownBy(() -> keys.issue(UUID.randomUUID(), null)).isInstanceOf(NotFoundException.class);
	}

}
