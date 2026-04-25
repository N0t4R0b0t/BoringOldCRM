/*
 * BoringOldCRM - Open-source multi-tenant CRM
 * Copyright (C) 2026 Ricardo Salvador
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Source: https://github.com/N0t4R0b0t/BoringOldCRM
 */
package com.bocrm.backend.service;

import com.bocrm.backend.dto.*;
import com.bocrm.backend.entity.ChatMessage;
import com.bocrm.backend.entity.EnabledAiModel;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.repository.ChatMessageRepository;
import com.bocrm.backend.repository.EnabledAiModelRepository;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.tools.AdminTools;
import com.bocrm.backend.tools.CrmTools;
import com.bocrm.backend.tools.DocumentGenerationTools;
import com.bocrm.backend.tools.PolicyManagementTools;
import com.bocrm.backend.tools.SystemAdminTools;
import com.bocrm.backend.tools.WebSearchTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * Core AI assistant service. Delegates to Spring AI for LLM communication,
 * enforces per-tenant token quotas, and manages confirm vs. auto tool execution.
 *
 * <p><strong>Tool Dispatch Pattern</strong>:
 * This service bridges user messages and Spring AI LLM calls. When the LLM decides to use a tool,
 * two paths are possible:
 * <ol>
 *   <li><strong>Confirm Mode</strong> (default): LLM returns tool schemas and invocations without executing.
 *     The service builds a {@link PendingActionDTO} list for the frontend, which shows tool summaries
 *     and asks user confirmation. User clicks "Execute" → frontend sends executeActions=true →
 *     service finds stored tool calls in {@link ChatMessage.toolCalls} JSON and invokes them via
 *     the {@link #executeToolByName(String, String, boolean)} dispatcher switch.</li>
 *   <li><strong>Auto Mode</strong>: LLM invokes tools immediately, returns final result. No confirmation.</li>
 * </ol>
 *
 * <p><strong>The Tool Dispatch Switch</strong>:
 * The giant {@code switch(toolName)} in {@link #executeToolByName} <em>must</em> stay in sync with
 * all {@code @Tool} methods across CrmTools, AdminTools, DocumentGenerationTools, PolicyManagementTools,
 * and WebSearchTools. Every new @Tool method requires a new case here. If a tool is added but not
 * dispatched, confirm-mode actions will fail silently.
 *
 * <p><strong>System Prompt</strong>:
 * Built dynamically in {@link #buildSystemPrompt()} and documents to the LLM:
 * <ul>
 *   <li>Available tools (CRM create/update/delete, search, analysis, document generation, etc.)
 *   <li>Custom field types including workflow milestone trackers with currentIndex tracking
 *   <li>Document style parameters (layout, colors, field inclusion)
 *   <li>Multi-provider resilience: Anthropic, OpenAI, Google
 *   <li>Admin-only tools (custom fields, calculated fields, policy rules, tenant settings)
 *   <li>Confirm-mode availability for users to review before execution
 * </ul>
 *
 * <p><strong>Confirm-Mode Google Gemini Bug</strong>:
 * Google Gemini has a known issue with {@code internalToolExecutionEnabled(false)}. If confirm mode
 * fails with Google, the service catches the exception and falls back to auto mode (see line 133-135).
 *
 * <p><strong>Token Quota</strong>:
 * Every message call checks {@link TokenQuotaService#checkQuota(Long)} before processing.
 * After LLM execution, input/output tokens are recorded via {@link TokenQuotaService#recordUsage}.
 *
 * <p><strong>Multi-Provider Model Resolution</strong>:
 * {@link ChatModelRegistry#getModel(String)} resolves ChatModel by provider name.
 * Options: "anthropic", "openai", "google". Model IDs come from per-tenant AI tier assignment.
 *
 * <p><strong>Chat History</strong>:
 * Messages are persisted to {@link ChatMessageRepository} per tenant/user/session.
 * The conversation maintains context via {@link #buildConversationMessages}, keeping the last
 * {@code HISTORY_WINDOW} messages to avoid token bloat.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class AssistantService {

    private static final int HISTORY_WINDOW = 20;

    private final ChatModelRegistry chatModelRegistry;
    private final TokenQuotaService tokenQuotaService;
    private final CrmTools crmTools;
    private final AdminTools adminTools;
    private final WebSearchTools webSearchTools;
    private final DocumentGenerationTools documentGenerationTools;
    private final PolicyManagementTools policyManagementTools;
    private final SystemAdminTools systemAdminTools;
    private final ChatMessageRepository chatMessageRepository;
    private final TenantAdminService tenantAdminService;
    private final ObjectMapper objectMapper;
    private final EnabledAiModelRepository enabledAiModelRepository;

    public AssistantService(ChatModelRegistry chatModelRegistry,
                             TokenQuotaService tokenQuotaService,
                             CrmTools crmTools, AdminTools adminTools,
                             WebSearchTools webSearchTools,
                             DocumentGenerationTools documentGenerationTools,
                             PolicyManagementTools policyManagementTools,
                             SystemAdminTools systemAdminTools,
                             ChatMessageRepository chatMessageRepository,
                             TenantAdminService tenantAdminService,
                             ObjectMapper objectMapper,
                             EnabledAiModelRepository enabledAiModelRepository) {
        this.chatModelRegistry = chatModelRegistry;
        this.tokenQuotaService = tokenQuotaService;
        this.crmTools = crmTools;
        this.adminTools = adminTools;
        this.webSearchTools = webSearchTools;
        this.documentGenerationTools = documentGenerationTools;
        this.policyManagementTools = policyManagementTools;
        this.systemAdminTools = systemAdminTools;
        this.chatMessageRepository = chatMessageRepository;
        this.tenantAdminService = tenantAdminService;
        this.objectMapper = objectMapper;
        this.enabledAiModelRepository = enabledAiModelRepository;
    }

    /**
     * Process an incoming user message: build context, call LLM, handle tool invocations, record usage.
     *
     * <p><strong>Execution Flow</strong>:
     * <ol>
     *   <li>Validate tenant/user context from JWT (set by JwtAuthenticationFilter)
     *   <li>Check token quota (throw if exceeded)
     *   <li>Resolve AI model, provider, and admin role
     *   <li>If executeActions=true: find stored tool calls from last assistant message and invoke them
     *   <li>Otherwise: build conversation history and call LLM
     *   <li>In confirm mode: capture tool calls without executing; build PendingActionDTO for user review
     *   <li>In auto mode: LLM executes tools immediately; return final response
     *   <li>Record token usage for quota tracking
     *   <li>Persist user and assistant messages to ChatMessageRepository
     *   <li>Return ChatResponseDTO with message, pending actions (if any), and quota status
     * </ol>
     *
     * <p><strong>Confirm Mode</strong> (default):
     * When the LLM decides to invoke tools, this mode returns the tool schemas and proposed
     * invocations without executing them. The frontend displays pending actions and asks user
     * confirmation. If user approves, they send another request with executeActions=true.
     * The service then looks up the stored tool calls and executes them via {@link #executeStoredActions}.
     *
     * <p><strong>Why Google Fallback?</strong>
     * Google Gemini exhibits a bug with {@code internalToolExecutionEnabled(false)}.
     * If confirm mode fails, the service logs a warning and retries in auto mode.
     *
     * @param request the user message request (message text, sessionId, confirmMode, etc.)
     * @return ChatResponseDTO with the assistant's response and any pending actions
     * @throws ForbiddenException if tenant or user context is missing
     */
    public ChatResponseDTO processMessage(SendMessageRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        if (tenantId == null || userId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        // WHY: Check quota early to fail fast if tenant/user has exhausted their token allowance.
        // This prevents wasted LLM calls and ensures fair resource usage across tenants.
        tokenQuotaService.checkQuota(tenantId);

        // WHY: Resolve the model and provider per tenant, not globally. Each tenant can have a different
        // AI tier (e.g., one uses Claude, another uses GPT-4o). TierInfo also includes the model ID.
        TokenQuotaService.TierInfo tierInfo = tokenQuotaService.resolveTierInfoForTenant(tenantId);
        String modelId = tierInfo.modelId();
        String provider = tierInfo.provider();
        boolean isAdmin = isCurrentUserAdmin();
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId() : UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // WHY: executeActions flag indicates a follow-up message where user has approved pending actions.
        // We then execute the stored tool calls from the previous assistant message.
        if (Boolean.TRUE.equals(request.getExecuteActions())) {
            return executeStoredActions(request, tenantId, userId, provider, modelId, isAdmin, sessionId, startTime);
        }

        // WHY: Build conversation history with context. The request may contain page context
        // (e.g., which list page is active) so the assistant knows what data to operate on.
        List<Message> messages = buildConversationMessages(tenantId, userId, sessionId, request, isAdmin);

        // WHY: Confirm vs. auto mode is user-configurable. Default is confirm to show actions before execution.
        // Exception: workspace setup prompts are forced to auto mode so the LLM runs the full
        // tool-call loop (settings → custom fields → bulk data → templates) in one response
        // without stopping after each batch waiting for user confirmation.
        String confirmMode = (request.getConfirmMode() != null) ? request.getConfirmMode() : "confirm";
        if (!"auto".equals(confirmMode) && isWorkspaceSetupPrompt(request.getMessage())) {
            log.info("Workspace setup detected — overriding confirm mode to auto for tenant {}", tenantId);
            confirmMode = "auto";
        }

        ChatResponse chatResponse;
        List<PendingActionDTO> pendingActions = null;
        JsonNode toolCallsJson = null;

        if ("auto".equals(confirmMode)) {
            chatResponse = callInAutoModeWithFallback(messages, provider, modelId, isAdmin);
        } else {
            // WHY: Confirm mode sends tool definitions to the LLM and asks it to propose actions.
            // The LLM returns tool calls but we don't execute them yet. Google Gemini sometimes fails here.
            try {
                chatResponse = callInConfirmModeWithFallback(messages, provider, modelId, isAdmin);
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    List<AssistantMessage.ToolCall> toolCalls = chatResponse.getResult().getOutput().getToolCalls();
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        pendingActions = buildPendingActions(toolCalls);
                        // WHY: Serialize tool calls to JSON for storage in ChatMessage.toolCalls (JSONB column).
                        // This allows the next request (with executeActions=true) to retrieve and execute them.
                        toolCallsJson = serializeToolCalls(toolCalls);
                    }
                }
            } catch (Exception e) {
                // WHY: Confirm mode with all fallback models failed. Fall back to auto mode as last resort.
                // This ensures the user still gets a response even if confirm mode entirely fails.
                log.warn("Confirm mode failed for all enabled models, falling back to auto mode: {}", e.getMessage());
                chatResponse = callInAutoModeWithFallback(messages, provider, modelId, isAdmin);
            }
        }

        // WHY: Extract token usage from LLM response metadata. This is used for quota tracking
        // and billing (if applicable). Some providers may not include usage info.
        int inputTokens = 0, outputTokens = 0;
        if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
            inputTokens = (int) chatResponse.getMetadata().getUsage().getPromptTokens();
            outputTokens = (int) chatResponse.getMetadata().getUsage().getCompletionTokens();
        }
        tokenQuotaService.recordUsage(tenantId, userId, inputTokens, outputTokens, modelId, sessionId, "chat");

        // WHY: Persist messages for audit and history. This enables users to scroll back and see
        // previous conversations, and allows the service to build conversation history for context.
        saveUserMessage(tenantId, userId, request, sessionId);
        long processingTimeMs = System.currentTimeMillis() - startTime;
        saveAssistantMessage(tenantId, userId, chatResponse, sessionId, modelId, inputTokens, outputTokens, toolCallsJson, processingTimeMs);
        // 8. Build response
        try {
            return buildResponse(chatResponse, pendingActions, sessionId, modelId, processingTimeMs,
                    buildHistory(tenantId, userId, sessionId), tokenQuotaService.getQuotaStatus(tenantId),
                    crmTools.popPendingNavigation());
        } catch (Exception e) {
            log.error("Error building response for session {}: {}", sessionId, e.getMessage(), e);
            // Return minimal response to avoid broken pipe on serialization
            String responseText = "Response processed but encountered an error during serialization. " +
                    "Your message was received and actions may have been executed. " +
                    "Please refresh and check your data.";
            return ChatResponseDTO.builder()
                    .response(responseText)
                    .sessionId(sessionId)
                    .modelUsed(modelId)
                    .processingTimeMs(processingTimeMs)
                    .pendingActions(Collections.emptyList())
                    .quotaStatus(tokenQuotaService.getQuotaStatus(tenantId))
                    .history(Collections.emptyList())
                    .suggestedActions(Collections.emptyList())
                    .navigateTo(null)
                    .build();
        }
    }

    // --- Private helpers ---

    /**
     * Call LLM in auto mode: LLM can invoke tools and receives results automatically.
     *
     * <p>WHY: Auto mode uses ChatClient fluent API, which handles tool execution internally.
     * The LLM sees tool results and continues reasoning with them. No human-in-the-loop.
     * This is faster but less safe for destructive operations.
     *
     * @param messages the conversation history
     * @param provider the AI provider (anthropic, openai, google)
     * @param modelId the specific model ID for this provider
     * @param isAdmin whether current user is admin (enables admin-only tools)
     * @return ChatResponse with final LLM output (may include tool results)
     */
    private ChatResponse callInAutoMode(List<Message> messages, String provider, String modelId, boolean isAdmin) {
        ChatOptions options = buildOptions(provider, modelId);
        ChatModel model = chatModelRegistry.getModel(provider);
        ChatClient client = ChatClient.create(model);
        try {
            ChatClient.ChatClientRequestSpec spec = client.prompt()
                    .messages(messages)
                    .options(options)
                    .tools(crmTools)
                    .tools(webSearchTools)
                    .tools(documentGenerationTools);
            if (isAdmin) {
                // WHY: Admin tools (custom fields, policy rules, etc.) are only available to admins.
                // Non-admin users never see these tool definitions.
                log.info("Registering admin tools for isAdmin={}, isSystemAdmin={}", isAdmin, isCurrentUserSystemAdmin());
                spec = spec.tools(adminTools).tools(policyManagementTools).tools(systemAdminTools);
            }
            return spec.call().chatResponse();
        } catch (Exception e) {
            log.error("Error calling {} model {} in auto mode: {}", provider, modelId, e.getMessage(), e);
            // Wrap exception to provide context to fallback handler
            throw new RuntimeException("Model call failed for " + provider + "/" + modelId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Call LLM in confirm mode: LLM returns tool definitions and proposed invocations without executing.
     *
     * <p>WHY: Confirm mode uses MethodToolCallbackProvider with internalToolExecutionEnabled(false).
     * This tells the LLM "here are available tools, suggest how to use them, but I'll execute them."
     * The LLM returns tool calls in its response, which we serialize and ask the user to confirm.
     *
     * <p>Tool callbacks are built manually (not via ChatClient fluent API) because we need to
     * set internalToolExecutionEnabled(false) on ChatOptions, which is not available via the fluent API.
     *
     * @param messages the conversation history
     * @param provider the AI provider (anthropic, openai, google)
     * @param modelId the specific model ID for this provider
     * @param isAdmin whether current user is admin (enables admin-only tools)
     * @return ChatResponse with tool call proposals (not yet executed)
     */
    private ChatResponse callInConfirmMode(List<Message> messages, String provider, String modelId, boolean isAdmin) {
        // WHY: Build tool callbacks manually. Each tool object (CrmTools, WebSearchTools, etc.)
        // is scanned for @Tool methods, which become available to the LLM.
        List<ToolCallback> callbacks = new ArrayList<>(List.of(
                MethodToolCallbackProvider.builder().toolObjects(crmTools).build().getToolCallbacks()));
        callbacks.addAll(List.of(
                MethodToolCallbackProvider.builder().toolObjects(webSearchTools).build().getToolCallbacks()));
        callbacks.addAll(List.of(
                MethodToolCallbackProvider.builder().toolObjects(documentGenerationTools).build().getToolCallbacks()));
        if (isAdmin) {
            callbacks.addAll(List.of(
                    MethodToolCallbackProvider.builder().toolObjects(adminTools).build().getToolCallbacks()));
            callbacks.addAll(List.of(
                    MethodToolCallbackProvider.builder().toolObjects(policyManagementTools).build().getToolCallbacks()));
            callbacks.addAll(List.of(
                    MethodToolCallbackProvider.builder().toolObjects(systemAdminTools).build().getToolCallbacks()));
        }
        ChatOptions options = buildOptionsWithTools(provider, modelId, callbacks);
        log.info("Calling {} model {} with {} tool callbacks", provider, modelId, callbacks.size());
        Prompt prompt = new Prompt(messages, options);
        ChatResponse response = chatModelRegistry.getModel(provider).call(prompt);
        log.info("Response from {} has {} generations", provider, response.getResults().size());
        return response;
    }

    private ChatOptions buildOptions(String provider, String modelId) {
        return switch (provider) {
            case "openai" -> OpenAiChatOptions.builder().model(modelId).maxTokens(4096).build();
            case "google" -> GoogleGenAiChatOptions.builder().model(modelId).maxOutputTokens(4096).build();
            case "ollama" -> OllamaChatOptions.builder().model(modelId).numCtx(16384).build();
            default -> AnthropicChatOptions.builder().model(modelId).maxTokens(4096).build();
        };
    }

    private ChatOptions buildOptionsWithTools(String provider, String modelId, List<ToolCallback> callbacks) {
        return switch (provider) {
            case "openai" -> OpenAiChatOptions.builder().model(modelId).maxTokens(4096)
                    .toolCallbacks(callbacks).internalToolExecutionEnabled(false).build();
            case "google" -> GoogleGenAiChatOptions.builder().model(modelId).maxOutputTokens(4096)
                    .toolCallbacks(callbacks).internalToolExecutionEnabled(false).build();
            case "ollama" -> OllamaChatOptions.builder().model(modelId).numCtx(16384)
                    .toolCallbacks(callbacks).internalToolExecutionEnabled(false).build();
            default -> AnthropicChatOptions.builder().model(modelId).maxTokens(4096)
                    .toolCallbacks(callbacks).internalToolExecutionEnabled(false).build();
        };
    }

    private ChatResponseDTO executeStoredActions(SendMessageRequest request, Long tenantId, Long userId,
                                                  String provider, String modelId,
                                                  boolean isAdmin, String sessionId, long startTime) {
        // Find last assistant message with tool calls for this session
        List<ChatMessage> lastAssistant = chatMessageRepository.findLastAssistantMessageBySession(
                tenantId, userId, sessionId, PageRequest.of(0, 1));
        if (lastAssistant.isEmpty() || lastAssistant.get(0).getToolCalls() == null) {
            return ChatResponseDTO.builder()
                    .response("No pending actions found for this session.")
                    .sessionId(sessionId)
                    .quotaStatus(tokenQuotaService.getQuotaStatus(tenantId))
                    .build();
        }

        JsonNode storedToolCalls = lastAssistant.get(0).getToolCalls();
        List<Message> messages = buildConversationMessages(tenantId, userId, sessionId, request, isAdmin);

        // Execute each stored tool call and collect results
        List<Message> toolResultMessages = new ArrayList<>(messages);
        // Re-include the assistant message that has tool calls
        toolResultMessages.add(AssistantMessage.builder()
                .content(lastAssistant.get(0).getContent())
                .toolCalls(parseStoredToolCalls(storedToolCalls))
                .build());

        // Execute tools and build tool result messages
        List<org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
        for (JsonNode toolCall : storedToolCalls) {
            String toolCallId = toolCall.path("id").asText();
            String toolName = toolCall.path("name").asText();
            String arguments = toolCall.path("arguments").asText("{}");
            String result = executeToolByName(toolName, arguments, isAdmin);
            toolResponses.add(new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                    toolCallId, toolName, result));
        }
        if (!toolResponses.isEmpty()) {
            toolResultMessages.add(org.springframework.ai.chat.messages.ToolResponseMessage.builder()
                    .responses(toolResponses).build());
        }

        // Call model again with tool results to get final response, with fallback support
        ChatResponse finalResponse = callInAutoModeWithFallback(toolResultMessages, provider, modelId, isAdmin);

        int inputTokens = 0, outputTokens = 0;
        if (finalResponse.getMetadata() != null && finalResponse.getMetadata().getUsage() != null) {
            inputTokens = (int) finalResponse.getMetadata().getUsage().getPromptTokens();
            outputTokens = (int) finalResponse.getMetadata().getUsage().getCompletionTokens();
        }
        tokenQuotaService.recordUsage(tenantId, userId, inputTokens, outputTokens, modelId, sessionId, "tool_execution");

        long processingTimeMs = System.currentTimeMillis() - startTime;
        saveAssistantMessage(tenantId, userId, finalResponse, sessionId, modelId, inputTokens, outputTokens, null, processingTimeMs);
        return buildResponse(finalResponse, null, sessionId, modelId, processingTimeMs,
                buildHistory(tenantId, userId, sessionId), tokenQuotaService.getQuotaStatus(tenantId),
                crmTools.popPendingNavigation());
    }

    private String executeToolByName(String toolName, String argumentsJson, boolean isAdmin) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, Map.class);
            return switch (toolName) {
                case "getCrmSummary" -> crmTools.getCrmSummary();
                case "createCustomer" -> crmTools.createCustomer(str(args, "name"), str(args, "status"), str(args, "customFieldsJson"));
                case "bulkCreateCustomers" -> crmTools.bulkCreateCustomers(str(args, "recordsJson"));
                case "bulkCreateContacts" -> crmTools.bulkCreateContacts(str(args, "recordsJson"));
                case "bulkCreateOpportunities" -> crmTools.bulkCreateOpportunities(str(args, "recordsJson"));
                case "bulkCreateActivities" -> crmTools.bulkCreateActivities(str(args, "recordsJson"));
                case "bulkCreateCustomRecords" -> crmTools.bulkCreateCustomRecords(str(args, "recordsJson"));
                case "updateCustomer" -> crmTools.updateCustomer(longVal(args, "customerId"), str(args, "name"), str(args, "status"), str(args, "customFieldsJson"));
                case "updateContact" -> crmTools.updateContact(longVal(args, "contactId"), str(args, "name"),
                        str(args, "email"), str(args, "phone"), str(args, "title"), str(args, "customFieldsJson"));
                case "createContact" -> crmTools.createContact(str(args, "name"), longVal(args, "customerId"),
                        str(args, "email"), str(args, "phone"), str(args, "title"), str(args, "customFieldsJson"));
                case "createOpportunity" -> crmTools.createOpportunity(str(args, "name"), longVal(args, "customerId"),
                        str(args, "stage"), str(args, "value"), str(args, "closeDate"), str(args, "customFieldsJson"));
                case "updateOpportunity" -> crmTools.updateOpportunity(longVal(args, "opportunityId"),
                        str(args, "name"), str(args, "stage"), str(args, "value"), str(args, "customFieldsJson"));
                case "advancedOpportunitySearch" -> crmTools.advancedOpportunitySearch(str(args, "query"), str(args, "stage"), longVal(args, "customerId"), str(args, "sortBy"), str(args, "sortOrder"));
                case "analyzeOpportunities" -> crmTools.analyzeOpportunities(str(args, "analysis"), str(args, "stage"), longVal(args, "customerId"), str(args, "query"));
                case "createActivity" -> crmTools.createActivity(str(args, "subject"), str(args, "type"),
                        str(args, "relatedType"), longVal(args, "relatedId"), str(args, "description"),
                        str(args, "dueAt"), str(args, "status"), str(args, "customFieldsJson"));
                case "updateActivity" -> crmTools.updateActivity(longVal(args, "activityId"),
                        str(args, "subject"), str(args, "status"), str(args, "description"), str(args, "dueAt"), str(args, "customFieldsJson"));
                case "advancedCustomerSearch" -> crmTools.advancedCustomerSearch(str(args, "query"), str(args, "status"), str(args, "sortBy"), str(args, "sortOrder"));
                case "analyzeCustomers" -> crmTools.analyzeCustomers(str(args, "analysis"), str(args, "status"), str(args, "query"));
                case "advancedContactSearch" -> crmTools.advancedContactSearch(str(args, "query"), longVal(args, "customerId"), str(args, "sortBy"), str(args, "sortOrder"));
                case "analyzeContacts" -> crmTools.analyzeContacts(str(args, "analysis"), longVal(args, "customerId"), str(args, "status"), str(args, "query"));
                case "advancedActivitySearch" -> crmTools.advancedActivitySearch(str(args, "query"), str(args, "type"), str(args, "status"), str(args, "sortBy"), str(args, "sortOrder"));
                case "analyzeActivities" -> crmTools.analyzeActivities(str(args, "analysis"), str(args, "type"), str(args, "status"), str(args, "query"));
                case "advancedCustomRecordSearch" -> crmTools.advancedCustomRecordSearch(str(args, "query"), str(args, "status"), str(args, "sortBy"), str(args, "sortOrder"));
                case "analyzeCustomRecords" -> crmTools.analyzeCustomRecords(str(args, "analysis"), str(args, "status"), str(args, "query"));
                case "deleteCustomer" -> crmTools.deleteCustomer(longVal(args, "customerId"));
                case "deleteContact" -> crmTools.deleteContact(longVal(args, "contactId"));
                case "deleteOpportunity" -> crmTools.deleteOpportunity(longVal(args, "opportunityId"));
                case "deleteActivity" -> crmTools.deleteActivity(longVal(args, "activityId"));
                case "navigateTo" -> crmTools.navigateTo(str(args, "path"));
                case "applyFilters" -> crmTools.applyFilters(str(args, "entityType"), str(args, "filtersJson"));
                case "createCustomRecord" -> crmTools.createCustomRecord(str(args, "name"), str(args, "type"), str(args, "serialNumber"), str(args, "status"), longVal(args, "customerId"), str(args, "customFieldsJson"));
                case "updateCustomRecord" -> crmTools.updateCustomRecord(longVal(args, "customRecordId"), str(args, "name"), str(args, "type"), str(args, "status"), str(args, "serialNumber"));
                case "deleteCustomRecord" -> crmTools.deleteCustomRecord(longVal(args, "customRecordId"));
                case "listCustomFields" -> isAdmin ? adminTools.listCustomFields(str(args, "entityType")) : "Permission denied";
                case "bulkCreateCustomFields" -> isAdmin ? adminTools.bulkCreateCustomFields(str(args, "fieldsJson")) : "Permission denied";
                case "createCustomField" -> isAdmin ? adminTools.createCustomField(str(args, "entityType"),
                        str(args, "key"), str(args, "label"), str(args, "fieldType"), boolVal(args, "required"),
                        str(args, "options"), str(args, "milestones"), boolVal(args, "displayInTable")) : "Permission denied";
                case "updateCustomField" -> isAdmin ? adminTools.updateCustomField(longVal(args, "fieldId"),
                        str(args, "label"), boolVal(args, "required"), str(args, "options"), str(args, "milestones"), boolVal(args, "displayInTable")) : "Permission denied";
                case "deleteCustomField" -> isAdmin ? adminTools.deleteCustomField(longVal(args, "fieldId")) : "Permission denied";
                case "listCalculatedFields" -> isAdmin ? adminTools.listCalculatedFields(str(args, "entityType")) : "Permission denied";
                case "createCalculatedField" -> isAdmin ? adminTools.createCalculatedField(str(args, "entityType"),
                        str(args, "key"), str(args, "label"), str(args, "expression"), str(args, "returnType")) : "Permission denied";
                case "updateCalculatedField" -> isAdmin ? adminTools.updateCalculatedField(longVal(args, "fieldId"),
                        str(args, "label"), str(args, "expression"), boolVal(args, "enabled"), boolVal(args, "displayInTable")) : "Permission denied";
                case "deleteCalculatedField" -> isAdmin ? adminTools.deleteCalculatedField(longVal(args, "fieldId")) : "Permission denied";
                case "getTenantSettings" -> isAdmin ? adminTools.getTenantSettings() : "Permission denied";
                case "setLogoFromUrl" -> isAdmin ? adminTools.setLogoFromUrl(str(args, "imageUrl")) : "Permission denied";
                case "updateTenantSettings" -> isAdmin ? adminTools.updateTenantSettings(str(args, "primaryColor"), str(args, "language"), str(args, "orgBio")) : "Permission denied";
                case "updateEntityLabels" -> isAdmin ? adminTools.updateEntityLabels(str(args, "customerLabel"), str(args, "contactLabel"), str(args, "opportunityLabel"), str(args, "activityLabel"), str(args, "customRecordLabel"), str(args, "orderLabel"), str(args, "invoiceLabel")) : "Permission denied";
                case "listNotificationTemplates" -> isAdmin ? adminTools.listNotificationTemplates() : "Permission denied";
                case "createNotificationTemplate" -> isAdmin ? adminTools.createNotificationTemplate(str(args, "notificationType"), str(args, "name"), str(args, "subjectTemplate"), str(args, "bodyTemplate"), boolVal(args, "isActive")) : "Permission denied";
                case "generateOpportunityReport" -> documentGenerationTools.generateOpportunityReport(longVal(args, "opportunityId"), str(args, "title"), longVal(args, "templateId"));
                case "generateCrmReport" -> documentGenerationTools.generateCrmReport(str(args, "entityType"), str(args, "filtersJson"), str(args, "title"), longVal(args, "templateId"));
                case "generateOnePager" -> documentGenerationTools.generateOnePager(str(args, "entityType"), longVal(args, "entityId"), str(args, "title"), str(args, "styleJson"), longVal(args, "templateId"));
                case "generateSlideDeck" -> documentGenerationTools.generateSlideDeck(str(args, "entityType"), longVal(args, "entityId"), str(args, "title"), str(args, "styleJson"), longVal(args, "templateId"));
                case "generateCsvExport" -> documentGenerationTools.generateCsvExport(str(args, "entityType"), str(args, "filtersJson"), longVal(args, "templateId"));
                case "listDocumentTemplates" -> documentGenerationTools.listDocumentTemplates();
                case "createDocumentTemplate" -> isAdmin ? documentGenerationTools.createDocumentTemplate(
                        str(args, "name"), str(args, "templateType"), str(args, "description"), str(args, "styleJson")) : "Permission denied";
                case "updateDocumentTemplate" -> isAdmin ? documentGenerationTools.updateDocumentTemplate(
                        longVal(args, "templateId"), str(args, "name"), str(args, "description"),
                        str(args, "templateType"), str(args, "styleJson")) : "Permission denied";
                case "deleteDocumentTemplate" -> isAdmin ? documentGenerationTools.deleteDocumentTemplate(
                        longVal(args, "templateId")) : "Permission denied";
                case "searchWikipedia"      -> webSearchTools.searchWikipedia(str(args, "query"));
                case "searchWeb"            -> webSearchTools.searchWeb(str(args, "query"));
                case "searchOpenFDA"        -> webSearchTools.searchOpenFDA(str(args, "query"));
                case "searchClinicalTrials" -> webSearchTools.searchClinicalTrials(str(args, "query"));
                case "searchPubMed"         -> webSearchTools.searchPubMed(str(args, "query"));
                case "searchPubChem"        -> webSearchTools.searchPubChem(str(args, "query"));
                case "searchReddit"         -> webSearchTools.searchReddit(str(args, "query"));
                case "searchArXiv"          -> webSearchTools.searchArXiv(str(args, "query"));
                case "searchNews"           -> webSearchTools.searchNews(str(args, "query"));
                case "searchBrave"          -> webSearchTools.searchBrave(str(args, "query"));
                case "searchSerper"         -> webSearchTools.searchSerper(str(args, "query"));
                case "searchYouTube"        -> webSearchTools.searchYouTube(str(args, "query"));
                case "listPolicies" -> isAdmin ? policyManagementTools.listPolicies(str(args, "entityType")) : "Permission denied";
                case "createPolicy" -> isAdmin ? policyManagementTools.createPolicy(str(args, "entityType"), str(args, "operations"),
                        str(args, "name"), str(args, "expression"), str(args, "severity"), str(args, "description")) : "Permission denied";
                case "updatePolicy" -> isAdmin ? policyManagementTools.updatePolicy(longVal(args, "policyId"),
                        str(args, "name"), str(args, "operations"), str(args, "expression"), str(args, "severity"),
                        str(args, "description"), boolVal(args, "enabled")) : "Permission denied";
                case "deletePolicy" -> isAdmin ? policyManagementTools.deletePolicy(longVal(args, "policyId")) : "Permission denied";
                case "explainPolicyTrigger" -> isAdmin ? policyManagementTools.explainPolicyTrigger(str(args, "entityType"),
                        str(args, "policyName"), str(args, "entityDataJson")) : "Permission denied";
                case "createOrder" -> crmTools.createOrder(longVal(args, "customerId"), str(args, "name"),
                        str(args, "status"), str(args, "currency"), str(args, "totalAmount"), str(args, "customFieldsJson"));
                case "updateOrder" -> crmTools.updateOrder(longVal(args, "orderId"), str(args, "status"),
                        str(args, "totalAmount"), str(args, "customFieldsJson"));
                case "getOrder" -> crmTools.getOrder(longVal(args, "orderId"));
                case "searchOrders" -> crmTools.searchOrders(str(args, "query"), intVal(args, "page"), intVal(args, "size"));
                case "createInvoice" -> crmTools.createInvoice(longVal(args, "customerId"), str(args, "status"),
                        str(args, "currency"), str(args, "totalAmount"), str(args, "paymentTerms"), str(args, "customFieldsJson"));
                case "updateInvoice" -> crmTools.updateInvoice(longVal(args, "invoiceId"), str(args, "status"),
                        str(args, "totalAmount"), str(args, "paymentTerms"), str(args, "customFieldsJson"));
                case "getInvoice" -> crmTools.getInvoice(longVal(args, "invoiceId"));
                case "searchInvoices" -> crmTools.searchInvoices(str(args, "query"), intVal(args, "page"), intVal(args, "size"));
                case "backupTenant" -> isCurrentUserSystemAdmin()
                        ? systemAdminTools.backupTenant(str(args, "tenantName"))
                        : "Permission denied: SYSTEM_ADMIN role required";
                case "createTenant" -> isCurrentUserSystemAdmin()
                        ? systemAdminTools.createTenant(str(args, "tenantName"))
                        : "Permission denied: SYSTEM_ADMIN role required";
                case "restoreTenantFromBackup" -> isCurrentUserSystemAdmin()
                        ? systemAdminTools.restoreTenantFromBackup(str(args, "backupJson"), str(args, "tenantNameOverride"))
                        : "Permission denied: SYSTEM_ADMIN role required";
                case "cloneTenant" -> isCurrentUserSystemAdmin()
                        ? systemAdminTools.cloneTenant(str(args, "sourceTenantName"), str(args, "newTenantName"))
                        : "Permission denied: SYSTEM_ADMIN role required";
                default -> "Unknown tool: " + toolName;
            };
        } catch (Exception e) {
            log.error("Tool execution failed for {}: {}", toolName, e.getMessage());
            return "Error executing " + toolName + ": " + e.getMessage();
        }
    }

    private List<Message> buildConversationMessages(Long tenantId, Long userId, String sessionId,
                                                     SendMessageRequest request, boolean isAdmin) {
        // System prompt
        String tenantName = getTenantName(tenantId);
        String orgBio = tenantAdminService.getOrgBio(tenantId);
        String systemPrompt = buildSystemPrompt(tenantName, orgBio, isAdmin, isCurrentUserSystemAdmin(), request.getConfirmMode(), request.getPageContext());
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Prior conversation (last HISTORY_WINDOW messages)
        List<ChatMessage> history = chatMessageRepository.findRecentBySession(
                tenantId, userId, sessionId, PageRequest.of(0, HISTORY_WINDOW));
        // History comes back newest-first; reverse for chronological order
        Collections.reverse(history);
        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole()) && msg.getContent() != null
                    && !msg.getContent().isBlank() && msg.getToolCalls() == null) {
                // Skip pending tool-call messages — executeStoredActions re-adds them with
                // the actual tool call payloads. Including them here would produce a duplicate
                // assistant turn that makes the LLM error when tool results follow.
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // Current user message
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            // Check for attachment
            if (request.getAttachmentBase64() != null && !request.getAttachmentBase64().isBlank()) {
                messages.add(buildUserMessageWithAttachment(request));
            } else {
                String userText = request.getMessage();
                if (isWorkspaceSetupPrompt(userText)) {
                    userText = "[WORKSPACE SETUP — AUTO MODE — EXECUTE ALL STEPS WITHOUT STOPPING]\n"
                            + "You are in auto mode. Call tools continuously until the entire setup is complete. "
                            + "Do NOT stop, do NOT ask for confirmation, do NOT describe what you will do. "
                            + "Execute every step in sequence:\n"
                            + "1. updateTenantSettings (colors, bio, website, language)\n"
                            + "2. setLogoFromUrl (if logo URL provided)\n"
                            + "3. createCustomField — one call per Customer custom field\n"
                            + "4. createCustomField — one call per Opportunity/Contact/Activity custom field\n"
                            + "5. createCalculatedField — one call per calculated field\n"
                            + "6. createPolicy — one call per business policy\n"
                            + "7. bulkCreateCustomers — all customers in one call\n"
                            + "8. bulkCreateContacts — all contacts in one call (use customerName)\n"
                            + "9. bulkCreateOpportunities — all opportunities in one call (use customerName)\n"
                            + "10. bulkCreateActivities — all activities in one call\n"
                            + "11. createCustomRecord — one call per customRecord\n"
                            + "12. createOrder / createInvoice — one call per order/invoice\n"
                            + "13. createDocumentTemplate — one call per template\n"
                            + "14. createNotificationTemplate — one call per notification template\n"
                            + "After completing ALL steps, output a brief summary of what was created.\n"
                            + "The spec follows:\n\n" + userText;
                }
                messages.add(new UserMessage(userText));
            }
        } else if (request.getAttachmentBase64() != null && !request.getAttachmentBase64().isBlank()) {
            // Attachment without text message
            messages.add(buildUserMessageWithAttachment(request));
        }
        return messages;
    }

    /**
     * Builds a UserMessage with optional file attachment (image, PDF, or text).
     * For vision models (Anthropic Claude, OpenAI GPT-4V), encodes images/PDFs as base64 data URLs.
     * For text/CSV files, appends content directly.
     */
    private UserMessage buildUserMessageWithAttachment(SendMessageRequest request) {
        String text = (request.getMessage() != null && !request.getMessage().isBlank())
                ? request.getMessage()
                : "Please analyze this file and suggest CRM entries to create.";

        String mimeType = request.getAttachmentMimeType();
        String base64 = request.getAttachmentBase64();

        if (mimeType == null || base64 == null) {
            return new UserMessage(text);
        }

        try {
            // For images and PDFs, include as markdown image/link with base64 data URL
            // This works with vision models like Claude 3, GPT-4V, Gemini
            if (mimeType.startsWith("image/")) {
                String dataUrl = "data:" + mimeType + ";base64," + base64;
                String combinedText = text + "\n\n[Image attachment: " + request.getAttachmentFileName() + "]\n" + dataUrl;
                return new UserMessage(combinedText);
            } else if ("application/pdf".equals(mimeType)) {
                // For PDFs, include the base64 data with clear markers
                String combinedText = text + "\n\n[PDF attachment: " + request.getAttachmentFileName() + "]\n"
                        + "Base64 PDF content:\n" + base64;
                return new UserMessage(combinedText);
            } else {
                // For CSV or other text formats, append content as text
                byte[] bytes = Base64.getDecoder().decode(base64);
                String fileContent = new String(bytes);
                String combinedText = text + "\n\n**File content (" + request.getAttachmentFileName() + "):**\n" + fileContent;
                return new UserMessage(combinedText);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to decode attachment base64 for file {}: {}", request.getAttachmentFileName(), e.getMessage());
            return new UserMessage(text + " (Note: file attachment could not be processed)");
        }
    }

    private String buildSystemPrompt(String tenantName, String orgBio, boolean isAdmin, boolean isSystemAdmin, String confirmMode, String pageContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are BOCRM Assistant, an AI assistant for the ").append(tenantName).append(" CRM.\n");
        sb.append("Today's date is ").append(LocalDate.now()).append(".\n");
        if (orgBio != null && !orgBio.isBlank()) {
            sb.append("Organization context: ").append(orgBio).append("\n");
        }
        if (pageContext != null && !pageContext.isBlank()) {
            sb.append("The user is currently viewing: ").append(pageContext).append(".\n");

            // Report Builder context: the user is refining a live preview, not editing tenant-wide settings.
            if ("report_builder".equalsIgnoreCase(pageContext) || "Report Builder".equalsIgnoreCase(pageContext)) {
                sb.append("\n=== REPORT BUILDER MODE (CRITICAL) ===\n");
                sb.append("The user is configuring a LIVE REPORT PREVIEW. They are NOT editing tenant-wide settings.\n");
                sb.append("When the user asks to change the logo, background, colors, layout, fields, or any style aspect, ")
                        .append("you MUST NOT call tenant-modification tools (setLogoFromUrl, updateTenantSettings, createCustomField, etc.). ")
                        .append("Instead, respond with ONLY a markdown JSON code block in this exact format:\n");
                sb.append("```json\n{\"reportBuilderUpdate\": {\"<key>\": <value>, ...}}\n```\n");
                sb.append("Supported keys (all optional, send only the ones the user is changing):\n");
                sb.append("  Layout & entity:\n");
                sb.append("    - layout: \"dark\" | \"light\" | \"corporate\" | \"minimal\" (resets colors to the preset palette)\n");
                sb.append("    - reportType: \"slide_deck\" | \"one_pager\"\n");
                sb.append("    - entityType: \"Customer\" | \"Contact\" | \"Opportunity\" | \"Activity\" | \"CustomRecord\"\n");
                sb.append("    - title: string\n");
                sb.append("  Colors (all hex strings like \"#3355cc\"):\n");
                sb.append("    - accentColor: primary accent (buttons, borders, badges)\n");
                sb.append("    - backgroundColor: outer page background (around the slide panel)\n");
                sb.append("    - slideBackground: the slide panel itself — USE THIS for \"change the background\" requests\n");
                sb.append("    - textColor: main body text color\n");
                sb.append("    - h1Color: title heading color\n");
                sb.append("    - h2Color: section heading color\n");
                sb.append("  Typography & layout:\n");
                sb.append("    - fontFamily: CSS font-family string, e.g. \"Georgia, serif\"\n");
                sb.append("    - customCss: raw CSS string appended after the base stylesheet. Use this for creative tweaks like gradients, animations, or custom grids. Available CSS variables: --slide-bg, --slide-accent, --slide-text, --slide-h1, --slide-h2, --body-bg, --font.\n");
                sb.append("  Logo & fields:\n");
                sb.append("    - logoPlacement: \"none\" | \"title\" | \"header\" | \"footer\"\n");
                sb.append("    - includeFields: array of field names, e.g. [\"stage\",\"value\"]\n");
                sb.append("    - excludeFields: array of field names\n");
                sb.append("Examples:\n");
                sb.append("  User: \"switch to light theme\" → ```json\\n{\"reportBuilderUpdate\": {\"layout\": \"light\"}}\\n```\n");
                sb.append("  User: \"change the background to pink\" → ```json\\n{\"reportBuilderUpdate\": {\"slideBackground\": \"#ffc0cb\", \"backgroundColor\": \"#ffe6eb\"}}\\n```\n");
                sb.append("  User: \"make the headings red\" → ```json\\n{\"reportBuilderUpdate\": {\"h1Color\": \"#cc0000\", \"h2Color\": \"#cc0000\"}}\\n```\n");
                sb.append("  User: \"use a serif font\" → ```json\\n{\"reportBuilderUpdate\": {\"fontFamily\": \"Georgia, serif\"}}\\n```\n");
                sb.append("  User: \"add a gradient background\" → ```json\\n{\"reportBuilderUpdate\": {\"customCss\": \"section { background: linear-gradient(135deg, #1a1a4e, #533483); }\"}}\\n```\n");
                sb.append("  User: \"put the logo in the footer\" → ```json\\n{\"reportBuilderUpdate\": {\"logoPlacement\": \"footer\"}}\\n```\n");
                sb.append("  User: \"remove the notes field\" → ```json\\n{\"reportBuilderUpdate\": {\"excludeFields\": [\"notes\"]}}\\n```\n");
                sb.append("IMPORTANT: \"background\" in user requests almost always means slideBackground (the slide panel color). Set both slideBackground AND backgroundColor for a consistent look unless the user is specific.\n");
                sb.append("The frontend applies the update and regenerates the preview automatically. Add a short confirmation sentence AFTER the code block if helpful, but keep it brief.\n");
                sb.append("If the user asks to SAVE the report (persist to Documents), THEN call generateSlideDeck or generateOnePager with the config.\n");
                sb.append("If the user asks to save the style as a reusable template, call createDocumentTemplate.\n");
                sb.append("For questions about data (\"how many customers?\", \"which opportunities are closing?\") use the normal read tools.\n");
                sb.append("=== END REPORT BUILDER MODE ===\n\n");
            }

            boolean isListPage = pageContext.contains("list");
            if (isListPage) {
                String entityType = "Customers";
                String toolCall = "advancedCustomerSearch(\"\", null, \"createdAt\", \"desc\")";
                if (pageContext.contains("Contacts")) {
                    entityType = "Contacts";
                    toolCall = "advancedContactSearch(\"\", null, \"createdAt\", \"desc\")";
                } else if (pageContext.contains("Opportunities")) {
                    entityType = "Opportunities";
                    toolCall = "advancedOpportunitySearch(\"\", null, null, \"createdAt\", \"desc\")";
                } else if (pageContext.contains("Activities")) {
                    entityType = "Activities";
                    toolCall = "advancedActivitySearch(\"\", null, null, \"createdAt\", \"desc\")";
                } else if (pageContext.contains("CustomRecords")) {
                    entityType = "CustomRecords";
                    toolCall = "advancedCustomRecordSearch(\"\", null, \"createdAt\", \"desc\")";
                }
                sb.append("CRITICAL: The user is on the ").append(entityType).append(" list page. ")
                        .append("YOU MUST ALWAYS START by calling ").append(toolCall).append(" (with empty query) to see the current list data. ")
                        .append("Only AFTER seeing the data can you answer questions. Never claim there are no ").append(entityType.toLowerCase())
                        .append(" if you haven't fetched the list yet. This is non-negotiable.\n");
            }
        }
        sb.append("You help users manage their CRM data conversationally.\n");
        sb.append("Document generation tools (available to all users): generateOpportunityReport, generateCrmReport, generateOnePager, generateSlideDeck, generateCsvExport.\n");
        sb.append("Use these tools to produce Markdown reports, one-pagers, HTML slide decks, and CSV exports that are saved to the Documents page.\n");
        sb.append("Reports include custom fields and calculated fields stored on each record automatically.\n");
        sb.append("TEMPLATE WORKFLOW: Before generating any document, call listDocumentTemplates() to see if saved templates exist.\n");
        sb.append("  - If templates exist and the user has not specified one, ask which template to use (show the list).\n");
        sb.append("  - If the user specifies a template by name or id, pass its id as the templateId parameter.\n");
        sb.append("  - If no templates exist or the user prefers custom styling, proceed without a templateId.\n");
        sb.append("  - Templates provide a base styleJson; you can still layer additional overrides on top via the styleJson parameter.\n");
        sb.append("generateOnePager and generateSlideDeck accept an optional styleJson parameter to customize appearance and content:\n");
        sb.append("  - layout: \"dark\" (default, deep blue), \"light\" (white/blue), \"corporate\" (navy/gray), \"minimal\" (white/black)\n");
        sb.append("  - accentColor, backgroundColor, slideBackground, textColor, h1Color, h2Color: hex color overrides\n");
        sb.append("  - fontFamily: CSS font-family string (e.g. \"Georgia, serif\")\n");
        sb.append("  - includeFields: array of field names to show (e.g. [\"stage\",\"value\",\"closeDate\"])\n");
        sb.append("  - excludeFields: array of field names to hide\n");
        sb.append("  - For one-pagers: includeCustomFields (boolean, default true), layout (\"compact\"|\"detailed\")\n");
        sb.append("When a user asks to change colors, layout, or which fields to include in a report or deck, pass the appropriate styleJson. Example: {\"layout\":\"corporate\",\"accentColor\":\"#2e6da4\",\"includeFields\":[\"stage\",\"value\",\"closeDate\"]}\n");
        sb.append("\n**BULK CREATE tools — use these whenever creating 2 or more records of the same type:**\n");
        sb.append("bulkCreateCustomers(recordsJson), bulkCreateContacts(recordsJson), bulkCreateOpportunities(recordsJson), bulkCreateActivities(recordsJson), bulkCreateCustomRecords(recordsJson)\n");
        sb.append("  - recordsJson is a JSON array string; each element has the same scalar fields as the single-record create tool.\n");
        sb.append("  - Custom fields: use a nested 'customFields' object directly inside each record element — DO NOT double-encode as a string.\n");
        sb.append("    Correct:   {\"name\":\"Acme\",\"customFields\":{\"industry\":\"Cloud\",\"annual_revenue\":125000000}}\n");
        sb.append("    Incorrect: {\"name\":\"Acme\",\"customFieldsJson\":\"{\\\"industry\\\":\\\"Cloud\\\"}\"}\n");
        sb.append("  - Workflow custom fields inside customFields: use {\"sales_stage\":{\"currentIndex\":2}} to set the active milestone.\n");
        sb.append("  - A single bulk call replaces N individual create calls — dramatically reduces round-trips and token usage.\n");
        sb.append("  - Errors on individual rows are reported per-row; successful rows are still created.\n");
        sb.append("  - bulkCreateContacts and bulkCreateOpportunities accept 'customerName' (string) in place of 'customerId' — the tool resolves the id by name automatically. Use customerName after a bulkCreateCustomers call when you know names but not ids yet.\n");
        sb.append("  - **SIZE LIMITS** (to avoid timeouts on very large operations):\n");
        sb.append("    - Recommended: Up to 100 records per bulk call (fastest, lowest timeout risk)\n");
        sb.append("    - Warning zone: 100–200 records (may take 30–60 seconds; acceptable but slower)\n");
        sb.append("    - Absolute maximum: 1000 records per call (will be rejected if exceeded)\n");
        sb.append("    - For >200 records: ALWAYS split into chunks of ~100. Example: 500 customers → 5 calls of 100 each.\n");
        sb.append("  - Recommended sequence for full tenant setup: (1) bulkCreateCustomers → (2) bulkCreateContacts with customerName → (3) bulkCreateOpportunities with customerName → (4) bulkCreateActivities.\n");
        sb.append("  - IMPORTANT: Prefer bulk tools over looping createCustomer/createContact/etc. whenever the user provides a list. For very large lists (>200), proactively chunk them and inform the user of the expected duration.\n");
        sb.append("CustomRecord tools (available to all users): advancedCustomRecordSearch, createCustomRecord, updateCustomRecord, deleteCustomRecord.\n");
        sb.append("Order tools (available to all users): createOrder, updateOrder, getOrder, searchOrders.\n");
        sb.append("  - createOrder: Create a new order for a customer (customerId required, name optional).\n");
        sb.append("  - updateOrder: Update an existing order (orderId required, other fields optional).\n");
        sb.append("  - getOrder: Retrieve order details by ID.\n");
        sb.append("  - searchOrders: Search orders by status or payment terms.\n");
        sb.append("Invoice tools (available to all users): createInvoice, updateInvoice, getInvoice, searchInvoices.\n");
        sb.append("  - createInvoice: Create a new invoice for a customer (customerId required).\n");
        sb.append("  - updateInvoice: Update an existing invoice (invoiceId required, other fields optional).\n");
        sb.append("  - getInvoice: Retrieve invoice details by ID.\n");
        sb.append("  - searchInvoices: Search invoices by status or payment terms.\n");
        sb.append("\n**MANDATORY custom field search workflow (when user asks to filter by custom fields):**\n");
        sb.append("When a user asks to filter records by custom field values (e.g., \"show opportunities where Service Type = Tutoring\"):\n");
        sb.append("YOU MUST execute this exact workflow for ANY entity type. Do NOT claim you have filtered anything without calling tools:\n");
        sb.append("1. Call getCustomFieldSchema(entityType) to discover custom field names, types, and possible values.\n");
        sb.append("2. Call the appropriate advanced*Search tool with query=<filter_value> to search JSONB custom fields:\n");
        sb.append("   - CUSTOMERS: advancedCustomerSearch(query=<filter_value>, ...)\n");
        sb.append("   - CONTACTS: advancedContactSearch(query=<filter_value>, ...)\n");
        sb.append("   - OPPORTUNITIES: advancedOpportunitySearch(query=<filter_value>, ...)\n");
        sb.append("   - ACTIVITIES: advancedActivitySearch(query=<filter_value>, ...)\n");
        sb.append("   - CUSTOM RECORDS: advancedCustomRecordSearch(query=<filter_value>, ...)\n");
        sb.append("   The query parameter searches across ALL text content AND custom field JSONB, dramatically narrowing the result set.\n");
        sb.append("3. Display the returned records with their full details including the relevant custom field values.\n");
        sb.append("4. CRITICAL: Only tell the user what you found AFTER executing the search. Never claim filtering is complete without showing the results from a tool call.\n");
        sb.append("\nExamples:\n");
        sb.append("  Opportunities: \"show opportunities where priority=High\"\n");
        sb.append("    → getCustomFieldSchema(\"Opportunity\") → advancedOpportunitySearch(query=\"High\", ...) → display [Opp 1: priority=High, value=$50K]\n");
        sb.append("  Customers: \"show customers where industry=Technology\"\n");
        sb.append("    → getCustomFieldSchema(\"Customer\") → advancedCustomerSearch(query=\"Technology\", ...) → display [Cust 1: industry=Technology]\n");
        sb.append("  Contacts: \"show contacts where department=Sales\"\n");
        sb.append("    → getCustomFieldSchema(\"Contact\") → advancedContactSearch(query=\"Sales\", ...) → display [Contact 1: department=Sales]\n");
        sb.append("  Activities: \"show activities where priority=Urgent\"\n");
        sb.append("    → getCustomFieldSchema(\"Activity\") → advancedActivitySearch(query=\"Urgent\", ...) → display [Activity 1: priority=Urgent]\n");
        sb.append("  CustomRecords: \"show customRecords where location=Warehouse\"\n");
        sb.append("    → getCustomFieldSchema(\"CustomRecord\") → advancedCustomRecordSearch(query=\"Warehouse\", ...) → display [CustomRecord 1: location=Warehouse]\n");
        sb.append("IMPORTANT: Do NOT say \"I've filtered X\" for any entity unless you have actually called the corresponding advanced*Search tool and displayed results.\n");
        sb.append("\nAdvanced analytics (available to all users) — use these to provide intelligent insights:\n");
        sb.append("For optional numeric IDs (e.g., customerId), pass null or omit the field when unknown. Never pass 0 as a placeholder.\n");
        sb.append("CUSTOMERS: advancedCustomerSearch(query, status, sortBy, sortOrder) | analyzeCustomers(analysis, status, query)\n");
        sb.append("  - Analyze types: total_count, by_status, recent, status_breakdown\n");
        sb.append("CONTACTS: advancedContactSearch(query, customerId, sortBy, sortOrder) | analyzeContacts(analysis, customerId, status, query)\n");
        sb.append("  - Analyze types: total_count, by_status, by_customer, recent, status_breakdown\n");
        sb.append("OPPORTUNITIES: advancedOpportunitySearch(query, stage, customerId, sortBy, sortOrder) | analyzeOpportunities(analysis, stage, customerId, query)\n");
        sb.append("  - Analyze types: highest_value, lowest_value, total_value, average_value, by_stage, closing_soon\n");
        sb.append("ACTIVITIES: advancedActivitySearch(query, type, status, sortBy, sortOrder) | analyzeActivities(analysis, type, status, query)\n");
        sb.append("  - Analyze types: total_count, by_type, by_status, overdue, upcoming, type_breakdown\n");
        sb.append("CUSTOM RECORDS: advancedCustomRecordSearch(query, status, sortBy, sortOrder) | analyzeCustomRecords(analysis, status, query)\n");
        sb.append("  - Analyze types: total_count, by_type, by_status, type_breakdown, status_breakdown\n");
        if (isAdmin) {
            sb.append("The user is a tenant ADMIN. You have access to CRM tools AND admin tools:\n");
            sb.append("- Custom fields: listCustomFields, bulkCreateCustomFields (PREFERRED for creating multiple fields), createCustomField (single field), updateCustomField, deleteCustomField\n");
            sb.append("  IMPORTANT: When creating multiple custom fields, ALWAYS use bulkCreateCustomFields with a single JSON array call instead of repeated createCustomField calls. This saves tokens and is much faster.\n");
            sb.append("  fieldType must be one of: text, number, select, date, boolean, textarea, multiselect, url, email, phone, currency, percentage, richtext, document, document_multi, custom_record, custom_record_multi, workflow.\n");
            sb.append("  'document' = single linked document, 'document_multi' = multiple linked documents, 'custom_record' = single linked custom record, 'custom_record_multi' = multiple linked custom records.\n");
            sb.append("  'workflow' = milestone tracker with sequential steps. Config: {\"milestones\": [\"Step1\", \"Step2\", \"Step3\"]}\n");
            sb.append("  Workflow field values are stored as {\"currentIndex\": number | null}, where currentIndex is the active milestone (0-based) or null if not started.\n");
            sb.append("- Calculated fields: listCalculatedFields, createCalculatedField, updateCalculatedField, deleteCalculatedField\n");
            sb.append("  Calculated fields use SpEL expressions evaluated at view time (e.g. 'value * probability / 100', 'name.toUpperCase()').\n");
            sb.append("  returnType must be one of: text, number, boolean, date.\n");
            sb.append("  Calculated fields CAN reference workflow fields: customField_workflowKey.currentIndex to get the active milestone index.\n");
            sb.append("- Business policy rules: listPolicies, createPolicy, updatePolicy, deletePolicy, explainPolicyTrigger\n");
            sb.append("  Policies enforce business logic using Rego expressions (Open Policy Agent). entityType must be one of: Customer, Contact, Opportunity, Activity, TenantDocument.\n");
            sb.append("  operation must be: CREATE, UPDATE, or DELETE. severity must be: DENY (hard block, returns HTTP 422) or WARN (user confirms).\n");
            sb.append("  Expressions are Rego condition bodies — write just the conditions, not a full Rego document.\n");
            sb.append("  Use 'input.entity.fieldName' for new/merged values, 'input.previous.fieldName' for old values on UPDATE, 'input.operation' for the operation string.\n");
            sb.append("  **⚠️ CRITICAL REGO SYNTAX RULES (read carefully):**\n");
            sb.append("  - Do NOT use && or || operators. Rego will reject them with 'unexpected and token' error.\n");
            sb.append("  - WRONG: input.entity.stage in [\"negotiation\",\"closed\"] && input.entity.value > 100000\n");
            sb.append("  - CORRECT: put conditions on separate lines (each line is implicitly ANDed):\n");
            sb.append("      input.entity.stage in [\"negotiation\",\"closed\"]\n");
            sb.append("      input.entity.value > 100000\n");
            sb.append("  - For OR logic, create two separate rules instead of using ||.\n");
            sb.append("  - Use brackets for list membership: 'in [\",\",\"]' not 'in (...)'\n");
            sb.append("  - Single condition example:\n");
            sb.append("      input.entity.email == \"blocked@spam.com\"\n");
            sb.append("  - Multi-line condition example:\n");
            sb.append("      input.entity.status == \"locked\"\n");
            sb.append("      input.entity.value > 100000\n");
            sb.append("  - Field comparisons: use ==, !=, >, <, >=, <= operators. Strings in double quotes.\n");
            sb.append("- Tenant settings: getTenantSettings, updateTenantSettings, setLogoFromUrl\n");
            sb.append("- Entity label renaming: updateEntityLabels (customerLabel, contactLabel, opportunityLabel, activityLabel, customRecordLabel, orderLabel, invoiceLabel)\n");
            sb.append("- Document template management: createDocumentTemplate, updateDocumentTemplate, deleteDocumentTemplate\n");
            sb.append("  Use createDocumentTemplate to create new templates with name, templateType (slide_deck, one_pager, csv_export, report), description, and styleJson.\n");
            sb.append("  Use updateDocumentTemplate to modify existing templates (pass templateId + fields to change).\n");
            sb.append("  Use deleteDocumentTemplate to remove a template by ID.\n");
            sb.append("  When a user asks to create or modify a template, use these tools directly — do NOT redirect them to the admin page.\n");
            sb.append("- Notification/email template management: listNotificationTemplates, createNotificationTemplate\n");
            sb.append("  Use createNotificationTemplate(notificationType, name, subjectTemplate, bodyTemplate, isActive) to create email/notification templates.\n");
            sb.append("  notificationType is a free-form string matching the event, e.g.: OPPORTUNITY_CREATED, OPPORTUNITY_UPDATED, CUSTOMER_CREATED,\n");
            sb.append("    CONTACT_CREATED, ACTIVITY_DUE_SOON, ACTIVITY_CREATED, ORDER_CREATED, INVOICE_CREATED, DAILY_INSIGHT, CUSTOM_MESSAGE.\n");
            sb.append("  subjectTemplate and bodyTemplate support {{placeholder}} substitution, e.g. {{customerName}}, {{amount}}, {{dueDate}}, {{assignee}}, {{link}}.\n");
        } else {
            sb.append("The user is a regular user. Admin tools are not available.\n");
        }
        if ("confirm".equals(confirmMode) || confirmMode == null) {
            sb.append("IMPORTANT: When you want to create or update records, call the appropriate tool. ");
            sb.append("The user will review and confirm before actions are executed.\n");
        } else {
            sb.append("You are in AUTO mode. Tools are executed immediately without confirmation.\n");
        }
        sb.append("\nTo filter the list page, use applyFilters(entityType, filtersJson). Always call getCustomFieldSchema first to discover valid custom field keys and their options before constructing the filter. You can combine multiple filter keys in a single call.\n");
        sb.append("\n**File attachment handling:**\n");
        sb.append("When a file is uploaded, the file content appears in the message with a heading like '**File content (filename.ext):**'\n");
        sb.append("- JSON backup files: Pass the COMPLETE JSON text (from '{' to the final '}') as the backupJson parameter to restoreTenantFromBackup.\n");
        sb.append("- PDFs: Extract names, companies, emails, deal values, dates, and other CRM-relevant data.\n");
        sb.append("- Images: Recognize business cards, documents, or other visual information and extract contact/company data.\n");
        sb.append("- CSV/spreadsheets: Treat each row as a potential customer, contact, opportunity, or activity record.\n");
        sb.append("For non-backup files, always use confirm mode to propose what records you'll create before calling create tools.\n");
        sb.append("\n**WORKSPACE SETUP — execute immediately, do NOT summarize:**\n");
        sb.append("When the user provides a complete workspace/tenant setup specification (branding, custom fields, sample data, policies, templates, etc.),\n");
        sb.append("DO NOT echo, summarize, or list back what they sent. DO NOT say 'Here is the complete setup' or 'Please let me know if this meets your requirements'.\n");
        sb.append("Instead, BEGIN EXECUTING immediately by calling tools in this sequence:\n");
        sb.append("  1. updateTenantSettings — apply colors, bio, website, language from the spec\n");
        sb.append("  2. setLogoFromUrl — apply logo URL if provided\n");
        sb.append("  3. bulkCreateCustomFields — create ALL custom fields across ALL entity types in ONE call (pass a JSON array with all fields)\n");
        sb.append("     NEVER use createCustomField in a loop — always use bulkCreateCustomFields when creating more than one field\n");
        sb.append("  6. createCalculatedField — create each calculated field\n");
        sb.append("  7. createPolicy — create each business policy rule\n");
        sb.append("  8. bulkCreateCustomers — create all sample customers in one call (include customFields)\n");
        sb.append("  9. bulkCreateContacts — create all sample contacts (use customerName for linking)\n");
        sb.append("  10. bulkCreateOpportunities — create all sample opportunities (use customerName for linking)\n");
        sb.append("  11. bulkCreateActivities — create all sample activities\n");
        sb.append("  12. createCustomRecord — create each sample customRecord\n");
        sb.append("  13. createOrder / createInvoice — create sample orders and invoices if specified\n");
        sb.append("  14. createDocumentTemplate — create each document template\n");
        sb.append("  15. createNotificationTemplate — create each notification template\n");
        sb.append("After each step, output one brief line confirming what was just created (e.g. '✓ 13 Customer custom fields created').\n");
        sb.append("Skip any step where the spec provides no data. Do not ask for confirmation before starting — execute immediately.\n");
        sb.append("Be concise and helpful. If you're unsure about details, ask the user to clarify.");
        if (isSystemAdmin) {
            sb.append("\n\n**SYSTEM ADMIN tools (SYSTEM_ADMIN role only):**\n");
            sb.append("- backupTenant(tenantName): Back up a tenant and return a download URL for the JSON file. tenantName is optional — omit to back up the current tenant.\n");
            sb.append("- createTenant(tenantName): Create a blank new tenant with default templates. You are added as admin.\n");
            sb.append("- restoreTenantFromBackup(backupJson, tenantNameOverride): Create a new tenant from a BOCRM backup JSON string.\n");
            sb.append("  WHEN USER ATTACHES A JSON BACKUP FILE: Do NOT ask for confirmation. Immediately:\n");
            sb.append("    1. Extract the entire JSON content (labeled '**File content (*.json):**')\n");
            sb.append("    2. Extract tenantNameOverride from the user's message (e.g., 'restore as potatozz' → 'potatozz')\n");
            sb.append("    3. Call restoreTenantFromBackup(backupJson, tenantNameOverride) immediately with the full JSON and name override\n");
            sb.append("    4. Then summarize what's being restored (record counts, custom fields, etc.) as a status message\n");
            sb.append("- cloneTenant(sourceTenantName, newTenantName): Backup an existing tenant and restore it as a new tenant.\n");
            sb.append("  sourceTenantName is the name of the tenant to clone. newTenantName is optional — defaults to '<source name> (Clone)'.\n");
            sb.append("  ALWAYS confirm with the user before executing — creates a new permanent tenant and is irreversible.\n");
        }
        return sb.toString();
    }

    private String getTenantName(Long tenantId) {
        try {
            TenantDTO tenant = tenantAdminService.getCurrentTenant();
            return tenant != null ? tenant.getName() : "Unknown";
        } catch (Exception e) {
            return "BOCRM";
        }
    }

    private List<PendingActionDTO> buildPendingActions(List<AssistantMessage.ToolCall> toolCalls) {
        return toolCalls.stream().map(tc -> {
            Map<String, Object> params = new LinkedHashMap<>();
            try {
                params = objectMapper.readValue(tc.arguments(), Map.class);
            } catch (Exception ignored) {}
            return PendingActionDTO.builder()
                    .toolCallId(tc.id())
                    .toolName(tc.name())
                    .parameters(params)
                    .description(buildToolDescription(tc.name(), params))
                    .build();
        }).collect(Collectors.toList());
    }

    private String buildToolDescription(String toolName, Map<String, Object> params) {
        return switch (toolName) {
            case "createCustomer" -> "Create customer: " + params.getOrDefault("name", "?");
            case "bulkCreateCustomers" -> "Bulk create customers";
            case "bulkCreateContacts" -> "Bulk create contacts";
            case "bulkCreateOpportunities" -> "Bulk create opportunities";
            case "bulkCreateActivities" -> "Bulk create activities";
            case "bulkCreateCustomRecords" -> "Bulk create customRecords";
            case "updateCustomer" -> "Update customer id=" + params.getOrDefault("customerId", "?");
            case "createContact" -> "Create contact: " + params.getOrDefault("name", "?");
            case "createOpportunity" -> "Create opportunity: " + params.getOrDefault("name", "?");
            case "updateOpportunity" -> "Update opportunity id=" + params.getOrDefault("opportunityId", "?");
            case "updateContact" -> "Update contact id=" + params.getOrDefault("contactId", "?");
            case "createActivity" -> "Create activity: " + params.getOrDefault("subject", "?");
            case "updateActivity" -> "Update activity id=" + params.getOrDefault("activityId", "?");
            case "deleteCustomer" -> "Delete customer id=" + params.getOrDefault("customerId", "?");
            case "deleteContact" -> "Delete contact id=" + params.getOrDefault("contactId", "?");
            case "deleteOpportunity" -> "Delete opportunity id=" + params.getOrDefault("opportunityId", "?");
            case "deleteActivity" -> "Delete activity id=" + params.getOrDefault("activityId", "?");
            case "createDocumentTemplate" -> "Create document template: " + params.getOrDefault("name", "?") + " (" + params.getOrDefault("templateType", "?") + ")";
            case "updateDocumentTemplate" -> "Update document template id=" + params.getOrDefault("templateId", "?");
            case "deleteDocumentTemplate" -> "Delete document template id=" + params.getOrDefault("templateId", "?");
            case "createCustomField" -> "Create custom field '" + params.getOrDefault("key", "?") + "' on " + params.getOrDefault("entityType", "?");
            case "updateCustomField" -> "Update custom field id=" + params.getOrDefault("fieldId", "?");
            case "deleteCustomField" -> "Delete custom field id=" + params.getOrDefault("fieldId", "?");
            case "createCalculatedField" -> "Create calculated field '" + params.getOrDefault("key", "?") + "' on " + params.getOrDefault("entityType", "?") + " | expr: " + params.getOrDefault("expression", "?");
            case "updateCalculatedField" -> "Update calculated field id=" + params.getOrDefault("fieldId", "?");
            case "deleteCalculatedField" -> "Delete calculated field id=" + params.getOrDefault("fieldId", "?");
            case "updateEntityLabels" -> "Update entity display labels";
            case "listNotificationTemplates" -> "List notification templates";
            case "createNotificationTemplate" -> "Create notification template: " + params.getOrDefault("name", "?") + " (" + params.getOrDefault("notificationType", "?") + ")";
            case "createCustomRecord" -> "Create customRecord: " + params.getOrDefault("name", "?");
            case "updateCustomRecord" -> "Update customRecord id=" + params.getOrDefault("customRecordId", "?");
            case "deleteCustomRecord" -> "Delete customRecord id=" + params.getOrDefault("customRecordId", "?");
            case "createOrder" -> "Create order for customer id=" + params.getOrDefault("customerId", "?") + " | Status: " + params.getOrDefault("status", "DRAFT");
            case "updateOrder" -> "Update order id=" + params.getOrDefault("orderId", "?");
            case "createInvoice" -> "Create invoice for customer id=" + params.getOrDefault("customerId", "?") + " | Status: " + params.getOrDefault("status", "DRAFT");
            case "updateInvoice" -> "Update invoice id=" + params.getOrDefault("invoiceId", "?");
            case "generateOpportunityReport" -> "Generate opportunity report for id=" + params.getOrDefault("opportunityId", "?");
            case "generateCrmReport" -> "Generate " + params.getOrDefault("entityType", "?") + " report";
            case "generateOnePager" -> "Generate one-pager for " + params.getOrDefault("entityType", "?") + " id=" + params.getOrDefault("entityId", "?");
            case "generateSlideDeck" -> "Generate slide deck for " + params.getOrDefault("entityType", "?") + " id=" + params.getOrDefault("entityId", "?");
            case "generateCsvExport" -> "Generate CSV export for " + params.getOrDefault("entityType", "?");
            case "listPolicies" -> "List policies for " + params.getOrDefault("entityType", "?");
            case "createPolicy" -> "Create policy '" + params.getOrDefault("name", "?") + "' on " + params.getOrDefault("entityType", "?") + " | " + params.getOrDefault("operation", "?");
            case "updatePolicy" -> "Update policy id=" + params.getOrDefault("policyId", "?");
            case "deletePolicy" -> "Delete policy id=" + params.getOrDefault("policyId", "?");
            case "explainPolicyTrigger" -> "Explain why policy '" + params.getOrDefault("policyName", "?") + "' was triggered on " + params.getOrDefault("entityType", "?");
            default -> toolName + ": " + params;
        };
    }

    private JsonNode serializeToolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        ArrayNode arr = objectMapper.createArrayNode();
        toolCalls.forEach(tc -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", tc.id());
            node.put("name", tc.name());
            node.put("type", tc.type());
            node.put("arguments", tc.arguments());
            arr.add(node);
        });
        return arr;
    }

    private List<AssistantMessage.ToolCall> parseStoredToolCalls(JsonNode storedToolCalls) {
        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        if (storedToolCalls == null || !storedToolCalls.isArray()) return result;
        storedToolCalls.forEach(node ->
                result.add(new AssistantMessage.ToolCall(
                        node.path("id").asText(),
                        node.path("type").asText("tool_use"),
                        node.path("name").asText(),
                        node.path("arguments").asText("{}")
                ))
        );
        return result;
    }

    private void saveUserMessage(Long tenantId, Long userId, SendMessageRequest request, String sessionId) {
        // Save if there's a message OR an attachment
        if ((request.getMessage() == null || request.getMessage().isBlank()) &&
            (request.getAttachmentBase64() == null || request.getAttachmentBase64().isBlank())) {
            return;
        }
        ChatMessage msg = ChatMessage.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role("user")
                .content(request.getMessage() != null ? request.getMessage() : "")
                .contextEntityType(request.getContextEntityType())
                .contextEntityId(request.getContextEntityId())
                .sessionId(sessionId)
                .attachmentFileName(request.getAttachmentFileName())
                .attachmentMimeType(request.getAttachmentMimeType())
                .build();
        chatMessageRepository.save(msg);
    }

    private void saveAssistantMessage(Long tenantId, Long userId, ChatResponse response, String sessionId,
                                       String modelId, int inputTokens, int outputTokens, JsonNode toolCallsJson,
                                       long processingTimeMs) {
        String content = "";
        if (response.getResult() != null && response.getResult().getOutput() != null) {
            content = response.getResult().getOutput().getText();
            if (content == null) content = "";
        }
        ChatMessage msg = ChatMessage.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role("assistant")
                .content(content)
                .sessionId(sessionId)
                .modelUsed(modelId)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .toolCalls(toolCallsJson)
                .processingTimeMs(processingTimeMs)
                .build();
        chatMessageRepository.save(msg);
    }

    private List<ChatMessageDTO> buildHistory(Long tenantId, Long userId, String sessionId) {
        return chatMessageRepository.findByTenantIdAndUserIdAndSessionIdOrderByCreatedAtAsc(tenantId, userId, sessionId)
                .stream()
                .map(m -> {
                    String content = m.getContent() != null ? m.getContent() : "";
                    return ChatMessageDTO.builder()
                            .id(m.getId())
                            .role(m.getRole())
                            .content(content)
                            .contextEntityType(m.getContextEntityType())
                            .contextEntityId(m.getContextEntityId())
                            .createdAt(m.getCreatedAt())
                            .attachmentFileName(m.getAttachmentFileName())
                            .attachmentMimeType(m.getAttachmentMimeType())
                            .modelUsed(m.getModelUsed())
                            .processingTimeMs(m.getProcessingTimeMs())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ChatResponseDTO buildResponse(ChatResponse chatResponse, List<PendingActionDTO> pendingActions,
                                           String sessionId, String modelId, long processingTimeMs,
                                           List<ChatMessageDTO> history, QuotaStatusDTO quotaStatus,
                                           String navigateTo) {
        String responseText = "";
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            responseText = chatResponse.getResult().getOutput().getText();
            if (responseText == null) responseText = "";
        }
        return ChatResponseDTO.builder()
                .response(responseText)
                .sessionId(sessionId)
                .modelUsed(modelId)
                .processingTimeMs(processingTimeMs)
                .pendingActions(pendingActions)
                .quotaStatus(quotaStatus)
                .history(history)
                .suggestedActions(Collections.emptyList())
                .navigateTo(navigateTo)
                .build();
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_SYSTEM_ADMIN".equals(a));
    }

    private boolean isCurrentUserSystemAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Build a fallback list of models prioritizing the preferred model, then other models from the same provider,
     * then all enabled models from other providers (shuffled within each group).
     *
     * <p>WHY: When a model hits rate limits or is unavailable, this ensures graceful fallback to alternative models.
     * The list respects the EnabledAiModel.enabled flag — disabled models are excluded.
     *
     * @param preferredProvider the tenant's assigned provider (e.g., "anthropic")
     * @param preferredModelId the tenant's assigned model (e.g., "claude-3-5-sonnet-20241022")
     * @return list of (provider, modelId) pairs in fallback order
     */
    private List<ModelFallback> buildModelFallbackList(String preferredProvider, String preferredModelId) {
        List<EnabledAiModel> allEnabled = enabledAiModelRepository.findAll().stream()
                .filter(m -> m.getEnabled() != null && m.getEnabled())
                .toList();

        List<ModelFallback> sameProvider = new ArrayList<>();
        List<ModelFallback> otherProviders = new ArrayList<>();

        for (EnabledAiModel m : allEnabled) {
            ModelFallback fallback = new ModelFallback(m.getProvider(), m.getModelId());
            if (preferredProvider.equals(m.getProvider())) {
                sameProvider.add(fallback);
            } else {
                otherProviders.add(fallback);
            }
        }

        // Put preferred model first in sameProvider list, shuffle the rest
        sameProvider.sort((a, b) -> {
            if (a.modelId().equals(preferredModelId)) return -1;
            if (b.modelId().equals(preferredModelId)) return 1;
            return 0;
        });

        // Shuffle other models within their groups
        Collections.shuffle(sameProvider.subList(1, sameProvider.size()));
        Collections.shuffle(otherProviders);

        // Build final list: preferred first, then same provider others, then other providers
        List<ModelFallback> result = new ArrayList<>();
        if (!sameProvider.isEmpty() && sameProvider.get(0).modelId().equals(preferredModelId)) {
            result.add(sameProvider.get(0));
            result.addAll(sameProvider.subList(1, sameProvider.size()));
        } else {
            result.addAll(sameProvider);
        }
        result.addAll(otherProviders);

        return result;
    }

    /**
     * Call LLM in auto mode with fallback: try each model in the fallback list until one succeeds.
     * Log warnings on failures but only throw if all models fail.
     */
    private ChatResponse callInAutoModeWithFallback(List<Message> messages, String preferredProvider,
                                                     String preferredModelId, boolean isAdmin) {
        List<ModelFallback> fallbackList = buildModelFallbackList(preferredProvider, preferredModelId);
        Exception lastException = null;

        for (int i = 0; i < fallbackList.size(); i++) {
            ModelFallback fallback = fallbackList.get(i);
            try {
                log.debug("Auto mode attempt {} of {}: provider={}, model={}", i + 1, fallbackList.size(),
                        fallback.provider(), fallback.modelId());
                return callInAutoMode(messages, fallback.provider(), fallback.modelId(), isAdmin);
            } catch (Exception e) {
                lastException = e;
                if (isToolsNotSupportedError(e)) {
                    log.warn("Auto mode skipping provider={}, model={}: model does not support tools.",
                            fallback.provider(), fallback.modelId());
                } else if (isOomError(e)) {
                    log.warn("Auto mode skipping provider={}, model={}: insufficient system memory ({}).",
                            fallback.provider(), fallback.modelId(), extractOomMessage(e));
                } else if (i < fallbackList.size() - 1) {
                    log.warn("Auto mode attempt {} failed for provider={}, model={}: {}. Trying next model.",
                            i + 1, fallback.provider(), fallback.modelId(), e.getMessage());
                } else {
                    log.error("All {} auto mode attempts failed. Last error: {}", fallbackList.size(), e.getMessage());
                }
            }
        }

        if (lastException != null) {
            throw new RuntimeException("All enabled models failed in auto mode", lastException);
        }
        throw new RuntimeException("No enabled models available for auto mode");
    }

    /**
     * Call LLM in confirm mode with fallback: try each model in the fallback list until one succeeds.
     * Log warnings on failures but only throw if all models fail.
     */
    private ChatResponse callInConfirmModeWithFallback(List<Message> messages, String preferredProvider,
                                                       String preferredModelId, boolean isAdmin) {
        List<ModelFallback> fallbackList = buildModelFallbackList(preferredProvider, preferredModelId);
        Exception lastException = null;

        for (int i = 0; i < fallbackList.size(); i++) {
            ModelFallback fallback = fallbackList.get(i);
            try {
                log.debug("Confirm mode attempt {} of {}: provider={}, model={}", i + 1, fallbackList.size(),
                        fallback.provider(), fallback.modelId());
                return callInConfirmMode(messages, fallback.provider(), fallback.modelId(), isAdmin);
            } catch (Exception e) {
                lastException = e;
                if (isToolsNotSupportedError(e)) {
                    log.warn("Confirm mode skipping provider={}, model={}: model does not support tools.",
                            fallback.provider(), fallback.modelId());
                } else if (isOomError(e)) {
                    log.warn("Confirm mode skipping provider={}, model={}: insufficient system memory ({}).",
                            fallback.provider(), fallback.modelId(), extractOomMessage(e));
                } else if (i < fallbackList.size() - 1) {
                    log.warn("Confirm mode attempt {} failed for provider={}, model={}: {}. Trying next model.",
                            i + 1, fallback.provider(), fallback.modelId(), e.getMessage());
                } else {
                    log.error("All {} confirm mode attempts failed. Last error: {}", fallbackList.size(), e.getMessage());
                }
            }
        }

        if (lastException != null) {
            throw new RuntimeException("All enabled models failed in confirm mode", lastException);
        }
        throw new RuntimeException("No enabled models available for confirm mode");
    }

    private boolean isWorkspaceSetupPrompt(String message) {
        if (message == null || message.length() < 500) return false;
        int signals = 0;
        String lower = message.toLowerCase();
        if (lower.contains("custom field")) signals++;
        if (lower.contains("sample data")) signals++;
        if (lower.contains("business polic") || lower.contains("policy")) signals++;
        if (lower.contains("calculated field")) signals++;
        if (lower.contains("branding") || lower.contains("primary color") || lower.contains("logo")) signals++;
        if (lower.contains("set up") || lower.contains("setup") || lower.contains("setting up")) signals++;
        return signals >= 3;
    }

    private boolean isToolsNotSupportedError(Exception e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("does not support tools");
    }

    private boolean isOomError(Exception e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("more system memory");
    }

    private String extractOomMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "unknown";
        int idx = msg.indexOf("more system memory");
        return idx >= 0 ? msg.substring(Math.max(0, idx - 20)).replaceAll("[{}\"\\\\]", "") : msg;
    }

    // Record type to hold provider+modelId pairs for fallback
    private record ModelFallback(String provider, String modelId) {}

    // Type-safe argument extraction helpers
    private static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        String s = v.toString();
        // LLMs sometimes send "" or the literal "null" for optional string fields; treat as null
        return (s.isBlank() || "null".equalsIgnoreCase(s)) ? null : s;
    }

    private static Long longVal(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        long result;
        if (v instanceof Number) {
            result = ((Number) v).longValue();
        } else {
            try { result = Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
        }
        // LLMs sometimes send 0 for "not provided" on optional ID fields; treat as null
        return result == 0L ? null : result;
    }

    private static Boolean boolVal(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private static Integer intVal(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        int result;
        if (v instanceof Number) {
            result = ((Number) v).intValue();
        } else {
            try { result = Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
        }
        return result;
    }
}
