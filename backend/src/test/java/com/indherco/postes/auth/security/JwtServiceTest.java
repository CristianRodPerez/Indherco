package com.indherco.postes.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.users.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-characters";

    @Test
    void generatedTokenIsValidForActiveUser() {
        JwtService jwtService = new JwtService(SECRET, 30);
        SecurityUser securityUser = securityUser(true);

        String token = jwtService.generateToken(securityUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.isValid(token, securityUser)).isTrue();
    }

    @Test
    void tokenIsRejectedWhenUserWasDisabled() {
        JwtService jwtService = new JwtService(SECRET, 30);
        SecurityUser activeUser = securityUser(true);
        String token = jwtService.generateToken(activeUser);

        SecurityUser disabledUser = securityUser(false);

        assertThat(jwtService.isValid(token, disabledUser)).isFalse();
    }

    private SecurityUser securityUser(boolean active) {
        User user = new User();
        user.setName("Administrador");
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setBaseRole(BaseRole.ADMIN_OFICINA);
        user.setActive(active);
        return new SecurityUser(user);
    }
}
