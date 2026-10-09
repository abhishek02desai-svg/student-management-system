package com.example.sms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Used inside @PreAuthorize as  @authz.isSelf(authentication, #id)
 * "Is the logged-in student the same student whose data is being requested?"
 * The student id comes from the "studentId" claim of the JWT.
 */
@Component("authz")
public class AuthorizationChecker {

    public boolean isSelf(Authentication authentication, Long studentId) {

        if (authentication == null || studentId == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object claim = jwt.getClaims().get("studentId");
            return claim instanceof Number number && number.longValue() == studentId;
        }
        return false;
    }
}
