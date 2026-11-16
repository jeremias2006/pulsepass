package com.pulsepass.domain;

/**
 * Roles de seguridad. Se guardan como texto en {@code user_roles.role} (CHECK chk_user_role en V4).
 *
 * <p>USER: quien se registra por la API (único rol que se obtiene así). En el código, STAFF y ADMIN solo se crean con el
 * seeder local. Spring Security los usa con el prefijo {@code ROLE_} (ver {@code SecurityPrincipal}).</p>
 */
public enum Role {
    USER,
    STAFF,
    ADMIN
}
