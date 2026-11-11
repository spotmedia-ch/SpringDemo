package dev.sample.registry.key;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.sample.registry.api.ApiService;
import dev.sample.registry.api.ApiStatus;
import dev.sample.registry.common.ConflictException;
import dev.sample.registry.common.NotFoundException;
import dev.sample.registry.subscription.SubscriptionCancelled;
import dev.sample.registry.subscription.SubscriptionService;
import dev.sample.registry.subscription.SubscriptionStatus;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeyService {

	static final int MAX_ACTIVE_KEYS = 2;

	private final ApiKeyRepository repository;

	private final SubscriptionService subscriptions;

	private final ApiService apis;

	private final Clock clock;

	private final SecureRandom random = new SecureRandom();

	KeyService(ApiKeyRepository repository, SubscriptionService subscriptions, ApiService apis, Clock clock) {
		this.repository = repository;
		this.subscriptions = subscriptions;
		this.apis = apis;
		this.clock = clock;
	}

	@Transactional
	public IssuedKeyResponse issue(UUID subscriptionId, Instant expiresAt) {
		var subscription = subscriptions.get(subscriptionId);
		if (subscription.status() != SubscriptionStatus.ACTIVE) {
			throw new ConflictException("Keys can only be issued for an ACTIVE subscription; this one is "
					+ subscription.status());
		}
		var now = clock.instant();
		if (repository.countUsable(subscriptionId, KeyStatus.ACTIVE, now) >= MAX_ACTIVE_KEYS) {
			throw new ConflictException("A subscription holds at most " + MAX_ACTIVE_KEYS
					+ " active keys; revoke one first");
		}
		var generated = ApiKeys.generate(random);
		var key = repository.saveAndFlush(
				new ApiKey(subscriptionId, generated.prefix(), generated.hash(), expiresAt, now));
		return new IssuedKeyResponse(key.getId(), subscriptionId, generated.plaintext(), key.getKeyPrefix(),
				key.getCreatedAt(), key.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public List<KeyMetadataResponse> list(UUID subscriptionId) {
		subscriptions.get(subscriptionId);
		return repository.findBySubscriptionIdOrderByCreatedAt(subscriptionId).stream()
			.map(KeyMetadataResponse::from)
			.toList();
	}

	/** Revoking a key that is already revoked is a no-op, so retries are safe. */
	@Transactional
	public void revoke(UUID keyId) {
		var key = repository.findById(keyId).orElseThrow(() -> new NotFoundException("Key", keyId));
		if (key.getStatus() == KeyStatus.ACTIVE) {
			key.revoke(clock.instant());
			repository.saveAndFlush(key);
		}
	}

	@Transactional
	public ValidateKeyResponse validate(ValidateKeyRequest request) {
		var now = clock.instant();
		var key = ApiKeys.prefixOf(request.apiKey()).flatMap(prefix -> findMatching(prefix, request.apiKey()));
		if (key.isEmpty()) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.UNKNOWN_KEY);
		}
		var found = key.get();
		if (found.getStatus() == KeyStatus.REVOKED) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.REVOKED);
		}
		if (found.isExpiredAt(now)) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.EXPIRED);
		}
		var subscription = subscriptions.get(found.getSubscriptionId());
		if (subscription.status() != SubscriptionStatus.ACTIVE) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.SUBSCRIPTION_INACTIVE);
		}
		if (!subscription.apiId().equals(request.apiId())) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.WRONG_API);
		}
		var api = apis.get(subscription.apiId());
		if (api.status() != ApiStatus.PUBLISHED && api.status() != ApiStatus.DEPRECATED) {
			return ValidateKeyResponse.invalid(InvalidKeyReason.API_NOT_AVAILABLE);
		}
		found.markUsed(now);
		repository.save(found);
		return ValidateKeyResponse.valid(subscription.consumerId(), subscription.id(), subscription.plan(),
				subscription.rateLimitPerMinute());
	}

	/** Runs inside the transaction that cancels the subscription. */
	@EventListener
	@Transactional
	void onSubscriptionCancelled(SubscriptionCancelled event) {
		repository.revokeAllActive(event.subscriptionId(), KeyStatus.ACTIVE, KeyStatus.REVOKED, clock.instant());
	}

	private Optional<ApiKey> findMatching(String prefix, String plaintext) {
		var hash = ApiKeys.hash(plaintext);
		return repository.findByKeyPrefix(prefix).stream().filter(k -> ApiKeys.sameHash(k.getKeyHash(), hash)).findFirst();
	}

}
