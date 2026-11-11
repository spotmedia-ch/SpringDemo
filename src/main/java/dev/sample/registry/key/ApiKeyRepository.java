package dev.sample.registry.key;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

	List<ApiKey> findByKeyPrefix(String keyPrefix);

	List<ApiKey> findBySubscriptionIdOrderByCreatedAt(UUID subscriptionId);

	@Query("""
			select count(k) from ApiKey k
			where k.subscriptionId = :subscriptionId and k.status = :active
			and (k.expiresAt is null or k.expiresAt > :now)""")
	long countUsable(@Param("subscriptionId") UUID subscriptionId, @Param("active") KeyStatus active,
			@Param("now") Instant now);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update ApiKey k set k.status = :revoked, k.revokedAt = :now
			where k.subscriptionId = :subscriptionId and k.status = :active""")
	int revokeAllActive(@Param("subscriptionId") UUID subscriptionId, @Param("active") KeyStatus active,
			@Param("revoked") KeyStatus revoked, @Param("now") Instant now);

}
