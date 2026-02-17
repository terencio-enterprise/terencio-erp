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
                // However, if the Service implementation uses security context to get
                // companyId, we might need to mock that or setup RBAC correctly.
                // For now, inserting company to be safe.
                jdbcClient.sql("INSERT INTO companies (id, name, tax_id) VALUES (?, ?, ?)")
                                .params(companyId, "Test Company", "B12345678")
                                .update();
        }

        private CustomUserDetails createAdminUser() {
                return new CustomUserDetails(1L, UUID.randomUUID(), "admin", "Admin", "pass", "ADMIN", null, companyId);
        }

        @Test
        void testCreateAndGetTemplate_Success() throws Exception {
                TemplateDto dto = new TemplateDto();
                dto.setCode("WELCOME");
                dto.setName("Welcome Email");
                dto.setSubject("Welcome!");
                dto.setBodyHtml("<p>Hi</p>");

                // 1. Create
                String response = mockMvc.perform(post("/api/v1/marketing/templates")
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
                mockMvc.perform(get("/api/v1/marketing/templates/{id}", created.getId())
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

                String response = mockMvc.perform(post("/api/v1/marketing/templates")
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

                mockMvc.perform(put("/api/v1/marketing/templates/{id}", created.getId())
                                .with(user(createAdminUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(created)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Monthly Newsletter"));
        }

        @Test
        void testListTemplates() throws Exception {
                mockMvc.perform(get("/api/v1/marketing/templates")
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

                String response = mockMvc.perform(post("/api/v1/marketing/templates")
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
                mockMvc.perform(delete("/api/v1/marketing/templates/{id}", created.getId())
                                .with(user(createAdminUser())))
                                .andExpect(status().isOk());

                // Verify gone
                mockMvc.perform(get("/api/v1/marketing/templates/{id}", created.getId())
                                .with(user(createAdminUser())))
                                .andExpect(status().is4xxClientError());
        }
}
