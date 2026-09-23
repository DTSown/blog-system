package com.son.blog.controller;

import com.son.blog.dto.ApiResponse;
import com.son.blog.dto.PageResponse;
import com.son.blog.dto.PostRequest;
import com.son.blog.dto.PostResponse;
import com.son.blog.entity.Post;
import com.son.blog.repository.specification.PostSpecification;
import com.son.blog.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final List<String> WHITELISTED_SORT_PROPERTIES = List.of("createdAt", "title", "id");

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody PostRequest request) {
        return new ResponseEntity<>(ApiResponse.success(postService.createPost(request)), HttpStatus.CREATED);
    }

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
            sortProperty = "createdAt"; // fallback to safe sort property
        }
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        return ResponseEntity.ok(ApiResponse.success(postService.getAllPosts(spec, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPostById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(ApiResponse.success(postService.updatePost(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Post deleted successfully"));
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> toggleLikePost(@PathVariable Long id) {
        postService.toggleLikePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Post like toggled successfully"));
    }
}
