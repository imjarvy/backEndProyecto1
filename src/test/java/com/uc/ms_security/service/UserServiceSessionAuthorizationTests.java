package com.uc.ms_security.service;

import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class UserServiceSessionAuthorizationTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserService userService = new UserService(
            userRepository,
            userMapper,
            passwordEncoder
    );

    @Test
    void rejectsSessionDetailsForAnotherUser() {
        assertThrows(
                AccessDeniedException.class,
                () -> userService.findByIdAndSessions(22L, 11L)
        );

        verifyNoInteractions(userRepository, userMapper);
    }
}
