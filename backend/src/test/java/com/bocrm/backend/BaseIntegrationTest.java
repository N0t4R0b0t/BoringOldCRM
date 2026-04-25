package com.bocrm.backend;

import com.bocrm.backend.dto.LoginRequest;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.entity.TenantMembership;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected TenantMembershipRepository membershipRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ObjectMapper objectMapper;

    protected User testUser;
    protected Tenant testTenant;
    protected String accessToken;

    @BeforeEach
    void baseSetUp() throws Exception {
        // Clear existing data
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create a test tenant
        testTenant = Tenant.builder()
                .name("Test Tenant")
                .status("ACTIVE")
                .build();
        tenantRepository.save(testTenant);

        // Create a test user
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .status("ACTIVE")
                .build();
        userRepository.save(testUser);

        // Create membership
        TenantMembership membership = TenantMembership.builder()
                .tenantId(testTenant.getId())
                .userId(testUser.getId())
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        membershipRepository.save(membership);

        // Login to get token
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseContent).get("accessToken").asText();
    }
}
