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

import com.bocrm.backend.dto.ActivityDTO;
import com.bocrm.backend.dto.CustomerDTO;
import com.bocrm.backend.dto.OpportunityDTO;
import com.bocrm.backend.entity.EnabledAiModel;
import com.bocrm.backend.repository.EnabledAiModelRepository;
import com.bocrm.backend.tools.WebSearchTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Generates and caches a brief AI insight for the dashboard landing page.
 * Insight is per-tenant and re-generated after 1 hour.
 *
 * <p><strong>Two Insight Modes</strong>:
 * <ul>
 *   <li><strong>New/Empty Tenant</strong> (zero records): Onboarding message encouraging setup
 *     (company profile, custom fields, theme customization, imports).
 *   <li><strong>Active Tenant</strong> (has data): Clippy-style observation about the pipeline—
 *     which deals are closing, what's stalled, patterns spotted—optionally enriched with relevant news.
 * </ul>
 *
 * <p><strong>Per-Tenant In-Memory Cache with TTL</strong>:
 * Uses {@link ConcurrentHashMap} keyed by tenantId. Each insight has a {@code generatedAt} timestamp.
 * On cache hit, checks if the insight is stale (>1 hour old). If stale or missing, calls
 * {@link #callLlm(Long, String)} to regenerate. This prevents hammering the LLM on every page load.
 *
 * <p><strong>Provider Rotation with Resilience</strong>:
 * The service shuffles enabled providers (Anthropic, OpenAI, Google) and tries each in order.
 * If a provider fails (API error, timeout, etc.), it logs a warning and tries the next provider.
 * If all providers fail, returns a fallback message instead of crashing.
 * This ensures the dashboard always renders, even if the insight fails.
 *
 * <p><strong>Smart News Context</strong> (Active Tenant Only):
 * The LLM is given optional recent news, which makes insights more topical and engaging.
 * News query priority:
 * <ol>
 *   <li>Highest-value opportunity's customer name (if any deals exist)
 *   <li>Subject of the most recent activity (if any activities exist)
 *   <li>Top customer names (if any customers exist)
 *   <li>Industry hint from org bio (first sentence)
 *   <li>Fallback: tenant name + "industry news"
 * </ol>
 * News is fetched via {@link WebSearchTools} (Tavily first, NewsAPI fallback).
 * If no news service is configured or both fail, the insight is generated without news context.
 *
 * <p><strong>ChatModelRegistry</strong>:
 * Resolves {@link org.springframework.ai.chat.model.ChatModel} by provider name.
 * The registry caches instances; new instances are created only if config changes.
 *
 * <p><strong>Exposure</strong>:
 * REST endpoints: {@code GET /assistant/insight} (cached), {@code POST /assistant/insight/refresh} (force-refresh).
 * Frontend: Dashboard card with "Refresh" button to manually regenerate.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class DashboardInsightService {

    private static final Duration TTL = Duration.ofHours(1);
    // WHY: Max length for news context. Prevents token bloat if multiple articles are returned.
    private static final int MAX_NEWS_CONTEXT = 900;
    private static final Random RANDOM = new Random();

    public record DashboardInsight(String text, Instant generatedAt) {}

    private final ConcurrentHashMap<Long, DashboardInsight> cache = new ConcurrentHashMap<>();

    private final ChatModelRegistry chatModelRegistry;
    private final EnabledAiModelRepository enabledAiModelRepository;
    private final CustomerService customerService;
    private final OpportunityService opportunityService;
    private final ActivityService activityService;
    private final ContactService contactService;
    private final TenantAdminService tenantAdminService;
    private final WebSearchTools webSearchTools;

    public DashboardInsightService(ChatModelRegistry chatModelRegistry,
                                    EnabledAiModelRepository enabledAiModelRepository,
                                    CustomerService customerService,
                                    OpportunityService opportunityService,
                                    ActivityService activityService,
                                    ContactService contactService,
                                    TenantAdminService tenantAdminService,
                                    WebSearchTools webSearchTools) {
        this.chatModelRegistry = chatModelRegistry;
        this.enabledAiModelRepository = enabledAiModelRepository;
        this.customerService = customerService;
        this.opportunityService = opportunityService;
        this.activityService = activityService;
        this.contactService = contactService;
        this.tenantAdminService = tenantAdminService;
        this.webSearchTools = webSearchTools;
    }

    /**
     * Get the cached insight for this tenant, or generate a fresh one if stale or missing.
     *
     * <p>WHY: Cache prevents repeated LLM calls on every dashboard load. The 1-hour TTL balances
     * freshness (insights reflect recent data changes) with efficiency (not hammering the LLM).
     *
     * @param tenantId the tenant ID
     * @param tenantName the tenant display name (used in prompts)
     * @return the cached or newly generated insight
     */
    public DashboardInsight getInsight(Long tenantId, String tenantName) {
        DashboardInsight cached = cache.get(tenantId);
        if (cached != null && Duration.between(cached.generatedAt(), Instant.now()).compareTo(TTL) < 0) {
            return cached;
        }
        return generateAndCache(tenantId, tenantName);
    }

    /**
     * Force-refresh the insight, bypassing the TTL cache.
     * Called when user clicks the "Refresh" button on the dashboard.
     *
     * @param tenantId the tenant ID
     * @param tenantName the tenant display name
     * @return the newly generated insight
     */
    public DashboardInsight refreshInsight(Long tenantId, String tenantName) {
        // WHY: Remove cached entry to force regeneration, even if the previous one is recent.
        cache.remove(tenantId);
        return generateAndCache(tenantId, tenantName);
    }

    /**
     * Generate a new insight and cache it. If generation fails, return a friendly fallback message.
     *
     * <p>WHY: Catch all exceptions and return a fallback instead of throwing. This ensures
     * the dashboard is never broken by a failed insight generation (e.g., provider API down,
     * network timeout, etc.). The user gets a sensible message instead of an error page.
     *
     * @param tenantId the tenant ID
     * @param tenantName the tenant display name
     * @return the generated insight, or a fallback message if generation failed
     */
    private DashboardInsight generateAndCache(Long tenantId, String tenantName) {
        try {
            String text = callLlm(tenantId, tenantName);
            DashboardInsight insight = new DashboardInsight(text, Instant.now());
            cache.put(tenantId, insight);
            return insight;
        } catch (Exception e) {
            log.error("Failed to generate dashboard insight for tenant {}", tenantId, e);
            // WHY: Return a helpful fallback message that encourages user action without relying on LLM.
            return new DashboardInsight(
                    "It looks like I couldn't pull an insight right now — but don't worry! "
                    + "Head to your pipeline to see what deals need attention, "
                    + "or check Recent Activities to pick up where you left off.", Instant.now());
        }
    }

    private String callLlm(Long tenantId, String tenantName) {
        // ── Gather counts ────────────────────────────────────────────────────
        int customers = 0, contacts = 0, opportunities = 0, activities = 0;
        try { customers     = (int) customerService.listCustomers(0, 1, "name", "asc", "", java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getTotalElements(); } catch (Exception ignored) {}
        try { contacts      = (int) contactService.listContacts(0, 1, "name", "asc", "", null, null, java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getTotalElements(); } catch (Exception ignored) {}
        try { opportunities = (int) opportunityService.listOpportunities(0, 1, "name", "asc", "", null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getTotalElements(); } catch (Exception ignored) {}
        try { activities    = (int) activityService.listActivities(0, 1, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getTotalElements(); } catch (Exception ignored) {}

        // ── Build the prompt messages (independent of which model will run them) ──
        List<org.springframework.ai.chat.messages.Message> messages;
        boolean isNew = (customers + contacts + opportunities + activities) == 0;

        if (isNew) {
            messages = List.of(
                new SystemMessage(
                    "You are a sharp, friendly CRM assistant for BOCRM — think of yourself like a helpful " +
                    "office assistant who just noticed this workspace is brand new and wants to help. " +
                    "Write 1-2 sentences (max 70 words) that feel personal and observational, like: " +
                    "'It looks like you\'re just getting started — here\'s what I\'d do first...' " +
                    "Mention 2-3 concrete setup steps picked from: " +
                    "(1) add a company profile under Settings → Company Profile so I can give smarter insights, " +
                    "(2) create custom fields under Settings → Custom Fields to track what matters to your business, " +
                    "(3) customize your theme under Settings → Appearance, " +
                    "(4) import your first customers or contacts. " +
                    "Sound like a knowledgeable colleague, not a help page. No markdown, no bullet points."),
                new UserMessage("Tenant: " + tenantName + ". Write a Clippy-style welcome with 2-3 concrete setup suggestions.")
            );
        } else {
            // ── Active path — fetch rich CRM context ─────────────────────────
            List<OpportunityDTO> recentOpps = Collections.emptyList();
            try { recentOpps = opportunityService.listOpportunities(0, 6, "closeDate", "asc", "", null, java.util.Collections.emptyList(), null, java.util.Collections.emptyMap()).getContent(); } catch (Exception ignored) {}

            List<ActivityDTO> recentActivities = Collections.emptyList();
            try { recentActivities = activityService.listActivities(0, 5, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent(); } catch (Exception ignored) {}

            List<CustomerDTO> topCustomers = Collections.emptyList();
            try { topCustomers = customerService.listCustomers(0, 5, "createdAt", "desc", "", java.util.Collections.emptyList(), java.util.Collections.emptyMap()).getContent(); } catch (Exception ignored) {}

            String orgBio = null;
            try { orgBio = tenantAdminService.getOrgBio(tenantId); } catch (Exception ignored) {}

            StringBuilder context = new StringBuilder();
            context.append("CRM workspace: ").append(tenantName).append("\n");
            if (orgBio != null && !orgBio.isBlank()) {
                context.append("Company: ").append(orgBio).append("\n");
            }
            context.append("Totals: ").append(customers).append(" customers, ")
                   .append(contacts).append(" contacts, ")
                   .append(opportunities).append(" open opportunities, ")
                   .append(activities).append(" activities.\n");

            if (!recentOpps.isEmpty()) {
                context.append("\nOpportunities (sorted by close date):\n");
                for (OpportunityDTO o : recentOpps) {
                    context.append("  - \"").append(o.getName()).append("\" — stage: ").append(o.getStage());
                    if (o.getValue() != null) context.append(", value: $").append(String.format("%,.0f", o.getValue().doubleValue()));
                    if (o.getProbability() != null) context.append(", win probability: ").append(o.getProbability()).append("%");
                    if (o.getCloseDate() != null) context.append(", close date: ").append(o.getCloseDate());
                    context.append("\n");
                }
            }

            if (!recentActivities.isEmpty()) {
                context.append("\nRecent activities:\n");
                for (ActivityDTO a : recentActivities) {
                    context.append("  - [").append(a.getType()).append("] ").append(a.getSubject())
                           .append(" — ").append(a.getStatus());
                    if (a.getDueAt() != null) context.append(", due: ").append(a.getDueAt());
                    context.append("\n");
                }
            }

            if (!topCustomers.isEmpty()) {
                context.append("\nTop customers: ")
                       .append(topCustomers.stream().map(CustomerDTO::getName).collect(Collectors.joining(", ")))
                       .append("\n");
            }

            String newsContext = fetchNewsContext(tenantName, orgBio, recentOpps, recentActivities, topCustomers);
            if (newsContext != null && !newsContext.isBlank()) {
                context.append("\nRecent relevant news:\n").append(newsContext).append("\n");
            }

            String systemPrompt = (newsContext != null && !newsContext.isBlank())
                    ? "You are a sharp-eyed CRM assistant who has been watching this pipeline closely and just tapped " +
                      "the user on the shoulder. Based on the pipeline data and recent news, write 2-3 sentences (max 100 words) " +
                      "that connect a specific deal or customer to something happening in the news right now. " +
                      "Start with an observation like 'I noticed...' or 'Heads up —'. Name the specific deal or customer. " +
                      "Sound like a smart colleague, not a news summary. No markdown."
                    : "You are a sharp-eyed CRM assistant who has been watching this pipeline closely and just tapped " +
                      "the user on the shoulder. Based on the pipeline data, write 2-3 actionable sentences (max 100 words). " +
                      "Start with a direct observation — which deal is about to close, what looks stalled, what pattern you spot. " +
                      "Use language like 'I noticed...', 'Heads up —', or 'It looks like...'. Name specific deals or customers. " +
                      "Sound like a knowledgeable colleague who pays attention. No markdown.";

            messages = List.of(new SystemMessage(systemPrompt), new UserMessage(context.toString()));
        }

        // ── Build list of enabled models per provider ────────────────────────
        Map<String, List<EnabledAiModel>> enabledModelsByProvider = enabledAiModelRepository.findAllByOrderByProviderAscModelIdAsc()
                .stream()
                .filter(EnabledAiModel::getEnabled)
                .collect(Collectors.groupingBy(EnabledAiModel::getProvider));

        // ── Shuffle providers and try each enabled model in each provider ────
        List<String> providers = new java.util.ArrayList<>(enabledModelsByProvider.keySet());
        Collections.shuffle(providers, RANDOM);

        Exception lastException = null;
        for (String provider : providers) {
            List<EnabledAiModel> modelsForProvider = enabledModelsByProvider.get(provider);

            // Shuffle models within this provider
            Collections.shuffle(modelsForProvider, RANDOM);

            for (EnabledAiModel enabledModel : modelsForProvider) {
                String modelId = enabledModel.getModelId();
                log.debug("Dashboard insight for tenant {} trying provider={} model={}", tenantId, provider, modelId);
                try {
                    return chatModelRegistry.getModel(provider)
                            .call(new Prompt(messages, buildOptions(provider, modelId)))
                            .getResult().getOutput().getText();
                } catch (Exception e) {
                    log.warn("Provider {} model {} failed for tenant {} insight: {}", provider, modelId, tenantId, e.getMessage());
                    lastException = e;
                }
            }
        }
        throw new RuntimeException("All enabled models failed to generate insight", lastException);
    }

    private ChatOptions buildOptions(String provider, String modelId) {
        return switch (provider) {
            case "openai" -> OpenAiChatOptions.builder().model(modelId).maxTokens(1024).build();
            case "google" -> GoogleGenAiChatOptions.builder().model(modelId).maxOutputTokens(1024).build();
            case "ollama" -> OllamaChatOptions.builder().model(modelId).build();
            default       -> AnthropicChatOptions.builder().model(modelId).maxTokens(1024).build();
        };
    }

    /**
     * Attempts to fetch news relevant to the tenant's most active CRM data.
     * Query priority: highest-value opportunity → most recent activity → customer names → org bio.
     * Tries Tavily first, then NewsAPI; returns null if neither is configured or both fail.
     */
    private String fetchNewsContext(String tenantName, String orgBio,
                                     List<OpportunityDTO> opps, List<ActivityDTO> activities,
                                     List<CustomerDTO> customers) {
        String query = buildSearchQuery(tenantName, orgBio, opps, activities, customers);
        if (query == null || query.isBlank()) return null;

        try {
            String result = webSearchTools.searchWeb(query);
            if (!result.startsWith("Tavily web search is not configured")) {
                return result.length() > MAX_NEWS_CONTEXT ? result.substring(0, MAX_NEWS_CONTEXT) : result;
            }
        } catch (Exception e) {
            log.debug("Tavily search skipped: {}", e.getMessage());
        }

        try {
            String result = webSearchTools.searchNews(query);
            if (!result.startsWith("News search not configured")) {
                return result.length() > MAX_NEWS_CONTEXT ? result.substring(0, MAX_NEWS_CONTEXT) : result;
            }
        } catch (Exception e) {
            log.debug("NewsAPI search skipped: {}", e.getMessage());
        }

        return null;
    }

    private String buildSearchQuery(String tenantName, String orgBio,
                                     List<OpportunityDTO> opps, List<ActivityDTO> activities,
                                     List<CustomerDTO> customers) {
        // Industry hint: first sentence of org bio, capped at 50 chars
        String industryHint = "";
        if (orgBio != null && !orgBio.isBlank()) {
            String first = orgBio.split("[.\\n]")[0].trim();
            if (first.length() > 5) {
                industryHint = " " + first.substring(0, Math.min(first.length(), 50));
            }
        }

        // Priority 1: customer linked to the highest-value open opportunity
        OpportunityDTO topOpp = opps.stream()
                .filter(o -> o.getValue() != null)
                .max(Comparator.comparing(OpportunityDTO::getValue))
                .orElse(null);
        if (topOpp != null) {
            String subject = customers.stream()
                    .filter(c -> c.getId().equals(topOpp.getCustomerId()))
                    .map(CustomerDTO::getName)
                    .findFirst()
                    .orElse(topOpp.getName()); // fall back to opp name if customer not in list
            return (subject + industryHint).trim();
        }

        // Priority 2: subject of the most recent activity
        if (!activities.isEmpty() && activities.get(0).getSubject() != null
                && !activities.get(0).getSubject().isBlank()) {
            String subject = activities.get(0).getSubject();
            subject = subject.length() > 60 ? subject.substring(0, 60) : subject;
            return (subject + industryHint).trim();
        }

        // Priority 3: customer names
        if (!customers.isEmpty()) {
            String names = customers.stream().map(CustomerDTO::getName)
                    .limit(2).collect(Collectors.joining(" OR "));
            return (names + industryHint).trim();
        }

        // Fallback: org bio or tenant name
        if (orgBio != null && !orgBio.isBlank()) {
            String firstSentence = orgBio.split("[.\\n]")[0].trim();
            return firstSentence.length() > 80 ? firstSentence.substring(0, 80) : firstSentence;
        }
        return tenantName + " industry news";
    }
}
