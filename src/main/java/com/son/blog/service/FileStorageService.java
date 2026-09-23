package com.son.blog.service;

import com.son.blog.dto.AttachmentResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    AttachmentResponse uploadFile(MultipartFile file);
    Resource loadFileAsResource(String fileName);
    void deleteFile(String fileName);
}
