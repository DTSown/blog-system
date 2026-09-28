package com.son.blog.controller.public_api;

import com.son.blog.dto.ApiResponse;
import com.son.blog.dto.PageResponse;
import com.son.blog.dto.PostResponse;
import com.son.blog.entity.Post;
import com.son.blog.repository.specification.PostSpecification;
import com.son.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;
    private final List<String> WHITELISTED_SORT_PROPERTIES = List.of("createdAt", "title", "id");

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getAllPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Specification<Post> spec = Specification.where((root, query, cb) -> cb.conjunction());
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(PostSpecification.searchByTitle(keyword));
        }
        if (authorId != null) {
            spec = spec.and(PostSpecification.hasAuthorId(authorId));
        }

        String sortProperty = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        
        if (!WHITELISTED_SORT_PROPERTIES.contains(sortProperty)) {
            sortProperty = "createdAt";
        }
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        return ResponseEntity.ok(ApiResponse.success(postService.getAllPosts(spec, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(id)));
    }
}
