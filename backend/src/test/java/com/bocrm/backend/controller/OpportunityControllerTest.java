package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateOpportunityRequest;
import com.bocrm.backend.dto.UpdateOpportunityRequest;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.entity.Opportunity;
import com.bocrm.backend.repository.CustomerRepository;
import com.bocrm.backend.repository.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpportunityControllerTest extends BaseIntegrationTest {

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUpOpportunities() {
        testCustomer = Customer.builder()
                .tenantId(testTenant.getId())
                .name("Test Customer")
                .status("ACTIVE")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void createOpportunity_ShouldReturnCreatedOpportunity() throws Exception {
        CreateOpportunityRequest request = new CreateOpportunityRequest();
        request.setCustomerId(testCustomer.getId());
        request.setName("Big Deal");
        request.setStage("PROSPECTING");
        request.setValue(new BigDecimal("10000.00"));

        mockMvc.perform(post("/opportunities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Big Deal"))
                .andExpect(jsonPath("$.value").value(10000.00));
    }

    @Test
    void getOpportunities_ShouldReturnList() throws Exception {
        Opportunity o1 = Opportunity.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Opp 1").stage("NEW").value(BigDecimal.TEN).build();
        Opportunity o2 = Opportunity.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Opp 2").stage("WON").value(BigDecimal.ONE).build();
        opportunityRepository.save(o1);
        opportunityRepository.save(o2);

        mockMvc.perform(get("/opportunities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void updateOpportunity_ShouldUpdateAndReturnOpportunity() throws Exception {
        Opportunity opportunity = Opportunity.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Old Name").stage("NEW").value(BigDecimal.TEN).build();
        opportunity = opportunityRepository.save(opportunity);

        UpdateOpportunityRequest request = new UpdateOpportunityRequest();
        request.setName("New Name");
        request.setStage("WON");
        request.setValue(new BigDecimal("20000.00"));

        mockMvc.perform(put("/opportunities/" + opportunity.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.stage").value("WON"))
                .andExpect(jsonPath("$.value").value(20000.00));
    }

    @Test
    void deleteOpportunity_ShouldRemoveOpportunity() throws Exception {
        Opportunity opportunity = Opportunity.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("To Delete").stage("NEW").value(BigDecimal.TEN).build();
        opportunity = opportunityRepository.save(opportunity);

        mockMvc.perform(delete("/opportunities/" + opportunity.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/opportunities/" + opportunity.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
