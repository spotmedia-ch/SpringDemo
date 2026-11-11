package dev.sample.registry.key;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KeyController.class)
class KeyControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private KeyService service;

	@Test
	void anInvalidKeyIsStillAnHttp200WithAReason() throws Exception {
		when(service.validate(any())).thenReturn(ValidateKeyResponse.invalid(InvalidKeyReason.REVOKED));

		mvc.perform(post("/api/v1/keys/validate").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"apiKey":"x","apiId":"%s"}""".formatted(UUID.randomUUID())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.reason").value("REVOKED"))
			.andExpect(jsonPath("$.consumerId").doesNotExist());
	}

	@Test
	void validatingWithoutAnApiIdIsABadRequest() throws Exception {
		mvc.perform(post("/api/v1/keys/validate").contentType(MediaType.APPLICATION_JSON).content("""
				{"apiKey":"x"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field=='apiId')]").exists());
	}

	@Test
	void revokingAKeyReturns204() throws Exception {
		var id = UUID.randomUUID();

		mvc.perform(post("/api/v1/keys/" + id + "/revoke")).andExpect(status().isNoContent());

		verify(service).revoke(id);
	}

	@Test
	void issuingAKeyWithoutABodyIsAccepted() throws Exception {
		var subscriptionId = UUID.randomUUID();
		when(service.issue(subscriptionId, null)).thenReturn(
				new IssuedKeyResponse(UUID.randomUUID(), subscriptionId, "ark_x", "x", Instant.now(), null));

		mvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/keys"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.apiKey").value("ark_x"));
	}

	@Test
	void anExpiryInThePastIsRejected() throws Exception {
		mvc.perform(post("/api/v1/subscriptions/" + UUID.randomUUID() + "/keys").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"expiresAt":"2020-01-01T00:00:00Z"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field=='expiresAt')]").exists());
	}

}
