package com.portfoliopilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Transactional mail. SMTP comes from env (SPRING_MAIL_*); when no host is
 * configured every send fails fast with 503 — we never pretend mail went out.
 * Passwords, JWTs and raw tokens are never logged here.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String baseUrl;
    private final boolean configured;

    public MailService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.base-url}") String baseUrl,
            @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.configured = host != null && !host.isBlank();
    }

    public boolean isConfigured() {
        return configured;
    }

    public void sendVerification(String toEmail, String username, String rawToken, long ttlHours) {
        String link = baseUrl + "/verify?token=" + rawToken;
        String body = "Hi " + username + ",\n\n"
                + "Your Portfolio Pilot account has been created. Verify your email within " + ttlHours
                + " hours to activate it:\n\n" + link + "\n\n"
                + "Your username is: " + username + "\n"
                + "After verification, sign in with your username or email address and password.\n\n"
                + "If you did not register, ignore this email — the account stays inactive.\n\n"
                + "— Portfolio Pilot";
        send(toEmail, "Verify your Portfolio Pilot account", body);
    }

    public void sendVerifiedConfirmation(String toEmail, String username) {
        String body = "Hi " + username + ",\n\n"
                + "Your email is verified — your Portfolio Pilot account is ready.\n"
                + "Sign in with your username (" + username + ") or email address.\n\n"
                + "— Portfolio Pilot";
        send(toEmail, "Your Portfolio Pilot account is ready", body);
    }

    /** Best-effort welcome mail for immediate-login registration. */
    public void sendWelcome(String toEmail, String username) {
        String body = "Hi " + username + ",\n\n"
                + "Welcome to Portfolio Pilot — your account is ready.\n"
                + "Sign in any time with your username (" + username + ") or email address.\n\n"
                + "— Portfolio Pilot";
        send(toEmail, "Welcome to Portfolio Pilot", body);
    }

    public void sendPasswordReset(String toEmail, String username, String rawToken, long ttlMinutes) {
        String link = baseUrl + "/reset?token=" + rawToken;
        String body = "Hi " + username + ",\n\n"
                + "A password reset was requested for your Portfolio Pilot account. Use this link within "
                + ttlMinutes + " minutes:\n\n" + link + "\n\n"
                + "If you did not request this, ignore this email — your password is unchanged.\n\n"
                + "— Portfolio Pilot";
        send(toEmail, "Reset your Portfolio Pilot password", body);
    }

    private void send(String to, String subject, String body) {
        if (!configured) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is not configured. Please try again later.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Mail sent: subject='{}' to domain='{}'", subject, domainOf(to));
        } catch (MailException e) {
            log.warn("Mail delivery failed: subject='{}' to domain='{}': {}", subject, domainOf(to), e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email could not be delivered. Please try again later.");
        }
    }

    private String domainOf(String email) {
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1) : "unknown";
    }
}
