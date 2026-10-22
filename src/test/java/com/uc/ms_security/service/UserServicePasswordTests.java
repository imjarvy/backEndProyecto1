package com.uc.ms_security.service;

import com.uc.ms_security.dto.user.CreateUserDTO;
import com.uc.ms_security.dto.user.UpdateUserDTO;
import com.uc.ms_security.dto.user.UserResponseDTO;
import com.uc.ms_security.entity.User;
import com.uc.ms_security.mapper.UserMapper;
import com.uc.ms_security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicePasswordTests {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    void createStoresEncodedPassword() {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setName("Ana");
        dto.setEmail("ana@example.com");
        dto.setPassword("ClaveSegura123!");

        User user = new User();
        UserResponseDTO response = new UserResponseDTO(1L, "Ana", "ana@example.com");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(eq(dto), anyString())).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(response);

        userService.create(dto);

        verify(userMapper).toEntity(
                eq(dto),
                org.mockito.ArgumentMatchers.argThat(encodedPassword ->
                        !dto.getPassword().equals(encodedPassword)
                                && passwordEncoder.matches(
                                        dto.getPassword(),
                                        encodedPassword
                                )
                )
        );
        verify(userRepository).save(user);
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
