package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateActivityRequest;
import com.bocrm.backend.dto.UpdateActivityRequest;
import com.bocrm.backend.entity.Activity;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.repository.ActivityRepository;
import com.bocrm.backend.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActivityControllerTest extends BaseIntegrationTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUpActivities() {
        testCustomer = Customer.builder()
                .tenantId(testTenant.getId())
                .name("Test Customer")
                .status("ACTIVE")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void createActivity_ShouldReturnCreatedActivity() throws Exception {
        CreateActivityRequest request = new CreateActivityRequest();
        request.setRelatedId(testCustomer.getId());
        request.setRelatedType("CUSTOMER");
        request.setType("CALL");
        request.setSubject("Intro Call");
        request.setDescription("Discussed requirements");
        request.setDueAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Intro Call"))
                .andExpect(jsonPath("$.type").value("CALL"));
    }

    @Test
    void getActivities_ShouldReturnList() throws Exception {
        Activity a1 = Activity.builder().tenantId(testTenant.getId()).relatedId(testCustomer.getId()).relatedType("CUSTOMER").type("EMAIL").subject("Email 1").status("NEW").build();
        Activity a2 = Activity.builder().tenantId(testTenant.getId()).relatedId(testCustomer.getId()).relatedType("CUSTOMER").type("MEETING").subject("Meeting 1").status("NEW").build();
        activityRepository.save(a1);
        activityRepository.save(a2);

        mockMvc.perform(get("/activities")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void updateActivity_ShouldUpdateAndReturnActivity() throws Exception {
        Activity activity = Activity.builder().tenantId(testTenant.getId()).relatedId(testCustomer.getId()).relatedType("CUSTOMER").type("TASK").subject("Old Subject").status("NEW").build();
        activity = activityRepository.save(activity);

        UpdateActivityRequest request = new UpdateActivityRequest();
        request.setSubject("New Subject");
        request.setStatus("COMPLETED");

        mockMvc.perform(put("/activities/" + activity.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("New Subject"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteActivity_ShouldRemoveActivity() throws Exception {
        Activity activity = Activity.builder().tenantId(testTenant.getId()).relatedId(testCustomer.getId()).relatedType("CUSTOMER").type("TASK").subject("To Delete").status("NEW").build();
        activity = activityRepository.save(activity);

        mockMvc.perform(delete("/activities/" + activity.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/activities/" + activity.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
