package dev.sample.registry.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubscriptionPlanTest {

	@Test
	void eachPlanSetsItsDefaultRateLimit() {
		assertThat(SubscriptionPlan.FREE.rateLimitPerMinute()).isEqualTo(60);
		assertThat(SubscriptionPlan.STANDARD.rateLimitPerMinute()).isEqualTo(600);
		assertThat(SubscriptionPlan.PREMIUM.rateLimitPerMinute()).isEqualTo(6000);
	}

	@Test
	void aCancelledSubscriptionCannotBeReactivatedOrSuspended() {
		assertThat(SubscriptionStatus.CANCELLED.canMoveTo(SubscriptionStatus.ACTIVE)).isFalse();
		assertThat(SubscriptionStatus.CANCELLED.canMoveTo(SubscriptionStatus.SUSPENDED)).isFalse();
	}

	@Test
	void anActiveOrSuspendedSubscriptionCanBeCancelled() {
		assertThat(SubscriptionStatus.ACTIVE.canMoveTo(SubscriptionStatus.CANCELLED)).isTrue();
		assertThat(SubscriptionStatus.SUSPENDED.canMoveTo(SubscriptionStatus.CANCELLED)).isTrue();
	}

	@Test
	void suspendAndReactivateAreOnlyValidFromTheOppositeState() {
		assertThat(SubscriptionStatus.ACTIVE.canMoveTo(SubscriptionStatus.SUSPENDED)).isTrue();
		assertThat(SubscriptionStatus.SUSPENDED.canMoveTo(SubscriptionStatus.ACTIVE)).isTrue();
		assertThat(SubscriptionStatus.ACTIVE.canMoveTo(SubscriptionStatus.ACTIVE)).isFalse();
	}

}
