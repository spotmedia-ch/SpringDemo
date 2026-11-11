package dev.sample.registry.subscription;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

	List<Subscription> findByApiIdAndStatusNot(UUID apiId, SubscriptionStatus status);

}
