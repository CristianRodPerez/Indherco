package com.indherco.postes.auth;

import com.indherco.postes.auth.security.SecurityUser;
import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.users.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesion.");
        }
        return securityUser.getUser();
    }

    public boolean isAdmin() {
        return getCurrentUser().getBaseRole() == BaseRole.ADMIN_OFICINA;
    }
}
