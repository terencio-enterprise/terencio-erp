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

                // Setup permissions and roles for runtime check
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:campaign:view", "View Campaign", "Desc", "MARKETING")
                                .update();
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:campaign:launch", "Launch Campaign", "Desc", "MARKETING")
                                .update();
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:email:preview", "Email Preview", "Desc", "MARKETING")
                                .update();

                jdbcClient.sql("INSERT INTO roles (name, description) VALUES (?, ?) ON CONFLICT (name) DO NOTHING")
                                .params("MARKETING_MANAGER", "Marketing Manager")
                                .update();

                // Map permissions to role
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:campaign:view")
                                .update();
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:campaign:launch")
                                .update();
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:email:preview")
                                .update();

                // Create Admin User in DB (ID 1L to match mock user)
                jdbcClient.sql("INSERT INTO employees (id, username, password_hash, full_name, uuid, is_active) VALUES (?, ?, ?, ?, ?, ?)")
                                .params(1L, "admin", "pass", "Admin", UUID.randomUUID(), true)
                                .update();

                // Grant Role to User for Company
                jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)")
                                .params(1L, "MARKETING_MANAGER", "COMPANY", companyId,
                                                java.sql.Timestamp.from(Instant.now()))
                                .update();
        }

        private CustomUserDetails createAdminUser() {
                return new CustomUserDetails(1L, UUID.randomUUID(), "admin", "Admin", "pass");
        }

        @Test
        void testGetCampaignHistory_Success() throws Exception {
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/campaigns", companyId)
                                .param("status", "SENT")
                                .with(user(createAdminUser())))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        void testGetCampaignHistory_Forbidden() throws Exception {
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/campaigns", companyId))
                                .andExpect(status().isForbidden());
        }

        @Test
        void testLaunchCampaign_Success() throws Exception {
                // Create a template first
                MarketingTemplate template = new MarketingTemplate(
                                null,
                                companyId,
                                "TEST_CAMPAIGN",
                                "Test Campaign",
                                "Hello {{name}}",
                                "<p>Hello</p>",
                                true,
                                Instant.now(),
                                Instant.now(),
                                new ArrayList<>());

                template = templateRepository.saveTemplate(template);

                CampaignRequest request = new CampaignRequest();
                request.setTemplateId(template.getId());
                CampaignRequest.AudienceFilter filter = new CampaignRequest.AudienceFilter();
                filter.setCustomerType("active");
                request.setAudienceFilter(filter);

                mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/campaigns", companyId)
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void testDryRun_Success() throws Exception {
                // Create a template first
                MarketingTemplate template = new MarketingTemplate(
                                null,
                                companyId,
                                "DRY_RUN_TEST",
                                "Dry Run Template",
                                "Test {{name}}",
                                "<p>Test</p>",
                                true,
                                Instant.now(),
                                Instant.now(),
                                new ArrayList<>());

                template = templateRepository.saveTemplate(template);

                Map<String, Object> payload = Map.of(
                                "templateId", template.getId(),
                                "testEmail", "test@example.com");

                mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/campaigns/dry-run", companyId)
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk());
        }
}
