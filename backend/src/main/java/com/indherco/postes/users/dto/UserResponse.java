package com.indherco.postes.users.dto;

import com.indherco.postes.shared.enums.BaseRole;

public record UserResponse(
    Long id,
    String name,
    String username,
    boolean active,
    BaseRole baseRole,
    boolean canRegisterProduction,
    boolean canRegisterDispatch,
    boolean canRegisterConsumption
) {
}
