package com.indherco.postes.users.dto;

import com.indherco.postes.shared.enums.BaseRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @NotBlank String name,
    @NotBlank String username,
    @NotBlank String password,
    @NotNull BaseRole baseRole,
    boolean canRegisterProduction,
    boolean canRegisterDispatch,
    boolean canRegisterConsumption
) {
}
