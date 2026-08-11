package com.indherco.postes.users;

import com.indherco.postes.audit.AuditService;
import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.shared.exception.ApiException;
import com.indherco.postes.users.dto.CreateUserRequest;
import com.indherco.postes.users.dto.UpdateUserRequest;
import com.indherco.postes.users.dto.UserResponse;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un usuario con ese nombre.");
        }

        User user = new User();
        user.setName(request.name());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        user.setBaseRole(request.baseRole());
        applyPermissions(user, request.baseRole(), request.canRegisterProduction(), request.canRegisterDispatch(), request.canRegisterConsumption());
        User saved = userRepository.save(user);
        auditService.record("USUARIOS", "CREATE", "User", saved.getId(), null, "username=" + saved.getUsername(), null);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getById(id);
        user.setName(request.name());
        user.setBaseRole(request.baseRole());
        applyPermissions(user, request.baseRole(), request.canRegisterProduction(), request.canRegisterDispatch(), request.canRegisterConsumption());
        auditService.record("USUARIOS", "UPDATE", "User", user.getId(), null, "username=" + user.getUsername(), null);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setStatus(Long id, boolean active) {
        User user = getById(id);
        user.setActive(active);
        auditService.record("USUARIOS", "STATUS_CHANGE", "User", user.getId(), null, "active=" + active, null);
        return userMapper.toResponse(user);
    }

    private User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private void applyPermissions(User user, BaseRole baseRole, boolean production, boolean dispatch, boolean consumption) {
        if (baseRole == BaseRole.ADMIN_OFICINA) {
            user.setCanRegisterProduction(true);
            user.setCanRegisterDispatch(true);
            user.setCanRegisterConsumption(true);
            return;
        }
        if (baseRole == BaseRole.OFICINA) {
            user.setCanRegisterProduction(false);
            user.setCanRegisterDispatch(false);
            user.setCanRegisterConsumption(false);
            return;
        }
        user.setCanRegisterProduction(production);
        user.setCanRegisterDispatch(dispatch);
        user.setCanRegisterConsumption(consumption);
    }
}
