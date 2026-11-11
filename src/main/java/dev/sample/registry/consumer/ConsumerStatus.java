package dev.sample.registry.consumer;

public enum ConsumerStatus {

	ACTIVE, SUSPENDED;

	public boolean canMoveTo(ConsumerStatus next) {
		return next != this;
	}

}
