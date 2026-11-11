package dev.sample.registry.api;

public enum ApiStatus {

	DRAFT, PUBLISHED, DEPRECATED, RETIRED;

	/** Lifecycle only moves one step forward; skipping a step or going back is not allowed. */
	public boolean canMoveTo(ApiStatus next) {
		return next.ordinal() == ordinal() + 1;
	}

}
