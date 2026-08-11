package com.indherco.postes.auth.security;

import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.shared.enums.Permission;
import com.indherco.postes.users.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUser implements UserDetails {

    private final User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getBaseRole().name()));
        permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.name())));
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public boolean isAdmin() {
        return user.getBaseRole() == BaseRole.ADMIN_OFICINA;
    }

    public List<Permission> permissions() {
        if (user.getBaseRole() == BaseRole.ADMIN_OFICINA) {
            return List.of(Permission.values());
        }
        if (user.getBaseRole() == BaseRole.OFICINA) {
            return List.of(
                Permission.PRODUCCION_VER,
                Permission.DESPACHO_VER,
                Permission.CONSUMO_VER,
                Permission.CIERRE_DIARIO,
                Permission.REPORTES_VER,
                Permission.INVENTARIO_AJUSTAR
            );
        }

        List<Permission> permissions = new ArrayList<>();
        if (user.isCanRegisterProduction()) {
            permissions.add(Permission.PRODUCCION_CREAR);
            permissions.add(Permission.PRODUCCION_VER);
        }
        if (user.isCanRegisterDispatch()) {
            permissions.add(Permission.DESPACHO_CREAR);
            permissions.add(Permission.DESPACHO_VER);
        }
        if (user.isCanRegisterConsumption()) {
            permissions.add(Permission.CONSUMO_CREAR);
            permissions.add(Permission.CONSUMO_VER);
        }
        return permissions;
    }
}
