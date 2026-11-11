package dev.sample.registry.subscription;

public enum SubscriptionPlan {

	FREE(60), STANDARD(600), PREMIUM(6000);

	private final int rateLimitPerMinute;

	SubscriptionPlan(int rateLimitPerMinute) {
		this.rateLimitPerMinute = rateLimitPerMinute;
	}

	public int rateLimitPerMinute() {
		return rateLimitPerMinute;
	}

}
