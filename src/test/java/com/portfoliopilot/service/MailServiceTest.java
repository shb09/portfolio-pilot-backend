package com.portfoliopilot.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Mail must never report success when delivery failed or is unconfigured.
 */
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void unconfiguredMailFailsWith503() {
        MailService service = new MailService(mailSender, "App <n@example.com>", "http://x.test", "");

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.sendWelcome("a@test.com", "asha"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void deliveryFailureFailsWith503() {
        MailService service = new MailService(mailSender, "App <n@example.com>", "http://x.test", "smtp.x.test");
        doThrow(new MailSendException("down")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.sendPasswordReset("a@test.com", "asha", "tok", 60));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
