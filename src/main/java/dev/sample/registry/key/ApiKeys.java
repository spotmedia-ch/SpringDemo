package dev.sample.registry.key;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/** Format: ark_&lt;8-char prefix&gt;_&lt;43 chars of base64url&gt;, the latter from 32 random bytes (256 bits). */
final class ApiKeys {

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private static final int PREFIX_LENGTH = 8;

	private static final int SECRET_BYTES = 32;

	private static final int SECRET_LENGTH = 43;

	private static final Pattern FORMAT = Pattern
		.compile("ark_([a-z0-9]{" + PREFIX_LENGTH + "})_[A-Za-z0-9_-]{" + SECRET_LENGTH + "}");

	private ApiKeys() {
	}

	static GeneratedKey generate(SecureRandom random) {
		var prefix = new StringBuilder(PREFIX_LENGTH);
		for (int i = 0; i < PREFIX_LENGTH; i++) {
			prefix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		var secret = new byte[SECRET_BYTES];
		random.nextBytes(secret);
		var plaintext = "ark_" + prefix + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
		return new GeneratedKey(plaintext, prefix.toString(), hash(plaintext));
	}

	static Optional<String> prefixOf(String plaintext) {
		var matcher = FORMAT.matcher(plaintext);
		return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	/**
	 * Plain SHA-256 is fine here: the input is 256 random bits, so there is nothing to guess and a slow hash
	 * would only cost CPU on every validation call. A password has far less entropy and needs a slow hash.
	 */
	static String hash(String plaintext) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(plaintext.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required by the Java platform", e);
		}
	}

	static boolean sameHash(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

	record GeneratedKey(String plaintext, String prefix, String hash) {

		@Override
		public String toString() {
			return "GeneratedKey[prefix=" + prefix + "]";
		}

	}

}
