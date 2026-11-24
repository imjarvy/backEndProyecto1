package com.uc.ms_security.service;

import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.dto.user.UpdateUserDTO;
import com.uc.ms_security.dto.user.UserResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.PasswordRecoveryTokenRepository;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicePasswordTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final RegistrationService registrationService =
            mock(RegistrationService.class);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                mock(PasswordRecoveryTokenRepository.class),
                userMapper,
                passwordEncoder,
                registrationService
        );
    }

    @Test
    void createUsesRegistrationFlow() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setPassword("ClaveSegura123!");

        User user = new User();
        UserResponseDTO response = new UserResponseDTO(1L, "Ana", "ana@example.com");

        when(registrationService.register(dto)).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(response);

        assertEquals(response, userService.create(dto));

        verify(registrationService).register(dto);
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void updateWithoutPasswordKeepsExistingHash() {
        User user = new User();
        user.setPassword(passwordEncoder.encode("ClaveAnterior123!"));
        String existingHash = user.getPassword();

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(dto.getEmail(), 1L))
                .thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDTO(user))
                .thenReturn(new UserResponseDTO(1L, "Ana", "ana@example.com"));

        userService.update(1L, dto);

        assertEquals(existingHash, user.getPassword());
        verify(userMapper).updateBasicData(dto, user);
    }

    @Test
    void updateWithPasswordStoresNewHash() {
        User user = new User();
        user.setPassword(passwordEncoder.encode("ClaveAnterior123!"));
        String existingHash = user.getPassword();

        UpdateUserDTO dto = new UpdateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setPassword("ClaveNueva123!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(dto.getEmail(), 1L))
                .thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDTO(user))
                .thenReturn(new UserResponseDTO(1L, "Ana", "ana@example.com"));

        userService.update(1L, dto);

        assertNotEquals(existingHash, user.getPassword());
        assertTrue(passwordEncoder.matches(dto.getPassword(), user.getPassword()));
    }
}
