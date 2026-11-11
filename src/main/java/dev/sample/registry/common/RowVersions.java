package dev.sample.registry.common;

public final class RowVersions {

	private RowVersions() {
	}

	/**
	 * The client sends the rowVersion it read. Hibernate's @Version only protects against races inside the
	 * server, so a version that was already stale when the request arrived has to be compared explicitly.
	 */
	public static void check(Long current, Long supplied) {
		if (!current.equals(supplied)) {
			throw new ConflictException("rowVersion " + supplied + " is stale; the current version is " + current);
		}
	}

}
