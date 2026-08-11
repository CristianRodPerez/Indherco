package com.indherco.postes.users.dto;

import com.indherco.postes.shared.enums.BaseRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotBlank String name,
    @NotNull BaseRole baseRole,
    boolean canRegisterProduction,
    boolean canRegisterDispatch,
    boolean canRegisterConsumption
) {
}
