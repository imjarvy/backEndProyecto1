package com.uc.ms_security.dto.session;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class SessionResponseDTO {

    Long id;

    LocalDateTime expiration;
}