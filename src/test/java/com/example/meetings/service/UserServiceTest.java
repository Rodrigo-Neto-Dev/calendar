package com.example.meetings.service;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "encodedPassword");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User registered = userService.register("testuser", "test@example.com", "rawPassword");

        assertNotNull(registered);
        assertEquals("testuser", registered.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_UsernameTaken_ThrowsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                userService.register("testuser", "test@example.com", "rawPassword")
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_NullUsername_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register(null, "e@e.com", "pass")
        );
        assertEquals("Username cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_EmptyUsername_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("", "e@e.com", "pass")
        );
        assertEquals("Username cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_BlankUsername_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("   ", "e@e.com", "pass")
        );
        assertEquals("Username cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_NullEmail_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", null, "pass")
        );
        assertEquals("Email cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_EmptyEmail_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "", "pass")
        );
        assertEquals("Email cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_BlankEmail_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "   ", "pass")
        );
        assertEquals("Email cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_NullPassword_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "e@e.com", null)
        );
        assertEquals("Password cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_EmptyPassword_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "e@e.com", "")
        );
        assertEquals("Password cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_BlankPassword_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.register("user", "e@e.com", "   ")
        );
        assertEquals("Password cannot be null or empty", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void requireByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User found = userService.requireByUsername("testuser");

        assertEquals(testUser, found);
    }

    @Test
    void requireByUsername_NotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.requireByUsername("unknown"));
    }
}