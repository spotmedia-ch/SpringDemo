package dev.sample.registry.key;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApiKeysTest {

	private final SecureRandom random = new SecureRandom();

	@Test
	void aGeneratedKeyHasThePrefixAndLengthTheFormatPromises() {
		var key = ApiKeys.generate(random);

		assertThat(key.plaintext()).matches("ark_[a-z0-9]{8}_[A-Za-z0-9_-]{43}");
		assertThat(key.plaintext()).startsWith("ark_" + key.prefix() + "_");
	}

	@Test
	void theHashIsSha256InHexAndNeverEqualsThePlaintext() {
		var key = ApiKeys.generate(random);

		assertThat(key.hash()).matches("[0-9a-f]{64}").isNotEqualTo(key.plaintext());
		assertThat(ApiKeys.hash(key.plaintext())).isEqualTo(key.hash());
	}

	@Test
	void hashingKnownInputGivesTheKnownSha256() {
		assertThat(ApiKeys.hash("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void generatedKeysDoNotRepeat() {
		var seen = new HashSet<String>();
		for (int i = 0; i < 1000; i++) {
			assertThat(seen.add(ApiKeys.generate(random).plaintext())).isTrue();
		}
	}

	@Test
	void theLookupPrefixCanBeReadBackFromAKey() {
		var key = ApiKeys.generate(random);

		assertThat(ApiKeys.prefixOf(key.plaintext())).contains(key.prefix());
	}

	@Test
	void malformedKeysHaveNoPrefix() {
		assertThat(ApiKeys.prefixOf("")).isEmpty();
		assertThat(ApiKeys.prefixOf("ark_short")).isEmpty();
		assertThat(ApiKeys.prefixOf("xxx_abcdefgh_" + "a".repeat(43))).isEmpty();
		assertThat(ApiKeys.prefixOf("ark_ABCDEFGH_" + "a".repeat(43))).isEmpty();
	}

	@Test
	void keyMaterialNeverAppearsInToStringOutput() {
		var key = ApiKeys.generate(random);
		var issued = new IssuedKeyResponse(UUID.randomUUID(), UUID.randomUUID(), key.plaintext(), key.prefix(), null, null);
		var validate = new ValidateKeyRequest(key.plaintext(), UUID.randomUUID());

		assertThat(key.toString()).doesNotContain(key.plaintext()).doesNotContain(key.hash());
		assertThat(issued.toString()).doesNotContain(key.plaintext());
		assertThat(validate.toString()).doesNotContain(key.plaintext());
	}

}
