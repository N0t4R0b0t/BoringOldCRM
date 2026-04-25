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
package com.bocrm.backend.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Web search tools available to all assistant users.
 * Free tools (Wikipedia, OpenFDA, ClinicalTrials, PubMed, PubChem, Reddit, ArXiv) require no API key.
 * Optional keyed tools (Tavily, NewsAPI, Brave, Serper, YouTube) are enabled via application-local.yml.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
@Slf4j
public class WebSearchTools {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.assistant.tavily-api-key:}")
    private String tavilyApiKey;

    @Value("${app.assistant.news-api-key:}")
    private String newsApiKey;

    @Value("${app.assistant.brave-api-key:}")
    private String braveApiKey;

    @Value("${app.assistant.serper-api-key:}")
    private String serperApiKey;

    @Value("${app.assistant.youtube-api-key:}")
    private String youtubeApiKey;

    public WebSearchTools(ObjectMapper objectMapper, HttpClient lenientHttpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = lenientHttpClient;
    }

    // ── Free tools — no API key required ─────────────────────────────────────

    @Tool(description = "Search Wikipedia for information about a company, person, place, concept, drug, or condition. Returns a summary and key facts. Use this to look up company background, founding history, industry, headquarters, products, etc.")
    public String searchWikipedia(String query) {
        try {
            String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=json&srlimit=3";

            HttpResponse<String> searchResp = get(searchUrl);
            JsonNode results = objectMapper.readTree(searchResp.body()).path("query").path("search");

            if (results.isEmpty()) return "No Wikipedia results found for: " + query;

            String pageTitle = results.get(0).path("title").asText();
            String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8).replace("+", "_");
            HttpResponse<String> summaryResp = get("https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedTitle);
            JsonNode s = objectMapper.readTree(summaryResp.body());

            StringBuilder sb = new StringBuilder();
            sb.append("Wikipedia: ").append(text(s.path("title"), pageTitle));
            String desc = text(s.path("description"));
            if (!desc.isBlank()) sb.append(" — ").append(desc);
            sb.append("\n\n").append(text(s.path("extract")));
            String url = text(s.path("content_urls").path("desktop").path("page"));
            if (!url.isBlank()) sb.append("\n\nSource: ").append(url);

            if (results.size() > 1) {
                sb.append("\n\nOther results: ");
                for (int i = 1; i < results.size(); i++) {
                    if (i > 1) sb.append(", ");
                    sb.append(results.get(i).path("title").asText());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Wikipedia search failed for '{}': {}", query, e.getMessage());
            return "Wikipedia search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search the FDA database for drug labels, ingredients, warnings, adverse events, and drug approvals. Useful in pharma contexts when researching a drug name, manufacturer, or active ingredient.")
    public String searchOpenFDA(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Drug label search
            JsonNode json = objectMapper.readTree(get("https://api.fda.gov/drug/label.json?search=" + encoded + "&limit=3").body());
            JsonNode results = json.path("results");
            if (results.isEmpty()) return "No FDA drug label results found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("OpenFDA Drug Search: ").append(query).append("\n\n");

            for (JsonNode r : results) {
                JsonNode fda = r.path("openfda");
                sb.append("Drug: ").append(firstOf(fda.path("brand_name"))).append("\n");
                sb.append("Generic: ").append(firstOf(fda.path("generic_name"))).append("\n");
                sb.append("Manufacturer: ").append(firstOf(fda.path("manufacturer_name"))).append("\n");
                String purpose = firstOf(r.path("purpose"));
                if (!purpose.isBlank()) sb.append("Purpose: ").append(truncate(purpose, 300)).append("\n");
                String warnings = firstOf(r.path("warnings"));
                if (!warnings.isBlank()) sb.append("Warnings: ").append(truncate(warnings, 200)).append("\n");
                sb.append("\n");
            }

            // Top adverse event reactions
            try {
                String evUrl = "https://api.fda.gov/drug/event.json?search=patient.drug.openfda.brand_name:"
                        + encoded + "&count=patient.reaction.reactionmeddrapt.exact&limit=5";
                JsonNode evJson = objectMapper.readTree(get(evUrl).body());
                JsonNode evResults = evJson.path("results");
                if (!evResults.isEmpty()) {
                    sb.append("Top reported adverse events:\n");
                    for (JsonNode ev : evResults) {
                        sb.append("  - ").append(ev.path("term").asText())
                          .append(" (").append(ev.path("count").asInt()).append(" reports)\n");
                    }
                }
            } catch (Exception e) {
                log.debug("OpenFDA adverse event lookup skipped: {}", e.getMessage());
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("OpenFDA search failed for '{}': {}", query, e.getMessage());
            return "OpenFDA search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search ClinicalTrials.gov for clinical trials by drug name, condition, sponsor, or NCT ID. Returns trial title, status, phase, sponsor, and summary. Free, no key required.")
    public String searchClinicalTrials(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            JsonNode json = objectMapper.readTree(
                    get("https://clinicaltrials.gov/api/v2/studies?query.term=" + encoded + "&pageSize=5&format=json").body());

            JsonNode studies = json.path("studies");
            if (studies.isEmpty()) return "No clinical trials found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("ClinicalTrials.gov: ").append(query)
              .append(" (").append(json.path("totalCount").asInt()).append(" total)\n\n");

            for (JsonNode study : studies) {
                JsonNode proto = study.path("protocolSection");
                JsonNode id     = proto.path("identificationModule");
                JsonNode status = proto.path("statusModule");
                JsonNode desc   = proto.path("descriptionModule");
                JsonNode sponsor = proto.path("sponsorCollaboratorsModule");
                JsonNode design = proto.path("designModule");

                String nct = id.path("nctId").asText("N/A");
                sb.append("Trial: ").append(id.path("briefTitle").asText("N/A")).append("\n");
                sb.append("NCT ID: ").append(nct).append(" | Status: ").append(status.path("overallStatus").asText("N/A")).append("\n");
                sb.append("Phase: ").append(firstOf(design.path("phases")))
                  .append(" | Sponsor: ").append(sponsor.path("leadSponsor").path("name").asText("N/A")).append("\n");
                String summary = desc.path("briefSummary").asText("");
                if (!summary.isBlank()) sb.append("Summary: ").append(truncate(summary, 300)).append("\n");
                sb.append("URL: https://clinicaltrials.gov/study/").append(nct).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("ClinicalTrials search failed for '{}': {}", query, e.getMessage());
            return "ClinicalTrials.gov search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search PubMed for peer-reviewed scientific and medical literature. Returns article titles, authors, journal, year, and links. Useful for drug research, clinical evidence, and condition background.")
    public String searchPubMed(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Step 1: get IDs
            JsonNode searchJson = objectMapper.readTree(
                    get("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="
                            + encoded + "&retmax=5&format=json").body());
            JsonNode idList = searchJson.path("esearchresult").path("idlist");
            if (idList.isEmpty()) return "No PubMed results found for: " + query;

            // Step 2: fetch summaries
            StringBuilder ids = new StringBuilder();
            for (JsonNode id : idList) { if (!ids.isEmpty()) ids.append(","); ids.append(id.asText()); }

            JsonNode summaryJson = objectMapper.readTree(
                    get("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=" + ids + "&format=json").body());
            JsonNode articles = summaryJson.path("result");

            StringBuilder sb = new StringBuilder();
            sb.append("PubMed results for: ").append(query)
              .append(" (").append(searchJson.path("esearchresult").path("count").asText()).append(" total)\n\n");

            for (JsonNode id : idList) {
                JsonNode a = articles.path(id.asText());
                if (a.isMissingNode()) continue;
                sb.append("Title: ").append(a.path("title").asText("N/A")).append("\n");
                sb.append("Authors: ").append(authorsFromNode(a.path("authors"))).append("\n");
                sb.append("Journal: ").append(a.path("source").asText("N/A"))
                  .append(" | Year: ").append(a.path("pubdate").asText("N/A")).append("\n");
                sb.append("URL: https://pubmed.ncbi.nlm.nih.gov/").append(a.path("uid").asText()).append("/\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("PubMed search failed for '{}': {}", query, e.getMessage());
            return "PubMed search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search PubChem for chemical compound information: molecular formula, weight, IUPAC name, and structure. Useful for drug compound lookups in pharma contexts.")
    public String searchPubChem(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/" + encoded
                    + "/property/IUPACName,MolecularFormula,MolecularWeight,CanonicalSMILES/JSON?name_type=word";

            HttpResponse<String> resp = get(url);
            if (resp.statusCode() == 404) return "No PubChem compound found for: " + query;

            JsonNode compounds = objectMapper.readTree(resp.body()).path("PropertyTable").path("Properties");
            if (compounds.isEmpty()) return "No PubChem results for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("PubChem compounds for: ").append(query).append("\n\n");

            int count = 0;
            for (JsonNode c : compounds) {
                if (count++ >= 3) break;
                long cid = c.path("CID").asLong();
                sb.append("CID: ").append(cid).append("\n");
                sb.append("IUPAC Name: ").append(c.path("IUPACName").asText("N/A")).append("\n");
                sb.append("Formula: ").append(c.path("MolecularFormula").asText("N/A"))
                  .append(" | Weight: ").append(c.path("MolecularWeight").asText("N/A")).append(" g/mol\n");
                sb.append("URL: https://pubchem.ncbi.nlm.nih.gov/compound/").append(cid).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("PubChem search failed for '{}': {}", query, e.getMessage());
            return "PubChem search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search Reddit for community discussions, opinions, and user experiences about a topic, company, product, or drug. Useful for understanding public sentiment.")
    public String searchReddit(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            JsonNode json = objectMapper.readTree(
                    get("https://www.reddit.com/search.json?q=" + encoded + "&limit=5&sort=relevance&type=link").body());

            JsonNode posts = json.path("data").path("children");
            if (posts.isEmpty()) return "No Reddit results found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("Reddit discussions: ").append(query).append("\n\n");

            for (JsonNode post : posts) {
                JsonNode d = post.path("data");
                sb.append("Title: ").append(d.path("title").asText()).append("\n");
                sb.append("Subreddit: r/").append(d.path("subreddit").asText())
                  .append(" | Score: ").append(d.path("score").asInt())
                  .append(" | Comments: ").append(d.path("num_comments").asInt()).append("\n");
                String body = d.path("selftext").asText("");
                if (!body.isBlank()) sb.append("Content: ").append(truncate(body, 250)).append("\n");
                sb.append("URL: https://reddit.com").append(d.path("permalink").asText()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Reddit search failed for '{}': {}", query, e.getMessage());
            return "Reddit search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search ArXiv for preprint research papers in biology, chemistry, medicine, computer science, and related fields. Useful for finding cutting-edge research before peer-review publication.")
    public String searchArXiv(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpResponse<String> resp = get("https://export.arxiv.org/api/query?search_query=all:" + encoded + "&max_results=5&sortBy=relevance");

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(resp.body().getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList entries = doc.getElementsByTagName("entry");
            if (entries.getLength() == 0) return "No ArXiv results found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("ArXiv papers for: ").append(query).append("\n\n");

            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                sb.append("Title: ").append(getTagText(entry, "title")).append("\n");

                NodeList authors = entry.getElementsByTagName("author");
                if (authors.getLength() > 0) {
                    sb.append("Authors: ");
                    for (int j = 0; j < Math.min(3, authors.getLength()); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(getTagText((Element) authors.item(j), "name"));
                    }
                    if (authors.getLength() > 3) sb.append(" et al.");
                    sb.append("\n");
                }

                String published = getTagText(entry, "published");
                if (published.length() >= 10) sb.append("Published: ").append(published, 0, 10).append("\n");

                String summary = getTagText(entry, "summary").replaceAll("\\s+", " ").trim();
                sb.append("Abstract: ").append(truncate(summary, 300)).append("\n");
                sb.append("URL: ").append(getTagText(entry, "id")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("ArXiv search failed for '{}': {}", query, e.getMessage());
            return "ArXiv search failed: " + e.getMessage();
        }
    }

    // ── Keyed tools — require API key in application-local.yml ───────────────

    @Tool(description = "Search the web for current information using Tavily. Use for recent news, non-Wikipedia topics, or time-sensitive queries. Returns search results with titles, URLs, and content snippets. Requires TAVILY_API_KEY.")
    public String searchWeb(String query) {
        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            return "Tavily web search is not configured. Set app.assistant.tavily-api-key in application-local.yml. "
                    + "Get a free API key at https://tavily.com. Try searchWikipedia instead.";
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "api_key", tavilyApiKey,
                    "query", query,
                    "search_depth", "basic",
                    "max_results", 5,
                    "include_answer", true
            ));

            HttpResponse<String> resp = post("https://api.tavily.com/search", body, "Content-Type", "application/json");
            JsonNode json = objectMapper.readTree(resp.body());

            StringBuilder sb = new StringBuilder();
            String answer = json.path("answer").asText("");
            if (!answer.isBlank()) sb.append("Summary: ").append(answer).append("\n\n");

            JsonNode results = json.path("results");
            if (!results.isEmpty()) {
                sb.append("Sources:\n");
                for (JsonNode r : results) {
                    sb.append("- ").append(r.path("title").asText()).append("\n");
                    sb.append("  ").append(r.path("url").asText()).append("\n");
                    String content = r.path("content").asText("");
                    if (!content.isBlank()) sb.append("  ").append(truncate(content, 300)).append("\n");
                }
            }
            return sb.isEmpty() ? "No Tavily results found for: " + query : sb.toString();
        } catch (Exception e) {
            log.error("Tavily search failed for '{}': {}", query, e.getMessage());
            return "Web search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search for recent news articles about a company, person, drug, or topic. Returns article titles, sources, publication dates, and links. Requires NEWS_API_KEY (free at newsapi.org).")
    public String searchNews(String query) {
        if (newsApiKey == null || newsApiKey.isBlank()) {
            return "News search not configured. Set app.assistant.news-api-key in application-local.yml. Get a free key at https://newsapi.org.";
        }
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://newsapi.org/v2/everything?q=" + encoded
                    + "&pageSize=5&sortBy=relevance&language=en&apiKey=" + newsApiKey;

            JsonNode json = objectMapper.readTree(get(url).body());
            if (!"ok".equals(json.path("status").asText())) {
                return "NewsAPI error: " + json.path("message").asText("Unknown error");
            }

            JsonNode articles = json.path("articles");
            if (articles.isEmpty()) return "No news found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("News for: ").append(query)
              .append(" (").append(json.path("totalResults").asInt()).append(" total)\n\n");

            for (JsonNode a : articles) {
                String published = a.path("publishedAt").asText("");
                sb.append("Title: ").append(a.path("title").asText()).append("\n");
                sb.append("Source: ").append(a.path("source").path("name").asText());
                if (published.length() >= 10) sb.append(" | ").append(published, 0, 10);
                sb.append("\n");
                String desc = a.path("description").asText("");
                if (!desc.isBlank()) sb.append("Description: ").append(truncate(desc, 250)).append("\n");
                sb.append("URL: ").append(a.path("url").asText()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("NewsAPI search failed for '{}': {}", query, e.getMessage());
            return "News search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search the web using Brave Search, an independent privacy-focused search engine. Good complement or alternative to Tavily. Requires BRAVE_API_KEY (free at api.search.brave.com).")
    public String searchBrave(String query) {
        if (braveApiKey == null || braveApiKey.isBlank()) {
            return "Brave Search not configured. Set app.assistant.brave-api-key in application-local.yml. Get a free key at https://api.search.brave.com.";
        }
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.search.brave.com/res/v1/web/search?q=" + encoded + "&count=5"))
                    .header("Accept", "application/json")
                    .header("X-Subscription-Token", braveApiKey)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            JsonNode json = objectMapper.readTree(httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body());
            JsonNode results = json.path("web").path("results");
            if (results.isEmpty()) return "No Brave Search results for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("Brave Search results for: ").append(query).append("\n\n");
            for (JsonNode r : results) {
                sb.append("Title: ").append(r.path("title").asText()).append("\n");
                sb.append("URL: ").append(r.path("url").asText()).append("\n");
                String desc = r.path("description").asText("");
                if (!desc.isBlank()) sb.append("Description: ").append(truncate(desc, 250)).append("\n");
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Brave Search failed for '{}': {}", query, e.getMessage());
            return "Brave Search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search Google via Serper and get organic results, knowledge graph data, and answer boxes. Good complement to Tavily for Google-specific results. Requires SERPER_API_KEY (free tier at serper.dev).")
    public String searchSerper(String query) {
        if (serperApiKey == null || serperApiKey.isBlank()) {
            return "Serper not configured. Set app.assistant.serper-api-key in application-local.yml. Get a free key at https://serper.dev.";
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of("q", query, "num", 5));
            HttpResponse<String> resp = post("https://google.serper.dev/search", body, "Content-Type", "application/json", "X-API-KEY", serperApiKey);
            JsonNode json = objectMapper.readTree(resp.body());

            StringBuilder sb = new StringBuilder();

            JsonNode kg = json.path("knowledgeGraph");
            if (!kg.isMissingNode() && !kg.path("title").asText().isBlank()) {
                sb.append("Knowledge: ").append(kg.path("title").asText())
                  .append(" — ").append(kg.path("description").asText()).append("\n\n");
            }

            JsonNode ab = json.path("answerBox");
            if (!ab.isMissingNode()) {
                String ans = ab.path("answer").asText(ab.path("snippet").asText(""));
                if (!ans.isBlank()) sb.append("Answer: ").append(ans).append("\n\n");
            }

            JsonNode results = json.path("organic");
            if (!results.isEmpty()) {
                sb.append("Google results for: ").append(query).append("\n\n");
                for (JsonNode r : results) {
                    sb.append("Title: ").append(r.path("title").asText()).append("\n");
                    sb.append("URL: ").append(r.path("link").asText()).append("\n");
                    String snippet = r.path("snippet").asText("");
                    if (!snippet.isBlank()) sb.append("Snippet: ").append(truncate(snippet, 250)).append("\n");
                    sb.append("\n");
                }
            }
            return sb.isEmpty() ? "No Serper results for: " + query : sb.toString();
        } catch (Exception e) {
            log.error("Serper search failed for '{}': {}", query, e.getMessage());
            return "Serper search failed: " + e.getMessage();
        }
    }

    @Tool(description = "Search YouTube for videos about a topic, company, drug, conference talk, or event. Returns video titles, channels, and links. Requires YOUTUBE_API_KEY (free from Google Cloud Console).")
    public String searchYouTube(String query) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            return "YouTube search not configured. Set app.assistant.youtube-api-key in application-local.yml. Get a free key from Google Cloud Console.";
        }
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=" + encoded
                    + "&maxResults=5&type=video&key=" + youtubeApiKey;

            JsonNode json = objectMapper.readTree(get(url).body());
            if (json.has("error")) {
                return "YouTube API error: " + json.path("error").path("message").asText();
            }

            JsonNode items = json.path("items");
            if (items.isEmpty()) return "No YouTube videos found for: " + query;

            StringBuilder sb = new StringBuilder();
            sb.append("YouTube videos for: ").append(query).append("\n\n");

            for (JsonNode item : items) {
                JsonNode snippet = item.path("snippet");
                String videoId = item.path("id").path("videoId").asText();
                String published = snippet.path("publishedAt").asText("");
                sb.append("Title: ").append(snippet.path("title").asText()).append("\n");
                sb.append("Channel: ").append(snippet.path("channelTitle").asText());
                if (published.length() >= 10) sb.append(" | ").append(published, 0, 10);
                sb.append("\n");
                String desc = snippet.path("description").asText("");
                if (!desc.isBlank()) sb.append("Description: ").append(truncate(desc, 200)).append("\n");
                sb.append("URL: https://www.youtube.com/watch?v=").append(videoId).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("YouTube search failed for '{}': {}", query, e.getMessage());
            return "YouTube search failed: " + e.getMessage();
        }
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    private HttpResponse<String> get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "BOCRM-Assistant/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, String body, String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "BOCRM-Assistant/1.0")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length - 1; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ── Text helpers ─────────────────────────────────────────────────────────

    private String text(JsonNode node) {
        return text(node, "");
    }

    private String text(JsonNode node, String defaultValue) {
        if (node == null || node.isNull() || node.isMissingNode()) return defaultValue;
        try {
            String v = node.asText(null);
            return v != null ? v : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String firstOf(JsonNode arrayNode) {
        if (arrayNode == null || arrayNode.isMissingNode() || !arrayNode.isArray() || arrayNode.isEmpty()) return "";
        return arrayNode.get(0).asText("");
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s == null ? "" : s;
        return s.substring(0, max) + "...";
    }

    private String authorsFromNode(JsonNode authors) {
        if (authors == null || authors.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (JsonNode author : authors) {
            if (count > 0) sb.append(", ");
            sb.append(author.path("name").asText());
            if (++count >= 3) { sb.append(" et al."); break; }
        }
        return sb.toString();
    }

    private String getTagText(Element element, String tagName) {
        NodeList list = element.getElementsByTagName(tagName);
        return list.getLength() == 0 ? "" : list.item(0).getTextContent().trim();
    }
}
