package es.terencio.erp.auth.infrastructure.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class GranularSecurityAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        if (auth.getPrincipal() instanceof CustomUserDetails user) {
            String requiredPerm = requiresPermission.value();

            boolean hasPermission = user.getPermissions().stream()
                    .anyMatch(ctx -> ctx.permission().equals(requiredPerm));

            if (!hasPermission) {
                throw new AccessDeniedException("Missing required permission: " + requiredPerm);
            }
        }
    }
}
