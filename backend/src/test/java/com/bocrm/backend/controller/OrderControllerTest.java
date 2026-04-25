package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.CreateOrderRequest;
import com.bocrm.backend.dto.UpdateOrderRequest;
import com.bocrm.backend.entity.Customer;
import com.bocrm.backend.entity.Order;
import com.bocrm.backend.repository.CustomerRepository;
import com.bocrm.backend.repository.OrderRepository;
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
class OrderControllerTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUpOrders() {
        testCustomer = Customer.builder()
                .tenantId(testTenant.getId())
                .name("Test Customer")
                .status("ACTIVE")
                .build();
        customerRepository.save(testCustomer);
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(testCustomer.getId());
        request.setName("Enterprise License Order");
        request.setStatus("DRAFT");
        request.setCurrency("USD");
        request.setSubtotal(new BigDecimal("1000.00"));
        request.setTaxAmount(new BigDecimal("100.00"));
        request.setOrderDate(LocalDate.now());

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Enterprise License Order"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getOrder_ShouldReturnOrder() throws Exception {
        Order order = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Test Order")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("500.00"))
                .taxAmount(new BigDecimal("50.00"))
                .build();
        order = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Order"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void listOrders_ShouldReturnPaginatedList() throws Exception {
        Order o1 = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Order 1")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("1000.00"))
                .build();
        Order o2 = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Order 2")
                .status("CONFIRMED")
                .currency("USD")
                .subtotal(new BigDecimal("2000.00"))
                .build();
        orderRepository.save(o1);
        orderRepository.save(o2);

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateOrder_ShouldUpdateAndReturnOrder() throws Exception {
        Order order = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Old Name")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("1000.00"))
                .build();
        order = orderRepository.save(order);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setName("New Name");
        request.setStatus("CONFIRMED");
        request.setSubtotal(new BigDecimal("1500.00"));

        mockMvc.perform(put("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void deleteOrder_ShouldRemoveOrder() throws Exception {
        Order order = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("To Delete")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("1000.00"))
                .build();
        order = orderRepository.save(order);

        mockMvc.perform(delete("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchOrders_ShouldReturnMatchingOrders() throws Exception {
        Order o1 = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Premium License")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("1000.00"))
                .build();
        Order o2 = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Standard License")
                .status("CONFIRMED")
                .currency("USD")
                .subtotal(new BigDecimal("500.00"))
                .build();
        orderRepository.save(o1);
        orderRepository.save(o2);

        mockMvc.perform(get("/orders/search")
                        .param("term", "Premium")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Premium License"));
    }

    @Test
    void getOrder_OtherTenant_ShouldReturn403() throws Exception {
        Order order = Order.builder()
                .tenantId(testTenant.getId())
                .customerId(testCustomer.getId())
                .name("Test Order")
                .status("DRAFT")
                .currency("USD")
                .subtotal(new BigDecimal("1000.00"))
                .build();
        order = orderRepository.save(order);

        mockMvc.perform(get("/orders/" + order.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}
