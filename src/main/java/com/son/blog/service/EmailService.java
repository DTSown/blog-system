package com.son.blog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendLikeNotification(String toEmail, String postTitle, String likedByUsername) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Thông báo: Bài viết của bạn được yêu thích!");
        message.setText("Xin chào,\n\n" +
                "Người dùng " + likedByUsername + " vừa mới nhấn yêu thích bài viết '" + postTitle + "' của bạn.\n\n");

        mailSender.send(message);
    }
}
