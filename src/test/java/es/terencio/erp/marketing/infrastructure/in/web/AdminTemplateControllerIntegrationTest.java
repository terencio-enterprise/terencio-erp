package es.terencio.erp.marketing.infrastructure.in.web;

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
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;
import es.terencio.erp.marketing.application.dto.MarketingDtos.TemplateDto;

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
        companyId = createTestCompany();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                .params("marketing:template:view", "View Template", "Desc", "MARKETING").update();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                .params("marketing:template:create", "Create Template", "Desc", "MARKETING").update();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                .params("marketing:template:edit", "Edit Template", "Desc", "MARKETING").update();
        jdbcClient.sql(
                "INSERT INTO permissions (code, name, description, module) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO NOTHING")
                .params("marketing:template:delete", "Delete Template", "Desc", "MARKETING").update();
        jdbcClient.sql("INSERT INTO roles (name, description) VALUES (?, ?) ON CONFLICT (name) DO NOTHING")
                .params("MARKETING_MANAGER", "Marketing Manager").update();
        jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params("MARKETING_MANAGER", "marketing:template:view").update();
        jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params("MARKETING_MANAGER", "marketing:template:create").update();
        jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params("MARKETING_MANAGER", "marketing:template:edit").update();
        jdbcClient.sql("INSERT INTO role_permissions (role_name, permission_code) VALUES (?, ?) ON CONFLICT DO NOTHING")
                .params("MARKETING_MANAGER", "marketing:template:delete").update();
        jdbcClient.sql(
                "INSERT INTO employees (id, username, password_hash, full_name, uuid, organization_id, is_active) VALUES (?, ?, ?, ?, ?, (SELECT organization_id FROM companies WHERE id = ?), ?)")
                .params(1L, "admin", "pass", "Admin", UUID.randomUUID(), companyId, true).update();
        jdbcClient.sql(
                "INSERT INTO employee_access_grants (employee_id, role, scope, target_id, created_at) VALUES (?, ?, ?, ?, ?)")
                .params(1L, "MARKETING_MANAGER", "COMPANY", companyId, java.sql.Timestamp.from(java.time.Instant.now()))
                .update();
    }

    private CustomUserDetails createAdminUser() {
        return new CustomUserDetails(1L, UUID.randomUUID(), "admin", "Admin", "pass");
    }

    @Test
    void testCreateAndGetTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "WELCOME", "Welcome Email", "Welcome!", "<p>Hi</p>", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                        .with(user(createAdminUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.code").value("WELCOME")).andReturn().getResponse().getContentAsString();
        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();
        mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId, created.id())
                .with(user(createAdminUser()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Welcome Email"));
    }

    @Test
    void testUpdateTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "NEWSLETTER", "Weekly Newsletter", "News", "Content", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                        .with(user(createAdminUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();
        TemplateDto updateDto = new TemplateDto(created.id(), created.code(), "Monthly Newsletter", created.subject(),
                created.bodyHtml(), created.active(), created.lastModified());
        mockMvc.perform(put("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId, created.id())
                .with(user(createAdminUser())).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Monthly Newsletter"));
    }

    @Test
    void testListTemplates() throws Exception {
        mockMvc.perform(
                get("/api/v1/companies/{companyId}/marketing/templates", companyId).with(user(createAdminUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testDeleteTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "TO_DELETE", "Delete Me", "Bye", "Bye", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", companyId)
                        .with(user(createAdminUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();
        mockMvc.perform(delete("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId, created.id())
                .with(user(createAdminUser()))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", companyId, created.id())
                .with(user(createAdminUser()))).andExpect(status().is4xxClientError());
    }
}
