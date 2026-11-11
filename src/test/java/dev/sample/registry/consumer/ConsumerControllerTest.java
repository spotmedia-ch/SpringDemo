package dev.sample.registry.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

@WebMvcTest(ConsumerController.class)
class ConsumerControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsumerService service;

	@Test
	void registeringAConsumerReturns201WithALocationHeader() throws Exception {
		var id = UUID.randomUUID();
		when(service.create(any())).thenReturn(
				new ConsumerResponse(id, "acme", "a@example.com", ConsumerStatus.ACTIVE, Instant.now(), Instant.now(), 0L));

		mvc.perform(post("/api/v1/consumers").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"acme","contactEmail":"a@example.com"}"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/v1/consumers/" + id));
	}

	@Test
	void anInvalidEmailAddressIsReportedAgainstItsField() throws Exception {
		mvc.perform(post("/api/v1/consumers").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name":"acme","contactEmail":"not-an-email"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field=='contactEmail')]").exists());
	}

}
