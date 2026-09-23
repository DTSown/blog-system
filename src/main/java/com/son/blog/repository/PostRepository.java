package com.son.blog.repository;

import com.son.blog.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Query(value = "SELECT id FROM posts WHERE slug = :slug LIMIT 1", nativeQuery = true)
    Long findIdBySlugIncludeDeleted(@Param("slug") String slug);

    @Query(value = "SELECT COUNT(*) FROM post_likes WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    long countLikesByUser(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "INSERT INTO post_likes (post_id, user_id) VALUES (:postId, :userId)", nativeQuery = true)
    void addLike(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM post_likes WHERE post_id = :postId AND user_id = :userId", nativeQuery = true)
    void removeLike(@Param("postId") Long postId, @Param("userId") Long userId);
}
