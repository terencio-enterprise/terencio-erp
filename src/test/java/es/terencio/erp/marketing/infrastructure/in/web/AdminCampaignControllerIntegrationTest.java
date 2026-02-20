package es.terencio.erp.marketing.infrastructure.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.AbstractIntegrationTest;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;
import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignRequest;
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

        private CustomUserDetails getGlobalMockUser() {
                return new CustomUserDetails(globalAdminId, java.util.UUID.randomUUID(), "admin", "Admin", "pass");
        }

        @Test
        void testGetCampaignHistory_Success() throws Exception {
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/campaigns", globalCompanyId)
                                .param("status", "SENT")
                                .with(user(getGlobalMockUser()))).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        void testGetCampaignHistory_Forbidden() throws Exception {
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/campaigns", globalCompanyId))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testLaunchCampaign_Success() throws Exception {
                MarketingTemplate template = new MarketingTemplate(null, globalCompanyId, "TEST_CAMPAIGN",
                                "Test Campaign",
                                "Hello {{name}}", "<p>Hello</p>", true, Instant.now(), Instant.now());
                template = templateRepository.saveTemplate(template);
                CampaignRequest request = new CampaignRequest(null, template.getId(),
                                new AudienceFilter(null, null, "CLIENT"));
                mockMvc.perform(
                                post("/api/v1/companies/{companyId}/marketing/campaigns", globalCompanyId)
                                                .with(user(getGlobalMockUser()))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void testDryRun_Success() throws Exception {
                MarketingTemplate template = new MarketingTemplate(null, globalCompanyId, "DRY_RUN_TEST",
                                "Dry Run Template",
                                "Test {{name}}", "<p>Test</p>", true, Instant.now(), Instant.now());
                template = templateRepository.saveTemplate(template);
                Map<String, Object> payload = Map.of("templateId", template.getId(), "testEmail", "test@example.com");
                mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/campaigns/dry-run", globalCompanyId)
                                .with(user(getGlobalMockUser())).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload))).andExpect(status().isOk());
        }
}