package com.indherco.postes.users;

import com.indherco.postes.users.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.isActive(),
            user.getBaseRole(),
            user.isCanRegisterProduction(),
            user.isCanRegisterDispatch(),
            user.isCanRegisterConsumption()
        );
    }
}
