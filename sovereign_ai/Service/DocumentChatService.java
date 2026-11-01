

package com.example.sovereign_ai.Service;

import com.example.sovereign_ai.entity.Conversation;
import com.example.sovereign_ai.entity.Document;
import com.example.sovereign_ai.entity.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentChatService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentChatService.class);

    private static final int TOP_K = 6;

    private final VectorStore vectorStore;

    public DocumentChatService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // Backward-compatible method
    public RetrievalResult retrieve(
            Conversation conversation,
            String question
    ) {
        ScoredRetrievalResult result =
                retrieveScored(conversation, question);

        return new RetrievalResult(
                result.context(),
                result.sources()
        );
    }

    // Semantic retrieval with Qdrant scores
    public ScoredRetrievalResult retrieveScored(
            Conversation conversation,
            String question
    ) {

        Set<Document> attached = conversation.getDocuments();

        if (attached == null || attached.isEmpty()) {
            return new ScoredRetrievalResult(
                    "",
                    List.of(),
                    List.of()
            );
        }

        List<String> documentIds = attached.stream()
                .map(Document::getId)
                .map(String::valueOf)
                .distinct()
                .toList();

        // Qdrant documentId metadata is stored as String.
        String filter;

        if (documentIds.size() == 1) {
            filter = "documentId == '"
                    + documentIds.get(0) + "'";
        } else {
            String quotedIds = documentIds.stream()
                    .map(id -> "'" + id + "'")
                    .collect(Collectors.joining(", "));

            filter = "documentId IN [" + quotedIds + "]";
        }

        log.info("PDF semantic search: {}", question);
        log.info("Qdrant filter: {}", filter);

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .filterExpression(filter)
                .build();

        List<org.springframework.ai.document.Document> matches =
                vectorStore.similaritySearch(request);

        if (matches == null || matches.isEmpty()) {
            return new ScoredRetrievalResult(
                    "",
                    List.of(),
                    List.of()
            );
        }

        StringBuilder context = new StringBuilder();

        List<Source> sources = new ArrayList<>();
        List<ScoredPassage> passages = new ArrayList<>();

        Set<String> seen = new LinkedHashSet<>();

        int number = 1;

        for (org.springframework.ai.document.Document match : matches) {

            String text = match.getText();

            if (text == null || text.isBlank()) {
                continue;
            }

            Map<String, Object> metadata = match.getMetadata();

            String fileName = String.valueOf(
                    metadata.getOrDefault(
                            "fileName",
                            "Unknown PDF"
                    )
            );

            Integer page = toInteger(
                    metadata.get("pageNumber")
            );

            Integer chunk = toInteger(
                    metadata.get("chunkIndex")
            );

            Double score = match.getScore();

            String citationId = "PDF-" + number++;

            Source source = page == null
                    ? null
                    : new Source(fileName, chunk, page);

            String key = fileName + ":" + page + ":" + chunk;

            if (source != null && seen.add(key)) {
                sources.add(source);
            }

            passages.add(
                    new ScoredPassage(
                            citationId,
                            text,
                            score,
                            source
                    )
            );

            context.append("\n[")
                    .append(citationId)
                    .append("]\nFile: ")
                    .append(fileName)
                    .append("\nPage: ")
                    .append(page == null ? "Unknown" : page)
                    .append("\nContent:\n")
                    .append(text)
                    .append("\n");

            log.info(
                    "PDF match: file={}, page={}, score={}",
                    fileName,
                    page,
                    score
            );
        }

        return new ScoredRetrievalResult(
                context.toString(),
                sources,
                passages
        );
    }

    private Integer toInteger(Object value) {

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    public record RetrievalResult(
            String context,
            List<Source> sources
    ) {}

    public record ScoredPassage(
            String citationId,
            String text,
            Double score,
            Source source
    ) {}

    public record ScoredRetrievalResult(
            String context,
            List<Source> sources,
            List<ScoredPassage> passages
    ) {}
}
