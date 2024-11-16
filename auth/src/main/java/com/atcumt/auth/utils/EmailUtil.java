package com.atcumt.auth.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailUtil {
    private final JavaMailSender mailSender;

    /**
     * 发送 HTML 格式邮件
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param content 邮件正文（可以HTML 格式）
     * @throws MessagingException 消息异常
     */
    public void sendEmail(String to, String subject, String content, boolean isHTML) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, isHTML); // 第二个参数为 true 表示发送 HTML
        helper.setFrom("wotemo@qq.com");
        mailSender.send(mimeMessage);
    }

    /**
     * 发送 HTML 格式邮件
     *
     * @param from    发件人地址
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param html    邮件正文（可以HTML 格式）
     * @throws MessagingException 消息异常
     */
    public void sendEmail(String from, String to, String subject, String html, boolean isHTML) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, isHTML); // 第二个参数为 true 表示发送 HTML
        helper.setFrom(from);
        mailSender.send(mimeMessage);
    }
}
