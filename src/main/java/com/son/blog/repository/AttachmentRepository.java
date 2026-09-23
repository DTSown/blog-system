package com.son.blog.repository;

import com.son.blog.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByPostIdIsNullAndCreatedAtBefore(LocalDateTime threshold);
}
