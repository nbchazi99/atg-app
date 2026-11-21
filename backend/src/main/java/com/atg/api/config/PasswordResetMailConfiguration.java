package com.atg.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class PasswordResetMailConfiguration {

    @Bean
    public JavaMailSender passwordResetMailSender(
            @Value("${app.mail.host:}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.smtp-auth:true}") boolean smtpAuth,
            @Value("${app.mail.smtp-starttls:true}") boolean startTls,
            @Value("${app.mail.smtp-ssl:false}") boolean ssl) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", Boolean.toString(smtpAuth));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        mailProperties.put("mail.smtp.starttls.required", Boolean.toString(startTls));
        mailProperties.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        mailProperties.put("mail.smtp.connectiontimeout", "10000");
        mailProperties.put("mail.smtp.timeout", "10000");
        mailProperties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }
}
