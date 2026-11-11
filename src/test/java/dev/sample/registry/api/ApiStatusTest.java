package dev.sample.registry.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiStatusTest {

	@Test
	void anApiMovesForwardOneStepAtATime() {
		assertThat(ApiStatus.DRAFT.canMoveTo(ApiStatus.PUBLISHED)).isTrue();
		assertThat(ApiStatus.PUBLISHED.canMoveTo(ApiStatus.DEPRECATED)).isTrue();
		assertThat(ApiStatus.DEPRECATED.canMoveTo(ApiStatus.RETIRED)).isTrue();
	}

	@Test
	void anApiCannotSkipAStep() {
		assertThat(ApiStatus.DRAFT.canMoveTo(ApiStatus.DEPRECATED)).isFalse();
		assertThat(ApiStatus.DRAFT.canMoveTo(ApiStatus.RETIRED)).isFalse();
		assertThat(ApiStatus.PUBLISHED.canMoveTo(ApiStatus.RETIRED)).isFalse();
	}

	@Test
	void anApiCannotGoBackwardsOrStayWhereItIs() {
		assertThat(ApiStatus.PUBLISHED.canMoveTo(ApiStatus.DRAFT)).isFalse();
		assertThat(ApiStatus.RETIRED.canMoveTo(ApiStatus.DEPRECATED)).isFalse();
		assertThat(ApiStatus.PUBLISHED.canMoveTo(ApiStatus.PUBLISHED)).isFalse();
	}

	@Test
	void aRetiredApiCannotMoveAnywhere() {
		for (ApiStatus next : ApiStatus.values()) {
			assertThat(ApiStatus.RETIRED.canMoveTo(next)).isFalse();
		}
	}

}
