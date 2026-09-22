package com.son.blog.security;

import com.son.blog.entity.User;
import com.son.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cacheKey = "userCache:" + username;
        
        com.son.blog.dto.UserCacheDto userCache = (com.son.blog.dto.UserCacheDto) redisTemplate.opsForValue().get(cacheKey);
        User user = null;
        
        if (userCache != null) {
            user = User.builder()
                    .id(userCache.getId())
                    .username(userCache.getUsername())
                    .password(userCache.getPassword())
                    .email(userCache.getEmail())
                    .role(userCache.getRole())
                    .deleted(userCache.isDeleted())
                    .build();
        } else {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
                    
            userCache = com.son.blog.dto.UserCacheDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .deleted(user.isDeleted())
                    .build();
                    
            // Cache user for 10 minutes
            redisTemplate.opsForValue().set(cacheKey, userCache, 10, java.util.concurrent.TimeUnit.MINUTES);
        }
        
        return new CustomUserDetails(user);
    }
}
