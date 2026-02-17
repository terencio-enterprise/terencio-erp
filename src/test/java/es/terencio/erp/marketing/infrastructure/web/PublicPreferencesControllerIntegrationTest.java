package es.terencio.erp.marketing.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.marketing.application.dto.UnsubscribeRequest;

@AutoConfigureMockMvc
class PublicPreferencesControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID companyId;

    @BeforeEach
    void setup() {
        cleanDatabase();
        companyId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO companies (id, name, tax_id) VALUES (?, ?, ?)")
                .params(companyId, "Test Company", "B12345678")
                .update();

        // We need a customer with a token to test this.
        // Inserting a dummy customer with a known token.
        // Assuming customers table structure from migration V2 and V1
        jdbcClient.sql(
                "INSERT INTO customers (company_id, email, unsubscribe_token, marketing_status) VALUES (?, ?, ?, ?)")
                .params(companyId, "test@example.com", "valid-token-123", "SUBSCRIBED")
                .update();
    }

    @Test
    void testGetPreferences_Success() throws Exception {
        mockMvc.perform(get("/api/v1/public/marketing/preferences")
                .param("token", "valid-token-123"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPreferences_InvalidToken() throws Exception {
        // Depending on implementation, might return 404 or 400 or empty
        // Assuming 404 for invalid token or 400
        mockMvc.perform(get("/api/v1/public/marketing/preferences")
                .param("token", "invalid-token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testUpdatePreferences_Success() throws Exception {
        UnsubscribeRequest request = new UnsubscribeRequest();
        request.setToken("valid-token-123");
        request.setReason("Too many emails");
        // request.setPauseMonths(1);

        mockMvc.perform(put("/api/v1/public/marketing/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testUnsubscribeOneClick_Success() throws Exception {
        mockMvc.perform(post("/api/v1/public/marketing/unsubscribe-one-click")
                .param("token", "valid-token-123"))
                .andExpect(status().isOk());
    }
}
