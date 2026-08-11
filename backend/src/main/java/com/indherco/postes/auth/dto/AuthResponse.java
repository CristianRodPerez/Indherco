package com.indherco.postes.auth.dto;

import com.indherco.postes.users.dto.UserResponse;

public record AuthResponse(
    String token,
    UserResponse user
) {
}
