package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateInvoiceRequest;
import com.bocrm.backend.dto.UpdateInvoiceRequest;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.entity.Invoice;
import com.bocrm.backend.repository.CustomerRepository;
import com.bocrm.backend.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class InvoiceControllerTest extends BaseIntegrationTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUpInvoices() {
        testCustomer = Customer.builder()
                .tenantId(testTenant.getId())
                .name("Test Customer")
                .status("ACTIVE")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void createInvoice_ShouldReturnCreatedInvoice() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(testCustomer.getId());
        request.setStatus("DRAFT");
        request.setCurrency("USD");
        request.setSubtotal(new BigDecimal("1000.00"));
        request.setTaxAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("1100.00"));
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setPaymentTerms("NET-30");

        mockMvc.perform(post("/invoices")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalAmount").value(1100.00));
    }

    @Test
    void getInvoice_ShouldReturnInvoice() throws Exception {
        Invoice invoice = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("500.00"))
                .taxAmount(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("550.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        invoice = invoiceRepository.save(invoice);

        mockMvc.perform(get("/invoices/" + invoice.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(550.00));
    }

    @Test
    void listInvoices_ShouldReturnPaginatedList() throws Exception {
        Invoice i1 = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("DRAFT")
                .currency("USD")
                .totalAmount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        Invoice i2 = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("SENT")
                .currency("USD")
                .totalAmount(new BigDecimal("2000.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        invoiceRepository.save(i1);
        invoiceRepository.save(i2);

        mockMvc.perform(get("/invoices")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateInvoice_ShouldUpdateAndReturnInvoice() throws Exception {
        Invoice invoice = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("DRAFT")
                .currency("USD")
                .totalAmount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        invoice = invoiceRepository.save(invoice);

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setStatus("SENT");
        request.setTotalAmount(new BigDecimal("1500.00"));

        mockMvc.perform(put("/invoices/" + invoice.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.totalAmount").value(1500.00));
    }

    @Test
    void deleteInvoice_ShouldRemoveInvoice() throws Exception {
        Invoice invoice = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("DRAFT")
                .currency("USD")
                .totalAmount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        invoice = invoiceRepository.save(invoice);

        mockMvc.perform(delete("/invoices/" + invoice.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/invoices/" + invoice.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchInvoices_ShouldReturnMatchingInvoices() throws Exception {
        Invoice i1 = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("DRAFT")
                .currency("USD")
                .totalAmount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusDays(30))
                .paymentTerms("NET-30")
                .build();
        Invoice i2 = Invoice.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .status("PAID")
                .currency("USD")
                .totalAmount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().minusDays(10))
                .paymentTerms("NET-15")
                .build();
        invoiceRepository.save(i1);
        invoiceRepository.save(i2);

        mockMvc.perform(get("/invoices/search")
                        .param("term", "PAID")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PAID"));
    }
}
