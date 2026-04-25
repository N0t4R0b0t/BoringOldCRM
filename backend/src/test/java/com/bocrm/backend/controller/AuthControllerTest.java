package com.bocrm.backend.controller;

import com.bocrm.backend.dto.LoginRequest;
import com.bocrm.backend.dto.RefreshTokenRequest;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.entity.TenantMembership;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear existing data to avoid conflicts
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create a test tenant
        Tenant tenant = Tenant.builder()
                .name("Test Tenant")
                .status("ACTIVE")
                .build();
        tenantRepository.save(tenant);

        // Create a test user
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .status("ACTIVE")
                .build();
        userRepository.save(testUser);

        // Create membership
        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(testUser.getId())
                .role("ADMIN")
                .status("ACTIVE")
                .build();
        membershipRepository.save(membership);
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void login_ShouldFail_WhenCredentialsAreInvalid() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_ShouldReturnNewToken_WhenRefreshTokenIsValid() throws Exception {
        // First login to get tokens
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseContent).get("refreshToken").asText();

        // Now refresh
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }
}
