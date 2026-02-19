package es.terencio.erp.marketing.infrastructure.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private CustomUserDetails getGlobalMockUser() {
        return new CustomUserDetails(globalAdminId, java.util.UUID.randomUUID(), "admin", "Admin", "pass");
    }

    @Test
    void testCreateAndGetTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "WELCOME", "Welcome Email", "Welcome!", "<p>Hi</p>", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", globalCompanyId)
                        .with(user(getGlobalMockUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.code").value("WELCOME")).andReturn().getResponse().getContentAsString();

        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();

        mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", globalCompanyId, created.id())
                .with(user(getGlobalMockUser()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Welcome Email"));
    }

    @Test
    void testUpdateTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "NEWSLETTER", "Weekly Newsletter", "News", "Content", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", globalCompanyId)
                        .with(user(getGlobalMockUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();

        TemplateDto updateDto = new TemplateDto(created.id(), created.code(), "Monthly Newsletter", created.subject(),
                created.bodyHtml(), created.active(), created.lastModified());

        mockMvc.perform(put("/api/v1/companies/{companyId}/marketing/templates/{id}", globalCompanyId, created.id())
                .with(user(getGlobalMockUser())).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Monthly Newsletter"));
    }

    @Test
    void testListTemplates() throws Exception {
        mockMvc.perform(
                get("/api/v1/companies/{companyId}/marketing/templates", globalCompanyId)
                        .with(user(getGlobalMockUser())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testDeleteTemplate_Success() throws Exception {
        TemplateDto dto = new TemplateDto(null, "TO_DELETE", "Delete Me", "Bye", "Bye", true, null);
        String response = mockMvc
                .perform(post("/api/v1/companies/{companyId}/marketing/templates", globalCompanyId)
                        .with(user(getGlobalMockUser())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        es.terencio.erp.shared.presentation.ApiResponse<TemplateDto> apiResponse = objectMapper.readValue(response,
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                });
        TemplateDto created = apiResponse.getData();

        mockMvc.perform(delete("/api/v1/companies/{companyId}/marketing/templates/{id}", globalCompanyId, created.id())
                .with(user(getGlobalMockUser()))).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/companies/{companyId}/marketing/templates/{id}", globalCompanyId, created.id())
                .with(user(getGlobalMockUser()))).andExpect(status().is4xxClientError());
    }
}