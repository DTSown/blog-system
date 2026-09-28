package com.son.blog.controller.user;

import com.son.blog.dto.AttachmentResponse;
import com.son.blog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/files")
@RequiredArgsConstructor
public class UserFileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<AttachmentResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        AttachmentResponse response = fileStorageService.uploadFile(file);
        return ResponseEntity.ok(response);
    }
}
