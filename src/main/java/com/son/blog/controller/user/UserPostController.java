package com.son.blog.controller.user;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/posts")
@RequiredArgsConstructor
public class UserPostController {

    private final PostService postService;
    private final List<String> WHITELISTED_SORT_PROPERTIES = List.of("createdAt", "title", "id");

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart("post") @Valid PostRequest request,
            @RequestPart(value = "files", required = false) List<org.springframework.web.multipart.MultipartFile> files) {
        return new ResponseEntity<>(ApiResponse.success(postService.createPost(request, files)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getMyPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        // User gets their own posts or we can just let PostService filter based on user context
        // By calling getAllPosts with no authorId, PostService already filters by current user's visibility
        Specification<Post> spec = Specification.where((root, query, cb) -> cb.conjunction());
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(PostSpecification.searchByTitle(keyword));
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

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @RequestPart("post") @Valid PostRequest request,
            @RequestPart(value = "files", required = false) List<org.springframework.web.multipart.MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(postService.updatePost(id, request, files)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Post deleted successfully"));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<String>> toggleLikePost(@PathVariable Long id) {
        postService.toggleLikePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Post like toggled successfully"));
    }
}
