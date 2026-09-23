package com.son.blog.dto;

import jakarta.validation.constraints.NotBlank;
import com.son.blog.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private List<Long> attachmentIds;
    
    private PostStatus status;
}
