package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantAdminControllerTest extends BaseIntegrationTest {

    @Test
    void createTenant_ShouldReturnCreatedTenant() throws Exception {
        String requestJson = "{\"name\": \"New Tenant\"}";

        mockMvc.perform(post("/admin/tenants")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Tenant"));
    }

    @Test
    void listTenants_ShouldReturnList() throws Exception {
        // We already have one tenant from BaseIntegrationTest
        // Create another one
        Tenant t2 = Tenant.builder().name("Tenant 2").status("ACTIVE").build();
        tenantRepository.save(t2);

        mockMvc.perform(get("/admin/tenants")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTenant_ShouldReturnTenant() throws Exception {
        mockMvc.perform(get("/admin/tenants/" + testTenant.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Tenant"));
    }

    @Test
    void updateTenant_ShouldUpdateName() throws Exception {
        String requestJson = "{\"name\": \"Updated Name\"}";

        mockMvc.perform(put("/admin/tenants/" + testTenant.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
}
