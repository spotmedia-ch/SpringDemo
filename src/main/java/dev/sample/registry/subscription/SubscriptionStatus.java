package dev.sample.registry.subscription;

public enum SubscriptionStatus {

	ACTIVE, SUSPENDED, CANCELLED;

	public boolean canMoveTo(SubscriptionStatus next) {
		return switch (this) {
			case ACTIVE -> next == SUSPENDED || next == CANCELLED;
			case SUSPENDED -> next == ACTIVE || next == CANCELLED;
			case CANCELLED -> false;
		};
	}

}
