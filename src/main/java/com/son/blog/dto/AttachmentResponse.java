package com.son.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
}
