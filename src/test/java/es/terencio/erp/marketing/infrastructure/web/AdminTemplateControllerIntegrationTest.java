package es.terencio.erp.marketing.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.marketing.application.dto.TemplateDto;

@AutoConfigureMockMvc
class AdminTemplateControllerIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        private UUID companyId;

        @BeforeEach
        void setup() {
                cleanDatabase();
                companyId = UUID.randomUUID();
                // Insert a dummy company to satisfy FK constraints if we were to hit the DB
                // with entities.
                jdbcClient.sql("INSERT INTO companies (id, name, tax_id) VALUES (?, ?, ?)")
                                .params(companyId, "Test Company", "B12345678")
                                .update();

                // Setup permissions and roles for runtime check
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:template:view", "View Template", "Desc", "MARKETING")
                                .update();
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:template:create", "Create Template", "Desc", "MARKETING")
                                .update();
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:template:edit", "Edit Template", "Desc", "MARKETING")
                                .update();
                jdbcClient.sql("INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                                .params("marketing:template:delete", "Delete Template", "Desc", "MARKETING")
                                .update();

                jdbcClient.sql("INSERT INTO roles (name, description) VALUES (?, ?) ON CONFLICT (name) DO NOTHING")
                                .params("MARKETING_MANAGER", "Marketing Manager")
                                .update();

                // Map permissions to role
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:template:view")
                                .update();
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:template:create")
                                .update();
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:template:edit")
                                .update();
                jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                .params("MARKETING_MANAGER", "marketing:template:delete")
                                .update();

                // Create Admin User in DB (ID 1L to match mock user)
                jdbcClient.sql("INSERT INTO employees (id, username, password_hash, full_name, uuid, is_active) VALUES (?, ?, ?, ?, ?, ?)")
                                .params(1L, "admin", "pass", "Admin", UUID.randomUUID(), true)
                                .update();

                // Grant Role to User for Company
                jdbcClient.sql("INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)")
                                .params(1L, "MARKETING_MANAGER", "COMPANY", companyId,
                                                java.sql.Timestamp.from(java.time.Instant.now()))
                                .update();
        }

        private CustomUserDetails createAdminUser() {
                return new CustomUserDetails(1L, UUID.randomUUID(), "admin", "Admin", "pass");
        }

        @Test
        void testCreateAndGetTemplate_Success() throws Exception {
                TemplateDto dto = new TemplateDto();
                dto.setCode("WELCOME");
                dto.setName("Welcome Email");
                dto.setSubject("Welcome!");
                dto.setBodyHtml("<p>Hi</p>");

                // 1. Create
                String response = mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").exists())
                                .andExpect(jsonPath("$.data.code").value("WELCOME"))
                                .andReturn().getResponse().getContentAsString();

                // Extract data from ApiResponse
                es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(
                                response,
                                new com.fasterxml.jackson.core.type.TypeReference<es.terencio.erp.shared.presentation.ApiResponse<TemplateDto>>() {
                                });
                TemplateDto created = apiResponse.getData();

                // 2. Get
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId,
                                created.getId())
                                .with(user(createAdminUser())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Welcome Email"));
        }

        @Test
        void testUpdateTemplate_Success() throws Exception {
                // Setup existing template via POST
                TemplateDto dto = new TemplateDto();
                dto.setCode("NEWSLETTER");
                dto.setName("Weekly Newsletter");
                dto.setSubject("News");
                dto.setBodyHtml("Content");

                String response = mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(
                                response,
                                new com.fasterxml.jackson.core.type.TypeReference<es.terencio.erp.shared.presentation.ApiResponse<TemplateDto>>() {
                                });
                TemplateDto created = apiResponse.getData();

                // Update
                created.setName("Monthly Newsletter");

                mockMvc.perform(put("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId,
                                created.getId())
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(created)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Monthly Newsletter"));
        }

        @Test
        void testListTemplates() throws Exception {
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates", companyId)
                                .with(user(createAdminUser())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        void testDeleteTemplate_Success() throws Exception {
                // Create first
                TemplateDto dto = new TemplateDto();
                dto.setCode("TO_DELETE");
                dto.setName("Delete Me");
                dto.setSubject("Bye");
                dto.setBodyHtml("Bye");

                String response = mockMvc.perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(
                                response,
                                new com.fasterxml.jackson.core.type.TypeReference<es.terencio.erp.shared.presentation.ApiResponse<TemplateDto>>() {
                                });
                TemplateDto created = apiResponse.getData();

                // Delete
                mockMvc.perform(delete("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId,
                                created.getId())
                                .with(user(createAdminUser())))
                                .andExpect(status().isOk());

                // Verify gone
                mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId,
                                created.getId())
                                .with(user(createAdminUser())))
                                .andExpect(status().is4xxClientError());
        }
}
