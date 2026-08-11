package com.indherco.postes.auth;

import com.indherco.postes.auth.dto.AuthResponse;
import com.indherco.postes.auth.dto.LoginRequest;
import com.indherco.postes.auth.security.JwtService;
import com.indherco.postes.auth.security.SecurityUser;
import com.indherco.postes.audit.AuditService;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.users.User;
import com.indherco.postes.users.UserMapper;
import com.indherco.postes.users.UserRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final int maxFailedAttempts;
    private final int lockMinutes;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserMapper userMapper,
        UserRepository userRepository,
        AuditService auditService,
        @Value("${app.login.max-failed-attempts}") int maxFailedAttempts,
        @Value("${app.login.lock-minutes}") int lockMinutes
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElse(null);
        ensureNotLocked(user);

        var authentication = authenticate(request, user);
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        recordSuccessfulLogin(securityUser.getUser());
        auditService.record("AUTH", "LOGIN_SUCCESS", "User", securityUser.getUser().getId(), null, "username=" + securityUser.getUsername(), null);
        return new AuthResponse(jwtService.generateToken(securityUser), userMapper.toResponse(securityUser.getUser()));
    }

    private org.springframework.security.core.Authentication authenticate(LoginRequest request, User user) {
        try {
            return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException exception) {
            recordFailedLogin(user);
            auditService.record("AUTH", "LOGIN_FAILED", "User", user == null ? null : user.getId(), null, "username=" + request.username(), null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Usuario o contrasena incorrectos.");
        }
    }

    private void ensureNotLocked(User user) {
        if (user == null || user.getLockedUntil() == null) {
            return;
        }
        if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.LOCKED, "USER_TEMPORARILY_LOCKED", "Usuario temporalmente bloqueado. Intente nuevamente mas tarde.");
        }
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
    }

    private void recordSuccessfulLogin(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
    }

    private void recordFailedLogin(User user) {
        if (user == null) {
            return;
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }
    }
}
