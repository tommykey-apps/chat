package com.example.chat.controller;

import com.example.chat.model.dto.SearchResponse;
import com.example.chat.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public Page<SearchResponse> searchAllRooms(@RequestParam String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return searchService.searchAllRooms(q, page, size);
    }
}
