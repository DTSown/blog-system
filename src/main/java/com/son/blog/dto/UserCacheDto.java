package com.son.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheDto implements Serializable {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String role;
    private boolean deleted;
}
