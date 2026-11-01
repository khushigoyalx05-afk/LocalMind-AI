

package com.example.sovereign_ai.Tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class WebSearchTool {

    private static final Logger log =
            LoggerFactory.getLogger(WebSearchTool.class);

    private static final int MAX_RESULTS = 5;
    private static final int MAX_PAGE_CHARS = 8000;
    private static final int MAX_SNIPPET_CHARS = 1200;

    private static final Set<String> OFFICIAL_DOMAINS =
            Set.of(
                    "apple.com",
                    "microsoft.com",
                    "google.com",
                    "samsung.com",
                    "openai.com",
                    "github.com",
                    "oracle.com"
            );

    private final RestClient searchClient;
    private final ObjectMapper objectMapper;

    public WebSearchTool(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${localmind.search.url:http://localhost:8888}")
            String searchUrl
    ) {
        this.searchClient = builder
                .baseUrl(searchUrl)
                .build();

        this.objectMapper = objectMapper;
    }

    // =====================================================
    // LIVE WEB SEARCH
    // =====================================================

    @Tool(description = """
            Search the live internet for current information,
            product prices, launch dates, software releases,
            news and recent events.

            Returns source URLs, retrieved page content
            and retrieval status.

            Retrieved content is evidence, not guaranteed truth.
            """)
    public String searchWeb(
            @ToolParam(description = "Web search query")
            String query
    ) {

        if (query == null || query.isBlank()) {
            return "WEB_SEARCH_ERROR: Empty query.";
        }

        final String trimmed = query.trim();

        final String safeQuery =
                trimmed.length() > 300
                        ? trimmed.substring(0, 300)
                        : trimmed;

        log.info("WEB SEARCH EXECUTED: {}", safeQuery);

        try {

            String response = searchClient.get()
                    .uri(builder -> builder
                            .path("/search")
                            .queryParam("q", safeQuery)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                return "WEB_SEARCH_ERROR: Empty SearXNG response.";
            }

            log.info("SearXNG returned {} response characters", response.length());
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                log.warn("SearXNG returned zero results for query: {}", safeQuery);
                return "WEB_SEARCH_ERROR: No search results. Check SearXNG engines and JSON configuration.";
            }
            log.info("SearXNG returned {} search results", results.size());

            List<String> evidence = new ArrayList<>();

            for (JsonNode result : results) {

                if (evidence.size() >= MAX_RESULTS) {
                    break;
                }

                String title =
                        result.path("title").asText("");

                String url =
                        result.path("url").asText("");

                String snippet =
                        result.path("content").asText("");

                if (!isAllowedPublicUrl(url)) {
                    continue;
                }

                String classification = classifySource(url);

                String pageText = fetchPageText(url);

                boolean pageRetrieved =
                        pageText != null && !pageText.isBlank();

                String verificationStatus =
                        pageRetrieved
                                ? "PAGE_RETRIEVED"
                                : "PAGE_NOT_VERIFIED";

                String content =
                        pageRetrieved
                                ? pageText
                                : "Page retrieval failed. Search snippet (unverified): "
                                  + (snippet.length() > MAX_SNIPPET_CHARS
                                     ? snippet.substring(0, MAX_SNIPPET_CHARS)
                                     : snippet);

                // Prevent retrieved text from injecting
                // delimiters into the downstream parser.
                content = sanitizeContent(content);

                if (!pageRetrieved && snippet.isBlank()) {
                    log.debug("Skipping source with no page text or snippet: {}", url);
                    continue;
                }

                evidence.add("""
                        SOURCE_START
                        SOURCE_TITLE: %s
                        SOURCE_URL: %s
                        SOURCE_TYPE: %s
                        FETCH_STATUS: %s
                        FETCHED_AT: %s
                        CONTENT_START
                        %s
                        CONTENT_END
                        SOURCE_END
                        """.formatted(
                        sanitizeContent(title),
                        url,
                        classification,
                        verificationStatus,
                        Instant.now(),
                        content
                ));
            }

            if (evidence.isEmpty()) {
                return "WEB_SEARCH_ERROR: No usable public sources.";
            }

            log.info(
                    "Web search completed with {} sources",
                    evidence.size()
            );

            return String.join("\n\n", evidence);

        } catch (Exception e) {

            log.error("SearXNG request failed for query: {}", safeQuery, e);

            return "WEB_SEARCH_ERROR: SearXNG request failed ("
                    + e.getClass().getSimpleName() + "): " + e.getMessage();
        }
    }

    // =====================================================
    // FETCH SOURCE PAGE
    // =====================================================

    private String fetchPageText(String url) {

        try {

            if (!isAllowedPublicUrl(url)) {
                return null;
            }

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 LocalMindAI/1.0")
                    .timeout(5000)
                    .maxBodySize(500_000)
                    .followRedirects(false)
                    .get();

            document.select(
                    "script, style, nav, footer, header, "
                            + "aside, noscript"
            ).remove();

            String text =
                    document.body() == null
                            ? ""
                            : document.body().text();

            if (text.isBlank()) {
                return null;
            }

            return text.length() > MAX_PAGE_CHARS
                    ? text.substring(0, MAX_PAGE_CHARS)
                    : text;

        } catch (Exception e) {

            log.warn(
                    "Unable to retrieve source page: {} ({})",
                    url,
                    e.getMessage()
            );

            return null;
        }
    }

    // =====================================================
    // SOURCE CLASSIFICATION
    // =====================================================

    private String classifySource(String url) {

        try {

            String host = URI.create(url)
                    .getHost()
                    .toLowerCase(Locale.ROOT);

            for (String domain : OFFICIAL_DOMAINS) {

                if (host.equals(domain)
                        || host.endsWith("." + domain)) {
                    return "OFFICIAL_DOMAIN";
                }
            }

            return "THIRD_PARTY_DOMAIN";

        } catch (Exception e) {
            return "UNVERIFIED_DOMAIN";
        }
    }

    // =====================================================
    // SANITIZE CONTENT
    // =====================================================

    private String sanitizeContent(String content) {

        if (content == null) {
            return "";
        }

        return content
                .replace("SOURCE_START", "[SOURCE START]")
                .replace("SOURCE_END", "[SOURCE END]")
                .replace("CONTENT_START", "[CONTENT START]")
                .replace("CONTENT_END", "[CONTENT END]");
    }

    // =====================================================
    // BLOCK PRIVATE / LOCAL URLS
    // =====================================================

    private boolean isAllowedPublicUrl(String url) {

        try {

            URI uri = URI.create(url);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return false;
            }

            if (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https")) {
                return false;
            }

            if (uri.getUserInfo() != null) {
                return false;
            }

            InetAddress[] addresses =
                    InetAddress.getAllByName(host);

            for (InetAddress address : addresses) {

                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}

