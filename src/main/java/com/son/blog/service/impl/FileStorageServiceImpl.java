package com.son.blog.service.impl;

import com.son.blog.dto.AttachmentResponse;
import com.son.blog.entity.Attachment;
import com.son.blog.entity.User;
import com.son.blog.exception.BadRequestException;
import com.son.blog.exception.ResourceNotFoundException;
import com.son.blog.repository.AttachmentRepository;
import com.son.blog.repository.UserRepository;
import com.son.blog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final Tika tika = new Tika();

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    @Override
    public AttachmentResponse uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds limit of 5MB");
        }

        try {
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new BadRequestException("File type not allowed: " + mimeType);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String storedFileName = UUID.randomUUID().toString() + extension;

            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(storedFileName)
                    .toUriString();

            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User uploader = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new BadRequestException("Uploader not found"));

            Attachment attachment = Attachment.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileSize(file.getSize())
                    .fileType(mimeType)
                    .fileUrl(fileDownloadUri)
                    .uploader(uploader)
                    .build();

            Attachment savedAttachment = attachmentRepository.save(attachment);

            return AttachmentResponse.builder()
                    .id(savedAttachment.getId())
                    .fileName(savedAttachment.getOriginalFileName())
                    .fileUrl(savedAttachment.getFileUrl())
                    .fileSize(savedAttachment.getFileSize())
                    .fileType(savedAttachment.getFileType())
                    .createdAt(savedAttachment.getCreatedAt())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new BadRequestException("Invalid file path!");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found " + fileName);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                return;
            }
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // Log warning
        }
    }
}
