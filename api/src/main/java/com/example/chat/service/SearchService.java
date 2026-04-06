package com.example.chat.service;

import com.example.chat.model.dto.MessageResponse;
import com.example.chat.model.dto.SearchResponse;
import com.example.chat.model.entity.ChatMessage;
import com.example.chat.model.entity.ChatMessageDocument;
import com.example.chat.repository.ChatMessageSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final ChatMessageSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public SearchService(ChatMessageSearchRepository searchRepository,
                         ElasticsearchOperations elasticsearchOperations) {
        this.searchRepository = searchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void indexMessage(ChatMessage message) {
        searchRepository.save(new ChatMessageDocument(message));
    }

    public Page<MessageResponse> searchMessages(UUID roomId, String query, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return searchRepository
                .findByRoomIdAndContentContaining(roomId.toString(), query, pageable)
                .map(doc -> new MessageResponse(
                        UUID.fromString(doc.getId()),
                        doc.getSenderId(),
                        doc.getSenderName(),
                        doc.getContent(),
                        doc.getMessageType(),
                        doc.getCreatedAt()
                ));
    }

    public Page<SearchResponse> searchAllRooms(String query, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var highlight = new Highlight(List.of(new HighlightField("content")));

        var nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m.field("content").query(query)))
                .withHighlightQuery(new HighlightQuery(highlight, null))
                .withPageable(pageable)
                .build();

        SearchHits<ChatMessageDocument> hits = elasticsearchOperations.search(
                nativeQuery, ChatMessageDocument.class);

        var results = hits.getSearchHits().stream()
                .map(this::toSearchResponse)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    private SearchResponse toSearchResponse(SearchHit<ChatMessageDocument> hit) {
        var doc = hit.getContent();
        var highlightedContent = hit.getHighlightFields().containsKey("content")
                ? String.join(" ... ", hit.getHighlightFields().get("content"))
                : doc.getContent();

        return new SearchResponse(
                UUID.fromString(doc.getId()),
                UUID.fromString(doc.getRoomId()),
                doc.getSenderId(),
                doc.getSenderName(),
                doc.getContent(),
                highlightedContent,
                doc.getMessageType(),
                doc.getCreatedAt()
        );
    }
}
