package com.son.blog.service.impl;

import com.son.blog.dto.AttachmentResponse;
import com.son.blog.dto.PageResponse;
import com.son.blog.dto.PostRequest;
import com.son.blog.dto.PostResponse;
import com.son.blog.dto.UserSummary;
import com.son.blog.entity.Attachment;
import com.son.blog.entity.Post;
import com.son.blog.entity.User;
import com.son.blog.enums.ErrorCode;
import com.son.blog.enums.PostStatus;
import com.son.blog.exception.AppException;
import com.son.blog.repository.AttachmentRepository;
import com.son.blog.repository.PostRepository;
import com.son.blog.repository.UserRepository;
import com.son.blog.service.PostService;
import com.son.blog.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final com.son.blog.service.kafka.KafkaProducerService kafkaProducerService;
    private final com.son.blog.service.FileStorageService fileStorageService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isCurrentUserSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = getCurrentUser();

        PostStatus status = request.getStatus() != null ? request.getStatus() : PostStatus.PUBLISHED;

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle(), null))
                .status(status)
                .content(request.getContent())
                .author(currentUser)
                .build();

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            List<Attachment> attachments = attachmentRepository.findAllById(request.getAttachmentIds());
            for (Attachment attachment : attachments) {
                if (!attachment.getUploader().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not the owner of attachment ID: " + attachment.getId());
                }
                if (attachment.getPost() != null) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED);
                }
                post.addAttachment(attachment);
            }
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getAllPosts(Specification<Post> spec, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), PostStatus.PUBLISHED));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deleted"), false));
        } else {
            if (!isCurrentUserSuperAdmin()) {
                User currentUser = getCurrentUser();
                Specification<Post> statusSpec = (root, query, cb) -> cb.or(
                        cb.equal(root.get("status"), PostStatus.PUBLISHED),
                        cb.equal(root.get("author").get("id"), currentUser.getId())
                );
                spec = spec.and(statusSpec);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("deleted"), false));
            }
        }

        Page<Post> page = postRepository.findAll(spec, pageable);
        List<PostResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<PostResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
                
        if (post.isDeleted() && !isCurrentUserSuperAdmin()) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }
                
        // Only author or admin can see draft or private posts
        if (post.getStatus() == PostStatus.DRAFT || post.getStatus() == PostStatus.PRIVATE) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                throw new AppException(ErrorCode.POST_NOT_FOUND);
            }
            User currentUser = getCurrentUser();
            if (!post.getAuthor().getId().equals(currentUser.getId()) && !isCurrentUserSuperAdmin()) {
                throw new AppException(ErrorCode.POST_NOT_FOUND);
            }
        }
                
        return mapToResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!post.getTitle().equals(request.getTitle())) {
            post.setSlug(generateUniqueSlug(request.getTitle(), id));
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }

        // Update attachments (simple replace strategy for now)
        if (request.getAttachmentIds() != null) {
            // Unlink current attachments not in new list
            List<Attachment> currentAttachments = post.getAttachments();
            currentAttachments.removeIf(att -> {
                boolean shouldRemove = !request.getAttachmentIds().contains(att.getId());
                if (shouldRemove) {
                    fileStorageService.deleteFile(att.getStoredFileName());
                }
                return shouldRemove;
            });
            
            // Link new attachments
            List<Attachment> requestedAttachments = attachmentRepository.findAllById(request.getAttachmentIds());
            for (Attachment attachment : requestedAttachments) {
                if (!attachment.getUploader().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not the owner of attachment ID: " + attachment.getId());
                }
                if (attachment.getPost() != null && !attachment.getPost().getId().equals(post.getId())) {
                    throw new AppException(ErrorCode.VALIDATION_FAILED);
                }
                if (attachment.getPost() == null) {
                    post.addAttachment(attachment);
                }
            }
        }

        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isCurrentUserSuperAdmin()) {
            throw new AccessDeniedException("Only the author or Super Admin can delete this post");
        }

        post.setDeleted(true);
        post.setDeletedAt(java.time.LocalDateTime.now());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            post.setDeletedBy(auth.getName());
        }

        postRepository.save(post);
    }

    @Override
    @Transactional
    public void toggleLikePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        if (post.isDeleted()) {
            throw new AppException(ErrorCode.POST_NOT_FOUND);
        }

        User currentUser = getCurrentUser();
        boolean isLiked = postRepository.countLikesByUser(id, currentUser.getId()) > 0;

        if (isLiked) {
            postRepository.removeLike(id, currentUser.getId());
        } else {
            try {
                postRepository.addLike(id, currentUser.getId());
                try {
                    String authorEmail = (post.getAuthor() != null) ? post.getAuthor().getEmail() : null;
                    kafkaProducerService.sendLikeEvent(id, currentUser.getUsername(), authorEmail, post.getTitle());
                } catch (Exception ex) {
                    // Prevent author fetching or Kafka issues from failing the transaction
                }
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Ignore, means it was already liked concurrently
            }
        }
    }
    
    private String generateUniqueSlug(String title, Long currentId) {
        String baseSlug = SlugUtil.toSlug(title);
        String slug = baseSlug;
        int count = 1;
        while (true) {
            Long existingId = postRepository.findIdBySlugIncludeDeleted(slug);
            if (existingId == null || existingId.equals(currentId)) {
                return slug;
            }
            slug = baseSlug + "-" + count++;
        }
    }

    private PostResponse mapToResponse(Post post) {
        UserSummary author = UserSummary.builder()
                .id(post.getAuthor().getId())
                .username(post.getAuthor().getUsername())
                .email(post.getAuthor().getEmail())
                .build();

        List<AttachmentResponse> attachments = post.getAttachments().stream()
                .map(att -> AttachmentResponse.builder()
                        .id(att.getId())
                        .fileName(att.getOriginalFileName())
                        .fileUrl(att.getFileUrl())
                        .fileSize(att.getFileSize())
                        .fileType(att.getFileType())
                        .createdAt(att.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .status(post.getStatus())
                .content(post.getContent())
                .author(author)
                .attachments(attachments)
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deleted(post.isDeleted())
                .deletedAt(post.getDeletedAt())
                .deletedBy(post.getDeletedBy())
                .build();
    }
}
