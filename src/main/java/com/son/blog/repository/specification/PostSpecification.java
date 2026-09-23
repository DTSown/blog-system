package com.son.blog.repository.specification;

import com.son.blog.entity.Post;
import com.son.blog.enums.PostStatus;
import org.springframework.data.jpa.domain.Specification;

public class PostSpecification {

    public static Specification<Post> hasAuthorId(Long authorId) {
        return (root, query, cb) -> authorId == null ? null : cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Post> searchByTitle(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            return cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
        };
    }

    public static Specification<Post> hasStatus(PostStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
