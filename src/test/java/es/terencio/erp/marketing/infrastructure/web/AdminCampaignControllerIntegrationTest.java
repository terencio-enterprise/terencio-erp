package es.terencio.erp.marketing.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.marketing.application.dto.CampaignRequest;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

@AutoConfigureMockMvc
class AdminCampaignControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignRepositoryPort templateRepository;

    private UUID companyId;

    @BeforeEach
    void setup() {
        cleanDatabase();
        companyId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO companies (id, name, tax_id) VALUES (?, ?, ?)")
                .params(companyId, "Test Company", "B12345678")
                .update();
    }

    private CustomUserDetails createAdminUser() {
        return new CustomUserDetails(1L, UUID.randomUUID(), "admin", "Admin", "pass", "ADMIN", null, companyId);
    }

    @Test
    void testGetCampaignHistory_Success() throws Exception {
        mockMvc.perform(get("/api/v1/marketing/campaigns")
                .param("status", "SENT")
                .with(user(createAdminUser())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCampaignHistory_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/marketing/campaigns"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLaunchCampaign_Success() throws Exception {
        // Create a template first
        MarketingTemplate template = MarketingTemplate.builder()
                .companyId(companyId)
                .code("TEST_CAMPAIGN")
                .name("Test Campaign")
                .subjectTemplate("Hello {{name}}")
                .bodyHtml("<p>Hello</p>")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .attachments(new ArrayList<>())
                .build();

        template = templateRepository.saveTemplate(template);

        CampaignRequest request = new CampaignRequest();
        request.setTemplateId(template.getId());
        CampaignRequest.AudienceFilter filter = new CampaignRequest.AudienceFilter();
        filter.setCustomerType("active");
        request.setAudienceFilter(filter);

        mockMvc.perform(post("/api/v1/marketing/campaigns")
                .with(user(createAdminUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDryRun_Success() throws Exception {
        // Create a template first
        MarketingTemplate template = MarketingTemplate.builder()
                .companyId(companyId)
                .code("DRY_RUN_TEST")
                .name("Dry Run Template")
                .subjectTemplate("Test {{name}}")
                .bodyHtml("<p>Test</p>")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .attachments(new ArrayList<>())
                .build();

        template = templateRepository.saveTemplate(template);

        Map<String, Object> payload = Map.of(
                "templateId", template.getId(),
                "testEmail", "test@example.com");

        mockMvc.perform(post("/api/v1/marketing/campaigns/dry-run")
                .with(user(createAdminUser()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }
}
