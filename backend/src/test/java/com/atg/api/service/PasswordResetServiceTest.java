package com.atg.api.service;

import com.atg.api.model.PasswordResetCode;
import com.atg.api.model.User;
import com.atg.api.repo.PasswordResetCodeRepository;
import com.atg.api.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordResetCodeRepository resetCodes;
    @Mock
    private PasswordEncoder passwords;
    @Mock
    private JavaMailSender mailSender;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                users, resetCodes, passwords, mailSender, "smtp.example.com", "no-reply@example.com");
    }

    @Test
    void sendsAHashedOneTimeCodeAndResetsPassword() {
        User user = new User();
        user.setId(42L);
        user.setEmail("trader@example.com");
        AtomicReference<PasswordResetCode> storedCode = new AtomicReference<>();
        when(users.findByEmailIgnoreCase("trader@example.com")).thenReturn(Optional.of(user));
        when(resetCodes.findById(42L)).thenAnswer(ignored -> Optional.ofNullable(storedCode.get()));
        when(resetCodes.save(any(PasswordResetCode.class))).thenAnswer(invocation -> {
            PasswordResetCode code = invocation.getArgument(0);
            storedCode.set(code);
            return code;
        });
        when(passwords.encode("new-password")).thenReturn("bcrypt-hash");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        service.requestCode("trader@example.com");

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        Matcher matcher = Pattern.compile("(?<!\\d)\\d{8}(?!\\d)").matcher(mail.getValue().getText());
        assertThat(matcher.find()).isTrue();
        String code = matcher.group();
        assertThat(storedCode.get().getCodeHash()).isNotEqualTo(code);
        assertThat(storedCode.get().getExpiresAt())
                .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(14));

        service.resetPassword("trader@example.com", code, "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(users).save(user);
        verify(resetCodes).deleteById(42L);
    }

    @Test
    void unknownEmailDoesNotSendMailOrCreateResetCode() {
        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        service.requestCode("nobody@example.com");

        verifyNoInteractions(resetCodes, mailSender);
    }

    @Test
    void wrongCodeConsumesAnAttempt() {
        User user = new User();
        user.setId(42L);
        user.setEmail("trader@example.com");
        PasswordResetCode stored = new PasswordResetCode();
        stored.setId(42L);
        stored.setCodeHash("not-the-code");
        stored.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
        when(users.findByEmailIgnoreCase("trader@example.com")).thenReturn(Optional.of(user));
        when(resetCodes.findById(42L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() ->
                service.resetPassword("trader@example.com", "00000000", "new-password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid or expired reset code");

        assertThat(stored.getFailedAttempts()).isEqualTo(1);
        verify(resetCodes).save(stored);
    }
}
