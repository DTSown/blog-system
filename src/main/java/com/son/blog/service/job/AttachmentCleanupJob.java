package com.son.blog.service.job;

import com.son.blog.entity.Attachment;
import com.son.blog.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttachmentCleanupJob {

    private final AttachmentRepository attachmentRepository;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    // Runs every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOrphanAttachments() {
        log.info("Starting scheduled job: Orphan attachment cleanup");
        
        // Find attachments older than 24 hours that are not linked to any post
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Attachment> orphans = attachmentRepository.findByPostIdIsNullAndCreatedAtBefore(threshold);
        
        int deletedCount = 0;
        for (Attachment attachment : orphans) {
            try {
                Path filePath = fileStorageLocation.resolve(attachment.getStoredFileName());
                Files.deleteIfExists(filePath);
                attachmentRepository.delete(attachment);
                deletedCount++;
                log.debug("Deleted orphan attachment: " + attachment.getStoredFileName());
            } catch (IOException e) {
                log.error("Failed to delete physical file for attachment ID: " + attachment.getId(), e);
            }
        }
        
        log.info("Finished orphan attachment cleanup. Total deleted: " + deletedCount);
    }
}
