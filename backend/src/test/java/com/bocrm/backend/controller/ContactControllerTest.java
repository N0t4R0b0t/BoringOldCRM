package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateContactRequest;
import com.bocrm.backend.dto.UpdateContactRequest;
import com.bocrm.backend.entity.Contact;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.repository.ContactRepository;
import com.bocrm.backend.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactControllerTest extends BaseIntegrationTest {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUpContacts() {
        testCustomer = Customer.builder()
                .tenantId(testTenant.getId())
                .name("Test Customer")
                .status("ACTIVE")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void createContact_ShouldReturnCreatedContact() throws Exception {
        CreateContactRequest request = new CreateContactRequest();
        request.setCustomerId(testCustomer.getId());
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");

        mockMvc.perform(post("/contacts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getContacts_ShouldReturnList() throws Exception {
        Contact c1 = Contact.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Contact 1").email("c1@test.com").build();
        Contact c2 = Contact.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Contact 2").email("c2@test.com").build();
        contactRepository.save(c1);
        contactRepository.save(c2);

        mockMvc.perform(get("/contacts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void updateContact_ShouldUpdateAndReturnContact() throws Exception {
        Contact contact = Contact.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("Old Name").email("old@test.com").build();
        contact = contactRepository.save(contact);

        UpdateContactRequest request = new UpdateContactRequest();
        request.setName("New Name");
        request.setEmail("new@test.com");

        mockMvc.perform(put("/contacts/" + contact.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void deleteContact_ShouldRemoveContact() throws Exception {
        Contact contact = Contact.builder().tenantId(testTenant.getId()).customerId(testCustomer.getId()).name("To Delete").email("del@test.com").build();
        contact = contactRepository.save(contact);

        mockMvc.perform(delete("/contacts/" + contact.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/contacts/" + contact.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
