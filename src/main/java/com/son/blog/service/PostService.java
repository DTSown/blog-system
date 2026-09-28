package com.son.blog.service;

import com.son.blog.dto.PageResponse;
import com.son.blog.dto.PostRequest;
import com.son.blog.dto.PostResponse;
import com.son.blog.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PostService {
    PostResponse createPost(PostRequest request, java.util.List<org.springframework.web.multipart.MultipartFile> files);
    PageResponse<PostResponse> getAllPosts(Specification<Post> spec, Pageable pageable);
    PostResponse getPostById(Long id);
    PostResponse updatePost(Long id, PostRequest request, java.util.List<org.springframework.web.multipart.MultipartFile> files);
    void deletePost(Long id);
    void toggleLikePost(Long id);
}
