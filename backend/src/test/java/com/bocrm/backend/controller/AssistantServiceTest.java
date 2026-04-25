package com.bocrm.backend.controller;

import com.bocrm.backend.BaseIntegrationTest;
import com.bocrm.backend.dto.SendMessageRequest;
import com.bocrm.backend.entity.AssistantTier;
import com.bocrm.backend.entity.TenantSubscription;
import com.bocrm.backend.repository.AssistantTierRepository;
import com.bocrm.backend.repository.TenantSubscriptionRepository;
import com.bocrm.backend.service.ChatModelRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantServiceTest extends BaseIntegrationTest {

    @MockitoBean
    ChatModel chatModel;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @MockitoBean
    ChatModelRegistry chatModelRegistry;

    @Autowired
    private AssistantTierRepository tierRepository;

    @Autowired
    private TenantSubscriptionRepository subscriptionRepository;

    private AssistantTier testTier;

    @BeforeEach
    void setUpAssistant() {
        when(chatModelRegistry.getModel(anyString())).thenReturn(chatModel);

        // Create a tier with a small limit so we can test quota exceeded
        testTier = tierRepository.save(AssistantTier.builder()
                .name("test-tier-" + System.nanoTime())
                .displayName("Test Tier")
                .monthlyTokenLimit(500L)
                .modelId("claude-haiku-4-5-20251001")
                .priceMonthly(BigDecimal.ZERO)
                .build());
    }

    @Test
    void testGetTiers_ReturnsNonEmptyList() throws Exception {
        mockMvc.perform(get("/assistant/tiers")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetSubscription_ReturnsCurrentQuotaStatus() throws Exception {
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .tier(testTier)
                .periodStartDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusMonths(1))
                .tokensUsedThisPeriod(250L)
                .build());

        mockMvc.perform(get("/assistant/subscription")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokensUsedThisPeriod").value(250))
                .andExpect(jsonPath("$.monthlyTokenLimit").value(500));
    }

    @Test
    void testSendMessage_QuotaExceeded_Returns429() throws Exception {
        // Subscription is maxed out at the 500-token limit
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .tier(testTier)
                .periodStartDate(LocalDate.now())
                .periodEndDate(LocalDate.now().plusMonths(1))
                .tokensUsedThisPeriod(500L)
                .build());

        SendMessageRequest request = new SendMessageRequest();
        request.setMessage("Create a customer named Acme");
        request.setSessionId("test-session-quota");
        request.setConfirmMode("auto");

        mockMvc.perform(post("/chat/message")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testSendMessage_ConfirmMode_MockedResponse_Returns200() throws Exception {
        // Mock chatModel to return a plain text response (no tool calls)
        AssistantMessage assistantMsg = new AssistantMessage("Hello! How can I help you?");
        Generation generation = new Generation(assistantMsg);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        SendMessageRequest request = new SendMessageRequest();
        request.setMessage("Hello");
        request.setSessionId("test-session-confirm");
        request.setConfirmMode("confirm");

        mockMvc.perform(post("/chat/message")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    void testSendMessage_RequiresAuth_Returns401() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setMessage("Hello");
        request.setSessionId("test-session-noauth");

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
