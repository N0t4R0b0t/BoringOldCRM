package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateCustomerRequest;
import com.bocrm.backend.dto.UpdateCustomerRequest;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest extends BaseIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void createCustomer_ShouldReturnCreatedCustomer() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Acme Corp");
        request.setStatus("ACTIVE");

        mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getCustomers_ShouldReturnList() throws Exception {
        // Create some customers
        Customer c1 = Customer.builder().tenantId(testTenant.getId()).name("Customer 1").status("ACTIVE").build();
        Customer c2 = Customer.builder().tenantId(testTenant.getId()).name("Customer 2").status("ACTIVE").build();
        customerRepository.save(c1);
        customerRepository.save(c2);

        mockMvc.perform(get("/customers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void getCustomer_ShouldReturnCustomer_WhenExists() throws Exception {
        Customer customer = Customer.builder().tenantId(testTenant.getId()).name("Target Customer").status("ACTIVE").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(get("/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Target Customer"));
    }

    @Test
    void updateCustomer_ShouldUpdateAndReturnCustomer() throws Exception {
        Customer customer = Customer.builder().tenantId(testTenant.getId()).name("Old Name").status("ACTIVE").build();
        customer = customerRepository.save(customer);

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName("New Name");
        request.setStatus("INACTIVE");

        mockMvc.perform(put("/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deleteCustomer_ShouldRemoveCustomer() throws Exception {
        Customer customer = Customer.builder().tenantId(testTenant.getId()).name("To Delete").status("ACTIVE").build();
        customer = customerRepository.save(customer);

        mockMvc.perform(delete("/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
